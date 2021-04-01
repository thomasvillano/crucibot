package keyforge;

import static gameUtils.Utils.*;

public class KFUpgrade extends KFCard {
	public KFUpgrade(String name, House house)
	{
		super(name, house);
	}
	public KFUpgrade(KFUpgrade card) {
		super(card);
	}
	public KFUpgrade() {}
	@Override
	public void assignBlock(String[] block) {		
		super.assignBlock(block);
	}
	@Override
	public double evaluateUtility(String move) {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public void resetModifiers() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void updateAfterPlay() {
		this.position = FieldPosition.playarea;
		this.isNew = true;
	}
	
}
