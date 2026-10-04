package org.elfen.engine;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Explicit animation/input test session. Not a combat simulator or a story mode. */
public final class PreviewSession implements Script.Host {
    public interface Audio {void play(Pack p,int sound);}
    public final List<Script> backgrounds=new ArrayList<>(),objects=new ArrayList<>();
    private final List<Script> spawned=new ArrayList<>();
    private final java.util.IdentityHashMap<Script,Script[]> slots=new java.util.IdentityHashMap<>();
    public final Script player,partner;
    public final Pack stage;
    public final int[] globals=new int[16];
    public final Commands commands=new Commands();
    public Audio audio;
    public int frames,mask,cameraX=320,cameraY=480,recognized=-1;
    private int movement=0;private boolean action=false,airborne=false;
    public String lastCommand="",error="";
    public PreviewSession(Pack p,Pack other,Pack stage){
        this.stage=stage;
        for(int i=1;i<stage.starts.length;i++){
            if((stage.types[i]&3)==3||stage.starts[i]>=stage.end(i))continue;
            Script bg=new Script(stage,this,globals,false);bg.start(i,(stage.types[i]&32)==0);backgrounds.add(bg);
        }
        player=new Script(p,this,globals,true);partner=new Script(other,this,globals,true);
        player.x=390<<16;partner.x=890<<16;player.y=player.ground;partner.y=partner.ground;partner.facingLeft=true;
        player.start(p.builtin(0),true);partner.start(other.builtin(0),true);
    }
    public void tick(int input){
        if(!error.isEmpty())return;mask=input;frames++;player.input=input;commands.push(input);
        try{
            boolean air=player.y<player.ground||player.vy<0;
            if(airborne&&!air){action=false;movement=-1;}
            airborne=air;
            int dir=Input.direction(input,player.facingLeft);
            int stance=air?0:((input&Input.DOWN)!=0?3:1);
            recognized=commands.match(player.pack,player.facingLeft,stance);
            if(recognized>=0&&!action){
                Pack.Command cmd=player.pack.commands[recognized];player.start(cmd.skills[stance],false);action=true;lastCommand=cmd.name;
            }
            if(action&&player.ended){action=false;movement=-1;}
            if(!action){
                if(!air){
                    if((input&Input.UP)!=0){int i=dir==9?4:dir==7?5:3;player.start(player.pack.builtin(i),false);action=true;airborne=true;}
                    else {int next=(input&Input.DOWN)!=0?8:dir==2?1:dir==6?2:0;
                        if(next!=movement||player.ended){movement=next;player.start(player.pack.builtin(next),true);}}
                }else if(player.ended)player.start(player.pack.builtin(6),true);
            }
            for(Script bg:backgrounds)bg.tick();player.tick();partner.tick();
            for(Iterator<Script> it=objects.iterator();it.hasNext();){Script s=it.next();s.tick();if(s.ended)it.remove();}
            objects.addAll(spawned);spawned.clear();
            player.x=Math.max(0,Math.min(1280<<16,player.x));
            cameraX=Math.max(0,Math.min(640,((player.x/65536+partner.x/65536)/2+cameraX-320)/2));
            cameraY=Math.max(0,Math.min(480,((player.y/65536+partner.y/65536)/2+cameraY-320)/2));
        }catch(IllegalStateException ex){error=ex.getMessage();throw ex;}
    }
    public void faceOther(){player.facingLeft=!player.facingLeft;commands.clear();}
    public void sound(Pack p,int index){
        if(index<0||index>=p.sounds.length)throw new IllegalStateException("Invalid sound index "+p.id+":"+index);
        if(index>0&&p.sounds[index].isEmpty())throw new IllegalStateException("Empty referenced sound "+p.id+":"+index);
        if(index>0&&audio!=null)audio.play(p,index);
    }
    public void spawn(Script owner,byte[] b){
        int skill=Pack.u16(b,2),block=Pack.u(b,4),flags=Pack.u(b,1),x=Pack.s16(b,8),y=Pack.s16(b,10),number=Pack.u(b,12);
        Script[] table=slots.get(owner.controller);if(table==null){table=new Script[10];slots.put(owner.controller,table);}
        if(number>=10)throw owner.fault("Object slot "+number);
        if((flags&4)==0&&table[number]!=null&&!table[number].ended){
            int branch=Pack.u16(b,5);if(branch>0){owner.jump(branch,Pack.u(b,7));return;}
            table[number].ended=true;table[number]=null;
        }
        if(skill==0)return;
        if(objects.size()+spawned.size()>=1024)throw owner.fault("Object limit exceeded");
        Script s=new Script(owner.pack,this,globals,false);s.absolute=(flags&64)!=0;
        s.x=(s.absolute?0:owner.x)+(x<<16)*(s.absolute||!owner.facingLeft?1:-1);s.y=(s.absolute?0:owner.y)+(y<<16);
        s.controller=owner.controller;s.charVars=owner.charVars;s.parent=owner;s.followParent=(flags&32)!=0;s.parentX=x<<16;s.parentY=y<<16;
        s.facingLeft=owner.facingLeft;s.depth=(flags&3)==2?Pack.u(b,13):(flags&3)==1?Math.min(127,owner.depth+1):Math.max(10,owner.depth-1);
        s.start(skill,false);s.pc+=block;spawned.add(s);if((flags&4)==0)table[number]=s;
    }
    public void gauge(Script owner,int a,int b,int c,int d){
        owner.life=Math.max(0,Math.min(owner.pack.life(),owner.life+a));owner.special+=b;
        Script other=owner==player?partner:player;other.life=Math.max(0,Math.min(other.pack.life(),other.life+c));other.special+=d;
    }
}
