package org.elfen.controls;

import org.elfen.engine.*;
import java.nio.file.*;
import java.util.*;

/** Tests the actual Android UI adapter's pure geometry and ordered Input bridge. */
public final class ArcadeStickTest {
    static int checks;static Path root;static Map<String,Pack> packs=new HashMap<>();
    static void yes(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    static void eq(int a,int b,String label){yes(a==b,label+": "+a+" != "+b);}
    static Pack p(String id)throws Exception{Pack p=packs.get(id);if(p==null){p=Pack.read(id,Files.newInputStream(root.resolve(id+"/data.efp")));packs.put(id,p);}return p;}
    static void geometry(){
        int[] directions={Input.RIGHT,Input.RIGHT|Input.DOWN,Input.DOWN,Input.LEFT|Input.DOWN,Input.LEFT,Input.LEFT|Input.UP,Input.UP,Input.RIGHT|Input.UP};
        for(int[] size:new int[][]{{640,480},{800,480},{1280,720},{1920,1080},{2340,1080},{2400,1080},{2208,1768}})
        for(float scale:new float[]{.65f,1,1.5f})for(float dead:new float[]{.05f,.18f,.5f})for(boolean mirror:new boolean[]{false,true}){
            float x=size[0]*(mirror?.85f:.15f),y=size[1]*.75f,r=size[1]*.17f*scale,t=r*ArcadeStick.TRAVEL;
            ArcadeStick s=new ArcadeStick();s.configure(x,y,r,dead);yes(s.begin(4,x,y),"capture");eq(s.mask(),0,"neutral");
            yes(!s.begin(9,x,y),"second finger cannot steal stick");yes(!s.move(9,x+500,y),"ignore unrelated finger");
            s.move(4,x+t*dead*.99f,y);eq(s.mask(),0,"inside circular deadzone");s.move(4,x+t*dead*1.01f,y);eq(s.mask(),Input.RIGHT,"deadzone edge");
            for(int n=0;n<8;n++)for(int offset:new int[]{-20,0,20}){
                double angle=Math.toRadians(n*45+offset);
                s.move(4,x+(float)Math.cos(angle)*t*.95f,y+(float)Math.sin(angle)*t*.95f);eq(s.mask(),directions[n],"eight gate sectors");
                yes(Math.hypot(s.offsetX(),s.offsetY())<=t*1.001,"cap within gate");
            }
            s.move(4,x+r*10,y+r*10);eq(s.mask(),Input.DOWN|Input.RIGHT,"outside circle remains captured");yes(Math.abs(Math.hypot(s.offsetX(),s.offsetY())-t)<.02,"clamped cap");
            yes(!s.end(9)&&s.active(),"another finger release preserves hold");s.end(4);eq(s.mask(),0,"release neutral");yes(s.offsetX()==0&&s.offsetY()==0,"cap recentres");
            yes(!s.begin(4,x+r*2,y),"outside acquisition");s.begin(12,x+t,y);s.cancel();yes(!s.active()&&s.mask()==0,"cancel focus/background");
            s.begin(2,x,y);s.move(12,x+t,y);eq(s.mask(),0,"stale pointer after resume");s.end(2);
        }
    }
    static void simultaneous(){
        ArcadeStick s=new ArcadeStick();s.configure(100,100,100,.18f);Input input=new Input();s.begin(3,100,100);
        s.move(3,130,130);input.set(3,s.mask());input.set(5,Input.A);input.set(8,Input.B);
        eq(input.mask(),Input.RIGHT|Input.DOWN|Input.A|Input.B,"direction plus two attack fingers");
        input.set(5,0);eq(input.mask(),Input.RIGHT|Input.DOWN|Input.B,"releasing A keeps direction and B");
        s.end(3);input.set(3,0);eq(input.mask(),Input.B,"releasing stick keeps attack");input.clear();s.cancel();eq(input.mask(),0,"cancel all");
        s.begin(3,100,100);s.move(3,100,145);input.set(3,s.mask());s.move(3,135,135);input.set(3,s.mask());s.move(3,145,100);input.set(3,s.mask());input.set(5,Input.A);input.set(5,0);
        int[] history=input.drain();yes(Arrays.equals(history,new int[]{8,10,2,18,2}),"batched rolling samples keep order");eq(input.sample(),18,"quick attack survives before simulation tick");eq(input.sample(),2,"quick attack consumed once");
    }
    static InputFrame frame(Input input,int tick,FrameInput adapter){return new InputFrame(tick,adapter.poll(input),new int[]{0});}
    static void command(String id,int[] masks,String expected)throws Exception{
        BattleSimulation b=new BattleSimulation(p(id),p("0104"),p("0080"),p("0116"),19);Input input=new Input();ArcadeStick s=new ArcadeStick();s.configure(0,0,100,.18f);s.begin(3,0,0);boolean found=false;FrameInput adapter=new FrameInput();
        for(int mask:masks){
            float x=((mask&Input.RIGHT)!=0?1:0)-((mask&Input.LEFT)!=0?1:0),y=((mask&Input.DOWN)!=0?1:0)-((mask&Input.UP)!=0?1:0);
            s.move(3,x*45,y*45);input.set(3,s.mask());input.set(5,mask&0x3f0);b.step(frame(input,b.frame(),adapter));
            for(BattleSimulation.Event e:b.events())if(e.kind.equals("command")&&e.packId.equals(id)&&p(id).commands[e.value].name.equals(expected))found=true;
        }
        yes(found,"Real command recognizer from stick: "+id+" "+expected);System.out.println("PASS stick → InputFrame → command "+id+" "+expected);
    }
    public static void main(String[] args)throws Exception{
        root=Paths.get(args[0]);geometry();simultaneous();
        for(String id:new String[]{"0170","0104","0128"})command(id,new int[]{0,8,10,2,18},id.equals("0170")?"とび弱":id.equals("0104")?"ロケット弱":"磯部くん");
        command("0128",new int[]{0,2,10,8,9,1,17},"コマンド 1");int[] charge=new int[95];Arrays.fill(charge,1);charge[94]=18;command("0128",charge,"ねこ座薬");
        command("0104",new int[]{0,8,9,1,17},"のろい弱");command("0104",new int[]{0,8,9,1,33},"呪い中");command("0104",new int[]{0,8,9,1,65},"呪い大");
        System.out.println("PASS arcade stick "+checks+" assertions; 7 viewports, 3 scales, 3 deadzones, mirror, multi-source and real commands");
    }
}
