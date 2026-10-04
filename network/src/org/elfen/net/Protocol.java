package org.elfen.net;
import java.io.*;
import org.elfen.engine.InputFrame;

/** Small bounded binary protocol over a reliable TLS stream. No game objects on wire. */
public final class Protocol {
    private Protocol(){}
    public static final int VERSION=1,MAX_PACKET=4096,MAX_FRAME=216000;
    public static final int CREATE=1,JOIN=2,READY=3,INPUT=4,HASH=5,LEAVE=6,PING=7;
    public static final int CREATED=11,CONFIG=12,START=13,REMOTE_INPUT=14,REMOTE_HASH=15,CLOSED=16,ERROR=17,PONG=18;
    public interface Body {void write(DataOutputStream out)throws IOException;}
    public static final class Packet {
        public final int type;private final byte[] payload;
        private Packet(int type,byte[] payload){this.type=type;this.payload=payload.clone();}
        public DataInputStream data(){return new DataInputStream(new ByteArrayInputStream(payload));}
    }
    public static Packet packet(int type,Body body){try{ByteArrayOutputStream b=new ByteArrayOutputStream();body.write(new DataOutputStream(b));if(b.size()+1>MAX_PACKET)throw new IllegalArgumentException("Packet limit");return new Packet(type,b.toByteArray());}catch(IOException e){throw new IllegalArgumentException(e);}}
    public static Packet empty(int type){return packet(type,o->{});}
    public static Packet message(int type,String s){return packet(type,o->o.writeUTF(s));}
    public static void write(OutputStream stream,Packet packet)throws IOException{DataOutputStream out=new DataOutputStream(stream);out.writeInt(packet.payload.length+1);out.writeByte(packet.type);out.write(packet.payload);out.flush();}
    public static Packet read(InputStream stream)throws IOException{DataInputStream in=new DataInputStream(stream);int n=in.readInt();if(n<1||n>MAX_PACKET)throw new IOException("Packet size");int type=in.readUnsignedByte();byte[] payload=new byte[n-1];in.readFully(payload);return new Packet(type,payload);}
    public static void end(DataInputStream in)throws IOException{if(in.available()!=0)throw new IOException("Trailing packet fields");}
    public static String text(DataInputStream in,int limit)throws IOException{String s=in.readUTF();if(s.length()>limit)throw new IOException("String length");return s;}
    public static String digest(DataInputStream in)throws IOException{String s=text(in,64);if(!s.matches("[0-9a-f]{64}"))throw new IOException("Digest");return s;}
    public static String packId(DataInputStream in)throws IOException{String s=text(in,4);if(!s.matches("[0-9]{4}"))throw new IOException("Pack ID");return s;}
    public static int frame(DataInputStream in)throws IOException{int f=in.readInt();if(f<0||f>MAX_FRAME)throw new IOException("Frame number");return f;}
    public static void samples(DataOutput out,int[] values)throws IOException{new InputFrame(0,values,new int[]{0});out.writeShort(values.length);for(int n:values)out.writeShort(n);}
    public static int[] samples(DataInputStream in)throws IOException{int n=in.readUnsignedShort();if(n<1||n>256)throw new IOException("Input samples");int[] values=new int[n];for(int j=0;j<n;j++){values[j]=in.readUnsignedShort();if((values[j]&~1023)!=0)throw new IOException("Input mask");}return values;}
    public static Packet input(int type,int frame,int[] values){return packet(type,o->{o.writeInt(frame);samples(o,values);});}
    public static Packet hash(int type,int frame,String hash){return packet(type,o->{o.writeInt(frame);o.writeUTF(hash);});}
}
