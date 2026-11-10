package com.dozingcatsoftware.vectorpinball.editor.elements;


public interface PropertyContainer {

    boolean hasProperty(String propertyName);

    Object getProperty(String propertyName);

    void setProperty(String propertyName, Object value);

    void removeProperty(String propertyName);

    default void setOrRemoveProperty(String propertyName, Object value) {
        if (value != null) {
            setProperty(propertyName, value);
        }
        else {
            removeProperty(propertyName);
        }
    }
}
