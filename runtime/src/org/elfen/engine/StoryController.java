package org.elfen.engine;

import java.io.*;import java.util.*;

/** Frame-addressed original single-player route. UI/audio consume detached output only. */
public final class StoryController {
    public static final int SCENE=0,FIGHT=1,CONTINUE=2,COMPLETE=3,EXIT=4;
    final StoryCatalog catalog;final StoryProgram program;final int p1Cpu;final String identity;final boolean classicRounds,requireFinalWin;
    int frame,slot=-1,mode,seed,previousResult,losses,lastLife,fightCount,wonCount,continueChoice,lastMenuInput;
    BattleSimulation battle;StoryRound round;DemoSession demo;MatchOverlay overlay;
    private final ArrayList<BattleSimulation.Event> events=new ArrayList<>();
    public StoryController(StoryCatalog catalog,Pack player,int seed){this(catalog,player,seed,0);}
    /** CPU P1 is a deterministic test/demo input source, not a story difficulty change. */
    public StoryController(StoryCatalog catalog,Pack player,int seed,int cpu){this(catalog,player,seed,cpu,false);}
    public StoryController(StoryCatalog catalog,Pack player,int seed,int cpu,boolean classicRounds){
        this(catalog,player,seed,cpu,classicRounds,false);
    }
    /** Explicit TEST008 story policy; legacy constructors remain replay-compatible. */
    public StoryController(StoryCatalog catalog,Pack player,int seed,int cpu,boolean classicRounds,boolean requireFinalWin){
        this.classicRounds=classicRounds;this.requireFinalWin=requireFinalWin;this.catalog=catalog;program=new StoryProgram(player);if(!program.available()||cpu<0||cpu>100)throw new IllegalArgumentException("Story protagonist/CPU");p1Cpu=cpu;this.seed=seed;lastLife=player.life();
        identity="ELF-STORY-1:combat="+BattleSimulation.RULESET_REVISION+":"+catalog.kgt.hash+":"+player.hash+":"+cpu+(classicRounds?":first-to-two-v1":"")+(requireFinalWin?":final-victory-v1":"");advance();
    }
    public int frame(){return frame;}public int mode(){return mode;}public int slot(){return slot;}
    public boolean finished(){return mode==COMPLETE||mode==EXIT;}public int fights(){return fightCount;}public int wins(){return wonCount;}
    public int continueChoice(){return continueChoice;}public String identity(){return identity;}
    public Pack scenePack(){return demo==null?null:demo.pack;}public boolean canAdvanceScene(){return demo!=null&&demo.canAdvance();}
    public Pack stagePack(){return battle==null?null:battle.packs[2];}
    public int timer(){return round==null?-1:round.timer<0?-1:round.timer/100;}public int roundNumber(){return round==null?0:round.round;}
    public int phase(){return round==null?-1:round.state;}
    public int roundsToWin(){return round==null?2:round.rules.winsRequired;}
    public boolean classicRounds(){return classicRounds;}
    public boolean requiresFinalVictory(){return requireFinalWin;}
    public int score(int p){return round==null?0:p==0?round.wins1:round.wins2;}
    public BattleView view(){return battle==null?null:battle.view();}
    public List<BattleView.Sprite> sceneView(){return demo==null?Collections.emptyList():demo.view();}
    public List<BattleView.Sprite> overlays(){return overlay==null?Collections.emptyList():overlay.view();}
    public List<BattleSimulation.Event> events(){return Collections.unmodifiableList(new ArrayList<>(events));}
    private void emit(String kind,Pack p,int value){events.add(new BattleSimulation.Event(frame,-1,events.size(),kind,p==null?"":p.id,value,null));}
    private void advance(){
        slot=program.next(slot,previousResult,lastLife,losses);StoryProgram.Event e=program.event(slot);battle=null;round=null;overlay=null;demo=null;
        if(e.type==0||e.type==4){mode=e.type==4?COMPLETE:EXIT;emit("story-end",program.player,mode);return;}
        if(e.type==2){mode=SCENE;demo=new DemoSession(catalog.demo(e.value(1)));emit("music",demo.pack,demo.pack.bgm);emit("story-scene",demo.pack,slot);return;}
        mode=FIGHT;round=new StoryRound(catalog.kgt,e,classicRounds);fightCount++;newRound();emit("story-fight",battle.packs[1],slot);
    }
    private void newRound(){
        StoryProgram.Event e=program.event(slot);BattleSimulation old=battle;
        battle=new BattleSimulation(program.player,catalog.character(e.enemy()),catalog.stage(e.value(1)),catalog.kgt,seed);battle.setCpu(0,p1Cpu);battle.setCpu(1,Math.min(100,e.value(29)));
        for(int p=0;p<2;p++){
            Script s=battle.entities[p];s.x=e.word(p==0?8:31)<<16;s.y=920<<16;s.ground=920<<16;
            battle.cachedX[p]=s.x;battle.cachedY[p]=s.y;
            if(old!=null){s.stocks=old.entities[p].stocks;s.special=old.entities[p].special;}
        }
        // P1 reset/relative recovery at 411d39; current game uses mode0 = full life.
        if(e.value(3)!=0&&old!=null){int recovery=e.value(4);battle.entities[0].life=recovery==100?program.player.life():Math.min(program.player.life(),old.life(0)+program.player.life()*recovery/100);}
        battle.cameraX=Math.max(0,Math.min(640,(e.word(8)+e.word(31))/2-320));battle.beginPresentation();
        overlay=new MatchOverlay(catalog.kgt);overlay.start(round.rules.skill(0x4451c2));emit("music",battle.packs[2],battle.packs[2].bgm);
    }
    private void append(List<BattleSimulation.Event> source){for(BattleSimulation.Event e:source)events.add(new BattleSimulation.Event(frame,e.emitter,events.size(),e.kind,e.packId,e.value,e.instruction()));}
    private InputFrame local(InputFrame in,int f){InputFrame r=new InputFrame(f,in.samples(0),in.samples(1));for(int p=0;p<2;p++){int[] h=in.virtualHistory(p);if(h!=null)r=r.withCpuHistory(p,h);}return r;}
    public void step(InputFrame input){
        if(input.frame!=frame||finished())throw new IllegalArgumentException("Story input frame/state");events.clear();
        if(mode==SCENE){demo.step(local(input,demo.frame()));append(demo.events());if(demo.finished())advance();}
        else if(mode==CONTINUE){
            // Continue uses the original DEMO as backdrop; its buttons belong to the
            // two-choice menu instead of the scene manager (original task13).
            demo.step(new InputFrame(demo.frame(),0,0));append(demo.events());int mask=input.mask(0);
            if((mask&15)!=0&&(lastMenuInput&15)==0)continueChoice^=1;
            if((mask&0x3f0)!=0&&(lastMenuInput&0x3f0)==0&&demo.frame()>10){
                if(continueChoice==0){mode=FIGHT;demo=null;round=new StoryRound(catalog.kgt,program.event(slot),classicRounds);battle=null;newRound();emit("story-retry",program.player,slot);}
                else{mode=EXIT;demo=null;emit("story-end",program.player,mode);}
            }lastMenuInput=mask;
        }else if(mode==FIGHT){
            if(round.state==100){round.beginNextRound();seed=battle.rng.state();newRound();frame++;return;}
            int before=round.state,hp1=battle.life(0),hp2=battle.life(1);overlay.step(frame);InputFrame local=local(input,battle.frame());
            if(before==200)battle.step(local);else battle.stepPresentation(local);
            if(before==200)for(int p=0;p<2;p++)if((p==0?hp1:hp2)>0&&battle.life(p)==0){int id=battle.lastContact[p];round.death(p,id>=0&&battle.entities[id]!=null?battle.player(battle.entities[id]):-1);}
            round.step(battle.life(0),program.player.life(),battle.life(1),battle.packs[1].life());
            if(before==410||before==420||before==430)battle.endRound(round.winner);
            for(int s:round.started)overlay.start(s);append(battle.events());append(overlay.events);
            if(round.state==1000||round.state==1001){
                seed=battle.rng.state();lastLife=battle.life(0);losses+=round.losses;previousResult=round.winner==0?1:0;
                if(round.wins1>round.wins2)wonCount++;
                boolean defeatedFinal=requireFinalWin&&slot==program.finalFightSlot()&&round.wins1<=round.wins2;
                if(round.state==1000&&!defeatedFinal)advance();
                else offerContinue(input.mask(0));
            }
        }
        frame++;
    }
    private void offerContinue(int lastInput){mode=CONTINUE;demo=new DemoSession(catalog.screen(4));overlay=null;continueChoice=0;lastMenuInput=lastInput;emit("music",demo.pack,demo.pack.bgm);emit("story-continue",program.player,slot);}
    public byte[] snapshot(){try{
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream o=new DataOutputStream(bytes);o.writeInt(0x45465353);o.writeInt(1);o.writeUTF(identity);
        StateIO.writeInts(o,new int[]{frame,slot,mode,seed,previousResult,losses,lastLife,fightCount,wonCount,continueChoice,lastMenuInput});
        o.writeBoolean(battle!=null);if(battle!=null){round.write(o);blob(o,battle.snapshot());o.writeBoolean(overlay!=null);if(overlay!=null)overlay.write(o);}
        o.writeBoolean(demo!=null);if(demo!=null){o.writeUTF(demo.pack.id);blob(o,demo.snapshot());}return bytes.toByteArray();
    }catch(IOException e){throw new IllegalStateException(e);}}
    static void blob(DataOutput o,byte[] b)throws IOException{o.writeInt(b.length);o.write(b);}static byte[] blob(DataInput i)throws IOException{byte[] b=new byte[StateIO.count(i,16_000_000)];i.readFully(b);return b;}
    public void restore(byte[] bytes){try(DataInputStream i=new DataInputStream(new ByteArrayInputStream(bytes))){
        if(bytes.length>20_000_000||i.readInt()!=0x45465353||i.readInt()!=1||!i.readUTF().equals(identity))throw new IOException("Story identity");
        StoryController s=new StoryController(catalog,program.player,0,p1Cpu,classicRounds,requireFinalWin);s.frame=i.readInt();s.slot=i.readInt();s.mode=i.readInt();s.seed=i.readInt();s.previousResult=i.readInt();s.losses=i.readInt();s.lastLife=i.readInt();s.fightCount=i.readInt();s.wonCount=i.readInt();s.continueChoice=i.readInt();s.lastMenuInput=i.readInt();
        if(s.frame<0||s.slot<0||s.slot>=100||s.mode<0||s.mode>4||s.losses<0||s.lastLife<0||s.lastLife>program.player.life()||s.fightCount<0||s.wonCount<0||s.wonCount>s.fightCount||s.continueChoice<0||s.continueChoice>1||(s.lastMenuInput&~1023)!=0)throw new IOException("Story state");
        s.battle=null;s.round=null;s.demo=null;s.overlay=null;StoryProgram.Event e=program.event(s.slot);
        if(i.readBoolean()){if(e.type!=1)throw new IOException("Story battle event");s.round=new StoryRound(catalog.kgt,e,classicRounds);s.round.read(i);s.battle=new BattleSimulation(program.player,catalog.character(e.enemy()),catalog.stage(e.value(1)),catalog.kgt,0);s.battle.restore(blob(i));if(i.readBoolean()){s.overlay=new MatchOverlay(catalog.kgt);s.overlay.read(i);}}
        if(i.readBoolean()){String id=i.readUTF();Pack p=s.mode==CONTINUE?catalog.screen(4):catalog.demo(e.value(1));if(!p.id.equals(id))throw new IOException("Story scene identity");s.demo=new DemoSession(p);s.demo.restore(blob(i));}
        if(i.read()!=-1||(s.mode==FIGHT&&(s.battle==null||s.overlay==null))||((s.mode==SCENE||s.mode==CONTINUE)&&s.demo==null))throw new IOException("Story snapshot components");
        frame=s.frame;slot=s.slot;mode=s.mode;seed=s.seed;previousResult=s.previousResult;losses=s.losses;lastLife=s.lastLife;fightCount=s.fightCount;wonCount=s.wonCount;continueChoice=s.continueChoice;lastMenuInput=s.lastMenuInput;battle=s.battle;round=s.round;demo=s.demo;overlay=s.overlay;events.clear();
    }catch(IOException e){throw new IllegalArgumentException("Invalid story snapshot",e);}}
    /** Explicit save migration, not replay migration. Preserve current battle/VM/RNG.
     * A legacy ending reached after final defeat returns to that boss's Continue. */
    public static StoryController loadOfflineSave(StoryCatalog catalog,Pack player,byte[] bytes){
        String id;
        try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes))){
            if(in.readInt()!=0x45465353||in.readInt()!=1)throw new IOException("Story save header");id=in.readUTF();
        }catch(IOException e){throw new IllegalArgumentException("Invalid story save",e);}
        StoryController current=new StoryController(catalog,player,0,0,true,true);
        if(id.equals(current.identity)){current.restore(bytes);return current;}
        StoryController old=new StoryController(catalog,player,0,0,true);
        if(!id.equals(old.identity))old=new StoryController(catalog,player,0,0);
        old.restore(bytes);
        // Re-encode only the rules identity, then validate every component with the new rules.
        try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes));ByteArrayOutputStream b=new ByteArrayOutputStream()){
            DataOutputStream out=new DataOutputStream(b);out.writeInt(in.readInt());out.writeInt(in.readInt());in.readUTF();out.writeUTF(current.identity);
            byte[] rest=new byte[in.available()];in.readFully(rest);out.write(rest);current.restore(b.toByteArray());
            if(current.slot>current.program.finalFightSlot()&&current.previousResult==0&&current.fightCount>current.wonCount){
                current.slot=current.program.finalFightSlot();current.battle=null;current.round=null;current.offerContinue(0);
            }
            return current;
        }catch(IOException e){throw new IllegalArgumentException("Story save migration",e);}
    }
    public String stateHash(){return StateIO.hex(StateIO.hash(snapshot()));}
}
