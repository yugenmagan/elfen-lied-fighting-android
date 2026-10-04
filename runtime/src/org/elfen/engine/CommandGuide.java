package org.elfen.engine;

import java.util.*;
import static org.elfen.engine.Pack.*;

/** Read-only UI projection of PLAYER command records; never supplies combat input. */
public final class CommandGuide {
    public final int buttons, unsupported;
    public final List<Entry> entries;
    public static final class Step {
        public final int direction, buttons, holdFrames;
        Step(int flags,int amount){direction=flags&15;buttons=flags&0x3f0;holdFrames=(flags>>>14)==2?amount:0;}
        public String notation(){
            String[] directions={"","•","→","↘","↓","↙","←","↖","↑","↗","(← / ↙ / ↖)","(↑ / ↖ / ↗)","(→ / ↘ / ↗)","(↓ / ↙ / ↘)"};
            String s=directions[direction];
            for(int b=0;b<6;b++)if((buttons&(16<<b))!=0)s+=(s.isEmpty()?"":" + ")+(char)('A'+b);
            if(s.isEmpty())s="любой ввод";
            return holdFrames==0?s:"["+s+": держать "+holdFrames+" кадр.]";
        }
    }
    public static final class Entry {
        public final int index;public final String name, notation, stances;public final List<Step> steps;
        Entry(int index,Pack.Command c,List<Step> steps){
            this.index=index;name=c.name;this.steps=Collections.unmodifiableList(steps);
            StringBuilder n=new StringBuilder();for(Step s:steps){if(n.length()>0)n.append("  ,  ");n.append(s.notation());}notation=n.toString();
            String[] names={"в воздухе","стоя близко","стоя далеко","из приседа"};StringBuilder a=new StringBuilder();
            for(int i=0;i<4;i++)if(c.skills[i]!=0){if(a.length()>0)a.append(" · ");a.append(names[i]);}stances=a.toString();
        }
    }
    public CommandGuide(Pack p){
        ArrayList<Entry> out=new ArrayList<>();int mask=0,missing=0;
        if(!p.kind.equals(".player"))throw new IllegalArgumentException("PLAYER guide required");
        for(int ci=0;ci<p.commands.length;ci++){
            Pack.Command c=p.commands[ci];boolean target=false;for(int skill:c.skills)target|=skill!=0;
            if(c.time==0||!target)continue;
            if(c.steps.length!=20||c.amounts.length!=10||c.skills.length!=4)throw new IllegalArgumentException("Malformed command "+p.id+":"+ci);
            int end=9;while(end>=0&&(u16(c.steps,2*end)&0x2000)==0)end--;
            boolean known=end>=0;ArrayList<Step> steps=new ArrayList<>();
            for(int j=0;j<=end;j++){
                int flags=u16(c.steps,2*j),mode=flags>>>14;mask|=flags&0x3f0;
                if((flags&15)>13||(mode!=0&&mode!=2)||(mode==2&&c.amounts[j]<=0))known=false;
                steps.add(new Step(flags,c.amounts[j]));
            }
            for(int skill:c.skills)if(skill<0||skill>=p.starts.length)throw new IllegalArgumentException("Command target "+p.id+":"+ci);
            if(known)out.add(new Entry(ci,c,steps));else missing++;
        }
        // In-script command branches and an explicit guard button also count.
        for(int pc=0;pc<p.code.length;pc+=16)if(u(p.code,pc)==36&&u(p.code,pc+4)>0){
            int end=4;while(end>=0&&(u16(p.code,pc+5+2*end)&0x2000)==0)end--;
            for(int j=0;j<=end;j++)mask|=u16(p.code,pc+5+2*j)&0x3f0;
        }
        if(p.settings.length==1785&&(i32(p.settings,1766)&8)!=0){int b=u(p.settings,1753);if(b>5)throw new IllegalArgumentException("Guard button");mask|=16<<b;}
        buttons=mask;unsupported=missing;entries=Collections.unmodifiableList(out);
    }
}
