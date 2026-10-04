package org.elfen.fighting;
import android.graphics.*;import org.elfen.controls.TitleLayout;import org.elfen.presentation.LocaleCatalog;
/** Original KGT title / DEMO 7 / pack 0074 / image 11, with live menu choices. */
final class TitleScreen {
 static void draw(Canvas c,Bitmap artwork,LocaleCatalog locale,boolean modes,boolean canContinue,int selected){
  Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(0xff000008);c.drawRect(new RectF(0,0,640,480),p);p.setColor(0xffffffff);c.drawBitmap(artwork,0,0,p);
  // Original logo retained; replace only the baked STORY/VS choices.
  p.setColor(0xff59ff33);c.drawRect(new RectF(411,98,575,201),p);
  String[] keys=modes?new String[]{"Сюжет","Обычный VS","Назад","Онлайн"}:new String[]{"Новая игра","Продолжить","Настройки","Язык"};
  p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
  for(int i=0;i<4;i++){
   boolean enabled=modes||i!=1||canContinue;float[] b=TitleLayout.BUTTONS[i];RectF r=new RectF(b[0],b[1],b[2],b[3]);
   p.setColor(enabled?(selected==i?0xff075957:0xff164b39):0xff427847);c.drawRoundRect(r,7,7,p);
   if(selected==i&&enabled){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(0xfff9da62);c.drawRoundRect(r,7,7,p);p.setStyle(Paint.Style.FILL);}
   String text=locale.text(keys[i]);float font=i<2?22:18;p.setTextSize(font);while(p.measureText(text)>r.width()-12&&font>12)p.setTextSize(--font);
   p.setColor(enabled?0xffffffff:0xffabc3ae);c.drawText(text,r.centerX()-p.measureText(text)/2,r.centerY()-(p.ascent()+p.descent())/2,p);
  }
 }
}
