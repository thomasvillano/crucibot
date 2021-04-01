package keyforge;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONObject;
import static gameUtils.Utils.*;

public abstract class KFCard {
	protected String name;
	protected House house;
	protected int amber;
	protected KFCard leftNeighbor;
	protected KFCard rightNeighbor;
	protected FieldPosition position;
	protected List<KFAbility> abilities;
	protected boolean isNew;
	private boolean controlled;
	protected boolean ready;
	protected Boolean isEnemy;
	protected boolean exhausted;
	protected boolean playable;
	protected boolean selectable;
	protected boolean selected;
	public int dynamicIndexPosition;
	private String uuid;
	
	public KFCard() {	}
	public KFCard(KFCard card) {	
		name = card.getName();
		house = card.getHouse();
		amber = card.getAmber();
		leftNeighbor = card.getLeftNeighBor();
		rightNeighbor = card.getRightNeighbor();
		position = card.getPosition();
		abilities = card.getAbilities();
		ready = card.getReady();
		exhausted = card.getExhausted();
		playable = card.getPlayable();
		selectable = card.getSelectable();
		uuid = card.getUuid();
		isEnemy = card.isEnemy;
	}
	public KFCard(String name, House house) {
		this.abilities = new ArrayList<KFAbility>();
		this.name = name;
		this.house = house;
	}
	
	public String getUuid() { return this.uuid; }
	public void setUuid(String uuid) { this.uuid = uuid; }
	private boolean getSelectable() { return this.selectable; }
	private boolean getPlayable() { return this.playable; }
	private boolean getExhausted() { return this.exhausted; }
	private boolean getReady() { return this.ready; }
	private List<KFAbility> getAbilities() { return this.abilities; }
	private FieldPosition getPosition() { return this.position; }
	private KFCard getRightNeighbor() { return this.rightNeighbor; }
	private KFCard getLeftNeighBor() { return this.leftNeighbor; }
	public String getName() { return this.name; }
	public House getHouse() { return this.house; }
	public int getAmber() {	return this.amber; }
	
	public abstract void resetModifiers();
	public abstract double evaluateUtility(String move);
	
	public void assignBlock(String[] block) {
		if(block == null)
			return;
		switch(block[0].toLowerCase()) {
		case "amber": this.amber = Integer.parseInt(block[1]); break;
		case "ability": abilities.add(new KFAbility(this,block)); break;
		default: System.out.println("Not implemented yet.." + block[0]); break;
		}
	}
	public boolean equals(KFCard card) {
		return this.uuid.equals(card.uuid);
	}
	
	public boolean equals(String uuid) {
		if(this.uuid.equals(uuid))
			return true;
		else return false;
	}
	public abstract void updateAfterPlay();
	public void updateCardAfterDiscard() {
		this.playable = false;
		this.position = FieldPosition.discard;
	}
	public void updateCardAfterIgnore() {
		this.playable = false;
	}
	public void assessEffect(List<KFCard> matches) {
		abilities.forEach(x -> x.assessEffect(this, matches));
	}
	public void setHouse(String house) {
		this.house = resolveHouse(house);
	}
	public boolean getFriendPlaying() {
		return !this.isEnemy && this.position.equals(FieldPosition.playarea);
	}
	public void assessCard() {
		for(var abil : abilities) {
			abil.target.generatePredicates(this);	
		}
	}
	public void print() {
		print(0);
	}
	public void print(int index) {
		var header = "";
		for(int i = 0; i < index; i++)
			header += "\t";
		System.out.println();
		System.out.println(header + "Name: " + getName().toUpperCase());
		System.out.println(header + "Position: " + getPosition().name().toUpperCase());
		System.out.println(header + "house: " + house.name().toUpperCase());
		System.out.println();
		System.out.println(header + "Playable: " + playable);
		System.out.println(header + "Selectable: " + selectable);
		System.out.println();
		System.out.println(header + "Is enemy: " + isEnemy);
		System.out.println(header + "Is new: " + isNew);
		System.out.println(header + "Controlled: " + controlled);
		System.out.println(header + "Ready:" + ready);
		if(leftNeighbor != null) {
			System.out.println(header + "Left Neighbor: " + leftNeighbor.name);
		}		
		if(rightNeighbor != null) {
			System.out.println(header + "Right Neighbor: " + rightNeighbor.name);
		}
		System.out.print(header);
		for(int i = 0; i < 35 ; i++)
			System.out.print("-");
		System.out.println();
	}
	protected boolean hasCAbil(String abil) {
		return abilities.stream().anyMatch(x -> x.isConst() && x.getName() != null && x.getName().equals(abil));
	}
	protected boolean hasAbility(String name) { 
		return abilities.stream().anyMatch(x -> x != null && x.effectType != null && x.effectType.name().equals(name));
	}
	
	public KFAbility getAbility(String name) {
		var abil =  getAbilities(name);
		if(abil.size() > 0)
			return abil.get(0);
		else return null;
	}
	public List<KFAbility> getAbilities(String name) {
		return abilities.parallelStream()
				.filter(x -> x != null && !x.isConst() && x.effectType != null && x.effectType.name().equals(name)).collect(Collectors.toList());
		
	}
	public void updateAfterAction() {
		ready = false;
		exhausted = true;
		playable = false;
	}
	
	public void updateByJSON(JSONObject obj, boolean isOpponent) { 
		isEnemy    = isOpponent;
		playable   = coalesce(getValueFromClassAndJSON(obj, Boolean.class, "canPlay"), playable);
		position   = resolveFieldPosition(coalesce(getValueFromClassAndJSON(obj, String.class, "location"), position.name()));
		exhausted  = coalesce(getValueFromClassAndJSON(obj, Boolean.class, "exhausted"), exhausted);
		ready      = !exhausted;
		selectable = coalesce(getValueFromClassAndJSON(obj, Boolean.class, "selectable"), selectable);
		selected   = coalesce(getValueFromClassAndJSON(obj, Boolean.class, "selected"), selected);
	}
	
	public boolean hasAction() {
		return hasAbility("action");
	}
	public boolean hasOmni() {
		return hasAbility("omni");
	}
	public boolean hasPlay() {
		return hasAbility("play");
	}
	public boolean hasReap() {
		return hasAbility("reap");
	}
	public boolean isNew() { return this.isNew; }
	public void setIsNew(boolean isNew) { this.isNew = isNew; }
	public boolean isControlled() { return this.controlled; }
	public void setControlled(boolean controlled) { this.controlled = controlled; }
	public void ready() {
		ready = true;
		exhausted = false;
		playable = true;
	}

}