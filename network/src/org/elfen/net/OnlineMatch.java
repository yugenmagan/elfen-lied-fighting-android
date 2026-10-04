package org.elfen.net;
import java.io.*;import java.util.*;import org.elfen.engine.*;

/** Shared desktop/Android room flow. Only pump/advance may modify the simulation. */
public final class OnlineMatch implements AutoCloseable {
    public static final int WAITING=0,PREPARING=1,RUNNING=2,COMPLETE=3,DISCONNECTED=4,FAILED=5;
    private final RoomClient client;private final Map<String,Pack> packs;
    private RollbackSession rollback;private GameConfig config;private int state=WAITING,sentHash=-1;
    private String code="",message="Соединение с сервером";
    public OnlineMatch(RoomClient client,Map<String,Pack> packs){this.client=client;this.packs=packs;}
    public void create(String character,String stage,int wins,int time){client.create(GameConfig.contentIdentity(packs),character,stage,wins,time);}
    public void join(String character,String code){client.join(GameConfig.contentIdentity(packs),character,code);}
    public int state(){return state;}public String code(){return code;}public String message(){return message;}
    public GameConfig config(){return config;}public RollbackSession rollback(){return rollback;}
    public boolean canAdvance(){return state==RUNNING&&rollback.canAdvance();}
    public void pump(){
        if(state==FAILED||state==DISCONNECTED)return;
        try{
            Protocol.Packet p;int read=0;
            while((p=client.poll())!=null){if(++read>512)throw new IOException("Network queue flood");DataInputStream in=p.data();
                switch(p.type){
                    case Protocol.CREATED:if(state!=WAITING)throw new IOException("Unexpected room code");code=Protocol.text(in,8);message="Код комнаты: "+code+". Ожидание второго игрока";break;
                    case Protocol.CONFIG:
                        if(state!=WAITING)throw new IOException("Repeated match configuration");int slot=in.readUnsignedByte();config=GameConfig.read(in);rollback=new RollbackSession(config.create(packs),slot);state=PREPARING;message="Проверка игровых данных";client.send(Protocol.message(Protocol.READY,rollback.match().stateHash()));break;
                    case Protocol.START:if(state!=PREPARING)throw new IOException("Unexpected start");state=RUNNING;message="Бой";break;
                    case Protocol.REMOTE_INPUT:requireGame();rollback.receiveInput(Protocol.frame(in),Protocol.samples(in));break;
                    case Protocol.REMOTE_HASH:requireGame();rollback.receiveHash(Protocol.frame(in),Protocol.digest(in));break;
                    case Protocol.CLOSED:message=Protocol.text(in,1024);if(state!=COMPLETE)state=DISCONNECTED;client.close();break;
                    case Protocol.ERROR:message=Protocol.text(in,1024);state=FAILED;client.close();break;
                    default:throw new IOException("Unexpected server message "+p.type);
                }Protocol.end(in);
            }
            if(state==RUNNING||state==COMPLETE){rollback.reconcile();int frame=rollback.finished()?rollback.confirmedFrame():rollback.confirmedFrame()/RollbackSession.HASH_INTERVAL*RollbackSession.HASH_INTERVAL;if(frame>sentHash){client.send(Protocol.hash(Protocol.HASH,frame,rollback.confirmedHash(frame)));sentHash=frame;}
                if(rollback.finished()&&rollback.checkedHashFrame()>=rollback.match().frame()){state=COMPLETE;message="Матч завершён: хеши совпали";}else message=rollback.finished()?"Проверка результата":rollback.canAdvance()?"Бой":"Ожидание соединения";
            }
            if(state!=DISCONNECTED&&state!=FAILED&&state!=COMPLETE&&!client.failure().isEmpty())throw new IOException(client.failure());
        }catch(IOException|IllegalArgumentException|IllegalStateException e){state=FAILED;message=e.toString();client.close();}
    }
    private void requireGame()throws IOException{if(state!=RUNNING&&state!=COMPLETE)throw new IOException("Game message before start");}
    public void advance(int[] samples){
        if(!canAdvance())throw new IllegalStateException("Online simulation not ready");
        int frame=rollback.match().frame();client.send(Protocol.input(Protocol.INPUT,frame,samples));rollback.advance(samples);
    }
    @Override public void close(){client.close();if(state!=FAILED){state=DISCONNECTED;message="Соединение закрыто";}}
}
