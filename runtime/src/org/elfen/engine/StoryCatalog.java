package org.elfen.engine;
import java.io.*;import java.nio.charset.*;import java.util.*;
/** Checks the derived index against original CP932 KGT tables and pack hashes. */
public final class StoryCatalog {
    public final Pack kgt;private final Pack[] characters=new Pack[51],stages=new Pack[51],demos=new Pack[101];
    public StoryCatalog(Pack kgt,Map<String,Pack> packs,InputStream index)throws IOException{
        this.kgt=kgt;
        try(BufferedReader in=new BufferedReader(new InputStreamReader(index,StandardCharsets.UTF_8))){
            if(!("EFSI1\t"+kgt.hash).equals(in.readLine()))throw new IOException("Story index/KGT mismatch");String line;
            while((line=in.readLine())!=null){String[] v=line.split("\t",-1);if(v.length!=5)throw new IOException("Story index row");Pack[] table=table(v[0]);int n=Integer.parseInt(v[1]);
                Pack p=packs.get(v[2]);if(n<=0||n>=table.length||table[n]!=null||p==null||!p.kind.equals(v[0])||!p.hash.equals(v[3]))throw new IOException("Story reference "+line);
                int offset=v[0].equals(".player")?4:v[0].equals(".stage")?4+50*256+200*36+8:4+100*256+200*36+8;offset+=(n-1)*256;
                int end=offset;while(end<offset+256&&kgt.originalTail[end]!=0)end++;
                String name=new String(kgt.originalTail,offset,end-offset,Charset.forName("windows-31j"))+v[0];if(!name.equals(v[4]))throw new IOException("CP932 reference mismatch: "+name);table[n]=p;
            }
        }
        for(String kind:new String[]{".player",".stage",".demo"}){
            Pack[] t=table(kind);int base=kind.equals(".player")?4:kind.equals(".stage")?4+50*256+200*36+8:4+100*256+200*36+8;
            for(int n=1;n<t.length;n++)if((kgt.originalTail[base+(n-1)*256]!=0)!=(t[n]!=null))throw new IOException("Missing story table reference "+kind+":"+n);
        }
        for(Pack p:characters)if(p!=null){StoryProgram s=new StoryProgram(p);for(int n=0;n<100;n++){StoryProgram.Event e=s.event(n);if(e.type==1&&e.value(1)!=0){stage(e.value(1));character(e.enemy());}else if(e.type==2&&e.value(1)!=0)demo(e.value(1));}}
    }
    private Pack[] table(String kind){switch(kind){case ".player":return characters;case ".stage":return stages;case ".demo":return demos;default:throw new IllegalArgumentException("Story reference kind "+kind);}}
    private Pack get(Pack[] table,int index){if(index<=0||index>=table.length||table[index]==null)throw new IllegalArgumentException("Unresolved story reference "+index);return table[index];}
    public Pack character(int n){return get(characters,n);}public Pack stage(int n){return get(stages,n);}public Pack demo(int n){return get(demos,n);}
    public List<Pack> protagonists(){ArrayList<Pack> r=new ArrayList<>();for(Pack p:characters)if(p!=null&&new StoryProgram(p).available())r.add(p);return Collections.unmodifiableList(r);}
    public Pack screen(int n){if(n<0||n>=6)throw new IllegalArgumentException("Screen selector");return demo(Pack.u(kgt.originalTail,4+100*256+200*36+8+100*256+n));}
}
