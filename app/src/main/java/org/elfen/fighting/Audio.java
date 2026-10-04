package org.elfen.fighting;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.*;
import android.os.Handler;
import android.os.Looper;
import java.io.IOException;
import java.util.*;
import org.elfen.engine.Pack;

final class Audio {
    interface Failure {void fail(String message);}
    private final Context context;private final Failure failure;private final SoundPool pool;
    private final AudioManager manager;private final AudioFocusRequest focus;
    private final HashMap<String,Integer> ids=new HashMap<>();private final HashSet<Integer> ready=new HashSet<>(),pending=new HashSet<>();
    private MediaPlayer music;private boolean active=false,focused=false,prepared=false;private String current="";
    Audio(Context c,Failure failure){
        context=c;this.failure=failure;manager=(AudioManager)c.getSystemService(Context.AUDIO_SERVICE);
        AudioAttributes attrs=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
        pool=new SoundPool.Builder().setMaxStreams(16).setAudioAttributes(attrs).build();
        pool.setOnLoadCompleteListener((p,id,status)->{if(!ids.containsValue(id))return;if(status!=0){failure.fail("SoundPool load status "+status+", id="+id);return;}ready.add(id);if(pending.remove(id)&&active&&focused)pool.play(id,1,1,1,0,1);});
        focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(attrs).setOnAudioFocusChangeListener(change->{
            focused=change==AudioManager.AUDIOFOCUS_GAIN;
            if(focused){if(active){pool.autoResume();if(music!=null&&prepared)music.start();}}
            else {pending.clear();pool.autoPause();if(music!=null&&prepared)music.pause();}
        },new Handler(Looper.getMainLooper())).build();
    }
    void preload(Pack pack)throws IOException{
        for(int i=1;i<pack.sounds.length;i++)if(pack.sounds[i].endsWith(".wav")){
            String path="game/"+pack.id+"/"+pack.sounds[i];if(ids.containsKey(path))continue;
            try(AssetFileDescriptor fd=context.getAssets().openFd(path)){int id=pool.load(fd,1);if(id==0)throw new IOException("SoundPool rejected "+path);ids.put(path,id);}
        }
    }
    void retain(Collection<Pack> packs)throws IOException{
        HashSet<String> keep=new HashSet<>();for(Pack p:packs)for(String sound:p.sounds)if(sound.endsWith(".wav"))keep.add("game/"+p.id+"/"+sound);
        for(Iterator<Map.Entry<String,Integer>> i=ids.entrySet().iterator();i.hasNext();){Map.Entry<String,Integer> e=i.next();if(!keep.contains(e.getKey())){pool.unload(e.getValue());ready.remove(e.getValue());pending.remove(e.getValue());i.remove();}}
        for(Pack p:packs)preload(p);
    }
    void play(Pack pack,int i){
        String path="game/"+pack.id+"/"+pack.sounds[i];
        if(pack.sounds[i].endsWith(".mid")){music(pack,i);return;}
        Integer id=ids.get(path);if(id==null){failure.fail("Sound not preloaded: "+path);return;}
        if(!active||!focused)return;if(ready.contains(id))pool.play(id,1,1,1,0,1);else pending.add(id);
    }
    void music(Pack pack,int i){
        stopMusic();if(i==0)return;
        if(i<0||i>=pack.sounds.length||pack.sounds[i].isEmpty()){failure.fail("Missing BGM "+pack.id+":"+i);return;}
        current="game/"+pack.id+"/"+pack.sounds[i];music=new MediaPlayer();
        music.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
        try(AssetFileDescriptor fd=context.getAssets().openFd(current)){
            music.setDataSource(fd.getFileDescriptor(),fd.getStartOffset(),fd.getLength());music.setLooping(true);
            music.setOnPreparedListener(m->{prepared=true;if(active&&focused)m.start();});
            music.setOnErrorListener((m,what,extra)->{failure.fail("BGM "+current+": "+what+"/"+extra);return true;});music.prepareAsync();
        }catch(IOException|IllegalStateException e){failure.fail("BGM: "+e);stopMusic();}
    }
    void resume(){active=true;focused=manager.requestAudioFocus(focus)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED;if(focused){pool.autoResume();if(music!=null&&prepared)music.start();}}
    void pause(){active=false;focused=false;pending.clear();pool.autoPause();if(music!=null&&prepared)music.pause();manager.abandonAudioFocusRequest(focus);}
    void stopMusic(){prepared=false;if(music!=null){music.release();music=null;}}
    void release(){pause();stopMusic();pool.release();}
}
