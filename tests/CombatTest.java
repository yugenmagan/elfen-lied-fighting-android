package org.elfen.engine;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public final class CombatTest {
    static int checks;static Pack lucy,nana,stage,kgt;static Path root;
    static void check(boolean value,String reason){checks++;if(!value)throw new AssertionError(reason);}
    static Pack read(String id)throws IOException{return Pack.read(id,Files.newInputStream(root.resolve(id+"/data.efp")));}
    static BattleSimulation battle() {return new BattleSimulation(lucy,nana,stage,kgt,1);}
    static byte[] box(int op,int x,int y,int w,int h,int flags,int rate,int power){byte[] b=new byte[16];b[0]=(byte)op;put16(b,1,x);put16(b,3,y);put16(b,5,w);put16(b,7,h);b[10]=(byte)flags;b[11]=(byte)rate;b[12]=(byte)power;return b;}
    static void put16(byte[] b,int pos,int n){b[pos]=(byte)n;b[pos+1]=(byte)(n>>>8);}
    static void originalFixtures(Path path)throws IOException{
        for(String line:Files.readAllLines(path)){
            String[] a=line.split(" ");
            if(a[0].equals("damage")){int[] v=new int[5];for(int i=0;i<5;i++)v[i]=Integer.parseInt(a[i+1]);check(CombatMath.damage(v[0],v[1],v[2],v[3])==v[4],"x86 damage "+line);}
            else if(a[0].equals("rng")){CombatMath.Rng r=new CombatMath.Rng((int)Long.parseLong(a[1]));for(int i=2;i<a.length;i++)check(r.next()==Integer.parseInt(a[i]),"x86 rng "+a[1]+":"+i);}
            else if(a[0].equals("collision")){
                int[] v=new int[a.length-1];for(int i=0;i<v.length;i++)v[i]=Integer.parseInt(a[i+1]);BattleSimulation b=battle();
                Script at=b.entities[0],df=b.entities[1];at.x=v[5]<<16;df.x=v[6]<<16;at.facingLeft=v[7]!=0;df.facingLeft=v[8]!=0;
                df.input=BattleSimulation.relative(v[2],df.facingLeft);b.combo[1]=v[4];
                at.hit[0]=box(24,54,-110,30,23,v[3],0,v[0]);df.hurt[0]=box(25,1,-79,54,84,3,v[1],0);
                at.response=new byte[16];at.response[0]=23;for(int i=0;i<6;i++)put16(at.response,1+i*2,new int[]{1,2,3,7,8,9}[i]);
                b.collide();int[] got={df.life,df.pendingSkill,at.freeze,df.freeze,b.combo[1],at.battleFlags,df.battleFlags};
                for(int i=0;i<7;i++)check(got[i]==v[9+i],"x86 collision field "+i+" got "+got[i]+" expected "+v[9+i]+" / "+line);
            }
        }
    }
    static void snapshotRoundtrip(BattleSimulation b){byte[] before=b.snapshot();String hash=b.stateHash();b.restore(before);check(Arrays.equals(before,b.snapshot()),"snapshot byte roundtrip at "+b.frame());check(hash.equals(b.stateHash()),"hash roundtrip");}
    static void runFight(Pack fighter)throws Exception{
        BattleSimulation a=new BattleSimulation(fighter,nana,stage,kgt,1),b=new BattleSimulation(fighter,nana,stage,kgt,1);
        ArrayList<InputFrame> recording=new ArrayList<>();ArrayList<String> hashes=new ArrayList<>();int hitFrames=0,objectFrames=0;
        byte[] checkpoint=null;int saved=-1;
        for(int f=0;f<18000;f++){
            int distance=Math.abs(a.entities[0].x-a.entities[1].x)/65536;
            int p1=a.entities[0].facingLeft?Input.LEFT:Input.RIGHT;
            int attack=f%50==0?Input.A:0;
            InputFrame input=new InputFrame(f,(distance>75?p1:0)|attack,0);recording.add(input);
            a.step(input);b.step(input);check(a.stateHash().equals(b.stateHash()),"twin desync "+fighter.id+" at "+f);
            hashes.add(a.stateHash());for(BattleSimulation.Event e:a.events())if(e.kind.equals("hit"))hitFrames++;
            if(a.activeObjects()>0)objectFrames++;
            if(f==350){checkpoint=a.snapshot();saved=f+1;snapshotRoundtrip(a);}
            if(f%251==0)snapshotRoundtrip(b);
            if(a.phase()==BattleSimulation.KO&&f>a.koFrame+350)break;
        }
        check(hitFrames>0,"actual attacks must hit "+fighter.id);check(a.phase()==BattleSimulation.KO,"fight reaches KO "+fighter.id);
        BattleSimulation c=new BattleSimulation(fighter,nana,stage,kgt,1);c.restore(checkpoint);
        for(int f=saved;f<recording.size();f++){c.step(recording.get(f));check(c.stateHash().equals(hashes.get(f)),"restored replay desync at "+fighter.id+":"+f);}
        check(c.stateHash().equals(a.stateHash()),"replay final hash");
        System.out.println("FIGHT "+fighter.id+" frames="+a.frame()+" hits="+hitFrames+" objects="+objectFrames+" hp="+a.life(0)+":"+a.life(1)+" hash="+a.stateHash());
    }
    public static void main(String[] args)throws Exception{
        root=Paths.get(args[0]);lucy=read("0170");nana=read("0104");stage=read("0080");kgt=read("0116");
        originalFixtures(Paths.get(args[1]));System.out.println("PASS original collision/damage/RNG fixtures "+checks);
        BattleSimulation b=battle();snapshotRoundtrip(b);byte[] initial=b.snapshot();boolean invalid=false;
        try{b.step(new InputFrame(1,0,0));}catch(IllegalArgumentException e){invalid=true;}check(invalid&&Arrays.equals(initial,b.snapshot()),"wrong frame rejected without mutation");
        invalid=false;byte[] corrupt=initial.clone();corrupt[3]^=1;try{b.restore(corrupt);}catch(IllegalArgumentException e){invalid=true;}check(invalid&&Arrays.equals(initial,b.snapshot()),"bad snapshot transactional rejection");
        b.meter(0,450);check(b.entities[0].stocks==5&&b.entities[0].special==50,"meter gain carries");b.meter(0,-500);check(b.entities[0].stocks==2&&b.entities[0].special==150,"meter drain borrows");
        for(String id:new String[]{"0170","0104","0128"})runFight(read(id));
        System.out.println("PASS combat and deterministic restore assertions="+checks);
        System.out.println("Not Android installation, original AI, match rules, story or online validation.");
    }
}
