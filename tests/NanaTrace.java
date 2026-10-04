package org.elfen.engine;
import java.nio.file.*;
/** Reproduce the crossed-side CPU regression without Android or wall-clock time. */
public final class NanaTrace {
 static Path root;static Pack p(String id)throws Exception{return Pack.read(id,Files.newInputStream(root.resolve(id+"/data.efp")));}
 public static void main(String[] args)throws Exception{
  root=Paths.get(args[0]);BattleSimulation b=new BattleSimulation(p("0104"),p("0104"),p("0080"),p("0116"),19);b.setCpu(0,80);b.setCpu(1,80);
  for(int f=0;f<54000&&b.phase()==0;f++){
   b.step(new InputFrame(f,0,0));if(f%3000==0){System.out.print(f+" hp "+b.life(0)+":"+b.life(1));
    for(int n=0;n<2;n++){Script s=b.entities[n];System.out.print(" p"+n+"="+s.skill+":"+s.pc+"@"+s.x/65536+","+s.y/65536+" left="+s.facingLeft+" flags="+s.battleFlags+" wait="+s.wait+" vx="+s.vx+" vy="+s.vy+" ay="+s.ay+" ended="+s.ended);}
    System.out.println();
   }
  }System.out.println("FINAL "+b.frame()+" hp "+b.life(0)+":"+b.life(1));
 }
}
