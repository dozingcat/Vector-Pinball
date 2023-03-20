package com.dozingcatsoftware.bouncy;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.badlogic.gdx.physics.box2d.Box2D;
import com.dozingcatsoftware.vectorpinball.model.IStringResolver;
import com.dozingcatsoftware.vectorpinball.util.IOUtils;
import com.dozingcatsoftware.vectorpinball.model.Field;
import com.dozingcatsoftware.vectorpinball.model.FieldDriver;
import com.dozingcatsoftware.vectorpinball.model.GameState;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Rect;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

public class BouncyActivity extends Activity {

    static {
        Box2D.init();
    }

    CanvasFieldView canvasFieldView;
    ScoreView scoreView;
    View pauseButton;

    GLFieldView glFieldView;
    GL10Renderer gl10Renderer;
    GL20Renderer gl20Renderer;
    // Semi-arbitrary requirement for Android 6.0 or later to use the OpenGL ES 2.0 renderer.
    // Older devices tend to perform better with 1.0.
    final boolean useOpenGL20 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;

    View buttonPanel;
    View highScorePanel;
    View selectTableRow;
    ImageButton nextTableButton;
    ImageButton previousTableButton;
    Button startGameButton;
    Button resumeGameButton;
    Button endGameButton;
    Button aboutButton;
    Button preferencesButton;
    Button quitButton;
    Button showHighScoreButton;
    Button hideHighScoreButton;
    CheckBox unlimitedBallsToggle;
    ViewGroup highScoreListLayout;
    View noHighScoresTextView;
    final static int ACTIVITY_PREFERENCES = 1;

    Handler handler = new Handler(Looper.myLooper());

    IStringResolver stringLookupFn = (key, params) -> {
        int stringId = getResources().getIdentifier(key, "string", getPackageName());
        return getString(stringId, params);
    };
    Field field = new Field(System::currentTimeMillis, stringLookupFn, new VPSoundpool.Player());

    int numberOfLevels;
    int currentLevel = 1;
    List<Long> highScores;
    Long lastScore = 0L;
    boolean showingHighScores = false;
    static int MAX_NUM_HIGH_SCORES = 5;
    static String HIGHSCORES_PREFS_KEY = "highScores";
    static String LAST_SCORE_PREFS_KEY = "lastScore";
    static String OLD_HIGHSCORE_PREFS_KEY = "highScore";
    static String INITIAL_LEVEL_PREFS_KEY = "initialLevel";

    boolean useZoom = true;
    static final float ZOOM_FACTOR = 1.5f;

    final boolean supportsHapticFeedback = Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
    boolean useHapticFeedback;

    // Delay after ending a game, before a touch will start a new game.
    static final long END_GAME_DELAY_MS = 1000;
    Long endGameTime = System.currentTimeMillis() - END_GAME_DELAY_MS;

    FieldDriver fieldDriver = new FieldDriver();
    FieldViewManager fieldViewManager = new FieldViewManager();
    OrientationListener orientationListener;

    private static final String TAG = "BouncyActivity";

    /** Called when the activity is first created. */
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String arch = System.getProperty("os.arch");
        Log.i(TAG, "App started, os.arch: " + arch + ", API level: " + Build.VERSION.SDK_INT);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.getWindow().setNavigationBarColor(Color.BLACK);
        }

        this.numberOfLevels = FieldLayoutReader.getNumberOfLevels(this);
        this.currentLevel = getInitialLevel();
        resetFieldForCurrentLevel();

        canvasFieldView = findViewById(R.id.canvasFieldView);
        canvasFieldView.setManager(fieldViewManager);

        glFieldView = findViewById(R.id.glFieldView);
        if (useOpenGL20) {
            gl20Renderer = new GL20Renderer(glFieldView, (shaderPath) -> {
                try {
                    InputStream input = getAssets().open(shaderPath);
                    return IOUtils.utf8FromStream(input);
                }
                catch(IOException ex) {
                    throw new RuntimeException(ex);
                }
            });
            gl20Renderer.setManager(fieldViewManager);
        }
        else {
            gl10Renderer = new GL10Renderer(glFieldView);
            gl10Renderer.setManager(fieldViewManager);
        }

        fieldViewManager.setField(field);
        fieldViewManager.setStartGameAction(() -> doStartGame(null));

        scoreView = findViewById(R.id.scoreView);
        scoreView.setField(field);

        fieldDriver.setField(field);
        fieldDriver.setDrawFunction(fieldViewManager::draw);

        highScores = this.highScoresFromPreferencesForCurrentLevel();
        lastScore = this.lastScoreFromPreferencesForCurrentLevel();
        scoreView.setHighScores(highScores);

        buttonPanel = findViewById(R.id.buttonPanel);
        selectTableRow = findViewById(R.id.selectTableRow);
        highScorePanel = findViewById(R.id.highScorePanel);
        nextTableButton = findViewById(R.id.nextTableButton);
        previousTableButton = findViewById(R.id.previousTableButton);
        startGameButton = findViewById(R.id.startGameButton);
        resumeGameButton = findViewById(R.id.resumeGameButton);
        endGameButton = findViewById(R.id.endGameButton);
        aboutButton = findViewById(R.id.aboutButton);
        preferencesButton = findViewById(R.id.preferencesButton);
        quitButton = findViewById(R.id.quitButton);
        unlimitedBallsToggle = findViewById(R.id.unlimitedBallsToggle);
        showHighScoreButton = findViewById(R.id.highScoreButton);
        hideHighScoreButton = findViewById(R.id.hideHighScoreButton);
        highScoreListLayout = findViewById(R.id.highScoreListLayout);
        noHighScoresTextView = findViewById(R.id.noHighScoresTextView);
        pauseButton = findViewById(R.id.pauseIcon);

        // Ugly workaround that seems to be required when supporting keyboard navigation.
        // In main.xml, all buttons have `android:focusableInTouchMode` set to true.
        // If it's not, then they don't get focused even when using the dpad on a
        // Motorola Droid or plugging in a keyboard to a Pixel 3a. (Android documentation
        // says that the UI should automatically go in and out of touch mode, but that
        // seems to not happen). With that setting, the default touch behavior on a
        // non-focused button is to focus it but not click it. We want a click in that case,
        // so we have to set a touch listener and call `performClick` on a ACTION_UP event
        // (after checking that the event was within the button bounds). This is likely
        // fragile but seems to be working ok.
        List<View> allButtons = Arrays.asList(
                nextTableButton, previousTableButton, startGameButton, resumeGameButton, endGameButton,
                aboutButton, preferencesButton, quitButton, unlimitedBallsToggle, showHighScoreButton, hideHighScoreButton);
        for (View button : allButtons) {
            button.setOnTouchListener((view, motionEvent) -> {
                // Log.i(TAG, "Button motion event: " + motionEvent);
                // Log.i(TAG, "View: " + view);
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    Rect r = new Rect();
                    view.getLocalVisibleRect(r);
                    // Log.i(TAG, "Button rect: " + r);
                    // Log.i(TAG, "Event location: " + motionEvent.getX() + " " + motionEvent.getY());
                    if (r.contains((int)motionEvent.getX(), (int)motionEvent.getY())) {
                        // Log.i(TAG, "Button click, focused: " + view.hasFocus());
                        // This calls the button's click action, but for some reason doesn't
                        // do the ripple animation if the button was previously focused.
                        view.requestFocus();
                        view.performClick();
                        return true;
                    }
                }
                return false;
            });
        }

        // TODO: allow field configuration to specify whether tilting is allowed
        /*
        orientationListener = new OrientationListener(this, SensorManager.SENSOR_DELAY_GAME,
        		new OrientationListener.Delegate() {
        	public void receivedOrientationValues(float azimuth, float pitch, float roll) {
            	field.receivedOrientationValues(azimuth, pitch, roll);
        	}
        });
         */
        updateFromPreferences();

        // Initialize audio, loading resources in a separate thread.
        VPSoundpool.initSounds(this);
        (new Thread(VPSoundpool::loadSounds)).start();
        VPSoundpool.hapticFn = () -> {
            if (supportsHapticFeedback && useHapticFeedback) {
                scoreView.performHapticFeedback(
                        HapticFeedbackConstants.KEYBOARD_TAP,
                        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
            }
        };
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    void enterFullscreenMode() {
        // https://stackoverflow.com/questions/62643517/immersive-fullscreen-on-android-11
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController insetsController = getWindow().getInsetsController();
            if (insetsController != null) {
                insetsController.hide(WindowInsets.Type.statusBars());
                insetsController.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    |  View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    |  View.SYSTEM_UI_FLAG_FULLSCREEN
                    |  View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            final View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(flags);
            decorView.setOnSystemUiVisibilityChangeListener((visibility) -> {
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    decorView.setSystemUiVisibility(flags);
                }
            });
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            final View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        }
    }

    @Override public void onResume() {
        super.onResume();

        // Go to full screen mode here; if we only do it in onCreate then the UI can get stuck
        // with the navigation bar on top of the field.
        enterFullscreenMode();
        // Reset frame rate since app or system settings that affect performance could have changed.
        fieldDriver.resetFrameRate();
        updateButtons();
    }

    @Override public void onPause() {
        pauseGame();
        super.onPause();
    }

    @Override public void onWindowFocusChanged(boolean hasWindowFocus) {
        // This handles the main activity pausing and resuming.
        super.onWindowFocusChanged(hasWindowFocus);
        if (!hasWindowFocus) {
            pauseGame();
        }
        else {
            // If game is in progress, return to the paused menu rather than immediately resuming.
            if (field.getGameState().isGameInProgress()) {
                if (glFieldView != null) {
                    // This may result in multiple calls to onResume, but that seems to be ok.
                    glFieldView.onResume();
                }
                fieldViewManager.draw();
                updateButtons();
            }
            else {
                unpauseGame();
            }
        }
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        // When a game is in progress, pause rather than exit when the back button is pressed.
        // This prevents accidentally quitting the game. Also pause (but don't quit) on "P".
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_P) {
            if (field.getGameState().isGameInProgress() && !field.getGameState().isPaused()) {
                pauseGame();
                return true;
            }
        }
        // When showing the main menu, switch tables with the flipper buttons.
        if (!field.getGameState().isGameInProgress() && buttonPanel.getVisibility() == View.VISIBLE) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                doPreviousTable(null);
                startGameButton.requestFocus();
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                doNextTable(null);
                startGameButton.requestFocus();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }


    public void pauseGame() {
        VPSoundpool.pauseMusic();
        GameState state = field.getGameState();
        if (state.isPaused()) return;
        state.setPaused(true);

        if (orientationListener != null) orientationListener.stop();
        fieldDriver.stop();
        if (glFieldView != null) glFieldView.onPause();
        showingHighScores = false;

        updateButtons();
    }

    public void unpauseGame() {
        if (!field.getGameState().isPaused()) return;
        field.getGameState().setPaused(false);

        handler.postDelayed(this::tick, 75);
        if (orientationListener != null) orientationListener.start();

        fieldDriver.start();
        if (glFieldView != null) glFieldView.onResume();
        showingHighScores = false;

        updateButtons();
    }

    void updateButtons() {
        GameState state = field.getGameState();
        boolean paused = state.isPaused();
        boolean gameInProgress = state.isGameInProgress();

        if (gameInProgress && !paused) {
            // Game is active, no menus visible, show pause "button".
            buttonPanel.setVisibility(View.GONE);
            highScorePanel.setVisibility(View.GONE);
            pauseButton.setVisibility(View.VISIBLE);
        }
        else if (showingHighScores) {
            // High scores are visible, hide main menu.
            buttonPanel.setVisibility(View.GONE);
            highScorePanel.setVisibility(View.VISIBLE);
            hideHighScoreButton.requestFocus();
            pauseButton.setVisibility(View.GONE);
        }
        else if (gameInProgress) {
            // Menu when game is in progress, show resume/end buttons, hide table picker.
            buttonPanel.setVisibility(View.VISIBLE);
            highScorePanel.setVisibility(View.GONE);
            selectTableRow.setVisibility(View.GONE);
            unlimitedBallsToggle.setVisibility(View.GONE);
            startGameButton.setVisibility(View.GONE);
            resumeGameButton.setVisibility(View.VISIBLE);
            endGameButton.setVisibility(View.VISIBLE);
            resumeGameButton.requestFocus();
            pauseButton.setVisibility(View.GONE);
        } else {
            // Menu when game is not in progress, show table picker.
            buttonPanel.setVisibility(View.VISIBLE);
            highScorePanel.setVisibility(View.GONE);
            selectTableRow.setVisibility(View.VISIBLE);
            unlimitedBallsToggle.setVisibility(View.VISIBLE);
            startGameButton.setVisibility(View.VISIBLE);
            resumeGameButton.setVisibility(View.GONE);
            endGameButton.setVisibility(View.GONE);
            startGameButton.requestFocus();
            pauseButton.setVisibility(View.GONE);
        }
    }

    @Override public void onDestroy() {
        VPSoundpool.cleanup();
        super.onDestroy();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        switch (requestCode) {
            case ACTIVITY_PREFERENCES:
                updateFromPreferences();
                break;
        }
    }

    void gotoPreferences() {
        Intent settingsActivity = new Intent(getBaseContext(), BouncyPreferences.class);
        startActivityForResult(settingsActivity, ACTIVITY_PREFERENCES);
    }

    void gotoAbout() {
        AboutActivity.startForLevel(this, this.currentLevel);
    }

    // Update settings from preferences, called at launch and when preferences activity finishes.
    void updateFromPreferences() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        fieldViewManager.setIndependentFlippers(prefs.getBoolean("independentFlippers", true));
        scoreView.setShowFPS(prefs.getBoolean("showFPS", false));

        // If switching line width or OpenGL/Canvas, reset frame rate manager because maximum
        // achievable frame rate may change.
        int lineWidth = 0;
        try {
            lineWidth = Integer.parseInt(prefs.getString("lineWidth", "0"));
        }
        catch (NumberFormatException ignored) {}
        if (lineWidth != fieldViewManager.getCustomLineWidth()) {
            fieldViewManager.setCustomLineWidth(lineWidth);
        }

        boolean useOpenGL = prefs.getBoolean("useOpenGL", true);
        if (useOpenGL) {
            if (glFieldView.getVisibility() != View.VISIBLE) {
                canvasFieldView.setVisibility(View.GONE);
                glFieldView.setVisibility(View.VISIBLE);
                fieldViewManager.setFieldRenderer(useOpenGL20 ? gl20Renderer : gl10Renderer);
            }
        }
        else {
            if (canvasFieldView.getVisibility() != View.VISIBLE) {
                glFieldView.setVisibility(View.GONE);
                canvasFieldView.setVisibility(View.VISIBLE);
                fieldViewManager.setFieldRenderer(canvasFieldView);
            }
        }

        useZoom = prefs.getBoolean("zoom", true);
        fieldViewManager.setZoom(useZoom ? ZOOM_FACTOR : 1.0f);

        VPSoundpool.setSoundEnabled(prefs.getBoolean("sound", true));
        VPSoundpool.setMusicEnabled(prefs.getBoolean("music", true));
        useHapticFeedback = prefs.getBoolean("haptic", false);
    }

    // Called every 100 milliseconds while app is visible, to update score view and high score.
    void tick() {
        scoreView.invalidate();
        scoreView.setFPS(fieldDriver.getAverageFPS());
        scoreView.setDebugMessage(field.getDebugMessage());
        updateHighScoreAndButtonPanel();
        handler.postDelayed(this::tick, 100);
    }

    /**
     * If the score of the current or previous game is greater than the previous high score,
     * update high score in preferences and ScoreView. Also show button panel if game has ended.
     */
    void updateHighScoreAndButtonPanel() {
        // We only need to check once when the game is over, before the button panel is visible.
        if (buttonPanel.getVisibility() == View.VISIBLE ||
            highScorePanel.getVisibility() == View.VISIBLE) return;
        synchronized (field) {
            GameState state = field.getGameState();
            if (!field.getGameState().isGameInProgress()) {
                // game just ended, show button panel and set end game timestamp
                this.endGameTime = System.currentTimeMillis();
                updateButtons();

                // No high scores for unlimited balls.
                if (!state.hasUnlimitedBalls()) {
                    long score = field.getGameState().getScore();
                    // Add to high scores list if the score beats the lowest existing high score,
                    // or if all the high score slots aren't taken.
                    if (score > highScores.get(this.highScores.size() - 1) ||
                            highScores.size() < MAX_NUM_HIGH_SCORES) {
                        this.updateHighScoreForCurrentLevel(score);
                    }
                }
            }
        }
    }

    // Store separate high scores for each field, using unique suffix in prefs key.
    String highScorePrefsKeyForLevel(int theLevel) {
        return HIGHSCORES_PREFS_KEY + "." + theLevel;
    }

    // Store separate high scores for each field, using unique suffix in prefs key.
    String lastScorePrefsKeyForLevel(int theLevel) {
        return LAST_SCORE_PREFS_KEY + "." + theLevel;
    }

    /**
     * Returns a list of the high score stored in SharedPreferences. Always returns a nonempty
     * list, which will be [0] if no high scores have been stored.
     */
    List<Long> highScoresFromPreferences(int theLevel) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String scoresAsString = prefs.getString(highScorePrefsKeyForLevel(theLevel), "");
        if (scoresAsString.length() > 0) {
            try {
                String[] fields = scoresAsString.split(",");
                List<Long> scores = new ArrayList<>();
                for (String f : fields) {
                    scores.add(Long.valueOf(f));
                }
                return scores;
            }
            catch (NumberFormatException ex) {
                return Collections.singletonList(0L);
            }
        }
        else {
            // Check pre-1.5 single high score.
            long oldPrefsScore = prefs.getLong(OLD_HIGHSCORE_PREFS_KEY + "." + currentLevel, 0);
            return Collections.singletonList(oldPrefsScore);
        }
    }

    Long lastScoreFromPreferences(int theLevel) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        return prefs.getLong(lastScorePrefsKeyForLevel(theLevel), 0L);
    }

    void writeHighScoresToPreferences(int level, List<Long> scores, long lastScore) {
        StringBuilder scoresAsString = new StringBuilder();
        scoresAsString.append(scores.get(0));
        for (int i = 1; i < scores.size(); i++) {
            scoresAsString.append(",").append(scores.get(i));
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(highScorePrefsKeyForLevel(level), scoresAsString.toString());
        editor.putLong(lastScorePrefsKeyForLevel(level), lastScore);
        editor.commit();
    }

    List<Long> highScoresFromPreferencesForCurrentLevel() {
        return highScoresFromPreferences(currentLevel);
    }

    Long lastScoreFromPreferencesForCurrentLevel() {
        return lastScoreFromPreferences(currentLevel);
    }

    /** Updates the high score in the ScoreView display, and persists it to SharedPreferences. */
    void updateHighScore(int theLevel, long score) {
        List<Long> newHighScores = new ArrayList<>(this.highScores);
        newHighScores.add(score);
        Collections.sort(newHighScores);
        Collections.reverse(newHighScores);
        if (newHighScores.size() > MAX_NUM_HIGH_SCORES) {
            newHighScores = newHighScores.subList(0, MAX_NUM_HIGH_SCORES);
        }
        this.highScores = newHighScores;
        this.lastScore = score;
        writeHighScoresToPreferences(theLevel, this.highScores, this.lastScore);
        scoreView.setHighScores(this.highScores);
    }

    void updateHighScoreForCurrentLevel(long score) {
        updateHighScore(currentLevel, score);
    }

    int getInitialLevel() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        int startLevel = prefs.getInt(INITIAL_LEVEL_PREFS_KEY, 1);
        if (startLevel < 1 || startLevel > numberOfLevels) startLevel = 1;
        return startLevel;
    }

    void setInitialLevel(int level) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(INITIAL_LEVEL_PREFS_KEY, level);
        editor.commit();
    }

    // Button action methods defined by android:onClick values in main.xml.
    public void doStartGame(View view) {
        if (field.getGameState().isPaused()) {
            unpauseGame();
            return;
        }
        // Avoids accidental starts due to touches just after game ends.
        if (endGameTime == null || (System.currentTimeMillis() < endGameTime + END_GAME_DELAY_MS)) {
            return;
        }
        if (!field.getGameState().isGameInProgress()) {
            // https://github.com/dozingcat/Vector-Pinball/issues/91
            // These actions need to be synchronized so that we don't try to
            // start the game while the FieldDriver thread is updating the
            // Box2d world. It's not clear what should be synchronized and what
            // shouldn't; for example pauseGame() above should not be
            // synchronized because that can deadlock the FieldDriver thread.
            // All of this concurrency is badly in need of refactoring.
            synchronized (field) {
                buttonPanel.setVisibility(View.GONE);
                highScorePanel.setVisibility(View.GONE);
                resetFieldForCurrentLevel();

                if (unlimitedBallsToggle.isChecked()) {
                    field.startGameWithUnlimitedBalls();
                }
                else {
                    field.startGame();
                }
            }
            VPSoundpool.playStart();
            endGameTime = null;
            updateButtons();
        }
    }

    public void doEndGame(View view) {
        // Game might be paused, if manually ended from button.
        unpauseGame();
        synchronized (field) {
            field.endGame();
        }
    }

    public void doPreferences(View view) {
        gotoPreferences();
    }

    public void doQuit(View view) {
        this.finish();
    }

    public void doAbout(View view) {
        gotoAbout();
    }

    public void scoreViewClicked(View view) {
        // Start a new game or pause a running game, but don't resume a paused game.
        GameState state = field.getGameState();
        if (state.isGameInProgress() && !state.isPaused()) {
            pauseGame();
        }
        else if (!state.isGameInProgress()) {
            doStartGame(view);
        }
    }

    void switchToTable(int tableNum) {
        this.currentLevel = tableNum;
        synchronized (field) {
            resetFieldForCurrentLevel();
        }
        this.setInitialLevel(currentLevel);
        this.highScores = this.highScoresFromPreferencesForCurrentLevel();
        this.lastScore = this.lastScoreFromPreferencesForCurrentLevel();
        scoreView.setHighScores(highScores);
        // Performance can be different on different tables.
        fieldDriver.resetFrameRate();
    }

    public void doSwitchTable(View view) {
        doNextTable(view);
    }

    public void doNextTable(View view) {
        int nextTableNum = (currentLevel == numberOfLevels) ? 1 : currentLevel + 1;
        switchToTable(nextTableNum);
    }

    public void doPreviousTable(View view) {
        int prevTableNum = (currentLevel == 1) ? numberOfLevels : currentLevel - 1;
        switchToTable(prevTableNum);
    }

    void resetFieldForCurrentLevel() {
        field.resetForLayoutMap(FieldLayoutReader.layoutMapForLevel(this, currentLevel));
    }

    public void showHighScore(View view) {
        this.fillHighScoreAdapter();
        showingHighScores = true;
        updateButtons();
    }

    public void hideHighScore(View view) {
        showingHighScores = false;
        updateButtons();
    }

    private void fillHighScoreAdapter() {
        LinearLayout.LayoutParams params = null;
        this.highScoreListLayout.removeAllViews();
        for (int index = 0; index < this.highScores.size(); index++) {
            long score = this.highScores.get(index);
            if (score > 0) {
                if (params == null) {
                    params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    params.bottomMargin = (int)(16 * getResources().getDisplayMetrics().scaledDensity);
                }
                TextView scoreItem = this.createHighScoreTextView(ScoreView.SCORE_FORMAT.format(score), params);
                this.highScoreListLayout.addView(scoreItem);
            }
        }

        if(this.lastScore > 0 && params != null){
            String lastScoreText = this.getString(R.string.last_score_message, ScoreView.SCORE_FORMAT.format(this.lastScore));
            TextView lastScoreItem = this.createHighScoreTextView(lastScoreText, params);
            this.highScoreListLayout.addView(lastScoreItem);
        }

        this.noHighScoresTextView.setVisibility(
                this.highScoreListLayout.getChildCount() == 0 ? View.VISIBLE : View.GONE);
    }

    private TextView createHighScoreTextView(String scoreText, LinearLayout.LayoutParams params){
        TextView scoreItem = new TextView(this);
        scoreItem.setLayoutParams(params);
        scoreItem.setText(scoreText);
        scoreItem.setTextSize(22);
        scoreItem.setTextColor(Color.argb(255, 240, 240, 240));
        scoreItem.setGravity(Gravity.END);
        return scoreItem;
    }
}
