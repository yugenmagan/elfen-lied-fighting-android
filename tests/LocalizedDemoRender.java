import org.elfen.engine.*;import org.elfen.presentation.*;import java.nio.file.*;import java.util.*;import java.awt.image.*;import javax.imageio.ImageIO;
/** Read-only full-scene preview using the real DEMO sprite stream. Desktop only. */
public final class LocalizedDemoRender {
 static final class Candidate{double score;int tick;List<BattleView.Sprite> view;}
 public static void main(String[] args)throws Exception{
  Path root=Paths.get(args[0]),render=Paths.get(args[1]),out=Paths.get(args[2]);Files.createDirectories(out);BattleRenderCheck.root=root.resolve("game");
  try(java.util.stream.Stream<Path> files=Files.list(out)){for(Path f:(Iterable<Path>)files.filter(p->p.getFileName().toString().matches("(ja|en|ru)-.*\\.png"))::iterator)Files.delete(f);}
  LocaleCatalog catalog=new LocaleCatalog(Files.newInputStream(root.resolve("localization/ui.tsv")),Files.newInputStream(root.resolve("localization/scenes.tsv")));Map<String,Set<Integer>> wanted=new HashMap<>();for(List<LocaleCatalog.Block> b:catalog.allArt())wanted.computeIfAbsent(b.get(0).pack,k->new TreeSet<>()).add(b.get(0).image);
  Map<String,Candidate> scenes=new TreeMap<>();
  for(String id:new TreeSet<>(wanted.keySet())){Pack p=BattleRenderCheck.p(id);if(!p.kind.equals(".demo"))continue;DemoSession demo=new DemoSession(p);
   for(int tick=0;tick<4200;tick++){demo.step(new InputFrame(tick,0,0));List<BattleView.Sprite> view=demo.view();for(BattleView.Sprite s:view)if(s.packId.equals(id)&&wanted.get(id).contains(s.image)){
    int x=s.x/65536+s.offsetX,y=s.y/65536+s.offsetY,w=p.widths[s.image],h=p.heights[s.image];if(!s.background){x-=w/2;y-=h;}
    double score=(double)Math.max(0,Math.min(640,x+w)-Math.max(0,x))*Math.max(0,Math.min(480,y+h)-Math.max(0,y));
    score*=s.colour==4?Math.max(0,Math.min(32,32-(byte)s.rgba))/32.0:1;score*=(s.rgba&0xffffff00)==0?1:.6;
    String key=id+"-"+s.image;Candidate previous=scenes.get(key);if(score>0&&(previous==null||score>previous.score)){Candidate v=new Candidate();v.tick=tick;v.score=score;v.view=view;scenes.put(key,v);}
   }}
  }
  int count=0;ArrayList<String> frames=new ArrayList<>();
  for(String code:LocaleCatalog.CODES){BattleRenderCheck.cache.clear();if(!code.equals("ja"))try(java.util.stream.Stream<Path> files=Files.list(render.resolve(code))){for(Path f:(Iterable<Path>)files::iterator){String[] pair=f.getFileName().toString().replace(".png","").split("-");BattleRenderCheck.cache.put(pair[0]+"/"+String.format(Locale.ROOT,"%04d.png",Integer.parseInt(pair[1])),ImageIO.read(f.toFile()));}}
   for(Map.Entry<String,Candidate> entry:scenes.entrySet()){Candidate v=entry.getValue();BufferedImage pic=new BufferedImage(640,480,BufferedImage.TYPE_INT_RGB);for(BattleView.Sprite s:v.view)BattleRenderCheck.sprite(pic,null,s);String name=code+"-"+entry.getKey()+"-f"+v.tick+".png";ImageIO.write(pic,"png",out.resolve(name).toFile());count++;frames.add(name);}
  }Files.write(out.resolve("frames.txt"),frames);System.out.println("PASS "+count+" desktop scene frames from original DEMO VM; not Android screenshots");
 }
}
