package org.elfen.engine;

import java.io.*;
import java.util.*;
import static org.elfen.engine.Pack.*;

/** Original task 16 (406c10), including its ten-tick input guard and release latch.
 * A scene is a set of concurrent original VMs, never a timed slideshow.
 */
public final class DemoSession implements Script.Host {
    public final Pack pack;
    private final int[] globals=new int[16];
    private final ArrayList<Script> scripts=new ArrayList<>();
    private final ArrayList<BattleSimulation.Event> events=new ArrayList<>();
    int frame,phase,guard,remaining;
    boolean released;
    public DemoSession(Pack pack){
        if(!pack.kind.equals(".demo"))throw new IllegalArgumentException("DEMO pack required");
        this.pack=pack;remaining=pack.demoTime;
        // 406790: skip blank names/sentinel; a root must contain an I instruction.
        // The type-3 divider changes the depth of subsequent roots from 12 to 100.
        int depth=12;
        for(int i=1;i<pack.starts.length-1;i++){
            if(pack.skillNames[i].isEmpty())continue;
            if(pack.types[i]==3)depth=100;
            boolean image=false;for(int p=pack.starts[i];p<pack.end(i);p++)if(u(pack.code,p*16)==12)image=true;
            if(!image)continue;
            Script s=new Script(pack,this,globals,false);s.absolute=true;s.depth=depth;s.start(i,(pack.types[i]&32)==0);scripts.add(s);
        }
    }
    public int frame(){return frame;}
    public boolean finished(){return phase==3;}
    public boolean canAdvance(){return guard>=10&&released&&(pack.skipInput&1)!=0;}
    public void step(InputFrame input){
        if(input.frame!=frame||finished())throw new IllegalArgumentException("DEMO input frame/state");events.clear();
        // Tasks created by the depth-127 manager first execute on the next tick.
        if(phase==0)phase=1;
        else{
            for(Script s:scripts)if(!s.ended)s.animationTick(false);
            for(Script s:scripts)if(!s.ended)s.integrate();
        }
        if(phase==2){phase=3;frame++;return;}
        // Original manager is depth 127, after the scene's tasks. Holding an attack
        // from the preceding fight cannot skip a newly entered scene.
        int buttons=input.mask(0)&0x3f0;
        if(guard<10)guard++;
        else if((pack.skipInput&1)!=0){if(buttons==0)released=true;else if(released){phase=2;frame++;return;}}
        if(remaining!=0&&--remaining==0)phase=2;
        frame++;
    }
    public List<BattleView.Sprite> view(){
        ArrayList<BattleView.Sprite> out=new ArrayList<>();for(int i=0;i<scripts.size();i++){Script s=scripts.get(i);if(!s.ended&&s.image>=0)out.add(new BattleView.Sprite(i,s,true));}
        out.sort((a,b)->{int d=Integer.compare(a.depth,b.depth);return d!=0?d:Integer.compare(a.id,b.id);});return Collections.unmodifiableList(out);
    }
    public List<BattleSimulation.Event> events(){return Collections.unmodifiableList(new ArrayList<>(events));}
    public boolean explicitEnd(Script s){return true;} // 412617: non-character task E terminates.
    public void sound(Pack p,int n){if(n<0||n>=p.sounds.length||n>0&&p.sounds[n].isEmpty())throw new IllegalStateException("Missing DEMO sound "+p.id+":"+n);if(n>0)events.add(new BattleSimulation.Event(frame,-1,events.size(),"sound",p.id,n,null));}
    public void spawn(Script s,byte[] b){throw s.fault("OO is absent from the audited 27 DEMO files");}
    public void gauge(Script s,int a,int b,int c,int d){throw s.fault("DEMO cannot change fighter gauges");}
    public byte[] snapshot(){try{
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream o=new DataOutputStream(bytes);o.writeInt(0x45464453);o.writeInt(1);o.writeUTF(pack.hash);
        StateIO.writeInts(o,new int[]{frame,phase,guard,remaining});o.writeBoolean(released);StateIO.writeInts(o,globals);o.writeInt(scripts.size());for(Script s:scripts){StateIO.writeInts(o,s.charVars);s.writeState(o);}return bytes.toByteArray();
    }catch(IOException e){throw new IllegalStateException(e);}}
    public void restore(byte[] bytes){try(DataInputStream i=new DataInputStream(new ByteArrayInputStream(bytes))){
        if(bytes.length>1_000_000||i.readInt()!=0x45464453||i.readInt()!=1||!i.readUTF().equals(pack.hash))throw new IOException("DEMO identity");
        DemoSession d=new DemoSession(pack);d.frame=i.readInt();d.phase=i.readInt();d.guard=i.readInt();d.remaining=i.readInt();d.released=i.readBoolean();StateIO.readInts(i,d.globals);
        if(d.frame<0||d.phase<0||d.phase>3||d.guard<0||d.guard>10||d.remaining<0||i.readInt()!=d.scripts.size())throw new IOException("DEMO state");
        for(Script s:d.scripts){StateIO.readInts(i,s.charVars);s.readState(i);}if(i.read()!=-1)throw new IOException("Trailing DEMO state");
        frame=d.frame;phase=d.phase;guard=d.guard;remaining=d.remaining;released=d.released;System.arraycopy(d.globals,0,globals,0,16);
        scripts.clear();for(Script source:d.scripts){Script s=new Script(pack,this,globals,false);ByteArrayOutputStream b=new ByteArrayOutputStream();source.writeState(new DataOutputStream(b));s.readState(new DataInputStream(new ByteArrayInputStream(b.toByteArray())));System.arraycopy(source.charVars,0,s.charVars,0,16);scripts.add(s);}events.clear();
    }catch(IOException e){throw new IllegalArgumentException("Invalid DEMO snapshot",e);}}
    public String stateHash(){return StateIO.hex(StateIO.hash(snapshot()));}
}
