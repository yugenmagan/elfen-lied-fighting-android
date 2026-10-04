package org.elfen.engine;
import java.io.*;
/** Immutable frame-numbered physical input; all input sources enter here. */
public final class InputFrame {
    public final int frame;private final int[][] samples;private final int[][] history=new int[2][];
    public InputFrame(int frame,int p1,int p2){this(frame,new int[]{p1},new int[]{p2});}
    public InputFrame(int frame,int[] p1,int[] p2){if(frame<0)throw new IllegalArgumentException("Negative input frame");this.frame=frame;samples=new int[][]{valid(p1),valid(p2)};}
    private static int[] valid(int[] a){if(a==null||a.length<1||a.length>256)throw new IllegalArgumentException("Input samples count");for(int v:a)if((v&~1023)!=0)throw new IllegalArgumentException("Invalid input mask");return a.clone();}
    public int mask(int p){int[] a=samples[p];return a[a.length-1];}
    public int[] samples(int p){return samples[p].clone();}
    InputFrame withCpuHistory(int p,int[] newest){if(newest.length!=1024)throw new IllegalArgumentException("CPU history length");InputFrame n=new InputFrame(frame,samples[0],samples[1]);for(int j=0;j<2;j++)n.history[j]=history[j]==null?null:history[j].clone();n.history[p]=newest.clone();n.samples[p]=new int[]{newest[0]};return n;}
    int[] virtualHistory(int p){return history[p]==null?null:history[p].clone();}
    void write(DataOutput o)throws IOException{o.writeInt(frame);for(int[] a:samples){o.writeInt(a.length);StateIO.writeInts(o,a);}for(int[] a:history){o.writeBoolean(a!=null);if(a!=null)StateIO.writeInts(o,a);}}
    static InputFrame read(DataInput i)throws IOException{int f=i.readInt();int[][] a=new int[2][];for(int p=0;p<2;p++){int n=StateIO.count(i,256);if(n==0)throw new IOException("Empty input");a[p]=new int[n];StateIO.readInts(i,a[p]);}try{InputFrame out=new InputFrame(f,a[0],a[1]);for(int p=0;p<2;p++)if(i.readBoolean()){int[] h=new int[1024];StateIO.readInts(i,h);for(int v:h)if((v&~1023)!=0)throw new IOException("Invalid CPU mask");out.history[p]=h;}return out;}catch(IllegalArgumentException e){throw new IOException("Corrupt input",e);}}
}
