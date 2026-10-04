package org.elfen.engine;
import java.nio.file.*;
public final class AllCpuCharacters {
 static Path root;static Pack read(String id)throws Exception{return Pack.read(id,Files.newInputStream(root.resolve(id+"/data.efp")));}
 public static void main(String[] args)throws Exception{
  root=Paths.get(args[0]);Pack nana=read("0104"),stage=read("0080"),kgt=read("0116");int failures=0;
  for(String id:new String[]{"0170","0104","0128","0122","0114","0110","0156","0108","0168","0136","0142","0144"}){
   // Nana mirror has an original AI range gap at ~259px. It belongs to the
   // timed-match regression; use Lucy here to test Nana's playable KO route.
   BattleSimulation b=new BattleSimulation(read(id),id.equals("0104")?read("0170"):nana,stage,kgt,19);b.setCpu(0,80);b.setCpu(1,80);
   try{for(int f=0;f<18000&&b.phase()==BattleSimulation.ACTIVE;f++)b.step(new InputFrame(f,0,0));
    System.out.println(id+" frame="+b.frame()+" hp="+b.life(0)+":"+b.life(1)+" phase="+b.phase());if(b.phase()!=BattleSimulation.KO){failures++;System.out.println("FAIL no KO");}}
   catch(RuntimeException e){failures++;System.out.println("FAIL "+id+" frame="+b.frame()+" "+e);}
  }
  if(failures>0)throw new AssertionError("Character combat failures "+failures);
 }
}
