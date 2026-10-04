package org.elfen.engine;
import java.io.*;import java.util.*;
/** Single-versus branch of original 0x4086a0. Numeric phases intentionally retained. */
public final class RoundController {
    final MatchRules rules;int state=110,wait,timer,round=1,wins1,wins2,winner=-1,reason;
    final ArrayList<Integer> started=new ArrayList<>();
    RoundController(MatchRules r){rules=r;timer=r.timeSetting*100-1;wait=r.duration(r.skill(0x4451c2));}
    private void show(int address){int s=rules.skill(address);started.add(s);wait=rules.duration(s);}
    void beginNextRound(){if(state!=100)throw new IllegalStateException("Round reset phase");round++;state=110;winner=-1;reason=0;timer=rules.timeSetting*100-1;wait=rules.duration(rules.skill(0x4451c2));}
    void step(int life1,int max1,int life2,int max2){
        started.clear();
        switch(state){
        case 110:if(wait-->0)return;state=111;if(round<10)show(0x4451c4+round*2);else wait=0;
        case 111:if(wait-->0)return;state=112;show(0x4451da);
        case 112:if(wait-->0)return;state=200;return;
        case 200:
            if(timer>=0&&--timer<0){timer=0;state=300;wait=0;}
            if(life1==0||life2==0){state=300;wait=0;}return;
        case 300:
            int a=max1==0?0:1000*life1/max1,b=max2==0?0:1000*life2/max2;
            state=a==0&&b==0?540:a==b?510:a>b?520:530;
            winner=state==520?0:state==530?1:-1;reason=state==540?3:state==510?2:timer==0?1:0;return;
        case 510:state=511;show(0x4451e8);if(!rules.decisiveRounds){wins1++;wins2++;}
        case 511:if(wait-->0)return;state=900;return;
        case 520:state=521;show(0x4451dc);wins1++;
        case 521:if(wait-->0)return;state=522;show(0x4451e4);
        case 522:if(wait-->0)return;if(life1==max1){state=523;show(0x4451de);}else state=900;return;
        case 523:if(wait-->0)return;state=900;return;
        case 530:state=531;show(0x4451dc);wins2++;
        case 531:if(wait-->0)return;state=532;show(0x4451e6);
        case 532:if(wait-->0)return;if(life2==max2){state=533;show(0x4451de);}else state=900;return;
        case 533:if(wait-->0)return;state=900;return;
        case 540:state=541;show(0x4451ea);if(!rules.decisiveRounds){wins1++;wins2++;}
        case 541:if(wait-->0)return;state=900;return;
        case 900:state=901;show(0x4451c4);
        case 901:if(wait-->0)return;state=902;
        case 902:if(wins1>=rules.winsRequired||wins2>=rules.winsRequired)state=1000;else{state=100;wait=0;}return;
        case 100:case 1000:return;
        default:throw new IllegalStateException("Unknown round phase "+state);
        }
    }
    void write(DataOutput o)throws IOException{StateIO.writeInts(o,new int[]{state,wait,timer,round,wins1,wins2,winner,reason});}
    void read(DataInput i)throws IOException{
        state=i.readInt();wait=i.readInt();timer=i.readInt();round=i.readInt();wins1=i.readInt();wins2=i.readInt();winner=i.readInt();reason=i.readInt();
        if(!Arrays.asList(100,110,111,112,200,300,510,511,520,521,522,523,530,531,532,533,540,541,900,901,902,1000).contains(state)||round<1||round>(rules.decisiveRounds?1000000:100)||wait< -1||timer< -1||wins1<0||wins2<0||winner< -1||winner>1||reason<0||reason>3)throw new IOException("Round snapshot");
    }
}
