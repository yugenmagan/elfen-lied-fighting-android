package org.elfen.presentation;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Presentation-only localisation. Never participates in battle snapshots or input. */
public final class LocaleCatalog {
    public static final String[] CODES={"ja","en","ru"};
    private final Map<String,String[]> ui=new LinkedHashMap<>();
    private final Map<String,List<Block>> art=new LinkedHashMap<>();
    private final List<String> fragments=new ArrayList<>();
    private int language;
    public static final class Block {
        public final String pack,mode,colour,background,align;
        public final int image,x,y,width,height,font;
        private final String[] text;
        Block(String[] v){pack=v[0];image=Integer.parseInt(v[1]);mode=v[2];x=Integer.parseInt(v[3]);y=Integer.parseInt(v[4]);width=Integer.parseInt(v[5]);height=Integer.parseInt(v[6]);font=Integer.parseInt(v[7]);colour=v[8];background=v[9];align=v[10];text=new String[]{v[11],v[12],v[13]};
            if(x<0||y<0||width<1||height<1||font<1||!Arrays.asList("replace","card","overlay").contains(mode))throw new IllegalArgumentException("Invalid translation region "+pack+":"+image);
        }
        public String text(int language){return text[language];}
    }
    public LocaleCatalog(InputStream uiFile,InputStream sceneFile)throws IOException{
        for(String[] row:read(uiFile,"ELF-UI-1",4)){
            if(ui.put(row[0],new String[]{row[1],row[2],row[3]})!=null)throw new IOException("Duplicate UI translation "+row[0]);
            if(!row[0].startsWith("pack:")&&!row[0].startsWith("command:"))fragments.add(row[0]);
        }
        fragments.sort((a,b)->Integer.compare(b.length(),a.length()));
        for(String[] row:read(sceneFile,"ELF-SCENES-1",14)){Block b=new Block(row);art.computeIfAbsent(b.pack+":"+b.image,k->new ArrayList<>()).add(b);}
        for(Map.Entry<String,List<Block>> e:art.entrySet())e.setValue(Collections.unmodifiableList(e.getValue()));
    }
    private static List<String[]> read(InputStream file,String magic,int columns)throws IOException{
        ArrayList<String[]> rows=new ArrayList<>();try(BufferedReader r=new BufferedReader(new InputStreamReader(file,StandardCharsets.UTF_8))){
            if(!magic.equals(r.readLine()))throw new IOException("Translation format "+magic);String s;
            while((s=r.readLine())!=null){String[] v=s.split("\t",-1);if(v.length!=columns)throw new IOException("Translation column count");for(int n=0;n<v.length;n++)v[n]=unescape(v[n]);for(String t:v)if(t.isEmpty())throw new IOException("Empty translation field");rows.add(v);}
        }return rows;
    }
    private static String unescape(String s)throws IOException{
        StringBuilder b=new StringBuilder();for(int n=0;n<s.length();n++){char c=s.charAt(n);if(c=='\\'){if(++n==s.length())throw new IOException("Translation escape");c=s.charAt(n);if(c=='n')c='\n';else if(c=='t')c='\t';else if(c!='\\')throw new IOException("Translation escape");}b.append(c);}return b.toString();
    }
    public void language(String code){for(int i=0;i<CODES.length;i++)if(CODES[i].equals(code)){language=i;return;}throw new IllegalArgumentException("Unknown language "+code);}
    public int index(){return language;}public String code(){return CODES[language];}
    public String text(String key){String[] t=ui.get(key);if(t==null)throw new IllegalArgumentException("Missing translation: "+key);return t[language];}
    public String move(String name){return name.isEmpty()?"":text("command:"+name);}
    /** Diagnostic/status fragments supplied by the existing runtime, outside simulation. */
    public String dynamic(String source){
        if(language==2)return source;String direct[]=ui.get(source);if(direct!=null)return direct[language];
        StringBuilder result=new StringBuilder();for(int n=0;n<source.length();){String matched=null;for(String k:fragments)if(source.startsWith(k,n)){matched=k;break;}if(matched==null)result.append(source.charAt(n++));else{result.append(ui.get(matched)[language]);n+=matched.length();}}return result.toString();
    }
    /** Null preserves every original Japanese pixel and original Latin title art. */
    public List<Block> sprite(String pack,int image){if(language==0)return null;List<Block> b=art.get(pack+":"+image);if(b==null)return null;boolean differs=false;for(Block v:b)differs|=!v.text(language).equals(v.text(0));return differs?b:null;}
    public Collection<List<Block>> allArt(){return Collections.unmodifiableCollection(art.values());}
    public Set<String> keys(){return Collections.unmodifiableSet(ui.keySet());}
}
