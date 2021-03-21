package gameInterface;

import java.io.PrintWriter;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;
import org.json.JSONArray;
import org.json.JSONObject;
import keyforge.Player;
public class GameWebSocket implements Listener {
	
	private String longData = "";
	private boolean update = false;
	private GameManager gm;
	private WebSocket lobbyWS;
	private String username;
	private Player botPlayer, opponent;
	
	public GameWebSocket(WebSocket ws, String username, Player botPlayer) {
		lobbyWS = ws;
		this.username = username;
		this.botPlayer = botPlayer;
	}
	
	@Override
    public void onOpen(WebSocket webSocket) {
		System.out.println("GameWS open");
		gm = new GameManager(username, botPlayer);
		Listener.super.onOpen(webSocket);
    }
	
	@Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
		String dataStr = data.toString();
		if(dataStr.equals("3probe"))
			webSocket.sendText("5", true);
		if(dataStr.equals("42[\"cleargamestate\"]"))
			webSocket.sendClose(1000, "close");
		if(dataStr.startsWith("42[\"gamestate\"")) {
			update = true;
			longData = "";
		}
		//a volte un JSON può arrivare in più messaggi; last is true quando il messaggio ricevuto è l'ultimo
		if(update) {
			longData += dataStr;
			if(last) {
				update = false;
				JSONObject gameState = new JSONArray(longData.substring(2)).getJSONObject(1);
				JSONArray response = gm.update(gameState);
				if(response != null)
					webSocket.sendText("42" + response.toString(), true);
			}
		}
		return Listener.super.onText(webSocket, data, last);
    }
	
	@Override
    public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
		System.out.println("GameWS binary");
		return Listener.super.onBinary(webSocket, data, last);
    }
 
    @Override
    public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
    	System.out.println("GameWS ping");
		return Listener.super.onPing(webSocket, message);
    }
 
    @Override
    public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
    	System.out.println("GameWS pong");
		return Listener.super.onPong(webSocket, message);
    }
 
    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
    	System.out.println("GameWS close");
		lobbyWS.sendClose(1000, "close");
    	gm.out.close();
		return Listener.super.onClose(webSocket, statusCode, reason);
    }
 
    @Override
    public void onError(WebSocket webSocket, Throwable error) {
    	System.out.println("GameWS error: " + error.getMessage());
		lobbyWS.sendClose(1000, "close");
    	gm.out.close();
		Listener.super.onError(webSocket, error);;
    }

}
