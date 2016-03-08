package com.dozingcatsoftware.bouncy.elements;

import static com.dozingcatsoftware.bouncy.util.MathUtils.asFloat;

import java.util.List;
import java.util.Map;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import com.dozingcatsoftware.bouncy.Ball;
import com.dozingcatsoftware.bouncy.Color;
import com.dozingcatsoftware.bouncy.Field;
import com.dozingcatsoftware.bouncy.IFieldRenderer;

/**
 * Abstract superclass of all elements in the pinball field, such as walls, bumpers, and flippers.
 */

public abstract class FieldElement {

    public static final String CLASS_PROPERTY = "class";
    public static final String ID_PROPERTY = "id";
    public static final String SCORE_PROPERTY = "score";
    public static final String COLOR_PROPERTY = "color";

    Map<String, ?> parameters;
    World box2dWorld;
    String elementID;
    Color initialColor;
    Color newColor;

    int flashCounter=0; // Inverts colors when >0, decrements in tick().
    long score = 0;

    // Default wall color shared by WallElement, WallArcElement, WallPathElement.
    static final Color DEFAULT_WALL_COLOR = Color.fromRGB(64, 64, 160);

    /**
     * Exception thrown when an element can't be created because it depends on another element that
     * hasn't been created yet.
     */
    public static class DependencyNotAvailableException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public DependencyNotAvailableException(String message) {
            super(message);
        }
    }

    /**
     * Creates and returns a FieldElement object from the given map of parameters. The class to
     * instantiate is given by the "class" property of the parameter map. Calls the no-argument
     * constructor of the default or custom class, and then calls initialize() passing the
     * parameter map and World.
     */
    @SuppressWarnings("unchecked")
    public static FieldElement createFromParameters(
            Map<String, ?> params, FieldElementCollection collection, World world)
                    throws DependencyNotAvailableException {
        if (!params.containsKey(CLASS_PROPERTY)) {
            throw new IllegalArgumentException("class not specified for element: " + params);
        }
        Class<? extends FieldElement> elementClass = null;
        // if package not specified, use this package
        String className = (String)params.get(CLASS_PROPERTY);
        if (className.indexOf('.')==-1) {
            className = "com.dozingcatsoftware.bouncy.elements." + className;
        }
        try {
            elementClass = (Class<? extends FieldElement>) Class.forName(className);
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        FieldElement self;
        try {
            self = elementClass.newInstance();
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        catch (InstantiationException e) {
            throw new RuntimeException(e);
        }
        self.initialize(params, collection, world);
        return self;
    }

    /**
     * Extracts common values from the definition parameter map, and calls finishCreate to allow
     * subclasses to further initialize themselves. Subclasses should override finishCreate, and
     * should not override this method.
     */
    public void initialize(Map<String, ?> params, FieldElementCollection collection, World world)
            throws DependencyNotAvailableException {
        this.parameters = params;
        this.box2dWorld = world;
        this.elementID = (String)params.get(ID_PROPERTY);

        @SuppressWarnings("unchecked")
        List<Number> colorList = (List<Number>)params.get(COLOR_PROPERTY);
        if (colorList!=null) {
            this.initialColor = Color.fromList(colorList);
        }

        if (params.containsKey(SCORE_PROPERTY)) {
            this.score = ((Number)params.get(SCORE_PROPERTY)).longValue();
        }

        this.finishCreateElement(params, collection);
        this.createBodies(world);
    }

    /**
     * Called after creation to determine if tick() needs to be called after every frame is
     * simulated. Default returns false, subclasses must override to return true in order for
     * tick() to be called. This is an optimization to avoid needless method calls in the game loop.
     */
    public boolean shouldCallTick() {
        return false;
    }

    /**
     * Called on every update from Field.tick. Default implementation decrements flash counter if
     * active, subclasses can override to perform additional processing, e.g. RolloverGroupElement
     * checking for balls within radius of rollovers. Subclasses should call super.tick(field).
     */
    public void tick(Field field) {
        if (flashCounter>0) flashCounter--;
    }

    /**
     * Called when the player activates one or more flippers. The default implementation does
     * nothing; subclasses can override.
     */
    public void flippersActivated(Field field, List<FlipperElement> flippers) {}

    /**
     * Causes the colors returned by red/blue/greenColorComponent methods to be inverted for the
     * given number of frames. This can be used to flash an element when it is hit by a ball.
     */
    public void flashForFrames(int frames) {
        flashCounter = frames;
    }

    /**
     * Must be overridden by subclasses, which should perform any setup required after creation.
     * Throws DependencyNotAvailableException if the element can't be initialized because it's
     * dependent on other uninitialized elements.
     */
    public abstract void finishCreateElement(Map<String, ?> params, FieldElementCollection collection)
            throws DependencyNotAvailableException;

    /**
     * Must be overridden by subclasses, to create the element's Box2D bodies. This will be called
     * after finishCreateElement has completed (without throwing DependencyNotAvailableException).
     */
    public abstract void createBodies(World world);

    /**
     * Must be overridden by subclasses to return a collection of all Box2D bodies which make up
     * this element.
     */
    public abstract List<Body> getBodies();

    /**
     * Must be overridden by subclasses to draw the element, using IFieldRenderer methods.
     */
    public abstract void draw(IFieldRenderer renderer);

    /**
     * Called when a ball collides with a Body in this element. The default implementation does
     * nothing (allowing objects to bounce off each other normally). Subclasses can override to
     * perform other actions like applying extra force.
     */
    public void handleCollision(Ball ball, Body bodyHit, Field field) {}

    /** Returns this element's ID, or null if not specified. */
    public String getElementId() {
        return elementID;
    }

    /** Returns the parameter map from which this element was created. */
    public Map<String, ?> getParameters() {
        return parameters;
    }

    public boolean hasParameterKey(String key) {
        return parameters.containsKey(key);
    }

    public Object getRawParameterValueForKey(String key) {
        return parameters.get(key);
    }

    public float getFloatParameterValueForKey(String key) {
        // TODO: parse function/math expressions.
        return asFloat(parameters.get(key));
    }

    public int getIntParameterValueForKey(String key) {
        // TODO: parse function/math expressions.
        Number num = (Number) parameters.get(key);
        return num.intValue();
    }

    public long getLongParameterValueForKey(String key) {
        // TODO: parse function/math expressions.
        Number num = (Number) parameters.get(key);
        return num.longValue();
    }

    public float[] getFloatArrayParameterValueForKey(String key) {
        // TODO: parse function/math expressions.
        List<?> list = (List<?>) parameters.get(key);
        float[] result = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            result[i] = asFloat(list.get(i));
        }
        return result;
    }

    public boolean getBooleanParameterValueForKey(String key) {
        Object value = parameters.get(key);
        return (Boolean.TRUE.equals(value) ||
                ((value instanceof Number) && ((Number) value).doubleValue() != 0));
    }

    /**
     * Returns the "score" value for this element. The score is automatically added when the
     * element is hit by a ball, and elements may apply scores under other conditions,
     * e.g. RolloverGroupElement adds the score when a ball comes within range of a rollover.
     */
    public long getScore() {
        return score;
    }

    public void setNewColor(Color value) {
        this.newColor = value;
    }

    /**
     * Gets the current color by using the defined color if set and the default color if not, and
     * inverting if the element is flashing. Subclasses can override.
     */
    protected Color currentColor(Color defaultColor) {
        Color baseColor = (this.newColor != null) ?
                this.newColor :
                    (this.initialColor != null) ? this.initialColor : defaultColor;
        return (flashCounter > 0) ? baseColor.inverted() : baseColor;
    }
}
