package org.elfen.engine;
import java.util.*;
import static org.elfen.engine.Pack.*;

/** Original task10 (406fc0) presentation. KGT defines the grid, roster, cursors
 * and anchors. Numbered 60Hz input; immutable render output; no combat mutation. */
public final class CharacterSelect implements Script.Host {
    private static final int BASE=0x435470;
    public final StoryCatalog catalog;
    public final boolean story;
    public final int columns,rows,gridX,gridY,spacingX,spacingY;
    private final int[] portraitX=new int[2],portraitY=new int[2],cursorSkills=new int[4],cells=new int[2];
    private final boolean[] confirmed=new boolean[2];
    private final MenuInput[] inputs={new MenuInput(),new MenuInput()};
    private final Script[] cursors=new Script[2];
    private final ArrayList<Script>[] portraits;
    private final ArrayList<Script> scripts=new ArrayList<>();
    private final ArrayList<BattleSimulation.Event> events=new ArrayList<>();
    private final int[] globals=new int[16];
    private final DemoSession background;
    private int frame,confirmTicks;
    private boolean finished;

    @SuppressWarnings("unchecked")
    public CharacterSelect(StoryCatalog catalog,boolean story,String p1,String p2){
        this.catalog=catalog;this.story=story;
        portraits=(ArrayList<Script>[])new ArrayList<?>[]{new ArrayList<Script>(),new ArrayList<Script>()};
        byte[] tail=catalog.kgt.originalTail;
        if(tail.length<=0x4452cc-BASE+50)throw new IllegalArgumentException("Truncated KGT selection");
        gridX=setting(0x4452b0);gridY=setting(0x4452b2);spacingX=setting(0x4452b4);spacingY=setting(0x4452b6);
        columns=setting(0x4452b8);rows=setting(0x4452ba);
        if(columns<1||rows<1||columns*rows>50||spacingX<=0||spacingY<=0)throw new IllegalArgumentException("Invalid KGT grid");
        for(int p=0;p<2;p++){portraitX[p]=setting(0x4452bc+p*8);portraitY[p]=setting(0x4452be+p*8);}
        for(int n=0;n<4;n++){
            cursorSkills[n]=u16(tail,0x445250-BASE+n*2);
            if(cursorSkills[n]<=0||cursorSkills[n]>=catalog.kgt.starts.length-1)throw new IllegalArgumentException("Invalid KGT cursor");
        }
        for(int n=0;n<size();n++)if(available(n)){
            Pack p=character(n);if(p.builtins.length<=21||p.builtins[21]<=0)throw new IllegalArgumentException("Missing select portrait: "+p.id);
        }
        background=new DemoSession(catalog.screen(story?1:2));
        cells[0]=initialCell(p1);cells[1]=initialCell(p2);
        for(int p=0;p<playerCount();p++){cursor(p,false);portrait(p);}
    }
    private int setting(int address){return s16(catalog.kgt.originalTail,address-BASE);}
    public int size(){return columns*rows;}
    public int playerCount(){return story?1:2;}
    public int frame(){return frame;}
    public boolean finished(){return finished;}
    public int activePlayer(){return !story&&confirmed[0]?1:0;}
    public boolean confirmed(int p){return confirmed[p];}
    public int cell(int p){return cells[p];}
    public int cursorX(int p){return gridX+cells[p]%columns*spacingX;}
    public int cursorY(int p){return gridY+cells[p]/columns*spacingY;}
    public Pack character(int cell){checkCell(cell);return catalog.character(cell+1);}
    public Pack selected(int p){return character(cells[p]);}
    public Pack backgroundPack(){return background.pack;}
    public boolean available(int cell){checkCell(cell);return (u(catalog.kgt.originalTail,0x4452cc-BASE+cell)&(story?1:2))!=0;}
    private void checkCell(int cell){if(cell<0||cell>=size())throw new IllegalArgumentException("Selection cell: "+cell);}
    private int initialCell(String id){
        int first=-1;for(int n=0;n<size();n++)if(available(n)){if(first<0)first=n;if(character(n).id.equals(id))return n;}
        if(first<0)throw new IllegalArgumentException("No selectable characters");return first;
    }
    /** Touch focus is applied before the next numbered presentation tick. */
    public void focus(int p,int cell){
        checkCell(cell);if(p<0||p>=playerCount())throw new IllegalArgumentException("Selection player");
        if(finished||confirmed[p]||cells[p]==cell)return;cells[p]=cell;portrait(p);
    }
    public boolean back(){
        if(finished)return true;
        if(!story&&confirmed[0]){for(int p=0;p<2;p++){confirmed[p]=false;cursor(p,false);}confirmTicks=0;return false;}
        return true;
    }
    /** Original helper 406e70, in the original UP/DOWN/LEFT/RIGHT order. */
    public static int navigate(int cell,int cols,int rows,int mask){
        int x=cell%cols,y=cell/cols;
        if((mask&Input.UP)!=0)y=(y+rows-1)%rows;if((mask&Input.DOWN)!=0)y=(y+1)%rows;
        if((mask&Input.LEFT)!=0)x=(x+cols-1)%cols;if((mask&Input.RIGHT)!=0)x=(x+1)%cols;
        return y*cols+x;
    }
    public void step(InputFrame input){
        if(input.frame!=frame||finished)throw new IllegalArgumentException("Selection input frame/state");
        events.clear();background.step(new InputFrame(background.frame(),0,0));events.addAll(background.events());
        // OO-created tasks first execute next tick.
        ArrayList<Script> order=new ArrayList<>(scripts);order.sort((a,b)->Integer.compare(a.depth,b.depth));
        for(Script s:order)if(!s.ended)s.animationTick(false);for(Script s:scripts)if(!s.ended)s.integrate();
        boolean already=confirmed[0]&&(story||confirmed[1]);
        for(int p=0;p<playerCount();p++){
            inputs[p].step(input.samples(p));cursors[p].x=cursorX(p)<<16;cursors[p].y=cursorY(p)<<16;
            if(confirmed[p])continue;
            int next=navigate(cells[p],columns,rows,inputs[p].repeated);
            if(!story&&accept(p))continue; // VS checks confirmation before navigation.
            boolean moved=next!=cells[p];if(moved)focus(p,next);if(story&&!moved)accept(p);
        }
        if(confirmed[0]&&(story||confirmed[1])&&(!story||already))if(confirmTicks++>100)finished=true;
        frame++;
    }
    private boolean accept(int p){
        if((inputs[p].pressed&0x3f0)==0||!available(cells[p]))return false;
        confirmed[p]=true;cursor(p,true);return true;
    }
    private void cursor(int p,boolean accepted){
        if(cursors[p]!=null)scripts.remove(cursors[p]);Script s=new Script(catalog.kgt,this,globals,false);
        s.absolute=true;s.depth=101;s.x=cursorX(p)<<16;s.y=cursorY(p)<<16;
        int skill=cursorSkills[p+(accepted?2:0)];s.start(skill,(s.pack.types[skill]&32)==0);scripts.add(s);cursors[p]=s;
    }
    private void portrait(int p){
        scripts.removeAll(portraits[p]);portraits[p].clear();if(!available(cells[p]))return;
        Pack pack=selected(p);Script s=new Script(pack,this,globals,false);s.absolute=true;s.depth=80;s.facingLeft=p==1;
        // Original world Y +480 cancels the selection camera at Y=480.
        s.x=portraitX[p]<<16;s.y=portraitY[p]<<16;int skill=pack.builtins[21];s.start(skill,(pack.types[skill]&32)==0);
        portraits[p].add(s);scripts.add(s);
    }
    public List<BattleView.Sprite> view(){
        ArrayList<BattleView.Sprite> out=new ArrayList<>(background.view());
        for(int n=0;n<scripts.size();n++){Script s=scripts.get(n);if(!s.ended&&s.image>=0)out.add(new BattleView.Sprite(1024+n,s,s.pack==catalog.kgt));}
        out.sort((a,b)->{int d=Integer.compare(a.depth,b.depth);return d!=0?d:Integer.compare(a.id,b.id);});return Collections.unmodifiableList(out);
    }
    public List<Pack> audioPacks(){
        ArrayList<Pack> out=new ArrayList<>(Arrays.asList(catalog.kgt,background.pack));
        for(int p=0;p<playerCount();p++)if(available(cells[p]))out.add(selected(p));return Collections.unmodifiableList(out);
    }
    public List<BattleSimulation.Event> events(){return Collections.unmodifiableList(new ArrayList<>(events));}
    public boolean explicitEnd(Script s){return true;} // Original non-character E terminates.
    public void sound(Pack p,int n){
        if(n<0||n>=p.sounds.length||n>0&&p.sounds[n].isEmpty())throw new IllegalStateException("Select sound: "+p.id+":"+n);
        if(n>0)events.add(new BattleSimulation.Event(frame,-1,events.size(),"sound",p.id,n,null));
    }
    public void gauge(Script s,int a,int b,int c,int d){throw s.fault("Selection cannot mutate combat gauges");}
    public void spawn(Script owner,byte[] b){
        int skill=u16(b,2),flags=u(b,1),number=u(b,12);
        if(number!=0)throw owner.fault("Unaudited selection OO number: "+number);if(skill==0)return;
        if(scripts.size()>=1024)throw owner.fault("Selection object limit");
        Script s=new Script(owner.pack,this,globals,false);s.absolute=true;int x=s16(b,8)<<16,y=s16(b,10)<<16;
        s.x=((flags&64)!=0?0:owner.x)+(owner.facingLeft?-x:x);s.y=((flags&64)!=0?0:owner.y)+y;
        s.parent=owner;s.controller=owner.controller;s.charVars=owner.charVars;s.followParent=(flags&32)!=0;s.parentX=x;s.parentY=y;s.facingLeft=owner.facingLeft;
        s.depth=(flags&3)==2?u(b,13):(flags&3)==1?Math.min(127,owner.depth+1):Math.max(10,owner.depth-1);
        s.start(skill,(s.pack.types[skill]&32)==0);s.pc+=u(b,4);
        boolean found=false;for(ArrayList<Script> group:portraits)if(group.contains(owner)){group.add(s);found=true;break;}
        if(!found)throw owner.fault("Unowned selection child");scripts.add(s);
    }
    /** Original repeat routine 414770: 50 initial ticks then 5. Preserve fast taps. */
    public static final class MenuInput {
        private int previous,countdown;public int pressed,repeated;
        public void step(int[] samples){
            if(samples.length==0)throw new IllegalArgumentException("Empty menu input");
            pressed=0;int last=previous;for(int mask:samples){pressed|=mask&~last;last=mask;}
            int mask=samples[samples.length-1];
            if(mask!=0&&mask==previous){repeated=--countdown==0?mask:0;if(countdown==0)countdown=5;}
            else{countdown=50;repeated=mask;if((previous&3)!=0)repeated&=~3;if((previous&12)!=0)repeated&=~12;}
            previous=mask;
        }
    }
}
