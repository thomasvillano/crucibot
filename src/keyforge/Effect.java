package keyforge;

import static gameUtils.Utils.*;
import java.util.*;
import java.util.stream.Collectors;

public class Effect {
	private String name;
	public int value;
	public String sValue;
	public FieldPosition position;
	public Target target;
	public List<String> tNames;
	public List<String> conds;
	public List<KFCard> realMatches;
	
	public Effect()
	{
		tNames = new ArrayList<String>();
		conds = new ArrayList<String>();
		realMatches = new ArrayList<KFCard>();
	}
	public void assignEffectValues(String property, String propValue)
	{
		switch(property)
		{
		case "effect":
			name = propValue;
			break;
		case "effect_value":
			if(propValue.equals("all") || propValue.equals("full"))
			{
				value = 100;
				break; 
			}
			try {
				value = Integer.parseInt(propValue);
			} catch(Exception e) {
				sValue = propValue;
			}
			break;
		case "effect_target":
			try {
				for(var tar : propValue.split(";"))
					tNames.add(tar);
			} catch(Exception e)
			{
				tNames.add(propValue);
			}
			break;
		case "effect_condition":
			evaluateEffectCondition(propValue);
			break;
		case "effect_position":
			position = resolveFieldPosition(propValue);
			break;
		default:
			System.out.println("Not recognizing any value of " + property + " with " + propValue);
			break;
		}
	}

	public void assessEffect(KFCard parentCard, Type type, List<KFCard> matches) {
		this.realMatches = new ArrayList<KFCard>();
		this.target = parentCard.abilities.stream()
				.filter(x -> x.effectType.equals(type))
				.findFirst()
				.orElse(null)
				.target;
		if(target.isEmpty) {
			System.out.println("Target of card " + parentCard.name + " is void");
		}
		if(matches == null) {
			return;
		}
		if(this.target.isEmpty)
			return;
		for(var name : tNames) {
			switch(name) {
			case "target":
				if(matches != null)	realMatches.addAll(matches);
				break;
			case "this":
				realMatches.add(parentCard);
				break;
			case "target_left_neighbor":
				matches.forEach(x -> {
					if(x.leftNeighbor != null) 
						realMatches.add(x.leftNeighbor);
					});
				break;
			case "target_right_neighbor":
				matches.forEach(x -> {
					if(x.rightNeighbor != null) 
						realMatches.add(x.rightNeighbor);
					});
				break;
			default:
				System.out.println("Target of effect must be evaluated for the case : " + name);
				break;
			}
		}
		realMatches = realMatches.stream().filter(distinctByKey(KFCard::getUuid)).collect(Collectors.toList());
	}
	public void evaluateEffectCondition(String propValues) {
		try {
			for(var cond : propValues.split(";"))
				conds.add(cond);
		} catch(Exception e)
		{
			conds.add(propValues);
		}		
	}
	public String getName() { 
		return this.name; 
	}
}
