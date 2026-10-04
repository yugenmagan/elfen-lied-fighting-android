package org.elfen.engine;
import java.io.*;
import java.util.Arrays;
import static org.elfen.engine.Pack.*;

/** Original 111-byte CPU records and 0x411270 decision/history procedure. */
final class CpuProgram {
    private final byte[] data;private final Pack pack;
    private static final int[] DIRECTIONS={0,0,2,10,8,9,1,5,4,6,1,4,2,8,0,0};
    static final class State {
        int level,pattern,step,remaining,cooldown;
        void write(DataOutput o)throws IOException{StateIO.writeInts(o,new int[]{level,pattern,step,remaining,cooldown});}
        void read(DataInput i)throws IOException{level=i.readInt();pattern=i.readInt();step=i.readInt();remaining=i.readInt();cooldown=i.readInt();if(level<0||level>100||pattern<0||pattern>100||step< -1||step>10)throw new IOException("Invalid CPU state");}
    }
    CpuProgram(Pack p){
        pack=p;byte[] t=p.originalTail;int off=4,n=i32(t,off);off+=4+82*n;n=i32(t,off);off+=4+4*n;n=i32(t,off);off+=4+6*n+10;
        if(off+11100+48>t.length)throw new IllegalArgumentException("Truncated original CPU records");data=Arrays.copyOfRange(t,off,off+11100+48);
    }
    int[] produce(State s,CombatMath.Rng rng,int selfX,int selfY,int ground,int otherX,int otherY,int otherGround,boolean reverse){
        int[] out=new int[1024];
        if(s.pattern==0){
            if(s.level==0||--s.cooldown>0)return out;
            s.cooldown=rng.next()%(101-s.level)-s.level+50;int distance=Math.abs((selfX-otherX)/65536);
            for(int n=0;n<100;n++){int r=n*111,roll=rng.next()%100;
                if(roll>=u(data,r+33)||distance<u16(data,r+34)||distance>u16(data,r+36))continue;
                int flags=u(data,r+32);if(((flags&1)!=0)!=(selfY<ground)||((flags&2)!=0)!=(otherY<otherGround))continue;
                s.pattern=n+1;s.remaining=0;s.step=-1;
            }
        }
        if(s.pattern==0)return out;
        int r=(s.pattern-1)*111;
        if(--s.remaining<0){s.step++;int at=r+41+s.step*7;if(s.step>=10||(u(data,at+2)&32)==0)s.pattern=0;s.remaining=rng.next()%(101-s.level)+u16(data,at+5);}
        if(s.pattern==0)return out;
        int at=r+41+s.step*7,held=DIRECTIONS[u(data,at+1)&15],command=u16(data,at+3),head=0;
        if(command==0)out[0]=held;
        else{
            if(command>pack.commands.length)throw new IllegalStateException("CPU command outside table "+pack.id+":"+command);
            Pack.Command c=pack.commands[command-1];
            for(int n=9;n>=0;n--){int flags=u16(c.steps,n*2);if((flags&0x2000)==0)continue;int mask=DIRECTIONS[flags&15]|(flags&0x3f0)|held;
                switch(flags>>>14){case 0:out[head++&1023]=mask;break;
                case 1:for(int i=0;i<c.amounts[n];i++){out[head++&1023]=0;out[head++&1023]=mask;}break;
                case 2:for(int i=0;i<c.amounts[n];i++)out[head++&1023]=mask;break;
                case 3:for(int i=0;i<c.amounts[n];i++)for(int d:new int[]{2,8,1,4})out[head++&1023]=mask|d;break;}
            }
        }
        if(reverse)for(int i=0;i<out.length;i++)out[i]=BattleSimulation.relative(out[i],true);return out;
    }
}
