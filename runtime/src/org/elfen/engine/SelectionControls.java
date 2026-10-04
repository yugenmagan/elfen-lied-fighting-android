package org.elfen.engine;
/** One phone/gamepad selects P1 then P2; held confirmation cannot accept both. */
public final class SelectionControls {
    private boolean releaseRequired;
    public void requireRelease(){releaseRequired=true;}
    public void step(CharacterSelect s,int[] raw){
        int p=s.activePlayer();boolean accepted=s.confirmed(p);int[] samples=raw.clone();
        if(releaseRequired){boolean released=(samples[samples.length-1]&0x3f0)==0;for(int n=0;n<samples.length;n++)samples[n]&=15;if(released)releaseRequired=false;}
        s.step(new InputFrame(s.frame(),p==0?samples:new int[]{0},p==1?samples:new int[]{0}));
        if(!accepted&&s.confirmed(p)&&(raw[raw.length-1]&0x3f0)!=0)releaseRequired=true;
    }
}
