package org.elfen.engine;
import java.nio.file.*;import java.util.*;

/** Two actual coordinators; delayed, duplicated, reordered inputs and restore/replay. */
public final class RollbackPeerTest {
    static Pack a,b,s,k;static int checks;
    static void check(boolean b,String message){checks++;if(!b)throw new AssertionError(message);}
    static MatchController make(int wins,int time){return new MatchController(a,b,s,k,77,new MatchRules(k,wins,time,0,0));}
    static int[] samples(int f,int p){int m=MatchRollbackTest.mask(f,p);return f%23==0?new int[]{m, m|Input.A,m}:new int[]{m};}
    static String effect(BattleSimulation.Event e){return e.frame+":"+e.emitter+":"+e.kind+":"+e.packId+":"+e.value+":"+Arrays.toString(e.instruction());}
    static final class Delivery {int to,frame;int[] samples;Delivery(int to,int frame,int[] samples){this.to=to;this.frame=frame;this.samples=samples;}}
    static void run(int delay,int jitter,boolean finalMatch)throws Exception{
        int wins=finalMatch?1:9,time=finalMatch?1:999;
        MatchController truth=make(wins,time);ArrayList<String> hashes=new ArrayList<>(),events=new ArrayList<>();hashes.add(truth.stateHash());
        int total=finalMatch?10000:3000;
        for(int f=0;f<total&&!truth.state().finished;f++){truth.step(new InputFrame(f,samples(f,0),samples(f,1)));hashes.add(truth.stateHash());for(BattleSimulation.Event e:truth.events())events.add(effect(e));}
        total=truth.frame();if(finalMatch)check(truth.state().finished,"Finite reference match");
        RollbackSession[] peers={new RollbackSession(make(wins,time),0),new RollbackSession(make(wins,time),1)};
        TreeMap<Integer,ArrayList<Delivery>> queue=new TreeMap<>();Random schedule=new Random(814);
        int[] sentHash={-1,-1};int ticks=0;
        int[] eventIndex={0,0};
        for(;ticks<total*4+500;ticks++){
            ArrayList<Delivery> delivery=queue.remove(ticks);
            if(delivery!=null){Collections.reverse(delivery);for(Delivery d:delivery){peers[d.to].receiveInput(d.frame,d.samples);peers[d.to].receiveInput(d.frame,d.samples);}}
            for(int p=0;p<2;p++){
                RollbackSession peer=peers[p];peer.reconcile();
                int confirmed=peer.confirmedFrame();check(peer.confirmedHash(confirmed).equals(hashes.get(confirmed)),"Confirmed prefix "+p+":"+confirmed);
                int hf=confirmed/30*30;if(hf>sentHash[p]){peers[1-p].receiveHash(hf,peer.confirmedHash(hf));sentHash[p]=hf;}
                for(BattleSimulation.Event e:peer.drainConfirmedEvents()){check(eventIndex[p]<events.size()&&events.get(eventIndex[p]++).equals(effect(e)),"Confirmed effect order, no rollback duplicates");}
                if(peer.match().frame()<total&&peer.canAdvance()){
                    int f=peer.match().frame();int[] own=samples(f,p);peer.advance(own);
                    int latency=delay+(jitter==0?0:schedule.nextInt(jitter+1));
                    // Simulate retransmission after a lost packet without modifying its contents.
                    if(f%83==0)latency+=8;
                    queue.computeIfAbsent(ticks+1+latency,n->new ArrayList<>()).add(new Delivery(1-p,f,own));
                }
            }
            if(peers[0].confirmedFrame()==total&&peers[1].confirmedFrame()==total)break;
        }
        for(int p=0;p<2;p++){RollbackSession peer=peers[p];for(BattleSimulation.Event e:peer.drainConfirmedEvents())check(eventIndex[p]<events.size()&&events.get(eventIndex[p]++).equals(effect(e)),"Final effects");check(eventIndex[p]==events.size(),"All confirmed effects delivered");
            check(peer.confirmedFrame()==total,"Flush before bounded deadline");check(peer.match().stateHash().equals(truth.stateHash()),"Final peer convergence");
            MatchReplay tape=MatchReplay.decode(peer.replay().encode());check(tape.length()==total,"Only confirmed inputs recorded");
            MatchController replay=make(wins,time);tape.restoreStart(replay);for(int n=0;n<tape.length();n++){replay.step(tape.input(n));check(replay.stateHash().equals(hashes.get(n+1)),"Canonical replay hash "+n);}
        }
        System.out.println("PASS paired rollback delayTicks="+delay+" jitter="+jitter+" final="+finalMatch+" frames="+total+" schedulerTicks="+ticks+" rollbacks="+peers[0].rollbackCount()+":"+peers[1].rollbackCount()+" hash="+truth.stateHash());
    }
    public static void main(String[] args)throws Exception{
        Path root=Paths.get(args[0]);Pack[] pp=new Pack[4];String[] ids={"0170","0104","0080","0116"};for(int n=0;n<4;n++)pp[n]=Pack.read(ids[n],Files.newInputStream(root.resolve(ids[n]+"/data.efp")));a=pp[0];b=pp[1];s=pp[2];k=pp[3];
        for(int delay:new int[]{3,6,9,12})run(delay,4,false);run(9,5,true);
        RollbackSession peer=new RollbackSession(make(1,60),0);for(int n=0;n<18;n++)peer.advance(new int[]{0});check(!peer.canAdvance(),"Bounded prediction stops on missing peer");
        boolean rejected=false;try{peer.receiveInput(200,new int[]{0});}catch(IllegalArgumentException e){rejected=true;}check(rejected,"Future input rejected");
        peer.receiveHash(0,"0000000000000000000000000000000000000000000000000000000000000000");rejected=false;try{peer.reconcile();}catch(IllegalStateException e){rejected=e.getMessage().startsWith("DESYNC");}check(rejected,"Desync is explicit, never replaced by received state");
        System.out.println("PASS RollbackPeerTest checks="+checks);
    }
}
