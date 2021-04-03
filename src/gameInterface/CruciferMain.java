package gameInterface;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.net.http.*;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import gameUtils.*;
import keyforge.*;

public class CruciferMain {
	public static DeckReader dr;
	public static String serverAddr = "192.168.1.119";
	public static String serverLobbyPort = ":4000";
	public static String serverGamePort = ":9500";
	public static void main (String[] args) throws IOException, InterruptedException
	{
		System.out.println("****************** CRUCIFER ******************");
		for(int i = 0; i < 3; i++)
			System.out.println();
		
		System.out.println("Reading cards\n");
		dr = new DeckReader("cards.txt");
		Player bot = new Player("player1", "player1");
		System.out.println("Assigning deck\n");
		bot.setDeck(chooseBotDeck());
		bot.deck = dr.AssignDeckByID(bot.getDeck());
		//usando lo stesso mazzo per ora --> lo leggo dal JSON
		System.out.println("Username:\t" + bot.name);
		
		HttpClient httpClient = HttpClient.newBuilder()
				.version(HttpClient.Version.HTTP_1_1)
				.build();
		HttpRequest request = HttpRequest.newBuilder()
				.GET()
				.uri(URI.create("http://" + serverAddr + serverLobbyPort + "/socket.io/?version=2019-11-13&EIO=3&transport=polling"))
				.setHeader("User-Agent", "Java 11 HttpClient")
				.build();
		
		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		String sid = new JSONObject(response.body().substring(4)).getString("sid");
		String wsUri = "ws://" + serverAddr + serverLobbyPort +"/socket.io/?version=2019-11-13&EIO=3&transport=websocket&sid=" + sid;
		WebSocket ws = HttpClient
				.newHttpClient()
				.newWebSocketBuilder()
				.buildAsync(URI.create(wsUri), new LobbyWebSocket(httpClient, bot))
				.join();
		ws.sendText("2probe", true);	//scambio iniziale e periodico di messaggi per mantenere la web socket
		new Thread() {
			public void run() {
				while(true) {
					try {
						Thread.sleep(25000);
						ws.sendText("2", true);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();
		String json = "{\"username\":\""+ bot.name +"\",\"password\":\""+ bot.password+"\"}";
		request = HttpRequest.newBuilder()
				.uri(URI.create("http://" + serverAddr + serverLobbyPort + "/api/account/login"))
				.header("Content-Type", "application/json")
				.POST(BodyPublishers.ofString(json))
				.build();
		response = httpClient.send(request, BodyHandlers.ofString());
		String token = new JSONObject(response.body()).getString("token");
		
		JSONArray auth = new JSONArray()
				.put("authenticate")
				.put(token);
		ws.sendText("42" + auth.toString(), true);
		
		
	}
	public static String chooseBotDeck() {
		try {
			BufferedReader br = new BufferedReader(new FileReader("decks.txt"));
			String line;
			Map<Integer,String> decks = new HashMap<>();
			br.readLine(); //header
			int i = 0;
			System.out.println("Which deck do you want to choose?");
			while((line = br.readLine()) != null) {
				System.out.println(i + ")\t" + line.split(";")[0]);
				decks.put(i, line.split(";")[1]);
				i++;
			}
			br.close();
			BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in));
			var cond = true;
			int intChoice = 0;
			if(true) 
			{
				keyboard.close();
				return decks.get(intChoice);
			}
			do {
				
				var choice = keyboard.readLine();
				
				try {
				intChoice = Integer.parseInt(choice);
				if(!decks.containsKey(intChoice)) {
					System.out.println("Error, must be one of the reported above");
					continue;
				}
				break;
				} catch(NumberFormatException e) {
					System.out.println("Not a number!");
				}
			} while(cond);
			keyboard.close();
			return decks.get(intChoice);
		} catch(FileNotFoundException e) {
			System.out.println("Deck list file not found");
			System.exit(-1);
		} catch(IOException e) {
			System.out.println("Errors occur when reading deck list file!");
			System.exit(-1);
		}
		return null;
	}
}
