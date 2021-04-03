package keyforge;

import java.util.*;
public class Action {
	private String actionName;
	private String action_move;
	private String target_name;
	private List<String> targetCondition;
	private int value;
	public Action() { 
		targetCondition = new ArrayList<>();
	}
	public void assignActionValues(String property, String propValue) {
		switch(property) {
		case "action":
			actionName = propValue;
			break;
		case "action_move":
			action_move = propValue;
			break;
		case "action_target":
			target_name = propValue;
			break;
		case "action_value":
			try { value = Integer.parseInt(propValue); }
			catch(Exception e) { System.out.println("Unable to retrieve integer on action value"); }
			break;
		case "action_target_condition":
			targetCondition.add(propValue);
			break;
		default:
			System.out.println("To be implemented!");
		}
		
	}
	public boolean coditionSatisfied(KFCard card, PlannedMove pm) {
		if(action_move != null && !pm.move.equals(action_move)) return false;
		if(target_name != null) {
			switch(target_name) {
			case "this": if(!pm.selectedCard.equals(card)) return false; break;
			default:
				System.out.println("This must be implemented (condition satisfied)");
				break;
			}
		}
		for(var cond : targetCondition) {
			switch(cond) {
			default:
				System.out.println("TBI " + cond);
			}
		}
		return true;
	}
	
}
