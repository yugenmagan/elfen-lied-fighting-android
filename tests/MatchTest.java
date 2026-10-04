package org.elfen.engine;
import java.nio.file.*;import java.util.*;import java.io.*;
public final class MatchTest {
 static Path root;static Pack a,b,s,k;static int checks;
 static void ok(boolean b,String message){checks++;if(!b)throw new AssertionError(message);}
 static Pack read(String id)throws Exception{return Pack.read(id,Files.newInputStream(root.resolve(id+"/data.efp")));}
 static MatchController make(int wins,int time,int cpu1,int cpu2,int seed){return new MatchController(a,b,s,k,seed,new MatchRules(k,wins,time,cpu1,cpu2));}
 static void fixtures(Path path)throws Exception{
  int count=0;for(String row:Files.readAllLines(path)){int[] n=Arrays.stream(row.split(" ")).mapToInt(Integer::parseInt).toArray();RoundController r=new RoundController(new MatchRules(k,3,60,0,0));r.state=n[0];r.wait=n[1];r.timer=n[2];r.wins1=n[5];r.wins2=n[6];r.step(n[3],400,n[4],400);int[] v={r.state,r.wait,r.timer,r.wins1,r.wins2};for(int j=0;j<5;j++)ok(v[j]==n[7+j],"x86 row="+count+" field="+j+" expected="+n[7+j]+" got="+v[j]);ok(r.started.size()==n[12],"system count");for(int j=0;j<r.started.size();j++)ok(r.started.get(j)==n[13+j],"system skill");count++;}System.out.println("PASS original dispatcher "+count+" cases");
 }
 public static void main(String[] args)throws Exception{
  root=Paths.get(args[0]);a=read("0170");b=read("0104");s=read("0080");k=read("0116");fixtures(Paths.get(args[1]));
  MatchController x=make(2,60,80,80,11);MatchReplay tape=new MatchReplay(x);List<String> hashes=new ArrayList<>();Map<Integer,byte[]> saved=new LinkedHashMap<>();int changes=0,previous=0;
  while(!x.state().finished&&x.frame()<45000){int f=x.frame();if(f==500||f==700||(x.state().phase==300&&changes<10))saved.put(f,x.snapshot());InputFrame input=new InputFrame(f,0,0);tape.append(input);x.step(input);hashes.add(x.stateHash());if(x.state().roundNumber!=previous){previous=x.state().roundNumber;changes++;System.out.println("Round "+previous+" at "+x.frame());}}
  ok(x.state().finished,"full match must finish");ok(x.state().roundNumber>=2,"multiple rounds");String finalHash=x.stateHash();byte[] data=tape.encode();MatchReplay decoded=MatchReplay.decode(data);Files.write(Paths.get(args[2]),data);
  for(int run=0;run<3;run++){MatchController y=make(2,60,80,80,0);decoded.restoreStart(y);for(int f=0;f<tape.length();f++){y.step(decoded.input(f));ok(y.stateHash().equals(hashes.get(f)),"replay run "+run+" frame "+f);}System.out.println("PASS full match replay "+run+" frames="+tape.length());}
  for(Map.Entry<Integer,byte[]> entry:saved.entrySet()){MatchController y=make(2,60,80,80,0);y.restore(entry.getValue());for(int f=entry.getKey();f<tape.length();f++){y.step(tape.input(f));if(f%60==0)ok(y.stateHash().equals(hashes.get(f)),"restore frame "+f);}ok(y.stateHash().equals(finalHash),"restore final");}
  MatchController y=make(2,60,80,80,11);MatchHistory history=new MatchHistory(y,120);for(int f=0;f<700;f++)history.advance(tape.input(f));String before=y.stateHash();InputFrame[] correction=new InputFrame[80];for(int n=0;n<80;n++)correction[n]=history.inputAt(620+n);history.resimulate(620,correction);ok(before.equals(y.stateHash()),"history resimulation");
  MatchController draw=make(2,1,0,0,2);while(!draw.state().finished&&draw.frame()<7000)draw.step(new InputFrame(draw.frame(),0,0));ok(draw.state().finished&&draw.state().score1==2&&draw.state().score2==2&&draw.state().winner==-1,"timer draw awards both");
  MatchController carry=make(2,1,0,0,2);carry.battle.entities[0].special=37;carry.battle.entities[0].stocks=2;while(carry.state().roundNumber==1)carry.step(new InputFrame(carry.frame(),0,0));ok(carry.battle.entities[0].special==37&&carry.battle.entities[0].stocks==2,"meter carry");ok(carry.battle.life(0)==a.life(),"life reset");
  byte[] broken=y.snapshot();broken[0]^=1;try{y.restore(broken);throw new AssertionError("accepted corruption");}catch(IllegalArgumentException expected){}ok(before.equals(y.stateHash()),"restore atomic");data[data.length-1]^=1;try{MatchReplay.decode(data);throw new AssertionError("accepted replay corruption");}catch(IOException expected){}
  System.out.println("PASS match checks="+checks+" final="+finalHash+" frames="+tape.length()+" score="+x.state().score1+":"+x.state().score2+" snapshots="+saved.size());
 }
}
