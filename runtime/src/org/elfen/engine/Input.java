package org.elfen.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

/** One mask per pointer/source, ORed atomically. Releasing one never releases another. */
public final class Input {
    public static final int LEFT=1,RIGHT=2,UP=4,DOWN=8,A=16,B=32,C=64,D=128,E=256,F=512;
    private final Map<Integer,Integer> sources=new HashMap<>();
    private final ArrayList<Integer> transitions=new ArrayList<>();private int pendingPress=0;
    public synchronized void set(int source,int mask){int before=mask();if(mask==0)sources.remove(source);else sources.put(source,mask);int after=mask();if(before!=after){pendingPress|=after&~before;transitions.add(after);}}
    public synchronized void clear(){sources.clear();transitions.clear();pendingPress=0;}
    public synchronized int mask(){int v=0;for(int n:sources.values())v|=n;return v;}
    public synchronized int sample(){int v=mask()|(pendingPress&0x3f0);pendingPress=0;return v;}
    public synchronized int[] drain(){int[] a=new int[transitions.size()];for(int i=0;i<a.length;i++)a[i]=transitions.get(i);transitions.clear();return a;}
    public static int direction(int mask,boolean facingLeft){
        boolean l=(mask&LEFT)!=0,r=(mask&RIGHT)!=0,u=(mask&UP)!=0,d=(mask&DOWN)!=0;
        int x=(r?1:0)-(l?1:0),y=(d?1:0)-(u?1:0);if(facingLeft)x=-x;
        if(x==0)return y==0?1:y>0?4:8;
        return x>0?(y==0?2:y>0?3:9):(y==0?6:y>0?5:7);
    }
    public static boolean matchesDirection(int wanted,int actual){
        switch(wanted){
            case 0:return true;case 10:return actual==5||actual==6||actual==7;
            case 11:return actual==7||actual==8||actual==9;
            case 12:return actual==9||actual==2||actual==3;
            case 13:return actual==3||actual==4||actual==5;
            default:return wanted==actual;
        }
    }
    /** Rolling D-pad with independent axes: diagonal sectors include both cardinal bits. */
    public static int dpad(float dx,float dy,float radius,float dead){
        double len=Math.sqrt(dx*dx+dy*dy);if(len<radius*dead)return 0;
        double nx=dx/len,ny=dy/len;int v=0;
        if(nx>.382683)v|=RIGHT;if(nx<-.382683)v|=LEFT;
        if(ny>.382683)v|=DOWN;if(ny<-.382683)v|=UP;return v;
    }
}
