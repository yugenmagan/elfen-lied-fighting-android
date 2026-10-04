package org.elfen.engine;

import java.io.*;
import java.util.*;
import static org.elfen.engine.Pack.*;

/** Headless integer combat. One step consumes exactly one numbered InputFrame.
 * No Android, clock, audio sink or renderer is reachable from the simulation.
 * This is the combat research milestone; story/match scheduling is separate.
 */
public final class BattleSimulation implements Script.Host {
    public static final int TICK_HZ=60,ACTIVE=0,KO=1,RULESET_REVISION=2;
    static final int MAX_ENTITIES=1024,SNAPSHOT_VERSION=3;
    final Pack[] packs;final CombatRules rules;final String identity;
    Script[] entities=new Script[MAX_ENTITIES];
    int frame,phase=ACTIVE,winner=-1,koFrame=-1,cameraX=320,cameraY=480;
    final int[] globals=new int[16],combo=new int[2],redLife=new int[2],redDelay=new int[2],lastContact={-1,-1},lastAttacker={-1,-1};
    final CommandRecognizer[] commands={new CommandRecognizer(),new CommandRecognizer()};
    final CpuProgram[] cpuPrograms;
    final CpuProgram.State[] cpu={new CpuProgram.State(),new CpuProgram.State()};
    final int[] cachedX=new int[2],cachedY=new int[2],frozenInput=new int[2],frozenInputActive=new int[2],lastCommand=new int[2];
    private InputFrame resolvedInput;
    final int[][] objectSlots=new int[2][10];
    final CombatMath.Rng rng;
    private int emitter=-1;private boolean stepping;
    private final ArrayList<Event> events=new ArrayList<>();

    public BattleSimulation(Pack p1,Pack p2,Pack stage,Pack kgt,int seed){
        if(!p1.kind.equals(".player")||!p2.kind.equals(".player")||!stage.kind.equals(".stage"))throw new IllegalArgumentException("Battle pack types");
        packs=new Pack[]{p1,p2,stage};rules=new CombatRules(kgt);rng=new CombatMath.Rng(seed);cpuPrograms=new CpuProgram[]{new CpuProgram(p1),new CpuProgram(p2)};
        identity="ELF-COMBAT-"+RULESET_REVISION+":"+p1.hash+":"+p2.hash+":"+stage.hash+":"+kgt.hash;
        for(int[] slots:objectSlots)Arrays.fill(slots,-1);
        for(int i=0;i<2;i++){
            Script s=new Script(packs[i],this,globals,true);entities[i]=s;
            s.x=(i==0?390:890)<<16;s.y=s.ground;s.depth=80;s.facingLeft=i==1;s.stocks=i32(s.pack.settings,1778);
            s.start(s.pack.builtin(0),false);cachedX[i]=s.x;cachedY[i]=s.y;
        }
        for(int i=1;i<stage.starts.length;i++){
            if((stage.types[i]&3)==3||stage.starts[i]>=stage.end(i))continue;
            Script bg=new Script(stage,this,globals,false);bg.start(i,(stage.types[i]&32)==0);allocate(bg);
        }
    }
    public int frame(){return frame;}
    public String identity(){return identity;}
    public int phase(){return phase;}
    public int winner(){return winner;}
    public void setCpu(int player,int level){if(frame!=0)throw new IllegalStateException("CPU configuration before frame0");if(level<0||level>100)throw new IllegalArgumentException("CPU level");cpu[player].level=level;}
    public int cpuLevel(int player){return cpu[player].level;}
    public int life(int player){return entities[player].life;}
    public int activeObjects(){int n=0;for(int i=2;i<MAX_ENTITIES;i++)if(live(entities[i])&&entities[i].pack!=packs[2])n++;return n;}
    private static boolean live(Script s){return s!=null&&!s.ended;}
    int id(Script s){if(s==null)return -1;for(int i=0;i<MAX_ENTITIES;i++)if(entities[i]==s)return i;throw new IllegalStateException("Unregistered entity reference: pack="+s.pack.id+" skill="+s.skill+" pc="+s.pc+" ended="+s.ended+" objectFrame="+s.frame+" battleFrame="+frame);}
    int player(Script s){return s.controller==entities[0]?0:s.controller==entities[1]?1:-1;}
    int allocate(Script s){
        for(int i=2;i<MAX_ENTITIES;i++)if(!live(entities[i])){
            Script old=entities[i];clearObjectLinks(i);entities[i]=s;
            if(old!=null)for(Script e:entities)if(e!=null){if(e.parent==old)e.parent=s;if(e.controller==old)e.controller=s;}
            return i;
        }
        throw new IllegalStateException("Original 1024 entity limit exhausted");
    }
    // Original 40e4a0 clears numbered OO slots when an object ends. These IDs
    // must never refer to a different object after task-pool slot reuse.
    private void clearObjectLinks(int index){for(int[] slots:objectSlots)for(int n=0;n<slots.length;n++)if(slots[n]==index)slots[n]=-1;}
    private void retireObject(Script s){s.ended=true;clearObjectLinks(id(s));}
    public void step(InputFrame input){stepInternal(input,true);}
    void stepPresentation(InputFrame input){stepInternal(input,false);}
    void beginPresentation(){if(frame!=0)throw new IllegalStateException("Intro must precede frame0");for(Script s:new Script[]{entities[0],entities[1]})if(s.pack.builtin(17)>0)s.start(s.pack.builtin(17),false);}
    void endRound(int result){phase=KO;winner=result;if(koFrame<0)koFrame=frame;}
    private void stepInternal(InputFrame input,boolean fighting){
        if(input.frame!=frame)throw new IllegalArgumentException("Expected input frame "+frame+", got "+input.frame);
        if(stepping)throw new IllegalStateException("Reentrant simulation");stepping=true;events.clear();resolvedInput=input;
        try{
            for(int i=0;i<2;i++){entities[i].input=input.mask(i);commands[i].applyFrame(input,i,autoReverse(entities[i]));}
            // Original 0x404d4c traverses a depth list captured before script execution.
            ArrayList<Integer> order=new ArrayList<>();for(int i=0;i<MAX_ENTITIES;i++)if(live(entities[i]))order.add(i);
            Collections.sort(order,(a,b)->{int d=Integer.compare(entities[a].depth,entities[b].depth);return d!=0?d:Integer.compare(a,b);});
            for(int index:order){
                Script s=entities[index];if(!live(s))continue;emitter=index;
                if(!s.character&&player(s)>=0)s.input=s.controller.input;
                boolean reaction=s.pendingSkill!=0;
                if(reaction){int skill=s.pendingSkill,block=s.pendingBlock;s.pendingSkill=s.pendingBlock=0;s.start(skill,false);s.pc+=block;}
                else if(s.freeze==0){
                    if(s.character&&(fighting||phase==KO))control(index);
                    else if(player(s)>=0){
                        land(s);
                        if(!s.followParent&&(s.x<(-50<<16)||s.x>(1330<<16)||s.y<(-50<<16)||s.y>(1010<<16)))retireObject(s);
                        if(s.followParent&&s.parent!=null&&s.parent.ended)retireObject(s);
                    }
                }
                s.animationTick(reaction);
            }
            if(fighting){clashes();collide();}
            // 0x40f910 integrates entities in slot order, then resolves solid FD boxes.
            for(int i=0;i<MAX_ENTITIES;i++)if(live(entities[i])){
                Script s=entities[i];s.integrate();if(s.character)clampToScreen(s);
                pushBoxes(i);
            }
            if(fighting&&phase==ACTIVE&&(entities[0].life==0||entities[1].life==0)){
                phase=KO;winner=entities[0].life==entities[1].life?-1:entities[0].life>0?0:1;koFrame=frame;
                event("ko",null,winner,null);
            }
            cameraX=Math.max(0,Math.min(640,((entities[0].x/65536+entities[1].x/65536)/2+cameraX-320)/2));
            cameraY=Math.max(0,Math.min(480,((entities[0].y/65536+entities[1].y/65536)/2+cameraY-320)/2));
            frame++;
        }finally{emitter=-1;stepping=false;resolvedInput=null;}
    }
    private boolean land(Script s){
        if(s.y<s.ground||s.vy<=0)return false;
        int skill=s.triggers[0],block=s.triggerBlocks[0];s.y=s.ground;s.vx=s.vy=s.ax=s.ay=0;s.battleFlags&=~3;
        if(skill!=0){
            // 0x4118b4 / 0x412463: ON1 changes PC and clears FA/FD only.
            // R, other ON handlers and VM locals survive (Lucy's helicopter uses R).
            s.jump(skill,block);s.wait=0;s.triggers[0]=s.triggerBlocks[0]=0;
            Arrays.fill(s.hit,null);Arrays.fill(s.hurt,null);
            if(s.character)s.battleFlags=(s.battleFlags&~8)|4;
        }
        else{s.battleFlags&=~28;if(s.character)s.start(s.pack.builtin(0),false);}
        return true;
    }
    private void finishAction(int p){
        Script s=entities[p];s.battleFlags&=~28;combo[p]=0;lastAttacker[p]=-1;
        if(s.life==0){s.start(s.pack.builtin(19),false);s.battleFlags|=8;return;}
        if(phase==KO){resultAction(s,p);return;}
        if(s.y!=s.ground||s.vy!=0){s.battleFlags=(s.battleFlags&~3)|2;s.start(s.pack.builtin(6),false);}
        else{s.vx=s.ax=0;faceOpponent(s);s.battleFlags=(s.battleFlags&~3)|((s.input&Input.DOWN)!=0?1:0);s.start(s.pack.builtin((s.input&Input.DOWN)!=0?8:0),false);}
    }
    private void resultAction(Script s,int p){
        int skill=s.pack.builtin(winner==p?18:20);
        if(s.pack.end(skill)-s.pack.starts[skill]<=1){s.ended=false;s.wait=-1;s.battleFlags|=4;return;}
        s.start(skill,false);s.battleFlags|=4;
    }
    public boolean finished(Script s){if(!s.character){retireObject(s);return false;}finishAction(player(s));return s.wait!=-1;}
    private void control(int p){
        Script s=entities[p],other=entities[1-p];frozenInputActive[p]=0;
        if(cpu[p].level>0&&phase==ACTIVE){
            boolean manual=(i32(s.pack.settings,1766)&8)!=0;
            int[] virtual=cpuPrograms[p].produce(cpu[p],rng,cachedX[p],cachedY[p],s.ground,cachedX[1-p],cachedY[1-p],other.ground,manual&&cachedX[p]>cachedX[1-p]);
            if(!manual&&s.facingLeft)for(int i=0;i<virtual.length;i++)virtual[i]=relative(virtual[i],true);
            resolvedInput=resolvedInput.withCpuHistory(p,virtual);commands[p].applyFrame(resolvedInput,p,autoReverse(s));s.input=resolvedInput.mask(p);
        }
        land(s);cachedX[p]=s.x;cachedY[p]=s.y;
        if(redDelay[p]>0)redDelay[p]--;else if(redLife[p]>0)redLife[p]--;
        if(s.life==0)return;
        if(phase==KO){if((s.battleFlags&12)==0){resultAction(s,p);}return;}
        int recognized=eligibleForCommand(s)?recognize(s,0):-1;
        if(recognized>=0&&canCancel(s,s.pack.commands[recognized].skills[stance(s)])){
            // 0x410d50 does not restart the same active skill on another match.
            int target=s.pack.commands[recognized].skills[stance(s)];
            if(s.skill!=target||s.ended)s.start(target,false);s.battleFlags=(s.battleFlags&~24)|4;
            event("command",s.pack,recognized,null);return;
        }
        if((s.battleFlags&12)!=0)return;
        boolean turn=(i32(s.pack.settings,1766)&8)==0&&s.facingLeft!=(s.x>=other.x);
        if(turn){s.facingLeft=s.x>=other.x;select(s,(s.battleFlags&1)!=0?13:12);}
        int relative=relative(s.input,s.facingLeft),mode=s.battleFlags&3;
        if(mode==2)return;
        if(mode==1){
            if((relative&Input.DOWN)==0){s.battleFlags&=~3;select(s,9);}
            else if(s.skill!=s.pack.builtin(7)&&s.skill!=s.pack.builtin(13))select(s,8);
            return;
        }
        if(s.y<s.ground){s.battleFlags=(s.battleFlags&~3)|2;select(s,6);return;}
        if((relative&Input.DOWN)!=0){s.battleFlags=(s.battleFlags&~3)|1;select(s,7);return;}
        if((relative&Input.UP)!=0){select(s,(relative&Input.RIGHT)!=0?4:(relative&Input.LEFT)!=0?5:3);s.battleFlags=(s.battleFlags&~3)|2;return;}
        int next=(relative&Input.RIGHT)!=0?1:(relative&Input.LEFT)!=0?2:0;
        if(next!=0||(s.skill!=s.pack.builtin(9)&&s.skill!=s.pack.builtin(12)))select(s,next);
    }
    private boolean autoReverse(Script s){return s.facingLeft&&(i32(s.pack.settings,1766)&8)==0;}
    private void faceOpponent(Script s){if((i32(s.pack.settings,1766)&8)==0)s.facingLeft=s.x>=entities[1-player(s)].x;}
    private boolean manualReverse(Script s){return s.facingLeft&&(i32(s.pack.settings,1766)&8)!=0;}
    private int stance(Script s){int p=player(s);return (s.y!=s.ground||s.vy!=0||s.ay!=0)?0:(s.input&Input.DOWN)!=0?3:Math.abs((cachedX[p]-cachedX[1-p])/65536)<=s16(s.pack.settings,1747)?1:2;}
    private boolean eligibleForCommand(Script s){if((s.battleFlags&12)==0)return true;if((s.battleFlags&12)==8||s.cancel==null)return false;int from=u(s.cancel,1)&7;return from==2||from==1&&s.allowHitCancel>1;}
    private int recognize(Script s,int start){if(s.y<s.ground&&s.y+(50<<16)>s.ground&&s.vy>0)return -1;int p=player(s),ci=commands[p].match(s.pack,manualReverse(s),stance(s),start);if(ci>=0)lastCommand[p]=ci+1;return ci;}
    public void commandFallback(Script s,int block){int ci=recognize(s,lastCommand[player(s)]);int target=ci<0?0:s.pack.commands[ci].skills[stance(s)];s.skill=target;s.pc=s.pack.starts[target]+block;s.ended=false;}
    public boolean commandBranch(Script s,byte[] b){int p=player(s);return p>=0&&commands[p].branch(b,manualReverse(s));}
    public int stocks(Script s){int p=player(s);return p<0?s.stocks:entities[p].stocks;}
    public int life(Script s){int p=player(s);return p<0?s.life:entities[p].life;}
    public void stocks(Script s,int value){int p=player(s);if(p<0)throw s.fault("Stocks outside player");entities[p].stocks=value;if(value>=i32(s.pack.settings,1762))entities[p].special=0;}
    private void select(Script s,int builtin){int skill=s.pack.builtin(builtin);if(s.skill!=skill||s.ended)s.start(skill,false);}
    private boolean canCancel(Script s,int skill){
        if((s.battleFlags&12)==0)return true;
        if((s.battleFlags&12)==8||s.cancel==null)return false;
        int flags=u(s.cancel,1),from=flags&7;
        if(from!=2&&(from!=1||s.allowHitCancel<=1))return false;
        if(((flags>>>3)&7)==1)return u16(s.cancel,3)==skill;
        if(((flags>>>3)&7)!=0)return false;
        int kind=u(s.pack.code,s.pack.starts[skill]*16+2);return kind>=u(s.cancel,2)&&kind<=u(s.cancel,5);
    }
    static int relative(int input,boolean left){return left?((input&~3)|((input&1)<<1)|((input&2)>>>1)):input;}
    void clashes(){
        // 0x40eb60: FA versus FA, with descending box slots, before hurt contacts.
        for(int ai=0;ai<MAX_ENTITIES;ai++){
            Script a=entities[ai];if(!live(a)||player(a)<0)continue;
            for(int ab=19;ab>=0;ab--){byte[] fa=a.hit[ab];if(fa==null)continue;CombatMath.Box box=CombatMath.box(fa,a.x,a.y,a.facingLeft);
                for(int bi=ai+1;bi<MAX_ENTITIES;bi++){
                    Script b=entities[bi];if(!live(b)||player(b)<0||player(a)==player(b)||((a.collisionPlane^b.collisionPlane)&1)!=0||(b.battleFlags&8)!=0)continue;
                    for(int bb=19;bb>=0;bb--){byte[] fb=b.hit[bb];if(fb==null||!box.overlaps(CombatMath.box(fb,b.x,b.y,b.facingLeft)))continue;
                        if(u(fa,12)==0&&u(fb,12)==0){Arrays.fill(a.hit,null);Arrays.fill(b.hit,null);continue;}
                        boolean clear=a.triggers[4]!=0||b.triggers[4]!=0;trigger(a,4);trigger(b,4);
                        if(clear){Arrays.fill(a.hit,null);Arrays.fill(b.hit,null);event("clash",null,0,null);}
                        if((u(fa,12)==0||u(fb,12)==0)&&((u(fa,11)|u(fb,11))&128)!=0)throw a.fault("Mutable attack instruction is not present in audited game data");
                    }
                }
            }
        }
    }
    void collide(){
        for(int ai=0;ai<MAX_ENTITIES;ai++){
            Script a=entities[ai];if(!live(a)||player(a)<0||(a.collisionPlane&2)!=0||(a.battleFlags&16)!=0)continue;
            for(byte[] fa:a.hit)if(fa!=null){
                CombatMath.Box ab=CombatMath.box(fa,a.x,a.y,a.facingLeft);
                for(int vi=0;vi<MAX_ENTITIES;vi++){
                    Script v=entities[vi];if(!live(v)||player(v)<0||player(a)==player(v)||((a.collisionPlane^v.collisionPlane)&1)!=0||(v.collisionPlane&2)!=0)continue;
                    int flags=u(fa,10);boolean ground=v.y==v.ground&&v.vy==0;
                    if(v.character&&((ground&&(flags&16)!=0)||(!ground&&(flags&32)!=0)||((v.battleFlags&12)==12&&(flags&8)!=0)||((v.battleFlags&12)==8&&(flags&128)!=0)))continue;
                    for(byte[] fd:v.hurt)if(fd!=null&&(u(fd,10)&6)!=0&&ab.overlaps(CombatMath.box(fd,v.x,v.y,v.facingLeft))){
                        contact(a,v,fa,fd);break;
                    }
                }
            }
        }
    }
    private void trigger(Script s,int index){if(s.triggers[index]!=0){s.pendingSkill=s.triggers[index];s.pendingBlock=s.triggerBlocks[index];s.triggers[index]=s.triggerBlocks[index]=0;}}
    private void contact(Script a,Script v,byte[] fa,byte[] fd){
        int ap=player(a),vp=player(v),power=u(fa,12);a.battleFlags|=16;lastContact[ap]=id(v);lastContact[vp]=id(a);
        if(a.character)a.depth=81;if(v.character)v.depth=79;
        if(!v.character){trigger(a,2);return;}
        if(entities[ap].allowHitCancel!=0)entities[ap].allowHitCancel=2;
        int mask=relative(frozenInputActive[vp]!=0?frozenInput[vp]:v.input,v.facingLeft),guardFlags=i32(v.pack.settings,1766);
        boolean guard=false;
        if((v.battleFlags&12)==0){if(cpu[vp].level>0)guard=rng.next()%100<cpu[vp].level;
            else{guard=(guardFlags&8)!=0?(mask&(1<<(u(v.pack.settings,1753)+4)))!=0:(mask&Input.LEFT)!=0;if((guardFlags&1)!=0&&(mask&~Input.DOWN)==0)guard=true;}}
        if((v.battleFlags&12)==12)guard=true;
        if((u(fa,10)&64)!=0||((guardFlags&2)==0&&v.y<v.ground))guard=false;
        int stance=v.y<v.ground?2:(mask&Input.DOWN)!=0?1:0;
        int j=a.response==null?0:u16(a.response,1+2*(stance+(guard?3:0)));
        if(guard&&cpu[vp].level>0&&stance<2&&j!=0&&(rules.junctionFlags(j)&1)!=0)j=u16(a.response,stance==0?9:7);
        if(j!=0){
            if(j>=v.pack.junctions.length||j>=a.pack.junctions.length)throw a.fault("Hit junction outside player table "+j);
            v.pendingSkill=v.pack.junctions[j][0];v.pendingBlock=0;
            if((rules.junctionFlags(j)&1)!=0)guard=false;
            if(power!=0){spark(a,v,fa,fd,a.pack.junctions[j][1]);if(v.pendingSkill==0){event("reaction-error-1",a.pack,a.skill,fa);return;}}
        }else if(power!=0){
            // Verified original 0x40f657 -> 0x415190 -> 0x40f8bf.
            // Original Anna skill88 has zero guard R entries: report and skip
            // this contact's damage/reaction, retaining already-set contact flag.
            event("reaction-error-2",a.pack,a.skill,fa);return;
        }
        v.facingLeft=!a.facingLeft;
        if(power!=0){
            v.freeze=a.freeze=guard?rules.guardstop:rules.hitstop;
            if(guard){v.battleFlags|=12;if((u(fa,10)&4)!=0)changeLife(vp,-CombatMath.chip(power,u(a.pack.settings,1749)));}
            else{
                v.battleFlags=(v.battleFlags&~4)|8;
                meter(ap,s16(a.pack.settings,1774));meter(vp,s16(v.pack.settings,1776));
                changeLife(vp,-CombatMath.damage(power,combo[vp],u(a.pack.settings,1752),u(fd,11)));combo[vp]++;
            }
            event(guard?"guard":"hit",a.pack,vp,null);
        }
        trigger(a,guard?1:2);if(a.character||a.followParent)lastAttacker[vp]=id(a);
    }
    private void spark(Script a,Script v,byte[] fa,byte[] fd,int skill){
        if(skill==0)return;
        CombatMath.Box x=CombatMath.box(fa,a.x,a.y,a.facingLeft),y=CombatMath.box(fd,v.x,v.y,v.facingLeft);
        Script e=new Script(a.pack,this,globals,false);e.controller=a.controller;e.charVars=a.charVars;e.parent=a;
        e.x=(Math.max(x.left,y.left)+Math.min(x.right,y.right))<<15;e.y=(Math.max(x.top,y.top)+Math.min(x.bottom,y.bottom))<<15;
        e.depth=93;e.collisionPlane=a.collisionPlane;e.facingLeft=a.facingLeft;e.start(skill,false);allocate(e);
    }
    void changeLife(int p,int delta){
        Script s=entities[p];delta=CombatMath.lifeDelta(s.life,s.pack.life(),delta,u(s.pack.settings,1750),u(s.pack.settings,1751));
        s.life=Math.max(0,Math.min(s.pack.life(),s.life+delta));redLife[p]=Math.max(0,redLife[p]-delta);redDelay[p]=20;
        if(s.life==0){s.pendingSkill=s.pack.builtin(19);s.pendingBlock=0;}
    }
    void meter(int p,int delta){
        Script s=entities[p];int max=i32(s.pack.settings,1758),stocks=i32(s.pack.settings,1762);if(stocks==0)return;
        if(max<=0)throw s.fault("Nonpositive meter capacity");s.special+=delta;
        while(s.special<0){if(s.stocks>0){s.stocks--;s.special+=max;}else{s.special=0;break;}}
        while(s.special>=max){if(s.stocks>=stocks){s.special=0;break;}s.special-=max;s.stocks++;}
        if(s.stocks>=stocks){s.stocks=stocks;s.special=0;}
    }
    private void clampToScreen(Script s){int before=s.x;s.x=Math.max((cameraX+50)<<16,Math.min((cameraX+590)<<16,s.x));if(s.x!=before)trigger(s,3);}
    private void pushBoxes(int ai){
        Script a=entities[ai];if(player(a)<0)return;
        for(int h=19;h>=0;h--){byte[] af=a.hurt[h];if(af==null||(u(af,10)&1)==0)continue;
            CombatMath.Box ab=CombatMath.box(af,a.x,a.y,a.facingLeft);
            for(int bi=ai+1;bi<MAX_ENTITIES;bi++){
                Script b=entities[bi];if(!live(b)||player(b)<0||((a.collisionPlane^b.collisionPlane)&1)!=0)continue;
                for(int j=19;j>=0;j--){byte[] bf=b.hurt[j];if(bf==null||(u(bf,10)&1)==0)continue;
                    CombatMath.Box bb=CombatMath.box(bf,b.x,b.y,b.facingLeft);if(!ab.overlaps(bb))continue;
                    int ac=(ab.left+ab.right)/2,bc=(bb.left+bb.right)/2;
                    int shift=ac>bc?ab.left-bb.right:ab.right-bb.left;
                    boolean airA=a.y<a.ground,airB=b.y<b.ground;
                    if(a.life!=0&&(!airB||airA))a.x-=shift<<14;
                    if(b.life!=0&&(!airA||airB))b.x+=shift<<14;
                }
            }
        }
    }
    public void sound(Pack p,int sound){
        if(sound<0||sound>=p.sounds.length||sound>0&&p.sounds[sound].isEmpty())throw new IllegalStateException("Missing sound "+p.id+":"+sound);
        if(sound>0)event("sound",p,sound,null);
    }
    public void visual(Script s,byte[] b){event("visual",s.pack,0,b);}
    public int random(Script s){return rng.next();}
    public void cancel(Script s,byte[] b){int p=player(s);if(p<0)throw s.fault("Cancel outside player");entities[p].cancel=b;}
    public int variable(Script s,int index){switch(index){case 194:return cameraX;case 195:return cameraY;case 196:if(s.parent!=null)return s.parent.x/65536;break;case 197:if(s.parent!=null)return s.parent.y/65536;break;}throw s.fault("Unbound special variable "+index);}
    public boolean pause(Script s,byte[] b){
        int owner=player(s);if(owner<0)throw s.fault("PS outside player");
        for(int i=0;i<MAX_ENTITIES;i++){
            Script v=entities[i];if(!live(v))continue;int target=player(v);if(target<0||(!v.character&&!v.followParent))continue;
            int ticks=target==owner?(s.character?u(b,1):0):u(b,2);v.freeze+=ticks;if(v.character&&ticks!=0){frozenInput[target]=v.input;frozenInputActive[target]=1;}
        }
        return false;
    }
    public void gauge(Script owner,int a,int b,int c,int d){int p=player(owner);if(p<0)throw owner.fault("Gauge outside player");changeLife(p,a);meter(p,b);changeLife(1-p,c);meter(1-p,d);}
    public void spawn(Script owner,byte[] b){
        int skill=u16(b,2),block=u(b,4),flags=u(b,1),number=u(b,12),p=player(owner);
        if(p<0)throw owner.fault("Object controller outside player");if(number>=10)throw owner.fault("Object slot "+number);
        int old=objectSlots[p][number];
        if((flags&4)==0&&old>=0&&live(entities[old])){
            int branch=u16(b,5);if(branch>0){owner.jump(branch,u(b,7));return;}
            retireObject(entities[old]);
        }
        if(skill==0)return;
        Script s=new Script(owner.pack,this,globals,false);s.absolute=(flags&64)!=0;
        int x=s16(b,8)<<16,y=s16(b,10)<<16;
        s.x=(s.absolute?0:owner.x)+(s.absolute||!owner.facingLeft?x:-x);s.y=(s.absolute?0:owner.y)+y;
        s.controller=owner.controller;s.charVars=owner.charVars;s.parent=owner;s.followParent=(flags&32)!=0;s.parentX=x;s.parentY=y;
        s.facingLeft=owner.facingLeft;s.collisionPlane=owner.collisionPlane;s.depth=(flags&3)==2?u(b,13):(flags&3)==1?Math.min(127,owner.depth+1):Math.max(10,owner.depth-1);
        s.start(skill,false);s.pc+=block;int index=allocate(s);if((flags&4)==0)objectSlots[p][number]=index;
    }
    public void reposition(Script s,byte[] b){
        int p=player(s);if(!s.character||p<0)return;int ref=lastContact[p];if(ref<0||!live(entities[ref]))return;
        Script v=entities[ref];int f=u(b,1),x=s16(b,4)<<16;s.depth=(f&1)!=0?81:79;v.depth=(f&1)!=0?79:81;
        v.x=s.x+(s.facingLeft?-x:x);v.y=s.y+(s16(b,6)<<16);v.facingLeft=((f&4)!=0)==s.facingLeft;
        Arrays.fill(s.hit,null);Arrays.fill(s.hurt,null);
        if(v.character){int j=u(b,2);if(j>0){if(j>=v.pack.junctions.length)throw s.fault("RP junction "+j);v.pendingSkill=v.pack.junctions[j][0];v.pendingBlock=0;}v.freeze=0;v.battleFlags=(v.battleFlags&~5)|10;}
    }
    private void event(String kind,Pack pack,int value,byte[] instruction){events.add(new Event(frame,emitter,events.size(),kind,pack==null?"":pack.id,value,instruction));}
    public List<Event> events(){return Collections.unmodifiableList(new ArrayList<>(events));}
    public static final class Event {
        public final int frame,emitter,ordinal,value;public final String kind,packId;private final byte[] instruction;
        Event(int f,int e,int n,String k,String p,int v,byte[] b){frame=f;emitter=e;ordinal=n;kind=k;packId=p;value=v;instruction=b==null?null:b.clone();}
        public byte[] instruction(){return instruction==null?null:instruction.clone();}
    }
    public BattleView view(){return new BattleView(this);}
    public byte[] snapshot(){
        if(stepping)throw new IllegalStateException("Snapshots require a tick boundary");
        try{
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream o=new DataOutputStream(bytes);
            o.writeInt(0x45464253);o.writeInt(SNAPSHOT_VERSION);o.writeUTF(identity);
            StateIO.writeInts(o,new int[]{frame,phase,winner,koFrame,cameraX,cameraY,rng.state()});
            for(int[] a:new int[][]{globals,combo,redLife,redDelay,lastContact,lastAttacker,cachedX,cachedY,frozenInput,frozenInputActive,lastCommand})StateIO.writeInts(o,a);
            for(CpuProgram.State c:cpu)c.write(o);for(CommandRecognizer c:commands)c.writeState(o);for(int[] a:objectSlots)StateIO.writeInts(o,a);
            for(Script s:entities){o.writeBoolean(s!=null);if(s==null)continue;
                int pack=s.pack==packs[0]?0:s.pack==packs[1]?1:s.pack==packs[2]?2:-1;if(pack<0)throw new IOException("Unregistered pack");
                o.writeInt(pack);o.writeInt(id(s.parent));o.writeInt(id(s.controller));StateIO.writeInts(o,s.charVars);s.writeState(o);
            }
            o.flush();return bytes.toByteArray();
        }catch(IOException ex){throw new IllegalStateException("Cannot encode battle state",ex);}
    }
    public void restore(byte[] bytes){
        if(stepping)throw new IllegalStateException("Restore requires a tick boundary");
        // Parse into a second instance first: malformed input cannot partly mutate a live match.
        BattleSimulation tmp=new BattleSimulation(packs[0],packs[1],packs[2],rules,identity);
        tmp.decode(bytes);copyFrom(tmp);events.clear();
    }
    private BattleSimulation(Pack a,Pack b,Pack c,CombatRules r,String id){packs=new Pack[]{a,b,c};rules=r;identity=id;rng=new CombatMath.Rng(0);cpuPrograms=new CpuProgram[]{new CpuProgram(a),new CpuProgram(b)};}
    private void decode(byte[] bytes){
        if(bytes.length>8_000_000)throw new IllegalArgumentException("Snapshot exceeds limit");
        try(DataInputStream i=new DataInputStream(new ByteArrayInputStream(bytes))){
            if(i.readInt()!=0x45464253||i.readInt()!=SNAPSHOT_VERSION||!i.readUTF().equals(identity))throw new IOException("Snapshot version or source mismatch");
            frame=i.readInt();phase=i.readInt();winner=i.readInt();koFrame=i.readInt();cameraX=i.readInt();cameraY=i.readInt();rng.restore(i.readInt());
            for(int[] a:new int[][]{globals,combo,redLife,redDelay,lastContact,lastAttacker,cachedX,cachedY,frozenInput,frozenInputActive,lastCommand})StateIO.readInts(i,a);
            for(CpuProgram.State c:cpu)c.read(i);for(CommandRecognizer c:commands)c.readState(i);for(int[] a:objectSlots)StateIO.readInts(i,a);
            int[] parents=new int[MAX_ENTITIES],owners=new int[MAX_ENTITIES];
            for(int n=0;n<MAX_ENTITIES;n++)if(i.readBoolean()){
                int pack=i.readInt();if(pack<0||pack>=3)throw new IOException("Unknown pack reference");
                parents[n]=i.readInt();owners[n]=i.readInt();Script s=new Script(packs[pack],this,globals,n<2);entities[n]=s;StateIO.readInts(i,s.charVars);s.readState(i);
            }
            if(entities[0]==null||entities[1]==null||frame<0||phase<0||phase>KO||winner<-1||winner>1)throw new IOException("Invalid battle header");
            for(int n=0;n<MAX_ENTITIES;n++)if(entities[n]!=null){Script s=entities[n];s.parent=reference(parents[n]);s.controller=reference(owners[n]);if(s.controller==null)throw new IOException("Missing controller");
                if(s.controller!=s){if(!Arrays.equals(s.charVars,s.controller.charVars))throw new IOException("Inconsistent shared variables");s.charVars=s.controller.charVars;}}
            for(int[] a:new int[][]{lastContact,lastAttacker,objectSlots[0],objectSlots[1]})for(int ref:a)reference(ref);
            if(i.read()!=-1)throw new IOException("Trailing snapshot data");
        }catch(IOException e){throw new IllegalArgumentException("Invalid battle snapshot",e);}
    }
    private Script reference(int n)throws IOException{if(n==-1)return null;if(n<0||n>=MAX_ENTITIES||entities[n]==null)throw new IOException("Invalid entity reference "+n);return entities[n];}
    private void copyFrom(BattleSimulation s){
        frame=s.frame;phase=s.phase;winner=s.winner;koFrame=s.koFrame;cameraX=s.cameraX;cameraY=s.cameraY;rng.restore(s.rng.state());
        int[][] to={globals,combo,redLife,redDelay,lastContact,lastAttacker,cachedX,cachedY,frozenInput,frozenInputActive,lastCommand,objectSlots[0],objectSlots[1]},from={s.globals,s.combo,s.redLife,s.redDelay,s.lastContact,s.lastAttacker,s.cachedX,s.cachedY,s.frozenInput,s.frozenInputActive,s.lastCommand,s.objectSlots[0],s.objectSlots[1]};
        for(int j=0;j<to.length;j++)System.arraycopy(from[j],0,to[j],0,to[j].length);
        // VM host must be this instance, never the temporary decoder.
        for(int n=0;n<MAX_ENTITIES;n++){Script old=s.entities[n];if(old==null){entities[n]=null;continue;}Script neo=new Script(old.pack,this,globals,old.character);
            try{ByteArrayOutputStream b=new ByteArrayOutputStream();old.writeState(new DataOutputStream(b));neo.readState(new DataInputStream(new ByteArrayInputStream(b.toByteArray())));}catch(IOException ex){throw new AssertionError(ex);}neo.charVars=old.charVars.clone();entities[n]=neo;}
        for(int n=0;n<MAX_ENTITIES;n++)if(entities[n]!=null){Script old=s.entities[n],neo=entities[n];int par=s.id(old.parent),own=s.id(old.controller);neo.parent=par<0?null:entities[par];neo.controller=entities[own];if(neo.controller!=neo)neo.charVars=neo.controller.charVars;}
        for(int n=0;n<2;n++)try{ByteArrayOutputStream b=new ByteArrayOutputStream();s.commands[n].writeState(new DataOutputStream(b));commands[n].readState(new DataInputStream(new ByteArrayInputStream(b.toByteArray())));}catch(IOException ex){throw new AssertionError(ex);}
        for(int n=0;n<2;n++)try{ByteArrayOutputStream b=new ByteArrayOutputStream();s.cpu[n].write(new DataOutputStream(b));cpu[n].read(new DataInputStream(new ByteArrayInputStream(b.toByteArray())));}catch(IOException ex){throw new AssertionError(ex);}
    }
    public String stateHash(){return StateIO.hex(StateIO.hash(snapshot()));}
}
