package org.elfen.engine;

import static org.elfen.engine.Pack.*;

/** Immutable subset of the original KGT rules, retaining source identity. */
public final class CombatRules {
    public final String sourceHash;
    public final int hitstop,guardstop,clashStop,flags;
    private final int[] junctionFlags=new int[200];
    public CombatRules(Pack kgt){
        if(!kgt.kind.equals(".kgt"))throw new IllegalArgumentException("KGT rules required");
        byte[] t=kgt.originalTail;int pos=4+50*256;
        if(t.length<pos+200*36+8)throw new IllegalArgumentException("Truncated KGT combat rules");
        for(int i=0;i<200;i++)junctionFlags[i]=i32(t,pos+i*36+32);
        pos+=200*36+5;hitstop=u(t,pos);guardstop=u(t,pos+1);clashStop=u(t,pos+2);sourceHash=kgt.hash;
        flags=u(t,58420);
        if((flags&2)!=0)throw new IllegalArgumentException("Enabled global clash spark requires KGT task implementation");
    }
    public int junctionFlags(int i){if(i<0||i>=200)throw new IllegalStateException("KGT hit junction "+i);return junctionFlags[i];}
}
