package org.elfen.engine;
/** Starts at the audited boss event, then uses real combat, HP, AI and round logic. */
public final class FinalBossNaturalLoss008Test {
 public static void main(String[] args)throws Exception{
  StoryCatalog c=StoryDataTest.load(args[0]);Pack p=StoryDataTest.packs.get("0170");StoryController s=FinalBoss008Test.finalFight(c,p,true);
  int f=0;for(;f<120000&&s.mode()==StoryController.FIGHT;f++)s.step(new InputFrame(s.frame(),0,0));
  if(s.mode()!=StoryController.CONTINUE||s.slot()!=18||s.score(0)!=0||s.score(1)!=2)throw new AssertionError("Expected actual two-round defeat -> Continue: "+s.mode()+" hp="+s.battle.life(0)+":"+s.battle.life(1));
  System.out.println("PASS natural idle-Lucy loss to final boss after "+f+" ticks, score 0:2 -> Continue at slot18; no HP, AI, damage or round overrides. Boss-event entry is a fixture; not a full route playthrough.");
 }
}
