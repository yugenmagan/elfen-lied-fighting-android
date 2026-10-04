package org.elfen.engine;
import java.nio.file.*;import java.util.*;
public final class MatchBranchesTest {
 static Path root;static Pack read(String id)throws Exception{return Pack.read(id,Files.newInputStream(root.resolve(id+"/data.efp")));}
 static void advance(MatchController m){m.step(new InputFrame(m.frame(),0,0));}
 public static void main(String[] args)throws Exception{
  root=Paths.get(args[0]);Pack k=read("0116"),stage=read("0080"),a=read("0170"),b=read("0104");int kos=0;
  for(String id:new String[]{"0170","0104","0128","0122","0114","0110","0156","0108","0168","0136","0142","0144"}){
   MatchController m=new MatchController(read(id),id.equals("0104")?a:b,stage,k,19,new MatchRules(k,1,60,80,80));
   while(!m.state().finished&&m.frame()<9000)advance(m);if(!m.state().finished)throw new AssertionError("Did not finish "+id);if(m.battle.life(0)==0||m.battle.life(1)==0)kos++;
   System.out.println("PASS match "+id+" frames="+m.frame()+" HP="+m.battle.life(0)+":"+m.battle.life(1)+" score="+m.state().score1+":"+m.state().score2);
  }
  if(kos==0)throw new AssertionError("No natural KO route tested");
  for(int scenario=0;scenario<4;scenario++){
   MatchController m=new MatchController(a,b,stage,k,19,new MatchRules(k,2,0,0,0));while(!m.state().fighting)advance(m);
   m.battle.entities[0].life=scenario==0||scenario==2?0:a.life();m.battle.entities[1].life=scenario==1||scenario==2?0:b.life();
   if(scenario==3){for(int i=0;i<700;i++)advance(m);if(!m.state().fighting||m.state().timer!=-1)throw new AssertionError("Infinite timer");continue;}
   while(m.state().phase!=300)advance(m);byte[] before=m.snapshot();MatchController twin=new MatchController(a,b,stage,k,0,new MatchRules(k,2,0,0,0));twin.restore(before);
   Set<Integer> phases=new HashSet<>();while(m.state().roundNumber==1){phases.add(m.state().phase);advance(m);advance(twin);if(!m.stateHash().equals(twin.stateHash()))throw new AssertionError("Result/round transition restore");}
   if(scenario==2&&(!phases.contains(540)||m.state().score1!=1||m.state().score2!=1))throw new AssertionError("Double KO");
   if(scenario<2&&!phases.contains(scenario==0?533:523))throw new AssertionError("Perfect presentation");
  }
  System.out.println("PASS natural KO matches="+kos+"; forced result branches: P1/P2 perfect, doubleKO, infinite clock, cross-round restore");
 }
}
