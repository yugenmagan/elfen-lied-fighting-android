import org.elfen.engine.*;import java.nio.file.*;import java.awt.*;import java.awt.image.*;import javax.imageio.*;
/** Actual simulation output rendered on desktop, explicitly not an Android screenshot. */
public final class MatchRenderCheck {
 public static void main(String[] args)throws Exception{
  BattleRenderCheck.root=Paths.get(args[0]);Path out=Paths.get(args[1]);Files.createDirectories(out);Pack a=BattleRenderCheck.p("0170"),b=BattleRenderCheck.p("0104"),s=BattleRenderCheck.p("0080"),k=BattleRenderCheck.p("0116");
  MatchController m=new MatchController(a,b,s,k,1,new MatchRules(k,1,10,0,80));int[] wanted={15,260,460,550,1510,1750};
  for(int f=0;f<1900&&!m.state().finished;f++){m.step(new InputFrame(f,0,0));for(int w:wanted)if(f==w){BufferedImage image=new BufferedImage(640,480,BufferedImage.TYPE_INT_RGB);BattleView view=m.view();for(BattleView.Sprite sp:view.sprites)BattleRenderCheck.sprite(image,view,sp);
   Graphics2D g=image.createGraphics();g.setColor(Color.WHITE);g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,14));g.drawString("Round "+m.state().roundNumber+" | "+m.state().score1+":"+m.state().score2+" | timer "+m.state().timer,200,70);g.dispose();for(BattleView.Sprite sp:m.overlays())BattleRenderCheck.sprite(image,view,sp);
   g=image.createGraphics();g.setColor(Color.YELLOW);g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,12));g.drawString("DESKTOP validation (not Android) | frame "+m.frame()+" phase "+m.state().phase,8,472);g.dispose();ImageIO.write(image,"png",out.resolve("match-"+f+".png").toFile());
  }}System.out.println("PASS desktop match render samples");
 }
}
