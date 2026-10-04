package org.elfen.engine;

import java.io.*;
import java.nio.charset.StandardCharsets;

/** Strict portable pack reader. Original bytecode and type-specific tail are retained. */
public final class Pack {
    public String id, name, kind, hash;
    public String[] skillNames, sounds;
    public int[] starts, types, widths, heights, soundFlags, builtins;
    public byte[] code, settings, originalTail;
    public Command[] commands;
    public int[][] junctions;
    public int bgm, demoTime, skipInput;
    public static final class Command {
        public String name; public int time; public int[] skills, amounts; public byte[] steps;
    }
    private static int count(DataInputStream in, int limit) throws IOException {
        int n=in.readInt(); if(n<0||n>limit)throw new IOException("Invalid pack count: "+n);return n;
    }
    private static byte[] blob(DataInputStream in) throws IOException {
        byte[] a=new byte[count(in,16000000)];in.readFully(a);return a;
    }
    private static String text(DataInputStream in) throws IOException {return new String(blob(in),StandardCharsets.UTF_8);}
    private static int[] ints(DataInputStream in) throws IOException {
        int[] a=new int[count(in,100000)];for(int i=0;i<a.length;i++)a[i]=in.readInt();return a;
    }
    public static Pack read(String id, InputStream source) throws IOException {
        try(DataInputStream in=new DataInputStream(new BufferedInputStream(source))){
            if(in.readInt()!=0x45465031)throw new IOException("EFP1 signature missing");
            Pack p=new Pack();p.id=id;p.name=text(in);p.kind=text(in);p.hash=text(in);
            int n=count(in,65536);p.starts=new int[n];p.types=new int[n];p.skillNames=new String[n];
            for(int i=0;i<n;i++){p.skillNames[i]=text(in);p.starts[i]=in.readInt();p.types[i]=in.readInt();}
            p.code=blob(in);if(p.code.length%16!=0)throw new IOException("Unaligned bytecode");
            for(int start:p.starts)if(start<0||start>p.code.length/16)throw new IOException("Skill outside code");
            n=count(in,8192);p.widths=new int[n];p.heights=new int[n];
            for(int i=0;i<n;i++){p.widths[i]=in.readInt();p.heights[i]=in.readInt();}
            n=count(in,10000);p.sounds=new String[n];p.soundFlags=new int[n];
            for(int i=0;i<n;i++){p.sounds[i]=text(in);p.soundFlags[i]=in.readInt();}
            p.builtins=ints(in);n=count(in,10000);p.commands=new Command[n];
            for(int i=0;i<n;i++){Command c=new Command();c.name=text(in);c.time=in.readInt();c.skills=ints(in);c.steps=blob(in);c.amounts=ints(in);p.commands[i]=c;}
            n=count(in,10000);p.junctions=new int[n][2];
            for(int[] h:p.junctions){h[0]=in.readInt();h[1]=in.readInt();}
            p.settings=blob(in);p.bgm=in.readInt();p.demoTime=in.readInt();p.skipInput=in.readInt();p.originalTail=blob(in);
            if(in.read()!=-1)throw new IOException("Unexpected trailing EFP data");return p;
        }
    }
    public int end(int skill){return skill+1<starts.length?starts[skill+1]:code.length/16;}
    public int builtin(int n){return n<builtins.length?builtins[n]:0;}
    public static int u(byte[] a,int p){return a[p]&255;}
    public static int u16(byte[] a,int p){return u(a,p)|(u(a,p+1)<<8);}
    public static int s16(byte[] a,int p){return (short)u16(a,p);}
    public static int i32(byte[] a,int p){return u16(a,p)|(u16(a,p+2)<<16);}
    public int life(){return settings.length==1785?i32(settings,1754):0;}
}
