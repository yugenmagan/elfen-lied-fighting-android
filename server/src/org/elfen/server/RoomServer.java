package org.elfen.server;

import org.elfen.net.*;
import java.io.*;import java.net.*;import java.nio.file.*;import java.security.*;import java.util.*;import java.util.concurrent.*;import javax.net.ssl.*;

/** Private two-player relay. No assets, accounts, simulation or state replacement.
 * Standard JDK TLS, bounded rooms/queues, input ordering, deterministic handshake.
 */
public final class RoomServer implements AutoCloseable {
    private static final String ALPHABET="ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final SSLServerSocket listener;private final SecureRandom random=new SecureRandom();
    private final HashMap<String,Room> rooms=new HashMap<>();private final HashSet<Peer> peers=new HashSet<>();
    private final HashMap<String,long[]> arrivals=new HashMap<>();
    private volatile boolean closed;private final ScheduledExecutorService expiry=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"room-expiry");t.setDaemon(true);return t;});
    private static final class Room {String code,build,p1,stage;int wins,time,seed;Peer[] players=new Peer[2];boolean started;long created=System.nanoTime();}

    public RoomServer(SSLServerSocket listener){this.listener=listener;listener.setEnabledProtocols(new String[]{"TLSv1.3","TLSv1.2"});}
    public int port(){return listener.getLocalPort();}
    public void start(){thread("room-accept",this::accept);expiry.scheduleAtFixedRate(this::expire,30,30,TimeUnit.SECONDS);}
    private static void thread(String name,Runnable work){Thread t=new Thread(work,name);t.setDaemon(true);t.start();}
    private void accept(){while(!closed)try{
        SSLSocket socket=(SSLSocket)listener.accept();String ip=socket.getInetAddress().getHostAddress();
        synchronized(this){
            long now=System.nanoTime();arrivals.entrySet().removeIf(e->now-e.getValue()[0]>60_000_000_000L);
            long[] limit=arrivals.get(ip);if(limit==null){limit=new long[]{now,0};arrivals.put(ip,limit);}
            long same=peers.stream().filter(p->p.ip.equals(ip)).count();
            if(peers.size()>=64||same>=8||limit[1]++>=30){socket.close();continue;}
            Peer peer=new Peer(socket,ip);peers.add(peer);peer.start();
        }
    }catch(IOException e){if(!closed)System.err.println("Accept failed: "+e.getClass().getSimpleName());}}
    private synchronized void expire(){long now=System.nanoTime();for(Room r:new ArrayList<>(rooms.values()))if(now-r.created>(r.started?7_200_000_000_000L:600_000_000_000L))endRoom(r,"Срок комнаты истёк");}
    private String code(){StringBuilder s=new StringBuilder();for(int i=0;i<8;i++)s.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));return s.toString();}
    private synchronized void received(Peer peer,Protocol.Packet packet)throws IOException{
        DataInputStream in=packet.data();
        if(packet.type==Protocol.PING){Protocol.end(in);peer.send(Protocol.empty(Protocol.PONG));return;}
        if(packet.type==Protocol.LEAVE){Protocol.end(in);peer.terminal(Protocol.CLOSED,"Игрок вышел");return;}
        if(packet.type==Protocol.CREATE||packet.type==Protocol.JOIN){
            if(peer.room!=null)throw new IOException("Уже в комнате");if(in.readInt()!=Protocol.VERSION)throw new IOException("Версия протокола");String build=Protocol.digest(in),character=Protocol.packId(in);
            if(packet.type==Protocol.CREATE){
                if(rooms.size()>=32)throw new IOException("Все комнаты заняты");String stage=Protocol.packId(in);int wins=in.readInt(),time=in.readInt();Protocol.end(in);
                if(wins<1||wins>9||time<0||time>999)throw new IOException("Правила матча");Room room=new Room();do{room.code=code();}while(rooms.containsKey(room.code));room.build=build;room.p1=character;room.stage=stage;room.wins=wins;room.time=time;room.seed=random.nextInt();room.players[0]=peer;
                peer.room=room;peer.player=0;rooms.put(room.code,room);peer.send(Protocol.message(Protocol.CREATED,room.code));
            }else{
                String code=Protocol.text(in,8);Protocol.end(in);if(!code.matches("[A-HJ-NP-Z2-9]{8}"))throw new IOException("Код комнаты");Room room=rooms.get(code);if(room==null||room.players[1]!=null)throw new IOException("Комната не найдена или занята");if(!build.equals(room.build))throw new IOException("Разные версии игры или игровых данных");
                peer.room=room;peer.player=1;room.players[1]=peer;GameConfig config=new GameConfig(build,room.p1,character,room.stage,room.seed,room.wins,room.time);
                for(int n=0;n<2;n++){final int slot=n;room.players[n].send(Protocol.packet(Protocol.CONFIG,o->{o.writeByte(slot);config.write(o);}));}
            }return;
        }
        Room room=peer.room;if(room==null||room.players[1]==null)throw new IOException("Ожидается второй игрок");Peer other=room.players[1-peer.player];
        if(packet.type==Protocol.READY){
            if(room.started||peer.ready!=null)throw new IOException("Повтор READY");peer.ready=Protocol.digest(in);Protocol.end(in);
            if(other.ready!=null){if(!other.ready.equals(peer.ready))throw new IOException("DESYNC начального состояния");room.started=true;peer.send(Protocol.empty(Protocol.START));other.send(Protocol.empty(Protocol.START));}return;
        }
        if(!room.started)throw new IOException("Матч ещё не начат");
        if(packet.type==Protocol.INPUT){
            int frame=Protocol.frame(in);int[] samples=Protocol.samples(in);Protocol.end(in);
            if(frame!=peer.nextFrame||frame>other.nextFrame+120)throw new IOException("Порядок сетевого ввода");peer.nextFrame++;other.send(Protocol.input(Protocol.REMOTE_INPUT,frame,samples));
        }else if(packet.type==Protocol.HASH){
            int frame=Protocol.frame(in);String hash=Protocol.digest(in);Protocol.end(in);
            if(frame<=peer.lastHash||frame>Math.min(peer.nextFrame,other.nextFrame))throw new IOException("Неподтверждённый hash");peer.lastHash=frame;other.send(Protocol.hash(Protocol.REMOTE_HASH,frame,hash));
        }else throw new IOException("Неизвестное сообщение");
    }
    private synchronized void disconnected(Peer p){peers.remove(p);Room r=p.room;if(r!=null&&rooms.remove(r.code)!=null){for(Peer other:r.players)if(other!=null&&other!=p)other.terminal(Protocol.CLOSED,"Соперник отключился");}}
    private synchronized void endRoom(Room r,String message){rooms.remove(r.code);for(Peer p:r.players)if(p!=null)p.terminal(Protocol.CLOSED,message);}
    @Override public synchronized void close(){closed=true;expiry.shutdownNow();try{listener.close();}catch(IOException e){System.err.println("Listener close failed");}for(Peer p:new ArrayList<>(peers))p.close();rooms.clear();}
    private final class Peer {
        final SSLSocket socket;final String ip;final BlockingQueue<Protocol.Packet> queue=new ArrayBlockingQueue<>(512);
        Room room;int player,nextFrame,lastHash=-1;String ready;volatile boolean dead,ending;
        Peer(SSLSocket socket,String ip){this.socket=socket;this.ip=ip;}
        void start(){thread("room-reader",()->{boolean handshake=false;try{socket.setSoTimeout(20000);socket.setTcpNoDelay(true);socket.startHandshake();handshake=true;thread("room-writer",this::writeLoop);while(!dead&&!ending)received(this,Protocol.read(socket.getInputStream()));}catch(Exception e){if(!handshake)close();else if(!dead)terminal(Protocol.ERROR,e instanceof EOFException?"Соединение закрыто":String.valueOf(e.getMessage()));}});}
        void writeLoop(){try{while(!dead){Protocol.Packet p=queue.poll(20,TimeUnit.SECONDS);if(p==null)throw new IOException("Idle connection");Protocol.write(socket.getOutputStream(),p);if(p.type==Protocol.CLOSED||p.type==Protocol.ERROR){close();return;}}}catch(Exception e){close();}}
        void send(Protocol.Packet p){if(!dead&&!queue.offer(p))close();}
        void terminal(int type,String message){if(dead||ending)return;ending=true;send(Protocol.message(type,message==null?"Ошибка соединения":message));}
        void close(){if(dead)return;dead=true;try{socket.close();}catch(IOException e){System.err.println("Peer close failed");}disconnected(this);}
    }
    public static SSLContext tls(Path keystore,char[] password)throws Exception{KeyStore store=KeyStore.getInstance("PKCS12");try(InputStream in=Files.newInputStream(keystore)){store.load(in,password);}KeyManagerFactory km=KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());km.init(store,password);SSLContext ctx=SSLContext.getInstance("TLS");ctx.init(km.getKeyManagers(),null,null);return ctx;}
    public static void main(String[] args)throws Exception{
        if(args.length!=4)throw new IllegalArgumentException("RoomServer BIND PORT PKCS12 PASSWORD_FILE");char[] password=new String(Files.readAllBytes(Paths.get(args[3])),java.nio.charset.StandardCharsets.UTF_8).trim().toCharArray();
        SSLContext context=tls(Paths.get(args[2]),password);Arrays.fill(password,'\0');SSLServerSocket socket=(SSLServerSocket)context.getServerSocketFactory().createServerSocket(Integer.parseInt(args[1]),64,InetAddress.getByName(args[0]));RoomServer server=new RoomServer(socket);Runtime.getRuntime().addShutdownHook(new Thread(server::close));server.start();System.out.println("Room relay listening on "+args[0]+":"+server.port());new CountDownLatch(1).await();
    }
}
