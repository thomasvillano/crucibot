package gameUtils;
import java.io.*;
import java.util.*;

import keyforge.KFAction;
import keyforge.KFArtifact;
import keyforge.KFCard;
import keyforge.KFCreature;
import keyforge.KFUpgrade;
import keyforge.PlannedMove;

public class DeckReader {
	private static List<KFCard> cards;
	public List<KFCard> botDeck, opponentDeck;
	public DeckReader() {}
	/**
	 * TODO change to JSON
	 * @param fileName
	 */
	public DeckReader(String fileName)
	{
		cards = new ArrayList<KFCard>();
		int brackets = 0;
		String card = "";
		boolean start = false;
		try {
			int r;
			BufferedReader reader = new BufferedReader(new FileReader(fileName));
			while((r = reader.read()) != -1) {
				char ch = (char) r;
				card += ch;
				if(ch == '{')
				{		
					start = true;
					brackets++;
				}
				else if ( ch == '}')
					brackets--;
				if(start && brackets == 0) {
					cards.add(this.getCardFromBlock(card));
					card = "";
					start = false;
				}
			}
			reader.close();
						
		} catch (FileNotFoundException e) {
			System.out.println("File " + fileName + " not found");
			System.exit(-1);
		} catch(IOException e) {
			System.out.println("Error while reading file " + e.getMessage());
			System.exit(-2);
		}
		
	}
	public KFCard getCardFromBlock(String card) {
		KFCard kc = null;
		Scanner scan = new Scanner(card);
		scan.useDelimiter("\\{|\\}");
		var name = Utils.getFilteredWord(scan.next());
		scan.useDelimiter(",");
		var bl = scan.next();
		var house = Utils.resolveHouse(Utils.getFilteredWord(bl.split(":")[1]));
		var is_a = Utils.getFilteredWord(scan.next().split(":")[1]);
		switch(is_a)
		{
		case "creature":
			kc = new KFCreature(name, house);
			break;
		case "action":
			kc = new KFAction(name, house);
			break;
		case "artifact":
			kc = new KFArtifact(name, house);
			break;
		case "upgrade":
			kc = new KFUpgrade(name, house);	
			break;
		default: 
			break;
		}
		scan.useDelimiter("\\{|\\}");
		scan.next();
		for(boolean cond = true; cond == true; cond = scan.hasNext()) {
			var block = scan.next();
			kc.assignBlock(processBlock(block));
			scan.next();
		}
		
		scan.close();
		kc.assessCard();
		return kc;
	}
	
	public String[] processBlock(String block)
	{
		var attribute = "";
		if(block.contains("[") && block.contains("]"))
		{
			block = block.replaceAll("[\\t\\n\\r]+", " ");
			block = block.trim();
			
			attribute = Utils.getFilteredAttribute(block);
			block = block.replaceAll("\\[(.*)\\]", "attr");
		}
		try {
			var splitBlock = block.split(",");
			String[] processed = new String[2];
			for(int i = 0; i < splitBlock.length; i++)
			{
				
				var attr = splitBlock[i].split(":");
				switch(Utils.getFilteredWord(attr[0]))
				{
					case "target":
						processed[0] = Utils.getFilteredWord(attr[1]);
						break;
					case "value":
						switch(processed[0])
						{
						case "ability":
							processed[1] = attribute;
							break;
						case "const_ability":
						default:
							processed[1] = Utils.getFilteredWord(attr[1]);
						}
						break;
					case "features":
						processed = Arrays.copyOf(processed, 3);
						processed[2] = Utils.getFilteredWord(attr[1]);
						break;
					default:
						System.out.println("Unvalid type/target/attribute format for" + Utils.getFilteredWord(attr[0]) + "\nIgnored");
						break;
				}
			}	
			return processed;
		} catch(Exception e) {
			System.out.println("Not a valid block, no card initialized");
			return null;
		}
	}
	public List<KFCard> AssignDeckByDeckName(String deckName) {
		var deck = new ArrayList<KFCard>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(deckName));
			var line = "";
			while((line = br.readLine()) != null)
			{
				var num = Integer.parseInt(line.substring(0, 1));
				var card = line.substring(3);
				var splitted = card.split("\\|");
				String maverik = "";
				if(splitted.length > 1) {
					card = splitted[0];
					maverik = splitted[1];
				}
				var sCard = card;
				KFCard kfCard = cards.parallelStream()
						.filter(x -> x.getName().toLowerCase().equals(sCard.toLowerCase()))
						.findAny()
						.orElse(null);
				if(kfCard != null)
					for(int i =0; i< num; i++)
					{
						var cloned = Utils.cloneCard(kfCard);
						if(!maverik.isBlank() && !maverik.isEmpty())
							cloned.setHouse(maverik);
						deck.add(cloned);
						System.out.println("New card " + card + " added to deck!");
					}
				else
				{
					System.out.println("Card " + card + " not found!");
					continue;
				}
			}
			
			br.close();
		} catch(Exception e)
		{
			System.out.println(e.getMessage());
			System.exit(-1);
		}		
		return deck;
	}
	public List<KFCard> AssignDeckByID(String deckID)
	{
		var name = findDeckByID(deckID);
		return AssignDeckByDeckName(name);
		
	}
	public String findDeckByID(String id) {
		var deckTransl = "decks.txt";
		var deckName = "";
		try {
			BufferedReader br = new BufferedReader(new FileReader(deckTransl));
			var line = br.readLine();
			while((line = br.readLine()) != null)
			{
				if(line.split(";")[1].equals(id))
				{
					deckName = line.split(";")[0];
					break;
				}
			}
			br.close();
			if(deckName.isEmpty() || deckName == null)
			{
				System.out.println("Deck not found");
				System.exit(-2);
			}
			return deckName;
		} catch (Exception e)
		{
			System.out.println(e.getMessage());
			System.exit(-1);
		}
		System.out.println("Deck not found");
		System.exit(-2);
		return null;
	}
	
}
	
