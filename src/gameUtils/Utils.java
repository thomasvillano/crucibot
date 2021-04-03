package gameUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import keyforge.*;


public class Utils {

	
	public static String getFilteredAttribute(String word) 
	{
		var patt = Pattern.compile("\\[(.*)\\]");
		var match = patt.matcher(word.trim());
		match.find();
		return match.group(1);
	}
	public static String getFilteredWord(String word) {
		var patt = Pattern.compile("\"([^\"]*)\"");
		var match = patt.matcher(word.trim());
		match.find();
		return match.group(1);		
	}
	public static String[] readBlock(String block) {
		var splitted = block.split(",");
		var patt = Pattern.compile("\"([^\"]*)\"");
		for(var split : splitted)
		{
			var match = patt.matcher(split.trim().split(":")[0]);
			match.find();
			System.out.println(match.group(1));
			
		}
		return null;
	}
	public static KFCard cardFromType(String type)
	{
		switch(type) {
		case "creature":
			return new KFCreature();
		case "artifact":
			return new KFArtifact();
		case "upgrade":
			return new KFUpgrade();
		case "action":
			return new KFAction();
			default:
				System.out.println("Not a valid type");
				return null;
		}
	}
	public static KFCard cloneCard(KFCard card) {
		if (KFCreature.class.isInstance(card))
			return new KFCreature((KFCreature) card);
		else if (KFAction.class.isInstance(card))
			return new KFAction((KFAction) card);
		else if (KFUpgrade.class.isInstance(card))
			return new KFUpgrade((KFUpgrade) card);
		else if (KFArtifact.class.isInstance(card))
			return new KFArtifact((KFArtifact) card);
		return null;
	}
	public static Type resolveType(String effect) {
		return Type.valueOf(effect);
	}
	
	
	public static <T> T coalesce(T value, T defaultValue) {
		return value != null ? value : defaultValue;
	}
	public static <T> T getValueFromClassAndJSON(JSONObject obj, Class<T> objClass, String key) {
		if(!obj.has(key))
			return null;
		if(key.equals("promptTitle") && JSONObject.class.isInstance(obj.get(key)) && objClass == String.class) {
				return objClass.cast(composeCrucibleString(obj.getJSONObject(key)));
		}
		try {
			if(objClass == (obj.get(key).getClass())) {
				return objClass.cast(obj.get(key));
			} else if(obj.get(key).getClass() == JSONArray.class) {
				var array = obj.getJSONArray(key);
				var last = array.get(array.length() -1);
				if(last.getClass() == objClass) {
					return objClass.cast(last);
				}
			} else {
				
				System.out.println(key + " is not an instance of " + objClass);
			}
				
		} catch(Exception e) {
			System.out.println("Problem with getValueFromClassAndJSON for class " + objClass);
			System.out.println(e.getMessage());
		}
		return null;
	}
	
	public static String composeCrucibleString(JSONObject obj) {

		var text = coalesce(getValueFromClassAndJSON(obj, String.class, "text"),null);
		var values = coalesce(getValueFromClassAndJSON(obj, JSONObject.class, "values"), null); 
		
		if(text == null || values == null)
			return null;
		
		var finalText = new String(text);
		
		Pattern p = Pattern.compile("(?:\\{\\{)(\\w+)(?:\\}\\})");
		Matcher m = p.matcher(text);
		while(m.find()) {
			var value_key = m.group(1);
			if(!values.has(value_key)) {
				System.out.println("object " + values + " has no key " + value_key);
				continue;
			}
			var value = values.get(value_key);
			try {
				finalText = finalText.replaceAll("\\{\\{" + m.group(1) + "\\}\\}", value.toString());
			} catch(Exception e) {
				System.out.println("unable to replace " + value);
			}
		}
		return finalText;
	}
	public static FieldPosition solveCruciblePosition(String cruciblePos) {
		switch(cruciblePos) {
			case "cardsInPlay":
				return FieldPosition.playarea;
			case "hand":
			case "discard":
			case "deck":
				return resolveFieldPosition(cruciblePos);
			default:
				System.out.println("case not implemented yet: " );
				return resolveFieldPosition(cruciblePos);
		}
	}
	public static FieldPosition resolveFieldPosition(String fieldPos){
		fieldPos = fieldPos.replaceAll("\\s+", "");
		try {
			return FieldPosition.valueOf(fieldPos.toLowerCase());
		} catch(Exception e) {
			return solveCruciblePosition(fieldPos);
		}
	}
	public static GamePhase resolveGamePhase(String gamePhase) {
		return GamePhase.valueOf(gamePhase);
	}
	public static Ability resolveAbility(String ability) {
		return Ability.valueOf(ability);
	}
	public static House resolveHouse(String house) {
		house = house.replaceAll("\\s+", "");
		return House.valueOf(house);
	}
	public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
	    Set<Object> seen = ConcurrentHashMap.newKeySet();
	    return t -> seen.add(keyExtractor.apply(t));
	}
	public static List<String> jsonArrayToList(JSONArray jsonArray) {
		ArrayList<String> list = new ArrayList<String>();      
		if (jsonArray != null) { 
		   int len = jsonArray.length();
		   for (int i=0;i<len;i++){ 
		    list.add(jsonArray.get(i).toString());
		   } 
		} 
		return list;
	}
	public enum GamePhase {
		forge,
		main,
		playphase,
		house,
		play,
		ready,
		draw,		
	}
	public enum Type {
		play,
		fight,
		reap,
		destroyed,
		action,
		before_fight,
		omni,
		constant,
	}
	public enum Ability
	{
		skirmish,
		deploy,
		taunt,
		hazardous,
		elusive,
	}
	public enum FieldPosition {
		playarea,
		play,
		field,
		purged,
		discard,
		archives,
		hand,
		neighbor,
		top_deck,
		deck,
	}
	public enum House {
		untamed,
		staralliance,
		dis,
		mars,
		brobnar,
		logos,
		sanctum,
		shadows,
	}
}