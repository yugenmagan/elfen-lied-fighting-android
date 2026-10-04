package org.elfen.fighting;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.*;
/** Fixed allowlist of diagnostic exports; game assets are never shareable. */
public final class ReportProvider extends ContentProvider {
    public boolean onCreate(){return true;}
    private File file(Uri u)throws FileNotFoundException{
        String path=u.getPath();if(!"/device-report.txt".equals(path)&&!"/device-report.zip".equals(path))throw new FileNotFoundException();return new File(getContext().getCacheDir(),path.substring(1));
    }
    public ParcelFileDescriptor openFile(Uri u,String mode)throws FileNotFoundException{if(!"r".equals(mode))throw new FileNotFoundException();return ParcelFileDescriptor.open(file(u),ParcelFileDescriptor.MODE_READ_ONLY);}
    public String getType(Uri u){return "/device-report.zip".equals(u.getPath())?"application/zip":"text/plain";}
    public Cursor query(Uri u,String[] projection,String selection,String[] args,String order){
        MatrixCursor c=new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE});
        try{File f=file(u);c.addRow(new Object[]{f.getName(),f.length()});}catch(FileNotFoundException e){throw new IllegalArgumentException(e);}return c;
    }
    public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}
    public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
    public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}
}
