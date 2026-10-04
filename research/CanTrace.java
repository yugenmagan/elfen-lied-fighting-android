package org.elfen.engine;
import java.nio.file.*;
public final class CanTrace {
 static Path root;
 static Pack p(String id)throws Exception{return Pack.read(id,Files.newInputStream(root.resolve(id+"/data.efp")));}
 public static void main(String[] args)throws Exception{
  root=Paths.get(args[0]);
  for(int attack:new int[]{Input.A,Input.B,Input.C})for(int gap:new int[]{70,160,260,400}){
   BattleSimulation b=new BattleSimulation(p("0104"),p("0170"),p("0080"),p("0116"),19);
   b.entities[0].x=390<<16;b.entities[1].x=(390+gap)<<16;
   int[] stream={0,Input.DOWN,Input.DOWN|Input.LEFT,Input.LEFT,Input.LEFT|attack,0};
   System.out.println("CASE button="+attack+" gap="+gap);
   for(int f=0;f<800;f++){
    b.step(new InputFrame(f,f<stream.length?stream[f]:0,0));
    for(BattleSimulation.Event e:b.events())if(!e.kind.equals("sound"))System.out.println("  f="+f+" "+e.kind+" id="+e.emitter+" value="+e.value+" hp="+b.life(1));
    if(f%60==0){for(int n=2;n<b.entities.length;n++){
     Script s=b.entities[n];if(s!=null&&!s.ended&&s.pack.id.equals("0104")&&(s.skill>=90&&s.skill<=95))System.out.println("  object "+n+" f="+f+" skill="+s.skill+" x="+s.x/65536+" y="+s.y/65536+" vx="+s.vx+" FA="+(s.hit[0]!=null)+" flags="+s.battleFlags);
    }}
   }
  }
 }
}
