import org.elfen.engine.*;import java.nio.file.*;import java.awt.image.*;import javax.imageio.*;
/** Export the real DEMO VM frames; these are desktop renders, not Android screenshots. */
public final class DemoRenderCheck {
    public static void main(String[] args)throws Exception{
        BattleRenderCheck.root=Paths.get(args[0]);Path out=Paths.get(args[1]);Files.createDirectories(out);
        for(String id:new String[]{"0118","0202","0222","0078","0068","0124","0064","0086"}){
            Pack pack=BattleRenderCheck.p(id);DemoSession d=new DemoSession(pack);
            for(int f=0;f<=2100;f++){d.step(new InputFrame(f,0,0));if(f==100||f==750||f==1700){
                BufferedImage image=new BufferedImage(640,480,BufferedImage.TYPE_INT_RGB);for(BattleView.Sprite s:d.view())BattleRenderCheck.sprite(image,null,s);ImageIO.write(image,"png",out.resolve(id+"-"+f+"-desktop.png").toFile());
            }}
        }System.out.println("PASS 24 original scene VM desktop renders; not Android execution");
    }
}
