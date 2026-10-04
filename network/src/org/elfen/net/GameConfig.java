package org.elfen.net;
import java.io.*;import java.nio.charset.StandardCharsets;import java.security.*;import java.util.*;
import org.elfen.engine.*;

/** Both peers construct their own match and validate its initial canonical hash. */
public final class GameConfig {
    public final String build,p1,p2,stage;public final int seed,wins,time;
    public GameConfig(String build,String p1,String p2,String stage,int seed,int wins,int time){
        if(!build.matches("[0-9a-f]{64}")||!p1.matches("[0-9]{4}")||!p2.matches("[0-9]{4}")||!stage.matches("[0-9]{4}")||wins<1||wins>9||time<0||time>999)throw new IllegalArgumentException("Room configuration");
        this.build=build;this.p1=p1;this.p2=p2;this.stage=stage;this.seed=seed;this.wins=wins;this.time=time;
    }
    public void write(DataOutput out)throws IOException{out.writeUTF(build);out.writeUTF(p1);out.writeUTF(p2);out.writeUTF(stage);out.writeInt(seed);out.writeInt(wins);out.writeInt(time);}
    public static GameConfig read(DataInputStream in)throws IOException{try{return new GameConfig(Protocol.digest(in),Protocol.packId(in),Protocol.packId(in),Protocol.packId(in),in.readInt(),in.readInt(),in.readInt());}catch(IllegalArgumentException e){throw new IOException("Room configuration",e);}}
    public MatchController create(Map<String,Pack> packs){if(!contentIdentity(packs).equals(build))throw new IllegalArgumentException("Несовместимые игровые данные");Pack k=require(packs,"0116",".kgt");return new MatchController(require(packs,p1,".player"),require(packs,p2,".player"),require(packs,stage,".stage"),k,seed,new MatchRules(k,wins,time,0,0));}
    private static Pack require(Map<String,Pack> packs,String id,String kind){Pack p=packs.get(id);if(p==null||!p.kind.equals(kind))throw new IllegalArgumentException("Invalid room pack "+id);return p;}
    public static String contentIdentity(Map<String,Pack> packs){try{
        MessageDigest hash=MessageDigest.getInstance("SHA-256");hash.update(("ELF-NET1:match1:combat"+BattleSimulation.RULESET_REVISION+"\n").getBytes(StandardCharsets.UTF_8));
        for(String id:new TreeSet<>(packs.keySet())){Pack p=packs.get(id);hash.update((id+":"+p.hash+"\n").getBytes(StandardCharsets.UTF_8));}
        StringBuilder result=new StringBuilder();for(byte b:hash.digest())result.append(String.format(Locale.ROOT,"%02x",b&255));return result.toString();
    }catch(NoSuchAlgorithmException e){throw new AssertionError(e);}}
}
