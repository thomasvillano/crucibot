package keyforge;

import static gameUtils.Utils.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONObject;
import gameUtils.LogFile;
import gameUtils.LogFile.Severity;

public class Player {
	private String dynamicDeckID;
	public String strategy;
	public String name;
	public List<String> deckHouses;
	public String password;
	public List<KFCard> deck, opponentDeck;
	public List<KFCard> targets;
	public KFCard selectedTarget;
	public KFCard selectedCard;
	public boolean selectCard;
	public String moveChosen;
	public PlannedMove plannedMoves;
	public int chains, enemyChains;
	private boolean cardSelection;
	private boolean cardSelected;
	private boolean flankSelection;
	private boolean targetSelection;
	private boolean targetSelected;
	public boolean activePlayer;
	private boolean attachUpgrade;
	private boolean fightTargetSelection;
	public PlannedMove currentMove;
	public House activeHouse;
	public int forgedKeys;
	public int enemyForgedKeys;
	public int possessedAmber;
	public int enemyAmber;
	private boolean casualMove;
	private boolean start = false;
	private boolean mulligan = false;
	public JSONArray controls;
	public JSONArray buttons;
	public String promptTitle;
	public String menuTitle;
	public String phase;
	public static boolean firstTurn;
	public static boolean nonEmptyArchive;
	private GameState gameState;

	public Player(String name, String password) {
		this.name = name;
		this.password = password;
		strategy = "amber";
		targets = new ArrayList<KFCard>();
		gameState = new GameState(this);
	}
	
	public String getDeck() { 
		return dynamicDeckID; 
	}
	
	public void setDeck(String deckID) {
		dynamicDeckID = deckID; 
	}
	
	public void updateGameState(JSONObject gameState, String enemyName) {
		this.gameState.update(gameState, enemyName);
		this.gameState.updateCardsInPlay();
	}
	
	public JSONArray forgeKey(JSONArray buttons) {
		return this.clickButton(0);
	}
	
	public boolean buttonEmpty() {
		return buttons.isEmpty();
	}
	
	public boolean controlsEmpty() {
		return controls.isEmpty();
	}
	
	private void checkFirstTurn() {
		if(menuTitle.contains("will go first") && menuTitle.contains(this.name))
			firstTurn = true;
	}
	
	private void resetModifiers() {
		deck.forEach(x -> x.resetModifiers());
		opponentDeck.forEach(y -> y.resetModifiers());
	}
	
	private void printPhaseState() {
		LogFile.WriteLog(Severity.INFO, "Phase: " + phase);
		LogFile.WriteLog(Severity.INFO, "Prompt Title: " + promptTitle);
		LogFile.WriteLog(Severity.INFO, "Menu Title: " + promptTitle);
		LogFile.WriteLog(Severity.INFO, "Buttons :" + coalesce(buttons, null));
		LogFile.WriteLog(Severity.INFO, "Controls:" + coalesce(controls, null));
	}
	
	public JSONArray planPhase() {
		printPhaseState();
		switch(phase) {
		case "setup":
			if(promptTitle != null && promptTitle.equals("Start Game")) {
				checkFirstTurn();
				return this.startGame();
			} else if(promptTitle != null && promptTitle.equals("Mulligan")) {
				return this.startGame();
			} 
			break;
		case "house":
			resetModifiers();
			this.plannedMoves = null;
			if(activePlayer && 
			(promptTitle == null || !promptTitle.toLowerCase().contains("archive"))) {	
				this.activeHouse = this.gameState.selectHouse();
				return this.chooseHouse();
			}
			else if (promptTitle.toLowerCase().contains("archive")) {
				buildTree();
				bestPathToGoal();
				currentMove = getNextMove();
				currentMove.printMove();
				var choice = currentMove.returnArchive ? "yes" : "no";
				return this.clickButton(choice);
			}
			break;
		case "main":
			if(plannedMoves == null) {
				buildTree();
				printHits();
				bestPathToGoal();
				firstTurn = false;
			}
			break;
		case "key":
			return this.forgeKey(buttons);
		default:
			break;
		}
		return this.genericMove();
	}
	
	public void updateUsingJSON(JSONObject player) {
		getButtons(player);
		getControls(player);
		getPhase(player);
		getPromptTitle(player);
		getMenuTitle(player);
		checkStartGame();
		getCards(player);
		if(!getIsActivePlayer(player))
			return;
		getHouse(player);
		checkMulligan();		
	}
	
	private JSONArray UpdateMoves() {
		resetState();
		buildTree();
		bestPathToGoal();
		resetState();
		casualMove = false;
		cardSelection = true;
		currentMove = getNextMove();
		return genericMove();
	}
	
	private JSONArray casualMove() {
		resetState();
		casualMove = true;
		
		if(promptTitle != null && promptTitle.toLowerCase().equals("play phase"))	
			return UpdateMoves();
		
		Random rand = new Random();
		var selectable = deck.stream().filter(x -> x.selectable && !x.selected).collect(Collectors.toList());
		selectable.addAll(opponentDeck.stream().filter(x -> x.selectable && !x.selected).collect(Collectors.toList()));
		
		if(buttons != null && buttons.length() > 0) {
			var choice = rand.nextInt(buttons.length());
			return this.clickButton(choice);
		} else if (selectable.size() != 0) {
			var choice = rand.nextInt(selectable.size());
			var card = selectable.get(choice);
			return this.selectCard(card);
		} else 
			return null;
	}
	
	private void resetState() {
		cardSelection = cardSelected = flankSelection = 
						targetSelection = targetSelected = 
						attachUpgrade = fightTargetSelection = false;
		if(currentMove != null)
			currentMove = getNextMove();
	}
	
	private String printState() {
		var blank_lines = "    ";
		var endline = "\n";
		var message = endline;
		message += blank_lines + "Card selection: " + cardSelection + endline;
		message += blank_lines + "Card selected: " + cardSelected + endline;
		message += blank_lines + "Flank selection: " + flankSelection + endline;
		message += blank_lines + "Target selection: " + targetSelection + endline;
		message += blank_lines + "Fight target selection " + fightTargetSelection + endline;
		message += blank_lines + "Attach upgrade" + attachUpgrade + endline;
		return message;
	}
	
	/***
	 * TODO improve..
	 * @return
	 */
	public JSONArray genericMove() {
		promptTitle = cleanString(promptTitle);
		
		if(currentMove == null) {
			currentMove = getNextMove();
			cardSelection = true;
		} 
		
		// if statement was else then the messages would not be printed out 
		if(currentMove != null) {
			LogFile.WriteLog(Severity.MESSAGE, "\n\nMoves\n");
			LogFile.WriteLog(Severity.MESSAGE, currentMove.printMove());
			LogFile.WriteLog(Severity.INFO, printState());
		}
		
		if(currentMove.move.equals("end"))
			return this.endTurn();
			
		if(menuTitle != null 
				&& menuTitle.contains("end"))
			return this.clickButton("no");
		
		if(promptTitle != null 
				&& promptTitle.toLowerCase().contains("triggered")) {
			if(!buttons.isEmpty())
				return clickButton("done");
			return casualMove(); 
		}
		
		if(cardSelection && !cardSelected) {
			if(promptTitle == null 
					|| !promptTitle.toLowerCase().equals("play phase"))
				return casualMove();
			cardSelected = true;
			return selectCard(currentMove.selectedCard); 
		} else if(cardSelection 
				&& cardSelected) {
			if(promptTitle == null 
					|| !promptTitle.equals(currentMove.selectedCard.getName()))
				return casualMove();
			cardSelection        = cardSelected = false;
			flankSelection       = (currentMove.flank != null && !currentMove.flank.isEmpty());
			targetSelection      = (currentMove.targetToSelect > 0);
			attachUpgrade        = (currentMove.upgradesAttached > 0);
			fightTargetSelection = (currentMove.move.equals("fight") && currentMove.target != null);
			return clickButton(currentMove.move);
		} else if (flankSelection) {
			flankSelection = false;
			return clickButton(currentMove.flank);
		} else if(attachUpgrade || fightTargetSelection) {
			attachUpgrade = fightTargetSelection = false;
			if(!currentMove.target.selectable)
				return casualMove();
			return selectCard(currentMove.target);
		} else if(targetSelection) {
			if(selectedTarget == null || targetSelected) {
				selectedTarget = currentMove.nextTarget();
				targetSelected = false;
			}
			if(selectedTarget != null && !this.targetSelected) {
				targetSelected = true;
				return selectCard(selectedTarget);
			}
			targetSelection = false;
			var button = clickButton("done");
			if(button != null)
				return button;
			LogFile.WriteLog(Severity.ERROR, "Unable to click any of the buttons because they were empty");
		} 
		if(!casualMove && (promptTitle != null && promptTitle.toLowerCase().equals("play phase"))) {
			resetState();
			cardSelection = true;
			return genericMove();
		} else
			return casualMove();
	}
	
	private PlannedMove getNextMove() {
		this.plannedMoves = this.plannedMoves.selectedNextMove;
		return this.plannedMoves;
	}
	
	public void printCardProperties(KFCard card) {
		System.out.print("Card : " + card.getName());
		if(card.playable) 
			System.out.print(" is playable and it is");
		else System.out.print(" is NOT playable and it is");
		if(card.selectable)
			System.out.println(" selectable.");
		else
			System.out.println(" NOT selectable.");	
	}
	
	public int selectFlank(JSONArray buttons, String flank) {
		int i = 0;
		for(var obj: buttons) {
			var line = (JSONObject)obj;
			var text = line.getString("text");
			if(text.toLowerCase().equals(flank))
				return i;
			i++;
		}
		return i;
	}
	
	public void printHits() {
		plannedMoves.printHits();
	}
	
	public void buildTree() {
		plannedMoves = new PlannedMove(this);
		plannedMoves.init();
		plannedMoves.recursiveSearch();
	}
	
	public void bestPathToGoal() {
		plannedMoves.setBestPathToGoal();
		plannedMoves.cleanIgnored();
	}
	
	public int evaluateAmberMoves() {
		return plannedMoves.getTotalAmber();
	}
	
	public void evaluateMovesFromStrategy() {
		switch (strategy) {
		case "amber":
			evaluateAmberMoves();
		default:
			break;
		}
	}
	
	private void updateNewDynamicIDs(JSONObject cardPiles, boolean isOpponent) {
		var positions = cardPiles.keys();
		while (positions.hasNext()) {
			var position = positions.next();
			if(cardPiles.get(position) instanceof JSONObject) {
				updatePosition(cardPiles.getJSONObject(position), position, isOpponent);
			}
			
		}
	}
	
	private void updatePosition(JSONObject cardPile, String position, boolean isOpponent) {
		if("hand".equals(position) && isOpponent)
			return;
		var keys = cardPile.keys();
		Map<KFCard, Integer> updatedIDs = new HashMap<KFCard, Integer>();
		while(keys.hasNext()) {
			var key = keys.next();
			if(!key.matches("\\d+"))
				continue;
			try {
				var id = Integer.parseInt(key);
				if(!(cardPile.get(key) instanceof JSONArray))
					continue;
				var obj = cardPile.getJSONArray(key).getJSONObject(0);
				if(!obj.has("uuid"))
					continue;
				var card = getCardFromJSONObject(obj, isOpponent);
				if(card == null) {
					System.out.println("Unable to retrieve card, sigh");
					continue;
				}
				card.updateByJSON(obj, isOpponent, position);
				card.setDynamicID(id);
				updatedIDs.put(card, id);
					
			} catch(Exception e) {
				System.out.println(e.getMessage());
				continue;
			}
		}
		// there could be an insertion ('_' means deletion, not sliding...)
		if(updatedIDs.size() <= 0)
			return;
		slideIndexes(updatedIDs, position, isOpponent);
	}
	
	private void slideIndexes(Map<KFCard, Integer> map, String pos, boolean isOpponent) {
		var deck = getDeck(isOpponent);
		var cards = deck.stream()
				.filter(x -> x.position == solveCruciblePosition(pos))
				.sorted(Comparator.comparingInt(KFCard::getDynamicID))
				.collect(Collectors.toList());
		int i = 0;
		
		for(var card : cards) {
			if(map.keySet().stream().anyMatch(x -> x.getUuid().equals(card.getUuid())))
				continue;
			
			if(map.containsValue(i)) {
				i = getFirstFreeID(map, i);
				card.setDynamicID(i);
				map.put(card, i);
				i++;
				continue;
			}
			card.setDynamicID(i);
			map.put(card, i);
			i++;
		}
	}
	
	private static int getFirstFreeID(Map<KFCard, Integer> map, int index) {
		List<Integer> values = new ArrayList<>(map.values());
		Collections.sort(values);
		values = values.subList(values.indexOf(index) + 1, values.size());
		int i = ++index;
		for(var value : values) {
			if(value == i) {
				i++;
				continue;
			}
			break;
		}
		return i;
	}
	
	private KFCard getCardFromJSONObject(JSONObject obj, boolean isOpponent) {
		return getCardFromNameAndUuid(
				coalesce(getValueFromClassAndJSON(obj, String.class, "name"), ""),
				coalesce(getValueFromClassAndJSON(obj, String.class, "uuid"), ""),
				isOpponent, 
				coalesce(getValueFromClassAndJSON(obj, Boolean.class, "controlled"), false));
	}

	private void getJSONObjectCardPile(JSONObject cardPile, String position, boolean isOpponent) {
		var keys = cardPile.keys();
		int intID = -1;
		while(keys.hasNext()) {
			var key = keys.next();
			if(key.startsWith("_"))
				continue;
			if(!(cardPile.get(key) instanceof JSONObject)) {
				LogFile.WriteLog(Severity.ERROR, cardPile.get(key).toString() + " is not an instance of JSONObject");
				continue;
			}
			try {
				intID = Integer.parseInt(key);
				updateCardByDynamicID(cardPile.getJSONObject(key), intID, position, isOpponent);
			} catch(NumberFormatException e) {
				LogFile.WriteLog(Severity.ERROR, key + " is not a valid number!");
				LogFile.WriteLog(Severity.ERROR, "unable to convert key");
			} catch (Exception e) {
				LogFile.WriteLog(Severity.ERROR, e.getMessage());
			}
			
		}
	}
	
	private List<KFCard> getDeck(boolean isOpponent) {
		return isOpponent ? opponentDeck : deck;
	}
	
	private void switchIndexes(List<Integer> indexes, String position, boolean isOpponent) {
		var cards = getDeck(isOpponent)
				.stream()
				.filter(x -> x.position == solveCruciblePosition(position))
				.sorted(Comparator.comparingInt(KFCard::getDynamicID))
				.collect(Collectors.toList());
		int i = 0;
		for(var card : cards) {
			if (indexes.contains(card.getDynamicID())) {
				continue;
			}
			card.setDynamicID(i);
			i++;
		}
	}
	
	private void updateCardByDynamicID(JSONObject jsonLine, int dynamicID, String position, boolean isOpponent) {
		if(isOpponent && (solveCruciblePosition(position) == FieldPosition.hand || solveCruciblePosition(position) == FieldPosition.archives))
			return;
		var deck = getDeck(isOpponent);
		KFCard card = deck.stream().filter(x -> x.dynamicIndexPosition == dynamicID && x.position == solveCruciblePosition(position)).findFirst().orElse(null);
		if(card == null) 
		{	
			LogFile.WriteLog(Severity.ERROR, "Unable to get card with dynamic id " + dynamicID + " while updating with it.\n" +
											 "Position: " + position + "\nOpponent: " + isOpponent + "\nJSONLine: " + jsonLine.toString(4));
			System.out.println("Error");
			return;
		}
		card.updateByJSON(jsonLine, isOpponent, position);
	}
	
	private KFCard updateCard(JSONObject jsonLine, String position, boolean isOpponent, int dynamicID, KFCreature neighbor) {
		var card = updateCard(jsonLine, position, isOpponent, dynamicID);
		if(card == null || neighbor == null || !(KFCreature.class.isInstance(card)))
			return card;
		neighbor.rightNeighbor = card; //could be null
		card.leftNeighbor = neighbor;
		return card;
	}
	
	private KFCard updateCard(JSONObject jsonLine, String position, boolean isOpponent, int dynamicID) {
		if (isOpponent && jsonLine.getBoolean("facedown"))
			return null;
		KFCard card = getCardFromJSONObject(jsonLine, isOpponent);
		if (card == null) {
			System.out.println("Error while linking card");
			return null;
		}
		card.updateByJSON(jsonLine, isOpponent, position);
		card.dynamicIndexPosition = dynamicID;
		if (!KFCreature.class.isInstance(card))
			return card;

		((KFCreature)card).upgrades = getUpgrades(jsonLine, isOpponent, position);
		return card;
	}
	
	/***
	 * @param cardPile
	 * @param position
	 * @param isOpponent
	 */
	private void getJSONArrayCardPile(JSONArray cardPile, String position, boolean isOpponent) {
		if(!isOpponent && position.equals("archives")) 
			nonEmptyArchive = cardPile.length() > 0;
		
		KFCreature prevCreature = null;
		int index = -1;
		for (Object line : cardPile) {
			index++;
			JSONObject jsonLine = (JSONObject) line;
			if (isOpponent && jsonLine.getBoolean("facedown"))
				break;
			var prevCard = updateCard(jsonLine, position, isOpponent, index, prevCreature);
			if(solveCruciblePosition(position) == FieldPosition.playarea && prevCard instanceof KFCreature)
				prevCreature = (KFCreature) prevCard;
		}
	}
	
	private void cleanPositions(JSONObject cardPiles, boolean isOpponent) {
		var positions = cardPiles.keys();
		while(positions.hasNext()) {
			var position = positions.next();
			if(!JSONObject.class.isInstance(cardPiles.get(position)))
				continue;
			cleanPosition(cardPiles.getJSONObject(position), position, isOpponent);
		}
	}
	
	private void cleanPosition(JSONObject cardPile, String position, boolean isOpponent) {
		var keys = cardPile.keys();
		List<Integer> removedIndexes = new ArrayList<Integer>();
		while(keys.hasNext()) {
			var key = keys.next();
			if(!key.matches("\\_\\d+"))
				continue;
			try {
				Pattern p = Pattern.compile("\\_(\\d+)");
				var m = p.matcher(key);
				m.find();
				var dynID = Integer.parseInt(m.group(1));
				removedIndexes.add(dynID);
			} catch(Exception e) {
				System.out.println(e.getMessage());
			}
		}
		if(removedIndexes.size() <= 0) 
			return;
		switchIndexes(removedIndexes, position, isOpponent);
		
	}
	
	private void updateCards(JSONObject cardPiles, boolean isOpponent) {
		var positions = cardPiles.keys();
		while (positions.hasNext()) {
			var position = positions.next();
			if(cardPiles.get(position) instanceof JSONArray) {
				getJSONArrayCardPile(cardPiles.getJSONArray(position), position, isOpponent);
			} else if (cardPiles.get(position) instanceof JSONObject) {
				getJSONObjectCardPile(cardPiles.getJSONObject(position), position, isOpponent);
			} else {
				System.out.println("Check me out");
			}
		}
	}
	public void convertCards(JSONObject cardPiles, boolean isOpponent) {
		cleanPositions(cardPiles, isOpponent);
		updateNewDynamicIDs(cardPiles, isOpponent);
		updateCards(cardPiles, isOpponent);
	}
	
	private List<KFUpgrade> getUpgrades(JSONObject jsonLine, boolean isOpponent, String position) {
		var upgradeArray = jsonLine.getJSONArray("upgrades");
		List<KFUpgrade> upgrades = new ArrayList<KFUpgrade>();
		for(var o : upgradeArray) {
			var jsonObj = (JSONObject)o;
			var card = getUpgradeFromNameAndUuid(jsonObj.getString("name"), jsonObj.getString("uuid"), isOpponent);
			if(card == null) 
				continue;
			card.updateByJSON(jsonObj, isOpponent, position);
			upgrades.add((KFUpgrade)card);
		}
		return upgrades;
	}

	public JSONArray clickButton(String name) {
		LogFile.WriteLog(Severity.MESSAGE, "Clicking button: " + name);
		var list = jsonArrayToList(buttons);
		var match = list.stream().filter(x -> x.toLowerCase().contains(name)).findFirst().orElse(null);
		if(match == null) {
			LogFile.WriteLog(Severity.ERROR, "Error while clicking button, button not found");
			return casualMove();
		}
		var index = list.indexOf(match);
		return clickButton(index);
	}
	
	public JSONArray clickButton(int index) {
		if (buttons == null || buttons.isEmpty())
			return casualMove();
		var jArray = new JSONArray().put("game").put("menuButton").put(buttons
				.getJSONObject(index).has("arg") ? buttons.getJSONObject(index).get("arg") : JSONObject.NULL)
				.put(buttons.getJSONObject(index).getString("uuid")).put(JSONObject.NULL);
		LogFile.WriteLog(Severity.MESSAGE, "Button Clicked: " + jArray);
		return jArray;
	}

	public JSONArray targetCard() {
		Map<KFCard, Double> playableCards = new HashMap<KFCard, Double>();
		for (var card : deck) {
			if (card.selectable && !targets.contains(card))
				playableCards.put(card, card.evaluateUtility(""));
		}
		if (playableCards.isEmpty()) {
			System.out.println("Cannot select any card.");
			return null;
		}
		selectedCard = playableCards.entrySet().stream()
				.max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey();
		targets.add(selectedCard);
		return new JSONArray().put("game").put("cardClicked").put(selectedCard.getUuid());

	}
	public JSONArray selectCard(KFCard card) {
		if(card == null) {
			LogFile.WriteLog(Severity.ERROR, "Error while selecting card, argument was null");
			LogFile.WriteLog(Severity.INFO, "Switching to casual move");
			return casualMove();
		}
		var jArr = new JSONArray()
				.put("game")
				.put("cardClicked")
				.put(card.getUuid());
		LogFile.WriteLog(Severity.MESSAGE, "Selecting card: " + card.name);
		LogFile.WriteLog(Severity.INFO, jArr.toString(4));
		return jArr;
	}

	public KFCard getCardFromNameAndUuid(String name, String uuid, boolean isOpponent, boolean controlled) {
		KFCard card = null;
		if((card = getCardFromUuid(uuid, isOpponent, controlled)) != null)
			return card;
		card = getCardFromName(name, isOpponent, controlled);
		if(card != null) {
			card.setUuid(uuid);
			return card;
		}
		LogFile.WriteLog(Severity.ERROR, "Error while adding card");
		LogFile.WriteLog(Severity.ERROR, "Name: " + name);
		LogFile.WriteLog(Severity.ERROR, "Uuid: " + uuid);
		LogFile.WriteLog(Severity.ERROR, "IsOpponent: " + isOpponent);
		LogFile.WriteLog(Severity.ERROR, "Controlled: " + controlled);
		System.out.println("Err!\tError while adding card");
		return null;	
	}
	
	/***
	 * TODO Clean
	 * @param name
	 * @param uuid
	 * @param isOpponent
	 * @return
	 */
	private KFCard getUpgradeFromNameAndUuid(String name, String uuid, boolean isOpponent) {
		var deck = getDeck(isOpponent);
		var filteredName = cleanString(name);
		
		var card = deck
				.stream()
				.filter(x -> x.getUuid() != null && x.equals(uuid))
				.findFirst()	
				.orElse(null);
		if(card != null) 
			return card;
		card = deck
				.stream()
				.filter(x -> x.getName().equals(filteredName) 
						&& (x.getUuid() == null || x.getUuid().isEmpty()))
				.findFirst()
				.orElse(null);
		if(card != null) {
			card.setUuid(uuid);
			return card;
		}
		return getUpgradeFromNameAndUuid(filteredName, uuid, !isOpponent);
	}

	public KFCard getCardFromName(String name, boolean isOpponent) {
		return this.getCardFromName(getDeck(isOpponent), name);
	}
	
	public KFCard getCardFromName(List<KFCard> deck, String name) {
		var filteredName = name.replace("’", "'");
		return deck.stream().filter(
				x -> x.name.toLowerCase().equals(filteredName.toLowerCase()) && (x.getUuid() == null || x.getUuid().isEmpty()))
				.findFirst().orElse(null);
	}
	
	public KFCard getCardFromUuid(List<KFCard> deck, String uuid) {
		return deck.stream().filter(x -> x.getUuid() != null && x.equals(uuid))
				.findFirst()
				.orElse(null);
	}
	
	public KFCard getCardFromUuid(String uuid, boolean isOpponent, boolean controlled) {
		var myDeck = ((isOpponent && !controlled) || (!isOpponent && controlled)) ? opponentDeck : deck;
		var card = getCardFromUuid(myDeck, uuid);
		if(card == null) {
			LogFile.WriteLog(Severity.WARNING, "Uuid not found: " + uuid);
			return null;
		}
		card.setControlled(controlled);
		return card;
	}
	
	public KFCard getCardFromName(String name, boolean isOpponent, boolean controlled) {
		var myDeck = ((isOpponent && !controlled) || (!isOpponent && controlled)) ? opponentDeck : deck;
		var card = this.getCardFromName(myDeck, name);
		if(card == null) {
			System.out.println("Card " + name + " not found");
			LogFile.WriteLog(Severity.ERROR, "Card not found : " + name);
			return null;
		}
		card.setControlled(controlled);
		return card;
	}

	public int evaluateHouse(JSONArray commands) {
		Map<House, Integer> map = new HashMap<House, Integer>();
		Map<House, Double> valueMap = new HashMap<House, Double>();
		for (Object houseIndex : commands) {
			var jsonLine = (JSONObject) houseIndex;
			var house = jsonLine.getString("text");
			var index = jsonLine.getInt("arg");
			map.put(resolveHouse(house), index);
			valueMap.put(resolveHouse(house), 0.0);
		}
		for (var card : deck) {
			if (card.position != FieldPosition.playarea && card.position != FieldPosition.hand) {
				continue;
			}
			valueMap.replace(card.house, valueMap.get(card.house) + card.evaluateUtility(""));
		}
		var key = valueMap.entrySet().stream().max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1)
				.get().getKey();
		var value = map.get(key);
		return value;
	}
	
	public int chooseMove(JSONArray buttons) {
		Map<String, Integer> moves = new HashMap<String, Integer>();
		Map<String, Double> movesValue = new HashMap<String, Double>();
		var i = 0;
		for (Object o : buttons) {
			var jsonLine = (JSONObject) o;
			var name = jsonLine.getString("text");
			moves.put(name, i);
			movesValue.put(name, 0.0);
		}
		return 0;
	}
	
	public JSONArray endTurn() {
		if(deck.stream().filter(x -> x.selectable || x.playable).count() > 0) {
			if(currentMove.endTurn) {
				currentMove.endTurn = false;
				return this.clickButton("end"); 
			} else {
				currentMove = plannedMoves = null;
				return this.clickButton("yes");
			}
		} else if(promptTitle != null && promptTitle.toLowerCase().equals("end turn")) {
			currentMove = plannedMoves = null;
			return this.clickButton("yes");
		}
		currentMove = plannedMoves = null;
		return this.clickButton("end");
	}

	public JSONArray startGame() {
		int index = 0;
		// always start, no need to mulligan and lose one card
		LogFile.WriteLog(Severity.MESSAGE, "Bot is starting game");
		return clickButton(index);
	}

	public JSONArray chooseHouse() {
		
		int index = 0;
		for(var obj: buttons) {
			var line = (JSONObject)obj;
			var text = line.getString("text");
			if(resolveHouse(text).equals(activeHouse)) {
				LogFile.WriteLog(Severity.MESSAGE, "Choosing house clicking button: " + activeHouse.name());
				return this.clickButton(index);
			}
			index++;
		}
		return null;
	}
	
	public void getCards(JSONObject obj) {
		if(obj.has("cardPiles"))
			convertCards(obj.getJSONObject("cardPiles"), false);
	}
		
	/**
	 * Check it
	 * @param obj
	 */
	public void getControls(JSONObject obj) {
		if(obj.has("controls")) {
			if (obj.get("controls") instanceof JSONArray) {
				controls = obj.getJSONArray("controls");
			} else if(obj.get("controls") instanceof JSONObject) {
				controls = BuildJSONArrayFromMap(obj.getJSONObject("controls"));
			} else {
				System.out.println("controls are not an instance of JSONArray, check out");
				LogFile.WriteLog(Severity.ERROR, "controls are not an instance of JSONArray nor JSONObject: " + obj.get("controls").toString());
			}
		}
	}
	
	private JSONArray BuildJSONArrayFromMap(JSONObject obj) {
		var instance = new JSONArray();
		var keys = obj.keys();
		while(keys.hasNext()) {
			var key = keys.next();
			if (key.startsWith("_")) 
				continue;
			var value = obj.get(key);
			if(value instanceof JSONArray) {
				instance.put(((JSONArray)value).get(0));
			} else {
				instance.put(value);
			}
		}
		return instance;
	}

	public void getButtons(JSONObject obj) {
		if(obj.has("buttons")) {
			if(obj.get("buttons") instanceof JSONArray) 
				buttons = obj.getJSONArray("buttons");
			else if(obj.get("buttons") instanceof JSONObject)
				buttons = BuildJSONArrayFromMap(obj.getJSONObject("buttons"));
			return;
		} 		
	}
	
	public void getPhase(JSONObject obj) {
		phase = coalesce(getValueFromClassAndJSON(obj, String.class, "phase" ), phase);
		if(phase == null)
			System.out.println("unable to retrieve phase from json: " + obj);
	}

	public void getMenuTitle(JSONObject player) {
		menuTitle = getValueFromClassAndJSON(player, String.class, "menuTitle");
		if (menuTitle == null)
			System.out.println("unable to retrieve menu title");	
		
	}
	public Boolean getIsActivePlayer(JSONObject player) {
		return activePlayer = coalesce(getValueFromClassAndJSON(player, Boolean.class, "activePlayer"), activePlayer);
	}

	public void getPromptTitle(JSONObject player) {
		promptTitle = getValueFromClassAndJSON(player, String.class, "promptTitle");
		if(promptTitle == null)
			System.out.println("unable to retrieve prompt title");
	}
	
	public void checkStartGame() {
		if(promptTitle != null && "start game".equals(promptTitle.toLowerCase()) && start) 
				return;
		start = true;
	}
	
	public void checkMulligan() {
		if(promptTitle != null && "mulligan".equals(promptTitle.toLowerCase()) && mulligan) 
			return;
		mulligan = true;
	}
	
	public House getHouse(JSONObject obj) {
		try {
			return activeHouse = coalesce(resolveHouse(getValueFromClassAndJSON(obj, String.class, "activeHouse")), activeHouse);
		} catch (Exception e) {
			System.out.println(e.getMessage());
			System.out.println("House not found");
			return null;
		}
	}
}
