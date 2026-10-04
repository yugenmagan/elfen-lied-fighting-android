import org.elfen.engine.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Desktop raster check of the same native core. Not an Android screenshot. */
public final class RenderCheck {
    static Path assets;static Map<String,BufferedImage> cache=new HashMap<>();
    static Pack read(String id)throws Exception{return Pack.read(id,Files.newInputStream(assets.resolve(id+"/data.efp")));}
    static void draw(Graphics2D g,PreviewSession scene,Script s,boolean bg)throws Exception{
        if(s.image<0||s.pack.widths[s.image]==0)return;String key=s.pack.id+"/"+String.format("%04d.png",s.image);
        BufferedImage image=cache.get(key);if(image==null){image=ImageIO.read(assets.resolve(key).toFile());cache.put(key,image);}
        int x=s.x/65536,y=s.y/65536;boolean flip=(s.imageFlags&16384)!=0,vflip=(s.imageFlags&32768)!=0;
        if(bg){x+=s.imageX-scene.cameraX;y+=s.imageY-scene.cameraY;}
        else{boolean left=s.facingLeft&&(s.imageOptions&1)==0;x+=(left?-s.imageX:s.imageX)-image.getWidth()/2;y+=s.imageY-image.getHeight();if(!s.absolute){x-=scene.cameraX;y-=scene.cameraY;}flip^=left;}
        AffineTransform a=new AffineTransform();a.translate(x+(flip?image.getWidth():0),y+(vflip?image.getHeight():0));a.scale(flip?-1:1,vflip?-1:1);g.drawImage(image,a,null);
    }
    public static void main(String[] args)throws Exception{
        assets=Paths.get(args[0]);Path out=Paths.get(args[1]);Files.createDirectories(out);
        for(String id:new String[]{"0170","0104","0128"}){
            PreviewSession scene=new PreviewSession(read(id),read("0104"),read("0080"));
            for(int i=0;i<100;i++){
                scene.tick(i==30?Input.C:0);
                if(i==15||i==55){BufferedImage image=new BufferedImage(640,480,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();g.setColor(Color.BLACK);g.fillRect(0,0,640,480);
                    for(Script s:scene.backgrounds)draw(g,scene,s,true);
                    ArrayList<Script> drawing=new ArrayList<>(scene.objects);drawing.add(scene.player);drawing.add(scene.partner);drawing.sort((a,b)->Integer.compare(a.depth,b.depth));for(Script s:drawing)if(!s.ended||s.character)draw(g,scene,s,false);
                    g.dispose();ImageIO.write(image,"png",out.resolve(id+"-frame"+i+"-desktop.png").toFile());}
            }
        }
        System.out.println("Six desktop render checks written; not Android screenshots.");
    }
}
