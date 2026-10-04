import org.elfen.engine.*;import java.nio.file.*;import java.awt.image.*;import javax.imageio.*;import java.util.*;
/** Desktop VM renders, explicitly not Android screenshots. */
public final class SelectionRenderCheck {
    public static void main(String[] args)throws Exception{
        Path root=Paths.get(args[0]),out=Paths.get(args[1]);Files.createDirectories(out);BattleRenderCheck.root=root;Map<String,Pack> packs=new HashMap<>();
        try(java.util.stream.Stream<Path> paths=Files.list(root)){for(Path p:(Iterable<Path>)paths.filter(Files::isDirectory)::iterator){Pack k=Pack.read(p.getFileName().toString(),Files.newInputStream(p.resolve("data.efp")));packs.put(k.id,k);}}
        StoryCatalog c=new StoryCatalog(packs.get("0116"),packs,Files.newInputStream(root.resolve("story-index.tsv")));CharacterSelect s=new CharacterSelect(c,false,"0170","0104");
        for(int cell=0;cell<s.size();cell++){
            s.focus(0,cell);s.focus(1,cell);
            for(int f=0;f<180;f++){
                s.step(new InputFrame(s.frame(),0,0));
                for(BattleView.Sprite v:s.view()){Pack p=packs.get(v.packId);if(p.widths[v.image]!=0&&p.heights[v.image]!=0&&!Files.isRegularFile(root.resolve(v.packId+"/"+String.format(Locale.ROOT,"%04d.png",v.image))))throw new AssertionError("Missing selection image "+v.packId+":"+v.image);}
                if(f==100)render(s,out.resolve(s.selected(0).id+"-both-desktop.png"));
            }
        }
        s=new CharacterSelect(c,false,"0170","0104");for(int f=0;f<100;f++)s.step(new InputFrame(s.frame(),0,0));render(s,out.resolve("lucy-nana-desktop.png"));
        System.out.println("PASS 10 portraits in both positions; 180 ticks each; all image references; 11 desktop VM renders (not Android screenshots)");
    }
    static void render(CharacterSelect s,Path file)throws Exception{BufferedImage image=new BufferedImage(640,480,BufferedImage.TYPE_INT_RGB);for(BattleView.Sprite v:s.view())BattleRenderCheck.sprite(image,null,v);ImageIO.write(image,"png",file.toFile());}
}
