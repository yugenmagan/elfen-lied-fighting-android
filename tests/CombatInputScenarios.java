import org.elfen.engine.*;import java.nio.file.*;
/** Numbered physical-direction inputs through the Android combat core. */
public final class CombatInputScenarios {
 static Path assets;static Pack p(String id)throws Exception{return Pack.read(id,Files.newInputStream(assets.resolve(id+"/data.efp")));}
 static void run(String id,int[] masks,String expected)throws Exception{
  BattleSimulation sim=new BattleSimulation(p(id),p("0104"),p("0080"),p("0116"),1);boolean recognized=false;
  for(int mask:masks){sim.step(new InputFrame(sim.frame(),mask,0));for(BattleSimulation.Event e:sim.events())if(e.kind.equals("command")&&e.packId.equals(id)&&p(id).commands[e.value].name.equals(expected))recognized=true;}
  if(!recognized)throw new AssertionError(id+" expected command "+expected);
  for(int i=0;i<600;i++)sim.step(new InputFrame(sim.frame(),0,0));System.out.println("PASS combat physical inputs "+id+" "+expected+" +600ticks");
 }
 public static void main(String[] args)throws Exception{assets=Paths.get(args[0]);run("0170",new int[]{0,8,10,2,18},"とび弱");run("0104",new int[]{0,8,10,2,18},"ロケット弱");run("0128",new int[]{0,8,10,2,18},"磯部くん");int[] charge=new int[95];java.util.Arrays.fill(charge,1);charge[94]=18;run("0128",charge,"ねこ座薬");run("0128",new int[]{0,2,10,8,9,1,17},"コマンド 1");}
}
