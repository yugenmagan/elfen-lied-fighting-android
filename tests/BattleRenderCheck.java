import org.elfen.engine.*;
import java.awt.*;import java.awt.image.*;import java.nio.file.*;import java.util.*;import javax.imageio.ImageIO;

/** Renders the immutable battle view. Evidence of core rendering, not device screenshots. */
public final class BattleRenderCheck {
 static Path root;static Map<String,BufferedImage> cache=new HashMap<>();
 static Pack p(String id)throws Exception{return Pack.read(id,Files.newInputStream(root.resolve(id+"/data.efp")));}
 static void sprite(BufferedImage dst,BattleView v,BattleView.Sprite s)throws Exception{
  String path=s.packId+"/"+String.format(Locale.ROOT,"%04d.png",s.image);BufferedImage image=cache.get(path);
  if(image==null){Path file=root.resolve(path);if(!Files.exists(file))return;image=ImageIO.read(file.toFile());if(image==null)throw new IllegalStateException(path);cache.put(path,image);}
  int w=image.getWidth(),h=image.getHeight(),x=s.x/65536,y=s.y/65536;boolean fx=(s.flags&16384)!=0,fy=(s.flags&32768)!=0;
  if(s.background){x+=s.offsetX;y+=s.offsetY;if(!s.absolute){x-=v.cameraX;y-=v.cameraY;}}
  else{boolean left=s.left&&(s.options&1)==0;x+=(left?-s.offsetX:s.offsetX)-w/2;y+=s.offsetY-h;if(!s.absolute){x-=v.cameraX;y-=v.cameraY;}fx^=left;}
  for(int yy=Math.max(0,y);yy<Math.min(480,y+h);yy++)for(int xx=Math.max(0,x);xx<Math.min(640,x+w);xx++){
   int sx=xx-x,sy=yy-y;if(fx)sx=w-1-sx;if(fy)sy=h-1-sy;int pixel=image.getRGB(sx,sy);if((pixel>>>24)==0)continue;
   int r=(pixel>>>16)&255,g=(pixel>>>8)&255,b=pixel&255;
   if((s.rgba&0xffffff00)!=0){r=Math.max(0,Math.min(31,(r>>3)+(byte)(s.rgba>>>24)))*8;g=Math.max(0,Math.min(31,(g>>3)+(byte)(s.rgba>>>16)))*8;b=Math.max(0,Math.min(31,(b>>3)+(byte)(s.rgba>>>8)))*8;if(r+g+b==0)b=8;}
   int old=dst.getRGB(xx,yy),dr=(old>>>16)&255,dg=(old>>>8)&255,db=old&255;
   switch(s.colour){case 1:r=(r+dr)/2;g=(g+dg)/2;b=(b+db)/2;break;case 2:r=Math.min(255,r+dr);g=Math.min(255,g+dg);b=Math.min(255,b+db);break;
    case 3:r=Math.max(0,dr-r);g=Math.max(0,dg-g);b=Math.max(0,db-b);break;
    case 4:int a=Math.max(0,Math.min(32,32-(byte)s.rgba));r=(r*a+dr*(32-a))/32;g=(g*a+dg*(32-a))/32;b=(b*a+db*(32-a))/32;break;
   }dst.setRGB(xx,yy,0xff000000|(r<<16)|(g<<8)|b);
  }
 }
 static void render(BattleView view,Path file)throws Exception{
  BufferedImage image=new BufferedImage(640,480,BufferedImage.TYPE_INT_RGB);for(BattleView.Sprite s:view.sprites)sprite(image,view,s);
  Graphics2D g=image.createGraphics();g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,13));
  for(int n=0;n<2;n++){BattleView.Fighter f=view.fighters.get(n);int x=n==0?12:356;g.setColor(new Color(0x18121d));g.fillRect(x,15,272,28);int w=268*f.life/f.maxLife;g.setColor(new Color(n==0?0xbd3158:0x687ddb));g.fillRect(n==0?x+2:x+270-w,17,w,24);g.setColor(Color.WHITE);g.drawString("P"+(n+1)+" HP "+f.life+" / "+f.maxLife+" stock "+f.stocks,x,58);}
  g.setColor(Color.WHITE);g.drawString("Desktop check | frame "+view.frame+" | not Android screenshot",12,474);g.dispose();ImageIO.write(image,"png",file.toFile());
 }
 public static void main(String[] args)throws Exception{
  root=Paths.get(args[0]);Path out=Paths.get(args[1]);Files.createDirectories(out);int count=0;
  for(String id:new String[]{"0170","0104","0128"}){BattleSimulation sim=new BattleSimulation(p(id),p("0104"),p("0080"),p("0116"),7);sim.setCpu(0,80);sim.setCpu(1,80);
   for(int frame=0;frame<1800;frame++){sim.step(new InputFrame(frame,0,0));if(frame==100||frame==700||frame==1500){render(sim.view(),out.resolve(id+"-"+frame+"-desktop.png"));count++;}}
  }System.out.println("Rendered "+count+" real simulation frames on desktop; no Android installation claim.");
 }
}
