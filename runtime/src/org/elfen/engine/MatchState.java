package org.elfen.engine;
/** Detached render/UI description. No writable simulation reference. */
public final class MatchState {
    public final int frame,roundNumber,roundsToWin,score1,score2,timer,phase,winner,resultReason;
    public final boolean fighting,finished;
    MatchState(int frame,RoundController r){this.frame=frame;roundNumber=r.round;roundsToWin=r.rules.winsRequired;score1=r.wins1;score2=r.wins2;timer=r.timer<0?-1:r.timer/100;phase=r.state;fighting=phase==200;finished=phase==1000;winner=finished?(score1==score2?-1:score1>score2?0:1):r.winner;resultReason=r.reason;}
}
