package org.elfen.engine;
import java.nio.file.*;import java.util.*;
public final class BattleCpuTest{
 static Path root;static int checks;static Pack read(String id)throws Exception{return Pack.read(id,Files.newInputStream(root.resolve(id+"/data.efp")));}
 static void check(boolean v,String why){checks++;if(!v)throw new AssertionError(why);}
 static void fight(String p1,String p2,int seed)throws Exception{
  Pack a=read(p1),b=read(p2),stage=read("0080"),kgt=read("0116");BattleSimulation x=new BattleSimulation(a,b,stage,kgt,seed),y=new BattleSimulation(a,b,stage,kgt,seed);
  for(BattleSimulation s:new BattleSimulation[]{x,y}){s.setCpu(0,80);s.setCpu(1,80);}Set<Integer> commands=new TreeSet<>();int hits=0,guards=0,objects=0;
  for(int frame=0;frame<18000;frame++){
   InputFrame input=new InputFrame(frame,0,0);try{x.step(input);y.step(input);}catch(RuntimeException e){throw new IllegalStateException(p1+" vs "+p2+" seed "+seed+" frame "+frame,e);}
   check(x.stateHash().equals(y.stateHash()),"CPU twin desync "+frame);if(frame%137==0){byte[] snapshot=y.snapshot();y.restore(snapshot);check(Arrays.equals(snapshot,y.snapshot()),"snapshot "+frame);}
   for(BattleSimulation.Event e:x.events()){if(e.kind.equals("command"))commands.add((e.packId.equals(p1)?0:1000)+e.value);if(e.kind.equals("hit"))hits++;if(e.kind.equals("guard"))guards++;}if(x.activeObjects()>0)objects++;
   if(x.phase()==BattleSimulation.KO&&frame>x.koFrame+200)break;
  }
  System.out.println("CPU FIGHT "+p1+":"+p2+" seed="+seed+" frames="+x.frame()+" hp="+x.life(0)+":"+x.life(1)+" commands="+commands+" hit="+hits+" guard="+guards+" objectFrames="+objects+" hash="+x.stateHash());
  for(int p=0;p<2;p++){Script s=x.entities[p];System.out.println("STATE "+p+" skill="+s.skill+" pc="+s.pc+" ended="+s.ended+" flags="+s.battleFlags+" wait="+s.wait+" x="+s.x/65536+" y="+s.y/65536);}
  check(hits>0,"CPU must hit");check(x.phase()==BattleSimulation.KO,"CPU reaches KO");
 }
 public static void main(String[] args)throws Exception{root=Paths.get(args[0]);fight("0170","0104",1);fight("0128","0170",2);fight("0104","0128",3);System.out.println("PASS active CPU twin/restore "+checks);}
}
