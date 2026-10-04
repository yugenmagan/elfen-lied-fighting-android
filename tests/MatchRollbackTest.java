package org.elfen.engine;
import java.nio.file.*;import java.util.*;
/** Two independent cores plus delayed remote input, with corrected-prefix checks. */
public final class MatchRollbackTest {
 static boolean classic;static Pack a,b,s,k;static MatchController make(){return new MatchController(a,b,s,k,77,new MatchRules(k,9,classic?1:999,0,0,classic));}
 static int mask(int f,int p){if(f<505)return 0;int t=(f+p*37)%180;return t<80?(p==0?Input.RIGHT:Input.LEFT):t==80?Input.DOWN:t==81?(Input.DOWN|(p==0?Input.RIGHT:Input.LEFT)):t==82?((p==0?Input.RIGHT:Input.LEFT)|Input.A):t<110?Input.A|Input.B:t<130?Input.UP:0;}
 public static void main(String[] args)throws Exception{
  classic=args.length>1&&args[1].equals("--classic");Path root=Paths.get(args[0]);String[] ids={"0170","0104","0080","0116"};Pack[] packs=new Pack[4];for(int n=0;n<4;n++)packs[n]=Pack.read(ids[n],Files.newInputStream(root.resolve(ids[n]+"/data.efp")));a=packs[0];b=packs[1];s=packs[2];k=packs[3];
  int ticks=3000;int transitions=0;String[] hashes=new String[ticks+1];MatchController truth=make(),twin=make();hashes[0]=truth.stateHash();
  for(int f=0;f<ticks;f++){InputFrame input=new InputFrame(f,mask(f,0),mask(f,1));int before=truth.state().roundNumber;truth.step(input);twin.step(input);if(truth.state().roundNumber!=before)transitions++;hashes[f+1]=truth.stateHash();if(!hashes[f+1].equals(twin.stateHash()))throw new AssertionError("Twin desync "+f);}
  for(int delay:new int[]{3,6,9,12}){
   MatchController predicted=make();MatchHistory history=new MatchHistory(predicted,120);int known=0,checks=0,resimulations=0;
   for(int f=0;f<ticks;f++){
    history.advance(new InputFrame(f,mask(f,0),known));int arrival=f-delay;
    if(arrival>=0){known=mask(arrival,1);InputFrame[] correction=new InputFrame[delay+1];for(int n=0;n<correction.length;n++){int g=arrival+n;correction[n]=new InputFrame(g,mask(g,0),known);}history.resimulate(arrival,correction);resimulations+=correction.length;
     String confirmed=StateIO.hex(StateIO.hash(history.stateAt(arrival+1)));if(!confirmed.equals(hashes[arrival+1]))throw new AssertionError("Confirmed desync delay="+delay+" frame="+arrival);checks++;
    }
   }
   InputFrame[] flush=new InputFrame[delay];for(int n=0;n<delay;n++){int f=ticks-delay+n;flush[n]=new InputFrame(f,mask(f,0),mask(f,1));}history.resimulate(ticks-delay,flush);
   if(!predicted.stateHash().equals(truth.stateHash()))throw new AssertionError("Final rollback desync "+delay);
   System.out.println("PASS delay="+(delay*1000/60)+"ms / "+delay+"ticks confirmed="+checks+" resimulated="+resimulations+" final="+predicted.stateHash());
  }
  if(classic&&transitions<1)throw new AssertionError("No classic round transition");System.out.println("Round transitions="+transitions+" classic="+classic);
  System.out.println("PASS independent twins="+ticks+" every-frame hashes; no network transport used");
 }
}
