package org.elfen.engine;
import java.nio.file.*;import java.util.*;

/** Test-only human-side input pilot. It never edits HP, actions, AI or story events. */
public final class StoryInputPilotTest {
    static int pilot(StoryController story,int variant){
        if(variant==7)variant=new int[]{3,1,2,0,4,5}[story.losses%6];
        if(story.mode()==StoryController.SCENE)return story.demo.frame()>300&&story.demo.frame()%30==0?Input.A:0;
        if(story.mode()==StoryController.CONTINUE)return story.demo.frame()>30&&story.demo.frame()%30==0?Input.A:0;
        if(story.mode()!=StoryController.FIGHT||story.phase()!=200)return 0;
        Script p=story.battle.entities[0],q=story.battle.entities[1];int forward=p.facingLeft?Input.LEFT:Input.RIGHT,back=p.facingLeft?Input.RIGHT:Input.LEFT;
        int t=story.frame(),distance=Math.abs(p.x/65536-q.x/65536);
        switch(variant){
            case 0:return (distance>80?forward:0)|(t%16<8?Input.A:0);
            case 1:return (distance>120?forward:0)|(t%16<8?Input.B:0);
            case 2:{int f=t%60;return f==0?0:f==1?Input.DOWN:f==2?Input.DOWN|forward:f==3?forward:f==4?forward|Input.A:back;}
            case 3:{int f=t%120;return f<95?back:f==95?forward|Input.A:f<105?forward:0;}
            case 4:return (distance>220?forward:back)|(t%20<10?Input.C:0);
            case 5:{int f=t%60;return f==0?0:f==1?Input.DOWN:f==2?Input.DOWN|forward:f==3?forward:f==4?forward|Input.B:back;}
            case 6:return (distance>100?forward:Input.DOWN)|(t%20<10?Input.D:0);
            default:throw new IllegalArgumentException("Pilot variant");
        }
    }
    public static void main(String[] args)throws Exception{
        StoryCatalog c=StoryDataTest.load(args[0]);Pack player=StoryDataTest.packs.get("0128");int variant=Integer.parseInt(args[1]);int limit=args.length>2?Integer.parseInt(args[2]):40000;
        StoryController story=new StoryController(c,player,19,0);StoryReplay tape=new StoryReplay(story);StoryHistory history=new StoryHistory(story,120);Map<Integer,String> hashes=new TreeMap<>();int last=-1,mode=-1;
        for(int f=0;f<limit&&!story.finished();f++){
            InputFrame in=new InputFrame(f,pilot(story,variant),0);tape.append(in);history.advance(in);
            if(story.frame()%1000==0||story.slot()!=last||story.mode()!=mode||story.finished())hashes.put(story.frame(),story.stateHash());
            if(story.slot()!=last||story.mode()!=mode){last=story.slot();mode=story.mode();System.out.println("variant="+variant+" frame="+story.frame()+" slot="+last+" mode="+mode+" hp="+(story.battle==null?"scene":story.battle.life(0)+":"+story.battle.life(1)));}
        }
        System.out.println("PILOT variant="+variant+" complete="+story.finished()+" wins="+story.wins()+" slot="+story.slot()+" hash="+story.stateHash());
        if(story.finished()){
            byte[] bytes=tape.encode();Files.write(Paths.get("research/story-0128-input-pilot"+variant+".efs"),bytes);StoryReplay replay=StoryReplay.decode(bytes);
            StoryController twin=new StoryController(c,player,0,0);replay.restoreStart(twin);for(int f=0;f<replay.length();f++){twin.step(replay.input(f));String h=hashes.get(twin.frame());if(h!=null&&!h.equals(twin.stateHash()))throw new AssertionError("Pilot replay hash "+f);if(f%5003==0)twin.restore(twin.snapshot());}
            if(!twin.stateHash().equals(story.stateHash()))throw new AssertionError("Final hash");System.out.println("PASS input-only full Mariko route frames="+story.frame()+" fights="+story.fights()+" wins="+story.wins()+" checkpoints="+hashes.size()+" hash="+story.stateHash());
        }
    }
}
