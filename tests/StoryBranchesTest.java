package org.elfen.engine;
import java.nio.file.*;import java.util.*;
/** Separate forced branch fixtures from natural-combat end-to-end StoryPlayTest. */
public final class StoryBranchesTest {
    public static void main(String[] args)throws Exception{
        StoryCatalog c=StoryDataTest.load(args[0]);int restores=0,fights=0;
        for(Pack p:c.protagonists()){
            StoryProgram program=new StoryProgram(p);
            for(int slot=0;slot<100;slot++){StoryProgram.Event e=program.event(slot);if(e.type!=1)continue;
                StoryController s=new StoryController(c,p,7);s.slot=slot-1;
                // Restore/transition coverage: move through the actual controller,
                // with HP changes only here, never in the natural route test.
                java.lang.reflect.Method advance=StoryController.class.getDeclaredMethod("advance");advance.setAccessible(true);advance.invoke(s);
                StoryController twin=new StoryController(c,p,0);twin.restore(s.snapshot());
                for(int f=0;f<750;f++){InputFrame in=new InputFrame(s.frame(),f%20<10?Input.RIGHT:Input.DOWN,0);s.step(in);twin.step(in);if(!s.stateHash().equals(twin.stateHash()))throw new AssertionError("Story fighter snapshot "+p.id+":"+slot+":"+f);}
                fights++;
            }
        }
        // Regression: a retired numbered object must not become Mayu skill96
        // after the same pool slot is reused by an unnumbered OO (flags4).
        Pack mayu=StoryDataTest.packs.get("0110");BattleSimulation objectSim=new BattleSimulation(StoryDataTest.packs.get("0170"),mayu,StoryDataTest.packs.get("0080"),c.kgt,19);
        Script owner=objectSim.entities[1];byte[] numbered=new byte[16];numbered[0]=4;numbered[2]=81;objectSim.spawn(owner,numbered);
        Script old=null;for(Script e:objectSim.entities)if(e!=null&&e!=owner&&e.pack==mayu&&e.skill==81)old=e;
        if(old==null)throw new AssertionError("Numbered object missing");int reused=objectSim.id(old);old.ended=true;
        objectSim.spawn(owner,Arrays.copyOfRange(mayu.code,1098*16,1099*16));Script replacement=objectSim.entities[reused];
        if(replacement.skill!=96)throw new AssertionError("Mayu pool-reuse fixture");replacement.animationTick(false);
        if(replacement.ended||objectSim.entities[reused]!=replacement)throw new AssertionError("Stale numbered slot deleted Mayu96");
        byte[] objectState=objectSim.snapshot();String objectHash=objectSim.stateHash();objectSim.restore(objectState);
        if(!objectHash.equals(objectSim.stateHash()))throw new AssertionError("Nested object reference restore");
        Pack lucy=StoryDataTest.packs.get("0170");StoryController s=new StoryController(c,lucy,19),twin=new StoryController(c,lucy,2);
        for(int f=0;f<12;f++)s.step(new InputFrame(s.frame(),0,0));byte[] beforeScene=s.snapshot();s.step(new InputFrame(s.frame(),16,0));s.step(new InputFrame(s.frame(),0,0));
        if(s.mode()!=StoryController.FIGHT)throw new AssertionError("Scene -> fight");
        while(s.phase()!=200)s.step(new InputFrame(s.frame(),0,0));
        s.battle.entities[0].life=0;s.round.points2=100;twin.restore(s.snapshot());
        while(s.mode()==StoryController.FIGHT){InputFrame in=new InputFrame(s.frame(),0,0);s.step(in);twin.step(in);if(!s.stateHash().equals(twin.stateHash()))throw new AssertionError("Loss transition snapshot");}
        if(s.mode()!=StoryController.CONTINUE)throw new AssertionError("Continue not offered");byte[] cont=s.snapshot();
        for(int f=0;f<12;f++)s.step(new InputFrame(s.frame(),0,0));s.step(new InputFrame(s.frame(),16,0));if(s.mode()!=StoryController.FIGHT||s.battle.life(0)!=lucy.life())throw new AssertionError("Retry restores real original round");
        s.restore(cont);for(int f=0;f<12;f++)s.step(new InputFrame(s.frame(),0,0));s.step(new InputFrame(s.frame(),Input.DOWN,0));s.step(new InputFrame(s.frame(),16,0));if(s.mode()!=StoryController.EXIT)throw new AssertionError("Continue -> menu");
        // Correct a late scene-skip input across the scene/fight boundary.
        s.restore(beforeScene);twin.restore(beforeScene);StoryHistory h=new StoryHistory(s,120);int start=s.frame();InputFrame[] correction=new InputFrame[40];
        for(int f=0;f<40;f++){h.advance(new InputFrame(start+f,0,0));correction[f]=new InputFrame(start+f,f==0?16:0,0);twin.step(correction[f]);}h.resimulate(start,correction);if(!s.stateHash().equals(twin.stateHash()))throw new AssertionError("Cross-scene rollback");
        String hash=s.stateHash();byte[] broken=s.snapshot();broken[0]^=1;try{s.restore(broken);throw new AssertionError("Corrupt snapshot accepted");}catch(IllegalArgumentException expected){}if(!hash.equals(s.stateHash()))throw new AssertionError("Non-atomic rejected restore");
        System.out.println("PASS 100 story-fight loading/twin runs (750 frames each), loss/retry/quit, cross-scene rollback, atomic rejected restore, Mayu nested-object slot reuse");
    }
}
