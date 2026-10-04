import org.elfen.engine.*;
import java.awt.image.*;import java.nio.file.*;import java.util.*;import javax.imageio.ImageIO;

/** Software previews of the actual HUD draw list; explicitly not Android screenshots. */
public final class HudRenderCheck {
    static final Map<String,Pack> packs=new HashMap<>();static BattleHud hud;static Path out;
    static void draw(BufferedImage image,BattleView view,List<BattleHud.Draw> draws)throws Exception{
        for(BattleHud.Draw d:draws){
            if(d.clipWidth<=0||d.clipHeight<=0)continue;
            BufferedImage layer=new BufferedImage(640,480,BufferedImage.TYPE_INT_ARGB);
            BattleRenderCheck.sprite(layer,view,d.sprite);
            for(int y=Math.max(0,d.clipTop);y<Math.min(480,d.clipTop+d.clipHeight);y++)for(int x=Math.max(0,d.clipLeft);x<Math.min(640,d.clipLeft+d.clipWidth);x++)if((layer.getRGB(x,y)>>>24)!=0)image.setRGB(x,y,layer.getRGB(x,y));
        }
    }
    static void render(BattleSimulation sim,String name,int seconds,int wins,boolean stage)throws Exception{
        BattleView v=sim.view();BufferedImage image=new BufferedImage(640,480,BufferedImage.TYPE_INT_ARGB);
        if(stage)for(BattleView.Sprite s:v.sprites)BattleRenderCheck.sprite(image,v,s);
        draw(image,v,hud.view(v,seconds,wins,0,2));ImageIO.write(image,"png",out.resolve(name+".png").toFile());
    }
    public static void main(String[] args)throws Exception{
        BattleRenderCheck.root=Paths.get(args[0]);out=Paths.get(args[1]);Files.createDirectories(out);
        try(java.util.stream.Stream<Path> files=Files.list(BattleRenderCheck.root)){for(Path d:(Iterable<Path>)files.filter(Files::isDirectory)::iterator){Pack p=BattleRenderCheck.p(d.getFileName().toString());packs.put(p.id,p);}}
        hud=new BattleHud(packs.get("0116"),packs);
        for(String[] ids:new String[][]{{"0104","0154"},{"0170","0104"},{"0128","0110"},{"0142","0156"}}){
            BattleSimulation sim=new BattleSimulation(packs.get(ids[0]),packs.get(ids[1]),packs.get("0138"),packs.get("0116"),19);
            for(int f=0;f<100;f++)sim.step(new InputFrame(f,0,0));
            render(sim,ids[0]+"-"+ids[1]+"-desktop",-1,0,true);
            if(ids[0].equals("0104"))render(sim,"reference-hud-layer",-1,0,false);
            sim=new BattleSimulation(packs.get(ids[0]),packs.get(ids[1]),packs.get("0138"),packs.get("0116"),19);
            sim.setCpu(0,80);sim.setCpu(1,80);for(int f=0;f<1100;f++)sim.step(new InputFrame(f,0,0));
            render(sim,ids[0]+"-"+ids[1]+"-damage-desktop",9,1,true);
        }
        System.out.println("PASS 9 desktop previews from original HUD assets and live simulation, including damage/meter/portraits; not Android screenshots");
    }
}
