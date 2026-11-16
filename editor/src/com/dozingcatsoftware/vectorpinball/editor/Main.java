package com.dozingcatsoftware.vectorpinball.editor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Entry point for the editor. Starts with a single window containing the starter table, and
 * keeps track of the open windows. The app exits when the last window is closed.
 */
public class Main extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    private final List<EditorWindow> windows = new ArrayList<>();

    @Override public void start(Stage primaryStage) {
        EditorWindow window = new EditorWindow(this, primaryStage);
        windows.add(window);
        window.show(null);
        window.loadStarterField();
    }

    /** Creates and shows a new empty window. The caller should load a table into it. */
    EditorWindow createWindow(EditorWindow parent) {
        EditorWindow window = new EditorWindow(this, new Stage());
        windows.add(window);
        window.show(parent);
        return window;
    }

    void windowClosed(EditorWindow window) {
        windows.remove(window);
    }

    /** Stops any game running in a window other than `window`. */
    void stopGamesInOtherWindows(EditorWindow window) {
        for (EditorWindow other : windows) {
            if (other != window && other.isGameRunning()) {
                other.stopGame();
            }
        }
    }

    /** Returns the window editing the file at `path`, or null if there isn't one. */
    EditorWindow windowForPath(Path path) {
        for (EditorWindow window : windows) {
            if (path.equals(window.savedFilePath)) {
                return window;
            }
        }
        return null;
    }
}
