package org.elfen.engine;

import java.util.*;

/** Prediction/rollback coordinator. All calls belong to the simulation thread.
 * Network threads only queue immutable messages. Time and transport are external.
 * No incoming snapshot is trusted: both peers simulate the same numbered inputs.
 */
public final class RollbackSession {
    public static final int HISTORY=120, MAX_PREDICTION=18, HASH_INTERVAL=30;
    private final MatchController match;
    private final MatchHistory history;
    private final MatchReplay replay;
    private final int localPlayer;
    private final TreeMap<Integer,int[]> remote=new TreeMap<>();
    private final TreeMap<Integer,String> peerHashes=new TreeMap<>();
    private final ArrayDeque<BattleSimulation.Event> confirmedEvents=new ArrayDeque<>();
    private final TreeMap<Integer,List<BattleSimulation.Event>> effects=new TreeMap<>();
    private int contiguous=-1, committed=-1, checkedHash=-1, dirty=Integer.MAX_VALUE;
    private int baseRemoteMask, rollbacks, resimulated;

    public RollbackSession(MatchController match,int localPlayer){
        if(match.frame()!=0||localPlayer<0||localPlayer>1||match.rules.cpu1!=0||match.rules.cpu2!=0)
            throw new IllegalArgumentException("Network match starts at frame0 with two human players");
        this.match=match;this.localPlayer=localPlayer;history=new MatchHistory(match,HISTORY);replay=new MatchReplay(match);
    }
    public MatchController match(){return match;}
    public int localPlayer(){return localPlayer;}
    public int confirmedFrame(){return Math.min(contiguous,match.frame()-1)+1;}
    public int predictionDepth(){return match.frame()-confirmedFrame();}
    public int rollbackCount(){return rollbacks;}
    public int resimulatedFrames(){return resimulated;}
    public int checkedHashFrame(){return checkedHash;}
    public MatchReplay replay(){return replay;}
    public boolean finished(){return match.state().finished&&confirmedFrame()==match.frame();}
    public boolean canAdvance(){return !match.state().finished&&predictionDepth()<MAX_PREDICTION;}
    public String confirmedHash(int frame){
        if(frame<0||frame>confirmedFrame())throw new IllegalArgumentException("Unconfirmed state hash");
        return StateIO.hex(StateIO.hash(history.stateAt(frame)));
    }
    /** Exact transition arrays matter: an intra-tick press+release is not neutral. */
    public void receiveInput(int frame,int[] samples){
        int[] copy=new InputFrame(frame,samples,new int[]{0}).samples(0);
        if(frame<match.frame()-HISTORY||frame>match.frame()+HISTORY)throw new IllegalArgumentException("Remote input outside window: "+frame);
        int[] prior=remote.get(frame);
        if(prior!=null){if(!Arrays.equals(prior,copy))throw new IllegalArgumentException("Conflicting confirmed input "+frame);return;}
        if(frame<=committed)throw new IllegalArgumentException("Expired input "+frame);
        remote.put(frame,copy);
        while(remote.containsKey(contiguous+1))contiguous++;
        if(frame<match.frame()&&!Arrays.equals(history.inputAt(frame).samples(1-localPlayer),copy))dirty=Math.min(dirty,frame);
    }
    public void receiveHash(int frame,String hash){
        if(frame<0||frame>match.frame()+HISTORY||hash==null||!hash.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("Peer hash message");
        if(frame<=checkedHash)return;
        String old=peerHashes.put(frame,hash);if(old!=null&&!old.equals(hash))throw new IllegalArgumentException("Conflicting peer hash");
    }
    private int[] predictedRemote(int frame){
        int[] actual=remote.get(frame);if(actual!=null)return actual;
        Map.Entry<Integer,int[]> prior=remote.lowerEntry(frame);
        int[] samples=prior==null?null:prior.getValue();return new int[]{samples==null?baseRemoteMask:samples[samples.length-1]};
    }
    private InputFrame merged(int frame,int[] own,int[] other){return new InputFrame(frame,localPlayer==0?own:other,localPlayer==0?other:own);}
    /** Called even when paused for network or a predicted final KO. */
    public void reconcile(){
        if(dirty!=Integer.MAX_VALUE){
            int start=dirty,end=match.frame();InputFrame[] corrected=new InputFrame[end-start];
            for(int i=0;i<corrected.length;i++){int f=start+i;corrected[i]=merged(f,history.inputAt(f).samples(localPlayer),predictedRemote(f));}
            // Step individually to replace effect records. Never emit speculative/replayed audio.
            match.restore(history.stateAt(start));effects.tailMap(start).clear();
            for(InputFrame input:corrected){
                if(match.state().finished)break; // Correction can move the final KO earlier.
                history.advance(input);effects.put(input.frame,match.events());
            }
            rollbacks++;resimulated+=match.frame()-start;dirty=Integer.MAX_VALUE;
        }
        int confirmed=confirmedFrame();
        while(committed+1<confirmed){
            int f=++committed;replay.append(history.inputAt(f));
            List<BattleSimulation.Event> list=effects.remove(f);if(list!=null)confirmedEvents.addAll(list);
        }
        Iterator<Map.Entry<Integer,String>> it=peerHashes.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry<Integer,String> e=it.next();if(e.getKey()>confirmed)break;
            String actual=confirmedHash(e.getKey());if(!actual.equals(e.getValue()))throw new IllegalStateException("DESYNC frame "+e.getKey()+" local="+actual+" remote="+e.getValue());
            checkedHash=e.getKey();it.remove();
        }
        int cutoff=match.frame()-HISTORY;
        while(!remote.isEmpty()&&remote.firstKey()<cutoff){int[] dropped=remote.pollFirstEntry().getValue();baseRemoteMask=dropped[dropped.length-1];}
    }
    public InputFrame advance(int[] samples){
        reconcile();if(!canAdvance())throw new IllegalStateException("Prediction window exhausted or match ended");
        InputFrame frame=merged(match.frame(),samples,predictedRemote(match.frame()));
        history.advance(frame);effects.put(frame.frame,match.events());reconcile();return frame;
    }
    /** Only newly confirmed effects: consuming them has no effect on the state hash. */
    public List<BattleSimulation.Event> drainConfirmedEvents(){ArrayList<BattleSimulation.Event> out=new ArrayList<>(confirmedEvents);confirmedEvents.clear();return out;}
}
