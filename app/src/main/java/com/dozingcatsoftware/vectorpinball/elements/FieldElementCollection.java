package com.dozingcatsoftware.vectorpinball.elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class FieldElementCollection {

    List<FieldElement> allElements = new ArrayList<>();
    Map<String, FieldElement> elementsById = new HashMap<>();

    List<FlipperElement> flipperElements = new ArrayList<>();
    List<FlipperElement> leftFlipperElements = new ArrayList<>();
    List<FlipperElement> rightFlipperElements = new ArrayList<>();

    Map<String, Object> variables = new HashMap<>();

    public void addElement(FieldElement element) {
        allElements.add(element);
        if (element.getElementId() != null) {
            elementsById.put(element.getElementId(), element);
        }
        if (element instanceof FlipperElement) {
            FlipperElement flipper = (FlipperElement) element;
            flipperElements.add(flipper);
            (flipper.isLeftFlipper() ? leftFlipperElements : rightFlipperElements).add(flipper);
        }
    }

    public void setVariable(String key, Object value) {
        variables.put(key, value);
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

    public Object getVariable(String key) {
        if (!variables.containsKey(key)) {
            throw new IllegalArgumentException("Variable not set: " + key);
        }
        return variables.get(key);
    }

    public Object getVariableOrDefault(String key, Object defaultValue) {
        return (variables.containsKey(key)) ? variables.get(key) : defaultValue;
    }
}
