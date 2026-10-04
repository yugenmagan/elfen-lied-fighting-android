package org.elfen.fighting;
import org.elfen.engine.*;import org.elfen.presentation.*;import org.elfen.controls.*;
import android.graphics.*;import java.nio.file.*;import java.util.*;import javax.imageio.ImageIO;
/** Production Canvas renderer through desktop adapters; not Android execution. */
public final class TitleScreenPreview {
 public static void main(String[] args)throws Exception{
  Path root=Paths.get(args[0]),out=Paths.get(args[1]);Files.createDirectories(out);Map<String,Pack> packs=new HashMap<>();
  try(java.util.stream.Stream<Path> files=Files.list(root.resolve("game"))){for(Path f:(Iterable<Path>)files.filter(Files::isDirectory)::iterator)packs.put(f.getFileName().toString(),Pack.read(f.getFileName().toString(),Files.newInputStream(f.resolve("data.efp"))));}
  StoryCatalog stories=new StoryCatalog(packs.get("0116"),packs,Files.newInputStream(root.resolve("game/story-index.tsv")));
  for(int i=0;i<6;i++)try{System.out.println("KGT screen["+i+"]="+stories.screen(i).id+" "+stories.screen(i).name);}catch(IllegalArgumentException e){System.out.println("KGT screen["+i+"] unassigned");}
  Pack title=stories.screen(0);if(!title.id.equals("0074")||stories.demo(7)!=title)throw new AssertionError("Title mapping");
  DemoSession demo=new DemoSession(title);for(int i=0;i<2;i++)demo.step(new InputFrame(demo.frame(),0,0));boolean found=false;
  for(BattleView.Sprite s:demo.view()){System.out.println("Title sprite="+s.packId+":"+s.image+" xy="+s.x+","+s.y+" bg="+s.background);found|=s.packId.equals("0074")&&s.image==11;}
  if(!found)throw new AssertionError("Original demo must emit title image");
  LocaleCatalog locale=new LocaleCatalog(Files.newInputStream(root.resolve("localization/ui.tsv")),Files.newInputStream(root.resolve("localization/scenes.tsv")));
  Bitmap art=new Bitmap(ImageIO.read(root.resolve("game/0074/0011.png").toFile()));int views=0,hits=0;
  for(String code:LocaleCatalog.CODES){locale.language(code);for(int state=0;state<3;state++){
   Bitmap b=Bitmap.createBitmap(640,480,Bitmap.Config.ARGB_8888);TitleScreen.draw(new Canvas(b),art,locale,state==2,state==1,state==1?1:0);
   ImageIO.write(b.image,"png",out.resolve(code+"-title-"+state+".png").toFile());views++;
   if((b.image.getRGB(635,475)>>>24)!=255)throw new AssertionError("Previous battle may bleed through title");
  }}
  for(int[] size:new int[][]{{640,480},{1280,720},{2048,945},{2400,1080},{800,1280},{2560,1600}}){TitleLayout l=new TitleLayout(size[0],size[1]);for(int i=0;i<4;i++){float[] b=TitleLayout.BUTTONS[i];if(l.hit(l.left+(b[0]+b[2])/2*l.scale,l.top+(b[1]+b[3])/2*l.scale)!=i)throw new AssertionError("Scaled menu hit");hits++;}if(l.hit(l.left-1,l.top+120*l.scale)!=-1)throw new AssertionError("Letterbox hit");hits++;}
  System.out.println("PASS "+views+" original-title desktop renders, "+hits+" scaled hit-region checks; bgm="+title.bgm+" demo="+title.name);
 }
}
