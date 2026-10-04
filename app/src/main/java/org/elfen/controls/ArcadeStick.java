package org.elfen.controls;

import org.elfen.engine.Input;

/** Touch adapter only: a captured pointer drives an eight-way arcade gate.
 * Floating point is confined to UI geometry; the combat engine receives bits.
 */
public final class ArcadeStick {
    public static final float TRAVEL=.58f, CAP=.36f, ACQUIRE=1.45f;
    private int pointer=-1,mask;
    private float centreX,centreY,radius=1,dead=.18f,offsetX,offsetY;

    public void configure(float x,float y,float r,float deadZone){
        if(!Float.isFinite(x)||!Float.isFinite(y)||!Float.isFinite(r)||!Float.isFinite(deadZone)||r<=0||deadZone<0||deadZone>=1)
            throw new IllegalArgumentException("Invalid stick geometry");
        centreX=x;centreY=y;radius=r;dead=deadZone;
    }
    public boolean contains(float x,float y){return Math.hypot(x-centreX,y-centreY)<=radius*ACQUIRE;}
    public boolean begin(int id,float x,float y){
        if(id<0||pointer>=0||!contains(x,y))return false;
        pointer=id;move(id,x,y);return true;
    }
    public boolean move(int id,float x,float y){
        if(id!=pointer||pointer<0)return false;
        if(!Float.isFinite(x)||!Float.isFinite(y))throw new IllegalArgumentException("Invalid touch coordinates");
        float dx=x-centreX,dy=y-centreY,travel=radius*TRAVEL;
        double length=Math.hypot(dx,dy);
        float scale=length>travel?(float)(travel/length):1;
        offsetX=dx*scale;offsetY=dy*scale;
        mask=length<=travel*dead?0:Input.dpad(dx,dy,travel,dead);
        return true;
    }
    public boolean end(int id){if(id!=pointer||pointer<0)return false;cancel();return true;}
    public void cancel(){pointer=-1;mask=0;offsetX=offsetY=0;}
    public boolean active(){return pointer>=0;}
    public int pointer(){return pointer;}
    public int mask(){return mask;}
    public float offsetX(){return offsetX;}
    public float offsetY(){return offsetY;}
}
