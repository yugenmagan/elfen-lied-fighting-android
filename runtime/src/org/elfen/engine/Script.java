package org.elfen.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.io.*;
import static org.elfen.engine.Pack.*;

/** Native data interpreter. Animation/input milestone; collision scheduling is separate.
 * Every used instruction has an explicit representation; unsupported effects fail visibly.
 * See FORMAT_NOTES for verified x86 addresses and provisional parts.
 */
public final class Script {
    public interface Host {
        void sound(Pack pack,int index);
        void spawn(Script owner,byte[] instruction);
        void gauge(Script owner,int selfLife,int selfSpecial,int enemyLife,int enemySpecial);
        default void visual(Script owner,byte[] b){throw owner.fault("Visual instruction needs host");}
        default void reposition(Script owner,byte[] b){throw owner.fault("RP instruction needs host");}
        default int random(Script owner){throw owner.fault("RNG instruction needs host");}
        default boolean pause(Script owner,byte[] b){owner.freeze=u(b,1);return true;}
        default int variable(Script owner,int index){throw owner.fault("Variable "+index+" needs host binding");}
        default void variable(Script owner,int index,int value){throw owner.fault("Variable "+index+" needs host binding");}
        default boolean finished(Script owner){return false;}
        default boolean explicitEnd(Script owner){return false;}
        default void cancel(Script owner,byte[] b){owner.cancel=b;}
        default int stocks(Script owner){return owner.stocks;}
        default void stocks(Script owner,int value){owner.stocks=value;}
        default int life(Script owner){return owner.life;}
        default void commandFallback(Script owner,int block){owner.ended=true;}
        default boolean commandBranch(Script owner,byte[] b){return false;}
    }
    public final Pack pack;public final Host host;
    public int skill,root,pc,image=-1,imageX,imageY,imageFlags,imageOptions;
    public int x,y,vx,vy,ax,ay,ground=920<<16,wait=0,freeze,depth;
    public boolean facingLeft,ended,loop,character,absolute,followParent;
    public Script parent,controller;public int parentX,parentY;
    public int input,life,special,stocks=0,frame,colour,rgba,trailCount,trailTime;
    public final int[] vars=new int[16];public int[] charVars=new int[16],systemVars;
    public final int[] triggers=new int[6],triggerBlocks=new int[6];
    public final byte[][] hit=new byte[20][],hurt=new byte[20][];
    public byte[] response,cancel,afterimage,commandBranch;
    private int subCount,subSkill,subBlock,returnSkill,returnPC,callSkill=-1,callPC;
    public int timing=100,velocityScale=655,accelerationScale=393;
    // Original task flags and deferred reaction, included in every battle snapshot.
    int battleFlags,collisionPlane,pendingSkill,pendingBlock,allowHitCancel;

    public Script(Pack p,Host h,int[] globals,boolean character){this.pack=p;host=h;systemVars=globals;this.character=character;life=p.life();controller=this;depth=50;}
    public void start(int index,boolean loop){
        if(index<0||index>=pack.starts.length)throw fault("Skill outside table: "+index);
        if(character&&y==ground&&vy==0){vx=vy=ax=ay=0;}
        this.loop=loop;root=index;skill=index;pc=pack.starts[index];wait=0;ended=false;image=-1;
        subCount=0;callSkill=-1;Arrays.fill(hit,null);Arrays.fill(hurt,null);Arrays.fill(triggers,0);Arrays.fill(triggerBlocks,0);response=null;cancel=null;commandBranch=null;allowHitCancel=0;
    }
    public void jump(int index,int block){
        if(index==0)return;if(index<0||index>=pack.starts.length)throw fault("Bad branch skill "+index);
        skill=index;pc=pack.starts[index]+block;
        if(pc>pack.end(index))throw fault("Branch outside skill "+index+":"+block);
    }
    public IllegalStateException fault(String s){return new IllegalStateException(pack.id+" "+pack.name+" skill="+skill+" pc="+pc+": "+s);}
    public void tick(){
        frame++;if(ended)return;if(freeze>0){freeze--;return;}
        integrate();
        if(character&&y>=ground&&vy>0){y=ground;vy=ay=0;int landing=triggers[0],block=triggerBlocks[0];if(landing>0){start(landing,false);pc+=block;}}
        instructions();
    }
    /** Battle scheduler runs VM, collision and integration as separate phases. */
    void animationTick(boolean reaction){
        frame++;if(ended)return;
        if(!reaction&&freeze!=0){if(freeze>0)freeze--;return;}
        instructions();
    }
    void integrate(){
        // Original 0x40f96a: acceleration -> velocity -> position, then attachment.
        // Integer overflow is intentional: the original uses 32-bit x86 ADD.
        if(freeze==0){vx+=ax;x+=vx;vy+=ay;y+=vy;}
        if(followParent&&parent!=null){x=parent.x+(parent.facingLeft?-parentX:parentX);y=parent.y+parentY;}
    }
    private void instructions(){
        if(wait<0)return;wait-=100;if(wait>=0)return;
        // 0x412582: rearm on image-step expiry, including looped images.
        for(byte[] box:hit)if(box!=null&&(u(box,10)&2)!=0)battleFlags&=~16;
        for(int budget=0;budget<300;budget++){
            if(pc>=pack.end(skill)){
                if(callSkill>=0){skill=callSkill;pc=callPC;callSkill=-1;continue;}
                if(subCount>0){if(--subCount>0)jump(subSkill,subBlock);else{skill=returnSkill;pc=returnPC;}continue;}
                if(loop&&image>=0){skill=root;pc=pack.starts[root];}else{ended=true;if(!host.finished(this))return;}
            }
            if(pc<0||pc>=pack.code.length/16)throw fault("PC outside code");
            byte[] b=Arrays.copyOfRange(pack.code,pc*16,(pc+1)*16);pc++;
            int op=u(b,0),f=u(b,1);
            switch(op){
                case 0:break; // skill metadata, not a timed instruction
                case 1:{int flags=u(b,9),dir=facingLeft?-1:1;boolean add=(flags&1)!=0;
                    if((flags&2)==0)vx=(add?vx:0)+s16(b,3)*velocityScale*dir;
                    if((flags&4)==0)vy=(add?vy:0)+s16(b,5)*velocityScale;
                    if((flags&8)==0)ax=(add?ax:0)+s16(b,1)*accelerationScale*dir;
                    if((flags&16)==0)ay=(add?ay:0)+s16(b,7)*accelerationScale;break;}
                case 2:if(f>0&&f<=6){triggers[f-1]=u16(b,2);triggerBlocks[f-1]=u(b,4);}break;
                case 3:host.sound(pack,u16(b,2));break;
                case 4:host.spawn(this,b);break;
                case 5:case 41:
                    if(op==5&&host.explicitEnd(this)){ended=true;return;}
                    if(callSkill>=0){skill=callSkill;pc=callPC;callSkill=-1;break;}
                    if(subCount>0){if(--subCount>0)jump(subSkill,subBlock);else{skill=returnSkill;pc=returnPC;}break;}
                    if(loop&&image>=0){skill=root;pc=pack.starts[root];break;}ended=true;if(host.finished(this))break;return;
                case 9:
                    if(f>0&&u16(b,2)>0){subCount=f;subSkill=u16(b,2);subBlock=u(b,4);returnSkill=skill;returnPC=pc;jump(subSkill,subBlock);}break;
                case 10:jump(u16(b,1),u(b,3));break;
                case 11:if(u16(b,1)>0){callSkill=skill;callPC=pc;jump(u16(b,1),u(b,3));}break;
                case 12:
                    imageFlags=u16(b,3);image=imageFlags&8191;imageX=s16(b,5);imageY=s16(b,7);imageOptions=u(b,9);
                    if(image>=pack.widths.length)throw fault("Image outside table");
                    wait=u16(b,1)==0?-1:wait+u16(b,1)*timing;
                    return; // Original I handler always ends this interpreter pass.
                case 14:host.visual(this,b);break;
                case 16:{int threshold=u(b,6),stock=host.stocks(this);boolean consume=(u(b,5)&1)!=0,take=consume?stock<=threshold:stock>=threshold;
                    if(take){if(u16(b,2)==0){if(consume)host.commandFallback(this,u(b,4));}else jump(u16(b,2),u(b,4));}
                    else if(consume){int max=pack.settings.length==1785?i32(pack.settings,1762):0;host.stocks(this,Math.max(0,Math.min(max,stock+b[7])));}if(ended)return;break;}
                case 17:if((u(b,5)&1)!=0?host.life(this)<=u16(b,6):host.life(this)>=u16(b,6)){if(u16(b,2)==0){if((u(b,5)&1)!=0)host.commandFallback(this,u(b,4));}else jump(u16(b,2),u(b,4));}break;
                case 20:host.reposition(this,b);break;
                case 21:host.gauge(this,s16(b,2),s16(b,4),s16(b,6),s16(b,8));break;
                case 22:{int condition=u(b,7);boolean match;
                    switch(condition){case 1:match=y>=ground;break;case 2:match=y>=ground&&(input&Input.DOWN)==0;break;
                        case 3:match=y>=ground&&(input&Input.DOWN)!=0;break;case 4:match=(input&(facingLeft?Input.LEFT:Input.RIGHT))!=0;break;
                        case 5:match=(input&(facingLeft?Input.RIGHT:Input.LEFT))!=0;break;case 6:match=(input&Input.UP)!=0;break;
                        case 7:match=(input&Input.DOWN)!=0;break;case 8:match=(input&15)==0;break;default:match=false;}
                    if((f&2)!=0)match=false;if(match==((f&1)==0))jump(u16(b,2),u(b,4));break;}
                case 23:response=b;break;
                case 24:case 25:{int n=u(b,9);if(n>=20)throw fault("Box slot "+n);(op==24?hit:hurt)[n]=(u16(b,5)==0||u16(b,7)==0)?null:b;
                    if(op==24&&hit[n]!=null){if((u(b,10)&2)!=0)battleFlags&=~16;if(character&&(u(b,10)&1)!=0)allowHitCancel=1;}break;}
                case 26:if(host.pause(this,b))return;break;
                case 30:host.cancel(this,b);break;
                case 31:variable(b);break;
                case 32:if(host.random(this)%(u16(b,1)+1)>u16(b,3))jump(u16(b,6),u(b,8));break;
                case 35:colour=f;rgba=(u(b,2)<<24)|(u(b,3)<<16)|(u(b,4)<<8)|u(b,5);break;
                case 36:commandBranch=b;if(host.commandBranch(this,b))jump(u16(b,1),u(b,3));break;
                case 37:afterimage=b;trailCount=u(b,3);trailTime=u(b,4);break;
                default:throw fault("Opcode "+op+" is not yet implemented in this milestone");
            }
        }
        throw fault("300-instruction budget exceeded without wait");
    }
    private int getVar(int index){
        if(index<16)return vars[index];if(index>=64&&index<80)return charVars[index-64];if(index>=128&&index<144)return systemVars[index-128];
        if(index==192)return x/65536;if(index==193)return y/65536;
        return host.variable(this,index);
    }
    private void setVar(int index,int value){
        if(index<16)vars[index]=value;else if(index>=64&&index<80)charVars[index-64]=value;
        else if(index>=128&&index<144)systemVars[index-128]=value;
        else if(index==192)x=value<<16;else if(index==193)y=value<<16;else host.variable(this,index,value);
    }
    private void variable(byte[] b){
        int index=u(b,4),flags=u(b,5),n=(flags&128)!=0?getVar(u(b,6)):s16(b,7);
        int v=getVar(index);if((flags&3)==1)v=(short)n;else if((flags&3)==2)v=Math.max(-30000,Math.min(30000,v+(short)n));setVar(index,v);
        int compare=s16(b,9);int test=flags&12;
        if((test==4&&v==compare)||(test==8&&v>compare)||(test==12&&v<compare))jump(u16(b,1),u(b,3));
    }
    // References and shared charVars/systemVars are encoded by BattleSimulation;
    // this codec covers every VM-local scalar, loop/call and instruction pointer.
    void writeState(DataOutput o)throws IOException{
        StateIO.writeInts(o,new int[]{skill,root,pc,image,imageX,imageY,imageFlags,imageOptions,x,y,vx,vy,ax,ay,ground,wait,freeze,depth,
            parentX,parentY,input,life,special,stocks,frame,colour,rgba,trailCount,trailTime,
            subCount,subSkill,subBlock,returnSkill,returnPC,callSkill,callPC,timing,velocityScale,accelerationScale,
            battleFlags,collisionPlane,pendingSkill,pendingBlock,allowHitCancel});
        for(boolean v:new boolean[]{facingLeft,ended,loop,character,absolute,followParent})o.writeBoolean(v);
        StateIO.writeInts(o,vars);StateIO.writeInts(o,triggers);StateIO.writeInts(o,triggerBlocks);
        for(byte[] b:hit)StateIO.instruction(o,b);for(byte[] b:hurt)StateIO.instruction(o,b);
        StateIO.instruction(o,response);StateIO.instruction(o,cancel);StateIO.instruction(o,afterimage);StateIO.instruction(o,commandBranch);
    }
    void readState(DataInput i)throws IOException{
        skill=i.readInt();root=i.readInt();pc=i.readInt();image=i.readInt();imageX=i.readInt();imageY=i.readInt();imageFlags=i.readInt();imageOptions=i.readInt();
        x=i.readInt();y=i.readInt();vx=i.readInt();vy=i.readInt();ax=i.readInt();ay=i.readInt();ground=i.readInt();wait=i.readInt();freeze=i.readInt();depth=i.readInt();
        parentX=i.readInt();parentY=i.readInt();input=i.readInt();life=i.readInt();special=i.readInt();stocks=i.readInt();frame=i.readInt();colour=i.readInt();rgba=i.readInt();trailCount=i.readInt();trailTime=i.readInt();
        subCount=i.readInt();subSkill=i.readInt();subBlock=i.readInt();returnSkill=i.readInt();returnPC=i.readInt();callSkill=i.readInt();callPC=i.readInt();timing=i.readInt();velocityScale=i.readInt();accelerationScale=i.readInt();
        battleFlags=i.readInt();collisionPlane=i.readInt();pendingSkill=i.readInt();pendingBlock=i.readInt();allowHitCancel=i.readInt();
        facingLeft=i.readBoolean();ended=i.readBoolean();loop=i.readBoolean();character=i.readBoolean();absolute=i.readBoolean();followParent=i.readBoolean();
        StateIO.readInts(i,vars);StateIO.readInts(i,triggers);StateIO.readInts(i,triggerBlocks);
        for(int n=0;n<20;n++)hit[n]=StateIO.instruction(i);for(int n=0;n<20;n++)hurt[n]=StateIO.instruction(i);
        response=StateIO.instruction(i);cancel=StateIO.instruction(i);afterimage=StateIO.instruction(i);commandBranch=StateIO.instruction(i);
        if(skill<0||skill>=pack.starts.length||root<0||root>=pack.starts.length||pc<0||pc>pack.code.length/16||image<-1||image>=pack.widths.length)
            throw new IOException("Invalid VM snapshot for "+pack.id);
    }
}
