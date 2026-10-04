package org.elfen.engine;
import java.nio.file.*;import java.util.*;import java.io.*;

/** Native HUD fixtures, all-player assets, and presentation/rollback isolation. */
public final class BattleHudTest {
    static final Map<String,Pack> packs=new HashMap<>();static int checks;
    static void check(boolean b,String message){checks++;if(!b)throw new AssertionError(message);}
    static BattleSimulation battle(String a,String b){return new BattleSimulation(packs.get(a),packs.get(b),packs.get("0138"),packs.get("0116"),19);}
    static BattleHud.Draw gauge(List<BattleHud.Draw> list,int image,int p){
        for(BattleHud.Draw d:list)if(d.sprite.packId.equals("0116")&&d.sprite.image==image&&((d.sprite.flags&16384)!=0)==(p==1))return d;
        throw new AssertionError("Missing gauge "+image+":"+p);
    }
    static String signature(List<BattleHud.Draw> list){StringBuilder b=new StringBuilder();for(BattleHud.Draw d:list){BattleView.Sprite s=d.sprite;b.append(s.packId).append(':').append(s.image).append(':').append(s.x).append(':').append(s.y).append(':').append(s.offsetX).append(':').append(s.offsetY).append(':').append(s.flags).append(':').append(s.left).append(':').append(s.rgba).append(':').append(d.clipLeft).append(':').append(d.clipTop).append(':').append(d.clipWidth).append(';');}return b.toString();}
    public static void main(String[] args)throws Exception{
        Path assets=Paths.get(args[0]),fixtures=Paths.get(args[1]);
        try(java.util.stream.Stream<Path> files=Files.list(assets)){for(Path d:(Iterable<Path>)files.filter(Files::isDirectory)::iterator){Pack p=Pack.read(d.getFileName().toString(),Files.newInputStream(d.resolve("data.efp")));packs.put(p.id,p);}}
        BattleHud hud=new BattleHud(packs.get("0116"),packs);BattleSimulation sim=battle("0104","0170");
        for(String line:Files.readAllLines(fixtures.resolve("clip-x86.tsv"))){
            int[] x=Arrays.stream(line.split("\\t")).mapToInt(Integer::parseInt).toArray();int p=x[0]%2,image=x[0]<20?82:83;
            int old=Pack.i32(sim.entities[p].pack.settings,image==82?1754:1758);
            byte[] setting=sim.entities[p].pack.settings;int off=image==82?1754:1758;
            for(int n=0;n<4;n++)setting[off+n]=(byte)(x[3]>>>(8*n));
            int oldValue=image==82?sim.entities[p].life:sim.entities[p].special;
            if(image==82)sim.entities[p].life=x[2];else sim.entities[p].special=x[2];
            BattleHud.Draw d=gauge(hud.view(sim.view(),60,0,0,2),image,p);int base=image==82?(p==0?41:393):(p==0?76:445);
            check(d.clipLeft-base==x[4],"native crop position "+line);check(d.clipWidth==x[5],"native crop width "+line);check(d.clipLeft-base==x[6],"native crop source "+line);
            for(int n=0;n<4;n++)setting[off+n]=(byte)(old>>>(8*n));if(image==82)sim.entities[p].life=oldValue;else sim.entities[p].special=oldValue;
        }
        for(String line:Files.readAllLines(fixtures.resolve("timer-x86.tsv"))){
            int[] x=Arrays.stream(line.split("\\t")).mapToInt(Integer::parseInt).toArray();ArrayList<BattleView.Sprite> digits=new ArrayList<>();
            for(BattleHud.Draw d:hud.view(sim.view(),x[0],0,0,2))if(d.sprite.packId.equals("0116")&&d.sprite.image>=25&&d.sprite.image<=35)digits.add(d.sprite);
            digits.sort((a,b)->Integer.compare(a.x,b.x));check(digits.size()==(x.length-1)/3,"native timer count "+x[0]);
            for(int n=0;n<digits.size();n++){BattleView.Sprite s=digits.get(n);check(s.image==x[1+n*3]-9&&s.x/65536==x[2+n*3]&&s.y/65536==x[3+n*3],"native timer position/skill "+line);}
        }
        int players=0,portraits=0,empty=0;
        for(Pack p:packs.values())if(p.kind.equals(".player")){
            players++;sim=battle(p.id,p.id);byte[] before=sim.snapshot();
            for(int tick:new int[]{0,1,5,6,10,11,127}){
                sim.frame=tick;
                for(int side=0;side<2;side++){sim.entities[side].life=side==0?sim.entities[side].pack.life()/2:0;sim.entities[side].special=Pack.i32(p.settings,1758)/2;}
                List<BattleHud.Draw> draws=hud.view(sim.view(),99,1,0,2);
                int faces=0;for(BattleHud.Draw d:draws){BattleView.Sprite s=d.sprite;check(Files.isRegularFile(assets.resolve(s.packId+"/"+String.format(Locale.ROOT,"%04d.png",s.image))),"missing HUD asset");if(s.packId.equals(p.id))faces++;}
                boolean expected=!(p.id.equals("0142")||p.id.equals("0144")||p.id.equals("0154"));check(faces==(expected?2:0),"original portrait availability "+p.id);
                if(tick==0){if(expected)portraits++;else empty++;}
                String hash=sim.stateHash(),draw=signature(draws);for(int n=0;n<3;n++)check(draw.equals(signature(hud.view(sim.view(),99,1,0,2))),"HUD projection repeat");check(hash.equals(sim.stateHash()),"HUD mutated battle "+p.id);
            }
            sim.restore(before);check(Arrays.equals(before,sim.snapshot()),"HUD state restore");
        }
        sim=battle("0170","0104");BattleSimulation twin=battle("0170","0104");sim.setCpu(0,80);sim.setCpu(1,80);twin.setCpu(0,80);twin.setCpu(1,80);
        byte[] snapshot=null;String saved=null;
        for(int f=0;f<2400;f++){
            InputFrame input=new InputFrame(f,0,0);sim.step(input);twin.step(input);
            List<BattleHud.Draw> draws=hud.view(sim.view(),Math.max(0,60-f/60),0,0,2);
            if(f==700){snapshot=sim.snapshot();saved=signature(draws);}
            if(f%31==0)check(sim.stateHash().equals(twin.stateHash()),"HUD execution changed checksum "+f);
        }
        sim.restore(snapshot);check(saved.equals(signature(hud.view(sim.view(),60-700/60,0,0,2))),"Restored frame HUD differs");
        sim=battle("0104","0170");sim.frame=5;int first=gauge(hud.view(sim.view(),60,0,0,2),83,0).sprite.rgba;sim.frame=6;int second=gauge(hud.view(sim.view(),60,0,0,2),83,0).sprite.rgba;
        check(first==0xf4000a00&&second==0x1e201e00,"Original five-tick meter colour cycle");
        check(players==14&&portraits==11&&empty==3,"all original PLAYER files");
        System.out.println("PASS HUD: "+checks+" checks, 56 x86 crops, 12 x86 timer layouts, "+players+" PLAYER files ("+portraits+" portraits / "+empty+" explicitly empty original skills); 2400 simulation ticks with/without HUD, restore/repeated views; no Android execution");
    }
}
