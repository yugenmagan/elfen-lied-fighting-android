package android.graphics;
import java.awt.*;import java.awt.font.FontRenderContext;
public final class Paint {
 public static final int ANTI_ALIAS_FLAG=1;public enum Align{LEFT,CENTER,RIGHT}public enum Style{FILL,STROKE}
 public static final FontRenderContext FRC=new FontRenderContext(null,true,true);
 public Align align=Align.LEFT;public float size=12,stroke=1;public int colour=0xff000000;public Style style=Style.FILL;public PorterDuffXfermode xfer;Typeface type=Typeface.create("",0);
 public Paint(){}public Paint(int flags){}public void setColor(int c){colour=c;}public void setTypeface(Typeface t){type=t;}
 public void setXfermode(PorterDuffXfermode x){xfer=x;}public void setTextSize(float s){size=s;}public void setTextAlign(Align a){align=a;}
 public void setStyle(Style s){style=s;}public void setStrokeWidth(float s){stroke=s;}
 public Font font(){return new Font("DejaVu Sans Condensed",type.style,1).deriveFont(size);}
 public float measureText(String s){return (float)font().getStringBounds(s,FRC).getWidth();}
 public float descent(){return font().getLineMetrics("Ag",FRC).getDescent();}
 public float ascent(){return -font().getLineMetrics("Ag",FRC).getAscent();}
}
