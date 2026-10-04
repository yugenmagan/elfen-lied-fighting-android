package org.elfen.engine;
import java.nio.file.*;import java.util.*;
public final class CharacterSelectTest {
    static int checks;
    static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
    static void tick(CharacterSelect s,int a,int b){s.step(new InputFrame(s.frame(),a,b));}
    static String view(CharacterSelect s){
        StringBuilder b=new StringBuilder(s.frame()+":"+s.cell(0)+":"+s.cell(1));
        for(BattleView.Sprite v:s.view())b.append('|').append(v.packId).append(',').append(v.image).append(',').append(v.x).append(',').append(v.y).append(',').append(v.offsetX).append(',').append(v.offsetY).append(',').append(v.flags).append(',').append(v.rgba).append(',').append(v.depth).append(',').append(v.left);
        return b.toString();
    }
    public static void main(String[] args)throws Exception{
        StoryCatalog catalog=StoryDataTest.load(args[0]);Map<String,Pack> packs=StoryDataTest.packs;Path research=Paths.get(args[1]);
        CharacterSelect.MenuInput input=new CharacterSelect.MenuInput();int nativeInputs=0,nativeGrid=0;
        for(String line:Files.readAllLines(research.resolve("select_input_x86_007.txt"))){int[] v=Arrays.stream(line.split(" ")).mapToInt(Integer::parseInt).toArray();input.step(new int[]{v[0]});check(input.pressed==v[1]&&input.repeated==v[2],"EXE menu input "+nativeInputs+":"+line);nativeInputs++;}
        for(String line:Files.readAllLines(research.resolve("select_grid_x86_007.txt"))){int[] v=Arrays.stream(line.split(" ")).mapToInt(Integer::parseInt).toArray();check(CharacterSelect.navigate(v[0],2,5,v[1])==v[2],"EXE wrap "+line);nativeGrid++;}
        CharacterSelect s=new CharacterSelect(catalog,false,"0170","0104");
        String[] ids={"0122","0128","0114","0110","0156","0108","0170","0168","0136","0104"};
        check(s.columns==2&&s.rows==5&&s.gridX==218&&s.gridY==50&&s.spacingX==150&&s.spacingY==83,"KGT grid");check(s.backgroundPack().id.equals("0064"),"DEMO reference");
        for(int n=0;n<10;n++)check(s.character(n).id.equals(ids[n])&&s.available(n),"KGT roster "+n);
        for(int[] size:new int[][]{{640,480},{800,480},{1280,720},{1920,1080},{2340,1080},{2400,1080},{2208,1768}}){
            SelectionLayout l=new SelectionLayout(size[0],size[1]);check(Math.abs((l.right-l.left)/(l.bottom-l.top)-4f/3f)<.00001,"Aspect ratio");
            check(l.left>=0&&l.right<=size[0]&&l.top>=size[1]*.099f&&l.bottom<=size[1]*.861f,"Toolbar clearance");
            for(int n=0;n<10;n++){float x=l.left+(s.gridX+n%2*s.spacingX+29)*l.scale,y=l.top+(s.gridY+n/2*s.spacingY+29)*l.scale;check(l.cellAt(s,x,y)==n,"Touch centre");check(l.cellAt(s,x-24*l.scale,y+24*l.scale)==n,"Touch corner");}
            check(l.cellAt(s,0,0)==-1&&l.cellAt(s,size[0]/2f,size[1]-1)==-1,"Toolbar cannot focus cell");
        }
        SelectionControls controls=new SelectionControls();controls.step(s,new int[]{Input.A});check(s.confirmed(0)&&!s.confirmed(1),"P1 confirm");
        for(int f=0;f<150;f++)controls.step(s,new int[]{Input.A});check(!s.confirmed(1)&&!s.finished(),"Held confirm protection");
        controls.step(s,new int[]{0});controls.step(s,new int[]{Input.A,0});check(s.confirmed(1),"Quick touch tap");
        int sounds=0,ticks=0;while(!s.finished()){controls.step(s,new int[]{0});ticks++;for(BattleSimulation.Event e:s.events())if(e.packId.equals("0116")&&e.kind.equals("sound")){check(e.value==4,"KGT confirm sound");sounds++;}}
        check(ticks==101,"VS confirm counter");check(sounds==1,"Confirm sound once");
        boolean rejects=false;try{s.step(new InputFrame(s.frame(),0,0));}catch(IllegalArgumentException expected){rejects=true;}check(rejects,"Ended selection rejects step");
        s=new CharacterSelect(catalog,false,"0170","0104");tick(s,Input.A,Input.B);check(s.confirmed(0)&&s.confirmed(1),"Simultaneous independent players");check(!s.back()&&!s.confirmed(0)&&!s.confirmed(1),"Back to P1");check(s.back(),"Back leaves");
        s=new CharacterSelect(catalog,false,"0170","0104");controls=new SelectionControls();controls.step(s,new int[]{Input.A,0});controls.step(s,new int[]{Input.A,0});check(s.confirmed(1),"Two separate subframe taps");
        s=new CharacterSelect(catalog,false,"0170","0104");controls=new SelectionControls();controls.requireRelease();controls.step(s,new int[]{Input.A});check(!s.confirmed(0),"Home/Back release guard");controls.step(s,new int[]{0});controls.step(s,new int[]{Input.F});check(s.confirmed(0),"F confirm");
        MatchRules rules=new MatchRules(catalog.kgt,3,60,0,50);MatchController paused=new MatchController(packs.get("0170"),packs.get("0104"),packs.get("0080"),catalog.kgt,123,rules);String hash=paused.stateHash();
        for(int cell=0;cell<10;cell++){
            CharacterSelect one=new CharacterSelect(catalog,true,ids[cell],"0104"),two=new CharacterSelect(catalog,true,ids[cell],"0104");
            for(int f=0;f<180;f++){tick(one,0,0);tick(two,0,0);check(view(one).equals(view(two)),"Portrait determinism "+ids[cell]+":"+f);check(!one.finished(),"Neutral selection persists");for(BattleView.Sprite v:one.view())check(v.absolute,"No battle camera dependency");}
            check(one.selected(0).id.equals(ids[cell]),"Correct protagonist");tick(one,Input.A,0);int wait=0;while(!one.finished()){tick(one,0,0);wait++;}check(wait==102,"Story confirm delay");
            StoryController story=new StoryController(catalog,one.selected(0),19);check(story.mode()==StoryController.SCENE,"Original intro");
            for(int f=0;f<2000&&story.mode()!=StoryController.FIGHT;f++)story.step(new InputFrame(story.frame(),story.canAdvanceScene()?Input.A:0,0));
            check(story.mode()==StoryController.FIGHT&&story.fights()==1,"First story fight");for(int f=0;f<180;f++)story.step(new InputFrame(story.frame(),0,0));check(story.view().fighters.get(0).name.equals(one.selected(0).name),"Selected story fighter");
            CharacterSelect vs=new CharacterSelect(catalog,false,ids[cell],ids[cell]);tick(vs,Input.A,Input.B);while(!vs.finished())tick(vs,0,0);
            MatchController match=new MatchController(vs.selected(0),vs.selected(1),packs.get("0080"),catalog.kgt,123,rules);for(int f=0;f<180;f++)match.step(new InputFrame(match.frame(),0,0));
            check(match.view().fighters.get(0).name.equals(vs.selected(0).name)&&match.view().fighters.get(1).name.equals(vs.selected(1).name),"Mirror VS identity");
        }
        check(paused.stateHash().equals(hash),"Selection never mutates paused battle");
        s=new CharacterSelect(catalog,false,"0170","0104");Random random=new Random(7007);
        for(int f=0;f<2000;f++){if(f%3==0)s.focus(f%2,random.nextInt(10));tick(s,0,0);check(s.view().size()<20,"OO children freed on focus change");}
        System.out.println("PASS CharacterSelect: "+checks+" assertions; "+nativeInputs+" original EXE input frames; "+nativeGrid+" grid cases; 10 story intro->fight transitions; 10 VS mirrors; 7 screen ratios; confirmation/release/back, deterministic visuals, paused-battle isolation");
    }
}
