package com.dozingcatsoftware.bouncy.elements;

import java.util.List;
import java.util.Map;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import com.dozingcatsoftware.bouncy.Color;
import com.dozingcatsoftware.bouncy.Field;
import com.dozingcatsoftware.bouncy.IFieldRenderer;
import com.dozingcatsoftware.bouncy.Point;

/**
 * Abstract superclass of all elements in the pinball field, such as walls, bumpers, and flippers.
 */

public abstract class FieldElement {

	Map parameters;
	World box2dWorld;
	String elementID;
	Color color;
	
	int flashCounter=0; // when >0, inverts colors (e.g. after being hit by the ball), decrements in tick()
	long score = 0;
	
	// default wall color shared by WallElement, WallArcElement, WallPathElement
	static final Color DEFAULT_WALL_COLOR = Color.fromRGB(64, 64, 160);

	/**
	 * Exception thrown when an element can't be created because it depends on another element that
	 * hasn't been created yet.
	 */
	public static class DependencyNotAvailableException extends Exception {
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
	public static FieldElement createFromParameters(Map params, FieldElementCollection collection, World world)
	        throws DependencyNotAvailableException {
	    if (!params.containsKey("class")) {
	        throw new IllegalArgumentException("class not specified for element: " + params);
	    }
	    Class elementClass = null;
		// if package not specified, use this package
		String className = (String)params.get("class");
		if (className.indexOf('.')==-1) {
			className = "com.dozingcatsoftware.bouncy.elements." + className;
		}
		try {
		    elementClass = Class.forName(className);
        }
		catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
			
		FieldElement self;
        try {
            self = (FieldElement) elementClass.newInstance();
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

	/** Extracts common values from the definition parameter map, and calls finishCreate to allow subclasses to further initialize themselves.
	 * Subclasses should override finishCreate, and should not override this method.
	 */
	public void initialize(Map params, FieldElementCollection collection, World world)
	        throws DependencyNotAvailableException {
		this.parameters = params;
		this.box2dWorld = world;
		this.elementID = (String)params.get("id");
		
		List<Integer> colorList = (List<Integer>)params.get("color");
		if (colorList!=null) {
			this.color = Color.fromList(colorList);
		}
		
		if (params.containsKey("score")) {
			this.score = ((Number)params.get("score")).longValue();
		}
		
		this.finishCreateElement(params, collection);
		this.createBodies(world);
	}

	/** Called after creation to determine if tick() needs to be called after every frame is simulated. Default returns false, 
	 * subclasses must override to return true in order for tick() to be called. This is an optimization to avoid needless
	 * method calls in the game loop.
	 */
	public boolean shouldCallTick() {
		return false;
	}

	/** Called on every update from Field.tick. Default implementation decrements flash counter if active, subclasses can override to perform
	 * additional processing, e.g. RolloverGroupElement checking for balls within radius of rollovers. Subclasses should call super.tick(field).
	 */
	public void tick(Field field) {
		if (flashCounter>0) flashCounter--;
	}
	
	/** Called when the player activates one or more flippers. The default implementation does nothing; subclasses can override.
	 */
	public void flippersActivated(Field field, List<FlipperElement> flippers) {
		
	}
	
	/** Causes the colors returned by red/blue/greenColorComponent methods to be inverted for the given number of frames. This can be used
	 * to flash an element when it is hit by a ball, see PegElement.
	 */
	public void flashForFrames(int frames) {
		flashCounter = frames;
	}

	/**
	 * Must be overridden by subclasses, which should perform any setup required after creation.
	 * Throws DependencyNotAvailableException if the element can't be initialized because it's
	 * dependent on other uninitialized elements.
	 */
	public abstract void finishCreateElement(Map params, FieldElementCollection collection)
	        throws DependencyNotAvailableException;

	/**
	 * Must be overridden by subclasses, to create the element's Box2D bodies. This will be called
	 * after finishCreateElement has completed (without throwing DependencyNotAvailableException).
	 */
	public abstract void createBodies(World world);

	/** Must be overridden by subclasses to return a collection of all Box2D bodies which make up this element.
	 */
	public abstract List<Body> getBodies();

	/** Must be overridden by subclasses to draw the element, using IFieldRenderer methods.
	 */
	public abstract void draw(IFieldRenderer renderer);
	
	/** Called when a ball collides with a Body in this element. The default implementation does nothing (allowing objects to
	 * bounce off each other normally), subclasses can override (e.g. to apply extra force)
	 */
	public void handleCollision(Body ball, Body bodyHit, Field field) {
	}
	
	/** Returns this element's ID as specified in the JSON definition, or null if the ID is not specified.
	 */
	public String getElementID() {
		return elementID;
	}
	
	/** Returns the parameter map from which this element was created.
	 */
	public Map getParameters() {
		return parameters;
	}

	/** Returns the "score" value for this element. The score is automatically added when the element is hit by a ball, and elements
	 * may apply scores under other conditions, e.g. RolloverGroupElement adds the score when a ball comes within range of a rollover.
	 */
	public long getScore() {
		return score;
	}

	/**
	 * Gets the current color by using the defined color if set and the default color if not, and
	 * inverting if the element is flashing. Subclasses can override.
	 */
	protected Color currentColor(Color defaultColor) {
	    Color baseColor = (this.color != null) ? this.color : defaultColor;
	    return (flashCounter > 0) ? baseColor.inverted() : baseColor;
	}

	/**
	 * Returns the point at which this element starts, for example the first endpoint of a wall.
	 * Default implementation throws UnsupportedOperationException, subclasses should override.
	 */
	public Point getStartPoint() {
	    throw new UnsupportedOperationException();
	}

    /**
     * Returns the point at which this element ends, for example the second endpoint of a wall.
     * Default implementation throws UnsupportedOperationException, subclasses should override.
     */
	public Point getEndPoint() {
	    throw new UnsupportedOperationException();
	}
}
