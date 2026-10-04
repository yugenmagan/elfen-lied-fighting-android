package org.elfen.engine;

/** Bounded tick-boundary state and input history; no rendering/audio side effects. */
public final class MatchHistory {
    private final MatchController battle;private final byte[][] states;
    private final InputFrame[] inputs;private final int[] numbers;
    public MatchHistory(MatchController battle,int ticks){
        if(ticks<2||ticks>600)throw new IllegalArgumentException("History capacity 2..600");
        this.battle=battle;states=new byte[ticks+1][];inputs=new InputFrame[ticks+1];numbers=new int[ticks+1];java.util.Arrays.fill(numbers,-1);remember();
    }
    private int slot(int frame){if(frame<0)throw new IllegalArgumentException("Negative history frame");return frame%states.length;}
    private void remember(){int f=battle.frame(),s=slot(f);states[s]=battle.snapshot();numbers[s]=f;inputs[s]=null;}
    public void advance(InputFrame input){int before=battle.frame();battle.step(input);inputs[slot(before)]=input;remember();}
    public byte[] stateAt(int frame){int s=slot(frame);if(frame<0||numbers[s]!=frame)throw new IllegalArgumentException("Frame outside state history: "+frame);return states[s].clone();}
    public InputFrame inputAt(int frame){int s=slot(frame);if(frame<0||numbers[s]!=frame||inputs[s]==null)throw new IllegalArgumentException("Frame outside input history: "+frame);return inputs[s];}
    /** Caller supplies corrected inputs; old predicted outputs are discarded. */
    public void resimulate(int from,InputFrame[] corrected){
        if(from<0||from+corrected.length!=battle.frame())throw new IllegalArgumentException("Correction must reach the current frame");
        for(int i=0;i<corrected.length;i++)if(corrected[i].frame!=from+i)throw new IllegalArgumentException("Noncontiguous correction");
        byte[] start=stateAt(from);battle.restore(start);remember();for(InputFrame f:corrected)advance(f);
    }
}
