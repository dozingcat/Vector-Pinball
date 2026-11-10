package com.dozingcatsoftware.vectorpinball.editor;

import static com.dozingcatsoftware.vectorpinball.util.Localization.localizedString;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Function;

import com.dozingcatsoftware.vectorpinball.editor.elements.EditableField;
import com.dozingcatsoftware.vectorpinball.editor.elements.EditableFieldElement;
import com.dozingcatsoftware.vectorpinball.groovy.GroovyFieldDelegateBuilder;
import com.dozingcatsoftware.vectorpinball.model.AudioPlayer;
import com.dozingcatsoftware.vectorpinball.model.Field;
import com.dozingcatsoftware.vectorpinball.model.FieldDriver;
import com.dozingcatsoftware.vectorpinball.model.GameMessage;
import com.dozingcatsoftware.vectorpinball.model.IStringResolver;
import com.dozingcatsoftware.vectorpinball.util.CollectionUtils;
import com.dozingcatsoftware.vectorpinball.util.JSONUtils;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class Main extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    enum EditorState {
        EDITING,
        SAMPLE_GAME,
    }

    static String WINDOW_TITLE_PREFIX = localizedString("Vector Pinball: ");

    static int WINDOW_WIDTH = 1000;
    static int WINDOW_HEIGHT = 860;
    static int BASE_CANVAS_WIDTH = 560;
    static int BASE_CANVAS_HEIGHT = 810;

    static int TOOLS_COLUMN_WIDTH = 360;
    static int SCRIPT_COLUMN_WIDTH = 550;

    Stage mainStage;
    VBox fieldBox;
    ScrollPane fieldScroller;
    Canvas fieldCanvas;
    ElementInspectorView inspectorView;
    ScoreView scoreView;
    Timer scoreViewTimer;

    ColumnConstraints scriptColumnConstraints;
    ScriptEditorView scriptView;
    Button showScriptButton;

    Field field;
    EditableField editableField;
    FxCanvasRenderer renderer;
    UndoStack undoStack;

    FieldDriver fieldDriver;
    EditorState editorState = EditorState.EDITING;
    boolean isResizingProgrammatically = false;

    Path savedFilePath;
    Map<String, Object> lastSavedFieldMap;

    final int numTemplateFields = 9;

    @Override public void start(Stage primaryStage) {
        this.mainStage = primaryStage;

        editableField = new EditableField();
        editableField.setElementChangedCallback(this::handleElementChangeFromField);
        editableField.setSelectionChangedCallback(this::handleSelectionChange);

        undoStack = new UndoStack();
        undoStack.setEditableField(editableField);

        renderer = new FxCanvasRenderer();
        renderer.setEditableField(editableField);
        renderer.setUndoStack(undoStack);

        inspectorView = new ElementInspectorView();
        inspectorView.setChangeCallback(this::handleElementChangeFromInspector);
        inspectorView.setEditableField(editableField);
        inspectorView.setUndoStack(undoStack);

        GridPane root = new GridPane();

        // Put the menu bar at the top of the window using grid constraints. For platforms that
        // support it (Mac at least), setUseSystemMenuBar(true) will use a native menu bar and
        // not show it in the window.
        MenuBar menuBar = buildMenuBar();
        menuBar.setUseSystemMenuBar(true);
        GridPane.setConstraints(menuBar, 0, 0, 3, 1);

        Insets leftColumnInsets = new Insets(20, 0, 20, 20);

        ColumnConstraints toolsCol = new ColumnConstraints(TOOLS_COLUMN_WIDTH);
        scriptColumnConstraints = new ColumnConstraints(0);
        scriptColumnConstraints.setHgrow(Priority.ALWAYS);
        ColumnConstraints fieldCol = new ColumnConstraints(0, 700, Double.MAX_VALUE);
        fieldCol.setHgrow(Priority.ALWAYS);
        root.getColumnConstraints().addAll(toolsCol, scriptColumnConstraints, fieldCol);

        RowConstraints row1 = new RowConstraints();
        RowConstraints row2 = new RowConstraints();
        RowConstraints row3 = new RowConstraints();
        row3.setVgrow(Priority.ALWAYS);
        root.getRowConstraints().addAll(row1, row2, row3);

        VBox topLeft = new VBox(5);
        topLeft.setPadding(leftColumnInsets);
        topLeft.getChildren().add(new ElementPaletteView(this::createElement));

        Region spacer = new Region();
        spacer.setMinHeight(15);
        Label simLabel = new Label(localizedString("Simulation"));
        simLabel.setFont(new Font(16));
        HBox simButtonRow = new HBox(5);
        Button startGameButton = new Button(localizedString("Start Game"));
        startGameButton.setOnAction((event) -> startGame());
        Button endGameButton = new Button(localizedString("Stop Game"));
        endGameButton.setOnAction((event) -> stopGame());
        this.showScriptButton = new Button(localizedString("Show Script"));
        showScriptButton.setOnAction((event) -> showScriptView());
        simButtonRow.getChildren().addAll(startGameButton, endGameButton, showScriptButton);
        topLeft.getChildren().addAll(spacer, simLabel, simButtonRow);

        topLeft.setBackground(new Background(new BackgroundFill(Color.rgb(240, 240, 240), null, null)));
        GridPane.setConstraints(topLeft, 0, 1);

        ScrollPane inspectorScroller = new ScrollPane();
        inspectorScroller.setContent(inspectorView);
        inspectorScroller.setStyle("-fx-background: #bdf;");
        inspectorScroller.setPadding(leftColumnInsets);
        GridPane.setConstraints(inspectorScroller, 0, 2);

        scriptView = new ScriptEditorView();
        scriptView.setEditableField(editableField);
        scriptView.setChangeHandler(this::handleScriptChange);
        GridPane.setConstraints(scriptView, 1, 1, 1, 2);

        fieldBox = new VBox();
        scoreView = new ScoreView();

        fieldScroller = new ScrollPane();
        fieldScroller.setStyle("-fx-background: #222;");
        fieldScroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        fieldScroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(fieldScroller, Priority.ALWAYS);

        createCanvas(BASE_CANVAS_WIDTH, BASE_CANVAS_HEIGHT);

        fieldBox.getChildren().addAll(fieldScroller);

        // Listen for container height changes to resize canvas
        fieldBox.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (!isResizingProgrammatically) {
                handleContainerResize(newVal.doubleValue());
            }
        });

        GridPane.setConstraints(fieldBox, 2, 1, 1, 2);

        root.getChildren().addAll(menuBar, topLeft, inspectorScroller, scriptView, fieldBox);

        primaryStage.setScene(new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT));
        primaryStage.setMinWidth(640);
        primaryStage.setMinHeight(500);
        primaryStage.setOnCloseRequest((event) -> {
            if (confirmCloseCurrentField()) {
                stopGame();  // Stops game thread if needed so that app will terminate.
            }
            else {
                event.consume();  // Prevents exit.
            }
        });
        primaryStage.show();

        // Set initial canvas size based on available space after layout is complete
        Platform.runLater(() -> handleContainerResize(fieldBox.getHeight()));

        loadStarterField();
    }

    void showScoreView() {
        fieldBox.getChildren().add(0, scoreView);
        if (scoreViewTimer == null) {
            scoreViewTimer = new Timer(true); // Set daemon to not stop quitting the app.
            scoreViewTimer.schedule(new TimerTask() {
                @Override public void run() {
                    updateScoreView();
                }
            }, 0, 100);
        }
        // Recalculate canvas size with reduced height for score view
        Platform.runLater(() -> handleContainerResize(fieldBox.getHeight()));
    }

    void hideScoreView() {
        if (scoreViewTimer != null) {
            scoreViewTimer.cancel();
            scoreViewTimer = null;
        }
        fieldBox.getChildren().remove(scoreView);
        // Recalculate canvas size with full height available
        Platform.runLater(() -> handleContainerResize(fieldBox.getHeight()));
    }

    static NumberFormat SCORE_FORMAT = NumberFormat.getInstance();

    void updateScoreView() {
        Field f = this.field;
        if (f == null) return;
        GameMessage gameMessage = f.getGameMessage();
        String msg = (gameMessage != null) ?
                gameMessage.text :
                SCORE_FORMAT.format(f.getGameState().getScore());
        double multiplier = f.getScoreMultiplier();
        int ballNumber = f.getGameState().getBallNumber();
        Platform.runLater(() -> {
            scoreView.setMessage(msg);
            scoreView.setMultiplier(multiplier);
            scoreView.setBallNumber(ballNumber);
        });
    }

    void showScriptView() {
        scriptColumnConstraints.setPrefWidth(SCRIPT_COLUMN_WIDTH);
        showScriptButton.setText(localizedString("Hide Script"));
        showScriptButton.setOnAction((event) -> hideScriptView());
        mainStage.setWidth(mainStage.getWidth() + SCRIPT_COLUMN_WIDTH);
    }

    void hideScriptView() {
        scriptColumnConstraints.setPrefWidth(0);
        showScriptButton.setText(localizedString("Show Script"));
        showScriptButton.setOnAction((event) -> showScriptView());
        mainStage.setWidth(mainStage.getWidth() - SCRIPT_COLUMN_WIDTH);
    }

    MenuItem createMenuItem(String label, String shortcutChar, Runnable onAction) {
        MenuItem item = new MenuItem(label);
        if (shortcutChar != null) {
            item.setAccelerator(new KeyCharacterCombination(shortcutChar, KeyCombination.SHORTCUT_DOWN));
        }
        if (onAction != null) {
            item.setOnAction((event) -> onAction.run());
        }
        return item;
    }

    MenuBar buildMenuBar() {
        Menu fileMenu = new Menu("File");

        Menu newFromTemplateMenu = new Menu("New From Template");
        for (int i = 1; i <= numTemplateFields; i++) {
            int ii = i;
            newFromTemplateMenu.getItems().add(
                    createMenuItem("Table " + ii, null, () -> loadBuiltInField(ii))
            );
        }
        MenuItem saveAsItem = createMenuItem("Save As...", null, this::saveNewFile);
        saveAsItem.setAccelerator(new KeyCharacterCombination(
            "S", KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));
        fileMenu.getItems().addAll(
                createMenuItem("New Table", "N", () -> loadStarterField()),
                newFromTemplateMenu,
                new SeparatorMenuItem(),
                createMenuItem("Open...", "O", this::openFile),
                createMenuItem("Save", "S", this::saveFile),
                saveAsItem
        );

        Menu editMenu = new Menu("Edit");
        // TODO: Selectively enable menu items based on editor state.
        MenuItem duplicateItem = createMenuItem("Duplicate", "D", this::duplicateSelectedElement);
        MenuItem undoItem = createMenuItem("Undo", "Z", this::undoEdit);
        MenuItem redoItem = createMenuItem("Redo", null, this::redoEdit);
        redoItem.setAccelerator(new KeyCharacterCombination(
                "Z", KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));
        editMenu.getItems().addAll(duplicateItem, undoItem, redoItem);

        Menu viewMenu = new Menu("View");
        viewMenu.getItems().addAll(
                createMenuItem("Zoom In", "+", this::zoomIn),
                createMenuItem("Zoom Out", "-", this::zoomOut),
                createMenuItem("Default Zoom", "0", this::zoomDefault));

        Menu helpMenu = new Menu("Help");
        helpMenu.getItems().addAll(
                createMenuItem("About...", null, this::showAboutDialog));

        MenuBar mbar = new MenuBar();
        mbar.getMenus().addAll(fileMenu, editMenu, viewMenu, helpMenu);
        return mbar;
    }

    void createCanvas(double width, double height) {
        fieldCanvas = new Canvas(width, height);
        // Assign focus so that canvas will receive key events.
        fieldCanvas.addEventFilter(MouseEvent.ANY, (e) -> fieldCanvas.requestFocus());
        fieldScroller.setContent(fieldCanvas);
        fieldCanvas.setOnMousePressed(this::handleCanvasMousePressed);
        fieldCanvas.setOnMouseReleased(this::handleCanvasMouseReleased);
        fieldCanvas.setOnMouseDragged(this::handleCanvasMouseDragged);
        fieldCanvas.setOnKeyPressed(this::handleCanvasKeyPressed);
        fieldCanvas.setOnKeyReleased(this::handleCanvasKeyReleased);
    }

    void zoomIn() {
        renderer.zoomIn();
        resizeWindowForPresetZoom();
    }

    void zoomOut() {
        renderer.zoomOut();
        resizeWindowForPresetZoom();
    }

    void zoomDefault() {
        renderer.zoomDefault();
        resizeWindowForPresetZoom();
    }

    private void resizeWindowForPresetZoom() {
        isResizingProgrammatically = true;
        try {
            double scale = renderer.getScale();
            double canvasWidth = renderer.getCanvasWidthForScale(scale);
            double canvasHeight = renderer.getCanvasHeightForScale(scale);

            // Calculate required window size accounting for other UI elements
            double scoreViewHeight = fieldBox.getChildren().contains(scoreView) ? scoreView.getHeight() : 0;
            double requiredFieldBoxHeight = canvasHeight + scoreViewHeight;

            // Get current sizes of other columns
            double toolsWidth = TOOLS_COLUMN_WIDTH;
            double scriptWidth = scriptColumnConstraints.getPrefWidth();

            // Estimate window chrome and padding
            double windowChromeWidth = 40;
            double windowChromeHeight = 80; // menu bar + title bar

            double newWindowWidth = toolsWidth + scriptWidth + canvasWidth + windowChromeWidth;
            double newWindowHeight = requiredFieldBoxHeight + windowChromeHeight;

            mainStage.setWidth(Math.max(newWindowWidth, mainStage.getMinWidth()));
            mainStage.setHeight(Math.max(newWindowHeight, mainStage.getMinHeight()));

            createCanvas(canvasWidth, canvasHeight);
            renderer.setCanvas(fieldCanvas);
            renderer.doDraw();
        } finally {
            Platform.runLater(() -> isResizingProgrammatically = false);
        }
    }

    void handleContainerResize(double containerHeight) {
        if (containerHeight <= 0) return;

        // Calculate available height for canvas (subtract score view if visible)
        double scoreViewHeight = fieldBox.getChildren().contains(scoreView) ? scoreView.getHeight() : 0;
        double availableHeight = containerHeight - scoreViewHeight;

        if (availableHeight <= 10) return; // Minimum viable height

        double newScale = renderer.getScaleForCanvasHeight(availableHeight);
        renderer.setManualScale(newScale);

        double newCanvasWidth = renderer.getCanvasWidthForScale(newScale);
        double newCanvasHeight = availableHeight;

        createCanvas(newCanvasWidth, newCanvasHeight);
        renderer.setCanvas(fieldCanvas);
        renderer.doDraw();
    }

    void loadFieldMap(Map<String, Object> map) {
        displayForEditing(map);
        undoStack.clearStack();
        undoStack.pushSnapshot();
        savedFilePath = null;
        // Keep snapshot so we know if it's changed.
        lastSavedFieldMap = editableField.getPropertyMapSnapshot();
    }

    void loadBuiltInField(int fieldNum) {
        if (!confirmCloseCurrentField()) return;

        JarFileFieldReader fieldReader = new JarFileFieldReader();
        loadFieldMap(fieldReader.layoutMapForBuiltInField(fieldNum));
        // TODO: localize
        mainStage.setTitle(WINDOW_TITLE_PREFIX + "Table Template " + fieldNum);
    }

    void loadStarterField() {
        if (!confirmCloseCurrentField()) return;

        JarFileFieldReader fieldReader = new JarFileFieldReader();
        loadFieldMap(fieldReader.layoutMapForStarterField());
        // TODO: localize
        mainStage.setTitle(WINDOW_TITLE_PREFIX + "New Table");
    }

    void displayForEditing(Map<String, Object> fieldMap) {
        renderer.setCanvas(fieldCanvas);
        stopGame();
        editableField.initFromProperties(fieldMap);
        renderer.doDraw();
        inspectorView.updateInspectorValues();
        scriptView.updateScriptText();
        editorState = EditorState.EDITING;
    }

    IStringResolver stringResolver = new StringResolver();
    Function<Field, Field.Delegate> groovyDelegateFn = (field -> {
        String script = field.getScriptText();
        if (script != null && script.trim().length() > 0) {
            return GroovyFieldDelegateBuilder.createFromScript(script, getClass().getClassLoader());
        }
        return Field.createDelegateFromLayoutClass(field);
    });

    void startGame() {
        stopGame();
        field = new Field(System::currentTimeMillis, stringResolver, AudioPlayer.NoOpPlayer.getInstance());
        field.resetForLayoutMap(editableField.getPropertyMapSnapshot(), groovyDelegateFn);
        field.setBallTrailsEnabled(true);
        field.setScoreAnimationsEnabled(true);
        renderer.setField(field);
        showScoreView();

        fieldDriver = new FieldDriver(field, renderer::doDraw);
        fieldDriver.start();

        field.startGame();
        launchBallIfNeeded();

        editorState = EditorState.SAMPLE_GAME;
        fieldCanvas.requestFocus();

        // Start polling every second to detect lost ball?
    }

    void stopGame() {
        if (fieldDriver != null) {
            fieldDriver.stop();
        }
        fieldDriver = null;

        renderer.setEditableField(editableField);
        renderer.doDraw();
        editorState = EditorState.EDITING;
        hideScoreView();
    }

    void handleCanvasMousePressed(MouseEvent event) {
        switch (editorState) {
            case SAMPLE_GAME:
                launchBallIfNeeded();
                field.setAllFlippersEngaged(true);
                break;
            case EDITING:
                renderer.handleEditorMouseDown(event);
                break;
        }
    }

    void handleCanvasMouseReleased(MouseEvent event) {
        switch (editorState) {
            case SAMPLE_GAME:
                field.setAllFlippersEngaged(false);
                break;
            case EDITING:
                renderer.handleEditorMouseUp(event);
                break;
        }
    }

    void handleCanvasMouseDragged(MouseEvent event) {
        switch (editorState) {
            case EDITING:
                renderer.handleEditorMouseDrag(event);
                break;
            default:
                break;
        }
    }

    void handleCanvasKeyPressed(KeyEvent event) {
        KeyCode code = event.getCode();
        switch (editorState) {
            case SAMPLE_GAME:
                boolean isActionKey = updateFlippersFromKey(code, true);
                if (isActionKey) launchBallIfNeeded();
                break;
            case EDITING:
                if (KeyCode.DELETE.equals(code) || KeyCode.BACK_SPACE.equals(code)) {
                    // Possibly cheating, but the logic is already there.
                    inspectorView.deleteSelectedElements();
                }
                else if (KeyCode.UP.equals(code)) {
                    renderer.moveSelectionByPixels(0, -1);
                }
                else if (KeyCode.DOWN.equals(code)) {
                    renderer.moveSelectionByPixels(0, 1);
                }
                else if (KeyCode.LEFT.equals(code)) {
                    renderer.moveSelectionByPixels(-1, 0);
                }
                else if (KeyCode.RIGHT.equals(code)) {
                    renderer.moveSelectionByPixels(1, 0);
                }
                break;
        }
    }

    void handleCanvasKeyReleased(KeyEvent event) {
        switch (editorState) {
            case SAMPLE_GAME:
                updateFlippersFromKey(event.getCode(), false);
                break;
            case EDITING:
                KeyCode code = event.getCode();
                if (KeyCode.UP.equals(code) ||
                        KeyCode.DOWN.equals(code) ||
                        KeyCode.LEFT.equals(code) ||
                        KeyCode.RIGHT.equals(code)) {
                    undoStack.pushSnapshot();
                }
            default:
                break;
        }
    }

    private static EnumSet<KeyCode> LEFT_FLIPPER_KEYS = EnumSet.of(KeyCode.Z, KeyCode.LEFT);
    private static EnumSet<KeyCode> RIGHT_FLIPPER_KEYS = EnumSet.of(KeyCode.SLASH, KeyCode.RIGHT);
    private static EnumSet<KeyCode> ALL_FLIPPER_KEYS = EnumSet.of(KeyCode.SPACE, KeyCode.ENTER);

    private boolean updateFlippersFromKey(KeyCode code, boolean pressed) {
        if (LEFT_FLIPPER_KEYS.contains(code)) {
            field.setLeftFlippersEngaged(pressed);
            return true;
        }
        else if (RIGHT_FLIPPER_KEYS.contains(code)) {
            field.setRightFlippersEngaged(pressed);
            return true;
        }
        else if (ALL_FLIPPER_KEYS.contains(code)) {
            field.setAllFlippersEngaged(pressed);
            return true;
        }
        return false;
    }

    private void launchBallIfNeeded() {
        // synchronized so we don't modify the Box2d world while it's updating.
        synchronized (field) {
            if (!field.getGameState().isGameInProgress()) {
                startGame();
            }
            field.launchBallIfNeeded();
        }
    }

    void handleSelectionChange() {
        inspectorView.update();
    }

    void handleElementChangeFromField() {
        inspectorView.updateInspectorValues();
    }

    void handleElementChangeFromInspector() {
        renderer.doDraw();
        undoStack.pushSnapshot();
    }

    void handleScriptChange() {
        undoStack.pushSnapshot();
    }

    void createElement(Class<? extends EditableFieldElement> elementClass) {
        if (editableField!=null && fieldDriver==null) {
            EditableFieldElement newElement = editableField.addNewElement(elementClass);
            // Push to undo stack before selecting the new element, so that the previous
            // selection will be restored if we undo.
            undoStack.pushSnapshot();
            editableField.selectElement(newElement);
            renderer.doDraw();
        }
    }

    void duplicateSelectedElement() {
        if (editableField != null && fieldDriver == null) {
            Set<EditableFieldElement> selection = editableField.getSelectedElements();
            if (!selection.isEmpty()) {
                Set<EditableFieldElement> duplicates = new HashSet<>();
                for (EditableFieldElement orig : selection) {
                    EditableFieldElement dup = editableField.addNewElement(orig.getClass());
                    dup.initFromPropertyMap(CollectionUtils.mutableDeepCopyOfMap(orig.getPropertyMap()));
                    dup.translate(Point.fromXY(0.5, -0.5));
                    duplicates.add(dup);
                }
                undoStack.pushSnapshot();
                editableField.setSelectedElements(duplicates);
                renderer.doDraw();
            }
        }
    }

    void undoEdit() {
        if (undoStack.canUndo()) {
            undoStack.undo();
            renderer.doDraw();
            inspectorView.updateInspectorValues();
        }
    }

    void redoEdit() {
        if (undoStack.canRedo()) {
            undoStack.redo();
            renderer.doDraw();
            inspectorView.updateInspectorValues();
        }
    }

    void openFile() {
        if (!confirmCloseCurrentField()) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open Table");
        File selectedFile = chooser.showOpenDialog(mainStage);
        if (selectedFile != null) {
            Path openFilePath = selectedFile.toPath();
            // TODO: Check for implausibly large files.
            byte[] contents = null;
            Map<String, Object> map = null;
            try {
                contents = Files.readAllBytes(openFilePath);
            } catch (IOException ex) {
                ex.printStackTrace();
                return;
            }
            try {
                map = JSONUtils.mapFromJSONString(new String(contents, StandardCharsets.UTF_8));
            }
            catch (JSONUtils.ParsingException ex) {
                ex.printStackTrace();
                return;
            }
            loadFieldMap(map);
            savedFilePath = openFilePath;
            mainStage.setTitle(WINDOW_TITLE_PREFIX + savedFilePath.toString());
        }
    }

    void saveFile() {
        if (savedFilePath != null) {
            writeToSavedFilePath();
        }
        else {
            saveNewFile();
        }
    }

    void saveNewFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Table");
        File selectedFile = chooser.showSaveDialog(mainStage);
        if (selectedFile != null) {
            savedFilePath = selectedFile.toPath();
            writeToSavedFilePath();
            mainStage.setTitle("Vector Pinball: " + savedFilePath.toString());
        }
    }

    void writeToSavedFilePath() {
        Map<String, Object> fieldMap = editableField.getPropertyMapSnapshot();
        String fileText = JSONUtils.jsonStringFromObject(fieldMap);
        try {
            Files.write(savedFilePath, fileText.getBytes(StandardCharsets.UTF_8));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        lastSavedFieldMap = fieldMap;
    }

    /**
     * Called when the user has requested an action which may close a field that has unsaved
     * changes. Returns true if the action should proceed, false if not. This method may
     * save the current field.
     */
    boolean confirmCloseCurrentField() {
        // Continue if no previously saved field, or if current field has no changes.
        if (lastSavedFieldMap == null) return true;
        if (lastSavedFieldMap.equals(editableField.getPropertyMapSnapshot())) return true;

        String title = localizedString("Discard changes?");
        String header = localizedString("The current table has unsaved changes.");

        List<ButtonType> buttons = new ArrayList<>();
        buttons.add(new ButtonType(localizedString("Save"), ButtonBar.ButtonData.YES));
        buttons.add(new ButtonType(localizedString("Don't Save"), ButtonBar.ButtonData.NO));
        buttons.add(new ButtonType(localizedString("Cancel"), ButtonBar.ButtonData.CANCEL_CLOSE));

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.getButtonTypes().clear();
        alert.getButtonTypes().addAll(buttons);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == buttons.get(0)) {
                // Save, don't continue.
                saveFile();
                return false;
            }
            if (result.get() == buttons.get(1)) {
                // Continue without saving.
                return true;
            }
        }
        // Dialog canceled.
        return false;
    }

    void showAboutDialog() {
        String title = localizedString("About Vector Pinball Editor");
        String header = localizedString("Vector Pinball Editor 0.1");
        String message = localizedString(
                "© 2015 Brian Nenninger\n" +
                "More info: www.vectorpinball.com\n" +
                "Email: brian@vectorpinball.com");
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.showAndWait();
    }
}
