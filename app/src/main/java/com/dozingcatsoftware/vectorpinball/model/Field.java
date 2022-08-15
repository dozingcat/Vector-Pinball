package com.dozingcatsoftware.vectorpinball.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.LongSupplier;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.dozingcatsoftware.vectorpinball.elements.BumperElement;
import com.dozingcatsoftware.vectorpinball.elements.DropTargetGroupElement;
import com.dozingcatsoftware.vectorpinball.elements.FieldElement;
import com.dozingcatsoftware.vectorpinball.elements.FlipperElement;
import com.dozingcatsoftware.vectorpinball.elements.RolloverGroupElement;
import com.dozingcatsoftware.vectorpinball.elements.SensorElement;

public class Field implements ContactListener {

    FieldLayout layout;
    WorldLayers worlds;

    ArrayList<Ball> balls;
    ArrayList<Shape> shapes;

    // Allow access to model objects from Box2d bodies.
    Map<Body, FieldElement> bodyToFieldElement;
    Map<String, FieldElement> fieldElementsByID;
    // Store FieldElements in arrays for optimized iteration.
    FieldElement[] fieldElementsArray;
    FieldElement[] fieldElementsToTick;

    Random RAND = new Random();

    long gameTimeNanos;
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
    static final long STUCK_BALL_NANOS = 10_000_000_000L;

    boolean usedMercyBall = false;
    Long ballStartGameTimeNanos = null;
    Long multiballStartGameTimeNanos = null;
    Long lastBallLaunchGameTimeNanos = null;
    Long lostBallWallTimeMillis = null;

    Long lastMultiplerIncrementGameTimeNanos = null;

    // `zoomNanos` is 0 if the field should be zoomed out fully and `ZOOM_DURATION_NANOS` if
    // zoomed in fully.
    static final long ZOOM_DURATION_NANOS = 1_000_000_000L;
    long zoomNanos = 0;
    Vector2 zoomCenter = null;

    LongSupplier milliTimeFn;
    AudioPlayer audioPlayer;
    IStringResolver stringResolver;

    // Pass System::currentTimeMillis as `milliTimeFn` to use the standard system clock.
    public Field(LongSupplier milliTimeFn, IStringResolver sr, AudioPlayer player) {
        this.milliTimeFn = milliTimeFn;
        this.stringResolver = sr;
        this.audioPlayer = player;
    }

    // Interface to allow custom behavior for various game events.
    public interface Delegate {
        void gameStarted(Field field);

        void ballLost(Field field);

        void gameEnded(Field field);

        void tick(Field field, long nanos);

        void processCollision(Field field, FieldElement element, Body hitBody, Ball ball);

        void flippersActivated(Field field, List<FlipperElement> flippers);

        void allDropTargetsInGroupHit(Field field, DropTargetGroupElement targetGroup, Ball ball);

        void allRolloversInGroupActivated(Field field, RolloverGroupElement rollovers, Ball ball);

        void ballInSensorRange(Field field, SensorElement sensor, Ball ball);

        boolean isFieldActive(Field field);
    }

    // Used by field delegates to retrieve localized strings.
    public String resolveString(String key, Object... params) {
        return this.stringResolver.resolveString(key, params);
    }

    // Helper class to represent actions scheduled in the future.
    static class ScheduledAction implements Comparable<ScheduledAction> {
        Long actionTimeNanos;
        Runnable action;

        @Override public int compareTo(ScheduledAction another) {
            // Sort by action time so these objects can be inserted into a PriorityQueue.
            return actionTimeNanos.compareTo(another.actionTimeNanos);
        }
    }

    /**
     * Creates Box2D world, reads layout definitions for the given level, and initializes the game
     * to the starting state.
     */
    public void resetForLayoutMap(
            Map<String, Object> layoutMap, Function<Field, Delegate> delegateFn) {
        this.worlds = new WorldLayers(this);
        this.layout = new FieldLayout(layoutMap, worlds);
        worlds.setGravity(new Vector2(0.0f, -this.layout.getGravity()));
        balls = new ArrayList<>();
        shapes = new ArrayList<>();

        scheduledActions = new PriorityQueue<>();
        gameTimeNanos = 0;

        // Map bodies and IDs to FieldElements, and get elements on whom tick() has to be called.
        bodyToFieldElement = new HashMap<>();
        fieldElementsByID = new HashMap<>();
        List<FieldElement> tickElements = new ArrayList<>();

        for (FieldElement element : layout.getFieldElements()) {
            if (element.getElementId() != null) {
                fieldElementsByID.put(element.getElementId(), element);
            }
            for (Body body : element.getBodies()) {
                bodyToFieldElement.put(body, element);
            }
            if (element.shouldCallTick()) {
                tickElements.add(element);
            }
        }
        fieldElementsToTick = tickElements.toArray(new FieldElement[0]);
        fieldElementsArray = layout.getFieldElements().toArray(new FieldElement[0]);

        delegate = delegateFn.apply(this);
    }

    public void resetForLayoutMap(Map<String, Object> layoutMap) {
        resetForLayoutMap(layoutMap, Field::createDelegateFromLayoutClass);
    }

    public static Delegate createDelegateFromLayoutClass(Field field) {
        String delegateClass = field.layout.getDelegateClassName();
        if (delegateClass != null) {
            if (delegateClass.indexOf('.') == -1) {
                delegateClass = "com.dozingcatsoftware.vectorpinball.fields." + delegateClass;
            }
            try {
                return (Delegate) Class.forName(delegateClass).getConstructor().newInstance();
            }
            catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        else {
            // Use no-op delegate if no class specified, so that field.getDelegate() is non-null.
            return new BaseFieldDelegate();
        }
    }

    private void _startGame(boolean unlimitedBalls) {
        ballStartGameTimeNanos = null;
        multiballStartGameTimeNanos = null;
        lostBallWallTimeMillis = null;
        lastBallLaunchGameTimeNanos = null;
        lastMultiplerIncrementGameTimeNanos = null;
        usedMercyBall = false;
        gameState.setTotalBalls(layout.getNumberOfBalls());
        gameState.setUnlimitedBalls(unlimitedBalls);
        gameState.startNewGame();
        getDelegate().gameStarted(this);
    }

    public void startGame() {
        _startGame(false);
    }

    public void startGameWithUnlimitedBalls() {
        _startGame(true);
    }

    /**
     * Returns the FieldElement with the given value for its "id" attribute, or null if there is
     * no such element.
     */
    public <T extends FieldElement> T getFieldElementById(String elementID) {
        return (T) fieldElementsByID.get(elementID);
    }

    /**
     * Called to advance the game's state by the specified number of nanoseconds. iters is the
     * number of times to call the Box2D World.step method; more iterations produce better accuracy.
     * After updating physics, processes element collisions, calls tick() on every FieldElement,
     * and performs scheduled actions.
     */
    public void tick(long nanos, int iters) {
        float dt = (nanos / 1e9f) / iters;

        for (int i = 0; i < iters; i++) {
            clearBallContacts();
            worlds.step(dt, 10, 10);
            processBallContacts();
        }

        gameTimeNanos += nanos;
        processElementTicks(nanos);
        processScheduledActions();
        processGameMessages();
        processZoom(nanos);
        checkForStuckBall(nanos);

        getDelegate().tick(this, nanos);
    }

    /** Calls the tick() method of every FieldElement in the layout. */
    private void processElementTicks(long nanos) {
        for (FieldElement elem : fieldElementsToTick) {
            elem.tick(this, nanos);
        }
    }

    /**
     * Runs actions that were scheduled with scheduleAction and whose execution time has arrived.
     */
    private void processScheduledActions() {
        while (true) {
            ScheduledAction nextAction = scheduledActions.peek();
            if (nextAction != null && gameTimeNanos >= nextAction.actionTimeNanos) {
                scheduledActions.poll();
                nextAction.action.run();
            }
            else {
                break;
            }
        }
    }

    public void setShapes(List<Shape> shapes) {
        this.shapes.clear();
        this.shapes.ensureCapacity(shapes.size());
        this.shapes.addAll(shapes);
    }

    public Ball createBall(float x, float y) {
        Ball ball = Ball.create(worlds, 0, x, y, layout.getBallRadius(),
                layout.getBallColor(), layout.getSecondaryBallColor());
        this.balls.add(ball);
        return ball;
    }

    public void playBallLaunchSound() {
        audioPlayer.playBall();
    }

    /**
     * Schedules an action to be run after the given interval in milliseconds has elapsed.
     * Interval is in game time, not real time.
     */
    public void scheduleAction(long intervalMillis, Runnable action) {
        ScheduledAction sa = new ScheduledAction();
        sa.actionTimeNanos = gameTimeNanos + TimeUnit.MILLISECONDS.toNanos(intervalMillis);;
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
        Ball ball = createBall(position.get(0), position.get(1));
        ball.getBody().setLinearVelocity(new Vector2(velocity.get(0), velocity.get(1)));
        playBallLaunchSound();
        lostBallWallTimeMillis = null;
        lastBallLaunchGameTimeNanos = gameTimeNanos;
        if (balls.size() > 1) {
            if (multiballStartGameTimeNanos == null) {
                multiballStartGameTimeNanos = gameTimeNanos;
            }
        }
        else {
            ballStartGameTimeNanos = gameTimeNanos;
        }
        return ball;
    }

    private boolean shouldLaunchMercyBall() {
        Long t = ballStartGameTimeNanos;
        // if (t != null) android.util.Log.i("Field", "Mercy time: " + (gameTimeNanos - t));
        return (!usedMercyBall && gameTimeNanos - t <= layout.getMercyBallDurationNanos());
    }

    private void launchMercyBall() {
        usedMercyBall = true;
        String msg = stringResolver.resolveString("ball_saved_message");
        showGameMessage(msg, 1500, true);
        launchBall();
    }

    private boolean shouldRestoreLostBallInMultiball() {
        return multiballStartGameTimeNanos != null &&
                gameTimeNanos - multiballStartGameTimeNanos <= layout.getMultiballSaverDurationNanos();
    }

    private void restoreLostBallInMultiball() {
        // Don't launch multiple balls in quick succession. If you lose two balls simultaneously,
        // you'll only get one back.
        if (gameTimeNanos - lastBallLaunchGameTimeNanos < 1000) {
            return;
        }
        String msg = stringResolver.resolveString("ball_saved_message");
        showGameMessage(msg, 1000, true);
        launchBall();
    }

    /** Removes a ball from play. If there are no other balls on the field, calls doBallLost. */
    public void removeBall(Ball ball) {
        this.removeBallWithoutBallLoss(ball);
        if (this.balls.isEmpty()) {
            if (shouldLaunchMercyBall()) {
                launchMercyBall();
            }
            else {
                this.doBallLost();
            }
        }
        else {
            if (shouldRestoreLostBallInMultiball()) {
                restoreLostBallInMultiball();
            }
        }
    }

    /**
     * Removes a ball from play, but does not call doBallLost for end-of-ball processing even if
     * no balls remain.
     */
    public void removeBallWithoutBallLoss(Ball ball) {
        ball.destroySelf();
        this.balls.remove(ball);
    }

    private boolean shouldPreserveLastMultiplierIncrease() {
        Long t = lastMultiplerIncrementGameTimeNanos;
        // if (t != null) android.util.Log.i("Field", "Multiplier time: " + (gameTimeNanos - t));
        return t != null && gameTimeNanos - t <= layout.getPreserveMultiplierIncreaseDurationNanos();
    }

    /**
     * Called when a ball has ended. Ends the game if that was the last ball, otherwise updates
     * GameState to the next ball. Shows a game message to indicate the ball number or game over.
     */
    private void doBallLost() {
        lostBallWallTimeMillis = milliTimeFn.getAsLong();
        usedMercyBall = false;

        boolean hasExtraBall = (this.gameState.getExtraBalls() > 0);
        this.gameState.doNextBall();
        // If ball was lost right after increasing the multiplier, preserve the increase.
        if (shouldPreserveLastMultiplierIncrease()) {
            gameState.incrementScoreMultiplier();
        }
        lastMultiplerIncrementGameTimeNanos = null;

        // Display message for next ball or game over.
        String msg = null;
        if (hasExtraBall) {
            msg = this.resolveString("shoot_again_message");
        }
        else if (this.gameState.isGameInProgress()) {
            msg = this.resolveString("ball_number_message", this.gameState.getBallNumber());
        }
        if (msg != null) {
            // Game is still going, show message after delay.
            final String msg2 = msg;
            this.scheduleAction(1500, () -> showGameMessage(msg2, 1500, false));
        }
        else {
            endGame();
        }
        getDelegate().ballLost(this);
    }

    /**
     * Returns true if there are no balls in play, and the most recent ball loss happened within
     * `millis` of the current time.
     */
    public boolean ballLostWithinMillis(long millis) {
        return lostBallWallTimeMillis != null && milliTimeFn.getAsLong() - lostBallWallTimeMillis <= millis;
    }

    /**
     * Returns true if there are active elements in motion. Returns false if there are no active
     * elements, indicating that tick() can be called with larger time steps, less frequently, or
     * not at all.
     */
    public boolean hasActiveElements() {
        // HACK: to allow flippers to drop properly at start of game, we need accurate simulation.
        if (this.gameTimeNanos < 500) return true;
        // Allow delegate to return true even if there are no balls.
        if (getDelegate().isFieldActive(this)) return true;
        // We need smooth animation if there are any balls, or if we're zooming out after all balls
        // were lost.
        return this.getBalls().size() > 0 || zoomNanos > 0;
    }

    /**
     * Removes balls that are not in play, as determined by optional "deadzone" property of
     * launch parameters in field layout.
     */
    public void removeDeadBalls() {
        List<Float> deadRect = layout.getLaunchDeadZone();
        if (deadRect == null) return;

        ArrayList<Ball> deadBalls = null;  // Don't allocate until needed.
        for (int i = 0; i < this.balls.size(); i++) {
            Ball ball = this.balls.get(i);
            Vector2 bpos = ball.getPosition();
            if (bpos.x > deadRect.get(0) && bpos.y > deadRect.get(1) &&
                    bpos.x < deadRect.get(2) && bpos.y < deadRect.get(3)) {
                if (deadBalls == null) {
                    deadBalls = new ArrayList<>();
                }
                deadBalls.add(ball);
            }
        }

        if (deadBalls != null) {
            for (Ball b : deadBalls) {
                this.removeBallWithoutBallLoss(b);
            }
        }
    }

    // Reusable array for sorting elements and balls into the order in which they should be draw.
    // Earlier items are drawn first, so "upper" items should compare "greater" than lower.
    private ArrayList<IDrawable> elementsInDrawOrder = new ArrayList<>();
    // At the same layer, balls are drawn after field elements, which are drawn after custom shapes.
    // Except bumpers which are drawn last, so balls appear under their outer circles.
    private Comparator<IDrawable> drawOrdering = Comparator
            .comparingInt(IDrawable::getLayer)
            .thenComparingInt(Field::drawOrderRank);

    private static int drawOrderRank(IDrawable obj) {
        if (obj instanceof BumperElement) {
            return 4;
        }
        if (obj instanceof FieldElement) {
            return 2;
        }
        if (obj instanceof Ball) {
            return 3;
        }
        return 1;
    }

    /**
     * Draws all field elements and balls. Levels are drawn low to high, and each ball is drawn
     * after (i.e. on top of) all elements at its level.
     */
    public void draw(IFieldRenderer renderer) {
        // Draw levels low to high, and draw each ball after everything else at its level.
        elementsInDrawOrder.clear();
        elementsInDrawOrder.addAll(Arrays.asList(this.getFieldElementsArray()));
        elementsInDrawOrder.addAll(this.balls);
        elementsInDrawOrder.addAll(this.shapes);
        Collections.sort(elementsInDrawOrder, drawOrdering);

        for (int i = 0; i < elementsInDrawOrder.size(); i++) {
            this.elementsInDrawOrder.get(i).draw(this, renderer);
        }
    }

    ArrayList<FlipperElement> activatedFlippers = new ArrayList<>();

    /**
     * Called to engage or disengage all flippers. If called with an argument of true, and all
     * flippers were not previously engaged, calls the flipperActivated methods of all field
     * elements and the field's delegate.
     */
    private void setFlippersEngaged(List<FlipperElement> flippers, boolean engaged) {
        activatedFlippers.clear();
        boolean allFlippersPreviouslyActive = true;
        int fsize = flippers.size();
        for (int i = 0; i < fsize; i++) {
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
            for (FieldElement element : this.getFieldElementsArray()) {
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
        for (Ball ball : this.getBalls()) {
            ball.destroySelf();
        }
        this.balls.clear();
        this.getGameState().setGameInProgress(false);
        this.showGameMessage(this.resolveString("game_over_message"), 2500);
        getDelegate().gameEnded(this);
    }

    /** Adjusts gravity in response to the device being tilted; not currently used. */
    /*
    public void receivedOrientationValues(float azimuth, float pitch, float roll) {
        double angle = roll - Math.PI / 2;
        float gravity = layout.getGravity();
        float gx = (float) (gravity * Math.cos(angle));
        float gy = -Math.abs((float) (gravity * Math.sin(angle)));
        world.setGravity(new Vector2(gx, gy));
    }
    */

    // Contact support. Keep parallel lists of balls and the fixtures they contact.
    // A ball can have multiple contacts in the same tick, e.g. against two walls.
    ArrayList<Ball> contactedBalls = new ArrayList<>();
    ArrayList<Fixture> contactedFixtures = new ArrayList<>();

    private void clearBallContacts() {
        contactedBalls.clear();
        contactedFixtures.clear();
    }

    /**
     * Called after Box2D world step method, to notify FieldElements that the ball collided with.
     */
    private void processBallContacts() {
        for (int i = 0; i < contactedBalls.size(); i++) {
            Ball ball = contactedBalls.get(i);
            Fixture f = contactedFixtures.get(i);
            FieldElement element = bodyToFieldElement.get(f.getBody());
            if (element != null) {
                element.handleCollision(ball, f.getBody(), this);
                if (delegate != null) {
                    delegate.processCollision(this, element, f.getBody(), ball);
                }
                if (element.getScore() != 0) {
                    this.gameState.addScore(element.getScore());
                    audioPlayer.playScore();
                }
            }
        }
    }

    private Ball ballWithBody(Body body) {
        for (int i = 0; i < this.balls.size(); i++) {
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
    public void showGameMessage(String text, long durationMillis, boolean playSound) {
        if (playSound) audioPlayer.playMessage();
        gameMessage = new GameMessage();
        gameMessage.text = text;
        gameMessage.durationMillis = durationMillis;
        gameMessage.creationTimeMillis = milliTimeFn.getAsLong();
    }

    public void showGameMessage(String text, long durationMillis) {
        showGameMessage(text, durationMillis, true);
    }

    /** Updates time remaining on current game message, and removes it if expired. */
    private void processGameMessages() {
        if (gameMessage != null) {
            long messageEndTime = gameMessage.creationTimeMillis + gameMessage.durationMillis;
            if (milliTimeFn.getAsLong() > messageEndTime) {
                gameMessage = null;
            }
        }
    }

    private boolean canBeZoomedIn() {
        return this.balls.size() == 1;
    }

    public float zoomRatio() {
        return (float) (1.0 * zoomNanos / ZOOM_DURATION_NANOS);
    }

    public Vector2 zoomCenterPoint() {
        return (zoomCenter != null) ?
                zoomCenter : new Vector2(getLaunchPosition().get(0), getLaunchPosition().get(1));
    }

    private void processZoom(long nanos) {
        zoomNanos = canBeZoomedIn() ?
                Math.min(ZOOM_DURATION_NANOS, zoomNanos + nanos) :
                Math.max(0, zoomNanos - nanos);
        // When the last ball goes away, zoom out from its last position.
        zoomCenter = (this.balls.size() >= 1) ? this.balls.get(0).getPosition() : zoomCenter;
    }

    /**
     * Checks whether the ball appears to be stuck, and nudges it if so.
     */
    private void checkForStuckBall(long nanos) {
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
        for (int i = 0; i < flippers.size(); i++) {
            if (flippers.get(i).isFlipperEngaged()) return;
        }
        // Increment time counter and bump if the ball hasn't moved in a while.
        nanosSinceBallMoved += nanos;
        if (nanosSinceBallMoved > STUCK_BALL_NANOS) {
            showGameMessage(this.stringResolver.resolveString("bump_message"), 1000);
            // Could make the bump impulse table-specific if needed.
            Vector2 impulse = new Vector2(RAND.nextBoolean() ? 1f : -1f, 1.5f);
            ball.applyLinearImpulse(impulse);
            nanosSinceBallMoved = 0;
        }
    }

    public void addExtraBall() {
        gameState.addExtraBall();
    }

    public boolean hasBallAtLayer(int layer) {
        for (int i = 0; i < this.balls.size(); i++) {
            if (this.balls.get(i).getLayer() == layer) {
                return true;
            }
        }
        return false;
    }

    // Not used in production builds, but shows the returned value in the ScoreView for debugging.
    public String getDebugMessage() {
        return null;
        /*
        if (!gameState.isGameInProgress() || ballStartGameTimeNanos == null) {
            return null;
        }
        if (balls.size() <= 1) {
            long elapsed = gameTimeNanos - ballStartGameTimeNanos;
            long remaining = layout.getMercyBallDurationNanos() - elapsed;
            return String.format("%.1f", Math.max(0, remaining) / 1e9);
        }
        else {
            long elapsed = gameTimeNanos - multiballStartGameTimeNanos;
            long remaining = layout.getMultiballSaverDurationNanos() - elapsed;
            return String.format("%.1f", Math.max(0, remaining) / 1e9);
        }
        */
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

    public double getScoreMultiplier() {
        return gameState.getScoreMultiplier();
    }

    public void setScoreMultiplier(double multiplier) {
        gameState.setScoreMultiplier(multiplier);
    }

    public void incrementAndDisplayScoreMultiplier(long durationMillis) {
        lastMultiplerIncrementGameTimeNanos = gameTimeNanos;
        gameState.incrementScoreMultiplier();
        String msg = resolveString("multiplier_message", (int) this.gameState.getScoreMultiplier());
        this.showGameMessage(msg, durationMillis);
    }

    // Accessors.
    public float getWidth() {
        return layout.getWidth();
    }

    public float getHeight() {
        return layout.getHeight();
    }

    public List<Ball> getBalls() {
        return balls;
    }

    public List<Float> getLaunchPosition() {
        return layout.getLaunchPosition();
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

    public long getGameTimeNanos() {
        return gameTimeNanos;
    }

    public float getTargetTimeRatio() {
        return layout.getTargetTimeRatio();
    }

    public Delegate getDelegate() {
        return delegate;
    }

    public String getScriptText() {
        return layout.getScriptText();
    }

    public Object getValueWithKey(String key) {
        return layout.getValueWithKey(key);
    }

    public AudioPlayer getAudioPlayer() {
        return audioPlayer;
    }
}
