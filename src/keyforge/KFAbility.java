package keyforge;

import java.util.ArrayList;
import java.util.List;
import gameUtils.Utils;
import gameUtils.Utils.Type;

public class KFAbility {
	protected Utils.Type effectType;
	protected keyforge.Effect effect;
	protected keyforge.Action action;
	protected Target target;
	private KFCard owner;
	private String name;
	private int value;
	private boolean constant;
	private boolean usedThisTurn;
	private List<KFAbility> abilities_to_gain;
	private boolean spreadable;
	
	public KFAbility(String name) {
		effectType = Utils.resolveType(name);
	}
	public KFAbility(KFCard owner, String[] ability) {
		this(ability);
		this.owner = owner;
	}
	private KFAbility(String[] ability) {
		abilities_to_gain = new ArrayList<KFAbility>();
		target = new Target();
		effect = new Effect();
		action = new keyforge.Action();
		if(ability[1].contains("[")) {
			String[] subAbil = {"",Utils.getFilteredAttribute(ability[1])};
			ability[1] = ability[1].replaceAll("\\[(.*)\\]", "skip");
			abilities_to_gain.add(new KFAbility(subAbil));
		}
		var abil_attributes = ability[1].split(",");
		for(var attr : abil_attributes)
		{
			var subAttr = attr.split(":");
			var property = Utils.getFilteredWord(subAttr[0]).toLowerCase();
			var propValue = Utils.getFilteredWord(subAttr[1]).toLowerCase();
			if(propValue.equals("skip")) continue;
			else if(property.startsWith("target")) assignTargetValues(target,property, propValue);
			else if(property.startsWith("effect")) effect.assignEffectValues(property, propValue);
			else if (property.startsWith("action")) action.assignActionValues(property, propValue);
			else if(property.equals("name")) {
				effectType = Utils.resolveType(propValue);
				if(effectType.equals(Type.constant)) constant = true;
			}
			else if(property.equals("constant_name")) name = propValue;
			else if(property.equals("value")) try { value = Integer.parseInt(propValue); } catch(Exception e) { System.out.println("Tryed my best but not succeded...");}
			else if(property.equals("feature")) assignFeatures(propValue);
			else 
				System.out.println("Not recognized. :(");
		}
	}
	private void assignFeatures(String propValue) {
		var splitted = propValue.split(";");
		for(var split : splitted) {
			switch(split) {
			case "spreadable": spreadable = true; break;
			default : System.out.println("Feature tb implemented");
			}
		}
	}
	public void assessEffect(KFCard card, List<KFCard> matches) {
		effect.assessEffect(card, effectType, matches);
	}
	
	public static void assignTargetValues(Target target, String tProp, String tValues) {
		switch(tProp)
		{
		case "target":
			target.tClass = tValues;
			break;
		case "target_value":
			if(tValues.equals("auto")) {
				target.tValue = 100;
				break;
			}
			try {
				target.tValue = Integer.parseInt(tValues); 
			} catch(Exception e)
			{
				System.out.println("Not able to convert integer " + tValues);
				System.out.println(e.getMessage());
			}
			break;
		case "target_name":
			target.tName = tValues;
			break;
		case "target_position":
			target.tPosition = Utils.resolveFieldPosition(tValues);
			break;
		case "target_condition":
			target.evaluateTargetCondition(tValues);
			break;
		case "target_house":
			target.tHouse = tValues;
			break;
		case "target_type":
			target.tType = tValues;
			break;
		default :
			System.out.println("Not able to recognize " + tProp + " attributes with " + tValues + " property(ies)");
			return;
		}
		target.isEmpty = false;
		
	}
	public boolean actionConditionSatisfied(PlannedMove pm) { return action.coditionSatisfied(owner, pm); }
	public boolean isUsed() { return usedThisTurn; }
	public void setUsed(boolean val) { this.usedThisTurn = val; }
	public boolean isConst() { return this.constant; }
	public String getName() { return this.name; }
	public boolean isSpreadable() { return this.spreadable; }
	public KFCard getOwner() { return this.owner; }
	public double evaluateUtility(String move) {
		return 1;
	}
	public int getValue() { return value; }

}
