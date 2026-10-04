package android.graphics;
import java.awt.image.BufferedImage;
/** Test adapter only. This is not an Android emulator. */
public final class Bitmap {
 public enum Config {ARGB_8888} public final BufferedImage image;
 public Bitmap(BufferedImage i){image=i;}
 public static Bitmap createBitmap(int w,int h,Config c){return new Bitmap(new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB));}
 public int getWidth(){return image.getWidth();}public int getHeight(){return image.getHeight();}
 public void getPixels(int[] p,int o,int stride,int x,int y,int w,int h){image.getRGB(x,y,w,h,p,o,stride);}
 public void setPixels(int[] p,int o,int stride,int x,int y,int w,int h){image.setRGB(x,y,w,h,p,o,stride);}
}
