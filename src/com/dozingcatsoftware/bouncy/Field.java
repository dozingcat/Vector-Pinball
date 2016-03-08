package com.dozingcatsoftware.bouncy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.World;
import com.dozingcatsoftware.bouncy.elements.DropTargetGroupElement;
import com.dozingcatsoftware.bouncy.elements.FieldElement;
import com.dozingcatsoftware.bouncy.elements.FlipperElement;
import com.dozingcatsoftware.bouncy.elements.RolloverGroupElement;
import com.dozingcatsoftware.bouncy.elements.SensorElement;

import android.content.Context;

public class Field implements ContactListener {

    FieldLayout layout;
    World world;

    Set<Body> layoutBodies;
    List<Ball> balls;
    Set<Body> ballsAtTargets;

    // Allow access to model objects from Box2d bodies.
    Map<Body, FieldElement> bodyToFieldElement;
    Map<String, FieldElement> fieldElementsByID;
    Map<String, List<FieldElement>> elementsByGroupID = new HashMap<String, List<FieldElement>>();
    // Store FieldElements in arrays for optimized iteration.
    FieldElement[] fieldElementsArray;
    FieldElement[] fieldElementsToTick;

    Random RAND = new Random();

    long gameTime;
    // Actions scheduled to occur at specific times in the future.
    PriorityQueue<ScheduledAction> scheduledActions;

    Delegate delegate;

    GameState gameState = new GameState();
    GameMessage gameMessage;

    // Used in checkForStuckBall() to see if the ball hasn't moved recently.
    float lastBallPositionX;
    float lastBallPositionY;
    long nanosSinceBallMoved = -1;
    // Duration after which the ball is considered stuck if it hasn't moved significantly,
    // if it's a single ball and no flippers are active. Normally the time ratio is around 2,
    // so this will be about 5 real-world seconds.
    static final long STUCK_BALL_NANOS = 10000000000L;

    AudioPlayer audioPlayer = AudioPlayer.NoOpPlayer.getInstance();
    Clock clock = Clock.SystemClock.getInstance();

    // Interface to allow custom behavior for various game events.
    public static interface Delegate {
        public void gameStarted(Field field);

        public void ballLost(Field field);

        public void gameEnded(Field field);

        public void tick(Field field, long nanos);

        public void processCollision(Field field, FieldElement element, Body hitBody, Ball ball);

        public void flippersActivated(Field field, List<FlipperElement> flippers);

        public void allDropTargetsInGroupHit(Field field, DropTargetGroupElement targetGroup);

        public void allRolloversInGroupActivated(Field field, RolloverGroupElement rolloverGroup);

        public void ballInSensorRange(Field field, SensorElement sensor, Ball ball);

        public boolean isFieldActive(Field field);
    }

    // Helper class to represent actions scheduled in the future.
    static class ScheduledAction implements Comparable<ScheduledAction> {
        Long actionTime;
        Runnable action;

        @Override public int compareTo(ScheduledAction another) {
            // Sort by action time so these objects can be inserted into a PriorityQueue.
            return actionTime.compareTo(another.actionTime);
        }
    }

    /**
     * Creates Box2D world, reads layout definitions for the given level, and initializes the game
     * to the starting state.
     */
    public void resetForLevel(Context context, int level) {
        Vector2 gravity = new Vector2(0.0f, -1.0f);
        boolean doSleep = true;
        world = new World(gravity, doSleep);
        world.setContactListener(this);

        layout = FieldLayout.layoutForLevel(level, world);
        world.setGravity(new Vector2(0.0f, -layout.getGravity()));
        balls = new ArrayList<Ball>();
        ballsAtTargets = new HashSet<Body>();

        scheduledActions = new PriorityQueue<ScheduledAction>();
        gameTime = 0;

        // Map bodies and IDs to FieldElements, and get elements on whom tick() has to be called.
        bodyToFieldElement = new HashMap<Body, FieldElement>();
        fieldElementsByID = new HashMap<String, FieldElement>();
        List<FieldElement> tickElements = new ArrayList<FieldElement>();

        for(FieldElement element : layout.getFieldElements()) {
            if (element.getElementId()!=null) {
                fieldElementsByID.put(element.getElementId(), element);
            }
            for(Body body : element.getBodies()) {
                bodyToFieldElement.put(body, element);
            }
            if (element.shouldCallTick()) {
                tickElements.add(element);
            }
        }
        fieldElementsToTick = tickElements.toArray(new FieldElement[0]);
        fieldElementsArray = layout.getFieldElements().toArray(new FieldElement[0]);

        delegate = null;
        String delegateClass = layout.getDelegateClassName();
        if (delegateClass!=null) {
            if (delegateClass.indexOf('.')==-1) {
                delegateClass = "com.dozingcatsoftware.bouncy.fields." + delegateClass;
            }
            try {
                delegate = (Delegate)Class.forName(delegateClass).newInstance();
            }
            catch(Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        else {
            // Use no-op delegate if no class specified, so that field.getDelegate() is non-null.
            delegate = new BaseFieldDelegate();
        }
    }

    public void startGame() {
        gameState.setTotalBalls(layout.getNumberOfBalls());
        gameState.startNewGame();
        getDelegate().gameStarted(this);
    }

    /**
     * Returns the FieldElement with the given value for its "id" attribute, or null if there is
     * no such element.
     */
    public FieldElement getFieldElementById(String elementID) {
        return fieldElementsByID.get(elementID);
    }

    /**
     * Called to advance the game's state by the specified number of nanoseconds. iters is the
     * number of times to call the Box2D World.step method; more iterations produce better accuracy.
     * After updating physics, processes element collisions, calls tick() on every FieldElement,
     * and performs scheduled actions.
     */
    void tick(long nanos, int iters) {
        float dt = (nanos/1e9f) / iters;

        for(int i=0; i<iters; i++) {
            clearBallContacts();
            world.step(dt, 10, 10);
            processBallContacts();
        }

        gameTime += nanos;
        processElementTicks();
        processScheduledActions();
        processGameMessages();
        checkForStuckBall(nanos);

        getDelegate().tick(this, nanos);
    }

    /** Calls the tick() method of every FieldElement in the layout. */
    void processElementTicks() {
        int size = fieldElementsToTick.length;
        for(int i=0; i<size; i++) {
            fieldElementsToTick[i].tick(this);
        }
    }

    /**
     * Runs actions that were scheduled with scheduleAction and whose execution time has arrived.
     */
    void processScheduledActions() {
        while (true) {
            ScheduledAction nextAction = scheduledActions.peek();
            if (nextAction!=null && gameTime >= nextAction.actionTime) {
                scheduledActions.poll();
                nextAction.action.run();
            }
            else {
                break;
            }
        }
    }

    /**
     * Schedules an action to be run after the given interval in milliseconds has elapsed.
     * Interval is in game time, not real time.
     */
    public void scheduleAction(long interval, Runnable action) {
        ScheduledAction sa = new ScheduledAction();
        // interval is in milliseconds, gameTime is in nanoseconds
        sa.actionTime = gameTime + (interval * 1000000);
        sa.action = action;
        scheduledActions.add(sa);
    }

    /**
     * Launches a new ball. The position and velocity of the ball are controlled by the parameters
     * in the field layout JSON.
     */
    public Ball launchBall() {
        List<Float> position = layout.getLaunchPosition();
        List<Float> velocity = layout.getLaunchVelocity();
        float radius = layout.getBallRadius();

        Ball ball = Ball.create(world, position.get(0), position.get(1), radius,
                layout.getBallColor(), layout.getSecondaryBallColor());
        ball.getBody().setLinearVelocity(new Vector2(velocity.get(0), velocity.get(1)));
        this.balls.add(ball);
        audioPlayer.playBall();
        return ball;
    }

    /** Removes a ball from play. If there are no other balls on the field, calls doBallLost. */
    public void removeBall(Ball ball) {
        world.destroyBody(ball.getBody());
        this.balls.remove(ball);
        if (this.balls.size()==0) {
            this.doBallLost();
        }
    }

    /**
     * Removes a ball from play, but does not call doBallLost for end-of-ball processing even if
     * no balls remain.
     */
    public void removeBallWithoutBallLoss(Ball ball) {
        world.destroyBody(ball.getBody());
        this.balls.remove(ball);
    }

    /**
     * Called when a ball has ended. Ends the game if that was the last ball, otherwise updates
     * GameState to the next ball. Shows a game message to indicate the ball number or game over.
     */
    public void doBallLost() {
        boolean hasExtraBall = (this.gameState.getExtraBalls() > 0);
        this.gameState.doNextBall();
        // display message for next ball or game over
        String msg = null;
        if (hasExtraBall) msg = "Shoot Again";
        else if (this.gameState.isGameInProgress()) msg = "Ball " + this.gameState.getBallNumber();

        if (msg!=null) {
            // game is still going, show message after delay
            final String msg2 = msg; // must be final for closure, yay Java
            this.scheduleAction(1500, new Runnable() {
                @Override
                public void run() {
                    showGameMessage(msg2, 1500, false); // no sound effect
                }
            });
        }
        else {
            endGame();
        }

        getDelegate().ballLost(this);
    }

    /**
     * Returns true if there are active elements in motion. Returns false if there are no active
     * elements, indicating that tick() can be called with larger time steps, less frequently, or
     * not at all.
     */
    public boolean hasActiveElements() {
        // HACK: to allow flippers to drop properly at beginning of game, we need accurate simulation.
        if (this.gameTime < 500) return true;
        // Allow delegate to return true even if there are no balls.
        if (getDelegate().isFieldActive(this)) return true;
        return this.getBalls().size() > 0;
    }


    ArrayList<Ball> deadBalls = new ArrayList<Ball>(); // avoid allocation every time
    /**
     * Removes balls that are not in play, as determined by optional "deadzone" property of
     * launch parameters in field layout.
     */
    public void removeDeadBalls() {
        List<Float> deadRect = layout.getLaunchDeadZone();
        if (deadRect==null) return;

        for(int i=0; i<this.balls.size(); i++) {
            Ball ball = this.balls.get(i);
            Vector2 bpos = ball.getPosition();
            if (bpos.x > deadRect.get(0) && bpos.y > deadRect.get(1) &&
                    bpos.x < deadRect.get(2) && bpos.y < deadRect.get(3)) {
                deadBalls.add(ball);
                world.destroyBody(ball.getBody());
            }
        }

        for(int i=0; i<deadBalls.size(); i++) {
            this.balls.remove(deadBalls.get(i));
        }
        deadBalls.clear();
    }

    /** Called by FieldView to draw the balls currently in play. */
    public void drawBalls(IFieldRenderer renderer) {
        for(int i=0; i<this.balls.size(); i++) {
            this.balls.get(i).draw(renderer);
        }
    }

    ArrayList<FlipperElement> activatedFlippers = new ArrayList<FlipperElement>();
    /**
     * Called to engage or disengage all flippers. If called with an argument of true, and all
     * flippers were not previously engaged, calls the flipperActivated methods of all field
     * elements and the field's delegate.
     */
    public void setFlippersEngaged(List<FlipperElement> flippers, boolean engaged) {
        activatedFlippers.clear();
        boolean allFlippersPreviouslyActive = true;
        int fsize = flippers.size();
        for(int i=0; i<fsize; i++) {
            FlipperElement flipper = flippers.get(i);
            if (!flipper.isFlipperEngaged()) {
                allFlippersPreviouslyActive = false;
                if (engaged) {
                    activatedFlippers.add(flipper);
                }
            }
            flipper.setFlipperEngaged(engaged);
        }

        if (engaged && !allFlippersPreviouslyActive) {
            audioPlayer.playFlipper();
            for(FieldElement element : this.getFieldElementsArray()) {
                element.flippersActivated(this, activatedFlippers);
            }
            getDelegate().flippersActivated(this, activatedFlippers);
        }
    }

    public void setAllFlippersEngaged(boolean engaged) {
        setFlippersEngaged(this.getFlipperElements(), engaged);
    }

    public void setLeftFlippersEngaged(boolean engaged) {
        setFlippersEngaged(layout.getLeftFlipperElements(), engaged);
    }
    public void setRightFlippersEngaged(boolean engaged) {
        setFlippersEngaged(layout.getRightFlipperElements(), engaged);
    }

    /**
     * Ends a game in progress by removing all balls in play, calling setGameInProgress(false)
     * on the GameState, and setting a "Game Over" message for display by the score view.
     */
    public void endGame() {
        audioPlayer.playStart(); // play startup sound at end of game
        for(Ball ball : this.getBalls()) {
            world.destroyBody(ball.getBody());
        }
        this.balls.clear();
        this.getGameState().setGameInProgress(false);
        this.showGameMessage("Game Over", 2500);
        getDelegate().gameEnded(this);
    }

    /** Adjusts gravity in response to the device being tilted; not currently used. */
    public void receivedOrientationValues(float azimuth, float pitch, float roll) {
        double angle = roll - Math.PI/2;
        float gravity = layout.getGravity();
        float gx = (float)(gravity * Math.cos(angle));
        float gy = -Math.abs((float)(gravity * Math.sin(angle)));
        world.setGravity(new Vector2(gx, gy));
    }

    // Contact support. Keep parallel lists of balls and the fixtures they contact.
    // A ball can have multiple contacts in the same tick, e.g. against two walls.
    ArrayList<Ball> contactedBalls = new ArrayList<Ball>();
    ArrayList<Fixture> contactedFixtures = new ArrayList<Fixture>();

    void clearBallContacts() {
        contactedBalls.clear();
        contactedFixtures.clear();
    }

    /**
     * Called after Box2D world step method, to notify FieldElements that the ball collided with.
     */
    void processBallContacts() {
        for(int i=0; i<contactedBalls.size(); i++) {
            Ball ball = contactedBalls.get(i);
            Fixture f = contactedFixtures.get(i);
            FieldElement element = bodyToFieldElement.get(f.getBody());
            if (element!=null) {
                element.handleCollision(ball, f.getBody(), this);
                if (delegate!=null) {
                    delegate.processCollision(this, element, f.getBody(), ball);
                }
                if (element.getScore()!=0) {
                    this.gameState.addScore(element.getScore());
                    audioPlayer.playScore();
                }
            }
        }
    }

    private Ball ballWithBody(Body body) {
        for (int i=0; i<this.balls.size(); i++) {
            Ball ball = this.balls.get(i);
            if (ball.getBody() == body) {
                return ball;
            }
        }
        return null;
    }

    // Box2D ContactListener methods.
    @Override public void beginContact(Contact contact) {
        // Nothing here, contact is recorded in endContact().
    }

    @Override public void endContact(Contact contact) {
        Fixture fixture = null;
        Ball ball = ballWithBody(contact.getFixtureA().getBody());
        if (ball != null) {
            fixture = contact.getFixtureB();
        }
        else {
            ball = ballWithBody(contact.getFixtureB().getBody());
            if (ball != null) {
                fixture = contact.getFixtureA();
            }
        }

        if (ball != null) {
            contactedBalls.add(ball);
            contactedFixtures.add(fixture);
        }
    }

    @Override public void postSolve(Contact arg0, ContactImpulse arg1) {
        // Not used.
    }

    @Override public void preSolve(Contact arg0, Manifold arg1) {
        // Not used.
    }
    // End ContactListener methods.

    /**
     * Displays a message in the score view for the specified duration in milliseconds.
     * Duration is in real world time, not simulated game time.
     */
    public void showGameMessage(String text, long duration, boolean playSound) {
        if (playSound) audioPlayer.playMessage();
        gameMessage = new GameMessage();
        gameMessage.text = text;
        gameMessage.duration = duration;
        gameMessage.creationTime = clock.currentTimeMillis();
    }

    public void showGameMessage(String text, long duration) {
        showGameMessage(text, duration, true);
    }

    /** Updates time remaining on current game message, and removes it if expired. */
    void processGameMessages() {
        if (gameMessage!=null) {
            if (clock.currentTimeMillis() - gameMessage.creationTime > gameMessage.duration) {
                gameMessage = null;
            }
        }
    }

    /**
     * Checks whether the ball appears to be stuck, and nudges it if so.
     */
    void checkForStuckBall(long nanos) {
        // Only do this for single balls. This means it's theoretically possible for multiple
        // balls to be simultaneously stuck during multiball; that would be impressive.
        if (this.getBalls().size() != 1) {
            nanosSinceBallMoved = -1;
            return;
        }
        Ball ball = this.getBalls().get(0);
        Vector2 pos = ball.getPosition();
        if (nanosSinceBallMoved < 0) {
            // New ball.
            lastBallPositionX = pos.x;
            lastBallPositionY = pos.y;
            nanosSinceBallMoved = 0;
            return;
        }
        if (ball.getLinearVelocity().len2() > 0.01f ||
                pos.dst2(lastBallPositionX, lastBallPositionY) > 0.01f) {
            // Ball has moved since last time; reset counter.
            lastBallPositionX = pos.x;
            lastBallPositionY = pos.y;
            nanosSinceBallMoved = 0;
            return;
        }
        // Don't add time if any flipper is activated (the flipper could be trapping the ball).
        List<FlipperElement> flippers = this.getFlipperElements();
        for (int i=0; i<flippers.size(); i++) {
            if (flippers.get(i).isFlipperEngaged()) return;
        }
        // Increment time counter and bump if the ball hasn't moved in a while.
        nanosSinceBallMoved += nanos;
        if (nanosSinceBallMoved > STUCK_BALL_NANOS) {
            showGameMessage("Bump!", 1000);
            // Could make the bump impulse table-specific if needed.
            Vector2 impulse = new Vector2(RAND.nextBoolean() ? 1f : -1f, 1.5f);
            ball.applyLinearImpulse(impulse);
            nanosSinceBallMoved = 0;
        }
    }

    public void addExtraBall() {
        gameState.setExtraBalls(gameState.getExtraBalls() + 1);
    }

    /**
     * Adds the given value to the game score. The value is multiplied by the current multiplier.
     */
    public void addScore(long s) {
        gameState.addScore(s);
    }

    public long getScore() {
        return gameState.getScore();
    }

    public void incrementScoreMultiplier() {
        gameState.incrementScoreMultiplier();
    }

    public double getScoreMultiplier() {
        return gameState.getScoreMultiplier();
    }

    public void setScoreMultiplier(double multiplier) {
        gameState.setScoreMultiplier(multiplier);
    }

    // Accessors.
    public float getWidth() {
        return layout.getWidth();
    }
    public float getHeight() {
        return layout.getHeight();
    }

    public Set<Body> getLayoutBodies() {
        return layoutBodies;
    }
    public List<Ball> getBalls() {
        return balls;
    }
    public FieldLayout getFieldLayout() {
        return layout;
    }
    public List<FlipperElement> getFlipperElements() {
        return layout.getFlipperElements();
    }
    public List<FieldElement> getFieldElements() {
        return layout.getFieldElements();
    }
    public FieldElement[] getFieldElementsArray() {
        return fieldElementsArray;
    }

    public GameMessage getGameMessage() {
        return gameMessage;
    }

    public GameState getGameState() {
        return gameState;
    }

    public long getGameTime() {
        return gameTime;
    }

    public float getTargetTimeRatio() {
        return layout.getTargetTimeRatio();
    }

    public World getBox2DWorld() {
        return world;
    }

    public Delegate getDelegate() {
        return delegate;
    }

    public Object getValueWithKey(String key) {
        return layout.getValueWithKey(key);
    }

    public AudioPlayer getAudioPlayer() {
        return audioPlayer;
    }
    public void setAudioPlayer(AudioPlayer player) {
        audioPlayer = player;
    }

    public Clock getClock() {
        return clock;
    }
    public void setClock(Clock clock) {
        this.clock = clock;
    }
}
