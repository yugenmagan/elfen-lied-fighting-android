package org.elfen.engine;
import java.io.*;import java.util.*;import static org.elfen.engine.Pack.*;
/** KGT system animation VM. Visual/audio output cannot alter the combat state. */
final class MatchOverlay implements Script.Host {
    final Pack pack;final int[] globals=new int[16];final ArrayList<Script> scripts=new ArrayList<>();final ArrayList<BattleSimulation.Event> events=new ArrayList<>();int frame;
    MatchOverlay(Pack k){pack=k;}
    void start(int skill){if(skill==0)return;Script s=new Script(pack,this,globals,false);s.depth=101;s.absolute=true;s.start(skill,(pack.types[skill]&32)==0);add(s);}
    private void add(Script s){if(scripts.size()>=1024)throw s.fault("System task limit");scripts.add(s);}
    void step(int frame){this.frame=frame;events.clear();ArrayList<Script> order=new ArrayList<>(scripts);order.sort((a,b)->Integer.compare(a.depth,b.depth));for(Script s:order)if(!s.ended)s.animationTick(false);for(Script s:scripts)if(!s.ended)s.integrate();}
    List<BattleView.Sprite> view(){ArrayList<BattleView.Sprite> out=new ArrayList<>();for(int n=0;n<scripts.size();n++){Script s=scripts.get(n);if(!s.ended&&s.image>=0)out.add(new BattleView.Sprite(1024+n,s,true));}out.sort((a,b)->{int d=Integer.compare(a.depth,b.depth);return d==0?Integer.compare(a.id,b.id):d;});return Collections.unmodifiableList(out);}
    public void sound(Pack p,int index){if(index<0||index>=p.sounds.length)throw new IllegalStateException("KGT sound "+index);events.add(new BattleSimulation.Event(frame,-1,events.size(),"sound",p.id,index,null));}
    public void gauge(Script s,int a,int b,int c,int d){throw s.fault("System animation must not mutate combat gauges");}
    public void visual(Script s,byte[] b){events.add(new BattleSimulation.Event(frame,-1,events.size(),"visual",pack.id,0,b));}
    public void spawn(Script owner,byte[] b){
        int skill=u16(b,2),flags=u(b,1),number=u(b,12);if(number!=0)throw owner.fault("Unaudited KGT object number "+number);if(skill==0)return;
        Script s=new Script(pack,this,globals,false);s.absolute=true;int x=s16(b,8)<<16,y=s16(b,10)<<16;
        s.x=((flags&64)!=0?0:owner.x)+(owner.facingLeft?-x:x);s.y=((flags&64)!=0?0:owner.y)+y;s.parent=owner;s.controller=owner.controller;s.charVars=owner.charVars;s.followParent=(flags&32)!=0;s.parentX=x;s.parentY=y;s.facingLeft=owner.facingLeft;
        s.depth=(flags&3)==2?u(b,13):(flags&3)==1?Math.min(127,owner.depth+1):Math.max(10,owner.depth-1);
        s.start(skill,(pack.types[skill]&32)==0);s.pc+=u(b,4);add(s);
    }
    void write(DataOutput o)throws IOException{o.writeInt(frame);StateIO.writeInts(o,globals);o.writeInt(scripts.size());for(Script s:scripts){o.writeInt(scripts.indexOf(s.parent));o.writeInt(scripts.indexOf(s.controller));StateIO.writeInts(o,s.charVars);s.writeState(o);}}
    void read(DataInput i)throws IOException{
        frame=i.readInt();StateIO.readInts(i,globals);int n=StateIO.count(i,1024);int[] parents=new int[n],owners=new int[n];
        for(int j=0;j<n;j++){parents[j]=i.readInt();owners[j]=i.readInt();Script s=new Script(pack,this,globals,false);StateIO.readInts(i,s.charVars);s.readState(i);scripts.add(s);}
        for(int j=0;j<n;j++){Script s=scripts.get(j);if(parents[j]< -1||parents[j]>=n||owners[j]<0||owners[j]>=n)throw new IOException("System task reference");s.parent=parents[j]<0?null:scripts.get(parents[j]);s.controller=scripts.get(owners[j]);if(s.controller!=s){if(!Arrays.equals(s.charVars,s.controller.charVars))throw new IOException("System variables mismatch");s.charVars=s.controller.charVars;}}
    }
}
