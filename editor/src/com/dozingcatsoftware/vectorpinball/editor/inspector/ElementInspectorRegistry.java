package com.dozingcatsoftware.vectorpinball.editor.inspector;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.dozingcatsoftware.vectorpinball.editor.elements.EditableBumperElement;
import com.dozingcatsoftware.vectorpinball.editor.elements.EditableDropTargetGroupElement;
import com.dozingcatsoftware.vectorpinball.editor.elements.EditableFieldElement;
import com.dozingcatsoftware.vectorpinball.editor.elements.EditableFlipperElement;
import com.dozingcatsoftware.vectorpinball.editor.elements.EditableRolloverGroupElement;
import com.dozingcatsoftware.vectorpinball.editor.elements.EditableSensorElement;
import com.dozingcatsoftware.vectorpinball.editor.elements.EditableSpinnerElement;
import com.dozingcatsoftware.vectorpinball.editor.elements.EditableWallArcElement;
import com.dozingcatsoftware.vectorpinball.editor.elements.EditableWallElement;
import com.dozingcatsoftware.vectorpinball.editor.elements.EditableWallPathElement;

/**
 * Maps each editable element type to the inspector that edits its properties. This replaces a
 * reflection-based lookup (deriving an inspector class name from the element's "class" property)
 * with an explicit table, so a missing or renamed mapping is a compile-time error rather than a
 * silent runtime failure. To support a new element type, add a line to the static initializer.
 */
public class ElementInspectorRegistry {

    private static final Map<Class<? extends EditableFieldElement>, Supplier<ElementInspector>>
            INSPECTORS = new LinkedHashMap<>();

    static {
        register(EditableBumperElement.class, BumperElementInspector::new);
        register(EditableDropTargetGroupElement.class, DropTargetGroupElementInspector::new);
        register(EditableFlipperElement.class, FlipperElementInspector::new);
        register(EditableRolloverGroupElement.class, RolloverGroupElementInspector::new);
        register(EditableSensorElement.class, SensorElementInspector::new);
        register(EditableSpinnerElement.class, SpinnerElementInspector::new);
        register(EditableWallArcElement.class, WallArcElementInspector::new);
        register(EditableWallElement.class, WallElementInspector::new);
        register(EditableWallPathElement.class, WallPathElementInspector::new);
    }

    private static void register(
            Class<? extends EditableFieldElement> elementClass, Supplier<ElementInspector> factory) {
        INSPECTORS.put(elementClass, factory);
    }

    /** Creates a new inspector for the given element, or throws if its type is not registered. */
    public static ElementInspector createInspector(EditableFieldElement element) {
        Supplier<ElementInspector> factory = INSPECTORS.get(element.getClass());
        if (factory == null) {
            throw new IllegalArgumentException(
                    "No inspector registered for element type: " + element.getClass().getName());
        }
        return factory.get();
    }
}
