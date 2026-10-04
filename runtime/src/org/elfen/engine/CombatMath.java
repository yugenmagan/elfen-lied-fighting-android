package org.elfen.engine;

import static org.elfen.engine.Pack.*;

/** Integer rules verified by executing the supplied EXE. No frame/FPS or UI state. */
public final class CombatMath {
    private CombatMath() {}
    /** Original signed 32-bit products and division toward zero. */
    public static int damage(int power,int combo,int correction,int hurtRate) {
        if(power==0)return 0;
        int scaled=Math.max(1,power-(power*combo*correction)/100);
        return Math.max(1,(scaled*hurtRate)/100);
    }
    public static int chip(int power,int percent){return power==0?0:Math.max(1,power*percent/100);}
    public static int lifeDelta(int hp,int maxHp,int delta,int threshold,int correction){
        if(delta<0&&hp<=maxHp*threshold/100)delta=-Math.max(1,(-delta)*correction/100);
        return delta;
    }
    public static final class Box {
        public final int left,top,right,bottom;
        Box(int l,int t,int r,int b){left=l;top=t;right=r;bottom=b;}
        public boolean overlaps(Box b){return right>b.left&&left<b.right&&bottom>b.top&&top<b.bottom;}
    }
    /** FA/FD coordinates are centre offsets and HALF extents, not top-left/size. */
    public static Box box(byte[] b,int x,int y,boolean facingLeft){
        int cx=x/65536+(facingLeft?-s16(b,1):s16(b,1)),cy=y/65536+s16(b,3);
        int w=s16(b,5),h=s16(b,7);return new Box(cx-w,cy-h,cx+w,cy+h);
    }
    /** Original CRT rand at 0x417a22. Deliberate uint32 overflow. */
    public static final class Rng {
        private int state;
        public Rng(int seed){state=seed;}
        public int next(){state=state*214013+2531011;return(state>>>16)&32767;}
        public int state(){return state;}
        public void restore(int value){state=value;}
    }
}
