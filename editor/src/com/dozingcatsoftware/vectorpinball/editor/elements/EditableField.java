package com.dozingcatsoftware.vectorpinball.editor.elements;

import java.util.*;

import com.dozingcatsoftware.vectorpinball.editor.IEditableFieldRenderer;
import com.dozingcatsoftware.vectorpinball.model.IFieldRenderer;
import com.dozingcatsoftware.vectorpinball.util.CollectionUtils;

/**
 * Contains the field elements and global field properties, and manages the selected items.
 **/
public class EditableField implements PropertyContainer {

    public static final String NAME_PROPERTY = "name";
    public static final String WIDTH_PROPERTY = "width";
    public static final String HEIGHT_PROPERTY = "height";
    public static final String TARGET_TIME_RATIO_PROPERTY = "targetTimeRatio";
    public static final String GRAVITY_PROPERTY = "gravity";
    public static final String NUM_BALLS_PROPERTY = "numballs";
    public static final String BALL_RADIUS_PROPERTY = "ballradius";
    public static final String BALL_COLOR_PROPERTY = "ballcolor";
    public static final String SECONDARY_BALL_COLOR_PROPERTY = "secondaryBallColor";
    public static final String LAUNCH_POSITION_PROPERTY = "launchPosition";
    public static final String LAUNCH_VELOCITY_PROPERTY = "launchVelocity";
    public static final String LAUNCH_RANDOM_VELOCITY_PROPERTY = "launchVelocityRandomDelta";
    public static final String LAUNCH_DEAD_ZONE_PROPERTY = "launchDeadZone";
    public static final String DELEGATE_PROPERTY = "delegate";
    public static final String SCRIPT_PROPERTY = "script";

    public static final String ELEMENTS_PROPERTY = "elements";

    Map<String, Object> properties;
    List<EditableFieldElement> elements = new ArrayList<>();
    Set<EditableFieldElement> selectedElements = new HashSet<>();

    Runnable elementChangedCallback;
    Runnable selectionChangedCallback;

    public void setElementChangedCallback(Runnable callback) {
        elementChangedCallback = callback;
    }

    public void setSelectionChangedCallback(Runnable callback) {
        selectionChangedCallback = callback;
    }

    public void initFromProperties(Map<String, Object> props) {
        clearSelection();
        properties = CollectionUtils.mutableDeepCopyOfMap(props);
        if (!properties.containsKey(ELEMENTS_PROPERTY)) {
            properties.put(ELEMENTS_PROPERTY, new ArrayList<Object>());
        }
        elements.clear();
        List<Map<String, Object>> elementMaps = (List<Map<String, Object>>)properties.get(ELEMENTS_PROPERTY);
        // Strings are allowed in the elements array, should really be fields in element maps.
        for (Object emap : elementMaps) {
            if (emap instanceof Map) {
                EditableFieldElement elem = EditableFieldElement.createFromParameters((Map)emap);
                elem.setChangeHandler(elementChangedCallback);
                elements.add(elem);
            }
        }
    }

    public EditableFieldElement addNewElement(Class<? extends EditableFieldElement> elementClass) {
        EditableFieldElement elem;
        try {
            elem = elementClass.newInstance();
            elem.initAsNewElement(this);
            elem.setChangeHandler(elementChangedCallback);
            elements.add(elem);
            return elem;
        }
        catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override public boolean hasProperty(String propertyName) {
        return (properties!=null && properties.containsKey(propertyName));
    }

    @Override public Object getProperty(String propertyName) {
        if (properties == null) return null;
        return properties.get(propertyName);
    }

    @Override public void setProperty(String propertyName, Object value) {
        if (properties == null) return;
        properties.put(propertyName, value);
        if (elementChangedCallback != null) {
            elementChangedCallback.run();
        }
    }

    @Override public void removeProperty(String propertyName) {
        if (properties == null) return;
        properties.remove(propertyName);
        if (elementChangedCallback != null) {
            elementChangedCallback.run();
        }
    }

    public List<EditableFieldElement> getElements() {
        return elements;
    }

    public List<EditableFieldElement> elementsSortedByLayer() {
        List<EditableFieldElement> els = new ArrayList<>(this.elements);
        Collections.sort(els, Comparator.comparingInt(EditableFieldElement::getLayer));
        return els;
    }

    public void removeElements(Collection<EditableFieldElement> elements) {
        this.elements.removeAll(elements);
    }

    /**
     * Returns a copy of the current field map. Any subsequent changes to the field will not be
     * reflected in the returned map.
     */
    public Map<String, Object> getPropertyMapSnapshot() {
        List<Map<String, Object>> elementMaps = new ArrayList<>();
        for (EditableFieldElement elem : elements) {
            elementMaps.add(elem.getPropertyMap());
        }
        Map<String, Object> mapCopy = CollectionUtils.mutableDeepCopyOfMap(properties);
        mapCopy.put(ELEMENTS_PROPERTY, CollectionUtils.mutableDeepCopyOfList(elementMaps));
        return mapCopy;
    }

    public void restoreFromPropertyMapSnapshot(Map<String, Object> snapshot) {
        this.initFromProperties(snapshot);
    }

    void runSelectionChangeCallback() {
        if (selectionChangedCallback != null) {
            selectionChangedCallback.run();
        }
    }

    public void clearSelection() {
        selectedElements.clear();
        runSelectionChangeCallback();
    }

    public boolean isElementSelected(EditableFieldElement element) {
        return selectedElements.contains(element);
    }

    public void selectElement(EditableFieldElement element) {
        selectedElements.clear();
        selectedElements.add(element);
        runSelectionChangeCallback();
    }

    public void setSelectedElements(Collection<EditableFieldElement> elements) {
        selectedElements.clear();
        selectedElements.addAll(elements);
        runSelectionChangeCallback();
    }

    public boolean hasSelection() {
        return !selectedElements.isEmpty();
    }

    public Set<EditableFieldElement> getSelectedElements() {
        return new HashSet(selectedElements);
    }

    public Set<Integer> getSelectedElementIndexes() {
        Set<Integer> indexes = new HashSet<>();
        for(int i=0; i<elements.size(); i++) {
            if (selectedElements.contains(elements.get(i))) {
                indexes.add(i);
            }
        }
        return indexes;
    }

    public void setSelectedElementIndexes(Collection<Integer> indexes) {
        List<EditableFieldElement> selected = new ArrayList<>();
        for (int i : indexes) {
            selected.add(elements.get(i));
        }
        setSelectedElements(selected);
    }

    public void drawForEditor(IEditableFieldRenderer renderer) {
        for (EditableFieldElement elem : this.elementsSortedByLayer()) {
            elem.drawForEditor(renderer, this.isElementSelected(elem));
        }
    }
}
