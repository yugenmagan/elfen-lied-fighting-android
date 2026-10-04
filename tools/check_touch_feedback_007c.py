#!/usr/bin/env python3
"""Run the real onTouchEvent method with JVM event/View fakes, vs unchanged 007b.
Checks callback dispatch/input preservation; does not claim Android/device vibration.
"""
from pathlib import Path
import subprocess
ROOT=Path(__file__).resolve().parents[1]
def method(text,marker):
    start=text.index(marker);p=text.index('{',start);depth=1;i=p+1
    while depth:
        if text[i]=='{':depth+=1
        elif text[i]=='}':depth-=1
        i+=1
    return text[start:i]
def main():
    source=(ROOT/'app/src/main/java/org/elfen/fighting/MainActivity.java').read_text()
    old=(ROOT/'tests/fixtures/touch_handler_007b.txt').read_text()
    actual=method(source,'public boolean onTouchEvent(MotionEvent e)')
    layout='\n'.join(method(source,s) for s in ['void layoutControls()','float buttonX(int i)','float buttonY(int i)'])
    java=r'''
import java.util.*;import org.elfen.engine.Input;import org.elfen.controls.*;
public final class TouchFeedbackCheck {
    static final class HapticFeedbackConstants {static final int VIRTUAL_KEY=1;}
    static final class MotionEvent {
        static final int ACTION_DOWN=0,ACTION_UP=1,ACTION_MOVE=2,ACTION_CANCEL=3,ACTION_POINTER_DOWN=5,ACTION_POINTER_UP=6;
        final int action,index;final int[] ids;final float[] xy;
        MotionEvent(int action,int index,int[] ids,float...xy){this.action=action;this.index=index;this.ids=ids;this.xy=xy;}
        int getActionMasked(){return action;}int getActionIndex(){return index;}int getPointerCount(){return ids.length;}int getPointerId(int n){return ids[n];}
        float getX(int n){return xy[n*2];}float getY(int n){return xy[n*2+1];}int getHistorySize(){return 0;}float getHistoricalX(int n,int h){return getX(n);}float getHistoricalY(int n,int h){return getY(n);}
    }
    static final class Preferences {
        final Map<String,Float> values=new HashMap<>();float getFloat(String k,float d){return values.containsKey(k)?values.get(k):k.equals("size")?Float.parseFloat(System.getProperty("size","1")):d;}boolean getBoolean(String k,boolean d){return k.equals("mirror")?Boolean.getBoolean("mirror"):d;}
        Preferences edit(){return this;}Preferences putFloat(String k,float v){values.put(k,v);return this;}void apply(){}
    }
    abstract static class Surface {
        boolean dialog,resumed=true,editing;Object selection;int maxPointers,haptics,pauses;int active=0x70;
        CombatLayout controlLayout;float dx,dy,bx,by,radius,buttonRadius;final Input input=new Input();final ArcadeStick stick=new ArcadeStick();final PauseGate pauseGate=new PauseGate();final SessionFlow flow=new SessionFlow();
        final HashMap<Integer,Integer> pointerGroup=new HashMap<>();final Preferences prefs=new Preferences();
        Surface(){pauseGate.foreground(true);flow.started();layoutControls();}
        int getWidth(){return Integer.getInteger("w",1280);}int getHeight(){return Integer.getInteger("h",720);}int activeButtons(){return active;}
        boolean titleTouch(MotionEvent e){throw new AssertionError("Unexpected title event");}
        void clearInputs(){input.clear();pointerGroup.clear();stick.cancel();}void invalidate(){}boolean selectionTouch(MotionEvent e){throw new AssertionError("Unexpected selection event");}
        void pauseMenu(){pauses++;clearInputs();pauseGate.request();dialog=true;}
        boolean performHapticFeedback(int n){if(n!=1)throw new AssertionError("Wrong effect");haptics++;return true;}
        abstract boolean onTouchEvent(MotionEvent e);
        /*LAYOUT*/
    }
    static final class Before extends Surface { /*BEFORE*/ }
    static final class After extends Surface { /*AFTER*/ }
    static int cases;static Surface before=new Before(),after=new After();
    static void event(int feedback,int action,int index,int[] ids,float...xy){
        MotionEvent e=new MotionEvent(action,index,ids,xy);before.onTouchEvent(e);after.onTouchEvent(e);cases++;
        if(after.haptics!=feedback)throw new AssertionError("feedback case="+cases+" actual="+after.haptics+" expected="+feedback);
        if(before.input.mask()!=after.input.mask()||!Arrays.equals(before.input.drain(),after.input.drain())||before.input.sample()!=after.input.sample()||before.stick.mask()!=after.stick.mask()||before.pauses!=after.pauses)throw new AssertionError("Input changed case="+cases);
    }
    public static void main(String[] args){
        float sx=after.dx,sy=after.dy,ax=after.buttonX(0),ay=after.buttonY(0),bx=after.buttonX(1),by=after.buttonY(1);
        event(0,0,0,new int[]{1},sx,sy);event(0,2,0,new int[]{1},sx+70,sy);
        event(1,5,1,new int[]{1,2},sx+70,sy,bx,by);
        for(int n=0;n<30;n++)event(1,2,0,new int[]{1,2},sx+70,sy,bx,by);
        event(2,5,2,new int[]{1,2,3},sx+70,sy,bx,by,ax,ay);
        if(after.input.mask()!=(Input.RIGHT|Input.A|Input.B))throw new AssertionError("Simultaneous stick+A+B");
        event(2,6,2,new int[]{1,2,3},sx+70,sy,bx,by,ax,ay);
        event(2,6,1,new int[]{1,2},sx+70,sy,bx,by);event(2,1,0,new int[]{1},sx+70,sy);
        event(2,0,0,new int[]{4},600,150);event(2,1,0,new int[]{4},600,150);
        before.editing=after.editing=true;event(2,0,0,new int[]{5},bx,by);event(2,1,0,new int[]{5},bx,by);before.editing=after.editing=false;
        before.layoutControls();after.layoutControls();float cx=after.buttonX(2),cy=after.buttonY(2);
        event(3,0,0,new int[]{6},cx,cy);event(3,3,0,new int[]{6},cx,cy);
        event(4,0,0,new int[]{7},after.getWidth()*.92f,after.getHeight()*.04f);event(4,1,0,new int[]{7},after.getWidth()*.92f,after.getHeight()*.04f);
        event(4,0,0,new int[]{8},ax,ay); // Modal consumes touch without feedback.
        System.out.println("PASS "+cases+" touch events; button-only down feedback; no stick/move/hold/up/cancel/empty/edit/modal vibration requests; 007b Input samples preserved");
    }
}
'''.replace('/*LAYOUT*/',layout).replace('/*BEFORE*/',old).replace('/*AFTER*/',actual)
    out=ROOT/'builds/touch_feedback_check';out.mkdir(parents=True,exist_ok=True);file=out/'TouchFeedbackCheck.java';file.write_text(java)
    sources=[file,ROOT/'runtime/src/org/elfen/engine/Input.java']+list((ROOT/'app/src/main/java/org/elfen/controls').glob('*.java'))
    subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8','-d',str(out),*map(str,sources)],check=True)
    for w,h,size,mirror in [(2048,945,1,False),(2048,945,1,True),(1280,720,1,False),(1280,720,1,True),(1024,768,1.5,False),(2400,1080,.65,True)]:
        subprocess.run(['java',f'-Dw={w}',f'-Dh={h}',f'-Dsize={size}',f'-Dmirror={str(mirror).lower()}','-cp',str(out),'TouchFeedbackCheck'],check=True)
if __name__=='__main__':main()
