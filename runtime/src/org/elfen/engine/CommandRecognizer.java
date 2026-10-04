package org.elfen.engine;
import java.io.*;
import static org.elfen.engine.Pack.*;
/** Original 0x410060; relative-at-capture history, frame-timed touch transitions. */
final class CommandRecognizer {
    private final int[] masks=new int[1024],times=new int[1024];private int count,frame;
    void applyFrame(InputFrame input,int player,boolean reverse){frame=input.frame;int[] virtual=input.virtualHistory(player);if(virtual!=null){count=1024;for(int age=0;age<1024;age++){masks[1023-age]=BattleSimulation.relative(virtual[age],reverse);times[1023-age]=frame-age;}return;}for(int mask:input.samples(player)){masks[count&1023]=BattleSimulation.relative(mask,reverse);times[count&1023]=frame;count++;}}
    int at(int age){return age<0||age>=Math.min(count,1024)?0:masks[(count-1-age)&1023];}
    private int time(int age){return age>=Math.min(count,1024)?frame-age:times[(count-1-age)&1023];}
    private static boolean direction(int expected,int mask,boolean reverse){mask=BattleSimulation.relative(mask,reverse)&15;switch(expected){case 0:return true;case 1:return mask==0;case 2:return mask==2;case 3:return mask==10;case 4:return mask==8;case 5:return mask==9;case 6:return mask==1;case 7:return mask==5;case 8:return mask==4;case 9:return mask==6;case 10:return (mask&1)!=0;case 11:return (mask&4)!=0;case 12:return (mask&2)!=0;case 13:return (mask&8)!=0;default:return false;}}
    boolean branch(byte[] b,boolean reverse){int step=4;while(step>=0&&(u(b,6+step*2)&32)==0)step--;if(step<0||u(b,4)==0)return false;for(int age=0;age<1024&&frame-time(age)<u(b,4);age++){int flags=u16(b,5+step*2),buttons=flags&0x3f0;if((flags&0xc000)==0&&direction(flags&15,at(age),reverse)&&(at(age)&buttons)==buttons&&(buttons==0||(at(age+1)&buttons)==0))if(--step<0)return true;}return false;}
    int match(Pack pack,boolean manualReverse,int stance,int start){
        for(int ci=start;ci<pack.commands.length;ci++){Pack.Command c=pack.commands[ci];if(c.time==0)continue;int step=9;while(step>=0&&(u(c.steps,step*2+1)&32)==0)step--;if(step<0)return -1;
            int remaining=c.amounts[step]*4,other=remaining,cw=-1,ccw=-1,mash=0;
            for(int age=0;age<1024&&frame-time(age)<c.time;age++){
                int flags=u16(c.steps,step*2),mode=flags>>>14,buttons=flags&0x3f0,mask=at(age);boolean matched=false;
                if(mode==0)matched=direction(flags&15,mask,manualReverse)&&(mask&buttons)==buttons&&(buttons==0||(at(age+1)&buttons)==0);
                else if(mode==1){if(mash==1){if((mask&0x3f0)==0)mash=0;}else if(direction(flags&15,mask,manualReverse)&&(mask&buttons)==buttons){mash=1;remaining-=4;matched=remaining<=0;}}
                else if(mode==2){int held=0,last=Integer.MAX_VALUE;for(int a=age;a<1024&&held<c.amounts[step];a++){int h=at(a);if(!direction(flags&15,h,manualReverse)||(h&buttons)!=buttons)break;if(time(a)!=last){held++;last=time(a);}}matched=held==c.amounts[step];}
                else{int dir=mask&15;if(cw<0&&ccw<0){remaining=other=c.amounts[step]*4;switch(dir){case 1:case 5:cw=ccw=3;break;case 2:case 10:cw=ccw=1;break;case 4:case 6:cw=ccw=0;break;case 8:case 9:cw=ccw=2;break;default:break;}}else{if(dir==new int[]{2,8,1,4}[cw&3]){cw++;remaining--;}if(dir==new int[]{1,4,2,8}[ccw&3]){ccw+=3;other--;}matched=remaining<2||other<2;}}
                if(matched){if(--step<0){if(c.time>29)for(int a=20;a<1024;a++)if(frame-time(a)>=20)masks[(count-1-a)&1023]=0;if(c.skills[stance]!=0)return ci;break;}remaining=c.amounts[step];}
            }
        }return -1;
    }
    void writeState(DataOutput o)throws IOException{o.writeInt(count);o.writeInt(frame);StateIO.writeInts(o,masks);StateIO.writeInts(o,times);}
    void readState(DataInput i)throws IOException{count=i.readInt();frame=i.readInt();if(count<0||frame<0)throw new IOException("Invalid command timeline");StateIO.readInts(i,masks);StateIO.readInts(i,times);}
}
