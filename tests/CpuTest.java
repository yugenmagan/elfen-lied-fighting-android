package org.elfen.engine;
import java.nio.file.*;
import java.util.*;
public final class CpuTest {
 public static void main(String[] args)throws Exception{
  Scanner in=new Scanner(Paths.get(args[1]));int checks=0,cases=0;
  while(in.hasNext()){
   if(!in.next().equals("case"))throw new AssertionError("fixture");String id=in.next();int level=in.nextInt(),distance=in.nextInt(),selfY=in.nextInt(),otherY=in.nextInt();
   Pack p=Pack.read(id,Files.newInputStream(Paths.get(args[0],id,"data.efp")));CpuProgram program=new CpuProgram(p);CpuProgram.State state=new CpuProgram.State();state.level=level;CombatMath.Rng rng=new CombatMath.Rng(1);
   for(int frame=0;frame<120;frame++){
    int[] history=program.produce(state,rng,390<<16,selfY<<16,920<<16,(390+distance)<<16,otherY<<16,920<<16,false);
    int[] actual={state.pattern,state.step,state.remaining,state.cooldown,rng.state()};
    for(int n=0;n<5;n++){int expected=(int)in.nextLong();checks++;if(actual[n]!=expected)throw new AssertionError("CPU "+id+" level "+level+" frame "+frame+" field "+n+" "+actual[n]+" != "+expected);}
    for(int n=0;n<1024;n++){int expected=in.nextInt();checks++;if(history[n]!=expected)throw new AssertionError("CPU input "+id+" level "+level+" frame "+frame+" age "+n+" "+history[n]+" != "+expected);}
   }cases++;
  }
  System.out.println("PASS original CPU "+cases+" x120 ticks / "+checks+" exact assertions");
 }
}
