package org.elfen.engine;

import java.util.*;

/** Detached immutable read model: render/audio cannot mutate a battle through it. */
public final class BattleView {
    public final int frame,phase,winner,cameraX,cameraY;
    public final List<Fighter> fighters;public final List<Sprite> sprites;
    BattleView(BattleSimulation sim){
        frame=sim.frame;phase=sim.phase;winner=sim.winner;cameraX=sim.cameraX;cameraY=sim.cameraY;
        ArrayList<Fighter> f=new ArrayList<>();for(int i=0;i<2;i++)f.add(new Fighter(sim.entities[i],sim.combo[i]));fighters=Collections.unmodifiableList(f);
        ArrayList<Sprite> s=new ArrayList<>();for(int i=0;i<sim.entities.length;i++){Script e=sim.entities[i];if(e!=null&&!e.ended&&e.image>=0)s.add(new Sprite(i,e,e.pack==sim.packs[2]));}
        Collections.sort(s,(a,b)->{int d=Integer.compare(a.depth,b.depth);return d!=0?d:Integer.compare(a.id,b.id);});sprites=Collections.unmodifiableList(s);
    }
    public static final class Fighter {
        public final String name,packId;public final int life,maxLife,meter,stocks,combo,x,y,skill,wait,hitstop,flags;
        Fighter(Script s,int c){name=s.pack.name;packId=s.pack.id;life=s.life;maxLife=s.pack.life();meter=s.special;stocks=s.stocks;combo=c;x=s.x;y=s.y;skill=s.skill;wait=s.wait;hitstop=s.freeze;flags=s.battleFlags;}
    }
    public static final class Sprite {
        public final String packId;public final int id,image,x,y,offsetX,offsetY,flags,options,depth,colour,rgba;public final boolean left,background,absolute;
        public final List<CombatMath.Box> hit,hurt;
        Sprite(int id,Script s,boolean bg){this.id=id;packId=s.pack.id;image=s.image;x=s.x;y=s.y;offsetX=s.imageX;offsetY=s.imageY;flags=s.imageFlags;options=s.imageOptions;depth=s.depth;colour=s.colour;rgba=s.rgba;left=s.facingLeft;background=bg;absolute=s.absolute;
            ArrayList<CombatMath.Box> a=new ArrayList<>(),b=new ArrayList<>();for(byte[] box:s.hit)if(box!=null)a.add(CombatMath.box(box,s.x,s.y,s.facingLeft));for(byte[] box:s.hurt)if(box!=null)b.add(CombatMath.box(box,s.x,s.y,s.facingLeft));hit=Collections.unmodifiableList(a);hurt=Collections.unmodifiableList(b);}
    }
}
