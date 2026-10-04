#!/usr/bin/env python3
"""One-time release-UI patch over the verified TEST008 Activity; not a build step."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1];p=ROOT/'app/src/main/java/org/elfen/fighting/MainActivity.java';s=p.read_text()
assert 'private LocaleCatalog locales;private boolean languageReadyForSelection;' in s,'Already patched or wrong base'
def rep(a,b):
 global s
 assert a in s,a[:100];s=s.replace(a,b)
def meth(marker,new):
 global s
 a=s.index(marker);i=s.index('{',a)+1;depth=1
 while depth:
  if s[i]=='{':depth+=1
  if s[i]=='}':depth-=1
  i+=1
 s=s[:a]+new+s[i:]
rep('private LocaleCatalog locales;private boolean languageReadyForSelection;', '''private LocaleCatalog locales;private boolean languageConfirmed;
    private final org.elfen.controls.SessionFlow flow=new org.elfen.controls.SessionFlow();
    private final org.elfen.controls.DialogRoutes<DialogInterface> dialogRoutes=new org.elfen.controls.DialogRoutes<>();
    private boolean titleModes,focusPausePending;private int titleChoice,titleAxis;''')
rep('locales.language(prefs.getString("language","ja"));','locales.language(prefs.getBoolean("releaseLanguageChosen",false)?prefs.getString("language","en"):"en");')
rep('loadSession();new Handler().post(()->chooseLanguage(this::intro,false));','''// A cold launch has no match. Retire TEST008 persistent continuation.
            new android.util.AtomicFile(new File(getFilesDir(),"story-save.bin")).delete();showTitle();''')
meth('    private void chooseLanguage(','''    private void chooseLanguage(Runnable continuation,boolean cancelable){
        final AlertDialog[] picker=new AlertDialog[1];
        picker[0]=LanguagePicker.create(this,locales.code(),code->route(picker[0],()->{
            locales.language(code);prefs.edit().putString("language",code).putBoolean("releaseLanguageChosen",true).apply();languageConfirmed=true;
            game.bitmaps.evictAll();clearInputs();continuation.run();
        }));picker[0].setCancelable(cancelable);show(picker[0],cancelable?continuation:null);
    }''')
meth('    private void intro()', '''    private void showTitle(){
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
    }''')
meth('    private void show(AlertDialog d)', '''    private void route(DialogInterface dialog,Runnable next){dialogRoutes.next(dialog,next);}
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
    private boolean canPlayAudio(){return resumed&&!flow.atTitle()&&!pauseGate.blocked()&&!game.editing&&lastError.isEmpty();}''')
meth('    private void resumeBattle()', '''    private void resumeBattle(){
        if(!flow.resume())return;pauseGate.resume();focusPausePending=false;clearInputs();game.editing=false;
        game.lastFrame=0;game.accumulator=0;game.invalidate();if(canPlayAudio())audio.resume();
    }''')
meth('    private void pauseMenu()', '''    private void pauseMenu(){
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
    }''')
rep('if(guide==null||guide.entries.isEmpty())return;','if(guide==null||guide.entries.isEmpty()){pauseMenu();return;}')
rep('''        if(guide.unsupported>0){TextView notice=new TextView(this);notice.setText(tr("Не показаны команды с неподтверждённым форматом: ")+guide.unsupported);content.addView(notice);}
''','')
rep('.setPositiveButton(tr("К паузе"),(d,n)->{}).create());','.setPositiveButton(tr("Назад"),(d,n)->{}).create(),this::pauseMenu);')
meth('    private void saveStoryToDisk()', '');meth('    private void loadSavedStory()', '')
s=s.replace('saveStoryToDisk();','')
rep('resumeBattle();leaveOnline();','leaveOnline();flow.started();pauseGate.resume();')
meth('    private void chooseStory()', '')
meth('    private void openSelection(boolean storyMode,boolean fromModes)', '''    private void openSelection(boolean storyMode,boolean fromModes){
        try{
            endCurrentGame();flow.selection();pauseGate.resume();game.selection=new CharacterSelect(storyCatalog,storyMode,selected,opponent);
            game.selectionFromModes=fromModes;game.selectionAudioKey="";game.selectionControls=new SelectionControls();game.selectionTouch=-1;
            game.editing=false;game.lastFrame=0;game.accumulator=0;clearInputs();lastError="";
            game.selectionAudio();audio.music(game.selection.backgroundPack(),game.selection.backgroundPack().bgm);
            if(canPlayAudio())audio.resume();log("select-open "+(storyMode?"story":"vs"));
        }catch(IOException|IllegalArgumentException e){fail(tr("Экран выбора: ")+e);}
    }''')
meth('    private void selectionBack()', '''    private void selectionBack(){
        if(game.selection==null)return;clearInputs();game.selectionTouch=-1;game.selectionControls.requireRelease();
        if(!game.selection.back())return;game.selection=null;audio.stopMusic();showTitle();
    }''')
s=s.replace('        // Save the suspended route under its original character identity.\n','')
meth('    private void choose(int kind)', '''    private void choose(int kind){
        if(kind!=2){openSelection(false);return;}
        String[] names=new String[stages.size()];for(int i=0;i<names.length;i++)names[i]=label(packs.get(stages.get(i)));
        show(new AlertDialog.Builder(this).setTitle(tr("Арена")).setItems(names,(d,n)->route(d,()->{
            selectedStage=stages.get(n);try{loadSession();}catch(IOException e){fail(e.toString());}
        })).setNegativeButton(tr("Назад"),(d,n)->{}).create(),this::matchOptions);
    }''')
meth('    private void menu()', '''    private void controlMenu(){
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
    }''')
meth('    private void storyMenu()', '')
meth('    private void onlineMenu()', '''    private void onlineMenu(){
        if(online!=null){
            show(new AlertDialog.Builder(this).setTitle(tr("Сетевой бой")).setMessage(locales.dynamic(online.message()))
                .setPositiveButton(tr("Продолжить"),(d,n)->route(d,this::resumeBattle))
                .setNegativeButton(tr("Выйти"),(d,n)->route(d,()->{endCurrentGame();showTitle();})).create(),this::resumeBattle);return;
        }
        String[] choices={tr("Создать комнату"),tr("Войти по коду"),tr("Персонаж: ")+label(packs.get(selected)),tr("Арена: ")+label(packs.get(selectedStage)),tr("Правила матча"),tr("Сервер")};
        show(new AlertDialog.Builder(this).setTitle(tr("Онлайн · два игрока")).setItems(choices,(d,n)->route(d,()->{
            switch(n){case 0:beginOnline(null);break;case 1:joinRoomDialog();break;case 2:chooseOnline(false);break;case 3:chooseOnline(true);break;case 4:matchRulesMenu(true);break;case 5:serverDialog();break;}
        })).setNegativeButton(tr("Назад"),(d,n)->{}).create(),this::showTitle);
    }''')
rep('new Handler().post(this::onlineMenu);}).create());','route(d,this::onlineMenu);}).create(),this::onlineMenu);')
meth('    private void joinRoomDialog()', '''    private void joinRoomDialog(){
        EditText entry=new EditText(this);entry.setSingleLine(true);entry.setHint(tr("Код из 8 знаков"));entry.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        show(new AlertDialog.Builder(this).setTitle(tr("Войти в комнату")).setView(entry).setPositiveButton(tr("Войти"),(d,n)->route(d,()->{
            String code=entry.getText().toString().trim().toUpperCase(Locale.ROOT);
            if(!code.matches("[A-HJ-NP-Z2-9]{8}")){show(new AlertDialog.Builder(this).setMessage(tr("Нужен код из 8 знаков")).setPositiveButton("OK",(a,i)->{}).create(),this::joinRoomDialog);return;}beginOnline(code);
        })).setNegativeButton(tr("Назад"),(d,n)->{}).create(),this::onlineMenu);
    }''')
meth('    private void serverDialog()', '''    private void serverDialog(){
        EditText entry=new EditText(this);entry.setSingleLine(true);entry.setHint(tr("tls://адрес:443"));entry.setText(prefs.getString("onlineServer",""));entry.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);
        show(new AlertDialog.Builder(this).setTitle(tr("Адрес сервера комнат")).setMessage(tr("Введи адрес сервера, к которому подключается второй игрок.")).setView(entry)
            .setPositiveButton(tr("Сохранить"),(d,n)->route(d,()->{prefs.edit().putString("onlineServer",entry.getText().toString().trim()).apply();onlineMenu();}))
            .setNegativeButton(tr("Назад"),(d,n)->{}).create(),this::onlineMenu);
    }''')
rep('if(server.isEmpty()){new Handler().post(this::serverDialog);return;}','if(server.isEmpty()){serverDialog();return;}')
rep('leaveOnline();game.selection=null;online=new OnlineMatch','endCurrentGame();flow.started();pauseGate.resume();game.selection=null;online=new OnlineMatch')
rep('catch(IllegalArgumentException|IllegalStateException e){leaveOnline();Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();}','catch(IllegalArgumentException|IllegalStateException e){leaveOnline();flow.discard();fail(e.toString());}')
meth('    private void matchRulesMenu()', '')
meth('    private void matchRulesMenu(boolean forOnline)', '''    private void matchRulesMenu(boolean forOnline){
        String[] labels={tr("До 1 победы"),tr("До 2 побед"),tr("До 3 побед"),tr("Таймер: 10"),tr("Таймер: 30"),tr("Таймер: 60"),tr("Без таймера")};
        show(new AlertDialog.Builder(this).setTitle(tr("Правила комнаты")).setItems(labels,(d,n)->route(d,()->{
            if(n<3)winsRequired=n+1;else timeSetting=new int[]{10,30,60,0}[n-3];onlineMenu();
        })).setNegativeButton(tr("Назад"),(d,n)->{}).create(),this::onlineMenu);
    }''')
meth('    private void cpuMenu()', '''    private void cpuMenu(){
        show(new AlertDialog.Builder(this).setTitle(tr("Уровень AI")).setItems(new String[]{tr("Манекен (0)"),"30","50","80","100"},(d,n)->route(d,()->{
            cpuLevel=new int[]{0,30,50,80,100}[n];try{loadSession();}catch(IOException e){fail(e.toString());}
        })).setNegativeButton(tr("Назад"),(d,n)->{}).create(),this::matchOptions);
    }''')
rep(';check(root,tr("Показывать исходные хитбоксы"),"boxes",false);',';')
rep('        AlertDialog d=new AlertDialog.Builder(this).setTitle(tr("Аркадный стик и кнопки")).setView(root)', '        ScrollView scroll=new ScrollView(this);scroll.addView(root);\n        AlertDialog d=new AlertDialog.Builder(this).setTitle(tr("Аркадный стик и кнопки")).setView(scroll)')
rep('new Handler().post(()->chooseLanguage(this::settings,true))','route(a,()->chooseLanguage(this::settings,true))')
rep('.remove("by").apply();}).create();show(d);','.remove("by").apply();}).create();show(d,this::controlMenu);')
meth('    private void remapMenu()', '''    private void remapMenu(){
        String[] labels={"A","B","C","D","E","F"};
        show(new AlertDialog.Builder(this).setTitle(tr("Переназначить геймпад")).setItems(labels,(d,i)->route(d,()->{
            remap=i;show(new AlertDialog.Builder(this).setTitle(tr("Переназначить геймпад")).setMessage(tr("Нажми кнопку геймпада для ")+(char)('A'+i))
                .setNegativeButton(tr("Назад"),(a,n)->{}).create(),()->{remap=-1;remapMenu();});
        })).setNegativeButton(tr("Назад"),(d,n)->{}).create(),this::controlMenu);
    }''')
rep('remap=-1;Toast.makeText(this,tr("Кнопка сохранена"),Toast.LENGTH_SHORT).show();return true;','remap=-1;for(DialogInterface d:dialogRoutes.invalidate())d.dismiss();remapMenu();return true;')
rep('''        int code=e.getKeyCode(),mask=0;
''','''        int code=e.getKeyCode(),mask=0;
        if(flow.atTitle()){
            boolean down=e.getAction()==KeyEvent.ACTION_DOWN&&e.getRepeatCount()==0;
            if(code==KeyEvent.KEYCODE_DPAD_DOWN||code==KeyEvent.KEYCODE_DPAD_RIGHT){if(down)titleNavigate(1);return true;}
            if(code==KeyEvent.KEYCODE_DPAD_UP||code==KeyEvent.KEYCODE_DPAD_LEFT){if(down)titleNavigate(-1);return true;}
            if(code==KeyEvent.KEYCODE_DPAD_CENTER||code==KeyEvent.KEYCODE_ENTER||code==KeyEvent.KEYCODE_BUTTON_START||code==keys[0]||code==KeyEvent.KEYCODE_Z){if(down)titleAction(titleChoice);return true;}
            if(code==KeyEvent.KEYCODE_ESCAPE||code==KeyEvent.KEYCODE_BUTTON_B){if(down)pauseMenu();return true;}
            return super.dispatchKeyEvent(e);
        }
''')
rep('''            int mask=Input.dpad(x,y,1,.2f)|Input.dpad(hx,hy,1,.2f);''','''            if(flow.atTitle()){
                int direction=y>.5f||hy>.5f||x>.5f||hx>.5f?1:y<-.5f||hy<-.5f||x<-.5f||hx<-.5f?-1:0;
                if(resumed&&!dialog&&direction!=0&&direction!=titleAxis)titleNavigate(direction);titleAxis=direction;return true;
            }
            int mask=Input.dpad(x,y,1,.2f)|Input.dpad(hx,hy,1,.2f);''')
meth('    @Override protected void onResume()', '''    @Override protected void onResume(){
        super.onResume();resumed=true;pauseGate.foreground(true);resumeCount++;
        if(game!=null){game.lastFrame=0;game.accumulator=0;game.start();}immersive();log("resume");
        // Home/unlock does not implicitly resume simulation or audio.
        if(locales!=null&&storyCatalog!=null&&!languageConfirmed&&!dialog&&lastError.isEmpty())chooseLanguage(this::showTitle,false);
    }''')
meth('    @Override protected void onPause()', '''    @Override protected void onPause(){
        resumed=false;pauseGate.foreground(false);pauseGate.request();pauseCount++;focusPausePending=false;remap=-1;
        // Invalidate first: pending Resume/Back callbacks cannot escape backgrounding.
        for(DialogInterface d:dialogRoutes.invalidate())d.dismiss();
        flow.background();titleModes=false;titleChoice=flow.canContinue()?1:0;titleAxis=0;clearInputs();
        if(game!=null){game.selection=null;game.selectionTouch=-1;game.selectionControls.requireRelease();game.titlePointer=-1;game.editing=false;game.lastFrame=0;game.accumulator=0;game.stop();}
        if(audio!=null)audio.pause();if(online!=null){leaveOnline();flow.discard();game.session=null;game.picture=null;}
        log("pause");super.onPause();
    }''')
meth('    @Override protected void onDestroy()', '''    @Override protected void onDestroy(){
        resumed=false;for(DialogInterface d:dialogRoutes.invalidate())d.dismiss();endCurrentGame();if(game!=null)game.stop();if(audio!=null)audio.release();super.onDestroy();
    }''')
meth('    @Override public void onWindowFocusChanged(boolean focused)', '''    @Override public void onWindowFocusChanged(boolean focused){
        super.onWindowFocusChanged(focused);
        if(focused){immersive();if(focusPausePending&&resumed&&!dialog&&!flow.atTitle()){focusPausePending=false;pauseMenu();}}
        else{clearInputs();if(resumed&&!dialog&&flow.screen()==org.elfen.controls.SessionFlow.PLAY){pauseGate.request();focusPausePending=true;audio.pause();}}
    }''')
rep('long lastFrame=0,accumulator=0;', 'int titlePointer=-1,titlePressed=-1;\n        long lastFrame=0,accumulator=0;')
rep('&&resumed&&!pauseGate.blocked()&&!editing&&lastError.isEmpty())','&&resumed&&!flow.atTitle()&&!pauseGate.blocked()&&!editing&&lastError.isEmpty())')
rep('new Handler().post(MainActivity.this::storyMenu);','flow.finished();showTitle();')
rep('if(session.state().finished||replayDone){accumulator=0;break;}','if(session.state().finished||replayDone){if(session.state().finished)flow.finished();accumulator=0;break;}')
rep('''            if(selection==null&&prefs.getBoolean("boxes",false)&&!s.background){boxes(canvas,s.hurt,0x9965e572);boxes(canvas,s.hit,0x99ff4466);}
''','')
rep('tr("Пауза → Меню → Новый матч / Повторить запись")','tr("Открой паузу, чтобы вернуться на главный экран")')
rep('tr("Хеш результата совпал · Пауза → Меню → Выйти")','tr("Открой паузу, чтобы вернуться на главный экран")')
rep('''            if(selection!=null){try{drawSelection(c);}''','''            if(flow.atTitle()){try{if(storyCatalog!=null&&locales!=null)drawTitle(c);}catch(IllegalStateException e){if(lastError.isEmpty())fail(e.getMessage());}drawFailure(c);return;}
            if(selection!=null){try{drawSelection(c);}''')
a=s.index('            paint.setAntiAlias(true);paint.setTextSize(Math.max(12,getHeight()*.025f));');b=s.index('            drawStartButton(c);',a);s=s[:a]+'            paint.setAntiAlias(true);\n'+s[b:]
rep('tr("Передвигай левый и правый блок. Пауза → Меню → Передвинуть элементы — закончить.")','tr("Передвинь элементы. Нажми паузу, чтобы закончить.")')
meth('        void drawFailure(Canvas c)', '''        void drawFailure(Canvas c){
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
        }''')
rep('''            if(dialog||!resumed)return true;
            if(selection!=null)return selectionTouch(e);''','''            if(dialog||!resumed)return true;
            if(flow.atTitle())return titleTouch(e);
            if(selection!=null)return selectionTouch(e);''')
s=s.replace('if(resumed&&!pauseGate.blocked())audio.resume();','if(canPlayAudio())audio.resume();')
for marker in ['        void replay()','        void saveSnapshot()','        void restoreSnapshot()','        void boxes(Canvas c']:
 meth(marker,'')
s=s.replace('if(!lastError.isEmpty()){shareReport();return true;}','if(!lastError.isEmpty())return true;').replace('Toast.makeText(MainActivity.this,replayStatus,Toast.LENGTH_LONG).show();','').replace('Elfen Fighting native test 008','Elfen Lied Fighting RC1')
p.write_text(s)
# Menu labels only; original scene translations are unchanged.
f=ROOT/'localization/ui.json';rows=json.loads(f.read_text());keys={row[0] for row in rows}
new=[['Новая игра','はじめから','New Game','Новая игра'],['Настройки','設定','Settings','Настройки'],['Главный экран','タイトルへ','Title Screen','Главный экран'],['Приёмы','技表','Moves','Приёмы'],['Настройки матча','対戦設定','Match Settings','Настройки матча'],['До 3 побед','3本先取','First to 3 wins','До 3 побед'],['Таймер: 60','タイマー: 60','Timer: 60','Таймер: 60'],['Открой паузу, чтобы вернуться на главный экран','ポーズからタイトルへ戻れます','Open Pause to return to the title screen','Открой паузу, чтобы вернуться на главный экран'],['Передвинь элементы. Нажми паузу, чтобы закончить.','配置を調整したらポーズを押してください。','Drag the controls. Press Pause when finished.','Передвинь элементы. Нажми паузу, чтобы закончить.'],['Не удалось продолжить игру','ゲームを続行できません','Unable to continue the game','Не удалось продолжить игру'],['Перезапусти приложение','アプリを再起動してください','Please restart the app','Перезапусти приложение'],['Адрес сервера комнат','ルームサーバーのアドレス','Room server address','Адрес сервера комнат'],['Введи адрес сервера, к которому подключается второй игрок.','対戦相手と同じサーバーのアドレスを入力してください。','Enter the same server address as the other player.','Введи адрес сервера, к которому подключается второй игрок.']]
for row in new:
 if row[0] not in keys:rows.append(row)
f.write_text(json.dumps(rows,ensure_ascii=False,indent=2)+'\n')
for name in ['app/build.gradle','app/src/main/AndroidManifest.xml']:
 f=ROOT/name;f.write_text(f.read_text().replace('versionCode 12','versionCode 13').replace('versionCode="12"','versionCode="13"').replace('0.0.8-languages','1.0.0-rc1').replace('Elfen Fighting · test 008','Elfen Lied Fighting'))
f=ROOT/'tools/build_android.py';f.write_text(f.read_text().replace('elfen-008.apk','elfen-rc1.apk'))
f=ROOT/'tools/check_apk.py';f.write_text(f.read_text().replace('TEST008','RC1').replace('elfen-008.apk','elfen-rc1.apk').replace("versionCode='12' versionName='0.0.8-languages'","versionCode='13' versionName='1.0.0-rc1'").replace('apk_validation_008.json','apk_validation_rc1.json'))
print('RC1 Activity/menu/lifecycle changes restored; combat sources untouched')
