package org.elfen.engine;
import java.nio.file.*;import java.util.*;
public final class ContactTest {
 static Path root;static Pack p(String id)throws Exception{return Pack.read(id,Files.newInputStream(root.resolve(id+"/data.efp")));}
 static byte[] box(int type,int x,int y,int w,int h,int flags,int rate,int power){byte[] b=new byte[16];b[0]=(byte)type;int[] vals={x,y,w,h};for(int i=0;i<4;i++){b[1+2*i]=(byte)vals[i];b[2+2*i]=(byte)(vals[i]>>8);}b[10]=(byte)flags;b[11]=(byte)rate;b[12]=(byte)power;return b;}
 public static void main(String[] args)throws Exception{
  root=Paths.get(args[0]);Pack a=p("0170"),b=p("0104"),s=p("0080"),k=p("0116");int checks=0;Scanner in=new Scanner(Paths.get(args[1]));
  while(in.hasNext()){
   String kind=in.next();BattleSimulation sim=new BattleSimulation(a,b,s,k,1);Script x=sim.entities[0],y=sim.entities[1];
   if(kind.equals("clash")){
    int pa=in.nextInt(),pb=in.nextInt(),ta=in.nextInt(),tb=in.nextInt(),apart=in.nextInt(),flag=in.nextInt();
    x.hit[0]=box(24,0,-60,30,40,0,0,pa);y.hit[0]=box(24,0,-60,30,40,0,0,pb);y.x=(apart!=0?590:410)<<16;x.triggers[4]=ta;y.triggers[4]=tb;y.battleFlags=flag;sim.clashes();
    int[] actual={x.hit[0]!=null?1:0,x.pendingSkill,x.triggers[4],y.hit[0]!=null?1:0,y.pendingSkill,y.triggers[4]};for(int v:actual){int expected=in.nextInt();checks++;if(v!=expected)throw new AssertionError("Clash field "+checks+" "+v+" != x86 "+expected);}
   }else if(kind.equals("reaction")){
    int absent=in.nextInt();y.x=450<<16;y.input=Input.RIGHT;x.hit[0]=box(24,54,-110,30,23,0,0,5);y.hurt[0]=box(25,1,-79,54,84,3,100,0);
    x.response=absent!=0?null:new byte[]{23,1,0,2,0,3,0,0,0,0,0,0,0,0,0,0};sim.collide();
    for(int v:new int[]{y.life,y.pendingSkill,x.battleFlags,y.battleFlags}){int expected=in.nextInt();checks++;if(v!=expected)throw new AssertionError("Original reaction-error semantics");}
    if(sim.events().stream().noneMatch(e->e.kind.equals("reaction-error-2")))throw new AssertionError("Original data error must be reported");checks++;
   }else throw new AssertionError(kind);
  }System.out.println("PASS original clash/reaction-error semantics "+checks+" assertions");
 }
}
