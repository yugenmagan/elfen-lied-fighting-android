import org.elfen.engine.*;
import java.nio.file.*;
public class MotionScenarios {
    static Path assets;
    static Pack load(String id)throws Exception{return Pack.read(id,Files.newInputStream(assets.resolve(id+"/data.efp")));}
    static void run(String id,int[] masks,String name,String expected)throws Exception{
        PreviewSession s=new PreviewSession(load(id),load("0104"),load("0080"));
        for(int mask:masks)s.tick(mask);String cmd=s.lastCommand;
        if(!cmd.equals(expected))throw new AssertionError(id+" "+name+": expected "+expected+", got "+cmd);
        for(int i=0;i<600;i++)s.tick(0);System.out.println("PASS "+id+" "+name+" command="+cmd);
    }
    public static void main(String[] a)throws Exception{
        assets=Paths.get(a[0]);run("0170",new int[]{0,8,10,2,18},"quarter-circle A","とび弱");
        run("0104",new int[]{0,8,10,2,18},"quarter-circle A","ロケット弱");run("0128",new int[]{0,8,10,2,18},"quarter-circle A","磯部くん");
        int[] charge=new int[95];java.util.Arrays.fill(charge,1);charge[94]=18;run("0128",charge,"back charge forward A","ねこ座薬");
        run("0128",new int[]{0,2,10,8,9,1,17},"half-circle A","コマンド 1");
        for(String stage:new String[]{"0080","0084","0138","0148"}){
            PreviewSession s=new PreviewSession(load("0170"),load("0104"),load(stage));
            for(int i=0;i<1800;i++)s.tick(0);System.out.println("PASS stage "+stage+" 1800 ticks");
        }
    }
}
