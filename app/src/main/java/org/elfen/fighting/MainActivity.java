package org.elfen.fighting;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.util.LruCache;
import android.view.*;
import android.widget.*;
import org.json.*;
import org.elfen.engine.*;
import org.elfen.presentation.*;
import org.elfen.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class MainActivity extends Activity {
    private final HashMap<String,Pack> packs=new HashMap<>();
    private final HashMap<String,CommandGuide> guides=new HashMap<>();
    private final org.elfen.controls.PauseGate pauseGate=new org.elfen.controls.PauseGate();
    private final ArrayList<String> characters=new ArrayList<>(),stages=new ArrayList<>();
    private StoryCatalog storyCatalog;
    private BattleHud battleHud;
    private LocaleCatalog locales;private boolean languageConfirmed;
    private final org.elfen.controls.SessionFlow flow=new org.elfen.controls.SessionFlow();
    private final org.elfen.controls.DialogRoutes<DialogInterface> dialogRoutes=new org.elfen.controls.DialogRoutes<>();
    private boolean titleModes,focusPausePending;private int titleChoice,titleAxis;
    private OnlineMatch online;private MatchController boundOnline;
    private final Input input=new Input();private GameView game;private Audio audio;
    private SharedPreferences prefs;private boolean resumed=false,dialog=false;private int remap=-1;
    private String selected="0170",opponent="0104",selectedStage="0080",lastError="";private int cpuLevel=50,winsRequired=2,timeSetting=60;private final StringBuilder events=new StringBuilder();
    private long startTime=System.currentTimeMillis();private int maxPointers=0,pauseCount=0,resumeCount=0;
    private final int[] keys={KeyEvent.KEYCODE_BUTTON_A,KeyEvent.KEYCODE_BUTTON_B,KeyEvent.KEYCODE_BUTTON_X,KeyEvent.KEYCODE_BUTTON_Y,KeyEvent.KEYCODE_BUTTON_L1,KeyEvent.KEYCODE_BUTTON_R1};
    @Override public void onCreate(Bundle state){
        super.onCreate(state);prefs=getSharedPreferences("controls",MODE_PRIVATE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);immersive();
        for(int i=0;i<6;i++)keys[i]=prefs.getInt("key"+i,keys[i]);
        selected=prefs.getString("character","0170");selectedStage=prefs.getString("stage","0080");opponent=prefs.getString("opponent","0104");cpuLevel=prefs.getInt("cpuLevel",50);winsRequired=2;timeSetting=prefs.getInt("timeSetting",60);
        audio=new Audio(this,this::fail);game=new GameView();setContentView(game);
        try{
            locales=new LocaleCatalog(getAssets().open("localization/ui.tsv"),getAssets().open("localization/scenes.tsv"));
            locales.language(prefs.getBoolean("releaseLanguageChosen",false)?prefs.getString("language","en"):"en");
            JSONArray rows=new JSONArray(readText(getAssets().open("game/catalog.json")));
            for(int i=0;i<rows.length();i++){
                JSONObject row=rows.getJSONObject(i);String id=row.getString("id");
                Pack p=Pack.read(id,getAssets().open("game/"+id+"/data.efp"));packs.put(id,p);if(p.kind.equals(".player"))guides.put(id,new CommandGuide(p));
                if(p.kind.equals(".player")&&!id.equals("0082")&&!id.equals("0144"))characters.add(id);
                if(p.kind.equals(".stage"))stages.add(id);
            }
            storyCatalog=new StoryCatalog(packs.get("0116"),packs,getAssets().open("game/story-index.tsv"));
            battleHud=new BattleHud(packs.get("0116"),packs);
            // A cold launch has no match. Retire TEST008 persistent continuation.
            new android.util.AtomicFile(new File(getFilesDir(),"story-save.bin")).delete();showTitle();
        }catch(Exception e){fail(tr("Загрузка данных: ")+e);}
        Thread.setDefaultUncaughtExceptionHandler((thread,ex)->{
            try{writeCrash(ex);}catch(IOException ignored){android.util.Log.e("ElfenNative","Unable to persist crash",ignored);}
            android.os.Process.killProcess(android.os.Process.myPid());
        });
    }
    private static String readText(InputStream in)throws IOException{try(InputStream s=in;ByteArrayOutputStream b=new ByteArrayOutputStream()){byte[] buf=new byte[8192];int n;while((n=s.read(buf))!=-1)b.write(buf,0,n);return new String(b.toByteArray(),StandardCharsets.UTF_8);}}
    private String tr(String source){return locales==null?source:locales.text(source);}
    private void chooseLanguage(Runnable continuation,boolean cancelable){
        final AlertDialog[] picker=new AlertDialog[1];
        picker[0]=LanguagePicker.create(this,locales.code(),code->route(picker[0],()->{
            locales.language(code);prefs.edit().putString("language",code).putBoolean("releaseLanguageChosen",true).apply();languageConfirmed=true;
            game.bitmaps.evictAll();clearInputs();continuation.run();
        }));picker[0].setCancelable(cancelable);show(picker[0],cancelable?continuation:null);
    }
    private void clearInputs(){
        input.clear();
        if(game!=null){game.pointerGroup.clear();game.stick.cancel();game.frameInput.reset();game.selectionTouch=-1;}
    }
    private void immersive(){getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);}
    private void showTitle(){
        flow.title();titleModes=false;titleChoice=flow.canContinue()?1:0;titleAxis=0;
        pauseGate.request();clearInputs();game.editing=false;game.titlePointer=-1;
        game.lastFrame=0;game.accumulator=0;audio.pause();game.invalidate();
    }
    private void endCurrentGame(){
        leaveOnline();audio.pause();audio.stopMusic();flow.discard();clearInputs();
        game.selection=null;game.story=null;game.session=null;game.picture=null;
        game.storyRecording=null;game.storyPlayback=null;game.storyHistory=null;
        game.recording=null;game.playback=null;game.history=null;game.savedSnapshot=null;
        game.replayDone=false;game.replayStatus="";game.lastCommand="";game.lastFrame=0;game.accumulator=0;
    }
    private void titleAction(int choice){
        if(!flow.atTitle()||dialog||!resumed)return;clearInputs();
        if(titleModes){switch(choice){case 0:openSelection(true,true);break;case 1:openSelection(false,true);break;case 2:showTitle();break;case 3:onlineMenu();break;}}
        else switch(choice){case 0:endCurrentGame();titleModes=true;titleChoice=0;game.invalidate();break;case 1:if(flow.canContinue())resumeBattle();break;case 2:controlMenu();break;case 3:chooseLanguage(this::showTitle,true);break;}
    }
    private void titleNavigate(int step){
        do{titleChoice=(titleChoice+step+4)%4;}while(!titleModes&&titleChoice==1&&!flow.canContinue());game.invalidate();
    }
    private void route(DialogInterface dialog,Runnable next){dialogRoutes.next(dialog,next);}
    private void show(AlertDialog d){show(d,null);}
    private void show(AlertDialog d,Runnable back){
        dialogRoutes.open(d,back);pauseGate.openModal();dialog=true;clearInputs();audio.pause();
        d.setOnDismissListener(a->{
            Runnable next=dialogRoutes.close(d);pauseGate.closeModal();dialog=pauseGate.hasModal();
            if(game!=null){game.lastFrame=0;game.accumulator=0;}
            if(!resumed)return;immersive();
            // One explicit action, after this window releases its pause token.
            if(next!=null)next.run();
            if(!languageConfirmed&&!dialog&&locales!=null&&storyCatalog!=null&&lastError.isEmpty())chooseLanguage(this::showTitle,false);
            if(canPlayAudio())audio.resume();
        });d.setCanceledOnTouchOutside(false);d.show();
    }
    private boolean canPlayAudio(){return resumed&&!flow.atTitle()&&!pauseGate.blocked()&&!game.editing&&lastError.isEmpty();}
    private void resumeBattle(){
        if(!flow.resume())return;pauseGate.resume();focusPausePending=false;clearInputs();game.editing=false;
        game.lastFrame=0;game.accumulator=0;game.invalidate();if(canPlayAudio())audio.resume();
    }
    private void pauseMenu(){
        if(dialog||!resumed)return;
        if(flow.atTitle()){if(titleModes)showTitle();else finish();return;}
        if(game.selection!=null){selectionBack();return;}if(online!=null){onlineMenu();return;}
        game.editing=false;pauseGate.request();clearInputs();audio.pause();
        if(!lastError.isEmpty()){endCurrentGame();lastError="";showTitle();return;}
        if(!flow.canContinue()){showTitle();return;}
        CommandGuide guide=guides.get(selected);boolean hasMoves=guide!=null&&!guide.entries.isEmpty();
        ArrayList<String> names=new ArrayList<>();ArrayList<Runnable> actions=new ArrayList<>();
        names.add(tr("Продолжить"));actions.add(this::resumeBattle);
        if(hasMoves){names.add(tr("Приёмы"));actions.add(this::moveList);}
        names.add(tr("Настройки"));actions.add(this::controlMenu);
        if(game.story==null){names.add(tr("Настройки матча"));actions.add(this::matchOptions);}
        names.add(tr("Главный экран"));actions.add(this::showTitle);
        show(new AlertDialog.Builder(this).setTitle(tr("ПАУЗА")).setItems(names.toArray(new String[0]),(d,n)->route(d,actions.get(n))).create(),this::resumeBattle);
    }
    private void moveList(){
        CommandGuide guide=guides.get(selected);if(guide==null||guide.entries.isEmpty()){pauseMenu();return;}
        LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);int pad=(int)(16*getResources().getDisplayMetrics().density);content.setPadding(pad,8,pad,8);
        TextView legend=new TextView(this);legend.setText(tr("Направления указаны лицом вправо. С другой стороны зеркально. Запятая — следующий ввод; + — одновременно; • — отпусти стик.\n\nНазвания и команды взяты из игры. Доступность приёма зависит от состояния и энергии персонажа. Удержания: 60 кадров = 1 секунда.\n"));legend.setTextSize(15);content.addView(legend);
        for(CommandGuide.Entry entry:guide.entries){
            TextView name=new TextView(this);name.setText(locales.move(entry.name));name.setTextSize(18);name.setTypeface(null,android.graphics.Typeface.BOLD);content.addView(name);
            TextView command=new TextView(this);command.setText(locales.dynamic(entry.notation)+"\n"+locales.dynamic(entry.stances)+"\n");command.setTextSize(16);content.addView(command);
        }
        ScrollView scroll=new ScrollView(this);scroll.addView(content);
        show(new AlertDialog.Builder(this).setTitle(tr("Приёмы · ")+label(packs.get(selected))).setView(scroll).setPositiveButton(tr("Назад"),(d,n)->{}).create(),this::pauseMenu);
    }
    private void loadSession()throws IOException{
        leaveOnline();flow.started();pauseGate.resume();
        
        game.selection=null;
        game.story=null;game.storyRecording=null;game.storyPlayback=null;game.storyHistory=null;
        Pack p=packs.get(selected),s=packs.get(selectedStage),other=packs.get(opponent);
        if(p==null||s==null||other==null)throw new IOException("Required pack missing");
        audio.pause();audio.preload(p);audio.preload(other);audio.preload(s);audio.preload(packs.get("0116"));
        game.session=new MatchController(p,other,s,packs.get("0116"),1,new MatchRules(packs.get("0116"),2,timeSetting,0,cpuLevel,true));
        game.picture=game.session.view();game.recording=new MatchReplay(game.session);game.history=new MatchHistory(game.session,120);game.playback=null;game.replayDone=false;game.replayStatus="";game.savedSnapshot=null;game.lastCommand="";clearInputs();lastError="";game.lastFrame=0;game.accumulator=0;
        audio.music(s,s.bgm);prefs.edit().putString("character",selected).putString("stage",selectedStage).putString("opponent",opponent).putInt("cpuLevel",cpuLevel).putInt("winsRequired",winsRequired).putInt("timeSetting",timeSetting).apply();
        log("load "+selected+" / "+selectedStage);if(canPlayAudio())audio.resume();
    }

    private void openSelection(boolean storyMode){openSelection(storyMode,false);}
    private void openSelection(boolean storyMode,boolean fromModes){
        try{
            endCurrentGame();flow.selection();pauseGate.resume();game.selection=new CharacterSelect(storyCatalog,storyMode,selected,opponent);
            game.selectionFromModes=fromModes;game.selectionAudioKey="";game.selectionControls=new SelectionControls();game.selectionTouch=-1;
            game.editing=false;game.lastFrame=0;game.accumulator=0;clearInputs();lastError="";
            game.selectionAudio();audio.music(game.selection.backgroundPack(),game.selection.backgroundPack().bgm);
            if(canPlayAudio())audio.resume();log("select-open "+(storyMode?"story":"vs"));
        }catch(IOException|IllegalArgumentException e){fail(tr("Экран выбора: ")+e);}
    }
    private void selectionBack(){
        if(game.selection==null)return;clearInputs();game.selectionTouch=-1;game.selectionControls.requireRelease();
        if(!game.selection.back())return;game.selection=null;audio.stopMusic();showTitle();
    }
    private void selectionComplete()throws IOException{
        CharacterSelect s=game.selection;boolean storyMode=s.story;
        game.story=null;selected=s.selected(0).id;if(!storyMode)opponent=s.selected(1).id;
        prefs.edit().putString("character",selected).putString("opponent",opponent).apply();
        log("select-confirm "+selected+" / "+opponent);if(storyMode)loadStory();else loadSession();
    }
    private void extraFighters(){
        if(game.selection==null||game.selection.story)return;
        ArrayList<Pack> extra=new ArrayList<>();
        for(String id:characters){boolean inGrid=false;for(int n=0;n<game.selection.size();n++)if(game.selection.character(n).id.equals(id))inGrid=true;if(!inGrid)extra.add(packs.get(id));}
        ArrayList<String> names=new ArrayList<>();for(Pack p:extra){names.add("P1: "+label(p));names.add(tr("Противник: ")+label(p));}
        show(new AlertDialog.Builder(this).setTitle(tr("Дополнительные бойцы VS")).setItems(names.toArray(new String[0]),(d,n)->{
            // Preserve TEST006 access without inventing cells absent from KGT.
            game.story=null;
            if(n%2==0){selected=extra.get(n/2).id;opponent=game.selection.selected(1).id;}
            else{selected=game.selection.selected(0).id;opponent=extra.get(n/2).id;}
            try{loadSession();}catch(IOException e){fail(e.toString());}
        }).setNegativeButton(tr("Назад"),(d,n)->{}).create());
    }
    private void loadStory()throws IOException{
        leaveOnline();flow.started();pauseGate.resume();
        game.selection=null;
        audio.pause();game.story=new StoryController(storyCatalog,packs.get(selected),19,0,true,true);game.storyRecording=new StoryReplay(game.story);game.storyHistory=new StoryHistory(game.story,120);game.storyPlayback=null;
        game.playback=null;game.replayDone=false;game.replayStatus="";game.savedSnapshot=null;game.lastCommand="";game.picture=null;game.lastFrame=0;game.accumulator=0;lastError="";clearInputs();game.storyAudio();
        log("story-start "+selected+" hash="+game.story.stateHash());if(canPlayAudio())audio.resume();
    }


    private void fail(String message){
        lastError=message;clearInputs();if(audio!=null)audio.pause();log("ERROR "+message);
        try{writeCrash(new IllegalStateException(message));}catch(IOException e){android.util.Log.e("ElfenNative","Error report write failed",e);}
        if(game!=null)game.invalidate();
    }
    private void log(String message){if(events.length()>60000)events.delete(0,10000);events.append(System.currentTimeMillis()-startTime).append(" ").append(message).append('\n');}
    private void writeCrash(Throwable ex)throws IOException{StringWriter s=new StringWriter();ex.printStackTrace(new PrintWriter(s));try(FileOutputStream out=openFileOutput("last-crash.txt",MODE_PRIVATE)){out.write(s.toString().getBytes(StandardCharsets.UTF_8));}}
    private void choose(int kind){
        if(kind!=2){openSelection(false);return;}
        String[] names=new String[stages.size()];for(int i=0;i<names.length;i++)names[i]=label(packs.get(stages.get(i)));
        show(new AlertDialog.Builder(this).setTitle(tr("Арена")).setItems(names,(d,n)->route(d,()->{
            selectedStage=stages.get(n);try{loadSession();}catch(IOException e){fail(e.toString());}
        })).setNegativeButton(tr("Назад"),(d,n)->{}).create(),this::matchOptions);
    }
    private String label(Pack p){return locales==null?p.name:locales.text("pack:"+p.id);}
    @Override public void onBackPressed(){pauseMenu();}
    private void controlMenu(){
        String[] items=flow.atTitle()?new String[]{tr("Настроить управление"),tr("Переназначить геймпад"),tr("Язык"),tr("Назад")}:
            new String[]{tr("Настроить управление"),tr("Передвинуть элементы"),tr("Переназначить геймпад"),tr("Язык"),tr("Назад")};
        boolean title=flow.atTitle();Runnable back=title?this::showTitle:this::pauseMenu;
        show(new AlertDialog.Builder(this).setTitle(tr("Настройки")).setItems(items,(d,n)->{
            if(n==items.length-1){route(d,back);return;}
            if(n==0)route(d,this::settings);
            else if(!title&&n==1)route(d,()->{game.editing=true;clearInputs();game.invalidate();});
            else if(n==items.length-2)route(d,()->chooseLanguage(this::controlMenu,true));else route(d,this::remapMenu);
        }).create(),back);
    }
    private void matchOptions(){
        show(new AlertDialog.Builder(this).setTitle(tr("Настройки матча")).setItems(new String[]{tr("Арена"),tr("Уровень AI"),tr("Таймер VS"),tr("Новый матч")},(d,n)->{
            route(d,n==0?()->choose(2):n==1?this::cpuMenu:n==2?this::timerMenu:()->openSelection(false));
        }).setNegativeButton(tr("Назад"),(d,n)->{}).create(),this::pauseMenu);
    }
    private void timerMenu(){
        show(new AlertDialog.Builder(this).setTitle(tr("Таймер VS")).setItems(new String[]{"10","30","60",tr("Без таймера")},(d,n)->route(d,()->{
            timeSetting=new int[]{10,30,60,0}[n];try{loadSession();}catch(IOException e){fail(e.toString());}
        })).setNegativeButton(tr("Назад"),(d,n)->{}).create(),this::matchOptions);
    }

    private void leaveOnline(){if(online!=null)online.close();online=null;boundOnline=null;}
    private void onlineMenu(){
        if(online!=null){
            show(new AlertDialog.Builder(this).setTitle(tr("Сетевой бой")).setMessage(locales.dynamic(online.message()))
                .setPositiveButton(tr("Продолжить"),(d,n)->route(d,this::resumeBattle))
                .setNegativeButton(tr("Выйти"),(d,n)->route(d,()->{endCurrentGame();showTitle();})).create(),this::resumeBattle);return;
        }
        String[] choices={tr("Создать комнату"),tr("Войти по коду"),tr("Персонаж: ")+label(packs.get(selected)),tr("Арена: ")+label(packs.get(selectedStage)),tr("Правила матча"),tr("Сервер")};
        show(new AlertDialog.Builder(this).setTitle(tr("Онлайн · два игрока")).setItems(choices,(d,n)->route(d,()->{
            switch(n){case 0:beginOnline(null);break;case 1:joinRoomDialog();break;case 2:chooseOnline(false);break;case 3:chooseOnline(true);break;case 4:matchRulesMenu(true);break;case 5:serverDialog();break;}
        })).setNegativeButton(tr("Назад"),(d,n)->{}).create(),this::showTitle);
    }
    private void chooseOnline(boolean stage){
        ArrayList<String> list=stage?stages:characters;String[] names=new String[list.size()];for(int n=0;n<names.length;n++)names[n]=label(packs.get(list.get(n)));
        show(new AlertDialog.Builder(this).setTitle(stage?tr("Арена комнаты"):tr("Твой персонаж")).setItems(names,(d,n)->{if(stage)selectedStage=list.get(n);else selected=list.get(n);route(d,this::onlineMenu);}).create(),this::onlineMenu);
    }
    private void joinRoomDialog(){
        EditText entry=new EditText(this);entry.setSingleLine(true);entry.setHint(tr("Код из 8 знаков"));entry.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        show(new AlertDialog.Builder(this).setTitle(tr("Войти в комнату")).setView(entry).setPositiveButton(tr("Войти"),(d,n)->route(d,()->{
            String code=entry.getText().toString().trim().toUpperCase(Locale.ROOT);
            if(!code.matches("[A-HJ-NP-Z2-9]{8}")){show(new AlertDialog.Builder(this).setMessage(tr("Нужен код из 8 знаков")).setPositiveButton("OK",(a,i)->{}).create(),this::joinRoomDialog);return;}beginOnline(code);
        })).setNegativeButton(tr("Назад"),(d,n)->{}).create(),this::onlineMenu);
    }
    private void serverDialog(){
        EditText entry=new EditText(this);entry.setSingleLine(true);entry.setHint(tr("tls://адрес:443"));entry.setText(prefs.getString("onlineServer",""));entry.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);
        show(new AlertDialog.Builder(this).setTitle(tr("Адрес сервера комнат")).setMessage(tr("Введи адрес сервера, к которому подключается второй игрок.")).setView(entry)
            .setPositiveButton(tr("Сохранить"),(d,n)->route(d,()->{prefs.edit().putString("onlineServer",entry.getText().toString().trim()).apply();onlineMenu();}))
            .setNegativeButton(tr("Назад"),(d,n)->{}).create(),this::onlineMenu);
    }
    private void beginOnline(String code){
        String server=prefs.getString("onlineServer","");if(server.isEmpty()){serverDialog();return;}
        try{
            endCurrentGame();flow.started();pauseGate.resume();game.selection=null;online=new OnlineMatch(RoomClient.connect(server),packs);
            game.story=null;game.storyRecording=null;game.storyPlayback=null;game.storyHistory=null;game.playback=null;game.replayDone=false;game.savedSnapshot=null;game.replayStatus="";game.editing=false;game.lastFrame=0;game.accumulator=0;clearInputs();lastError="";
            if(code==null)online.create(selected,selectedStage,winsRequired,timeSetting);else online.join(selected,code);
            audio.pause();log(code==null?"online-create":"online-join");
        }catch(IllegalArgumentException|IllegalStateException e){leaveOnline();flow.discard();fail(e.toString());}
    }

    private void matchRulesMenu(boolean forOnline){
        String[] labels={tr("До 1 победы"),tr("До 2 побед"),tr("До 3 побед"),tr("Таймер: 10"),tr("Таймер: 30"),tr("Таймер: 60"),tr("Без таймера")};
        show(new AlertDialog.Builder(this).setTitle(tr("Правила комнаты")).setItems(labels,(d,n)->route(d,()->{
            if(n<3)winsRequired=n+1;else timeSetting=new int[]{10,30,60,0}[n-3];onlineMenu();
        })).setNegativeButton(tr("Назад"),(d,n)->{}).create(),this::onlineMenu);
    }
    private void cpuMenu(){
        show(new AlertDialog.Builder(this).setTitle(tr("Уровень AI")).setItems(new String[]{tr("Манекен (0)"),"30","50","80","100"},(d,n)->route(d,()->{
            cpuLevel=new int[]{0,30,50,80,100}[n];try{loadSession();}catch(IOException e){fail(e.toString());}
        })).setNegativeButton(tr("Назад"),(d,n)->{}).create(),this::matchOptions);
    }
    private void settings(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(24,8,24,8);
        slider(root,tr("Прозрачность"),prefs.getFloat("opacity",.6f),.15f,1f,"opacity");
        slider(root,tr("Размер"),prefs.getFloat("size",1f),.65f,1.5f,"size");
        slider(root,tr("Мёртвая зона стика"),prefs.getFloat("dead",.18f),.05f,.5f,"dead");
        check(root,tr("Зеркальное расположение"),"mirror",false);check(root,tr("Скрывать кнопки с геймпадом"),"hidePad",true);
        ScrollView scroll=new ScrollView(this);scroll.addView(root);
        AlertDialog d=new AlertDialog.Builder(this).setTitle(tr("Аркадный стик и кнопки")).setView(scroll).setPositiveButton(tr("Готово"),(a,b)->{})
            .setNegativeButton(tr("Язык"),(a,b)->route(a,()->chooseLanguage(this::settings,true)))
            .setNeutralButton(tr("Сброс положения"),(a,b)->{prefs.edit().remove("dx").remove("dy").remove("bx").remove("by").apply();}).create();show(d,this::controlMenu);
    }
    private void slider(LinearLayout root,String label,float value,float min,float max,String key){
        TextView title=new TextView(this);title.setText(label);root.addView(title);SeekBar bar=new SeekBar(this);bar.setMax(100);bar.setProgress(Math.round((value-min)*100/(max-min)));root.addView(bar);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}public void onProgressChanged(SeekBar s,int n,boolean user){if(user)prefs.edit().putFloat(key,min+n*(max-min)/100).apply();}});
    }
    private void check(LinearLayout root,String title,String key,boolean initial){CheckBox c=new CheckBox(this);c.setText(title);c.setChecked(prefs.getBoolean(key,initial));c.setOnCheckedChangeListener((b,v)->prefs.edit().putBoolean(key,v).apply());root.addView(c);}
    private void remapMenu(){
        String[] labels={"A","B","C","D","E","F"};
        show(new AlertDialog.Builder(this).setTitle(tr("Переназначить геймпад")).setItems(labels,(d,i)->route(d,()->{
            remap=i;show(new AlertDialog.Builder(this).setTitle(tr("Переназначить геймпад")).setMessage(tr("Нажми кнопку геймпада для ")+(char)('A'+i))
                .setNegativeButton(tr("Назад"),(a,n)->{}).create(),()->{remap=-1;remapMenu();});
        })).setNegativeButton(tr("Назад"),(d,n)->{}).create(),this::controlMenu);
    }
    @Override public boolean dispatchKeyEvent(KeyEvent e){
        if(remap>=0&&e.getAction()==KeyEvent.ACTION_DOWN&&e.getKeyCode()!=KeyEvent.KEYCODE_BACK){keys[remap]=e.getKeyCode();prefs.edit().putInt("key"+remap,keys[remap]).apply();remap=-1;for(DialogInterface d:dialogRoutes.invalidate())d.dismiss();remapMenu();return true;}
        if(dialog)return super.dispatchKeyEvent(e);
        int code=e.getKeyCode(),mask=0;
        if(flow.atTitle()){
            boolean down=e.getAction()==KeyEvent.ACTION_DOWN&&e.getRepeatCount()==0;
            if(code==KeyEvent.KEYCODE_DPAD_DOWN||code==KeyEvent.KEYCODE_DPAD_RIGHT){if(down)titleNavigate(1);return true;}
            if(code==KeyEvent.KEYCODE_DPAD_UP||code==KeyEvent.KEYCODE_DPAD_LEFT){if(down)titleNavigate(-1);return true;}
            if(code==KeyEvent.KEYCODE_DPAD_CENTER||code==KeyEvent.KEYCODE_ENTER||code==KeyEvent.KEYCODE_BUTTON_START||code==keys[0]||code==KeyEvent.KEYCODE_Z){if(down)titleAction(titleChoice);return true;}
            if(code==KeyEvent.KEYCODE_ESCAPE||code==KeyEvent.KEYCODE_BUTTON_B){if(down)pauseMenu();return true;}
            return super.dispatchKeyEvent(e);
        }
        if(game.selection!=null&&(code==KeyEvent.KEYCODE_DPAD_CENTER||code==KeyEvent.KEYCODE_ENTER))mask=Input.A;
        if(code==KeyEvent.KEYCODE_BUTTON_START||code==KeyEvent.KEYCODE_ESCAPE){if(e.getAction()==0&&e.getRepeatCount()==0)pauseMenu();return true;}
        switch(code){case KeyEvent.KEYCODE_DPAD_LEFT:mask=Input.LEFT;break;case KeyEvent.KEYCODE_DPAD_RIGHT:mask=Input.RIGHT;break;case KeyEvent.KEYCODE_DPAD_UP:mask=Input.UP;break;case KeyEvent.KEYCODE_DPAD_DOWN:mask=Input.DOWN;break;}
        int[] keyboard={KeyEvent.KEYCODE_Z,KeyEvent.KEYCODE_X,KeyEvent.KEYCODE_C,KeyEvent.KEYCODE_A,KeyEvent.KEYCODE_S,KeyEvent.KEYCODE_D};
        for(int i=0;i<6;i++)if(code==keys[i]||code==keyboard[i])mask|=16<<i;
        if(mask!=0){if(pauseGate.blocked())return true;input.set(-1000-code,e.getAction()==KeyEvent.ACTION_UP?0:mask);return true;}return super.dispatchKeyEvent(e);
    }
    @Override public boolean onGenericMotionEvent(MotionEvent e){
        if((e.getSource()&InputDevice.SOURCE_JOYSTICK)==InputDevice.SOURCE_JOYSTICK){
            float x=e.getAxisValue(MotionEvent.AXIS_X),y=e.getAxisValue(MotionEvent.AXIS_Y),hx=e.getAxisValue(MotionEvent.AXIS_HAT_X),hy=e.getAxisValue(MotionEvent.AXIS_HAT_Y);
            if(flow.atTitle()){
                int direction=y>.5f||hy>.5f||x>.5f||hx>.5f?1:y<-.5f||hy<-.5f||x<-.5f||hx<-.5f?-1:0;
                if(resumed&&!dialog&&direction!=0&&direction!=titleAxis)titleNavigate(direction);titleAxis=direction;return true;
            }
            int mask=Input.dpad(x,y,1,.2f)|Input.dpad(hx,hy,1,.2f);
            if(e.getAxisValue(MotionEvent.AXIS_LTRIGGER)>.5f)mask|=Input.E;if(e.getAxisValue(MotionEvent.AXIS_RTRIGGER)>.5f)mask|=Input.F;
            input.set(-20000-e.getDeviceId(),pauseGate.blocked()?0:mask);return true;
        }return super.onGenericMotionEvent(e);
    }
    @Override protected void onResume(){
        super.onResume();resumed=true;pauseGate.foreground(true);resumeCount++;
        if(game!=null){game.lastFrame=0;game.accumulator=0;game.start();}immersive();log("resume");
        // Home/unlock does not implicitly resume simulation or audio.
        if(locales!=null&&storyCatalog!=null&&!languageConfirmed&&!dialog&&lastError.isEmpty())chooseLanguage(this::showTitle,false);
    }
    @Override protected void onPause(){
        resumed=false;pauseGate.foreground(false);pauseGate.request();pauseCount++;focusPausePending=false;remap=-1;
        // Invalidate first: pending Resume/Back callbacks cannot escape backgrounding.
        for(DialogInterface d:dialogRoutes.invalidate())d.dismiss();
        flow.background();titleModes=false;titleChoice=flow.canContinue()?1:0;titleAxis=0;clearInputs();
        if(game!=null){game.selection=null;game.selectionTouch=-1;game.selectionControls.requireRelease();game.titlePointer=-1;game.editing=false;game.lastFrame=0;game.accumulator=0;game.stop();}
        if(audio!=null)audio.pause();if(online!=null){leaveOnline();flow.discard();game.session=null;game.picture=null;}
        log("pause");super.onPause();
    }
    @Override protected void onDestroy(){
        resumed=false;for(DialogInterface d:dialogRoutes.invalidate())d.dismiss();endCurrentGame();if(game!=null)game.stop();if(audio!=null)audio.release();super.onDestroy();
    }
    @Override public void onWindowFocusChanged(boolean focused){
        super.onWindowFocusChanged(focused);
        if(focused){immersive();if(focusPausePending&&resumed&&!dialog&&!flow.atTitle()){focusPausePending=false;pauseMenu();}}
        else{clearInputs();if(resumed&&!dialog&&flow.screen()==org.elfen.controls.SessionFlow.PLAY){pauseGate.request();focusPausePending=true;audio.pause();}}
    }
    private void shareReport(){
        String data="Elfen Lied Fighting RC1\n"+Build.MANUFACTURER+" "+Build.MODEL+" / Android "+Build.VERSION.RELEASE+" SDK "+Build.VERSION.SDK_INT+"\nABIs "+Arrays.toString(Build.SUPPORTED_ABIS)+"\nScreen "+game.getWidth()+"x"+game.getHeight()+"\nCharacter "+selected+" Opponent "+opponent+" AI "+cpuLevel+" Stage "+selectedStage+"\nMax pointers "+maxPointers+"\nPause/resume "+pauseCount+"/"+resumeCount+"\nFrame count "+(game.story!=null?game.story.frame():game.session==null?0:game.session.frame())+"\nState hash "+(game.story!=null?game.story.stateHash():game.session==null?"":game.session.stateHash())+"\nReplay "+game.replayStatus+"\nSlow frames "+game.slowFrames+"\nSettings "+prefs.getAll()+"\nError "+lastError+"\n"+events;
        if(game.story!=null)data+="\nStory slot "+game.story.slot()+" mode "+game.story.mode()+" fights "+game.story.fights()+" wins "+game.story.wins()+"\n";
        if(online!=null){data+="\nOnline state "+online.state()+" message "+locales.dynamic(online.message())+"\n";if(online.rollback()!=null)data+="Confirmed "+online.rollback().confirmedFrame()+" rollback "+online.rollback().rollbackCount()+" resimulated "+online.rollback().resimulatedFrames()+"\n";}
        File crash=new File(getFilesDir(),"last-crash.txt");
        try{if(crash.exists())data+="\nLast error/crash:\n"+readText(new FileInputStream(crash));
            if(game.story==null&&game.recording!=null){try(FileOutputStream replay=openFileOutput("last-match.efm",MODE_PRIVATE)){replay.write(game.recording.encode());}}
            File f=new File(getCacheDir(),"device-report.txt");try(FileOutputStream out=new FileOutputStream(f)){out.write(data.getBytes(StandardCharsets.UTF_8));}
            File archive=new File(getCacheDir(),"device-report.zip");
            try(java.util.zip.ZipOutputStream out=new java.util.zip.ZipOutputStream(new FileOutputStream(archive))){
                out.putNextEntry(new java.util.zip.ZipEntry("device-report.txt"));out.write(data.getBytes(StandardCharsets.UTF_8));out.closeEntry();
                if(game.story==null&&game.recording!=null){out.putNextEntry(new java.util.zip.ZipEntry("last-match.efm"));out.write(game.recording.encode());out.closeEntry();}
                if(game.storyRecording!=null){out.putNextEntry(new java.util.zip.ZipEntry("last-story.efs"));out.write(game.storyRecording.encode());out.closeEntry();}
            }
            Uri uri=Uri.parse("content://org.elfen.fighting.nativeport.reports/device-report.zip");Intent intent=new Intent(Intent.ACTION_SEND);intent.setType("application/zip");intent.putExtra(Intent.EXTRA_STREAM,uri);intent.setClipData(ClipData.newRawUri("Match report and replay",uri));intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(Intent.createChooser(intent,tr("Сохранить или отправить отчёт и повтор")));
        }catch(IOException e){fail(tr("Не удалось создать отчёт: ")+e);}
    }

    private final class GameView extends View implements Choreographer.FrameCallback {
        CharacterSelect selection;SelectionControls selectionControls=new SelectionControls();String selectionAudioKey="";boolean selectionFromModes;int selectionTouch=-1;
        final RectF selectBack=new RectF(),selectConfirm=new RectF(),selectExtra=new RectF();
        StoryController story;StoryReplay storyRecording,storyPlayback;StoryHistory storyHistory;
        MatchController session;BattleView picture;MatchReplay recording,playback;MatchHistory history;byte[] savedSnapshot;int savedFrame,playIndex;String expectedReplayHash="",replayStatus="";boolean replayDone;String lastCommand="";Bitmap scene;Canvas sceneCanvas;final Paint paint=new Paint();final RectF dest=new RectF();
        final LruCache<String,Bitmap> bitmaps=new LruCache<String,Bitmap>(48*1024*1024){protected int sizeOf(String k,Bitmap b){return b.getAllocationByteCount();}};
        final HashMap<Integer,Integer> pointerGroup=new HashMap<>();
        final org.elfen.controls.ArcadeStick stick=new org.elfen.controls.ArcadeStick();
        final org.elfen.controls.FrameInput frameInput=new org.elfen.controls.FrameInput();
        int titlePointer=-1,titlePressed=-1;
        long lastFrame=0,accumulator=0;int slowFrames=0;boolean running=false,editing=false;
        float dx,dy,bx,by,radius,buttonRadius;int lastMask=0;
        org.elfen.controls.CombatLayout controlLayout;
        GameView(){super(MainActivity.this);setFocusable(true);setFocusableInTouchMode(true);paint.setFilterBitmap(false);}
        void selectionAudio()throws IOException{
            ArrayList<Pack> needed=new ArrayList<>(selection.audioPacks());
            for(BattleSimulation.Event e:selection.events())if(e.kind.equals("sound")&&!needed.contains(packs.get(e.packId)))needed.add(packs.get(e.packId));
            StringBuilder key=new StringBuilder();for(Pack p:needed)key.append(p.id).append('/');
            if(!selectionAudioKey.equals(key.toString())){audio.retain(needed);selectionAudioKey=key.toString();}
        }
        void selectionStep(int[] samples)throws IOException{
            selectionControls.step(selection,samples);selectionAudio();
            for(BattleSimulation.Event e:selection.events())if(e.kind.equals("sound"))audio.play(packs.get(e.packId),e.value);
            if(selection.finished())selectionComplete();
        }
        void storyAudio()throws IOException{
            if(story.finished()){audio.stopMusic();return;}
            if(story.mode()==StoryController.FIGHT){Pack a=packs.get(selected);
                ArrayList<Pack> needed=new ArrayList<>();needed.add(a);needed.add(story.stagePack());needed.add(packs.get("0116"));
                // Initial fighter sprites are not emitted before the first tick.
                StoryProgram.Event e=new StoryProgram(a).event(story.slot());needed.add(storyCatalog.character(e.enemy()));audio.retain(needed);audio.music(story.stagePack(),story.stagePack().bgm);
            }else{audio.retain(Collections.singletonList(story.scenePack()));audio.music(story.scenePack(),story.scenePack().bgm);}
        }



        void start(){if(!running){running=true;Choreographer.getInstance().postFrameCallback(this);}}
        void stop(){running=false;Choreographer.getInstance().removeFrameCallback(this);}
        void onlinePoll()throws IOException{
            if(online==null)return;online.pump();RollbackSession net=online.rollback();if(net==null){audio.pause();return;}
            if(boundOnline!=net.match()){
                boundOnline=net.match();session=boundOnline;GameConfig cfg=online.config();selected=net.localPlayer()==0?cfg.p1:cfg.p2;opponent=net.localPlayer()==0?cfg.p2:cfg.p1;selectedStage=cfg.stage;
                recording=net.replay();history=null;playback=null;replayDone=false;savedSnapshot=null;lastCommand="";picture=session.view();clearInputs();lastFrame=0;accumulator=0;
                audio.retain(Arrays.asList(packs.get(cfg.p1),packs.get(cfg.p2),packs.get(cfg.stage),packs.get("0116")));audio.music(packs.get(cfg.stage),packs.get(cfg.stage).bgm);if(canPlayAudio())audio.resume();
                log("online-ready role="+net.localPlayer()+" initial="+session.stateHash());
            }
            picture=session.view();
            for(BattleSimulation.Event e:net.drainConfirmedEvents()){
                if(e.kind.equals("sound"))audio.play(packs.get(e.packId),e.value);
                if(e.kind.equals("command")&&e.packId.equals(selected))lastCommand=packs.get(e.packId).commands[e.value].name;
            }
            if(online.state()==OnlineMatch.FAILED||online.state()==OnlineMatch.DISCONNECTED)audio.pause();
        }
        @Override public void doFrame(long time){
            if(!running)return;long delta=lastFrame==0?0:time-lastFrame;lastFrame=time;
            try{onlinePoll();}catch(IOException|IllegalStateException|IllegalArgumentException e){fail(e.toString());}
            if((selection!=null||(online!=null?online.canAdvance():story!=null?!story.finished():session!=null&&!session.state().finished))&&(selection!=null||!replayDone)&&resumed&&!flow.atTitle()&&!pauseGate.blocked()&&!editing&&lastError.isEmpty()){
                if(delta>33000000)slowFrames++;accumulator+=Math.min(delta,100000000);
                try{int count=0;while(accumulator>=16666667&&count++<6){
                    if(online!=null&&!online.canAdvance()){accumulator=0;break;}
                    int[] samples=frameInput.poll(input);int mask=samples[samples.length-1];
                    if(selection!=null){selectionStep(samples);if(selection==null){accumulator=0;break;}accumulator-=16666667;continue;}
                    if(online!=null){online.advance(samples);picture=session.view();accumulator-=16666667;continue;}
                    if(story!=null){
                        InputFrame frame=storyPlayback==null?new InputFrame(story.frame(),samples,new int[]{0}):storyPlayback.input(playIndex++);
                        int oldSlot=story.slot(),oldMode=story.mode();storyHistory.advance(frame);if(storyPlayback==null)storyRecording.append(frame);
                        boolean musicChanged=false;
                        for(BattleSimulation.Event e:story.events()){
                            if(e.kind.equals("music"))musicChanged=true;
                            if(e.kind.equals("sound"))audio.play(packs.get(e.packId),e.value);
                            if(e.kind.equals("command")&&e.packId.equals(selected))lastCommand=packs.get(e.packId).commands[e.value].name;
                            if(e.kind.startsWith("story-")||e.kind.equals("ko"))log(e.kind+" slot="+story.slot()+" frame="+story.frame()+" pack="+e.packId+" hash="+story.stateHash());
                            if(e.kind.startsWith("reaction-error-"))log("ORIGINAL_DATA_WARNING "+e.kind+" pack="+e.packId+" skill="+e.value);
                        }
                        if(musicChanged||oldSlot!=story.slot()||oldMode!=story.mode())storyAudio();
                        picture=story.view();
                        if(storyPlayback!=null&&playIndex==storyPlayback.length()){boolean same=expectedReplayHash.equals(story.stateHash());replayStatus=same?tr("Повтор: хеш совпал"):tr("Повтор: DESYNC");storyPlayback=null;replayDone=true;log(replayStatus+" "+story.stateHash());if(!same)throw new IllegalStateException(replayStatus);}
                        if(story.finished()||replayDone){accumulator=0;if(story.finished()&&!replayDone){flow.finished();showTitle();}break;}accumulator-=16666667;continue;
                    }
                    InputFrame frame=playback==null?new InputFrame(session.frame(),samples,new int[]{0}):playback.input(playIndex++);history.advance(frame);if(playback==null)recording.append(frame);
                    for(BattleSimulation.Event e:session.events()){
                        if(e.kind.equals("sound"))audio.play(packs.get(e.packId),e.value);
                        if(e.kind.equals("command")&&e.packId.equals(selected))lastCommand=packs.get(e.packId).commands[e.value].name;
                        if(e.kind.equals("ko"))log("KO winner="+e.value+" frame="+e.frame+" hash="+session.stateHash());
                        if(e.kind.startsWith("reaction-error-"))log("ORIGINAL_DATA_WARNING "+e.kind+" pack="+e.packId+" skill="+e.value+" frame="+e.frame);
                    }
                    picture=session.view();
                    if(playback!=null&&playIndex==playback.length()){
                        boolean same=expectedReplayHash.equals(session.stateHash());replayStatus=same?tr("Повтор: хеш совпал"):tr("Повтор: DESYNC");log(replayStatus+" frame="+session.frame()+" hash="+session.stateHash());playback=null;replayDone=true;if(!same)throw new IllegalStateException(replayStatus);
                    }
                    if(session.state().finished||replayDone){if(session.state().finished)flow.finished();accumulator=0;break;}
                    if(mask!=lastMask){log("input "+Integer.toHexString(mask)+" frame="+frame.frame);lastMask=mask;}accumulator-=16666667;
                }}catch(IOException|IllegalStateException|IllegalArgumentException ex){fail(ex.getMessage());}

            }else {accumulator=0;if(online!=null){input.drain();input.sample();}}
            invalidate();Choreographer.getInstance().postFrameCallback(this);
        }
        Bitmap image(Pack pack,int i){
            Bitmap original=rawImage(pack,i);if(original==null||locales==null)return original;
            List<LocaleCatalog.Block> blocks=locales.sprite(pack.id,i);if(blocks==null)return original;
            String key="locale:"+locales.code()+":"+pack.id+":"+i;Bitmap translated=bitmaps.get(key);
            if(translated==null){Bitmap card=blocks.get(0).mode.equals("card")?rawImage(packs.get("0064"),2):null;
                translated=LocalizedSprites.render(original,card,blocks,locales.index());bitmaps.put(key,translated);}
            return translated;
        }
        Bitmap rawImage(Pack pack,int i){
            if(i<0||i>=pack.widths.length)throw new IllegalStateException("Image index "+pack.id+":"+i);
            if(pack.widths[i]==0||pack.heights[i]==0)return null;
            String path="game/"+pack.id+"/"+String.format(Locale.ROOT,"%04d.png",i);Bitmap bmp=bitmaps.get(path);
            if(bmp==null)try(InputStream stream=getAssets().open(path)){BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;bmp=BitmapFactory.decodeStream(stream,null,options);if(bmp==null)throw new IOException("PNG decode failed");bitmaps.put(path,bmp);}catch(IOException e){throw new IllegalStateException(path+": "+e);}
            return bmp;
        }
        Bitmap tinted(Pack pack,int i,Bitmap original,int rgba){
            if((rgba&0xffffff00)==0)return original;
            String key=locales.code()+":"+pack.id+":"+i+":"+(rgba&0xffffff00);Bitmap result=bitmaps.get(key);if(result!=null)return result;
            int dr=(byte)(rgba>>>24),dg=(byte)(rgba>>>16),db=(byte)(rgba>>>8);
            int[] pixels=new int[original.getWidth()*original.getHeight()];original.getPixels(pixels,0,original.getWidth(),0,0,original.getWidth(),original.getHeight());
            for(int n=0;n<pixels.length;n++){
                int p=pixels[n];if((p>>>24)==0)continue;
                int r=Math.max(0,Math.min(31,((p>>>19)&31)+dr)),g=Math.max(0,Math.min(31,((p>>>11)&31)+dg)),b=Math.max(0,Math.min(31,((p>>>3)&31)+db));
                if(r+g+b==0)b=1;pixels[n]=0xff000000|(r<<19)|(g<<11)|(b<<3);
            }
            result=Bitmap.createBitmap(pixels,original.getWidth(),original.getHeight(),Bitmap.Config.ARGB_8888);bitmaps.put(key,result);return result;
        }
        void sprite(Canvas canvas,BattleView.Sprite s){
            Pack pack=packs.get(s.packId);if(pack==null)throw new IllegalStateException("Missing render pack "+s.packId);
            Bitmap bmp=image(pack,s.image);if(bmp==null)return;bmp=tinted(pack,s.image,bmp,s.rgba);
            float x=s.x/65536f,y=s.y/65536f;boolean flip=(s.flags&16384)!=0,vflip=(s.flags&32768)!=0;
            if(s.background){x+=s.offsetX;y+=s.offsetY;if(!s.absolute){x-=picture.cameraX;y-=picture.cameraY;}}
            else{boolean left=s.left&&(s.options&1)==0;x+=(left?-s.offsetX:s.offsetX)-bmp.getWidth()/2;y+=s.offsetY-bmp.getHeight();if(!s.absolute){x-=picture.cameraX;y-=picture.cameraY;}flip^=left;}
            if(s.colour==3)subtract(bmp,(int)x,(int)y,flip,vflip);
            else{
                int save=canvas.save();canvas.translate(x+(flip?bmp.getWidth():0),y+(vflip?bmp.getHeight():0));canvas.scale(flip?-1:1,vflip?-1:1);
                paint.setAlpha(s.colour==1?128:s.colour==4?Math.max(0,Math.min(255,(32-(byte)s.rgba)*255/32)):255);
                if(s.colour==2)paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));
                canvas.drawBitmap(bmp,0,0,paint);paint.setXfermode(null);paint.setAlpha(255);canvas.restoreToCount(save);
            }
        }
        void subtract(Bitmap source,int x,int y,boolean flip,boolean vflip){
            int w=source.getWidth(),h=source.getHeight(),left=Math.max(0,x),top=Math.max(0,y),right=Math.min(640,x+w),bottom=Math.min(480,y+h);
            if(left>=right||top>=bottom)return;int[] src=new int[w*h],dst=new int[(right-left)*(bottom-top)];source.getPixels(src,0,w,0,0,w,h);scene.getPixels(dst,0,right-left,left,top,right-left,bottom-top);
            for(int yy=top;yy<bottom;yy++)for(int xx=left;xx<right;xx++){
                int sx=xx-x,sy=yy-y;if(flip)sx=w-1-sx;if(vflip)sy=h-1-sy;int p=src[sy*w+sx];if((p>>>24)==0)continue;int i=(yy-top)*(right-left)+xx-left,q=dst[i];
                dst[i]=0xff000000|(Math.max(0,((q>>>16)&255)-((p>>>16)&255))<<16)|(Math.max(0,((q>>>8)&255)-((p>>>8)&255))<<8)|Math.max(0,(q&255)-(p&255));
            }
            scene.setPixels(dst,0,right-left,left,top,right-left,bottom-top);
        }

        void hud(Canvas c){
            MatchState match=story==null?session.state():null;
            int timer=story==null?match.timer:story.timer(),score1=story==null?match.score1:story.score(0),score2=story==null?match.score2:story.score(1);
            int rounds=story==null?match.roundsToWin:story.roundsToWin();
            for(BattleHud.Draw draw:battleHud.view(picture,timer,score1,score2,rounds)){
                if(draw.clipWidth<=0||draw.clipHeight<=0)continue;
                int save=c.save();c.clipRect(draw.clipLeft,draw.clipTop,draw.clipLeft+draw.clipWidth,draw.clipTop+draw.clipHeight);
                sprite(c,draw.sprite);c.restoreToCount(save);
            }
            if(story!=null)return;
            paint.setTextAlign(Paint.Align.CENTER);
            if(match.finished&&(online==null||online.state()==OnlineMatch.COMPLETE)){int me=online==null?0:online.rollback().localPlayer();paint.setColor(0xee101018);c.drawRect(115,153,525,265,paint);paint.setColor(Color.WHITE);paint.setTextSize(26);c.drawText(match.winner<0?tr("НИЧЬЯ"):match.winner==me?tr("МАТЧ ВЫИГРАН"):tr("МАТЧ ПРОИГРАН"),320,191,paint);paint.setTextSize(17);c.drawText(match.score1+" : "+match.score2,320,220,paint);paint.setTextSize(14);c.drawText(online==null?tr("Открой паузу, чтобы вернуться на главный экран"):tr("Открой паузу, чтобы вернуться на главный экран"),320,247,paint);}paint.setTextAlign(Paint.Align.LEFT);
        }
        @Override protected void onDraw(Canvas c){
            c.drawColor(Color.BLACK);paint.setAntiAlias(false);paint.setColor(Color.WHITE);
            if(flow.atTitle()){try{if(storyCatalog!=null&&locales!=null)drawTitle(c);}catch(IllegalStateException e){if(lastError.isEmpty())fail(e.getMessage());}drawFailure(c);return;}
            if(selection!=null){try{drawSelection(c);}catch(IllegalStateException e){if(lastError.isEmpty())fail(e.getMessage());}drawFailure(c);return;}
            layoutControls();
            if(picture!=null||story!=null){
                if(scene==null){scene=Bitmap.createBitmap(640,480,Bitmap.Config.ARGB_8888);sceneCanvas=new Canvas(scene);}sceneCanvas.drawColor(Color.BLACK);
                try{if(story!=null){
                    if(story.mode()==StoryController.FIGHT){for(BattleView.Sprite s:picture.sprites)sprite(sceneCanvas,s);hud(sceneCanvas);for(BattleView.Sprite s:story.overlays())sprite(sceneCanvas,s);}
                    else{for(BattleView.Sprite s:story.sceneView())sprite(sceneCanvas,s);paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(15);paint.setColor(Color.WHITE);
                        if(story.mode()==StoryController.SCENE)sceneCanvas.drawText(tr("A: продолжить"),320,468,paint);
                        if(story.mode()==StoryController.CONTINUE){paint.setColor(0xff000008);sceneCanvas.drawRect(100,202,520,420,paint);paint.setColor(0xdd15101a);sceneCanvas.drawRect(145,205,495,275,paint);paint.setColor(Color.WHITE);sceneCanvas.drawText(story.continueChoice()==0?tr("▶ Продолжить     В меню"):tr("Продолжить     ▶ В меню"),320,234,paint);sceneCanvas.drawText(tr("Направление: выбор · A: подтвердить"),320,260,paint);}
                        if(story.finished()){paint.setTextSize(25);sceneCanvas.drawText(story.mode()==StoryController.COMPLETE?tr("КОНЕЦ СЮЖЕТА"):tr("ВОЗВРАТ В МЕНЮ"),320,210,paint);paint.setTextSize(16);sceneCanvas.drawText(tr("Пауза → Меню → Другой персонаж / Обычный VS"),320,250,paint);}paint.setTextAlign(Paint.Align.LEFT);
                    }
                }else{for(BattleView.Sprite s:picture.sprites)sprite(sceneCanvas,s);if(!session.state().finished)hud(sceneCanvas);for(BattleView.Sprite s:session.overlays())sprite(sceneCanvas,s);if(session.state().finished)hud(sceneCanvas);}}catch(IllegalStateException e){if(lastError.isEmpty())fail(e.getMessage());}
                dest.set(controlLayout.left,controlLayout.top,controlLayout.right,controlLayout.bottom);paint.setAlpha(255);c.drawBitmap(scene,null,dest,paint);
            }
            paint.setAntiAlias(true);
            drawStartButton(c);
            if(editing){paint.setColor(0xfff2bf70);c.drawText(tr("Передвинь элементы. Нажми паузу, чтобы закончить."),getWidth()*.025f,getHeight()*.14f,paint);}
            if(editing||!prefs.getBoolean("hidePad",true)||!hasGamepad())drawControls(c);
            if(online!=null&&online.state()!=OnlineMatch.COMPLETE&&!online.canAdvance()){
                paint.setColor(0xee141523);c.drawRect(getWidth()*.13f,getHeight()*.33f,getWidth()*.87f,getHeight()*.62f,paint);paint.setColor(Color.WHITE);paint.setTextSize(getHeight()*.03f);
                String text=locales.dynamic(online.message());int row=0;for(int n=0;n<text.length()&&row<4;n+=55)c.drawText(text.substring(n,Math.min(n+55,text.length())),getWidth()*.16f,getHeight()*(.40f+.045f*row++),paint);
                c.drawText(tr("Пауза — продолжить или выйти"),getWidth()*.16f,getHeight()*.58f,paint);
            }
            drawFailure(c);
        }
        void drawFailure(Canvas c){
            if(!lastError.isEmpty()){
                paint.setColor(0xee24131b);c.drawRect(getWidth()*.08f,getHeight()*.30f,getWidth()*.92f,getHeight()*.65f,paint);
                paint.setColor(Color.WHITE);paint.setTextSize(getHeight()*.035f);paint.setTextAlign(Paint.Align.CENTER);
                c.drawText(tr("Не удалось продолжить игру"),getWidth()/2f,getHeight()*.43f,paint);
                paint.setTextSize(getHeight()*.027f);c.drawText(tr("Перезапусти приложение"),getWidth()/2f,getHeight()*.54f,paint);paint.setTextAlign(Paint.Align.LEFT);
            }
        }
        void drawTitle(Canvas c){
            if(scene==null){scene=Bitmap.createBitmap(640,480,Bitmap.Config.ARGB_8888);sceneCanvas=new Canvas(scene);}
            TitleScreen.draw(sceneCanvas,rawImage(storyCatalog.screen(0),11),locales,titleModes,flow.canContinue(),titleChoice);
            org.elfen.controls.TitleLayout layout=new org.elfen.controls.TitleLayout(getWidth(),getHeight());
            dest.set(layout.left,layout.top,layout.left+640*layout.scale,layout.top+480*layout.scale);paint.setAlpha(255);c.drawBitmap(scene,null,dest,paint);
        }
        boolean titleTouch(MotionEvent e){
            int action=e.getActionMasked(),index=e.getActionIndex();org.elfen.controls.TitleLayout layout=new org.elfen.controls.TitleLayout(getWidth(),getHeight());
            if(action==MotionEvent.ACTION_CANCEL){titlePointer=-1;titlePressed=-1;return true;}
            if(action==MotionEvent.ACTION_DOWN){titlePointer=e.getPointerId(index);titlePressed=layout.hit(e.getX(index),e.getY(index));
                if(titlePressed>=0&&(titleModes||titlePressed!=1||flow.canContinue())){titleChoice=titlePressed;performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);invalidate();}}
            if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_POINTER_UP){if(e.getPointerId(index)==titlePointer){
                int hit=layout.hit(e.getX(index),e.getY(index)),pressed=titlePressed;titlePointer=-1;titlePressed=-1;if(hit>=0&&hit==pressed)titleAction(hit);}}
            return true;
        }
        void drawSelection(Canvas c){
            if(scene==null){scene=Bitmap.createBitmap(640,480,Bitmap.Config.ARGB_8888);sceneCanvas=new Canvas(scene);}
            sceneCanvas.drawColor(Color.BLACK);paint.setAntiAlias(false);for(BattleView.Sprite s:selection.view())sprite(sceneCanvas,s);
            float w=getWidth(),h=getHeight(),top=h*.10f,bottom=h*.86f;
            SelectionLayout layout=new SelectionLayout(getWidth(),getHeight());dest.set(layout.left,layout.top,layout.right,layout.bottom);
            paint.setAlpha(255);c.drawBitmap(scene,null,dest,paint);
            paint.setAntiAlias(true);paint.setColor(0xff17151e);c.drawRect(0,0,w,top,paint);c.drawRect(0,bottom,w,h,paint);
            paint.setColor(0xfff4cf80);paint.setTextSize(h*.031f);paint.setTextAlign(Paint.Align.LEFT);
            c.drawText(selection.story?tr("СЮЖЕТ · ВЫБОР ПЕРСОНАЖА"):tr("VS · ВЫБОР ПЕРСОНАЖЕЙ"),w*.025f,h*.037f,paint);
            int active=selection.activePlayer();String prompt=selection.confirmed(active)?tr("Выбор подтверждён…"):active==0?tr("Твой боец: "):tr("Противник: ");
            if(!selection.confirmed(active))prompt+=label(selection.selected(active))+tr(" · нажми на портрет");
            paint.setColor(Color.WHITE);paint.setTextSize(h*.029f);
            while(paint.measureText(prompt)>w*.95f&&paint.getTextSize()>h*.018f)paint.setTextSize(paint.getTextSize()-1);
            c.drawText(prompt,w*.025f,h*.079f,paint);
            float gap=h*.015f;selectBack.set(gap,bottom+gap,w*.22f,h-gap);selectExtra.set(w*.24f,bottom+gap,w*.49f,h-gap);selectConfirm.set(w*.53f,bottom+gap,w-gap,h-gap);
            selectionButton(c,selectBack,tr("Назад"),0xff36323f);
            if(!selection.story)selectionButton(c,selectExtra,tr("Дополнительно"),0xff36323f);
            selectionButton(c,selectConfirm,selection.confirmed(active)?tr("Начинаем…"):active==0?tr("Выбрать бойца"):tr("Начать бой"),selection.confirmed(active)?0xff4b4247:0xff973851);
        }
        void selectionButton(Canvas c,RectF r,String text,int colour){
            paint.setColor(colour);c.drawRoundRect(r,getHeight()*.018f,getHeight()*.018f,paint);
            paint.setColor(Color.WHITE);paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(getHeight()*.036f);
            while(paint.measureText(text)>r.width()*.92f&&paint.getTextSize()>getHeight()*.018f)paint.setTextSize(paint.getTextSize()-1);
            c.drawText(text,r.centerX(),r.centerY()-(paint.ascent()+paint.descent())/2,paint);paint.setTextAlign(Paint.Align.LEFT);
        }
        boolean selectionTouch(MotionEvent e){
            int action=e.getActionMasked(),index=e.getActionIndex();
            if(action==MotionEvent.ACTION_CANCEL){clearInputs();selectionTouch=-1;return true;}
            if(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_POINTER_DOWN){
                float x=e.getX(index),y=e.getY(index);
                if(selectBack.contains(x,y)){selectionBack();return true;}
                if(!lastError.isEmpty())return true;
                if(selectExtra.contains(x,y)&&!selection.story){extraFighters();return true;}
                if(selectConfirm.contains(x,y)){if(!selection.confirmed(selection.activePlayer()))input.set(e.getPointerId(index),Input.A);return true;}
                if(dest.contains(x,y)&&selectionTouch<0)selectionTouch=e.getPointerId(index);
            }
            boolean up=action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_POINTER_UP;
            for(int n=0;n<e.getPointerCount();n++){
                int id=e.getPointerId(n);if(up&&n==index){input.set(id,0);if(selectionTouch==id)selectionTouch=-1;continue;}
                if(id!=selectionTouch)continue;
                int cell=new SelectionLayout(getWidth(),getHeight()).cellAt(selection,e.getX(n),e.getY(n));
                if(cell>=0&&selection.available(cell))selection.focus(selection.activePlayer(),cell);
            }invalidate();return true;
        }
        boolean hasGamepad(){for(int id:InputDevice.getDeviceIds()){InputDevice d=InputDevice.getDevice(id);if(d!=null&&(d.getSources()&InputDevice.SOURCE_GAMEPAD)==InputDevice.SOURCE_GAMEPAD)return true;}return false;}
        void layoutControls(){
            controlLayout=new org.elfen.controls.CombatLayout(getWidth(),getHeight(),prefs.getFloat("size",1),prefs.getBoolean("mirror",false),
                prefs.getFloat("dx",Float.NaN),prefs.getFloat("dy",Float.NaN),prefs.getFloat("bx",Float.NaN),prefs.getFloat("by",Float.NaN));
            dx=controlLayout.stickX;dy=controlLayout.stickY;bx=controlLayout.buttonsX;by=controlLayout.buttonsY;
            radius=controlLayout.stickRadius;buttonRadius=controlLayout.buttonRadius;
            stick.configure(dx,dy,radius,prefs.getFloat("dead",.18f));
        }
        int activeButtons(){CommandGuide g=guides.get(selected);return g==null?Input.A:g.buttons;}
        float buttonX(int i){return controlLayout.buttonX(i);}float buttonY(int i){return controlLayout.buttonY(i);}
        void drawStartButton(Canvas c){
            RectF r=new RectF(getWidth()*.84f,getHeight()*.01f,getWidth()*.99f,getHeight()*.09f);
            paint.setColor(0xffdad9d5);c.drawRoundRect(r,getHeight()*.018f,getHeight()*.018f,paint);
            // Pause symbol only: no letters on or beside the white Start button.
            float unit=r.height()*.12f,top=r.centerY()-unit*1.7f,bottom=r.centerY()+unit*1.7f;
            paint.setColor(0xff302f2e);c.drawRect(r.centerX()-unit*1.5f,top,r.centerX()-unit*.5f,bottom,paint);c.drawRect(r.centerX()+unit*.5f,top,r.centerX()+unit*1.5f,bottom,paint);
        }
        void drawControls(Canvas c){
            int alpha=(int)(prefs.getFloat("opacity",.6f)*255),mask=input.mask();
            // A recessed circular gate, eight marks and a moving thumb cap.
            paint.setColor(Color.argb(alpha,23,24,34));c.drawCircle(dx,dy,radius,paint);
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(Math.max(1,radius*.025f));
            paint.setColor(Color.argb(alpha,151,133,148));c.drawCircle(dx,dy,radius*.94f,paint);
            paint.setColor(Color.argb(alpha/2,198,178,187));c.drawCircle(dx,dy,radius*.58f,paint);
            paint.setStyle(Paint.Style.FILL);
            int[] directions={Input.RIGHT,Input.RIGHT|Input.DOWN,Input.DOWN,Input.LEFT|Input.DOWN,Input.LEFT,Input.LEFT|Input.UP,Input.UP,Input.RIGHT|Input.UP};
            for(int i=0;i<8;i++){
                double angle=i*Math.PI/4;boolean selected=(mask&15)==directions[i];
                paint.setColor(selected?Color.argb(alpha,255,209,132):Color.argb(alpha/2,194,184,201));
                c.drawCircle(dx+(float)Math.cos(angle)*radius*.81f,dy+(float)Math.sin(angle)*radius*.81f,radius*(selected?.046f:.025f),paint);
            }
            float capX=dx+stick.offsetX(),capY=dy+stick.offsetY(),cap=radius*org.elfen.controls.ArcadeStick.CAP;
            paint.setColor(Color.argb(alpha,13,12,20));c.drawCircle(capX,capY+radius*.045f,cap*1.08f,paint);
            paint.setStrokeWidth(radius*.13f);paint.setColor(Color.argb(alpha,128,110,124));c.drawLine(dx,dy,capX,capY,paint);
            paint.setColor(stick.active()?Color.argb(alpha,232,52,56):Color.argb(alpha,199,27,32));c.drawCircle(capX,capY,cap,paint);
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(radius*.025f);
            paint.setColor(Color.argb(alpha,stick.active()?255:238,stick.active()?134:85,stick.active()?134:91));c.drawCircle(capX,capY,cap,paint);
            paint.setStyle(Paint.Style.FILL);paint.setColor(Color.argb(alpha/3,255,244,232));c.drawCircle(capX-cap*.24f,capY-cap*.28f,cap*.24f,paint);
            // Base colours sampled from the supplied arcade-controller photograph.
            int[] colours={0xffc43438,0xffcab61b,0xff4bde60};
            for(int i=0;i<6;i++){
                if((activeButtons()&(16<<i))==0)continue;
                boolean pressed=(mask&(16<<i))!=0;int colour;
                if(i<3){
                    colour=colours[i];int red=Color.red(colour),green=Color.green(colour),blue=Color.blue(colour);
                    if(pressed){red+=(255-red)/5;green+=(255-green)/5;blue+=(255-blue)/5;}
                    colour=Color.argb(pressed?221:alpha,red,green,blue);
                }else colour=pressed?0xddce4661:Color.argb(alpha,45,49,65);
                paint.setColor(colour);c.drawCircle(buttonX(i),buttonY(i),buttonRadius,paint);
                if(i<3){
                    paint.setColor(Color.argb(pressed?221:alpha,238,237,233));paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(buttonRadius*.42f);
                    c.drawText(String.valueOf((char)('A'+i)),buttonX(i),buttonY(i)+buttonRadius*1.62f,paint);paint.setTextAlign(Paint.Align.LEFT);
                }
            }
        }
        @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){
            super.onSizeChanged(w,h,oldw,oldh);clearInputs();
        }
        @Override public boolean onTouchEvent(MotionEvent e){
            if(dialog||!resumed)return true;
            if(flow.atTitle())return titleTouch(e);
            if(selection!=null)return selectionTouch(e);
            layoutControls();int action=e.getActionMasked(),index=e.getActionIndex();maxPointers=Math.max(maxPointers,e.getPointerCount());
            if(action==MotionEvent.ACTION_CANCEL){clearInputs();pointerGroup.clear();return true;}
            if(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_POINTER_DOWN){int id=e.getPointerId(index);float x=e.getX(index),y=e.getY(index);
                if(x>getWidth()*.84f&&y<getHeight()*.12f){performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);pauseMenu();return true;}
                if(pauseGate.blocked()&&!editing)return true;
                int group;
                if(stick.contains(x,y))group=editing||stick.begin(id,x,y)?1:0;
                else{
                    group=2;
                    if(!editing)for(int b=0;b<6;b++)if((activeButtons()&(16<<b))!=0&&Math.hypot(x-buttonX(b),y-buttonY(b))<buttonRadius*1.12f){performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);break;}
                }
                pointerGroup.put(id,group);
            }
            boolean up=action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_POINTER_UP;
            for(int history=0;history<=e.getHistorySize();history++)for(int i=0;i<e.getPointerCount();i++){
                int id=e.getPointerId(i);
                if(!pointerGroup.containsKey(id))continue;float x=history<e.getHistorySize()?e.getHistoricalX(i,history):e.getX(i),y=history<e.getHistorySize()?e.getHistoricalY(i,history):e.getY(i);
                if(editing){if(i==0){boolean mirror=prefs.getBoolean("mirror",false);float nx=Math.max(.06f,Math.min(.94f,x/getWidth())),ny=Math.max(.2f,Math.min(.9f,y/getHeight()));if(mirror)nx=1-nx;String prefix=pointerGroup.get(id)==1?"d":"b";prefs.edit().putFloat(prefix+"x",nx).putFloat(prefix+"y",ny).apply();}input.set(id,0);continue;}
                int mask=0;if(pointerGroup.get(id)==1){stick.move(id,x,y);mask=stick.mask();}
                else if(pointerGroup.get(id)==2)for(int b=0;b<6;b++)if((activeButtons()&(16<<b))!=0&&Math.hypot(x-buttonX(b),y-buttonY(b))<buttonRadius*1.12f)mask|=16<<b;
                input.set(id,mask);
            }
            if(up){int id=e.getPointerId(index);stick.end(id);input.set(id,0);pointerGroup.remove(id);}
            invalidate();return true;
        }
    }
}
