package gameUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.json.JSONArray;

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
	public static FieldPosition resolveFieldPosition(String fieldPos){
		fieldPos = fieldPos.replaceAll("\\s+", "");
		return FieldPosition.valueOf(fieldPos.toLowerCase());
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