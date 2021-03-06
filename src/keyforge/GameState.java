package keyforge;

import java.util.*;
import java.util.stream.*;

import org.json.*;

import gameUtils.LogFile;
import static gameUtils.LogFile.*;
import static gameUtils.Utils.*;

public class GameState {
	private int amber;
	private int enemyAmber;
	private int keyCost;
	private int enemyKeyCost;
	private int chains;
	private int enemyChains;
	private Map<String, Boolean> keysForged;
	private Map<String, Boolean> enemyKeysForged;
	private Player botPlayer;
	public static String enemyName;
	private List<KFCard> cardsInPlay;
	private List<KFCreature> toFight;
	private List<KFCreature> fightWith;
	
	public GameState(Player botPlayer) {
		this.botPlayer = botPlayer;
		amber = 0;
		enemyAmber = 0;
		keysForged = new HashMap<>();
		keysForged.put("red",false);
		keysForged.put("yellow",false);
		keysForged.put("blue",false);
		keyCost = 6;
		enemyKeyCost = 6;
		enemyKeysForged = new HashMap<>();
		enemyKeysForged.put("red",false);
		enemyKeysForged.put("yellow",false);
		enemyKeysForged.put("blue",false);
		cardsInPlay = new ArrayList<KFCard>();
		
	}
	/***
	 * TODO change to inline assignment
	 * TODO transition messages :)
	 * @param gameState
	 * @param enemyName
	 */
	public void update(JSONObject gameState, String enemyName) {
		var players = gameState.getJSONObject("players");
		var j_enemy = getValueFromClassAndJSON(players, JSONObject.class, enemyName);
		var j_bot   = getValueFromClassAndJSON(players, JSONObject.class, botPlayer.name);
		if(j_enemy != null && j_enemy.has("stats")) 
		{
			try 
			{
				enemyAmber = botPlayer.enemyAmber = coalesce(getValueFromClassAndJSON(j_enemy.getJSONObject("stats"), Integer.class, "amber"), botPlayer.enemyAmber);
				enemyChains = botPlayer.enemyChains = coalesce(getValueFromClassAndJSON(j_enemy.getJSONObject("stats"), Integer.class, "chains"), enemyChains);
				var enemyKeys =  getValueFromClassAndJSON(j_enemy.getJSONObject("stats"), JSONObject.class, "keys");
				if(enemyKeys != null)
					enemyKeysForged.replaceAll((x,y) -> coalesce(getValueFromClassAndJSON(enemyKeys, Boolean.class, x), enemyKeysForged.get(x)));
				botPlayer.enemyForgedKeys = Collections.frequency(enemyKeysForged.values(), true);
			} catch (Exception e) {
				System.out.println(e.getMessage());
				System.out.println("error, check update, j_enemy section");
				/*doesn't matter*/ 
			}
			
		}
		if(j_bot != null && j_bot.has("stats")) {
			try
			{
				amber = botPlayer.possessedAmber = coalesce(getValueFromClassAndJSON(j_bot.getJSONObject("stats"), Integer.class, "amber"), amber);
				chains = botPlayer.chains = coalesce(getValueFromClassAndJSON(j_bot.getJSONObject("stats"), Integer.class, "chains"), chains);
				var botKeys = getValueFromClassAndJSON(j_bot.getJSONObject("stats"), JSONObject.class, "keys");
				if(botKeys != null)
					keysForged.replaceAll((x,y) ->  coalesce(getValueFromClassAndJSON(botKeys ,Boolean.class, x), keysForged.get(x)));
				botPlayer.forgedKeys = Collections.frequency(keysForged.values(), true);
			} catch (Exception e) { 
				System.out.println(e.getMessage());
				System.out.println("error, check update, j_bot section");
				/*Doesn't matter*/ 
			}
		}
	}
	public void updateCardsInPlay() 
	{
		cardsInPlay = PlannedMove.cloneCards(botPlayer.deck);
		cardsInPlay.addAll(botPlayer.opponentDeck);
		cardsInPlay = cardsInPlay.stream()
				.filter(x -> x != null  && x.position != null &&
				(x.position.equals(FieldPosition.playarea) || x.position.equals(FieldPosition.hand) || x.position.equals(FieldPosition.discard)))
				.map(kfCard -> {
					return cloneCard(kfCard);
				}).collect(Collectors.toList());
		if (cardsInPlay.size() <= 0)
			return;
		// no need to perform other operations
		toFight = cardsInPlay.stream()
				.filter(x -> x.position.equals(FieldPosition.playarea) && x.isEnemy && KFCreature.class.isInstance(x))
				.map(x -> (KFCreature)x)
				.collect(Collectors.toList());
		fightWith = cardsInPlay.stream()
				.filter(x -> x.position.equals(FieldPosition.playarea) && !x.isEnemy && KFCreature.class.isInstance(x))
				.map(x -> (KFCreature)x)
				.collect(Collectors.toList());
	}
	
	private void assessCard(KFCard card, KFAbility abil) {
		List<KFCard> allMatches = new ArrayList<>();
		allMatches = PlannedMove.applyEffectFilter(abil.target, cardsInPlay);
		card.assessEffect(allMatches);
	}
	public House selectHouse() {
		List<KFCard> cardsToPlay = 
				cardsInPlay.stream()
					.filter(x -> x != null && !x.isEnemy && (x.position.equals(FieldPosition.playarea) || x.position.equals(FieldPosition.hand)))
				.collect(Collectors.toList());
		Map<House, Double> rating = new HashMap<>();
		for(var card : cardsToPlay) {
			this.assessCard(card);
			var rate = rateCard(card);
			var cond = rating.containsKey(card.house);
			if(cond) {
				var newRate = rating.get(card.house) + rate;
				rating.replace(card.house, newRate);
			} else {
				rating.put(card.house, rate);
			}
		}
		for(var house : rating.keySet()) {
			var modHand = 1.8 * botPlayer.deck.stream().filter(x -> x != null && x.house.equals(house) && x.position != null && x.position.equals(FieldPosition.hand)).count();
			var modDeck = 0.2 * botPlayer.deck.stream().filter(x -> x != null && x.house.equals(house) && x.position != null && x.position.equals(FieldPosition.deck)).count();
			var modPlayarea = 0.43 * botPlayer.deck.stream().filter(x -> x != null && x.house.equals(house) && x.position != null && x.position.equals(FieldPosition.playarea) && x.ready).count();
			var mod = rating.get(house) + modDeck + modHand + modPlayarea;
			rating.replace(house, mod);
		}
		try {
			var key = rating.entrySet().stream().max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey();
			LogFile.WriteLog(Severity.MESSAGE, "House chosen is: " + key.name());
			return key;
		} catch(Exception e) {
			LogFile.WriteLog(Severity.ERROR, "error while evaluating houses: ");
			LogFile.WriteLog(Severity.ERROR, e.getMessage());
			return null;
		}
	}
	public double rateCard(KFCard card) {
		if(KFCreature.class.isInstance(card)) {
			return rateCreature((KFCreature)card);
		} else if(KFAction.class.isInstance(card)) {
			return rateAction((KFAction)card);
		} else if(KFUpgrade.class.isInstance(card)) {
			return rateUpgrade((KFUpgrade)card);
		} else if(KFArtifact.class.isInstance(card))  {
			return rateArtifact((KFArtifact)card);
		}
		return 0;
	}
	private double rateArtifact(KFArtifact card) {
		// TODO Auto-generated method stub
		return 0;
	}

	private double rateUpgrade(KFUpgrade card) {
		// TODO Auto-generated method stub
		return 0;
	}

	private double rateAction(KFAction card) {
		double rating = 0;
		rating += card.amber;
		rating += evaluateEffect(card);
		return rating;
	}

	private double evaluateEffect(KFAction card) {
		return evaluatePlayEffect(card);
	}
	private void assessCard(KFCard card) {
		card.abilities.forEach(x -> this.assessCard(card, x));
	}
	private double evaluatePlayEffect(KFCard card) {
		var abil = card.abilities.stream()
				.filter(x -> x.effectType.equals(resolveType("play")))
				.findFirst()
				.orElse(null);
		if(abil == null)
			return 0;
		return ratingByAbil(abil);
	}
	//TODO
	private double ratingByAbil(KFAbility abil) {
		double rate = 0;
		if(abil.effect.realMatches.stream().count() == 0) ; //TODO
		var friendMatches = abil.effect.realMatches.stream()
				.filter(x -> !x.isEnemy).count();
		var enemyMatches = abil.effect.realMatches.stream()
				.filter(x -> x.isEnemy).count();
		var tmp = ratingByAbil(abil, friendMatches, enemyMatches);
		rate = (rate == 0 || tmp > rate ) ? tmp : rate;
		
		return rate;
	}
	private double ratingByAbil(KFAbility abil, long friendMatches, long enemyMatches) {
		double rating = 0;
		double modifier = 0;
		if(abil.effect.getName() == null)
			return 0;
		rating = (double) abil.effect.realMatches.size();
		switch(abil.effect.getName()) {
		case "play":
			modifier = (double) (-0.7 * enemyMatches - 0.5 * friendMatches);
			break;
		case "destroy":
			if(abil.effect.conds.contains("friend"))
				modifier = -0.5;
			else if (abil.effect.conds.contains("enemy"))
				modifier = 0.8;
			else
				modifier = (double) (0.7 * enemyMatches - 0.9 * friendMatches);
			break;
		case "damage":
			if(abil.effect.target.conds.contains("friend")) {	
				modifier = -1 *  ((friendMatches > abil.effect.target.tValue) ? abil.effect.target.tValue : friendMatches);
			}
			if(abil.effect.conds.contains("each_house")) {
				var matchesPerHouse = abil.effect.realMatches.stream().collect(Collectors.groupingBy(x -> x.house));	
			}
			break;
		case "exhaust":
			modifier = 1.1;
			modifier = abil.effect.target.conds.contains("enemy") ? 1.3 : modifier;
			modifier = abil.effect.target.conds.contains("friend") ? -1.3 : modifier;
			break;
		case "return_hand":
			modifier = 1.1;
			modifier = abil.effect.realMatches.stream().filter(x -> x.isEnemy).count() * 1.1;
			break;		
		case "ready":
			modifier = 1.5;
			modifier = (abil.effect.realMatches.stream().filter(x -> x.isEnemy).count() > 0) ? modifier * -1 : modifier;
			break;
		case "capture":
			modifier = 1.5 * (abil.effect.conds.contains("enemy") ? -1 : 1);
			break;
		case "ready_fight":
			modifier = 1.4 * 
				((abil.effect.realMatches.stream().anyMatch(x -> KFCreature.class.isInstance(x) && !x.isEnemy)) ? 1.1 : 0) *
				((abil.effect.realMatches.stream().anyMatch(x -> KFCreature.class.isInstance(x) && x.isEnemy)) ? 1.1 : 0);
			break;
		default:
			System.out.println("This has to be implemented: " + abil.effect.getName());
		}
		return rating * modifier;
	}
	
	private double rateCreature(KFCreature card) {
		//potential amber +1
		double rating = 1;
		if(card.position.equals(FieldPosition.playarea) && !card.exhausted) {
			//reap possibility
			rating += evaluateFightMove(card);
			rating += evaluateReapEffect(card);
			rating += 1;
		} else if(card.position.equals(FieldPosition.hand)) {
			rating += evaluatePlayEffect(card);
			rating += 0.20 * evaluateFightMove(card);
			rating += 0.20 * evaluateReapEffect(card);
			rating += card.amber;
		}
		rating += card.getAttack() * 0.17;
		rating += card.getLifePoints() * 0.25;
		rating += card.getDArmor() * 0.10;
		return rating;
	}

	private double evaluateReapEffect(KFCreature card) {
		var abil  = card.abilities.stream()
			.filter(x -> x.effectType.equals(resolveType("reap")))
			.findFirst()
			.orElse(null);
		if(abil == null)
			return 0;
		double rating = 0;
		double modifier = 0;
		switch(abil.effect.getName()) {
		case "archive":
			rating = 1;
			modifier = 0.2;	
			break;
		case "return_hand":
			if(abil.target.conds.contains("friend")) {
				rating = abil.effect.realMatches.stream().count();
				modifier = 0.6;
			}
			break;
		case "play":
			rating = (abil.effect.realMatches.size() > abil.effect.target.tValue) ? abil.effect.target.tValue : abil.effect.realMatches.stream().count();
			modifier = 1.2;
			break;
		case "capture":
			rating = (abil.effect.realMatches.size() > abil.effect.target.tValue) ? abil.effect.value : 0;
			modifier = 1.5;
			break;
		case "heal":
			rating = (abil.effect.realMatches.size() > abil.effect.target.tValue) ? abil.effect.target.tValue : abil.effect.realMatches.size();
			modifier = 1.3;
			break;
		case "exhaust":
			rating = (abil.effect.realMatches.size() > abil.effect.target.tValue) ? abil.effect.target.tValue : abil.effect.realMatches.size();
			modifier = 1.8;
			break;
		case "gain_ability":
			rating = abil.effect.realMatches.stream().filter(x -> x.abilities.size() == 0).count() * 1.2;
			rating += abil.effect.realMatches.stream().filter(x -> x.abilities.size() > 0).count() * 0.7;
			modifier = 1.1;
			break;
		case "ready":
			rating = abil.effect.realMatches.size();
			modifier = 1.3;
			break;
		default:
			System.out.println("This has to be implemented: " + abil.effect.getName());
		}
		return rating*modifier;
	}

	private double evaluateFightMove(KFCreature card) {
		double rate = 0;
		if(toFight == null || toFight.isEmpty())
			return 0;
			
		//cards weaker than mine
		var weakerCards = toFight.stream()
				.filter(x -> card.getAttack() > x.lifePoints && card.lifePoints > x.getAttack())
				.count();
		if(weakerCards > 0) {
			rate += weakerCards * 0.35;
			rate += evaluateFightEffect(card);
		}
		//cards stronger than mine
		var strongerCards = toFight.stream()
				.filter(x -> card.getAttack() <= x.lifePoints && card.lifePoints <= x.getAttack())
				.count();
		if(strongerCards > 0) {
			rate += strongerCards * 0.15;
			rate += evaluateDestroyedEffect(card);
		}
		
		return rate;
	}
	private double evaluateDestroyedEffect(KFCreature card) {
		KFAbility abil = card.abilities.stream()
			.filter(x -> 
				(x.effectType.equals(resolveType("destroyed"))))
			.findFirst()
			.orElse(null);
		if(abil == null)
			return 0;
		double rate = 0;
		switch(abil.effect.getName()) {
		
		default:
			System.out.println("This destroyed effect must be implemented : " + abil.effect.getName());
			break;
		}
		
		return rate;
	}

	private double evaluateFightEffect(KFCreature card) {
		KFAbility abil = card.abilities.stream()
			.filter(x -> 
				(x.effectType.equals(resolveType("fight"))))
			.findFirst()
			.orElse(null);
		if(abil == null)
			return 0;
		if(abil.effect == null)
		{
			System.out.println("Attention on card: " + card.name);
			return 0;
		}
		double rate = 0;
		double modifier = 0;
		switch(abil.effect.getName()) {
			case "exhaust":
				modifier = 1;
				if(abil.target.conds.contains("enemy")) {
					modifier = 1 * modifier;
					var effectiveness =  (toFight.size() > abil.effect.tNames.size()) ? abil.effect.tNames.size() : toFight.size();
					rate += modifier * effectiveness;
				}
				else if(abil.target.conds.contains("friend")) {
					modifier = -1 * modifier;
				}
				break;
			case "play":
				rate = 1;
				modifier = 1.5 * (abil.effect.target.tValue > abil.effect
						.realMatches.stream()
						.filter(x -> !x.isEnemy).count() ? abil.effect.realMatches.stream().filter(x -> !x.isEnemy).count() : abil.effect.target.tValue);
				break;
			case "capture":
				rate = 1;
				modifier = 1.5 * (abil.effect.target.tValue > abil.effect
						.realMatches.stream()
						.filter(x -> !x.isEnemy).count() ? 1 : 1);
				break;
			default:
				System.out.println("This has to be implemented: " + abil.effect.getName());
				break;
		}
		return rate * modifier;
		
		
	}
	
	
	public void setKeyCost(int keyCost) {
		this.keyCost= keyCost;
	}	
	public void setEnemyKeyCost(int enemyKeyCost) {
		this.enemyKeyCost= enemyKeyCost;
	}	
	public void setAmber(int amber) {
		this.amber = amber;
	}
	public void setEnemyAmber(int enemyAmber) {
		this.enemyAmber = enemyAmber;
	}
	public void setKeysForged(Map<String, Boolean> keysForged) {
		this.keysForged = keysForged;
	}
	public void setEnemyKeysForged(Map<String, Boolean> enemyKeysForged) {
		this.enemyKeysForged = enemyKeysForged;
	}
	public int getAmber() { return this.amber; }
	public int getEnemyAmber() { return this.enemyAmber; }
	public Map<String, Boolean> getKeysForged() { return this.keysForged; }
	public Map<String, Boolean> getEnemyKeysForged() { return this.enemyKeysForged; }
	public int getKeyCost() { return this.keyCost; }
	public int getEnemyKeyCost() { return this.enemyKeyCost; }
	public String getEnemyName() { return enemyName; }
}
