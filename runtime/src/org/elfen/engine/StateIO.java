package org.elfen.engine;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Explicit, big-endian snapshot encoding, identical on JVM and Android. */
final class StateIO {
    private StateIO(){}
    static int count(DataInput in,int max)throws IOException{int n=in.readInt();if(n<0||n>max)throw new IOException("Invalid state count "+n);return n;}
    static void writeInts(DataOutput out,int[] a)throws IOException{for(int n:a)out.writeInt(n);}
    static void readInts(DataInput in,int[] a)throws IOException{for(int i=0;i<a.length;i++)a[i]=in.readInt();}
    static void instruction(DataOutput out,byte[] b)throws IOException{out.writeBoolean(b!=null);if(b!=null){if(b.length!=16)throw new IOException("Instruction length");out.write(b);}}
    static byte[] instruction(DataInput in)throws IOException{if(!in.readBoolean())return null;byte[] b=new byte[16];in.readFully(b);return b;}
    static byte[] hash(byte[] data){try{return MessageDigest.getInstance("SHA-256").digest(data);}catch(NoSuchAlgorithmException e){throw new AssertionError(e);}}
    static String hex(byte[] bytes){StringBuilder b=new StringBuilder(bytes.length*2);for(byte v:bytes){b.append(Character.forDigit((v>>>4)&15,16));b.append(Character.forDigit(v&15,16));}return b.toString();}
}
