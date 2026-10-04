package org.elfen.engine;
import java.io.*;import java.util.*;

/** Versioned portable recording: asset-bound initial snapshot and exact inputs. */
public final class ReplayTape {
    private final byte[] initial;private final ArrayList<InputFrame> inputs=new ArrayList<>();private final int firstFrame;
    public ReplayTape(BattleSimulation battle){initial=battle.snapshot();firstFrame=battle.frame();}
    private ReplayTape(byte[] state,int frame){initial=state;firstFrame=frame;}
    public void append(InputFrame frame){if(frame.frame!=firstFrame+inputs.size())throw new IllegalArgumentException("Replay frame order");inputs.add(frame);}
    public int length(){return inputs.size();}
    public InputFrame input(int index){return inputs.get(index);}
    public void restoreStart(BattleSimulation battle){battle.restore(initial);if(battle.frame()!=firstFrame)throw new IllegalStateException("Replay frame mismatch");}
    public byte[] encode()throws IOException{
        ByteArrayOutputStream buffer=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(buffer);
        out.writeInt(0x45465250);out.writeInt(1);out.writeInt(firstFrame);out.writeInt(initial.length);out.write(initial);out.writeInt(inputs.size());for(InputFrame f:inputs)f.write(out);out.flush();
        byte[] payload=buffer.toByteArray();out.write(StateIO.hash(payload));out.flush();return buffer.toByteArray();
    }
    public static ReplayTape decode(byte[] bytes)throws IOException{
        if(bytes.length<52||bytes.length>32_000_000)throw new IOException("Replay size");
        byte[] payload=Arrays.copyOf(bytes,bytes.length-32),digest=Arrays.copyOfRange(bytes,bytes.length-32,bytes.length);if(!Arrays.equals(StateIO.hash(payload),digest))throw new IOException("Replay checksum");
        try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(payload))){
            if(in.readInt()!=0x45465250||in.readInt()!=1)throw new IOException("Replay format");int first=in.readInt();if(first<0)throw new IOException("Replay first frame");
            byte[] initial=new byte[StateIO.count(in,8_000_000)];in.readFully(initial);ReplayTape tape=new ReplayTape(initial,first);int count=StateIO.count(in,216_000);
            for(int n=0;n<count;n++){InputFrame f=InputFrame.read(in);if(f.frame!=first+n)throw new IOException("Replay frame order");tape.inputs.add(f);}if(in.read()!=-1)throw new IOException("Trailing replay data");return tape;
        }
    }
}
