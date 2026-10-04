package org.elfen.engine;
import java.io.*;
import static org.elfen.engine.Pack.*;
/** Immutable single-versus rules; all delays refer to the original KGT table. */
public final class MatchRules {
    public final int winsRequired,timeSetting,cpu1,cpu2; final Pack kgt; public final boolean decisiveRounds;
    public MatchRules(Pack kgt,int wins,int time,int cpu1,int cpu2){this(kgt,wins,time,cpu1,cpu2,false);}
    public MatchRules(Pack kgt,int wins,int time,int cpu1,int cpu2,boolean decisiveRounds){
        if(!kgt.kind.equals(".kgt")||wins<1||wins>9||time<0||time>999||cpu1<0||cpu1>100||cpu2<0||cpu2>100)throw new IllegalArgumentException("Match rules");
        this.decisiveRounds=decisiveRounds;this.kgt=kgt;winsRequired=wins;timeSetting=time;this.cpu1=cpu1;this.cpu2=cpu2;
    }
    int skill(int address){int offset=address-0x435470;if(offset<0||offset+2>kgt.originalTail.length)throw new IllegalArgumentException("KGT system table");int s=u16(kgt.originalTail,offset);if(s>=kgt.starts.length)throw new IllegalArgumentException("System skill "+s);return s;}
    int duration(int skill){if(skill==0)return 0;int p=kgt.starts[skill]*16;if(p>=kgt.end(skill)*16||u(kgt.code,p)!=0)throw new IllegalStateException("System skill metadata "+skill);return u16(kgt.code,p+1);}
    void write(DataOutput o)throws IOException{for(int v:new int[]{winsRequired,timeSetting,cpu1,cpu2})o.writeInt(v);}
    static MatchRules read(Pack k,DataInput i)throws IOException{return new MatchRules(k,i.readInt(),i.readInt(),i.readInt(),i.readInt());}
    boolean same(MatchRules r){return decisiveRounds==r.decisiveRounds&&winsRequired==r.winsRequired&&timeSetting==r.timeSetting&&cpu1==r.cpu1&&cpu2==r.cpu2;}
}
