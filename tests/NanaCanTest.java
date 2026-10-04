package org.elfen.engine;

import java.nio.file.*;
import java.util.*;

/** Original-data regression: can motion, hit/guard, command path and state restore. */
public final class NanaCanTest {
    static Path root;static int checks;static final Map<String,Pack> packs=new HashMap<>();
    static Pack p(String id)throws Exception{Pack p=packs.get(id);if(p==null){p=Pack.read(id,Files.newInputStream(root.resolve(id+"/data.efp")));packs.put(id,p);}return p;}
    static void eq(int actual,int expected,String label){checks++;if(actual!=expected)throw new AssertionError(label+": "+actual+" != "+expected);}
    static void yes(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    static byte[] instruction(int pc)throws Exception{return Arrays.copyOfRange(p("0104").code,pc*16,(pc+1)*16);}
    static BattleSimulation battle(String target)throws Exception{return new BattleSimulation(p("0104"),p(target),p("0080"),p("0116"),19);}
    static void fixtures(Path path)throws Exception{
        try(Scanner in=new Scanner(path)){
            while(in.hasNext()){
                String type=in.next();
                if(type.equals("contact")){
                    int fa=in.nextInt(),r=in.nextInt(),trigger=in.nextInt(),face=in.nextInt(),guard=in.nextInt(),distance=in.nextInt();
                    BattleSimulation b=battle("0104");Script owner=b.entities[0],victim=b.entities[1];
                    owner.x=640<<16;victim.x=(640+distance)<<16;victim.facingLeft=face==0;victim.input=guard==0?0:face==0?Input.RIGHT:Input.LEFT;
                    victim.hurt[0]=ContactTest.box(25,1,-79,54,84,3,100,0);
                    Script can=new Script(owner.pack,b,b.globals,false);can.controller=owner;can.parent=owner;can.charVars=owner.charVars;
                    can.x=owner.x;can.y=can.ground;can.facingLeft=face!=0;can.hit[0]=instruction(fa);can.response=instruction(r);can.triggers[1]=trigger;b.allocate(can);
                    b.collide();int[] actual={victim.life,victim.pendingSkill,can.pendingSkill,can.battleFlags,victim.battleFlags};
                    for(int n:actual)eq(n,in.nextInt(),"Original contact "+fa+" face="+face+" guard="+guard+" distance="+distance);
                    b.collide();eq(victim.life,in.nextInt(),"No second contact without rearm");
                }else if(type.equals("motion")){
                    int pc=in.nextInt(),face=in.nextInt(),ticks=in.nextInt();BattleSimulation b=battle("0104");
                    Script s=new Script(p("0104"),b,b.globals,false);s.start(pc==795?90:pc==1025?93:pc==1041?94:95,false);
                    s.pc=pc;s.x=640<<16;s.y=420<<16;s.facingLeft=face!=0;s.animationTick(false);
                    for(int n=0;n<ticks;n++)s.integrate();
                    for(int value:new int[]{s.x,s.y,s.vx,s.vy,s.ax,s.ay})eq(value,in.nextInt(),"Original M/integration pc="+pc+" tick="+ticks);
                }else throw new AssertionError(type);
            }
        }
    }
    static void command(int attack,int gap,boolean left)throws Exception{
        BattleSimulation b=battle("0170");int direction=left?-1:1;
        b.entities[0].x=(left?890:390)<<16;b.entities[0].facingLeft=left;
        b.entities[1].x=b.entities[0].x+(gap*direction<<16);b.entities[1].facingLeft=!left;
        int back=left?Input.RIGHT:Input.LEFT;
        int[] stream={0,Input.DOWN,Input.DOWN|back,back,back|attack,0};
        int expectedSkill=attack==Input.A?91:attack==Input.B?94:95;
        boolean found=false,hit=false,falling=false,landed=false,recorded=false;int previousX=0,movement=0;
        ArrayList<InputFrame> recording=new ArrayList<>();ArrayList<String> hashes=new ArrayList<>();byte[] snapshot=null;
        for(int frame=0;frame<800;frame++){
            InputFrame input=new InputFrame(frame,frame<stream.length?stream[frame]:0,0);b.step(input);
            for(Script s:b.entities)if(s!=null&&!s.ended&&s.pack.id.equals("0104")&&!s.character&&s.root==expectedSkill){
                found=true;
                if(s.skill==94){if(recorded&&s.freeze==0)movement+=direction*(s.x-previousX);previousX=s.x;recorded=true;}
                if(s.skill==95){falling|=s.y<s.ground;yes(s.hit[0]==null,"Strong can is not an active hit while falling");}
                if(s.skill==91)landed=true;
            }
            if(!hit&&b.life(1)<400){eq(b.life(1),395,"First unguarded can hit has original power5");hit=true;}
            if(frame==60)snapshot=b.snapshot();
            if(frame>60){recording.add(input);hashes.add(b.stateHash());}
        }
        yes(found,"Can spawned from command input");yes(hit,"Can really damages Lucy");
        if(attack==Input.B)yes(movement>100*65536,"Medium can travels forward");
        if(attack==Input.C){yes(falling,"Strong can drops from above");yes(landed,"Strong can becomes grounded attack");}
        for(int run=0;run<2;run++){
            b.restore(snapshot);
            for(int n=0;n<recording.size();n++){b.step(recording.get(n));yes(hashes.get(n).equals(b.stateHash()),"Can restore/replay frame="+b.frame());}
        }
        System.out.println("PASS can command "+(attack==Input.A?"A":attack==Input.B?"B":"C")+" "+(left?"left":"right")+" gap="+gap+"; first hit5HP; restore/replay2x739");
    }
    static void guardedCommand()throws Exception{
        BattleSimulation b=battle("0170");b.entities[1].x=460<<16;
        int[] stream={0,Input.DOWN,Input.DOWN|Input.LEFT,Input.LEFT,Input.LEFT|Input.A,0};
        boolean guard=false,phase=false,flames=false,damage=false;
        for(int frame=0;frame<160;frame++){
            // Hold back as the can arrives. Holding it from frame0 makes the
            // defender walk out of range during Nana's original startup.
            b.step(new InputFrame(frame,frame<stream.length?stream[frame]:0,frame>=34?Input.RIGHT:0));
            for(BattleSimulation.Event e:b.events())if(e.kind.equals("guard")&&!guard){eq(b.life(1),400,"Direct can block has no chip");guard=true;}
            for(Script s:b.entities)if(s!=null&&!s.ended&&s.pack.id.equals("0104")){
                if(s.skill==93){phase=true;eq(s.vx,65500,"After guard curse phase moves at original M100");}
                if(s.skill==90)flames=true;
            }
            if(guard&&b.life(1)<400)damage=true;
        }
        yes(guard,"Can was guarded");yes(phase,"ON2 starts curse phase93");yes(flames,"Curse phase spawns original flame objects90");yes(damage,"Flames can damage a guarding target via original R junctions");
        System.out.println("PASS direct block0HP → original ON2 phase93 → four flames90; flame damage retained");
    }
    public static void main(String[] args)throws Exception{
        root=Paths.get(args[0]);fixtures(Paths.get(args[1]));
        for(boolean left:new boolean[]{false,true}){command(Input.A,70,left);command(Input.B,260,left);command(Input.C,260,left);}
        guardedCommand();
        System.out.println("PASS Nana can "+checks+" assertions; original hitboxes/power/motion untouched");
    }
}
