package org.elfen.engine;
import java.util.*;

/** Forced outcome fixtures test progression only; they are not natural boss victories. */
public final class FinalBoss008Test {
    static int checks;
    static void check(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
    static StoryController finalFight(StoryCatalog c,Pack p,boolean strict)throws Exception{
        StoryController s=new StoryController(c,p,19,0,true,strict);
        s.slot=s.program.finalFightSlot()-1;
        java.lang.reflect.Method a=StoryController.class.getDeclaredMethod("advance");a.setAccessible(true);a.invoke(s);
        return s;
    }
    static void outcome(StoryController s,boolean win){
        s.round.state=902;s.round.wins1=win?2:0;s.round.wins2=win?0:2;s.round.winner=win?0:1;
        s.battle.entities[win?1:0].life=0;
    }
    static void step(StoryController s,int mask){s.step(new InputFrame(s.frame(),mask,0));}
    static void readyContinue(StoryController s){for(int n=0;n<12;n++)step(s,0);}
    public static void main(String[] args)throws Exception{
        StoryCatalog c=StoryDataTest.load(args[0]);int routes=0;
        for(Pack p:c.protagonists()){
            routes++;StoryController s=finalFight(c,p,true);int slot=s.slot();
            check(slot==18,"audited final slot "+p.id);check(s.program.event(slot).value(5)==0,"original loss-advance flag");
            outcome(s,false);StoryController twin=new StoryController(c,p,0,0,true,true);twin.restore(s.snapshot());
            step(s,0);step(twin,0);check(s.stateHash().equals(twin.stateHash()),"loss snapshot "+p.id);
            check(s.mode()==StoryController.CONTINUE&&s.slot()==slot&&s.wins()==0,"defeat must not enter ending "+p.id);
            byte[] retry=s.snapshot();twin.restore(retry);check(s.stateHash().equals(twin.stateHash()),"Continue restore");
            readyContinue(s);step(s,Input.A);check(s.mode()==StoryController.FIGHT&&s.slot()==slot,"retry same boss");
            check(s.battle.life(0)==p.life()&&s.score(0)==0&&s.score(1)==0,"retry resets match");
            outcome(s,false);step(s,0);check(s.mode()==StoryController.CONTINUE&&s.slot()==slot,"repeated defeat remains Continue");
            readyContinue(s);step(s,Input.A);outcome(s,true);
            StoryReplay tape=new StoryReplay(s);twin.restore(s.snapshot());
            for(int n=0;n<40;n++){InputFrame f=new InputFrame(s.frame(),n==15?Input.A:0,0);tape.append(f);s.step(f);twin.step(f);check(s.stateHash().equals(twin.stateHash()),"victory twin");}
            check(s.slot()>slot&&s.wins()==1,"win opens ending");
            StoryReplay replay=StoryReplay.decode(tape.encode());replay.restoreStart(twin);for(int n=0;n<replay.length();n++)twin.step(replay.input(n));check(s.stateHash().equals(twin.stateHash()),"replay final victory");
            for(int n=0;n<100&&!s.finished();n++)step(s,n%20==15?Input.A:0);
            check(s.mode()==StoryController.COMPLETE,"victory -> ending -> credits -> complete");
            s.restore(retry);readyContinue(s);step(s,Input.DOWN);step(s,Input.A);check(s.mode()==StoryController.EXIT,"defeat -> quit");
            StoryController old=finalFight(c,p,false);outcome(old,false);step(old,0);check(old.mode()==StoryController.SCENE,"legacy replay behaviour preserved");
            StoryController migrated=StoryController.loadOfflineSave(c,p,old.snapshot());
            check(migrated.requiresFinalVictory()&&migrated.mode()==StoryController.CONTINUE&&migrated.slot()==slot,"legacy losing ending migration");
            twin.restore(migrated.snapshot());check(migrated.stateHash().equals(twin.stateHash()),"migrated Continue restore without stale battle");
            old=finalFight(c,p,false);outcome(old,true);step(old,0);migrated=StoryController.loadOfflineSave(c,p,old.snapshot());
            check(migrated.mode()==StoryController.SCENE&&migrated.slot()==old.slot(),"earned ending survives migration");
            old=finalFight(c,p,false);step(old,0);byte[] battle=old.battle.snapshot();migrated=StoryController.loadOfflineSave(c,p,old.snapshot());
            check(Arrays.equals(battle,migrated.battle.snapshot()),"migration preserves battle/RNG");
            boolean rejected=false;try{twin.restore(old.snapshot());}catch(IllegalArgumentException expected){rejected=true;}
            check(rejected,"policy-mismatched replay must be rejected");
        }
        check(routes==10,"ten selectable story routes");
        System.out.println("PASS "+checks+" progression checks across "+routes+" routes: final defeat/retry/repeated defeat/quit/victory/ending/credits; twins, replay, snapshot and 007d save migration. Forced outcome fixtures, not natural full playthroughs.");
    }
}
