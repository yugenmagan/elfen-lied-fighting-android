package org.elfen.fighting;
import android.graphics.*;
import java.util.List;
import org.elfen.presentation.*;

/** Render translated text into the original sprite dimensions, retaining VM timing. */
final class LocalizedSprites {
    static Bitmap render(Bitmap original,Bitmap cleanCard,List<LocaleCatalog.Block> blocks,int language){
        Bitmap out=Bitmap.createBitmap(original.getWidth(),original.getHeight(),Bitmap.Config.ARGB_8888);Canvas c=new Canvas(out);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        String mode=blocks.get(0).mode;
        if(mode.equals("overlay"))c.drawBitmap(original,0,0,null);
        else if(mode.equals("card")){if(cleanCard==null)throw new IllegalArgumentException("Missing original title-card template");c.drawBitmap(cleanCard,new Rect(0,0,out.getWidth(),out.getHeight()),new Rect(0,0,out.getWidth(),out.getHeight()),null);}
        for(LocaleCatalog.Block b:blocks){
            RectF region=new RectF(b.x,b.y,b.x+b.width,b.y+b.height);
            if(b.background.startsWith("mask:")){
                // Flat caption background with one protected artwork rectangle.
                // Source pixels and the protected yellow surprise mark stay exact.
                int[] pixels=new int[b.width*b.height];out.getPixels(pixels,0,b.width,b.x,b.y,b.width,b.height);
                String[] mask=b.background.split(":");if(mask.length!=6)throw new IllegalArgumentException("Caption mask");
                int background=(int)Long.parseLong(mask[1],16),left=Integer.parseInt(mask[2]),top=Integer.parseInt(mask[3]),right=left+Integer.parseInt(mask[4]),bottom=top+Integer.parseInt(mask[5]);
                for(int n=0;n<pixels.length;n++){int x=b.x+n%b.width,y=b.y+n/b.width;if(x<left||x>=right||y<top||y>=bottom)pixels[n]=background;}
                out.setPixels(pixels,0,b.width,b.x,b.y,b.width,b.height);
            }else if(!b.background.equals("none")){if(b.background.equals("clear")){p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));}else p.setColor((int)Long.parseLong(b.background,16));c.drawRect(region,p);p.setXfermode(null);}
            p.setTypeface(Typeface.create("sans-serif-condensed",b.pack.equals("0116")?Typeface.BOLD:Typeface.NORMAL));
            TextFit.Layout layout=TextFit.fit(b.text(language),b.width-4,b.height-4,b.font,(s,size)->{p.setTextSize(size);return p.measureText(s);});
            p.setTextSize(layout.size);p.setTextAlign(Paint.Align.LEFT);boolean center=b.align.equals("center");
            float y=b.y+2+(center?(b.height-4-layout.lines.size()*layout.lineHeight)/2:0)-p.ascent();
            int save=c.save();c.clipRect(region);
            for(String line:layout.lines){float x=b.x+2+(center?(b.width-4-p.measureText(line))/2:0);
                p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1,layout.size*.065f));p.setColor(0xff101018);c.drawText(line,x,y,p);
                p.setStyle(Paint.Style.FILL);p.setColor((int)Long.parseLong(b.colour,16));c.drawText(line,x,y,p);y+=layout.lineHeight;
            }c.restoreToCount(save);
        }return out;
    }
}
