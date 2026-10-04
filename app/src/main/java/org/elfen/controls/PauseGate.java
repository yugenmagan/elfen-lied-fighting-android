package org.elfen.controls;

/** UI/lifecycle gate only; no clock or mutable simulation state lives here. */
public final class PauseGate {
    private boolean requested,foreground;private int modals;
    public void request(){requested=true;}
    public void resume(){requested=false;}
    public boolean requested(){return requested;}
    public void foreground(boolean value){foreground=value;}
    public void openModal(){modals++;}
    public void closeModal(){if(modals==0)throw new IllegalStateException("Unbalanced modal");modals--;}
    public boolean hasModal(){return modals>0;}
    public boolean blocked(){return requested||modals>0||!foreground;}
}
