package keyforge;

import gameUtils.Utils;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

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
	public Utils.House activeHouse;
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
	public void updateGameState(JSONObject gameState, String enemyName) {
		this.gameState.update(gameState, enemyName);
	}
	public JSONArray forgeKey(JSONArray buttons) {
		return this.clickButton(0);
	}
	public JSONArray playPlannedMoves() {
		if(currentMove == null) {
			currentMove = getNextMove();
			cardSelection = true;
		}
		this.promptTitle = (promptTitle != null) ? promptTitle.replace("’", "'") : null;
		return this.genericMove();	
	}
	public boolean buttonEmpty() {
		return buttons.isEmpty();
	}
	public boolean controlsEmpty() {
		return controls.isEmpty();
	}
	private void checkFirstTurn() {
		if(menuTitle.contains("first player") && menuTitle.contains(this.name))
			firstTurn = true;
	}
	private void resetModifiers() {
		deck.forEach(x -> x.resetModifiers());
		opponentDeck.forEach(y -> y.resetModifiers());
	}
	public JSONArray planPhase() {
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
			if(activePlayer && phase.equals("house") && 
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
		return this.playPlannedMoves();
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
		if(promptTitle.toLowerCase().equals("play phase"))	return UpdateMoves();
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
		cardSelection = false;
		cardSelected = false;
		flankSelection = false;
		targetSelection = false;
		targetSelected = false;
		attachUpgrade = false;
		fightTargetSelection = false;
		currentMove = getNextMove();
	}
	public JSONArray genericMove() {
		if(currentMove != null) {
			System.out.println("\n\nMOVEs\n\n");
			currentMove.printMove();
		}
		if(currentMove.move.equals("end"))
			return this.endTurn();
		if(menuTitle != null && menuTitle.contains("end"))
			return this.clickButton("no");
		if(promptTitle != null && promptTitle.toLowerCase().contains("triggered")) {
			if(buttons.isEmpty()) {
				resetState();
				casualMove = true;
				return casualMove(); 
			}
			return clickButton("done");
		}
		if(cardSelection && !cardSelected) {
			if(promptTitle == null || !promptTitle.toLowerCase().equals("play phase")){
				resetState();
				casualMove = true;
				return casualMove();
			}
			cardSelected = true;
			return this.selectCard(currentMove.selectedCard); 
		} else if(cardSelection && cardSelected) {
			if(promptTitle == null || !promptTitle.equals(currentMove.selectedCard.getName())) {
				resetState();
				casualMove = true;
				return casualMove();
			}
			cardSelection = false;
			cardSelected = false;
			flankSelection = (currentMove.flank != null && !currentMove.flank.isEmpty());
			targetSelection = (currentMove.targetToSelect > 0);
			attachUpgrade = (currentMove.upgradesAttached > 0);
			fightTargetSelection = (currentMove.move.equals("fight") && currentMove.target != null);
			return this.clickButton(currentMove.move);
		} else if (flankSelection) {
			this.flankSelection = false;
			return this.clickButton(currentMove.flank);
		} else if(attachUpgrade || fightTargetSelection) {
			attachUpgrade = false;
			fightTargetSelection = false;
			if(!currentMove.target.selectable) {
				resetState();
				casualMove = true;
				return casualMove();
			}
			return this.selectCard(currentMove.target);
		} else if(targetSelection) {
			if(selectedTarget == null || targetSelected) {
				selectedTarget = currentMove.nextTarget();
				targetSelected = false;
			}
			if(selectedTarget != null && !this.targetSelected) {
				targetSelected = true;
				return this.selectCard(selectedTarget);
			}
			targetSelection = false;
			var button = clickButton("done");
			if(button != null)
				return button;
		} 
		if(!casualMove && promptTitle.toLowerCase().equals("play phase")) {
			resetState();
			cardSelection = true;
			return this.playPlannedMoves();
		} else
		{
			resetState();
			casualMove = true;
			return casualMove();
		}
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
	public String getDeck() { return this.dynamicDeckID; }
	public void setDeck(String deckID) { this.dynamicDeckID = deckID; }

	// if position.playarea then counter = 0; prevCard = card ecc
	/**
	 * BIG MESS
	 * VERY BIG MESS
	 * @param cardPiles
	 * @param isOpponent
	 */
	public void convertCards(JSONObject cardPiles, boolean isOpponent) {
		var positions = cardPiles.keys();

		while (positions.hasNext()) {
			KFCard prevCard = null;
			var position = positions.next();
			if(!(cardPiles.get(position) instanceof JSONArray))
				continue;
			JSONArray cardsInPosition = cardPiles.getJSONArray(position);
			if(!isOpponent && position.equals("archives")) 
				nonEmptyArchive = cardsInPosition.length() > 0;
			for (Object line : cardsInPosition) {
				JSONObject jsonLine = (JSONObject) line;
				if (isOpponent && jsonLine.getBoolean("facedown"))
					break;
				Object canPlay = null;
				if (!isOpponent)
					canPlay = jsonLine.get("canPlay");
				KFCard card;
				var name = jsonLine.getString("name");
				var uuid = jsonLine.getString("uuid");
				var controlled = jsonLine.getBoolean("controlled");
				//System.out.println("Card " + name + "\n\tcontrolled\t" + controlled + "\n\tis opponent\t" + isOpponent);
				card = getCardFromNameAndUuid(name, uuid, isOpponent, controlled);
				if (card != null) {
					card.position = Utils.resolveFieldPosition(jsonLine.getString("location"));
					if (!isOpponent && canPlay != JSONObject.NULL)
						card.playable = jsonLine.getBoolean("canPlay");
					card.isEnemy = isOpponent;
					card.exhausted = jsonLine.getBoolean("exhausted");
					card.ready = !card.exhausted;
					card.selectable = jsonLine.getBoolean("selectable");
					card.selected = jsonLine.has("selected") ? jsonLine.getBoolean("selected") : false;
					var tokens = (JSONObject) jsonLine.get("tokens");
					if (jsonLine.getString("type").equals("creature")) {
						var kCard = (KFCreature) card;
						kCard.taunt = jsonLine.getBoolean("taunt");
						kCard.stunned = jsonLine.getBoolean("stunned");
						kCard.ward = jsonLine.getBoolean("wardBroken");
						kCard.upgrades = getUpgrades(jsonLine, isOpponent);
						kCard.damage = tokens.has("damage") ? tokens.getInt("damage") : 0;
						var amberC = tokens.has("amber") ? tokens.getInt("amber") : 0;
						kCard.setCaptured(amberC);
						//System.out.println("Damage set to " + kCard.damage + " and amber set to " + amberC + " for card " + name);
					}
					if(!isOpponent && card.position.equals(Utils.FieldPosition.hand)) {
						System.out.println("Controlled card in hand : \n");
						card.print(1);
					}
				} else
					System.out.println("Unable to get card " + name);
				if (position.equals("cardsInPlay")) {
					if (prevCard != null && KFCreature.class.isInstance(prevCard) && KFCreature.class.isInstance(card)) {
						prevCard.rightNeighbor = card;
						card.leftNeighbor = prevCard;
					}
					if(KFCreature.class.isInstance(card))
						prevCard = card;
				}
			}
		}
	}

	private List<KFUpgrade> getUpgrades(JSONObject jsonLine, boolean isOpponent) {
		var upgradeArray = jsonLine.getJSONArray("upgrades");
		List<KFUpgrade> upgrades = new ArrayList<KFUpgrade>();
		for(var o : upgradeArray) {
			var jsonObj = (JSONObject)o;
			var card = this.getUpgradeFromaNameAndUuid(jsonObj.getString("name"), jsonObj.getString("uuid"), isOpponent);
			if(card == null) continue;
			card.position = Utils.resolveFieldPosition(jsonObj.getString("location"));
			card.isEnemy = isOpponent;
			card.exhausted = jsonObj.getBoolean("exhausted");
			card.ready = !card.exhausted;
			card.selectable = jsonObj.getBoolean("selectable");
			if (!isOpponent && jsonLine.get("canPlay") != JSONObject.NULL)
				card.playable = jsonLine.getBoolean("canPlay");
			upgrades.add((KFUpgrade)card);
		}
		return upgrades;
	}

	public JSONArray selectMove(JSONArray buttons) {
		targets = new ArrayList<KFCard>();
		int index = 0;
		return new JSONArray().put("game").put("menuButton").put(
				buttons.getJSONObject(index).has("arg") ? buttons.getJSONObject(index).get("arg") : JSONObject.NULL)
				.put(buttons.getJSONObject(index).getString("uuid")).put(JSONObject.NULL);

	}
	public JSONArray clickButton(String name) {
		var list = Utils.jsonArrayToList(buttons);
		var match = list.stream().filter(x -> x.toLowerCase().contains(name)).findFirst().orElse(null);
		if(match == null)
			return null;
		var index = list.indexOf(match);
		return clickButton(index);
	}
	
	public JSONArray clickButton(int index) {
		/***
		 * TODO could be a JSONArray not a JSONObject, wrong retrievement method...
		 */
		return new JSONArray().put("game").put("menuButton").put(
				buttons.getJSONObject(index).has("arg") ? buttons.getJSONObject(index).get("arg") : JSONObject.NULL)
				.put(buttons.getJSONObject(index).getString("uuid")).put(JSONObject.NULL);
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
		selectedCard = playableCards.entrySet().parallelStream()
				.max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey();
		targets.add(selectedCard);
		return new JSONArray().put("game").put("cardClicked").put(selectedCard.getUuid());

	}
	public JSONArray selectCard(KFCard card) {
		return new JSONArray()
				.put("game")
				.put("cardClicked")
				.put(card.getUuid());
	}

	public JSONArray selectCard() throws Exception {
		Map<KFCard, Double> playableCards = new HashMap<KFCard, Double>();
		for (var card : deck) {
			if (card.playable)
				playableCards.put(card, card.evaluateUtility(""));
		}
		if (playableCards.isEmpty()) {
			System.out.println("Cannot select any card.");
			throw new Exception();
		}
		selectedCard = playableCards.entrySet().parallelStream()
				.max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey();
		return new JSONArray().put("game").put("cardClicked").put(selectedCard.getUuid());
	}
	public KFCard getCardFromNameAndUuid(String name, String uuid, boolean isOpponent, boolean controlled) {
		KFCard card = null;
		try {
			card = getCardFromUuid(uuid, isOpponent, controlled);
		} catch (Exception e) {
			card = getCardFromName(name, isOpponent, controlled);
			card.setUuid(uuid);
		} finally {
			if( card == null) {
				System.out.println("Err!\tError while adding card");
				return null;
			}
		}
		return card;	
	}
	private KFCard getUpgradeFromaNameAndUuid(String name, String uuid, boolean isOpponent) {
		var deck = isOpponent ? opponentDeck : this.deck;
		var filteredName = name.replace("â€™", "'");
		//look for it in deck
		var card = deck.stream().filter(x -> x.getUuid() != null && x.equals(uuid)).findFirst()	.orElse(null);
		if(card != null) return card;
		card = deck.stream().filter(x -> x.getName().equals(filteredName) && (x.getUuid() == null || x.getUuid().isEmpty())).findFirst().orElse(null);
		if(card != null) {
			card.setUuid(uuid);
			return card;
		}
		//else look for it in opponent deck 
		deck = isOpponent ? this.deck : opponentDeck;		
		card = deck.stream().filter(x -> x.getUuid() != null && x.equals(uuid)).findFirst()	.orElse(null);
		if(card != null) return card;
		card = deck.stream().filter(x -> x.getName().equals(filteredName) && (x.getUuid() == null || x.getUuid().isEmpty())).findFirst().orElse(null);
		if(card != null) {
			card.setUuid(uuid);
			return card;
		}
		
		else return null;
	}

	public KFCard getCardFromName(String name, boolean isOpponent) {
		var myDeck = isOpponent ? opponentDeck : deck;
		return this.getCardFromName(myDeck, name);
	}
	public KFCard getCardFromName(List<KFCard> deck, String name) {
		var filteredName = name.replace("’", "'");
		return deck.parallelStream().filter(
				x -> x.name.toLowerCase().equals(filteredName.toLowerCase()) && (x.getUuid() == null || x.getUuid().isEmpty()))
				.findFirst().orElse(null);
	}
	public KFCard getCardFromUuid(List<KFCard> deck, String uuid) throws Exception {
		return deck.parallelStream().filter(x -> x.getUuid() != null && x.equals(uuid))
				.findFirst()
				.orElseThrow(() -> new Exception("uuidNotFound"));
	}
	public KFCard getCardFromUuid(String uuid, boolean isOpponent) throws Exception {
		var myDeck = isOpponent ? opponentDeck : deck;
		return this.getCardFromUuid(myDeck, uuid);
				
	}
	public KFCard getCardFromUuid(String uuid, boolean isOpponent, boolean controlled) throws Exception {
		var myDeck = ((isOpponent && !controlled) || (!isOpponent && controlled)) ? opponentDeck : deck;
		var card = this.getCardFromUuid(myDeck, uuid);
		card.setControlled(controlled);
		return card;
	}
	public KFCard getCardFromName(String name, boolean isOpponent, boolean controlled) {
		var myDeck = ((isOpponent && !controlled) || (!isOpponent && controlled)) ? opponentDeck : deck;
		var card = this.getCardFromName(myDeck, name);
		if(card == null) {
			System.out.println("Card " + name + " not found");
			return null;
		}
		card.setControlled(controlled);
		return card;
	}

	public int evaluateHouse(JSONArray commands) {
		Map<Utils.House, Integer> map = new HashMap<Utils.House, Integer>();
		Map<Utils.House, Double> valueMap = new HashMap<Utils.House, Double>();
		for (Object houseIndex : commands) {
			var jsonLine = (JSONObject) houseIndex;
			var house = jsonLine.getString("text");
			var index = jsonLine.getInt("arg");
			map.put(Utils.resolveHouse(house), index);
			valueMap.put(Utils.resolveHouse(house), 0.0);
		}
		for (var card : deck) {
			if (card.position != Utils.FieldPosition.playarea && card.position != Utils.FieldPosition.hand) {
				continue;
			}
			valueMap.replace(card.house, valueMap.get(card.house) + card.evaluateUtility(""));

		}
		var key = valueMap.entrySet().parallelStream().max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1)
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
				currentMove = null;
				plannedMoves = null;
				return this.clickButton("yes");
			}
		} else if(promptTitle.toLowerCase().equals("end turn")) {
			currentMove = null;
			plannedMoves = null;
			return this.clickButton("yes");
		}
		currentMove = null;
		plannedMoves = null;
		return this.clickButton("end");
	}

	public JSONArray startGame() {
		int index = 0;
		return clickButton(index);
	}

	public JSONArray chooseHouse() {
		
		int index = 0;
		for(var obj: buttons) {
			var line = (JSONObject)obj;
			var text = line.getString("text");
			if(Utils.resolveHouse(text).equals(activeHouse))
				return this.clickButton(index);
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
			} else {
				if(obj.get("controls") instanceof JSONObject) {
					controls = BuildJSONArrayFromMap(obj.getJSONObject("controls"));
				} else {
					System.out.println("controls are not an instance of JSONArray");
				}
				
			}
		}
	}
	private JSONArray BuildJSONArrayFromMap(JSONObject obj) {
		var instance = new JSONArray();
		var _keys = obj.keys();
		while(_keys.hasNext()) {
			var _key = _keys.next();
			if (_key.startsWith("_")) 
				continue;
			var _value = obj.get(_key);
			if(_value instanceof JSONArray) {
				instance.put(((JSONArray)_value).get(0));
			} else {
				instance.put(_value);
			}
		}
		return instance;
	}
	/**
	 * Still a mess
	 * @param obj
	 */
	public void getButtons(JSONObject obj) {
		if(obj.has("buttons") && obj.get("buttons") instanceof JSONArray) 
				buttons = obj.getJSONArray("buttons");
		else if(obj.has("buttons"))
		{
			if(!(obj.get("buttons") instanceof JSONObject)) {
				System.out.println("hey");
			}
			buttons = BuildJSONArrayFromMap(obj.getJSONObject("buttons"));
		}
		else {
			// Buttons could be left dirty by previous assignment 
			buttons = null;
			return;
		}
	}
	public void getPhase(JSONObject obj) {
		if(obj.has("phase")) {
			/**
			 * Sometimes we get "transition messages"
			 * such as ['house', 'main'] meaning that we did house but we are in main now
			 */
			var _phase = obj.get("phase");
			phase = String.class.isInstance(_phase) 
					? obj.getString("phase") 
					: ( JSONArray.class.isInstance(_phase)	?
							obj.getJSONArray("phase").getString(obj.getJSONArray("phase").length() - 1) : 
							null);
		}
	}
	
	public void getMenuTitle(JSONObject player) {
		/**
		 * Sometimes we recieve transition messages,
		 * the last one is the one that we have to consider
		 */
		try {
			if(player.has("menuTitle") ) {
				var _menuTitle = player.get("menuTitle");
				menuTitle = JSONObject.class.isInstance(_menuTitle) ?
						composeMenuTitle(player.getJSONObject("menuTitle")) : 
						( String.class.isInstance(_menuTitle) ?
								player.getString("menuTitle") :
								( JSONArray.class.isInstance(_menuTitle) ?
										player.getJSONArray("menuTitle").getString(player.getJSONArray("menuTitle").length() - 1):
										null
								)
						);
			}
		} catch(Exception e) {
			System.out.println(e.getMessage());
			menuTitle = null;
			return;
		}
		
		
	}
	public Boolean getIsActivePlayer(JSONObject player) {
		if(player.has("activePlayer")) {
			var _activePlayer = player.get("activePlayer");
			activePlayer = Boolean.class.isInstance(_activePlayer) ? 
					player.getBoolean("activePlayer") :
					(JSONArray.class.isInstance(_activePlayer) ?
							player.getJSONArray("activePlayer").getBoolean(player.getJSONArray("activePlayer").length() -1) : 
							null);	
			return activePlayer;
		}		
		return this.activePlayer;
	}
	/***
	 * TODO 
	 * @param player
	 */
	public void getPromptTitle(JSONObject player) {
		if(player.has("promptTitle")) {
			var _promptTitle = player.get("promptTitle");
			promptTitle = String.class.isInstance(_promptTitle) ? 
					player.getString("promptTitle") : 
						(JSONArray.class.isInstance(_promptTitle) ?
								player.getJSONArray("promptTitle").getString(player.getJSONArray("promptTitle").length() - 1) :
								null); 
		}
	}
	
	private static String composeMenuTitle(JSONObject menuTitle) {
		try {
			var toAssign = menuTitle.getString("text");
			if(!menuTitle.has("values"))
				return toAssign;
			var values = menuTitle.get("values");
			Pattern p = Pattern.compile("(?<=\\{\\{).*(?=\\}\\})");
			Matcher m = p.matcher(toAssign);
			while(m.find()) {
				var value = ((JSONObject)values).get(m.group(0));
				toAssign = toAssign.replaceAll("\\{\\{" + m.group(0) + "\\}\\}", value.toString());
			}
			
			return toAssign;
		} catch (Exception e) {
			return null;
		}
		
	}
	public void checkStartGame() {
		if(promptTitle != null && "start game".equals(promptTitle.toLowerCase())) {
			if(start) return;
			else
				start = true;
		}
		
	}
	public void checkMulligan() {
		if(promptTitle != null && "mulligan".equals(promptTitle.toLowerCase())) {
			if(mulligan) return;
			else
				mulligan = true;
		}
	}
	public Utils.House getHouse(JSONObject obj) {
		try {
			if(obj.has("activeHouse") && (obj.get("activeHouse") != JSONObject.NULL)) {
				activeHouse =  String.class.isInstance(obj.get("activeHouse")) ? 
						Utils.resolveHouse(obj.getString("activeHouse")) : 
						(JSONArray.class.isInstance(obj.get("activeHouse")) ? 
							Utils.resolveHouse(obj.getJSONArray("activeHouse").getString(obj.getJSONArray("activeHouse").length() -1)) :
							null);
			}
			return activeHouse;	
		} catch (Exception e) {
			System.out.println(e.getMessage());
			System.out.println("House not found");
			return null;
		}
		
	}
}
