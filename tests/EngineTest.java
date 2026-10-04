import org.elfen.engine.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public final class EngineTest {
    static int checks=0;static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static Pack read(Path root,String id)throws IOException{return Pack.read(id,Files.newInputStream(root.resolve(id+"/data.efp")));}
    static final Script.Host HOST=new Script.Host(){public void sound(Pack p,int i){check(i>=0&&i<p.sounds.length,"sound bounds");}public void spawn(Script s,byte[] b){throw new AssertionError("unexpected object in isolated test");}public void gauge(Script s,int a,int b,int c,int d){throw new AssertionError("unexpected gauge in pure test");}};
    public static void main(String[] args)throws Exception{
        Path root=Paths.get(args[0]);int n=0;
        try(java.util.stream.Stream<Path> dirs=Files.list(root)){
            for(Path dir:(Iterable<Path>)dirs::iterator)if(Files.isDirectory(dir)){
                Pack p=read(root,dir.getFileName().toString());check(p.hash.length()==64,"source hash");
                for(int i=0;i<p.widths.length;i++)if(p.widths[i]>0)check(Files.size(dir.resolve(String.format("%04d.png",i)))>0,"image missing "+dir+":"+i);
                for(String s:p.sounds)if(!s.isEmpty())check(Files.size(dir.resolve(s))>0,"sound missing");n++;
            }
        }
        check(n==46,"46 containers");
        Input input=new Input();input.set(7,Input.DOWN|Input.RIGHT);input.set(12,Input.A);input.set(31,Input.B);check(input.mask()==58,"three pointers");input.set(12,0);check(input.mask()==42,"release preserves direction and B");input.clear();check(input.mask()==0,"cancel releases all");
        check(Input.dpad(50,50,100,.18f)==10,"diagonal");check(Input.dpad(5,5,100,.18f)==0,"dead zone");check(Input.direction(10,false)==3,"forward diagonal");check(Input.direction(10,true)==5,"facing diagonal");
        input.set(1,Input.A);input.set(1,0);check(input.sample()==Input.A,"sub-frame tap retained");check(input.sample()==0,"tap releases next tick");input.clear();
        Pack lucy=read(root,"0170"),nana=read(root,"0104"),stage=read(root,"0080");check(lucy.life()==400,"original Lucy HP");
        Script s=new Script(lucy,HOST,new int[16],true);s.y=s.ground;s.start(lucy.builtin(1),true);s.tick();check(s.vx==150*655,"x86-derived Lucy walk coefficient");
        int x=s.x;s.tick();check(s.x-x==150*655,"fixed point motion");
        // Differential fixtures from original x86 integration block, 0x40f96a.
        Script motion=new Script(lucy,HOST,new int[16],false);motion.wait=-1;
        motion.x=100;motion.y=200;motion.vx=10;motion.vy=-20;motion.ax=3;motion.ay=7;motion.tick();
        check(motion.x==113&&motion.y==187&&motion.vx==13&&motion.vy==-13,"x86 acceleration-before-position");
        motion.x=Integer.MAX_VALUE-2;motion.vx=5;motion.ax=0;motion.tick();check(motion.x==Integer.MIN_VALUE+2,"x86 signed wraparound");
        Script parent=new Script(lucy,HOST,new int[16],false);parent.x=100<<16;parent.y=200<<16;parent.ended=true;
        motion.parent=parent;motion.followParent=true;motion.parentX=7<<16;motion.parentY=-9<<16;motion.tick();
        check(motion.x==(107<<16)&&motion.y==(191<<16)&&!motion.ended,"attachment overrides integrated position");
        parent.facingLeft=true;motion.tick();check(motion.x==(93<<16),"attachment mirrors parent's offset");
        Commands commands=new Commands();commands.push(Input.DOWN);commands.push(Input.RIGHT|Input.DOWN);commands.push(Input.RIGHT);commands.push(Input.RIGHT|Input.A);int cmd=commands.match(lucy,false,1);check(cmd>=0&&lucy.commands[cmd].skills[1]==79,"original Lucy quarter-circle command");
        commands.clear();commands.push(0);commands.push(Input.A|Input.B|Input.C);cmd=commands.match(lucy,false,1);check(cmd>=0&&lucy.commands[cmd].skills[1]==127,"simultaneous ABC priority");
        ArrayList<String> issues=new ArrayList<>();int paths=0;
        for(String id:new String[]{"0170","0104","0128","0122","0114","0110","0156","0108","0168","0136","0142"}){
            Pack p=read(root,id);
            for(int key:new int[]{0,Input.RIGHT,Input.LEFT,Input.DOWN,Input.UP,Input.UP|Input.RIGHT}){
                PreviewSession a=new PreviewSession(p,nana,stage);
                try{for(int frame=0;frame<240;frame++)a.tick(frame<100?key:0);paths++;}
                catch(IllegalStateException e){issues.add(id+" input="+key+" "+e.getMessage());}
            }
            for(int key:new int[]{Input.A,Input.B,Input.C}){
                PreviewSession a=new PreviewSession(p,nana,stage);
                try{a.tick(0);a.tick(key);for(int frame=0;frame<300;frame++)a.tick(0);paths++;}
                catch(IllegalStateException e){issues.add(id+" input="+key+" "+e.getMessage());}
            }
        }
        for(String issue:issues)System.out.println("UNSUPPORTED "+issue);
        check(issues.isEmpty()&&paths==99,"all 99 animation paths must complete");
        System.out.println("PASS "+checks+" assertions; loaded "+n+" packs; animation paths completed "+paths+" / 99");
        System.out.println("This is not combat, Android installation, or full story validation.");
    }
}
