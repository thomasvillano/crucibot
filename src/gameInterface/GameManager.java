package gameInterface;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import gameUtils.Utils;
import keyforge.GameState;
import keyforge.Player;

public class GameManager {

	public PrintWriter out;
	private boolean start = false;
	private boolean mulligan = false;
	public Player botPlayer, opponentPlayer;
	public GameManager(String username, Player botPlayer) {
		this.botPlayer = botPlayer;
		try {
			out = new PrintWriter("log/GameManagerLog_" + System.currentTimeMillis() + ".txt");
		} catch (FileNotFoundException e) {
		}
	}
	
	public JSONArray update(JSONObject gameState) {
				return getStatus(gameState);
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
	private void updateBotPlayer(JSONObject player) {

		botPlayer.convertCards(player.getJSONObject("cardPiles"), false);
		botPlayer.buttons = player.getJSONArray("buttons");
		botPlayer.controls = player.getJSONArray("controls");
		botPlayer.phase = player.getString("phase");
		try { botPlayer.promptTitle = player.has("promptTitle") ? player.getString("promptTitle") : null; } catch(Exception e) {}
		botPlayer.menuTitle = JSONObject.class.isInstance(player.get("menuTitle")) ? 
				composeMenuTitle(player.getJSONObject("menuTitle")) : player.getString("menuTitle");
		botPlayer.activePlayer = player.getBoolean("activePlayer");
		this.checkStartGame(botPlayer.promptTitle);
		this.checkMulligan(botPlayer.promptTitle);
		botPlayer.activeHouse = (player.get("activeHouse") != JSONObject.NULL) ? 
				Utils.resolveHouse(player.getString("activeHouse")) : null;
		
	}
	private String composeMenuTitle(JSONObject menuTitle) {
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
	}
	private void updateOpponent(JSONObject opponent) {
		if(botPlayer.opponentDeck == null)
		{
			var deckName = opponent.getString("deckName");
			botPlayer.opponentDeck = CruciferMain.dr.AssignDeckByDeckName(deckName);
		}
		botPlayer.convertCards(opponent.getJSONObject("cardPiles"), true);
	}
	private JSONArray getStatus(JSONObject gameState)
	{
		var opponent = gameState.getJSONObject("players").getJSONObject(gameState.getString("owner"));
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
		if(promptTitle != null && "mulligan".equals(promptTitle.toLowerCase())) {
			if(mulligan) return;
			else
				mulligan = true;
		}
	}
	private void checkStartGame(String promptTitle)
	{
		if(promptTitle != null && "start game".equals(promptTitle.toLowerCase())) {
			if(start) return;
			else
				start = true;
		}
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
