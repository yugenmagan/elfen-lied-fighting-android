package org.elfen.presentation;
import java.util.*;

/** Shared text fitting for Android Canvas and desktop layout verification. */
public final class TextFit {
    public interface Measure {float width(String s,float size);}
    public static final class Layout {
        public final List<String> lines;public final float size,lineHeight;
        Layout(List<String> lines,float size){this.lines=Collections.unmodifiableList(lines);this.size=size;lineHeight=size*1.18f;}
    }
    public static Layout fit(String text,int width,int height,int maximum,Measure measure){
        for(float size=maximum;size>=8;size-=.5f){
            // Shrink a long word (or a credits URL) before considering character
            // wrapping, so small captions do not read "He / e" or split names.
            boolean wide=false;for(String word:text.split("\\s+"))if(measure.width(word,size)>width){wide=true;break;}
            if(wide)continue;
            List<String> lines=wrap(text,width,size,measure);if(lines.size()*size*1.18f<=height)return new Layout(lines,size);
        }
        throw new IllegalArgumentException("Translation cannot fit "+width+"x"+height+": "+text);
    }
    private static List<String> wrap(String text,float width,float size,Measure m){
        ArrayList<String> result=new ArrayList<>();for(String paragraph:text.split("\n",-1)){
            if(paragraph.isEmpty()){result.add("");continue;}String remaining=paragraph;
            while(!remaining.isEmpty()){
                if(m.width(remaining,size)<=width){result.add(remaining);break;}
                int end=0,space=-1;
                while(end<remaining.length()){int next=end+Character.charCount(remaining.codePointAt(end));if(m.width(remaining.substring(0,next),size)>width)break;if(remaining.charAt(end)==' ')space=end;end=next;}
                if(end==0)throw new IllegalArgumentException("Glyph wider than translation region");
                int cut=space>0?space:end;result.add(remaining.substring(0,cut).trim());remaining=remaining.substring(cut).trim();
            }
        }return result;
    }
}
