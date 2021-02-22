package keyforge;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.HashMap;
import gameUtils.Utils;
import gameUtils.Utils.FieldPosition;

public class KFCreature extends KFCard {
	protected int lifePoints;
	private int attack;
	private int shield;
	private int dArmor;
	protected List<String> types;
	protected List<KFUpgrade> upgrades;
	protected int damage;
	protected boolean taunt;
	protected boolean stunned;
	protected boolean ward;
	private int hasCaptured;
	
	public KFCreature(String name, Utils.House house) {
		super(name, house);
		types = new ArrayList<String>();
	}
	public KFCreature(KFCreature card)
	{
		super(card);
		this.lifePoints = card.getLifePoints();
		this.attack = card.getAttack();
		this.shield = card.getShield();
		this.types = card.getTypes();
		this.upgrades = new ArrayList<KFUpgrade>(); 
		for(var upgrade: upgrades) {
			this.upgrades.add(new KFUpgrade(upgrade));
		}
		this.damage = card.getDamage();
		this.taunt = card.getTaunt();
		this.stunned = card.getStunned();
		this.ward = card.getWard();
	}
	public int getCaptured() { return this.hasCaptured; }
	public int getLifePoints() { return this.lifePoints; }
	public int getAttack() { return this.attack; }
	public int getShield() { return this.shield; }
	public List<String> getTypes() { return this.types; }
	public List<KFUpgrade> getUpgrades() { return this.upgrades; }
	public int getDamage() { return this.damage; }
	public boolean getTaunt() { return this.taunt; }
	public boolean getStunned() { return this.stunned; }
	public boolean getWard() { return this.ward; }
	/***
	 * Invoke only with target cards.
	 */
	public KFCreature() {
		types = new ArrayList<String>();
		upgrades = new ArrayList<KFUpgrade>();
	}

	/**
	 * This method should be called before each event, 
	 * to increase the efficiency of the bot
	 */
	public double evaluateUtility(String move) {
		return 0;
	}
	public void assignBlock(String[] block) {
		
		switch(block[0].toLowerCase()) {
		case "amber": this.amber = Integer.parseInt(block[1]); break;
		case "ability": abilities.add(new KFAbility(this, block)); break;
		case "type": types.add(block[1]); break;
		case "attack": 
			attack = Integer.parseInt(block[1]);
			lifePoints = (lifePoints == 0) ? attack : lifePoints;
			break;
		case "shield":	shield = Integer.parseInt(block[1]); break;
		default : System.out.println("Case not implemented yet..."); break;
		}
		
	}
	public void setCaptured(int capture) {
		this.hasCaptured += capture;
	}
	public int destroy() {
		var toReturn = hasCaptured;
		hasCaptured = 0;
		damage = 0;
		taunt = false;
		stunned = false;
		exhausted = false;
		ready = false;
		ward = false;
		position = Utils.FieldPosition.discard;
		upgrades.stream().forEach(x -> x.position = Utils.FieldPosition.discard);
		if(leftNeighbor != null) leftNeighbor.rightNeighbor = rightNeighbor;
		if(rightNeighbor != null) rightNeighbor.leftNeighbor = leftNeighbor;
		leftNeighbor = null;
		rightNeighbor = null;
		return toReturn;
	}
	public void returnHand() {
		this.damage = 0;
		this.hasCaptured = 0;
		this.taunt = false;
		this.stunned = false;
		this.exhausted = false;
		this.ready = false;
		this.ward = false;
		upgrades.stream().forEach(x -> x.position = Utils.FieldPosition.discard);
		this.position = Utils.FieldPosition.hand;
	}
	public void updateAfterPlay() {
		this.position = Utils.FieldPosition.playarea;
		var cond = this.abilities.stream().anyMatch(x -> x.isConst() && x.getName() != null && x.getName().equals("straight-ready"));
		this.exhausted = !cond;
		this.ready = cond;
		this.playable = cond;	
		this.isNew = true;
		this.stunned = this.hasCAbil("stun");
	}
	public void updateAfterPlay(KFCreature neighbour, String flank) {
		this.updateAfterPlay();
		if(!KFCreature.class.isInstance(neighbour))
			return;
		switch(flank) {
		case "left":
			neighbour.leftNeighbor = this;
			this.rightNeighbor = neighbour;
			break;
		case "right":
			neighbour.rightNeighbor = this;
			this.leftNeighbor = neighbour;
			break;
		default:
			System.out.println("Flank not recognized");
		}
		this.spreadAbilities();
		neighbour.spreadAbilities();
	}
	private void spreadAbility(KFAbility abil) {
		if(this.leftNeighbor != null) 
			this.leftNeighbor.abilities.add(abil);
		if(this.rightNeighbor != null)
			this.rightNeighbor.abilities.add(abil);
		
	}
	private void spreadAbilities() {
		var spreadableAbilities = this.abilities.stream()
				.filter(x -> this.equals(x.getOwner()) && x.isSpreadable())
				.collect(Collectors.toList());
		for(var abil : spreadableAbilities)
			spreadAbility(abil);
	}
	/**
	 * After any move and at the end of every turn the condition of an effect to be applicable must be checked
	 */
	private void checkAbilities() {
		//creo lista di predicati per eliminare?
		//TODO 
	}
	
	public void updateAfterPlay(KFCreature leftNeighbour, KFCreature rightNeighbour) {
		this.updateAfterPlay();
		//creature deployed
		if(leftNeighbor != null) {
			this.leftNeighbor = leftNeighbour;
			leftNeighbour.rightNeighbor = this;
			leftNeighbour.spreadAbilities();
		}
		if(rightNeighbor != null) {
			this.rightNeighbor = rightNeighbour;
			rightNeighbour.leftNeighbor = this;
			rightNeighbour.spreadAbilities();	
		}
		this.spreadAbilities();
	}
	private int evalEffectNameWithVal(String name) {
		var abi = this.abilities.stream().filter(x -> x.isConst() && x.getName() != null && x.getName().contains(name)).findFirst().orElse(null);
		if(abi == null)
			return 0;
		return abi.getValue();
	}
	private int evalHazardous() {
		return this.evalEffectNameWithVal("hazardous");
	}
	private int evalAssault() {
		return this.evalEffectNameWithVal("assault");
	}
	//todo , add amber caputred etc
	public Map<String, Integer> updateAfterFight(KFCreature enemy) {
		Map<String,Integer> to_ret = new HashMap<>();
		to_ret.put("friend", 0);
		to_ret.put("enemy", 0);
		this.exhausted = true;
		this.ready = false;
		this.playable = false;
		//check stunned
		if(this.stunned) {
			this.stunned = false;
			return to_ret;
		}
		//effect eval : hazardous and assault
		this.damage += enemy.evalHazardous();
		enemy.damage += evalAssault();
		
		boolean cond = false;
		
		if(this.damage >= this.lifePoints) {
			var value = destroy();
			to_ret.replace("enemy", value);
			cond = true;
		}	
		if(enemy.damage >= enemy.lifePoints) {
			var value = enemy.destroy();
			to_ret.replace("friend", value);
			return to_ret;
		}
		if(cond) return to_ret;
		//ward
		
		//hype
		
		//elusive
		if(enemy.elusiveFight()) return to_ret;
		//skirmish
		var skirmish = this.hasSkirmish();
		//fight phase
		
		var dArmor =  (enemy.getAttack() >= this.dArmor) ? 0 : this.dArmor - enemy.getAttack();
		this.dArmor = skirmish ? this.dArmor : dArmor;
		var damage_received = (enemy.getAttack() > this.dArmor) ? enemy.getAttack() - this.dArmor : 0;
		damage = skirmish ? damage : (damage + damage_received);
		
		enemy.dArmor = getAttack() >= enemy.dArmor ? 0 : enemy.dArmor - getAttack();
		var damage_inflicted = (this.getAttack() > enemy.dArmor) ? this.getAttack() - enemy.dArmor : 0;
		
		enemy.damage += damage_inflicted; 
		if(this.damage >= this.lifePoints) {
			var value = destroy();
			to_ret.replace("enemy", value);
		}
		if(enemy.damage >= enemy.lifePoints) {
			var value = enemy.destroy();
			to_ret.replace("friend", value);
		}
		return to_ret;
	}
	protected boolean hasCAbil(String abil) {
		var cond = super.hasCAbil(abil);
		if(cond) return true;
		for(var upgrade : upgrades)
			if(upgrade.hasCAbil(abil)) return true;
		return false;
	}
	protected boolean hasAbility(String name) { 
		var cond = super.hasAbility(name);
		if(cond) return true;
		for(var upgrade : upgrades)
			if(upgrade.hasAbility(name)) return true;
		return false;
	}
	public boolean hasDeploy() {
		return hasCAbil("deploy");
	}
	public boolean hasSkirmish() {
		return hasCAbil("skirmish");
	}
	public boolean hasTaunt() {
		return hasCAbil("taunt");
	}
	public boolean hasDestroyed() {
		return hasAbility("destroyed");
	}
	public boolean hasFight() {
		return hasAbility("fight");
	}
	public List<KFAbility> getAbilities(String name) {
		var abilityList = super.getAbilities(name);
		for(var upgrade : upgrades) {
			abilityList.addAll(upgrade.getAbilities(name));
		}
		return abilityList;
	}
	public boolean elusiveFight() {
		var elus = abilities.stream().filter(x -> x.isConst() && x.getName() != null && x.getName().equals("elusive")).findFirst().orElse(null);
		if(elus == null)
			return false;
		if(!elus.isUsed()) {
			elus.setUsed(true);
			return true;			
		}
		return false;
	}
	public void attachUpgrade(KFUpgrade upgrade) {
		upgrade.playable = false;
		upgrade.position = FieldPosition.playarea;
		upgrades.add(upgrade);		
	}
	public int updateAfterDamage(int damage) {
		if(!this.position.equals(FieldPosition.playarea)) return 0;
		this.damage += damage;
		var cond = (this.lifePoints <= this.damage);
		if(cond) return destroy();
		return 0;
	}
	public void updateStunned() {
		this.exhausted = true;
		this.ready = false;
		this.playable = false;
		this.stunned = false;
	}
	public boolean isStunned() {
		return this.stunned;
	}
	public void updateCardAfterReap() {
		this.exhausted = true;
		this.ready = false;
		this.playable = false;
	}
	public void print(int index) {
		super.print(index);
		var header = "";
		for(int i = 0; i < index; i++)
			header += "\t";
		System.out.println(header + "Attack: " + getAttack());
		System.out.println(header + "Shield: " + getShield());
		System.out.println(header + "LifePoints: " + getLifePoints());
		System.out.println(header + "Armor: " + getDArmor());
		System.out.println(header + "Damage: " + getDamage());
		System.out.println(header +"Stunned: " + isStunned());
		System.out.println(header + "Has Taunt: " + hasTaunt());

		System.out.print(header);
		for(int i = 0; i < 35; i++)
			System.out.print("-");
		System.out.println();
	}
	public int getDArmor() { return this.dArmor; }
	public void setDArmos(int dArmor) { this.dArmor = dArmor; } 
	@Override
	public void resetModifiers() {
		dArmor = shield;
		isNew = FieldPosition.playarea.equals(position) ? false : true;
		//isUsed
		abilities.forEach(x -> x.setUsed(false));
		//V abilities -> resetModifiers();
	}
	public boolean entersStunned() {  return this.hasCAbil("stun") ;}
	public void heal(int healed) {
		if(healed >= 100) {
			heal();
			return;
		}
		damage = healed > damage ? 0 : damage - healed;
	}
	public void heal() {
		damage = 0;
	}
}
