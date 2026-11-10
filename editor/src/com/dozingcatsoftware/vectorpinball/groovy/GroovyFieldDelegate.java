package com.dozingcatsoftware.vectorpinball.groovy;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.physics.box2d.Body;
import com.dozingcatsoftware.vectorpinball.elements.DropTargetGroupElement;
import com.dozingcatsoftware.vectorpinball.elements.FieldElement;
import com.dozingcatsoftware.vectorpinball.elements.FlipperElement;
import com.dozingcatsoftware.vectorpinball.elements.RolloverGroupElement;
import com.dozingcatsoftware.vectorpinball.elements.SensorElement;
import com.dozingcatsoftware.vectorpinball.elements.SpinnerElement;
import com.dozingcatsoftware.vectorpinball.model.Ball;
import com.dozingcatsoftware.vectorpinball.model.Field;

import groovy.lang.Closure;
import groovy.lang.Script;

public class GroovyFieldDelegate implements Field.Delegate {

	Script groovyScript;
	Closure<Object> gameStarted;
	Closure<Object> ballLost;
	Closure<Object> gameEnded;
	Closure<Object> tick;
	Closure<Object> processCollision;
	Closure<Object> flippersActivated;
	Closure<Object> allDropTargetsInGroupHit;
	Closure<Object> allRolloversInGroupActivated;
	Closure<Object> ballInSensorRange;
	Closure<Object> isFieldActive;
	Closure<Object> spinnerActivated;

	public GroovyFieldDelegate initWithScript(Script groovyScript) {
		this.groovyScript = groovyScript;
		Object result = groovyScript.run();
		@SuppressWarnings("rawtypes")
        Map<?, ?> methodMap = (result instanceof Map) ? ((Map) result) : Collections.emptyMap();

		gameStarted = getClosure(methodMap, "gameStarted");
		ballLost = getClosure(methodMap, "ballLost");
		gameEnded = getClosure(methodMap, "gameEnded");
		tick = getClosure(methodMap, "tick");
		processCollision = getClosure(methodMap, "processCollision");
		flippersActivated = getClosure(methodMap, "flippersActivated");
		allDropTargetsInGroupHit = getClosure(methodMap, "allDropTargetsInGroupHit");
		allRolloversInGroupActivated = getClosure(methodMap, "allRolloversInGroupActivated");
		ballInSensorRange = getClosure(methodMap, "ballInSensorRange");
		isFieldActive = getClosure(methodMap, "isFieldActive");
		spinnerActivated = getClosure(methodMap, "spinnerActivated");

		return this;
	}

	Map<?, ?> getDelegateMethodMap() {
		Object methodMap = groovyScript.getProperty("delegate");
		return (methodMap instanceof Map) ? ((Map<?, ?>) methodMap) : Collections.emptyMap();
	}

	@SuppressWarnings("unchecked")
	static Closure<Object> getClosure(Map<?, ?> methodMap, String methodName) {
		Object value = methodMap.get(methodName);
		return (value instanceof Closure) ? ((Closure<Object>) value) : null;
	}

	@Override public void gameStarted(Field field) {
		if (gameStarted != null) {
			gameStarted.call(field);
		}
	}

	@Override public void ballLost(Field field) {
		if (ballLost != null) {
			ballLost.call(field);
		}
	}

	@Override public void gameEnded(Field field) {
		if (gameEnded != null) {
			gameEnded.call(field);
		}

	}

	@Override public void tick(Field field, long nanos) {
		if (tick != null) {
			tick.call(field, nanos);
		}
	}

	@Override public void processCollision(Field field, FieldElement element, Body hitBody, Ball ball) {
		if (processCollision != null) {
			processCollision.call(field, element, hitBody, ball);
		}
	}

	@Override public void flippersActivated(Field field, List<FlipperElement> flippers) {
		if (flippersActivated != null) {
			flippersActivated.call(field, flippers);
		}
	}

	@Override public void allDropTargetsInGroupHit(Field field, DropTargetGroupElement targetGroup, Ball ball) {
		if (allDropTargetsInGroupHit != null) {
			allDropTargetsInGroupHit.call(field, targetGroup, ball);
		}
	}

	@Override public void allRolloversInGroupActivated(Field field, RolloverGroupElement rolloverGroup, Ball ball) {
		if (allRolloversInGroupActivated != null) {
			allRolloversInGroupActivated.call(field, rolloverGroup, ball);
		}
	}

	@Override public void ballInSensorRange(Field field, SensorElement sensor, Ball ball) {
		if (ballInSensorRange != null) {
			ballInSensorRange.call(field, sensor, ball);
		}

	}

	@Override public boolean isFieldActive(Field field) {
		if (isFieldActive != null) {
			return Boolean.TRUE.equals(isFieldActive.call(field));
		}
		return false;
	}

	@Override
	public void spinnerActivated(Field field, SpinnerElement spinner, Ball ball) {
		if (spinnerActivated != null) {
			spinnerActivated.call(field, spinner, ball);
		}
	}
}
