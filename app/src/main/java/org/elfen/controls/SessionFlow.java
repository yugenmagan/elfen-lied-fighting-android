package org.elfen.controls;
/** In-memory navigation only. No saved-instance-state or disk continuation. */
public final class SessionFlow {
 public static final int TITLE=0,SELECTION=1,PLAY=2;private int screen=TITLE;private boolean session;
 public int screen(){return screen;}public boolean atTitle(){return screen==TITLE;}public boolean canContinue(){return session;}
 public void title(){screen=TITLE;}public void selection(){session=false;screen=SELECTION;}public void started(){session=true;screen=PLAY;}
 public boolean resume(){if(!session)return false;screen=PLAY;return true;}
 public void finished(){session=false;}public void discard(){session=false;screen=TITLE;}public void background(){screen=TITLE;}
}
