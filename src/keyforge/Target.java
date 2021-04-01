package keyforge;

import static gameUtils.Utils.*;
import java.util.*;
import java.util.function.Predicate;

public class Target {
	public String tClass;
	public String tName;
	public String tType;
	public List<String> conds;
	public List<KFCard> cards;
	public int tValue;
	public FieldPosition tPosition;
	public String tHouse;
	public boolean isEmpty;
	public Boolean isEnemy;
	public List<Predicate<KFCard>> conditions;
	
	public Target() {
		cards = new ArrayList<KFCard>();
		conds = new ArrayList<String>();
		conditions = new ArrayList<Predicate<KFCard>>();
		isEmpty = true;
	}
	public static void evaluateCondition() {}
	
	
	/**
	 * This function intent is to create a filter scheme to find all possible targets and
	 * evaluate it's utility
	 */
	public void generatePredicates(KFCard card) {
		conditions = new ArrayList<Predicate<KFCard>>();
		this.typeConditions();
		if(tHouse != null) {
			if(tHouse.startsWith("not")) conditions.add(x -> !x.house.equals(resolveHouse(tHouse.split("_")[1])));
			else conditions.add(x -> x.house.equals(resolveHouse(tHouse)));			
		}
		if(tName != null) {
			if (tName.equals("this")) conditions.add(x -> x.equals(card)); 
			else conditions.add(x -> x.name.equals(tName)); 
		}
		
		if(tType != null) {
			if(tType.startsWith("not")) conditions.add(x -> !((KFCreature)x).types.contains(tType.split("_")[1]));
			else conditions.add(x -> ((KFCreature)x).types.contains(tType));
		}
		if(tPosition != null) conditions.add(x -> x.position.equals(tPosition));
		this.addConditions(card);
	}
	private boolean multipleClass() {
		if(tClass == null) return false;
		if(!tClass.contains(";")) return false;
		var splitted = tClass.split(";");
		List<Predicate<KFCard>> predicates = new ArrayList<>();
		for(var pred : splitted) {
			switch(pred) {
			case "creature": predicates.add(x -> KFCreature.class.isInstance(x)); break;
			case "action": predicates.add(x -> KFAction.class.isInstance(x)); break;
			case "upgrade": predicates.add(x-> KFUpgrade.class.isInstance(x)); break;
			case "artifact": predicates.add(x -> KFArtifact.class.isInstance(x)); break;
			}
		}
		Predicate<KFCard> predicate = predicates.stream()
		        .reduce(x -> true, Predicate::or);
		conditions.add(predicate);
		return true;
	}
	private void typeConditions() {
		if(multipleClass()) return;
		if(tClass != null) 
			switch(tClass) {
			case "creature": conditions.add(x -> KFCreature.class.isInstance(x)); break;
			case "action": conditions.add(x -> KFAction.class.isInstance(x)); break;
			case "upgrade": conditions.add(x-> KFUpgrade.class.isInstance(x)); break;
			case "artifact": conditions.add(x -> KFArtifact.class.isInstance(x)); break;
			case "card": conditions.add(x -> x != null); break;
			default:
				System.out.println("Not recognized case");
				break;
			}
	}
	private void addConditions(KFCard card) {
		for(var cond : conds) 
			addCondition(card, cond);
	}
	private void addCondition(KFCard card, String cond) {
		switch(cond) {
		case "friend": conditions.add(x -> !x.isEnemy); conditions.add(x -> !x.equals(card)); break;
		case "friend_all": conditions.add(x -> !x.isEnemy); break;
		case "enemy": conditions.add(x -> x.isEnemy); break;
		case "neighbor": conditions.add(x -> (card.rightNeighbor != null && card.rightNeighbor.equals(x)) || (card.leftNeighbor != null && card.leftNeighbor.equals(x))); break;
		case "power=odd": conditions.add(x -> KFCreature.class.isInstance(x) && ((KFCreature)x).getAttack()%2!=0); break;
		case "flank": conditions.add(x -> x.leftNeighbor == null || x.rightNeighbor == null); break;
		case "undamaged": conditions.add(x -> KFCreature.class.isInstance(x) && ((KFCreature)x).damage == 0); break;
		case "damaged": conditions.add(x -> KFCreature.class.isInstance(x) && ((KFCreature)x).damage > 0); break;
		case "exhausted": conditions.add(x -> KFCreature.class.isInstance(x) && ((KFCreature)x).exhausted == true); break;
		case "auto": break;
		case "is_new": conditions.add(x -> x.isNew()); break;
		default :
			System.out.println("Case not resolved yet " + cond);
			break;
		}
	}
	public void evaluateTargetCondition(String tValues) {
		try {
			for(var cond : tValues.split(";"))
				conds.add(cond);
		} catch(Exception e)
		{
			conds.add(tValues);
		}		
	}
	public boolean isEmpty() { return isEmpty; }
}
