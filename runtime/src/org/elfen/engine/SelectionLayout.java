package org.elfen.engine;
/** Shared draw/touch geometry: preserve original 4:3 with two external toolbars. */
public final class SelectionLayout {
    public final float left,top,right,bottom,scale;
    public SelectionLayout(int w,int h){
        if(w<=0||h<=0)throw new IllegalArgumentException("Selection viewport");
        float a=h*.10f,b=h*.86f;scale=Math.min(w/640f,(b-a)/480f);
        left=(w-640*scale)/2;top=a+(b-a-480*scale)/2;right=left+640*scale;bottom=top+480*scale;
    }
    public int cellAt(CharacterSelect s,float x,float y){
        if(x<left||x>=right||y<top||y>=bottom)return -1;
        // Cursor image84 is 58x58 in the original KGT; centre the touch region.
        int col=Math.round(((x-left)/scale-s.gridX-29)/s.spacingX),row=Math.round(((y-top)/scale-s.gridY-29)/s.spacingY);
        return col<0||col>=s.columns||row<0||row>=s.rows?-1:row*s.columns+col;
    }
}
