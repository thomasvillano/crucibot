package gameInterface;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.WebSocket.Listener;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;

import org.json.JSONArray;
import org.json.JSONObject;

import keyforge.Player;

public class LobbyWebSocket implements Listener {
	
	private HttpClient client = null;
	private String username;
	private String deckID;
	public Player botPlayer, oppositePlayer;
	public LobbyWebSocket(HttpClient client, Player botPlayer) {
		this.client = client;
		this.username = botPlayer.name;
		this.deckID = botPlayer.getDeck();
		this.botPlayer = botPlayer;
	}
	
	@Override
    public void onOpen(WebSocket webSocket) {
		System.out.println("LobbyWS open");
		Listener.super.onOpen(webSocket);
    }
	
	@Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
		//System.out.println("LobbyWS text: " + data);
		String dataStr = data.toString();
		if(dataStr.equals("3probe"))
			webSocket.sendText("5", true);
		//rilevato un nuovo game creato chiamato esattamente <NOME UTENTE>'s game, cio� il nome predefinito
		if(dataStr.contains("newgame") && dataStr.contains("thom"/*username*/ + "'s game")) {
			JSONArray gameInfo = new JSONArray(data.toString().substring(2));
			String id = gameInfo.getJSONArray(1).getJSONObject(0).getString("id");
			JSONArray response = new JSONArray()
					.put("joingame")
					.put(id)
					.put("");		//eventuale password del game
			webSocket.sendText("42" + response.toString(), true);
			response = new JSONArray()
					.put("selectdeck")
					.put(id)
					.put(deckID);		//id del mazzo che user� il bot, che deve essere prima aggiunto alla sua lista
			webSocket.sendText("42" + response.toString(), true);
		}
		//nel caso l'untente lascia il game, esce anche il bot (per evitare che si inchiodi dentro il game e non esca più)
		if(dataStr.contains("gamestate") && dataStr.contains(username + "'s game")) {
			JSONArray gameInfo = new JSONArray(data.toString().substring(2));
			String id = gameInfo.getJSONObject(1).getString("id");
			JSONObject players = gameInfo.getJSONObject(1).getJSONObject("players");
			if(!players.has(username)) {
				JSONArray response = new JSONArray()
						.put("leavegame")
						.put(id);
				webSocket.sendText("42" + response.toString(), true);
			}
		}
		//entrambi i giocatori (utente e bot) hanno scelto il mazzo e messo ready
		if(dataStr.contains("handoff")) {
			try {
				JSONArray handOff = new JSONArray(data.toString().substring(2));
				String node = handOff.getJSONObject(1).getString("name");	//pu� essere node1 o node2
				//System.out.println("Node is: " + node);
				String token = handOff.getJSONObject(1).getString("authToken");
				
				//NUOVO SESSION ID PER IL GAME
				String uri = "http://" + CruciferMain.serverAddr + CruciferMain.serverGamePort + "/" +  node +"/socket.io/?token=" + token + "&EIO=3&transport=polling";
				HttpRequest request = null;
				request = HttpRequest.newBuilder()
						.uri(URI.create(uri))
						.setHeader("User-Agent", "Java 11 HttpClient")
						.header("Origin", "http:/" + CruciferMain.serverAddr + CruciferMain.serverGamePort )
						.GET()
						.build();
				HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
				//HttpHeaders headers = response.headers();
				//headers.map().forEach((k, v) -> System.out.println(k + ":" + v));
		        //System.out.println(response.statusCode());
		        //System.out.println(response.body());
				
				String sid = new JSONObject(response.body().substring(4)).getString("sid");
				//System.out.println(sid);
				//COLLEGAMENTO WEB SOCKET PER IL GAME
				uri = "ws://" + CruciferMain.serverAddr + CruciferMain.serverGamePort + "/" + node +"/socket.io/?token=" + token + "&EIO=3&transport=websocket&sid=" + sid;
				//System.out.println(uri);
				WebSocket gameWS = HttpClient
						.newHttpClient()
						.newWebSocketBuilder()
						.buildAsync(URI.create(uri), new GameWebSocket(webSocket, username, botPlayer))
						.join();
				gameWS.sendText("2probe", true);	//scambio iniziale e periodico di messaggi per mantenere la web socket
				new Thread() {
					public void run() {
						while(true) {
							try {
								Thread.sleep(25000);
								gameWS.sendText("2", true);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}
				}.start();
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}
		return Listener.super.onText(webSocket, data, last);
    }
	
	@Override
    public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
		System.out.println("LobbyWS binary");
		return Listener.super.onBinary(webSocket, data, last);
    }
 
    @Override
    public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
    	System.out.println("LobbyWS ping");
		return Listener.super.onPing(webSocket, message);
    }
 
    @Override
    public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
    	System.out.println("LobbyWS pong");
		return Listener.super.onPong(webSocket, message);
    }
 
    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
    	System.out.println("LobbyWS close");
    	System.exit(0);
		return Listener.super.onClose(webSocket, statusCode, reason);
    }
 
    @Override
    public void onError(WebSocket webSocket, Throwable error) {
    	System.out.println("LobbyWS error: " + error.getMessage());
    	System.exit(0);
		Listener.super.onError(webSocket, error);;
    }

}
