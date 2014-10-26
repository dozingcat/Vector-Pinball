package com.dozingcatsoftware.bouncy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dozingcatsoftware.bouncy.elements.FieldElement;
import com.dozingcatsoftware.bouncy.elements.FlipperElement;

public class FieldElementCollection {

    List<FieldElement> allElements = new ArrayList<FieldElement>();
    Map<String, FieldElement> elementsById = new HashMap<String, FieldElement>();

    List<FlipperElement> flipperElements = new ArrayList<FlipperElement>();
    List<FlipperElement> leftFlipperElements = new ArrayList<FlipperElement>();
    List<FlipperElement> rightFlipperElements = new ArrayList<FlipperElement>();

    public void addElement(FieldElement element) {
        allElements.add(element);
        if (element.getElementID() != null) {
            elementsById.put(element.getElementID(), element);
        }
        if (element instanceof FlipperElement) {
            FlipperElement flipper = (FlipperElement) element;
            flipperElements.add(flipper);
            (flipper.isLeftFlipper() ? leftFlipperElements : rightFlipperElements).add(flipper);
        }
    }

    public List<FieldElement> getAllElements() {
        return allElements;
    }

    public List<FlipperElement> getFlipperElements() {
        return flipperElements;
    }

    public List<FlipperElement> getLeftFlipperElements() {
        return leftFlipperElements;
    }

    public List<FlipperElement> getRightFlipperElements() {
        return rightFlipperElements;
    }

    public FieldElement getElementForId(String id) {
        return elementsById.get(id);
    }
}
