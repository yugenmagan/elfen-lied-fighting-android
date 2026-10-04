package android.graphics;
import java.awt.*;import java.awt.geom.*;import java.util.*;
/** Maps just the production sprite renderer's Canvas calls to Java2D for preview. */
public final class Canvas {
 private Graphics2D g;private final ArrayList<Graphics2D> stack=new ArrayList<>();
 public Canvas(Bitmap b){g=b.image.createGraphics();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,RenderingHints.VALUE_FRACTIONALMETRICS_ON);}
 public void drawBitmap(Bitmap b,int x,int y,Paint p){g.drawImage(b.image,x,y,null);}
 public void drawBitmap(Bitmap b,Rect s,Rect d,Paint p){g.drawImage(b.image,d.left,d.top,d.right,d.bottom,s.left,s.top,s.right,s.bottom,null);}
 public void drawRect(RectF r,Paint p){setup(p);g.fill(new Rectangle2D.Float(r.left,r.top,r.right-r.left,r.bottom-r.top));g.setComposite(AlphaComposite.SrcOver);}
 public void drawRoundRect(RectF r,float rx,float ry,Paint p){setup(p);RoundRectangle2D shape=new RoundRectangle2D.Float(r.left,r.top,r.width(),r.height(),rx*2,ry*2);if(p.style==Paint.Style.STROKE)g.draw(shape);else g.fill(shape);}
 public void drawCircle(float x,float y,float r,Paint p){setup(p);Ellipse2D.Float q=new Ellipse2D.Float(x-r,y-r,2*r,2*r);if(p.style==Paint.Style.STROKE)g.draw(q);else g.fill(q);}
 public void drawLine(float x,float y,float xx,float yy,Paint p){setup(p);g.draw(new Line2D.Float(x,y,xx,yy));}
 public void drawRect(float x,float y,float xx,float yy,Paint p){drawRect(new RectF(x,y,xx,yy),p);}
 private void setup(Paint p){g.setColor(new java.awt.Color(p.colour,true));g.setStroke(new BasicStroke(p.stroke,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.setComposite(p.xfer==null?AlphaComposite.SrcOver:AlphaComposite.Clear);}
 public int save(){stack.add(g);g=(Graphics2D)g.create();return stack.size();}
 public void clipRect(RectF r){g.clip(new Rectangle2D.Float(r.left,r.top,r.right-r.left,r.bottom-r.top));}
 public void restoreToCount(int count){while(stack.size()>=count){g.dispose();g=stack.remove(stack.size()-1);}}
 public void drawText(String s,float x,float y,Paint p){setup(p);if(p.align==Paint.Align.CENTER)x-=p.measureText(s)/2;else if(p.align==Paint.Align.RIGHT)x-=p.measureText(s);if(p.style==Paint.Style.STROKE)g.draw(p.font().createGlyphVector(Paint.FRC,s).getOutline(x,y));else{g.setFont(p.font());g.drawString(s,x,y);}}
}
