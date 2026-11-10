package com.dozingcatsoftware.vectorpinball.editor.elements;

import static com.dozingcatsoftware.vectorpinball.util.MathUtils.asDouble;
import static com.dozingcatsoftware.vectorpinball.util.MathUtils.asInt;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dozingcatsoftware.vectorpinball.editor.IEditableFieldRenderer;
import com.dozingcatsoftware.vectorpinball.model.Color;
import com.dozingcatsoftware.vectorpinball.editor.Point;

public abstract class EditableFieldElement implements PropertyContainer {

    public static final String CLASS_PROPERTY = "class";
    public static final String ID_PROPERTY = "id";
    public static final String COLOR_PROPERTY = "color";
    public static final String SCORE_PROPERTY = "score";
    public static final String LAYER_PROPERTY = "layer";
    public static final String INACTIVE_LAYER_COLOR_PROPERTY = "inactiveLayerColor";

    static final int DEFAULT_WALL_COLOR = Color.fromRGB(64, 64, 160);
    static final String CLASS_PREFIX = "com.dozingcatsoftware.vectorpinball.editor.elements.Editable";

    private Map<String, Object> properties;
    private Runnable changeHandler;

    public void initFromPropertyMap(Map<String, Object> props) {
        properties = props;
    }

    public void initAsNewElement(EditableField field) {
        properties = new HashMap<>();
        properties.put(CLASS_PROPERTY, getClass().getName().replace(CLASS_PREFIX, ""));
        addPropertiesForNewElement(properties, field);
    }

    /**
     * Must be implemented by subclasses to fill a map of properties for the
     * element when it has been added to an existing field.
     */
    protected abstract void addPropertiesForNewElement(Map<String, Object> props, EditableField field);

    public void setChangeHandler(Runnable handler) {
        changeHandler = handler;
    }

    public Map<String, Object> getPropertyMap() {
        return properties;
    }

    @Override public void setProperty(String key, Object value) {
        properties.put(key, value);
        if (changeHandler != null) {
            changeHandler.run();
        }
    }

    @Override public void removeProperty(String key) {
        properties.remove(key);
        if (changeHandler != null) {
            changeHandler.run();
        }
    }

    @Override public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }

    @Override public Object getProperty(String key) {
        return properties.get(key);
    }

    // Typed accessors for reading values out of the property map. The property map is the single
    // source of truth for an element's state; subclasses read through these rather than caching
    // values in instance variables.

    protected double getDoubleProperty(String key) {
        return asDouble(getProperty(key));
    }

    protected int getIntProperty(String key) {
        return asInt(getProperty(key));
    }

    protected int getIntProperty(String key, int defaultValue) {
        return asInt(getProperty(key), defaultValue);
    }

    protected List<?> getListProperty(String key) {
        return (List<?>) getProperty(key);
    }

    /** Reads element {@code index} of a list-valued property (e.g. a coordinate) as a double. */
    protected double getListDoubleProperty(String key, int index) {
        return asDouble(getListProperty(key).get(index));
    }

    /** Reads a property whose value is a 2-element [x, y] list as a Point. */
    protected Point getPointProperty(String key) {
        List<?> list = getListProperty(key);
        return Point.fromXY(asDouble(list.get(0)), asDouble(list.get(1)));
    }

    /** Stores a Point as a 2-element [x, y] list property. */
    protected void setPointProperty(String key, Point point) {
        setProperty(key, Arrays.asList(point.x, point.y));
    }

    public static EditableFieldElement createFromParameters(Map params) {
        if (!params.containsKey(CLASS_PROPERTY)) {
            throw new IllegalArgumentException("class not specified for element: " + params);
        }
        EditableFieldElement self = null;
        try {
            // if package not specified, use this package
            String className = (String)params.get(CLASS_PROPERTY);
            if (className.indexOf('.')==-1) {
                className = CLASS_PREFIX + className;
            }
            Class elementClass = Class.forName(className);
            self = (EditableFieldElement) elementClass.getDeclaredConstructor().newInstance();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        self.initFromPropertyMap(params);
        return self;
    }

    /**
     * Gets the current color by using the defined color if set and the default color if not.
     */
    protected int currentColor(int defaultColor) {
        int color = baseColorOrDefault(defaultColor);
        return colorForDisplay(color);
    }

    protected int baseColorOrDefault(int defaultColor) {
        return properties.containsKey(COLOR_PROPERTY) ?
                Color.fromList((List<Number>)properties.get(COLOR_PROPERTY)) : defaultColor;
    }

    protected int colorForDisplay(int color) {
        // If very dark, make brighter for display.
        int rgbSum = Color.getRed(color) + Color.getGreen(color) + Color.getBlue(color);
        if (rgbSum < 192 || Color.getAlpha(color) < 64) {
            int extra = Math.max(0, (192 - rgbSum) / 3);
            return Color.fromRGBA(
                    Color.getRed(color) + extra,
                    Color.getGreen(color) + extra,
                    Color.getBlue(color) + extra,
                    Math.max(Color.getAlpha(color), 128));
        }
        return color;
    }

    public int getLayer() {
        return this.properties.containsKey(LAYER_PROPERTY) ?
                ((Number)this.properties.get(LAYER_PROPERTY)).intValue() : 0;
    }

    abstract public void drawForEditor(IEditableFieldRenderer renderer, boolean isSelected);

    /**
     * Determines whether a point is sufficiently close to any part of this element.
     */
    abstract public boolean isPointWithinDistance(Point point, double distance);

    /**
     * Called when a drag operation begins. Will be followed by any number of handleDrag calls.
     * This method can be used to store data needed to update correctly for drags, for example
     * which part of the element the drag started at.
     */
    public void startDrag(Point point) {}

    /**
     * Called when a drag is in progress to update the properties map. drawForEditor() may be
     * called any number of times during a drag. Default implementation calls `translate` to
     * move the entire element; subclasses can override for custom behavior (e.g. wall elements
     * where the user may be moving just one endpoint).
     */
    public void handleDrag(Point point, Point deltaFromStart, Point deltaFromPrevious) {
        translate(deltaFromPrevious);
    }

    /**
     * Moves the entire element by the given offset.
     */
    abstract public void translate(Point offset);
}
