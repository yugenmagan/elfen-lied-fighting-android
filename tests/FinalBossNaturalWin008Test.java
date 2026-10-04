package org.elfen.engine;
/** Starts at the boss event; ordinary directional/button input, no battle overrides. */
public final class FinalBossNaturalWin008Test {
 public static void main(String[] args)throws Exception{
  StoryCatalog c=StoryDataTest.load(args[0]);Pack p=StoryDataTest.packs.get("0156");StoryController s=FinalBoss008Test.finalFight(c,p,true);StoryReplay tape=new StoryReplay(s);java.util.Map<Integer,String> hashes=new java.util.TreeMap<>();int n=0,minimum=p.life();String lastScore="";
  for(;n<45000&&s.mode()==StoryController.FIGHT;n++){
   InputFrame f=new InputFrame(s.frame(),StoryInputPilotTest.pilot(s,2),0);tape.append(f);lastScore=s.score(0)+":"+s.score(1);s.step(f);
   if(s.battle!=null)minimum=Math.min(minimum,s.battle.life(1));if(s.frame()%1000==0)hashes.put(s.frame(),s.stateHash());
  }
  if(s.wins()!=1||s.mode()!=StoryController.SCENE||s.slot()!=19||minimum!=0)throw new AssertionError("Natural boss victory did not open ending");
  int fightTicks=n;for(int tick=0;tick<6000&&!s.finished();tick++){InputFrame f=new InputFrame(s.frame(),tick%1700==1650?Input.A:0,0);tape.append(f);s.step(f);}
  if(s.mode()!=StoryController.COMPLETE)throw new AssertionError("Won ending/credits did not complete");hashes.put(s.frame(),s.stateHash());
  StoryReplay replay=StoryReplay.decode(tape.encode());for(int run=0;run<3;run++){StoryController twin=new StoryController(c,p,0,0,true,true);replay.restoreStart(twin);
   for(int f=0;f<replay.length();f++){twin.step(replay.input(f));String hash=hashes.get(twin.frame());if(hash!=null&&!twin.stateHash().equals(hash))throw new AssertionError("Natural win replay mismatch "+f);if(f%5003==0)twin.restore(twin.snapshot());}
   if(!s.stateHash().equals(twin.stateHash()))throw new AssertionError("Final replay hash");}
  java.nio.file.Files.write(java.nio.file.Paths.get("research/final-boss-natural-win-008.efs"),tape.encode());
  System.out.println("PASS natural Young Lucy (0156) input-only boss victory, "+fightTicks+" fight ticks, score "+lastScore+" -> ending -> credits -> complete; "+tape.length()+" recorded frames; three replay/restore runs; final hash "+s.stateHash()+". Boss-event fixture, not a full route. No HP, AI, damage or round overrides.");
 }
}
