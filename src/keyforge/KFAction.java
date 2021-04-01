package keyforge;

import static gameUtils.Utils.*;

public class KFAction extends KFCard {
	public KFAction(String name, House house)
	{
		super(name, house);
	}
	public KFAction(KFAction card) {
		super(card);
	}
	public KFAction() {}

	@Override
	public double evaluateUtility(String move) {
		var utility = 0;
		if(this.name.toLowerCase().equals("three fates"))
			return -1;
		return utility;
	}
	@Override
	public void assignBlock(String[] block) {
		super.assignBlock(block);
	}
	@Override
	public void resetModifiers() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void updateAfterPlay() {
		position = FieldPosition.discard;
		playable = false;
		
	}

}
