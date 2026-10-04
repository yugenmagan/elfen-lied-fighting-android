package org.elfen.engine;
import java.io.*;import java.util.*;
/** Fixed-tick match. A snapshot contains the complete round, combat, RNG and KGT VM state. */
public final class MatchController {
    public static final int TICK_HZ=BattleSimulation.TICK_HZ;
    final Pack a,b,stage,kgt;final MatchRules rules;final String identity;BattleSimulation battle;RoundController round;MatchOverlay overlay;int frame;
    private final ArrayList<BattleSimulation.Event> events=new ArrayList<>();
    public MatchController(Pack a,Pack b,Pack stage,Pack kgt,int seed,MatchRules rules){this.a=a;this.b=b;this.stage=stage;this.kgt=kgt;this.rules=rules;identity="ELF-MATCH-1:combat="+BattleSimulation.RULESET_REVISION+":"+a.hash+":"+b.hash+":"+stage.hash+":"+kgt.hash;round=new RoundController(rules);newRound(seed,null);}
    private void newRound(int seed,BattleSimulation old){battle=new BattleSimulation(a,b,stage,kgt,seed);battle.setCpu(0,rules.cpu1);battle.setCpu(1,rules.cpu2);if(old!=null)for(int p=0;p<2;p++){battle.entities[p].stocks=old.entities[p].stocks;battle.entities[p].special=old.entities[p].special;}battle.beginPresentation();overlay=new MatchOverlay(kgt);overlay.start(rules.skill(0x4451c2));}
    public int frame(){return frame;}public String identity(){return identity;}public MatchState state(){return new MatchState(frame,round);}public BattleView view(){return battle.view();}public List<BattleView.Sprite> overlays(){return overlay.view();}public List<BattleSimulation.Event> events(){return Collections.unmodifiableList(new ArrayList<>(events));}
    public void step(InputFrame input){
        if(input.frame!=frame)throw new IllegalArgumentException("Match input frame "+input.frame+" != "+frame);events.clear();if(round.state==1000)throw new IllegalStateException("Match has finished");
        if(round.state==100){round.beginNextRound();newRound(battle.rng.state(),battle);events.add(new BattleSimulation.Event(frame,-1,0,"round-start","",round.round,null));frame++;return;}
        InputFrame local=new InputFrame(battle.frame(),input.samples(0),input.samples(1));for(int p=0;p<2;p++){int[] h=input.virtualHistory(p);if(h!=null)local=local.withCpuHistory(p,h);}
        int before=round.state;overlay.step(frame);if(before==200)battle.step(local);else battle.stepPresentation(local);
        round.step(battle.life(0),a.life(),battle.life(1),b.life());
        // Result flag is set by the original 510/520/530/540 phase, after adjudication.
        if(before==510||before==520||before==530||before==540)battle.endRound(round.winner);
        for(int skill:round.started)overlay.start(skill);
        for(BattleSimulation.Event e:battle.events())events.add(new BattleSimulation.Event(frame,e.emitter,events.size(),e.kind,e.packId,e.value,e.instruction()));
        for(BattleSimulation.Event e:overlay.events)events.add(new BattleSimulation.Event(frame,e.emitter,events.size(),e.kind,e.packId,e.value,e.instruction()));
        if(round.state==1000)events.add(new BattleSimulation.Event(frame,-1,events.size(),"match-end","",state().winner,null));frame++;
    }
    public byte[] snapshot(){try{ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream o=new DataOutputStream(b);o.writeInt(0x45464d53);o.writeInt(rules.decisiveRounds?2:1);o.writeUTF(identity);rules.write(o);o.writeInt(frame);round.write(o);byte[] core=battle.snapshot();o.writeInt(core.length);o.write(core);overlay.write(o);o.flush();return b.toByteArray();}catch(IOException e){throw new IllegalStateException(e);}}
    public void restore(byte[] bytes){
        if(bytes.length>16_000_000)throw new IllegalArgumentException("Match snapshot too large");
        try(DataInputStream i=new DataInputStream(new ByteArrayInputStream(bytes))){
            if(i.readInt()!=0x45464d53)throw new IOException("Match snapshot signature");int version=i.readInt();if((version!=1&&version!=2)||!i.readUTF().equals(identity))throw new IOException("Match snapshot identity");MatchRules read=MatchRules.read(kgt,i);read=new MatchRules(kgt,read.winsRequired,read.timeSetting,read.cpu1,read.cpu2,version==2);if(!rules.same(read))throw new IOException("Match rules differ");int f=i.readInt();if(f<0)throw new IOException("Negative match frame");
            RoundController r=new RoundController(rules);r.read(i);byte[] core=new byte[StateIO.count(i,8_000_000)];i.readFully(core);BattleSimulation c=new BattleSimulation(a,b,stage,kgt,0);c.restore(core);MatchOverlay ov=new MatchOverlay(kgt);ov.read(i);if(i.read()!=-1)throw new IOException("Trailing match snapshot");
            frame=f;round=r;battle=c;overlay=ov;events.clear();
        }catch(IOException e){throw new IllegalArgumentException("Invalid match snapshot",e);}
    }
    public String stateHash(){return StateIO.hex(StateIO.hash(snapshot()));}
}
