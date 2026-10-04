package org.elfen.engine;

import java.nio.file.*;import java.util.*;import org.elfen.controls.PauseGate;import org.elfen.controls.FrameInput;

/** Regression gates for commands, pause, classic rounds and explicit save migration. */
public final class Offline007bTest {
    static StoryCatalog catalog;static Pack a,b,k,stage;static int checks,commands;
    static void check(boolean condition,String why){checks++;if(!condition)throw new AssertionError(why);}
    static Pack pack(String id){return StoryDataTest.packs.get(id);}
    static MatchController match(){return new MatchController(a,b,stage,k,19,new MatchRules(k,2,60,0,0,true));}
    static void tick(MatchController m){m.step(new InputFrame(m.frame(),0,0));}
    static void ready(MatchController m){for(int n=0;!m.state().fighting&&n<1000;n++)tick(m);check(m.state().fighting,"intro reached fight");}
    static void ready(StoryController s){for(int n=0;(s.mode()!=StoryController.FIGHT||s.phase()!=200)&&n<15000;n++)s.step(new InputFrame(s.frame(),n%30==0?Input.A:0,0));check(s.mode()==StoryController.FIGHT&&s.phase()==200,"story reached fight");}
    static void result(MatchController m,int winner,boolean timeout){
        ready(m);int round=m.state().roundNumber;
        m.battle.entities[0].life=winner==0?a.life():0;m.battle.entities[1].life=winner==1?b.life():0;
        if(timeout){m.battle.entities[0].life=a.life();m.battle.entities[1].life=b.life();m.round.timer=0;}
        MatchController twin=match();twin.restore(m.snapshot());int n=0;
        while(!m.state().finished&&m.state().roundNumber==round&&n++<3000){tick(m);tick(twin);check(m.stateHash().equals(twin.stateHash()),"cross-round snapshot twin");}
        check(n<3000,"result presentation completed");
    }
    static void rounds(){
        for(int[] winners:new int[][]{{0,0},{1,1},{0,1,0},{0,1,1},{1,0,0},{1,0,1}}){
            MatchController m=match();int[] score={0,0};
            for(int n=0;n<winners.length;n++){
                result(m,winners[n],false);score[winners[n]]++;
                check(m.state().score1==score[0]&&m.state().score2==score[1],"score");
                check(m.state().finished==(n==winners.length-1),"first to two termination");
                if(!m.state().finished){check(m.battle.life(0)==a.life()&&m.battle.life(1)==b.life(),"HP reset");check(m.state().timer==59,"timer reset");check(m.battle.entities[0].x==match().battle.entities[0].x,"P1 position reset");check(m.battle.entities[1].x==match().battle.entities[1].x,"P2 position reset");check(m.battle.activeObjects()==0,"no old projectile");}
            }
            check(m.state().winner==winners[winners.length-1],"winner");
        }
        for(boolean timeout:new boolean[]{false,true}){
            MatchController m=match();result(m,0,false);result(m,1,false);result(m,-1,timeout);
            check(!m.state().finished&&m.state().score1==1&&m.state().score2==1,"draw at 1:1 repeats round");result(m,0,false);check(m.state().finished&&m.state().score1==2&&m.state().score2==1,"draw followed by deciding win");
        }
        MatchController m=match();ready(m);m.battle.entities[0].stocks=2;m.battle.entities[0].special=37;result(m,0,false);
        check(m.battle.entities[0].stocks==2&&m.battle.entities[0].special==37,"original meter carry retained");
        // A version-2 policy must not be silently interpreted by original rules.
        MatchController legacy=new MatchController(a,b,stage,k,19,new MatchRules(k,2,60,0,0));
        try{legacy.restore(m.snapshot());throw new AssertionError("policy mismatch accepted");}catch(IllegalArgumentException expected){checks++;}
    }
    static void story(){
        for(boolean playerWins:new boolean[]{true,false}){
            StoryController s=new StoryController(catalog,a,19,0,true);ready(s);int slot=s.slot();
            for(int r=1;r<=2;r++){
                // Original score adjudication is tested directly; no combat changes.
                s.round.state=300;s.round.points1=playerWins?100:0;s.round.points2=playerWins?0:100;
                int n=0;while(s.mode()==StoryController.FIGHT&&s.slot()==slot&&s.roundNumber()==r&&n++<3000){
                    byte[] snapshot=s.snapshot();s.step(new InputFrame(s.frame(),0,0));String expected=s.stateHash();
                    StoryController twin=new StoryController(catalog,a,0,0,true);twin.restore(snapshot);twin.step(new InputFrame(twin.frame(),0,0));check(expected.equals(twin.stateHash()),"story round transition restore");
                }
                check(n<3000,"story result presentation");
                if(r==1){check(s.slot()==slot&&s.mode()==StoryController.FIGHT&&s.roundNumber()==2,"no story progression after one win");check(s.score(playerWins?0:1)==1,"story score persists");ready(s);}
            }
            check(playerWins?s.slot()!=slot:s.mode()==StoryController.CONTINUE,"story advances/continues after two wins");
        }
        StoryController legacy=new StoryController(catalog,a,19);ready(legacy);
        byte[] oldBattle=legacy.battle.snapshot();int oldSlot=legacy.slot(),oldFrame=legacy.frame();
        StoryController migrated=StoryController.loadOfflineSave(catalog,a,legacy.snapshot());
        check(migrated.classicRounds()&&migrated.roundsToWin()==2,"old save upgraded explicitly");
        check(oldSlot==migrated.slot()&&oldFrame==migrated.frame(),"old progress preserved");check(Arrays.equals(oldBattle,migrated.battle.snapshot()),"old battle exactly preserved");
        StoryController again=StoryController.loadOfflineSave(catalog,a,migrated.snapshot());check(again.stateHash().equals(migrated.stateHash()),"new save restores without migration");
        StoryController s=new StoryController(catalog,a,19,0,true);ready(s);int slot=s.slot();s.round.state=300;s.round.points1=s.round.points2=100;
        for(int i=0;i<3000&&s.roundNumber()==1;i++)s.step(new InputFrame(s.frame(),0,0));
        check(s.slot()==slot&&s.roundNumber()==2&&s.score(0)==0&&s.score(1)==0,"story draw gives no score");
    }
    static void guides(){
        int[] directions={0,0,Input.RIGHT,Input.DOWN|Input.RIGHT,Input.DOWN,Input.DOWN|Input.LEFT,Input.LEFT,Input.UP|Input.LEFT,Input.UP,Input.UP|Input.RIGHT,Input.LEFT,Input.UP,Input.RIGHT,Input.DOWN};
        for(Pack p:StoryDataTest.packs.values())if(p.kind.equals(".player")){
            CommandGuide guide=new CommandGuide(p);check(guide.unsupported==0,"all shipped command modes understood "+p.id);
            check(guide.buttons==(p.id.equals("0142")||p.id.equals("0144")?0xf0:p.id.equals("0154")?0x30:0x70),"audited button mask "+p.id);
            for(CommandGuide.Entry entry:guide.entries){
                Pack.Command c=p.commands[entry.index];check(entry.name.equals(c.name),"original command name");check(!entry.notation.isEmpty(),"nonempty notation");
                for(boolean left:new boolean[]{false,true})for(int stance=0;stance<4;stance++)if(c.skills[stance]!=0){
                    CommandRecognizer recognizer=new CommandRecognizer();int f=0;recognizer.applyFrame(new InputFrame(f++,0,0),0,left);
                    for(CommandGuide.Step step:entry.steps){
                        int mask=directions[step.direction]|step.buttons;if(left)mask=BattleSimulation.relative(mask,true);
                        // A new press is separated by a neutral sample, not an invented game action.
                        if(step.buttons!=0)recognizer.applyFrame(new InputFrame(f++,0,0),0,left);
                        for(int n=0;n<Math.max(1,step.holdFrames);n++)recognizer.applyFrame(new InputFrame(f++,mask,0),0,left);
                    }
                    check(recognizer.match(p,false,stance,entry.index)==entry.index,"guide recognized "+p.id+":"+entry.index+" "+entry.notation+" stance="+stance+" left="+left);commands++;
                }
            }
        }
        CommandGuide lucy=new CommandGuide(a);check(lucy.entries.get(4).notation.equals("A + B + C"),"simultaneous ABC shown");
        CommandGuide nana=new CommandGuide(b);check(nana.entries.get(16).notation.equals("↓  ,  ←  ,  A"),"Nana original can command");
    }
    static void multiPointer(){
        for(int[] order:new int[][]{{0,1,2},{0,2,1},{1,0,2},{1,2,0},{2,0,1},{2,1,0}}){
            Input input=new Input();FrameInput adapter=new FrameInput();CommandRecognizer c=new CommandRecognizer();
            c.applyFrame(new InputFrame(0,adapter.poll(input),new int[]{0}),0,false);
            for(int i:order)input.set(20+i,16<<i);
            c.applyFrame(new InputFrame(1,adapter.poll(input),new int[]{0}),0,false);
            check(c.match(a,false,1,4)==4,"real three-pointer ABC order "+Arrays.toString(order));
            c.applyFrame(new InputFrame(2,adapter.poll(input),new int[]{0}),0,false);
            input.set(20,0);input.set(21,0);input.set(22,0);adapter.poll(input);
        }
        Input input=new Input();FrameInput adapter=new FrameInput();CommandRecognizer c=new CommandRecognizer();
        c.applyFrame(new InputFrame(0,adapter.poll(input),new int[]{0}),0,false);
        for(int direction:new int[]{Input.DOWN,Input.DOWN|Input.RIGHT,Input.RIGHT})input.set(9,direction);
        input.set(20,Input.A);input.set(20,0);
        int[] samples=adapter.poll(input);check(Arrays.equals(samples,new int[]{8,10,2,2,2,18}),"rolling directions and short press preserved");
        c.applyFrame(new InputFrame(1,samples,new int[]{0}),0,false);check(c.match(a,false,1,11)==11,"one-tick quarter circle still matches");
        check(Arrays.equals(adapter.poll(input),new int[]{2}),"short tap releases next tick");
        input.set(20,Input.A);adapter.poll(input);input.set(20,0);input.set(20,Input.A);
        check(Arrays.equals(adapter.poll(input),new int[]{2,2,18}),"release/repress in same tick retained");
        input.clear();adapter.reset();check(Arrays.equals(adapter.poll(input),new int[]{0}),"pause clears pending buttons");
    }
    static void pause(){
        PauseGate gate=new PauseGate();MatchController m=match();gate.foreground(true);ready(m);
        int frame=m.frame();byte[] state=m.snapshot();gate.request();gate.openModal();
        for(int n=0;n<1000;n++)if(!gate.blocked())tick(m);
        gate.openModal();gate.closeModal();check(gate.hasModal()&&gate.blocked(),"nested modal keeps pause");gate.closeModal();
        for(int n=0;n<1000;n++)if(!gate.blocked())tick(m);
        gate.foreground(false);gate.foreground(true);check(gate.blocked(),"lifecycle preserves manual pause");
        check(frame==m.frame()&&Arrays.equals(state,m.snapshot()),"pause freezes entire snapshot");
        gate.openModal();gate.resume();check(gate.blocked(),"resume waits for dialog dismissal");gate.closeModal();check(!gate.blocked(),"explicit resume");
        MatchController twin=match();twin.restore(state);tick(twin);if(!gate.blocked())tick(m);check(m.stateHash().equals(twin.stateHash()),"resume starts at exact next frame");
    }
    public static void main(String[] args)throws Exception{
        catalog=StoryDataTest.load(args[0]);a=pack("0170");b=pack("0104");k=pack("0116");stage=pack("0080");
        guides();multiPointer();System.out.println("PASS guide recognition cases="+commands);pause();System.out.println("PASS pause modal/lifecycle/state freeze");rounds();System.out.println("PASS VS first-to-two, draws, state reset, cross-round restore");story();System.out.println("PASS story first-to-two, draws, progression gating, save migration");System.out.println("PASS assertions="+checks);
    }
}
