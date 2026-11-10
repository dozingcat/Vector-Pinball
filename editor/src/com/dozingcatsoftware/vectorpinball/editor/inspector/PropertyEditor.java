package com.dozingcatsoftware.vectorpinball.editor.inspector;

import javafx.scene.layout.Pane;

public abstract class PropertyEditor<T> {

    Runnable changeHandler;
    Pane container;

    public void setOnChange(Runnable handler) {
        this.changeHandler = handler;
    }

    public Pane getContainer() {
        return container;
    }

    abstract T getValue();

    abstract void updateFromValue(T value);

    void runChangeHandler() {
        if (changeHandler != null) {
            changeHandler.run();
        }
    }

    void setContainer(Pane container) {
        this.container = container;
    }

}
