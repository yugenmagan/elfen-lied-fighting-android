package org.elfen.controls;

import org.elfen.engine.Input;

/** Collect button presses atomically at the fixed simulation boundary.
 * Android delivers different fingers in separate events. Recording each partial
 * A -> AB -> ABC press as a game sample prevents the original fresh-press check
 * from ever seeing ABC. Preserve ordered directions/releases, then commit the
 * sampled button mask once. InputFrame/replay still contains the exact result.
 */
public final class FrameInput {
    private int heldButtons;
    public void reset(){heldButtons=0;}
    public int[] poll(Input input){
        int[] transitions=input.drain();int finalMask=input.sample();
        int[] samples=new int[transitions.length+1];
        for(int n=0;n<transitions.length;n++){
            heldButtons&=transitions[n]&0x3f0;
            samples[n]=(transitions[n]&15)|heldButtons;
        }
        samples[transitions.length]=finalMask;heldButtons=finalMask&0x3f0;
        return samples;
    }
}
