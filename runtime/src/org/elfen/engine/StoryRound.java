package org.elfen.engine;
import java.io.*;import java.util.*;
/** Audited one-enemy story branch of 4086a0. Score thresholds are original data. */
final class StoryRound {
    final MatchRules rules;final StoryProgram.Event event;
    int state=110,wait,timer,round=1,wins1,wins2,points1,points2,losses,winner=-1;
    final ArrayList<Integer> started=new ArrayList<>();
    StoryRound(Pack k,StoryProgram.Event e){this(k,e,false);}
    StoryRound(Pack k,StoryProgram.Event e,boolean classic){event=e;rules=new MatchRules(k,classic?2:e.value(2),e.word(6),0,Math.min(100,e.value(29)),classic);timer=e.word(6)*100-1;wait=rules.duration(rules.skill(0x4451c2));
        if(e.enemyCount()!=1||e.value(28)==0||e.word(24)!=0||e.word(26)!=0||e.value(30)!=1||e.value(18)!=0||e.value(38)!=0)throw new IllegalArgumentException("Unaudited story score/team layout at "+e.slot);
    }
    private void show(int address){int s=rules.skill(address);started.add(s);wait=rules.duration(s);}
    void beginNextRound(){round++;state=110;points1=points2=0;winner=-1;timer=event.word(6)*100-1;wait=rules.duration(rules.skill(0x4451c2));}
    void death(int victim,int killer){if(killer<0)return;int award=event.value(victim==0?19:39);if(killer==0)points1+=award;else points2+=award;}
    void step(int hp1,int max1,int hp2,int max2){
        started.clear();
        switch(state){
        case 110:if(wait-->0)return;state=111;if((event.value(12)&1)==0){wait=0;return;}if(round<10)show(0x4451c4+round*2);else wait=0;
        case 111:if(wait-->0)return;state=112;if((event.value(12)&2)==0){wait=0;return;}show(0x4451da);
        case 112:if(wait-->0)return;state=200;return;
        case 200:if(timer>=0&&--timer<0){timer=0;state=300;wait=0;}if(hp1==0||points1>=100||points2>=100){state=300;wait=0;}return;
        case 300:
            if(timer==0){int a=max1==0?0:1000*hp1/max1,b=max2==0?0:1000*hp2/max2;if(a<b)points2+=event.value(17);if(b<a)points1+=event.value(44);}
            int opponentPoints=points2>=100?points2:0;
            state=points1==opponentPoints?430:points1>opponentPoints?410:420;winner=state==410?0:state==420?1:-1;if(state!=410&&(!rules.decisiveRounds||state!=430))losses++;return;
        case 410:state=411;wins1++;show(0x4451e0);
        case 411:if(wait-->0)return;state=900;return;
        case 420:state=421;wins2++;show(0x4451e2);
        case 421:if(wait-->0)return;state=900;return;
        case 430:state=431;if(!rules.decisiveRounds){wins1++;wins2++;}show(0x4451e8);
        case 431:if(wait-->0)return;state=900;return;
        case 900:state=901;show(0x4451c4);
        case 901:if(wait-->0)return;state=902;
        case 902:if(wins1>=rules.winsRequired||wins2>=rules.winsRequired)state=wins1>wins2||(event.value(5)&1)==0?1000:1001;else{state=100;wait=0;}return;
        case 100:case 1000:case 1001:return;
        default:throw new IllegalStateException("Story round phase "+state);
        }
    }
    void write(DataOutput o)throws IOException{StateIO.writeInts(o,new int[]{state,wait,timer,round,wins1,wins2,points1,points2,losses,winner});}
    void read(DataInput i)throws IOException{
        state=i.readInt();wait=i.readInt();timer=i.readInt();round=i.readInt();wins1=i.readInt();wins2=i.readInt();points1=i.readInt();points2=i.readInt();losses=i.readInt();winner=i.readInt();
        if(!Arrays.asList(100,110,111,112,200,300,410,411,420,421,430,431,900,901,902,1000,1001).contains(state)||wait< -1||timer< -1||round<1||round>(rules.decisiveRounds?1000000:100)||wins1<0||wins2<0||points1<0||points2<0||losses<0||winner< -1||winner>1)throw new IOException("Story round snapshot");
    }
}
