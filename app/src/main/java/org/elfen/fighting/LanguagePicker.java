package org.elfen.fighting;
import android.app.*;import android.content.*;import android.graphics.*;import android.view.*;import android.widget.*;
import org.elfen.presentation.LocaleCatalog;

/** Native text plus drawn flags: works offline without flag emoji/font support. */
final class LanguagePicker {
    interface Choice {void selected(String code);}
    static AlertDialog create(Context context,String current,Choice callback){
        String[] names={"日本語","English","Русский"};
        ArrayAdapter<String> adapter=new ArrayAdapter<String>(context,android.R.layout.simple_list_item_1,names){
            @Override public View getView(int position,View reuse,ViewGroup parent){
                float density=context.getResources().getDisplayMetrics().density;LinearLayout row=new LinearLayout(context);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding((int)(20*density),(int)(12*density),(int)(20*density),(int)(12*density));
                Flag flag=new Flag(context,position);row.addView(flag,new LinearLayout.LayoutParams((int)(54*density),(int)(36*density)));
                TextView text=new TextView(context);text.setText(names[position]+(LocaleCatalog.CODES[position].equals(current)?"  ✓":""));text.setTextSize(22);text.setPadding((int)(20*density),0,0,0);row.addView(text);row.setContentDescription(names[position]);return row;
            }
        };
        return new AlertDialog.Builder(context).setTitle("日本語 / English / Русский").setAdapter(adapter,(d,n)->callback.selected(LocaleCatalog.CODES[n])).create();
    }
    private static final class Flag extends View {
        final int country;final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        Flag(Context c,int country){super(c);this.country=country;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas canvas){float w=getWidth(),h=getHeight();
            p.setColor(Color.WHITE);canvas.drawRect(0,0,w,h,p);
            if(country==0){p.setColor(0xffbc002d);canvas.drawCircle(w/2,h/2,h*.30f,p);}
            else if(country==2){p.setColor(0xff0039a6);canvas.drawRect(0,h/3,w,h*2/3,p);p.setColor(0xffd52b1e);canvas.drawRect(0,h*2/3,w,h,p);}
            else{p.setColor(0xffb22234);for(int n=0;n<13;n+=2)canvas.drawRect(0,h*n/13,w,h*(n+1)/13,p);p.setColor(0xff3c3b6e);canvas.drawRect(0,0,w*.4f,h*7/13,p);p.setColor(Color.WHITE);
                for(int row=0;row<9;row++)for(int col=0;col<(row%2==0?6:5);col++){float x=w*.4f*(col*2+(row%2==0?1:2))/12,y=h*7/13*(row+1)/10;Path star=new Path();for(int i=0;i<10;i++){double a=-Math.PI/2+i*Math.PI/5;float r=h*.019f*(i%2==0?1:.4f),xx=x+(float)Math.cos(a)*r,yy=y+(float)Math.sin(a)*r;if(i==0)star.moveTo(xx,yy);else star.lineTo(xx,yy);}star.close();canvas.drawPath(star,p);}}
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1);p.setColor(0xff888888);canvas.drawRect(.5f,.5f,w-.5f,h-.5f,p);p.setStyle(Paint.Style.FILL);
        }
    }
}
