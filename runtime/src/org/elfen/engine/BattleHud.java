package org.elfen.engine;

import java.util.*;
import static org.elfen.engine.Pack.*;

/** Original KGT/PLAYER HUD. A pure projection of battle state and simulation tick:
 * no simulation writes, RNG, audio, clock, Android APIs or persistent animation state.
 * The supplied HUD skills contain metadata, colour and image instructions only;
 * unsupported instructions fail with their pack/skill, never become placeholders.
 */
public final class BattleHud {
    private static final int BASE=0x435470;
    private final Pack kgt;
    private final Map<String,Pack> packs;
    private final Map<String,Animation> animations=new HashMap<>();
    public static final class Draw {
        public final BattleView.Sprite sprite;
        public final int clipLeft,clipTop,clipWidth,clipHeight;
        Draw(BattleView.Sprite s,int x,int y,int w,int h){sprite=s;clipLeft=x;clipTop=y;clipWidth=w;clipHeight=h;}
    }
    private static final class Frame {
        final int image,flags,x,y,options,colour,rgba,duration;
        Frame(byte[] code,int at,int colour,int rgba){
            flags=u16(code,at+3);image=flags&8191;x=s16(code,at+5);y=s16(code,at+7);options=u(code,at+9);
            duration=u16(code,at+1);this.colour=colour;this.rgba=rgba;
        }
    }
    private static final class Animation {
        final List<Frame> frames=new ArrayList<>();int ticks;
        Animation(Pack p,int skill){
            if(skill<0||skill>=p.starts.length-1)throw new IllegalArgumentException("HUD skill "+p.id+":"+skill);
            int colour=0,rgba=0;boolean forever=false;
            for(int i=p.starts[skill];i<p.end(skill);i++){
                int at=i*16,op=u(p.code,at);
                if(op==0)continue;
                if(op==35){colour=u(p.code,at+1);rgba=(u(p.code,at+2)<<24)|(u(p.code,at+3)<<16)|(u(p.code,at+4)<<8)|u(p.code,at+5);continue;}
                if(op!=12||forever)throw new IllegalStateException("Unsupported HUD instruction "+p.id+":"+skill+":"+i+" opcode="+op);
                Frame f=new Frame(p.code,at,colour,rgba);
                if(f.image>=p.widths.length||p.widths[f.image]<=0||p.heights[f.image]<=0)throw new IllegalStateException("Missing HUD image "+p.id+":"+f.image);
                frames.add(f);ticks+=f.duration;forever=f.duration==0;
            }
        }
        Frame at(int tick){
            if(frames.isEmpty())return null; // An explicitly empty original skill (e.g. Kraft portrait).
            int elapsed=Math.max(0,tick-1);Frame last=frames.get(frames.size()-1);
            if(last.duration!=0)elapsed%=ticks;
            for(Frame f:frames){if(f.duration==0||elapsed<f.duration)return f;elapsed-=f.duration;}
            return last;
        }
    }
    private static final Script.Host READ_ONLY=new Script.Host(){
        public void sound(Pack p,int i){throw new IllegalStateException("HUD cannot play sound");}
        public void spawn(Script s,byte[] b){throw s.fault("HUD cannot create battle objects");}
        public void gauge(Script s,int a,int b,int c,int d){throw s.fault("HUD cannot mutate battle");}
    };
    public BattleHud(Pack kgt,Map<String,Pack> packs){
        if(kgt==null||!".kgt".equals(kgt.kind)||kgt.originalTail.length<=0x445242-BASE)throw new IllegalArgumentException("Missing KGT HUD settings");
        this.kgt=kgt;this.packs=new HashMap<>(packs);
        for(int va=0x4451ec;va<=0x445234;va+=2)animation(kgt,skill(va));
        for(Pack p:packs.values())if(".player".equals(p.kind)){
            if(p.builtins.length<=22||p.settings.length!=1785)throw new IllegalArgumentException("Missing player HUD data "+p.id);
            animation(p,p.builtins[22]);
        }
    }
    private int skill(int address){return u16(kgt.originalTail,address-BASE);}
    private Animation animation(Pack p,int skill){String key=p.id+":"+skill;Animation a=animations.get(key);if(a==null){a=new Animation(p,skill);animations.put(key,a);}return a;}
    private int anchor(int index,int byteOffset){int s=skill(0x445236+index*2);return s16(kgt.code,kgt.starts[s]*16+byteOffset);}
    private int spacing(int index,int byteOffset,boolean signed){int s=skill(0x445236+index*2),at=kgt.starts[s]*16+byteOffset;return signed?kgt.code[at]:u(kgt.code,at);}
    private void add(List<Draw> out,Pack pack,int skill,int tick,int x,int y,boolean portrait,boolean left,int value,int max,boolean cropLeft){
        Frame f=animation(pack,skill).at(tick);if(f==null)return;
        Script s=new Script(pack,READ_ONLY,new int[16],false);s.absolute=true;s.image=f.image;s.imageFlags=f.flags;s.imageX=f.x;s.imageY=f.y;s.imageOptions=f.options;
        s.x=x<<16;s.y=y<<16;s.facingLeft=left;s.colour=f.colour;s.rgba=f.rgba;s.depth=101;
        BattleView.Sprite sprite=new BattleView.Sprite(2048+out.size(),s,!portrait);
        if(max<0){out.add(new Draw(sprite,0,0,640,480));return;}
        int width=pack.widths[f.image],remaining=filledPixels(width,value,max),removed=cropLeft?width-remaining:0;
        out.add(new Draw(sprite,x+f.x+removed,y+f.y,remaining,pack.heights[f.image]));
    }
    /** Native 40dd95..40de7d: truncating integer ratio, no image stretching. */
    public static int filledPixels(int width,int value,int maximum){
        if(width<0||value<0||maximum<0||value>maximum)throw new IllegalArgumentException("Invalid HUD gauge");
        return maximum==0?0:(int)((long)width*value/maximum);
    }
    public List<Draw> view(BattleView state,int seconds,int score1,int score2,int roundsToWin){
        if(state==null||state.fighters.size()!=2||roundsToWin<1||roundsToWin>9)throw new IllegalArgumentException("HUD battle/round state");
        ArrayList<Draw> out=new ArrayList<>();int tick=state.frame;
        // Original ten stage layout slots, including both life frames, timer and meter backgrounds.
        for(int va=0x44521a;va<0x44522e;va+=2)add(out,kgt,skill(va),tick,0,0,false,false,0,-1,false);
        for(int p=0;p<2;p++){
            BattleView.Fighter f=state.fighters.get(p);Pack player=packs.get(f.packId);
            if(player==null)throw new IllegalStateException("Missing HUD player "+f.packId);
            add(out,player,player.builtins[22],tick,anchor(1+p,1),anchor(1+p,3),true,p==1,0,-1,false);
            add(out,kgt,skill(0x44522e+p*2),tick,0,0,false,false,f.life,f.maxLife,p==0);
            add(out,kgt,skill(0x445232+p*2),tick,0,0,false,false,f.meter,i32(player.settings,1758),p==0);
            if(f.stocks<0||f.stocks>9)throw new IllegalStateException("HUD stock digit outside original table "+f.stocks);
            add(out,kgt,skill(0x445202+f.stocks*2),tick,anchor(3+p,1),anchor(3+p,3),false,false,0,-1,false);
            int wins=p==0?score1:score2;
            for(int n=0;n<roundsToWin;n++)add(out,kgt,skill(n<wins?0x445216:0x445218),tick,
                anchor(5+p,1)+n*spacing(5+p,5,true),anchor(5+p,3)+n*spacing(5+p,6,true),false,false,0,-1,false);
        }
        int x=anchor(0,1),y=anchor(0,3),gap=spacing(0,5,false);
        if(seconds<0)add(out,kgt,skill(0x4451ec),tick,x,y,false,false,0,-1,false);
        else{
            int digits=seconds>=100?3:seconds>=10?2:1;
            for(int n=0,value=seconds;n<digits;n++,value/=10){
                int offset=digits==1?0:digits==2?gap/2-n*gap:gap-n*gap;
                add(out,kgt,skill(0x4451ee+(value%10)*2),tick,x+offset,y,false,false,0,-1,false);
            }
        }
        return Collections.unmodifiableList(out);
    }
}
