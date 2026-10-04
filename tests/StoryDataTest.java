package org.elfen.engine;
import java.nio.file.*;import java.util.*;import java.io.*;
public final class StoryDataTest {
    static Path root;static final Map<String,Pack> packs=new HashMap<>();static int checks;
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static StoryCatalog load(String path)throws Exception{root=Paths.get(path);try(java.util.stream.Stream<Path> s=Files.list(root)){for(Path f:(Iterable<Path>)s.filter(Files::isDirectory)::iterator){Pack p=Pack.read(f.getFileName().toString(),Files.newInputStream(f.resolve("data.efp")));packs.put(p.id,p);}}return new StoryCatalog(packs.get("0116"),packs,Files.newInputStream(root.resolve("story-index.tsv")));}
    public static void main(String[] args)throws Exception{
        StoryCatalog c=load(args[0]);int routes=0,battles=0,scenes=0;
        for(Pack p:c.protagonists()){StoryProgram program=new StoryProgram(p);int n=-1,steps=0;while(true){n=program.next(n,0,p.life(),0);StoryProgram.Event e=program.event(n);steps++;if(e.type==4)break;check(e.type==1||e.type==2,"Unexpected story event");if(e.type==1){new StoryRound(c.kgt,e);battles++;}else{c.demo(e.value(1));scenes++;}}check(steps==22,"22 records including ending expected "+p.id);routes++;}
        check(routes==10,"10 story protagonists");System.out.println("PASS "+routes+" routes, "+battles+" fights, "+scenes+" scenes, exact filename/hash references");
        for(String line:Files.readAllLines(Paths.get(args[2]))){int[] x=Arrays.stream(line.split(" ")).mapToInt(Integer::parseInt).toArray();DemoSession d=new DemoSession(packs.get("0118"));d.phase=x[0];d.guard=x[1];d.released=x[2]!=0;d.remaining=x[3];d.step(new InputFrame(0,x[4]&1023,0));int[] out={d.finished()?2:d.phase,d.guard,d.released?1:0,d.remaining,d.finished()?1:0};for(int n=0;n<5;n++)check(out[n]==x[5+n],"Native DEMO fixture "+line);}
        StoryProgram.Event lucy=new StoryProgram(packs.get("0170")).event(1);
        for(String line:Files.readAllLines(Paths.get(args[1]))){int[] x=Arrays.stream(line.split(" ")).mapToInt(Integer::parseInt).toArray();StoryRound r=new StoryRound(c.kgt,lucy);r.state=x[0];r.wait=x[1];r.timer=x[2];r.points1=x[5];r.points2=x[6];r.wins1=x[7];r.wins2=x[8];r.step(x[3],400,x[4],400);
            int[] out={r.state,r.wait,r.timer,r.wins1,r.wins2,r.points1,r.points2,r.losses};for(int i=0;i<8;i++)check(out[i]==x[9+i],"Native story round mismatch field "+i+" input="+line+" java="+Arrays.toString(out));check(r.started.size()==x[17],"System animation count");for(int i=0;i<r.started.size();i++)check(r.started.get(i)==x[18+i],"System animation");
        }
        int demos=0,totalFrames=0;
        for(Pack p:packs.values())if(p.kind.equals(".demo")){
            DemoSession d=new DemoSession(p),t=new DemoSession(p);for(int f=0;f<2100;f++){InputFrame input=new InputFrame(f,0,0);d.step(input);t.step(input);check(d.stateHash().equals(t.stateHash()),"DEMO twins "+p.id+":"+f);if(f==811){t=new DemoSession(p);t.restore(d.snapshot());}}
            check(!d.finished(),"Original scene waits for button, not animation end "+p.id);check(d.canAdvance(),"Release latch");d.step(new InputFrame(d.frame(),16,0));check(!d.finished(),"Original one-frame exit phase");d.step(new InputFrame(d.frame(),0,0));check(d.finished(),"Button scene transition");demos++;totalFrames+=d.frame();
        }
        DemoSession held=new DemoSession(packs.get("0118"));for(int f=0;f<100;f++)held.step(new InputFrame(f,16,0));check(!held.canAdvance()&&!held.finished(),"Held button must not skip");held.step(new InputFrame(100,0,0));held.step(new InputFrame(101,16,0));held.step(new InputFrame(102,0,0));check(held.finished(),"Release and press exits");
        System.out.println("PASS 1344 original story-round fixtures; "+demos+" DEMO twins, "+totalFrames+" ticks, snapshots/input guard; assertions="+checks);
    }
}
