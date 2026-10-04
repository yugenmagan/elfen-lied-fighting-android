package org.elfen.controls;
/** Shared original-art coordinates, letterboxed without stretching. */
public final class TitleLayout {
 public static final float[][] BUTTONS={{414,99,568,144},{414,153,568,198},{58,446,292,478},{348,446,582,478}};
 public final float scale,left,top;
 public TitleLayout(float width,float height){scale=Math.min(width/640f,height/480f);left=(width-640*scale)/2;top=(height-480*scale)/2;}
 public int hit(float x,float y){if(scale<=0)return -1;x=(x-left)/scale;y=(y-top)/scale;for(int i=0;i<4;i++){float[] b=BUTTONS[i];if(x>=b[0]&&x<b[2]&&y>=b[1]&&y<b[3])return i;}return -1;}
}
