package org.elfen.engine;

import java.util.*;
import static org.elfen.engine.Pack.*;

/** The 100 variable-size on-disk records expanded to native 206-byte story records. */
public final class StoryProgram {
    public final Pack player;private final Event[] events=new Event[100];
    public static final class Event {
        public final int slot,type;private final byte[] record;
        Event(int slot,byte[] record){this.slot=slot;this.record=record;type=record[0]&15;}
        public int value(int offset){return u(record,offset);}
        public int word(int offset){return u16(record,offset);}
        public int signed(int offset){return record[offset];}
        public byte[] bytes(){return record.clone();}
        public int enemyCount(){int n=0;for(int p=0;p<7;p++)if(value(28+26*p)!=0)n++;return n;}
        public int enemy(){if(type!=1||enemyCount()!=1||value(28)==0)throw new IllegalStateException("Unsupported story fighter layout at "+slot);return value(28);}
    }
    public StoryProgram(Pack p){
        if(!p.kind.equals(".player"))throw new IllegalArgumentException("PLAYER story required");player=p;byte[] t=p.originalTail;
        try{
            int at=advance(t,4,82);at=advance(t,at,4);at=advance(t,at,6);at=Math.addExact(at,10+11100+48+38+1785);
            for(int slot=0;slot<100;slot++){
                byte[] r=new byte[206];r[0]=t[at++];if((r[0]&255)>4)throw new IllegalArgumentException("Unknown story record "+p.id+":"+slot);
                if(r[0]!=0){if(at+205>t.length)throw new IllegalArgumentException("Truncated story "+p.id);System.arraycopy(t,at,r,1,205);at+=205;}events[slot]=new Event(slot,r);
            }
        }catch(IndexOutOfBoundsException|ArithmeticException e){throw new IllegalArgumentException("Truncated PLAYER story "+p.id,e);}
    }
    private static int advance(byte[] b,int at,int stride){int n=i32(b,at);if(n<0||n>100000)throw new IllegalArgumentException("Story table count");int end=Math.addExact(at+4,Math.multiplyExact(n,stride));if(end>b.length)throw new IllegalArgumentException("Truncated story table");return end;}
    public Event event(int slot){if(slot<0||slot>=100)throw new IllegalArgumentException("Story slot "+slot);return events[slot];}
    public boolean available(){return events[0].type!=0;}
    /** Last configured battle in these audited, linear routes. No assets are patched. */
    public int finalFightSlot(){int last=-1;for(Event e:events)if(e.type==1&&e.value(1)!=0)last=e.slot;return last;}
    /** 4069b0: zero references skip; type3 branches are relative to their own slot. */
    public int next(int current,int previousResult,int life,int losses){
        for(int budget=0;budget<200;budget++){
            Event e=event(++current);
            if(e.type==0||e.type==4)return current;
            if(e.type==1||e.type==2){if(e.value(1)!=0)return current;continue;}
            boolean branch;
            switch(e.value(1)){case 0:branch=true;break;case 1:branch=previousResult==1;break;case 2:branch=life<e.value(2);break;case 3:branch=losses==0;break;default:throw new IllegalStateException("Story condition "+e.value(1));}
            if(branch)current+=e.signed(5)-1;
        }
        throw new IllegalStateException("Story branch budget exceeded");
    }
}
