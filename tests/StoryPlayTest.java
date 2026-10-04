package org.elfen.engine;
import java.nio.file.*;import java.util.*;
/** Real combat driven only through InputFrame. No HP/AI/damage or win overrides. */
public final class StoryPlayTest {
    public static void main(String[] args)throws Exception{
        StoryCatalog c=StoryDataTest.load(args[0]);Pack p=StoryDataTest.packs.get(args.length>1?args[1]:"0170");int cpu=args.length>2?Integer.parseInt(args[2]):80;
        int seed=args.length>3?Integer.parseInt(args[3]):19;
        int limit=args.length>4?Integer.parseInt(args[4]):320000;
        int repeats=args.length>5?Integer.parseInt(args[5]):3;
        boolean classic=args.length>6&&Boolean.parseBoolean(args[6]);
        StoryController story=new StoryController(c,p,seed,cpu,classic);StoryReplay tape=new StoryReplay(story);StoryHistory history=new StoryHistory(story,120);int last=-1,mode=-1,stalled=0;String hp="";Map<Integer,String> checkpoints=new TreeMap<>();
        System.out.println("START character="+p.id+" name="+p.name+" seed="+seed+" cpu="+cpu);
        for(int f=0;f<limit&&!story.finished();f++){
            int input=0;
            if(story.mode()==StoryController.SCENE&&story.demo.frame()>1600&&story.demo.frame()%30==0)input=16;
            if(story.mode()==StoryController.CONTINUE&&story.demo.frame()>30&&story.demo.frame()%30==0)input=16;
            InputFrame frame=new InputFrame(f,input,0);tape.append(frame);history.advance(frame);
            if(story.frame()%1000==0||story.slot()!=last||story.mode()!=mode||story.finished())checkpoints.put(story.frame(),story.stateHash());
            if(story.slot()!=last||story.mode()!=mode){System.out.println("f="+story.frame()+" slot="+story.slot()+" mode="+story.mode()+" wins="+story.wins()+" hp="+(story.battle==null?"scene":story.battle.life(0)+":"+story.battle.life(1)));last=story.slot();mode=story.mode();stalled=0;}
            if(story.battle!=null&&story.mode()==StoryController.FIGHT){String now=story.battle.life(0)+":"+story.battle.life(1);stalled=now.equals(hp)?stalled+1:0;hp=now;if(stalled>15000)throw new AssertionError("CPU stalled at slot "+story.slot()+" HP="+hp+" x="+(story.battle.entities[0].x>>16)+":"+(story.battle.entities[1].x>>16));}
        }
        System.out.println("ROUTE with per-frame history snapshots, frames="+story.frame()+" slot="+story.slot()+" mode="+story.mode()+" wins="+story.wins()+" fights="+story.fights()+" hash="+story.stateHash());
        if(!story.finished())throw new AssertionError("Route frame bound");
        StoryReplay replay=StoryReplay.decode(tape.encode());Files.write(Paths.get("research/story-"+p.id+"-seed"+seed+(classic?"-first2":"")+".efs"),tape.encode());
        for(int run=0;run<repeats;run++){StoryController twin=new StoryController(c,p,0,cpu,classic);replay.restoreStart(twin);for(int f=0;f<replay.length();f++){twin.step(replay.input(f));String expected=checkpoints.get(twin.frame());if(expected!=null&&!expected.equals(twin.stateHash()))throw new AssertionError("Story replay desync frame "+twin.frame());if(f%5003==0){byte[] state=twin.snapshot();String hash=twin.stateHash();twin.restore(state);if(!hash.equals(twin.stateHash()))throw new AssertionError("Snapshot hash");}}if(!story.stateHash().equals(twin.stateHash()))throw new AssertionError("Story replay desync");System.out.println("PASS full route replay "+run+" checkpoints="+checkpoints.size()+" "+twin.stateHash());}
    }
}
