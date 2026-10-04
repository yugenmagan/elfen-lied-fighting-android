package org.elfen.engine;

import java.io.*;
import static org.elfen.engine.Pack.*;

/** Input history; original command order, directions, charge lengths, and time fields.
 * Permissive direction transitions still require differential validation against EXE.
 */
public final class Commands {
    private final int[] history=new int[1024],times=new int[1024];private int frame=0,count=0,edges=0,previous=0;private boolean directionChanged=false;
    public void push(int mask){frame++;note(mask);}
    public void pushFrame(int number,int[] samples){
        if(number<0)throw new IllegalArgumentException("Input frame");
        frame=number;for(int mask:samples)note(mask);
    }
    public void note(int mask){edges|=mask&~previous;directionChanged|=(mask&15)!=(previous&15);previous=mask;history[count&1023]=mask;times[count++&1023]=frame;}
    public void clear(){java.util.Arrays.fill(history,0);frame=count=edges=previous=0;directionChanged=false;}
    private int at(int age){return age<0||age>=Math.min(count,1024)?0:history[(count-1-age)&1023];}
    private int time(int age){return times[(count-1-age)&1023];}
    void writeState(DataOutput out)throws IOException{
        StateIO.writeInts(out,history);StateIO.writeInts(out,times);
        out.writeInt(frame);out.writeInt(count);out.writeInt(edges);out.writeInt(previous);out.writeBoolean(directionChanged);
    }
    void readState(DataInput in)throws IOException{
        StateIO.readInts(in,history);StateIO.readInts(in,times);
        frame=in.readInt();count=in.readInt();edges=in.readInt();previous=in.readInt();directionChanged=in.readBoolean();
        if(count<0||frame<0)throw new IOException("Invalid command history");
    }
    public int match(Pack p,boolean left,int stance){
        if(count<1)return -1;int pressed=edges;boolean changed=directionChanged;edges=0;directionChanged=false;
        for(int ci=0;ci<p.commands.length;ci++){
            Pack.Command c=p.commands[ci];if(c.skills[stance]==0)continue;
            int last=-1;for(int i=0;i<10;i++)if((u(c.steps,2*i+1)&32)!=0)last=i;
            if(last<0)continue;
            int f1=u(c.steps,last*2),f2=u(c.steps,last*2+1),buttons=(f1&240)|((f2&3)<<8);
            int now=at(0);
            if(buttons!=0&&(now&buttons&pressed)==0)continue;
            if(buttons==0&&!changed)continue;
            int age=0;boolean ok=true;
            for(int step=last;step>=0;step--){
                f1=u(c.steps,step*2);f2=u(c.steps,step*2+1);if((f2&32)==0)continue;
                int dir=f1&15,btn=(f1&240)|((f2&3)<<8);
                int deadline=step==last?frame:(age>0?time(age-1):frame)-Math.max(1,c.time);
                boolean found=false;
                for(;age<Math.min(count,1023)&&time(age)>=deadline;age++){
                    int mask=at(age);
                    if(!Input.matchesDirection(dir,Input.direction(mask,left))||(mask&btn)!=btn)continue;
                    if((f2&128)!=0){int held=0;while(age+held<count&&age+held<1023&&Input.matchesDirection(dir,Input.direction(at(age+held),left))&&(at(age+held)&btn)==btn)held++;
                        if(held==0||time(age)-time(age+held-1)+1<c.amounts[step])continue;age+=held-1;}
                    found=true;age++;break;
                }
                if(!found){ok=false;break;}
            }
            if(ok)return ci;
        }
        return -1;
    }
}
