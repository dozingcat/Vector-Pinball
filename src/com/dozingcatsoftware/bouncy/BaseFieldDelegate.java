package com.dozingcatsoftware.bouncy;

import com.badlogic.gdx.physics.box2d.Body;
import com.dozingcatsoftware.bouncy.elements.DropTargetGroupElement;
import com.dozingcatsoftware.bouncy.elements.FieldElement;
import com.dozingcatsoftware.bouncy.elements.RolloverGroupElement;


/** This class implements the Field.Delegate interface and does nothing for each of the interface methods. Real delegates
 * can subclass this class to avoid having to create empty implementations for events they don't care about. If a field 
 * definition doesn't specify a delegate class, an instance of this class will be used as a placeholder delegate. 
 * @author brian
 */
public class BaseFieldDelegate implements Field.Delegate {

	@Override
	public void allDropTargetsInGroupHit(Field field, DropTargetGroupElement targetGroup) {
	}

	@Override
	public void allRolloversInGroupActivated(Field field, RolloverGroupElement rolloverGroup) {
	}

	@Override
	public void flipperActivated(Field field) {
	}

	@Override
	public void processCollision(Field field, FieldElement element, Body hitBody, Body ball) {
	}

}
