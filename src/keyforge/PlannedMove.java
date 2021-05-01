package keyforge;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import static gameUtils.Utils.*;

public class PlannedMove {
	public List<PlannedMove> nextMoves;
	public PlannedMove previousMove;
	public PlannedMove selectedNextMove;
	public KFCard selectedCard;
	public String move;
	public Player botPlayer;
	public List<KFCard> initPool;
	public List<KFCard> possibleMoves;
	public List<KFCard> possibleTargets;
	public int targetToAssign;
	public List<KFCard> allTargets;
	public KFCard target;
	public List<KFCard> selectedTargets;
	public String selectedTargetsMove;
	public String flank;
	public int amberGained;
	public int potentialAmber;
	public House chosenHouse;
	public int depth;
	public int indexChosenPath;
	public boolean returnArchive;
	public final int maxDepth = 20 ;
	private int amberCaptured;
	private int enemiesDestroyed;
	private int friendsDestroyed;
	private int cardsInHand;
	private int enemyCardsInHand;
	private int chainGained;
	private int creaturesInPlay;
	private int enemyCreaturesInPlay;
	private int amberToForgeKey;
	private int amber;
	private int enemyAmber;
	private int keyCost;
	private int enemyKeyCost;
	public boolean effectUsed;
	public boolean firstTurn;
	public int upgradesAttached;
	public KFAbility selectedAbil;
	public int targetToSelect;
	public boolean endTurn;
	public boolean firstTurnSelected;
	private Effect selectedEffect;
	private int potentialEnemyAmber;
	private int nChains;
	private int oppNChains;
	private static List<Integer> hashCodes;
	private static int hits;
	private KFCard actionTarget;
	private Action actionPerformed;
	public boolean triggeredMove;
	
	public PlannedMove(Player botPlayer) {
		hashCodes = new ArrayList<>();
		nextMoves = new ArrayList<PlannedMove>();
		this.botPlayer = botPlayer;
		amber = botPlayer.possessedAmber;
		enemyAmber = botPlayer.enemyAmber;
		keyCost = 6;
		enemyKeyCost = 6;
		amberGained = 0;
		depth = 0;
		hits = 0;
		amber = botPlayer.possessedAmber;
		enemyAmber = botPlayer.enemyAmber;
		chainGained = 0;
		enemiesDestroyed = 0;
		friendsDestroyed = 0;
	}
	public int enemyPotentialAmberNextTurn() {
		return (int)possibleTargets.stream()
				.filter(x -> x != null 
					&& x.isEnemy 
					&& x.position != null 
					&& x.position.equals(FieldPosition.playarea)
					&& x.ready 
					&& !x.exhausted)
				.count();
	}
	public int getEnemyDistanceToGoal() {
		return enemyKeyCost - enemyAmber;
	}
	public int getDistanceToGoal() {
		return keyCost - amber;
	}
	public boolean keyAchievedNextTurn() {
		return getDistanceToGoal() <= 0;
	}
	public PlannedMove(PlannedMove pm) {
		previousMove = pm;
		initPool = cloneCards(pm.initPool);	
		nextMoves = new ArrayList<PlannedMove>();
		selectedTargets = new ArrayList<KFCard>();
		updatePossibleMovesRef();
		updateNeighborsRef();
		updatePossibleTargetsRef();
		keyCost = pm.keyCost;
		enemyKeyCost = pm.enemyKeyCost;
		botPlayer = pm.botPlayer;
		amberGained = 0;
		potentialAmber = 0;
		depth = pm.depth + 1;
		cardsInHand = pm.cardsInHand;
		amber = pm.amber;
		enemyAmber = pm.enemyAmber;
	}
	private void updateNeighborsRef() {
		for(var card : initPool) {
			if(card.leftNeighbor != null) {
				card.leftNeighbor = initPool.stream()
						.filter(x -> x.equals(card.leftNeighbor))
						.findFirst()
						.orElse(null);;
			}
			if(card.rightNeighbor != null) {
				card.rightNeighbor = initPool.stream()
						.filter(x -> x.equals(card.rightNeighbor))
						.findFirst()
						.orElse(null);
			}
		}
	}
	private void updatePossibleMovesRef() {
		possibleMoves = initPool.stream()
			.filter(x -> previousMove.possibleMoves.stream()
					.anyMatch((y -> y.equals(x))))
			.collect(Collectors.toList());
	}
	private void updatePossibleTargetsRef() {
		possibleTargets = initPool.stream()
				.filter(x -> previousMove.possibleTargets.stream()
						.anyMatch((y -> y.equals(x))))
				.collect(Collectors.toList()); 
	}
	
	public static List<KFCard> cloneCards(List<KFCard> cards) {
		var clonedCards = new ArrayList<KFCard>();
		if (cards == null)
			return clonedCards;
		for (var kfCard : cards) 
			clonedCards.add(cloneCard(kfCard));
		return clonedCards;
	}

	public int getCardsInDeck(House house) {
		return (int)botPlayer.deck.stream().filter(x -> x.position != null && x.position.equals(FieldPosition.deck) && x.house == house).count();
	}

	private void setCardsInHand() {
		cardsInHand = (int)botPlayer.deck.stream().filter(x -> x.position != null && x.position.equals(FieldPosition.hand)).count();
		//eval the one below
		enemyCardsInHand = (int)botPlayer.opponentDeck.stream().filter(x -> x.position != null && x.position.equals(FieldPosition.hand)).count();
	}
	private int getCardsToDraw() {
		return 6 - cardsInHand;
	}
	public void initTargets() {
		possibleTargets = initPool.stream()
				.filter(x -> x != null 
					&& x.position != null
					&& (x.position.equals(FieldPosition.playarea)
					|| x.position.equals(FieldPosition.discard)
					|| x.position.equals(FieldPosition.archives)
					|| x.position.equals(FieldPosition.hand)))
		.collect(Collectors.toList());
		possibleTargets.forEach(x -> x.selectable = true);
	}
	//first evaluation --> must be active house
	//second evaluation --> first evaluation w/ selected card updated
	//						add possible moves within a planned move
	protected void init() {
		chosenHouse = botPlayer.activeHouse;
		initPool();
		initCards();
		initTargets();
		setCardsInHand();
	}
	private void initPool() {
		var foo = botPlayer.deck.stream()
				.filter(x -> x.position 
						!= null 
						&& !x.position.equals(FieldPosition.deck))
				.collect(Collectors.toList());
		foo.addAll(botPlayer.opponentDeck
				.stream()
				.filter(x -> x.position != null 
					&& !x.position.equals(FieldPosition.deck))
				.collect(Collectors.toList()));
		initPool = cloneCards(foo);
	}
	/**
	 * First evaluation
	 */
	private void initCards() {
		possibleMoves = initPool.stream()
				.filter(x -> (chosenHouse == null || x.house.equals(chosenHouse)) 
				&& x != null
				&& x.position != null 
				&& !x.isEnemy
				&& (x.playable || x.selectable)
				&& (x.position.equals(FieldPosition.hand)
						|| x.position.equals(FieldPosition.playarea)
						|| x.position.equals(FieldPosition.archives)
						|| x.position.equals(FieldPosition.discard)))
				.collect(Collectors.toList());
		possibleMoves.addAll(initPool.stream()
				.filter(x -> x != null 
					&& x.isEnemy
					&& x.position != null
					&& x.isControlled() == true)
				.collect(Collectors.toList()));
		possibleMoves.addAll(initPool.stream()
				.filter(x -> x != null
						&& x.position != null 
						&& !x.isEnemy
						&& (x.playable || x.selectable)
						&& x.position.equals(FieldPosition.playarea)
						&& x.hasOmni())
				.collect(Collectors.toList()));
	}
	
	public void addPossibleCards(List<KFCard> cards, boolean isOpponent) {
		possibleMoves = cards.stream()
				.filter(x -> x != null && 
				x.position != null &&
				(x.position.equals(FieldPosition.hand) || 
						x.position.equals(FieldPosition.playarea) ||
						x.position.equals(FieldPosition.discard)))))
				.map(kfCard -> { return cloneCard(kfCard);})
				.collect(Collectors.toList());
		if(!isOpponent && selectedCard != null && !selectedCard.playable)
		{
			possibleMoves.removeIf(x->x.uuid.equals(selectedCard.uuid));
			chosenHouse = selectedCard.house;
			possibleMoves.removeIf(x->!x.house.equals(chosenHouse));
		}
	}
	
	public PlannedMove ignoreCardMove(KFCard card) {
		var pm = new PlannedMove(this);
		pm.move = "ignore";
		pm.selectCard(card);
		pm.selectedCard.updateCardAfterIgnore();
		return pm;

	}

	public PlannedMove discardCardMove(KFCard card) {
		var pm = new PlannedMove(this);
		pm.move = "discard";
		pm.cardsInHand--;
		pm.selectCard(card);
		pm.selectedCard.updateCardAfterDiscard();
		return pm;
	}

	public PlannedMove reapCardMove(KFCard card) {
		if (!KFCreature.class.isInstance(card))
			return null;
		var creature = (KFCreature) card;
		if (!creature.position.equals(FieldPosition.playarea) || creature.exhausted)
			return null;
		var pm = new PlannedMove(this);
		pm.move = "reap";
		pm.selectCard(card);
		((KFCreature)pm.selectedCard).updateCardAfterReap();
		pm.amberGained += 1;
		pm.evaluateEffect();
		return pm;
	}

	public PlannedMove fightCardMove(KFCard card) {
		if (!KFCreature.class.isInstance(card))
			return null;
		var creature = (KFCreature) card;
		if (!creature.position.equals(FieldPosition.playarea) || creature.exhausted)
			return null;
		var enemies = possibleTargets.stream()
				.filter(x -> KFCreature.class.isInstance(x) && x.isEnemy && x.position.equals(FieldPosition.playarea)).collect(Collectors.toList());
		if (enemies == null || enemies.isEmpty())
			return null;
		enemies = attackableEnemies(enemies);
		PlannedMove bestAttack = null;
		for (var enemy : enemies) {
			var pm = addEnemyFightCardMove(creature, enemy);
			if(bestAttack == null) {	
				bestAttack = pm; 
				continue; 
			}
			bestAttack = comparePlannedMoves(bestAttack, pm);
		}
		return bestAttack;
	}
	private List<KFCard> attackableEnemies(List<KFCard> enemies) {
		var attackable = new ArrayList<KFCard>();
		for(var card : enemies) {
			var creature = (KFCreature)card;
			if(creature.hasTaunt())	{
				attackable.add(creature);
				continue;
			}
			if(creature.leftNeighbor != null && ((KFCreature)creature.leftNeighbor).hasTaunt())	continue;
			if(creature.rightNeighbor != null && ((KFCreature)creature.rightNeighbor).hasTaunt()) continue;
			attackable.add(creature);
			
		}
		return attackable;
	}

	public PlannedMove compareFightMoves(PlannedMove pm1, PlannedMove pm2) {
		System.out.println("TODO");
		return null;
	}
	public PlannedMove addEnemyFightCardMove(KFCreature card, KFCard enemy) {
		if (!KFCreature.class.isInstance(enemy))
			return null;
		var pm = new PlannedMove(this);
		var pmEnemy = pm.possibleTargets.stream().filter(x -> x.equals(enemy)).findFirst().orElse(null);
		pm.move = "fight";
		var pmCreature = (KFCreature)pm.selectCard(card);
		var pmEnemyCreature = (KFCreature)pmEnemy;
		pm.target = pmEnemy;
		
		//before fight effect
		pm.evaluateEffect("before_fight");
		
		var captured = pmCreature.updateAfterFight(pmEnemyCreature);
		pm.amber += captured.get("friend");
		pm.enemyAmber += captured.get("enemy");
		
		if(pmCreature.position.equals(FieldPosition.discard)) {
			pm.friendsDestroyed += 1;
			pm.evaluateEffect("destroyed");
		} else if(pmCreature.position.equals(FieldPosition.playarea)) {
			pm.evaluateEffect();
		}
		if(pmEnemyCreature.position.equals(FieldPosition.discard)) {
			pm.enemiesDestroyed += 1;
		} else if(pmEnemyCreature.position.equals(FieldPosition.playarea)) {
			//TODO
		}
		return pm;

	}
	private void updatePossibleMoves() {
		possibleMoves.removeIf(x -> !x.playable);
	}

	public PlannedMove playCardMove(KFCard card) {
		if (!card.position.equals(FieldPosition.hand))
			return null;
		if (KFCreature.class.isInstance(card))
			return evaluateCreaturePosition(card);
		else if (KFAction.class.isInstance(card))
			return addActionPlayMove(card);
		else if (KFArtifact.class.isInstance(card))
			return addArtifactPlayMove(card);
		else if (KFUpgrade.class.isInstance(card))
			return addUpgradePlayMove(card);
		return null;
	}

	private PlannedMove addArtifactPlayMove(KFCard card) {
		var pm = new PlannedMove(this);
		pm.move = "play";
		pm.selectCard(card);
		pm.selectedCard.updateAfterPlay();
		pm.evaluateEffect();
		return pm;		
	}
	/***
	 * Check IT
	 * @param card
	 * @return
	 */
	public PlannedMove addUpgradePlayMove(KFCard card) {
		var cond = possibleTargets.stream().filter(
				x -> KFCreature.class.isInstance(x) && !x.isEnemy && x.position.equals(FieldPosition.playarea))
				.collect(Collectors.toList());
		if (cond == null || cond.isEmpty())
			return null;
		var upgrade = (KFUpgrade) card;
		PlannedMove bestAttach = null;
		for (var friend : cond) {
			var pm = attachUpgrade(upgrade, (KFCreature) friend);
			if(bestAttach == null) {
				bestAttach = pm;
				continue;
			}
			bestAttach = comparePlannedMoves(bestAttach, pm);
		}
		return bestAttach;
	}
	public PlannedMove compareAttachments(PlannedMove pm1, PlannedMove pm2) {
		System.out.println("TODO");
		return null;
	}
	public PlannedMove attachUpgrade(KFUpgrade upgrade, KFCreature creature) {
		var pm = new PlannedMove(this);
		pm.selectCard(upgrade);
		pm.move = "play";
		pm.upgradesAttached++;
		pm.target = creature;
		creature.attachUpgrade((KFUpgrade)pm.selectedCard);
		return pm;
	}

	public PlannedMove addActionPlayMove(KFCard card) {
		var action = (KFAction) card;
		var pm = new PlannedMove(this);
		pm.move = "play";
		pm.selectCard(action);
		pm.selectedCard.updateAfterPlay();
		pm.cardsInHand--;
		pm.evaluateEffect();
		return pm;
	}


	private PlannedMove evaluateCreaturePosition(KFCard card) {
		var creature = (KFCreature) card;
		var friendField = possibleTargets
				.stream().filter(x -> x != null && KFCreature.class.isInstance(x) && !x.isEnemy
						&& x.position != null && x.position.equals(FieldPosition.playarea))
				.collect(Collectors.toList());
		if (friendField.isEmpty()) {
			return addCardWithoutFlank(creature);
		} else 
			return evaluateFieldPosition(creature, friendField);
	}

	private PlannedMove evaluateFieldPosition(KFCreature card, List<KFCard> friends) {
		PlannedMove bestFlank = null;
		var flanks = new String[]{ "left", "right", "deploy-left", "deploy-right" };
		for (int i = 0; i < flanks.length; i++) {
			PlannedMove bestFriendPosition = null;
			for (var friend : friends) {
				var pm = addCardWithFlank(card, (KFCreature) friend, flanks[i]);
				if(bestFriendPosition == null) {
					bestFriendPosition = pm;
					continue;
				}
				bestFriendPosition = compareFriendPositions(bestFriendPosition, pm);
			}
			if(bestFlank == null) {
				bestFlank = bestFriendPosition;
				continue;
			}
			bestFlank = compareFriendPositions(bestFlank,bestFriendPosition);
		}
		return bestFlank;
	}

	private PlannedMove compareFriendPositions(PlannedMove pm1, PlannedMove pm2) {		
		if(pm1 == null && pm2 == null)
			return null;
		else if(pm1 == null)
			return pm2;
		else if(pm2 == null)
			return pm1;
		System.out.println("TODO");
		return pm1;
	}
	private PlannedMove addRight(KFCreature card, KFCreature friend) {
		if (friend.rightNeighbor != null)
			return null;
		var pm = new PlannedMove(this);
		pm.move = "play";
		pm.flank = "right";
		var creature_selected = (KFCreature)pm.selectCard(card);
		pm.potentialAmber += 1;
		var pmFriend = (KFCreature)pm.possibleTargets.stream()
				.filter(x -> x.equals(friend) && x.rightNeighbor == null)
				.findFirst().orElse(null);
		creature_selected.updateAfterPlay(pmFriend, pm.flank);
		pm.evaluateEffect();
		return pm;
	}
	private PlannedMove addLeft(KFCreature card, KFCreature friend) {
		if(friend.leftNeighbor != null)
			return null;
		var pm = new PlannedMove(this);
		pm.move = "play";
		pm.flank = "left";
		var creature_selected = (KFCreature)pm.selectCard(card);
		pm.potentialAmber += 1;
		var pmFriend = (KFCreature)pm.possibleTargets.stream()
				.filter(x -> x.equals(friend) && x.leftNeighbor == null)
				.findFirst().orElse(null);
		creature_selected.updateAfterPlay(pmFriend, pm.flank);
		pm.evaluateEffect();
		return pm;
	}
	private PlannedMove deployLeft (KFCreature card, KFCreature friend) { 
		if(!card.hasDeploy()) return null;
		var pm = new PlannedMove(this);
		pm.move = "play";
		pm.flank = "deploy left";
		pm.potentialAmber += 1;
		var selected_creature = (KFCreature)pm.selectCard(card);
		var pmFriend = (KFCreature)pm.possibleTargets.stream()
				.filter(x -> x.equals(friend))
				.findFirst().orElse(null);
		selected_creature.updateAfterPlay((KFCreature)pmFriend.leftNeighbor, pmFriend);
		pm.evaluateEffect();
		return pm;
	}
	
	private PlannedMove deployRight(KFCreature card, KFCreature friend) { 
		if(!card.hasDeploy()) return null;
		var pm = new PlannedMove(this);
		pm.move = "play";
		pm.flank = "deploy right";
		var selected_creature = (KFCreature)pm.selectCard(card);
		pm.potentialAmber += 1;
		var pmFriend = (KFCreature)pm.possibleTargets.stream()
				.filter(x -> x.equals(friend))
				.findFirst().orElse(null);
		selected_creature.updateAfterPlay(pmFriend, (KFCreature)pmFriend.rightNeighbor);
		pm.evaluateEffect();
		return pm;
	}
	
	private PlannedMove addCardWithFlank(KFCreature card, KFCreature friend, String flank) {
		switch (flank) {
		case "right": return addRight(card, friend);
		case "left": return addLeft(card, friend);
		case "deploy-left": return deployLeft(card, friend);
		case "deploy-right": return deployRight(card, friend);
		default:
			return null;
		}
	}

	public PlannedMove addCardWithoutFlank(KFCreature card) {
		var pm = new PlannedMove(this);
		pm.move = "play";
		pm.flank = "";
		pm.cardsInHand--;
		pm.selectCard(card);
		pm.evaluateEffect();
		((KFCreature)pm.selectedCard).updateAfterPlay();
		pm.potentialAmber += 1;
		return pm;
	}

	public boolean archiveCardMove() {
		if (!Player.nonEmptyArchive)
			return false;
		addArchiveToHand();
		doNotAddArchiveToHand();
		return true;
	}

	public void addArchiveToHand() {
		var pm = new PlannedMove(this);
		var archiveCards = pm.possibleMoves.stream()
				.filter(x -> x.position.equals(FieldPosition.archives)).collect(Collectors.toList());
		for (var card : archiveCards) {
			card.position = FieldPosition.hand;
		}
		pm.move = "archive";
		pm.returnArchive = true;
		nextMoves.add(pm);

	}

	public void doNotAddArchiveToHand() {
		var pm = new PlannedMove(this);
		var archiveCards = pm.possibleMoves.stream()
				.filter(x -> x.position.equals(FieldPosition.archives)).collect(Collectors.toList());
		for (var card : archiveCards) {
			card.position = FieldPosition.discard;
		}
		pm.move = "archive";
		pm.returnArchive = false;
		nextMoves.add(pm);

	}
	/***
	 * Da rivedere
	 * @param abil
	 * @param map
	 */
	public void bestTargetSelection (KFAbility abil, Map<KFCard,Integer> map) {
		map = map.entrySet().stream()
				.sorted(Map.Entry.comparingByValue())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue, LinkedHashMap::new));
		
		List<KFCard> targets = new ArrayList<KFCard>(map.keySet());
		if(!targets.isEmpty() && this.targetToAssign < targets.size())
			targets = targets.subList(0, this.targetToAssign);
		selectedTargets = targets;
		if(abil.effect.conds.contains("each_house")) {
			var val = (int) new ArrayList<KFCard>(map.keySet()).stream()
					.filter(distinctByKey(KFCard::getHouse))
					.count();
			var card = selectedTargets.get(0);
			for(int i = 1; i < val; i++)
				selectedTargets.add(card);
		} 
		if(abil.effect.conds.contains("each_friend")) {
			var val = (int) new ArrayList<KFCard>(map.keySet()).stream()
					.filter(distinctByKey(KFCard::getFriendPlaying))
					.count();
			var card = selectedTargets.get(0);
			for(int i = 1; i < val; i++)
				selectedTargets.add(card);
		}
		var cond = abil.target.conds.contains("auto");
		targetToSelect =  cond ? 0 : selectedTargets.size();
		targetToAssign = 0;
		this.updatePositions();
	}

	private void updatePositions() {
		switch(selectedEffect.getName()) {
		case "play_now":
			var card = selectedTargets.get(0);
			card.position = FieldPosition.hand;
			card.playable = true;
			possibleMoves.add(card);
			var pm = playCardMove(card);
			nextMoves.add(pm);
			targetToSelect = 1;
			return;
		case "exhaust":
			selectedTargets.stream().forEach(x -> x.ready = false);
			break;
		case "damage":
			if(selectedEffect.value == 0 && selectedEffect.sValue != null) {
				switch(selectedEffect.sValue) {
				case "doubled": 
					selectedTargets.stream()
					.filter(x -> KFCreature.class.isInstance(x))
					.map(creature -> (KFCreature)creature)
					.forEach(y -> y.updateAfterDamage(y.damage));
					break;
				default : System.out.println("Just another thing to implement");
				}
				break;
			}
			selectedTargets.stream()
			.filter(x -> KFCreature.class.isInstance(x))
			.map(creature -> (KFCreature)creature)
			.forEach(y -> y.updateAfterDamage(selectedEffect.value));
			break;
		case "capture":
			selectedTargets.stream()
			.filter(x -> KFCreature.class.isInstance(x))
			.map(creature -> (KFCreature)creature)
			.forEach(creature -> creature.setCaptured(selectedEffect.value));
			break;
		case "return_hand":
			selectedTargets.stream()
			.filter(x -> KFCreature.class.isInstance(x))
			.map(creature -> (KFCreature)creature)
			.forEach(creature -> creature.returnHand());
			break;
		case "play":
			break;
		case "destroy":
			selectedTargets.stream()
			.filter(x -> KFCreature.class.isInstance(x))
			.map(creature -> (KFCreature)creature)
			.forEach(creature -> {
				var amb = creature.destroy();
				this.amberGained += !creature.isEnemy ? amb : 0 ; 
				this.potentialEnemyAmber += creature.isEnemy ? amb : 0;
				this.enemiesDestroyed += creature.isEnemy ? 1 : 0;
				this.friendsDestroyed += !creature.isEnemy ? 1 : 0;
			});
			
			break;
		case "heal":
			selectedTargets.stream()
			.filter(x -> KFCreature.class.isInstance(x))
			.map(creature -> (KFCreature)creature)
			.forEach(creature -> creature.heal(selectedEffect.value));
			break;
		case "ready":
			selectedTargets.stream()
			.forEach(x -> x.ready());
			break;
		case "gain_amber":
			amberGained += selectedEffect.value;
			break;
		default:
			System.out.println("This effect " + selectedEffect.getName() + " under updatePositions() must be implemented ");
			break;
		}
		
	}
	public void readyFightTargetSelection() {
		KFCreature friendTarget = null, enemyTarget = null;
		var friend_cond = selectedEffect.realMatches.stream().anyMatch((x -> !x.isEnemy && KFCreature.class.isInstance(x)));
		
		if(!friend_cond) return;
		friendTarget = (KFCreature)selectedEffect.realMatches.stream()
			.filter(x -> !x.isEnemy && KFCreature.class.isInstance(x))
			.max(Comparator.comparing(x -> ((KFCreature)x).getAttack()))
			.get();
		
		var enemy_cond = selectedEffect.realMatches.stream().anyMatch((x -> x.isEnemy && KFCreature.class.isInstance(x)));
		if(!enemy_cond) return;
			enemyTarget = (KFCreature)possibleTargets.stream()
				.filter(x -> x.isEnemy && KFCreature.class.isInstance(x))
				.min(Comparator.comparing(x -> ((KFCreature)x).getLifePoints()))
				.get();
		
		friendTarget.updateAfterFight(enemyTarget);
		selectedTargets.add(friendTarget);
		selectedTargets.add(enemyTarget);
		targetToSelect =  selectedTargets.size();
		targetToAssign = 0;
	}
	private void playTargetSelection(KFAbility abil) {
		var tar = nextRealMatch(abil);
		if(tar == null) return;
		tar.playable = true;
		possibleMoves.add(tar);
	}
	private void reapTargetSelection() {
		if(selectedEffect.realMatches.size() < 0) return;
		selectedTargets = selectedEffect.realMatches;
		targetToSelect = selectedEffect.realMatches.size();
		PlannedMove oldPM = this;
		for(var tar : selectedTargets) {
			amberGained += 1;
		}
		
	}
	private void targetSelectionMove() {
		var abil = selectedCard.abilities.stream()
				.filter(x -> x.effectType.equals(resolveType(move)))
				.findFirst()
				.orElse(null);
		abil.assessEffect(selectedCard, allTargets);
		Map <KFCard, Integer> prioList = new HashMap<>();
		selectedEffect = abil.effect;
		selectedTargetsMove = abil.effect.getName();
		
		switch(selectedTargetsMove) {
		case "ready_fight":
			readyFightTargetSelection();
			return;
		case "reap":
			reapTargetSelection();
			return;
		case "play":
			playTargetSelection(abil);
			return;
		}
		for(var card : abil.effect.realMatches) {
			int prio = 0;
			switch(selectedTargetsMove) {
			case "play_now":
				prio = card.isEnemy ? 3 : 0;
				prio += card.hasPlay() ? 0 : 1;
				prio += card.hasReap() ? 0 : 1;
				break;
			case "damage":
				prio = card.isEnemy ? 0 : prio+2;
				prio = (card.isEnemy && ((KFCreature)card).lifePoints - abil.effect.value == 0) ? 0 : prio;
				prio = (!card.isEnemy && ((KFCreature)card).lifePoints - abil.effect.value == 0) ? prio+2 : prio;
				break;
			case "return_hand":
			case "capture":
			case "exhaust":
			case "destroy":
				prio = card.isEnemy ? 0 : prio+2;
				break;
			case "ready":
				prio = card.isEnemy ? 0 : prio+2;
				break;
			default:
				System.out.println(abil.effect.getName() + " Must be implemented");
				prio = 13;
				break;
			}
			prioList.put(card, prio);
		} 
		bestTargetSelection(abil, prioList);
	}
	
	
	private KFCard nextRealMatch(KFAbility abil) {
		if(abil == null) return null;
		if(abil.effect.realMatches == null || abil.effect.realMatches.size() == 0) return null;
		return abil.effect.realMatches.get(0);
	}
	private void selectNextMove() {
		if(this.move != null && this.move.equals("end"))
			return;
		if(this.possibleMoves == null || this.possibleMoves.isEmpty()) {
			plannedEndTurn();
			return;
		}
		if (depth == 0 && archiveCardMove()) {
			return;
		}
		if(nextMoves != null && nextMoves.size() > 0)
			return;
		evaluateBestMoves();
	}
	private PlannedMove plannedEndTurn() {
		var pm = new PlannedMove(this);
		pm.move = "end";
		pm.endTurn = true;
		nextMoves.add(pm);
		return pm;
	}

	public PlannedMove getBestMove(KFCard card) {
		if (!card.position.equals(FieldPosition.hand) && !card.position.equals(FieldPosition.playarea))
			return null;
		if (card.getName().equals("Three Fates")) {
			var pm = this.discardCardMove(card);
			return pm;
		}
		PlannedMove bestMove = null;
		if(card.position.equals(FieldPosition.playarea)) {
			if(checkStunned(card))
				return stunnedMove(card);
			bestMove = comparePlannedMoves(this.reapCardMove(card), this.fightCardMove(card));
			bestMove = comparePlannedMoves(bestMove, this.ignoreCardMove(card));
		} else {
			bestMove = comparePlannedMoves(this.playCardMove(card), this.discardCardMove(card));
			bestMove = comparePlannedMoves(bestMove, this.ignoreCardMove(card));

		}
		return bestMove;
	}
	private PlannedMove stunnedMove(KFCard card) {
		var pm = new PlannedMove(this);
		pm.move = "stun";
		var creature = (KFCreature)pm.selectCard(card);
		creature.updateStunned();
		pm.potentialAmber++;
		return pm;

	}
	private boolean checkStunned(KFCard card) {
		if(!KFCreature.class.isInstance(card))
			return false;
		return ((KFCreature)card).isStunned();
	}
	public void evaluateBestMoves() {
		for (var card : possibleMoves) {	
			if (!card.position.equals(FieldPosition.hand) && !card.position.equals(FieldPosition.playarea))
				continue;
			if (card.getName().equals("Three Fates")) {
				var pm = this.discardCardMove(card);
				nextMoves.add(pm);
				continue;
			}
			if(card.position.equals(FieldPosition.playarea)) {
				if(checkStunned(card)) {
					nextMoves.add(stunnedMove(card));
					continue;
				}
				var pm = reapCardMove(card);
				if(pm != null) nextMoves.add(pm);
				pm = fightCardMove(card);
				if(pm != null) nextMoves.add(pm);
				pm = actionCardMove(card);
				if(pm != null) nextMoves.add(pm);
			} else {
				var pm = playCardMove(card);
				if(pm != null) nextMoves.add(pm);
				pm = discardCardMove(card);
				if(pm != null) nextMoves.add(pm);
			}
			var pm = ignoreCardMove(card);
			nextMoves.add(pm);
			continue; 
		}
	}
	private PlannedMove actionCardMove(KFCard card) {
		if(!card.hasAction())
			return null;
		var pm = new PlannedMove(this);
		pm.selectCard(card);
		pm.move = "action";
		pm.selectedCard.updateAfterAction();
		pm.evaluateEffect();
		return pm;
	}
	
	private KFCard selectCard(KFCard card) {
		this.selectedCard = possibleMoves.stream().filter(x -> x.equals(card)).findFirst().orElse(null);
		if(selectedCard.position.equals(FieldPosition.hand))
				amberGained += selectedCard.amber;
		return this.selectedCard;
		
	}
	public static List<KFCard> applyEffectFilter(Target baseCard, List<KFCard> toFilter) {

		if (toFilter == null || toFilter.isEmpty()) {
			System.out.println("Filter list is empty");
			return null;
		}
		if(baseCard.isEmpty) {
			return null;
		}
		return toFilter.stream()
				.filter(baseCard.conditions.stream().reduce(x -> true, Predicate::and))
				.collect(Collectors.toList());

	}
	public void evaluateEffect(String name) {
		this.effectUsed = false;
		var abil = selectAbil(name);
		if(abil == null) return;
		selectedCard.assessCard();
		var matches = applyEffectFilter(abil.target, possibleTargets);
		if (matches == null || matches.isEmpty())
			return;
		allTargets = matches;
		//100 -> means automatic
		targetToAssign = (abil.target.tValue >= 100) ? 0 : abil.target.tValue;
		effectUsed = true;
		var autoTarget = abil.target.conds.contains("auto");
		if (this.allTargets != null && !this.allTargets.isEmpty() && (this.targetToAssign > 0 || autoTarget)) {
			this.targetSelectionMove();
		}
	}
	public KFAbility selectAbil(String name) {
		if (selectedCard == null || selectedCard.abilities == null || selectedCard.abilities.isEmpty())
			return null;
		var abil = selectedCard.abilities.stream()
				.filter(x -> x.effectType.name()
				.equals(name))
				.findFirst()
				.orElse(null);
		if (abil == null || abil.target.isEmpty)
			return null;
		return abil;
	}
	public void evaluateEffect() {
		evaluateEffect(move);
	}

	public int getTotalAmber() {
		if (nextMoves == null || nextMoves.isEmpty())
			return this.amberGained + this.potentialAmber;
		int i = 0;
		for (var x : nextMoves) {
			var childVal = x.getTotalAmber();
			if (childVal > this.amberGained) {
				this.amberGained = childVal;
				indexChosenPath = i;
			}
			i++;
		}
		return this.amberGained;
	}

	public void printPath() {
		if (this.selectedNextMove == null)
			return;
		if(selectedNextMove.selectedCard != null){	
			System.out.println("House chosen is " + selectedNextMove.selectedCard.getHouse());
			System.out.println(	"Card chosen is " + selectedNextMove.selectedCard.name + " with move " + selectedNextMove.move + " with index " + indexChosenPath);
		}
		selectedNextMove.printPath();	
	}

	public void recursiveSearch() {
		if (depth >= maxDepth)
			return;
		else if (depth == 0) {
			selectNextMove();
		}
		for (var nextPM : nextMoves) {			
			if(Player.firstTurn)	{
				nextPM.plannedEndTurn();
				continue;
			}
			if(nextPM.hitHash())
				continue;
			nextPM.updatePossibleMoves();
			nextPM.checkTriggeredMove();
			nextPM.selectNextMove();
			nextPM.recursiveSearch();
		}
	}
	
	private boolean hitHash() {
		List<String> toHash = new ArrayList<>();
		PlannedMove prevM = this;
		do {
			if(!prevM.move.equals("ignore")) {
				String cMove = prevM.move;
				if(prevM.selectedCard != null) {
					cMove += prevM.selectedCard.getUuid();
					cMove += prevM.selectedCard.getName();
					cMove += prevM.selectedCard.position;
					cMove += prevM.selectedCard.leftNeighbor != null ? prevM.selectedCard.leftNeighbor.getUuid() : "";
					cMove += prevM.selectedCard.rightNeighbor != null ? prevM.selectedCard.rightNeighbor.getUuid() : "";
					cMove += prevM.selectedCard.leftNeighbor != null ? prevM.selectedCard.leftNeighbor.getName() : "";
					cMove += prevM.selectedCard.rightNeighbor != null ? prevM.selectedCard.rightNeighbor.getName() : "";
				
				}
				cMove += prevM.flank != null ? prevM.flank : "";
				cMove += prevM.effectUsed;
				cMove += prevM.amberGained;
				cMove += prevM.amberCaptured;
				cMove += prevM.upgradesAttached;
				cMove += prevM.selectedEffect != null ? prevM.selectedEffect.getName(): "";
				cMove += prevM.selectedAbil != null  ? prevM.selectedAbil.getName() : "";
				cMove += prevM.target != null ? prevM.target.getUuid() + prevM.target.position : "";
				for(var tar : prevM.selectedTargets)
					cMove += tar.getUuid() + tar.position;
				toHash.add(cMove);
			}
			prevM = prevM.previousMove;
		}
		while(prevM != null && prevM.depth > 0);
		Collections.reverse(toHash);
		var code = String.join("", toHash).hashCode();
		if(hashCodes.contains(code)) {
			hits++;
			return true;
		}
		hashCodes.add(code);
		return false;
		
	}
	public String getCurrentMoveName() {
		if(this.selectedNextMove != null && this.selectedNextMove.move != null)
			return this.selectedNextMove.move;
		return null;
	}

	public PlannedMove getNextMove() {
		if (this.selectedNextMove != null)
			return this.selectedNextMove;
		return null;
	}
	public KFCard getCurrentTarget() {
		if(this.selectedNextMove != null && this.selectedNextMove.target != null)
			return this.selectedNextMove.target;
		return null;
	}
	public String getCurrentFlankToAssign() {
		if(this.selectedNextMove != null && this.selectedNextMove.flank != null)
			return this.selectedNextMove.flank;
		return null;
	}
	public KFCard getCurrentMoveCard() {
		if(this.selectedNextMove != null && this.selectedNextMove.selectedCard != null)
			return this.selectedNextMove.selectedCard;
		return null;
	}

	public void setNextMove() {
		this.selectedNextMove = this.selectedNextMove.selectedNextMove;
	}
	public void cleanIgnored() {
		if(selectedNextMove == null) 
			return;
		while(selectedNextMove.move.equals("ignore")) {
			selectedNextMove = selectedNextMove.selectedNextMove;
		}
		selectedNextMove.cleanIgnored();
	}
	public void setBestPathToGoal() {
		if(nextMoves == null || nextMoves.isEmpty()) {
			if(!move.equals("end"))
				selectedNextMove = plannedEndTurn();
			return;
		}
		for(var nextMove : nextMoves) {
			nextMove.setBestPathToGoal();
			if(this.selectedNextMove == null) { 
				selectedNextMove = nextMove;
				continue;
			}
			selectedNextMove = comparePlannedMoves(selectedNextMove, nextMove);
		}
		//add info of the move?
		this.amberCaptured += selectedNextMove.amberCaptured;
		this.amberGained += selectedNextMove.amberGained;
		this.cardsInHand = selectedNextMove.cardsInHand;
		this.enemiesDestroyed += selectedNextMove.enemiesDestroyed;
		this.potentialAmber += selectedNextMove.potentialAmber;
		this.friendsDestroyed += selectedNextMove.friendsDestroyed;
		this.cardsInHand = selectedNextMove.cardsInHand;
		this.enemyCardsInHand = selectedNextMove.enemyCardsInHand;
		this.chainGained += selectedNextMove.chainGained;
		this.amberToForgeKey = selectedNextMove.amberToForgeKey;
		this.amber = selectedNextMove.amber;
		this.enemyAmber = selectedNextMove.enemyAmber;
		this.potentialEnemyAmber = selectedNextMove.potentialEnemyAmber;
		this.nChains = selectedNextMove.nChains;
		this.oppNChains = selectedNextMove.oppNChains;
		/*****************************/
	}
	
	public List<KFCard> getTriggerables() {
		var dirtyTriggerables = initPool.stream().filter(x -> x != null && !x.isEnemy &&
				x.position != null && x.position.equals(FieldPosition.playarea)).collect(Collectors.toList());
		var triggerables = new ArrayList<KFCard>();
		for(var card : dirtyTriggerables) {
			if(card.abilities.stream().anyMatch(x -> x.isConst())) 
				triggerables.add(card);
		}
		return triggerables;
	}
	
	public void checkTriggeredMove() {
		var triggerables = getTriggerables();
		if(triggerables.isEmpty())
			return;
		for(var card : triggerables) {
			var abil = evaluateTriggerConditions(card);
			if(abil == null) continue;
			evaluateTriggerAbility(card, abil);
		}
		return;
	}
	
	public KFAbility evaluateTriggerConditions(KFCard card) {
		var cAbilities = card.abilities.stream().filter(x -> x.isConst()).collect(Collectors.toList());
		KFAbility triggeredAbil = null;
		for(var abil : cAbilities) {
			if(abil.getName() != null) continue;
			triggeredAbil = abil;
			break;
		}
		if(triggeredAbil == null) return null;
		if(!triggeredAbil.action.coditionSatisfied(card, this)) return null;
		card.assessCard();
		var eval = applyEffectFilter(triggeredAbil.target, initPool);
		if(eval == null || eval.isEmpty()) return null;
		
		return triggeredAbil;
	}
	
	public void evaluateTriggerAbility(KFCard card, KFAbility abil) {
		switch(abil.effect.getName()) {
		case "gain_amber":
			this.amberGained += abil.effect.value;
			break;
		case "damage":
			if(target != null && KFCreature.class.isInstance(target)) 
				((KFCreature)target).updateAfterDamage(abil.effect.value);
			if(!selectedTargets.isEmpty())
				selectedTargets.forEach(x -> ((KFCreature)x).updateAfterDamage(abil.effect.value));
		break;
		default:
			System.out.println("This triggered constant ability must be implemented");
			break;
		}
		//required action ? return true or false
		return;
	}
	
	public double rateMove() {
		double rate = 0;
		var toGoal = this.getDistanceToGoal();
		var enemyToGoal = this.getEnemyDistanceToGoal();
		potentialEnemyAmber = (int)initPool.stream().filter(x -> x!= null && x.isEnemy && x.position != null && x.position.equals(FieldPosition.playarea) && x.ready).count();
		var destroyMod = (potentialEnemyAmber > enemyToGoal) ? 0.8 : 0.4;
		rate = this.amberGained 
				+ (0.5 * this.amberCaptured) 
				+ (0.7 * this.potentialAmber) 
				+ (0.4 * this.amber)
				- (0.2 * this.chainGained)
				+ (destroyMod * this.enemiesDestroyed) 
				- (0.6 * this.friendsDestroyed)
				- (move.equals("fight") ? 0 : 0.3 * this.enemyPotentialAmberNextTurn())
				- (0.18 * enemyCreaturesInPlay)
				+ (1 - 0.1 * toGoal)
				- (1 - 0.2 * enemyToGoal)
				+ (KFCreature.class.isInstance(selectedCard) ? 1 : 0) 
				+ (this.getCardsToDraw() * 0.3) 
				+ (this.upgradesAttached * 0.6);
		return rate;
	}
	
	public PlannedMove comparePlannedMoves(PlannedMove pm1, PlannedMove pm2) {
		if(pm1 == null && pm2 == null)
			return null;
		else if (pm1 == null) 
			return pm2;
		else if (pm2 == null)
			return pm1;
		double r1 = pm1.rateMove(), r2 = pm2.rateMove();
			if(r1 >= r2) return pm1;
		else return pm2;
		
	}
	
	public KFCard nextTarget() {
		if(selectedTargets == null || selectedTargets.size() == 0)
			return null;
		var target = selectedTargets.get(0);
		selectedTargets.remove(0);
		targetToSelect = selectedTargets.size();
		return target;
	}
	
	public String printMove(int index) {
		String message;
		var endl = "\n";
		var padd = "    ";
		message  = padd + padd + index + "] Current move is " + move + endl;
		message += padd + "   Flank selected is:" + flank + endl;
		message += padd + "   Amber gained:" + amberGained + endl;
		message += padd + "   Potential amber:" + potentialAmber + endl;
		message += padd + "   Friends destroyed: " + friendsDestroyed + endl;
		message += padd + "   Enemy Destroyed: " + enemiesDestroyed + endl;
		message += endl;
		if(move.equals("archive")) 
			message += padd + "Return to hand archive: " + this.returnArchive + endl;
		if(selectedCard != null) {
			message += padd + "Selected Card is:" + endl;
			message += padd + selectedCard.print(1) + endl;
			if(move.equals("fight")) {
				message += padd + "\tTarget selected:" + endl;
				message += padd + target.print(2) + endl;
			}
			
		}
		message += padd + "\tThe ability of the move is used: " + this.effectUsed + endl;
		message += padd + "\tTarget to select are: " + this.targetToSelect + endl;
		message += padd + "\tEffect does :" + this.selectedTargetsMove + endl;
		for(int i = 0; i < targetToSelect; i++) {
			message += padd + "\t" + i + ") Target selected:" + endl;
			message += padd + selectedTargets.get(i).print(2) + endl;
		}
		if(selectedNextMove != null)
			message += padd + selectedNextMove.printMove(++index)+ endl;
		
		return message;
	}
	
	public String printMove() {
		return this.printMove(1);
	}
	
	public void printHits() {
		System.out.println("Hit counter :" + hits);
	}
}
