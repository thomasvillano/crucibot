package gameInterface;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import gameUtils.Utils;
import keyforge.GameState;
import keyforge.Player;

public class GameManager {

	public PrintWriter out;
	public Player botPlayer, opponentPlayer;
	public GameManager(String username, Player botPlayer) {
		this.botPlayer = botPlayer;
		try {
			out = new PrintWriter("log/GameManagerLog_" + System.currentTimeMillis() + ".txt");
		} catch (FileNotFoundException e) {
		}
	}
	
	public JSONArray update(JSONObject gameState) {
				return getWSStatus(gameState);
	}
	public void botCleaner(JSONObject bot)
	{
		bot.remove("deckCards");
		bot.remove("user");
		bot.remove("cardPiles");
		bot.remove("optionSettings");
		bot.remove("deckUuid");
		bot.remove("disconnected");
		bot.remove("deckSet");
		bot.remove("stats");
		bot.remove("deckName");
		bot.remove("houses");
		bot.remove("clock");
		bot.remove("name");
		bot.remove("cardback");
	}
	
	/***
	 * This is an entire mess....
	 * @param player
	 */
	private void updateBotPlayer(JSONObject player) {

		botPlayer.getCards(player);
		botPlayer.getButtons(player);
		botPlayer.getControls(player);
		botPlayer.getPhase(player);
		botPlayer.getPromptTitle(player);
		botPlayer.getMenuTitle(player);
		botPlayer.getIsActivePlayer(player);
		botPlayer.getHouse(player);
		botPlayer.checkStartGame();
		botPlayer.checkMulligan();		
	}
	
	private void updateOpponent(JSONObject opponent) {
		if(botPlayer.opponentDeck == null && opponent.has("deckData"))
		{
			var deckName = opponent.getJSONObject("deckData").getString("name");
			botPlayer.opponentDeck = CruciferMain.dr.AssignDeckByDeckName(deckName);
		}
		if(opponent.has("cardPiles")) {
			botPlayer.convertCards(opponent.getJSONObject("cardPiles"), true);
		}
	}
	
	/**
	 * Unpack the results of the ws message and update the state of both players to evaluate moves
	 * @param gameState
	 * @return
	 */
	private JSONArray getWSStatus(JSONObject gameState) {
		/**
		 * Loop through all keys and use just the necessary ones
		 * We need only players and owner maybe
		 */
		// maybe it makes more sense to log just the opponent name?
		if((opponentPlayer == null || opponentPlayer.name != null) && gameState.has("owner")) 
			opponentPlayer = new Player(gameState.getString("owner"),null);
		
		if(gameState.has("players")) {
			var players = gameState.getJSONObject("players");
			if(opponentPlayer != null && players.has(opponentPlayer.name))
				updateOpponent(players.getJSONObject(opponentPlayer.name));
			if(players.has(botPlayer.name))
				updateBotPlayer(players.getJSONObject(botPlayer.name));
		}
		/***
		 * 
			
			var status = checkStateON(opponent);
			if(status != null) return status;
			if(botPlayer.buttonEmpty() && botPlayer.controlsEmpty()) return null;		
			updateGameState(gameState);
			return botPlayer.planPhase();
		 * 
		 * 
		 */
		if ((botPlayer.buttons == null  || botPlayer.buttons.isEmpty()) && 
			(botPlayer.controls == null || botPlayer.controls.isEmpty()))
			return null;
		return botPlayer.planPhase();
	}
	/**
	 * Change this and use getWSStatus instead
	 * @param gameState
	 * @return
	 */
	@Deprecated
	private JSONArray getStatus(JSONObject gameState)
	{
		// in some cases we receive messages ... to understand the purpose we can simply skip it?
		if(gameState.has("players") && !gameState.getJSONObject("players").has(botPlayer.name)) 
			return null;
		// Get username from owner and then get JSONObject of the opponent
		if (this.opponentPlayer == null) {
			var opponentName = gameState.getString("owner");
			opponentPlayer = new Player(opponentName, "");
		}
		var opponent = gameState.getJSONObject("players").getJSONObject(opponentPlayer.name);
		updateOpponent(opponent);
		var player = gameState.getJSONObject("players").getJSONObject(botPlayer.name);
		updateBotPlayer(player);
		
		var status = checkStateON(opponent);
		if(status != null) return status;
		if(botPlayer.buttonEmpty() && botPlayer.controlsEmpty()) return null;		
		updateGameState(gameState);
		return botPlayer.planPhase();
	}
	private void updateGameState(JSONObject gameState) {
		botPlayer.updateGameState(gameState);
	}
	private void checkMulligan(String promptTitle)
	{
		
	}
	private void checkStartGame(String promptTitle)
	{
		
	}
	private JSONArray checkStateON(JSONObject opponent) 
	{
		if(opponent.has("left") && opponent.getBoolean("left")) {
			out.close();
			return new JSONArray()
					.put("game")
					.put("leavegame");
		}
		else return null;
		
	}
}
