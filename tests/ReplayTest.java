package org.elfen.engine;
import java.nio.file.*;import java.util.*;
public final class ReplayTest {
 static Path root;static Pack read(String id)throws Exception{return Pack.read(id,Files.newInputStream(root.resolve(id+"/data.efp")));}
 public static void main(String[] args)throws Exception{
  root=Paths.get(args[0]);Pack a=read("0170"),b=read("0104"),s=read("0080"),k=read("0116");BattleSimulation x=new BattleSimulation(a,b,s,k,77);x.setCpu(1,80);
  ReplayTape tape=new ReplayTape(x);BattleHistory history=new BattleHistory(x,120);List<String> hashes=new ArrayList<>();
  for(int f=0;f<2400;f++){int j=f%100,mask=j<30?2:j==30?8:j==31?10:j==32?18:0;InputFrame in=new InputFrame(f,mask,0);tape.append(in);history.advance(in);hashes.add(x.stateHash());}
  byte[] encoded=tape.encode();ReplayTape decoded=ReplayTape.decode(encoded);int checks=0;
  for(int run=0;run<3;run++){
   BattleSimulation y=new BattleSimulation(a,b,s,k,0);decoded.restoreStart(y);
   for(int f=0;f<decoded.length();f++){y.step(decoded.input(f));checks++;if(!hashes.get(f).equals(y.stateHash()))throw new AssertionError("Replay desync run="+run+" frame="+f);}
  }
  String expected=x.stateHash();InputFrame[] last=new InputFrame[60];for(int i=0;i<60;i++)last[i]=history.inputAt(x.frame()-60+i);history.resimulate(x.frame()-60,last);
  if(!x.stateHash().equals(expected))throw new AssertionError("History resimulation");
  boolean invalid=false;byte[] bad=encoded.clone();bad[bad.length-1]^=1;try{ReplayTape.decode(bad);}catch(java.io.IOException e){invalid=true;}if(!invalid)throw new AssertionError("Replay corruption not rejected");
  Path out=Paths.get(args[1]);Files.write(out,encoded);
  System.out.println("PASS replay 3 x 2400 every-frame hashes="+checks+"; history restore/resimulation 60 ticks; corruption rejected. bytes="+encoded.length+" hash="+expected);
 }
}
