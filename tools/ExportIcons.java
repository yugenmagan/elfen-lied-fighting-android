import java.awt.*;import java.awt.geom.*;import java.awt.image.*;import java.nio.file.*;import javax.imageio.ImageIO;
/** Deterministic Android resource exports from the original-derived portrait. */
public final class ExportIcons {
 static BufferedImage source;
 static BufferedImage draw(int size,boolean adaptive,boolean round){
  BufferedImage out=new BufferedImage(size,size,BufferedImage.TYPE_INT_ARGB);Graphics2D g=out.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
  if(!adaptive){if(round)g.setClip(new Ellipse2D.Double(0,0,size,size));g.setPaint(new GradientPaint(0,0,new Color(0x602139),size,size,new Color(0x200c19)));g.fillRect(0,0,size,size);}
  double scale=adaptive?.72:1;int art=(int)Math.round(size*scale),x=(size-art)/2,y=adaptive?(int)Math.round(size*.18):0;g.drawImage(source,x,y,art,art,null);g.dispose();return out;
 }
 public static void main(String[] a)throws Exception{
  Path root=Paths.get(a[0]),res=root.resolve("android/app/src/main/res");source=ImageIO.read(root.resolve("docs/icon/lucy-foreground-source.png").toFile());
  if(!source.getColorModel().hasAlpha())throw new IllegalStateException("Icon foreground must have alpha");
  int[] sizes={48,72,96,144,192};String[] density={"mdpi","hdpi","xhdpi","xxhdpi","xxxhdpi"};
  for(int n=0;n<sizes.length;n++){Path d=res.resolve("mipmap-"+density[n]);Files.createDirectories(d);ImageIO.write(draw(sizes[n],false,false),"png",d.resolve("ic_launcher.png").toFile());ImageIO.write(draw(sizes[n],false,true),"png",d.resolve("ic_launcher_round.png").toFile());}
  Files.createDirectories(res.resolve("drawable-nodpi"));ImageIO.write(draw(1024,true,false),"png",res.resolve("drawable-nodpi/launcher_foreground.png").toFile());ImageIO.write(draw(1024,false,false),"png",root.resolve("docs/icon/elfen-fighting-icon-1024.png").toFile());
  System.out.println("Exported ten density icons, adaptive foreground and 1024px source export.");
 }
}
