package org.elfen.fighting;
import org.elfen.engine.*;import org.elfen.presentation.*;import android.graphics.*;
import java.nio.file.*;import java.util.*;import java.io.*;import javax.imageio.ImageIO;
/** Production text renderer via a small Java2D Canvas adapter. Not Android execution. */
public final class Localization008Test {
 static int checks;static void check(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception {
  Path root=Paths.get(args[0]),out=Paths.get(args[1]);Files.createDirectories(out);
  LocaleCatalog lc=new LocaleCatalog(Files.newInputStream(root.resolve("localization/ui.tsv")),Files.newInputStream(root.resolve("localization/scenes.tsv")));
  check(lc.keys().size()==367,"UI coverage");int sprites=0,regions=0,commands=0,packs=0,demos=0;float smallest=999;ArrayList<String> fit=new ArrayList<>();
  for(String code:LocaleCatalog.CODES){lc.language(code);for(String key:lc.keys())check(!lc.text(key).isEmpty(),"missing "+key);}
  try(java.util.stream.Stream<Path> stream=Files.list(root.resolve("game"))){for(Path folder:(Iterable<Path>)stream.filter(Files::isDirectory)::iterator){
   Pack p=Pack.read(folder.getFileName().toString(),Files.newInputStream(folder.resolve("data.efp")));packs++;
   for(String code:LocaleCatalog.CODES){lc.language(code);check(!lc.text("pack:"+p.id).isEmpty(),"pack name");for(Pack.Command command:p.commands)if(!command.name.isEmpty()){check(!lc.move(command.name).isEmpty(),"command "+command.name);commands++;}}
   if(p.kind.equals(".demo")){demos++;DemoSession a=new DemoSession(p),b=new DemoSession(p);for(int f=0;f<2100;f++){lc.language(LocaleCatalog.CODES[f%3]);InputFrame i=new InputFrame(f,0,0);a.step(i);b.step(i);for(BattleView.Sprite s:a.view())lc.sprite(s.packId,s.image);check(a.stateHash().equals(b.stateHash()),"language affected DEMO "+p.id+":"+f);}}
  }}
  Bitmap card=new Bitmap(ImageIO.read(root.resolve("game/0064/0002.png").toFile()));
  for(List<LocaleCatalog.Block> blocks:lc.allArt()){
   LocaleCatalog.Block first=blocks.get(0);Bitmap source=new Bitmap(ImageIO.read(root.resolve("game/"+first.pack+"/"+String.format(Locale.ROOT,"%04d.png",first.image)).toFile()));
   lc.language("ja");check(lc.sprite(first.pack,first.image)==null,"JP must use original bitmap");
   for(int lang=1;lang<=2;lang++){
    lc.language(LocaleCatalog.CODES[lang]);for(LocaleCatalog.Block b:blocks){regions++;check(b.x+b.width<=source.getWidth()&&b.y+b.height<=source.getHeight(),"region bounds");
     Paint p=new Paint(1);p.setTypeface(Typeface.create("",b.pack.equals("0116")?1:0));
     TextFit.Layout layout=TextFit.fit(b.text(lang),b.width-4,b.height-4,b.font,(s,size)->{p.setTextSize(size);return p.measureText(s);});
     TextFit.fit(b.text(lang),b.width-4,b.height-4,b.font,(s,size)->{p.setTextSize(size);return p.measureText(s)*1.12f;});
     for(String line:layout.lines){p.setTextSize(layout.size);check(p.measureText(line)<=b.width-4+.01,"horizontal text overflow");for(int n=0;n<line.length();n++)check(p.font().canDisplay(line.charAt(n)),"unsupported translated glyph");}
     check(layout.lines.size()*layout.lineHeight<=b.height-4+.01,"vertical text overflow");smallest=Math.min(smallest,layout.size);fit.add(lc.code()+"\t"+b.pack+":"+b.image+"\t"+layout.size+"\t"+layout.lines.size());
    }
    Bitmap translated=lc.sprite(first.pack,first.image)==null?source:LocalizedSprites.render(source,card,blocks,lang);
    check(translated.getWidth()==source.getWidth()&&translated.getHeight()==source.getHeight(),"VM geometry changed");Path dest=out.resolve(lc.code());Files.createDirectories(dest);ImageIO.write(translated.image,"png",dest.resolve(first.pack+"-"+first.image+".png").toFile());sprites++;
   }
  }
  Files.write(out.resolve("text-layout.tsv"),fit);check(packs==46&&demos==27&&sprites==180&&regions==190,"all assets/catalogues");
  System.out.println("PASS "+checks+" checks: 367 UI keys / three languages; "+packs+" packs; "+commands+" nonempty command-name lookups; "+demos+" DEMO twin runs / 56700 ticks; "+sprites+" sprite renders / "+regions+" translated regions; minimum font "+smallest+"px. Desktop Java2D adapter, not Android execution.");
 }
}
