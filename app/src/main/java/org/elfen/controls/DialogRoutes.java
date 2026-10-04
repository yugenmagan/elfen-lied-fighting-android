package org.elfen.controls;
import java.util.*;
/** Run exactly one transition after dismissal; lifecycle invalidates stale choices. */
public final class DialogRoutes<T> {
 private static final class Entry {int epoch;Runnable next;Entry(int e,Runnable r){epoch=e;next=r;}}
 private final IdentityHashMap<T,Entry> entries=new IdentityHashMap<>();private int epoch;
 public void open(T dialog,Runnable back){if(entries.containsKey(dialog))throw new IllegalStateException("Dialog already open");entries.put(dialog,new Entry(epoch,back));}
 public void next(T dialog,Runnable action){Entry e=entries.get(dialog);if(e==null)throw new IllegalStateException("Dialog not open");e.next=action;}
 public Runnable close(T dialog){Entry e=entries.remove(dialog);if(e==null)throw new IllegalStateException("Dialog already closed");return e.epoch==epoch?e.next:null;}
 public List<T> invalidate(){epoch++;return new ArrayList<>(entries.keySet());}
}
