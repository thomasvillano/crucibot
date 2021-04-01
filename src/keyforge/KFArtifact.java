package keyforge;

import static gameUtils.Utils.*;

public class KFArtifact extends KFCard {
	public KFArtifact(String name, House house)
	{
		super(name, house);
	}
	public KFArtifact(KFArtifact card) {
		super(card);
	}
	public KFArtifact() {}
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
		this.ready = false;
		this.playable = false;
		this.position = FieldPosition.playarea;
		this.exhausted = true;	
		this.isNew = true;
	}
}
