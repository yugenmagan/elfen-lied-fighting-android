package org.elfen.engine;
import java.nio.file.*;import java.util.*;
public final class CommandRecognizerTest {
 public static void main(String[] args)throws Exception{
  Map<String,Pack> packs=new HashMap<>();int scenario=0,checks=0;
  for(String line:Files.readAllLines(Paths.get(args[1]))){Scanner in=new Scanner(line);String id=in.next();Pack p=packs.get(id);if(p==null){p=Pack.read(id,Files.newInputStream(Paths.get(args[0],id,"data.efp")));packs.put(id,p);}
   int reverse=in.nextInt(),stance=in.nextInt(),start=in.nextInt(),skill=in.nextInt(),chosen=in.nextInt(),custom=in.nextInt();Pack.Command[] original=p.commands;
   if(custom!=0){Pack.Command c=new Pack.Command();c.time=120;c.skills=new int[]{11,12,13,14};c.steps=new byte[20];c.amounts=new int[10];for(int n=0;n<10;n++){int f=in.nextInt();c.steps[2*n]=(byte)f;c.steps[2*n+1]=(byte)(f>>>8);}for(int n=0;n<10;n++)c.amounts[n]=in.nextInt();p.commands=new Pack.Command[]{c};}
   int[] h=new int[1024];for(int n=0;n<1024;n++)h[n]=in.nextInt();CommandRecognizer m=new CommandRecognizer();m.applyFrame(new InputFrame(1023,0,0).withCpuHistory(0,h),0,false);
   int ci=m.match(p,reverse!=0,stance,start),actual=ci<0?0:p.commands[ci].skills[stance];checks++;if(actual!=skill)throw new AssertionError("scenario "+scenario+" skill "+actual+" != x86 "+skill+" chosen="+chosen);
   for(int age=0;age<1024;age++){int expected=in.nextInt();checks++;if(m.at(age)!=expected)throw new AssertionError("history scenario "+scenario+" age "+age);}
   p.commands=original;scenario++;in.close();
  }System.out.println("PASS original matcher "+scenario+" cases / "+checks+" assertions");
 }
}
