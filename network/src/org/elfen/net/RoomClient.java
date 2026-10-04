package org.elfen.net;
import java.io.*;import java.net.*;import java.util.concurrent.*;import javax.net.ssl.*;

/** Network I/O stays off the Android/simulation thread; queues are bounded. */
public final class RoomClient implements AutoCloseable {
    private final BlockingQueue<Protocol.Packet> incoming=new ArrayBlockingQueue<>(512),outgoing=new ArrayBlockingQueue<>(512);
    private final String host;private final int port;private final SSLSocketFactory factory;
    private volatile Socket socket;private volatile boolean closed;private volatile String failure="";
    private volatile long lastReceiveNanos=System.nanoTime();
    private RoomClient(String host,int port,SSLSocketFactory factory){this.host=host;this.port=port;this.factory=factory;}
    public static RoomClient connect(String endpoint){
        URI uri=URI.create(endpoint);if(!"tls".equals(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null||(uri.getPath()!=null&&!uri.getPath().isEmpty()))throw new IllegalArgumentException("Адрес сервера: tls://имя:порт");
        return open(uri.getHost(),uri.getPort()<0?443:uri.getPort(),(SSLSocketFactory)SSLSocketFactory.getDefault());
    }
    /** Explicit trust store for isolated TLS tests; never a trust-all callback. */
    public static RoomClient open(String host,int port,SSLSocketFactory factory){if(port<1||port>65535||factory==null)throw new IllegalArgumentException("TLS endpoint");RoomClient client=new RoomClient(host,port,factory);client.worker("elfen-network-reader",client::readLoop);return client;}
    private void worker(String name,Runnable work){Thread t=new Thread(work,name);t.setDaemon(true);t.start();}
    private void readLoop(){
        try{
            Socket raw=new Socket();socket=raw;raw.connect(new InetSocketAddress(host,port),10000);if(closed){raw.close();return;}
            SSLSocket tls=(SSLSocket)factory.createSocket(raw,host,port,true);socket=tls;
            SSLParameters parameters=tls.getSSLParameters();parameters.setEndpointIdentificationAlgorithm("HTTPS");tls.setSSLParameters(parameters);tls.setEnabledProtocols(new String[]{"TLSv1.3","TLSv1.2"});tls.setTcpNoDelay(true);tls.setSoTimeout(20000);tls.startHandshake();if(closed){tls.close();return;}
            worker("elfen-network-writer",this::writeLoop);
            while(!closed){Protocol.Packet p=Protocol.read(tls.getInputStream());lastReceiveNanos=System.nanoTime();if(p.type==Protocol.PONG)continue;if(!incoming.offer(p))throw new IOException("Incoming network queue overflow");}
        }catch(Exception e){fail(e);}
    }
    private void writeLoop(){try{
        while(!closed){Protocol.Packet p=outgoing.poll(2,TimeUnit.SECONDS);if(p==null)p=Protocol.empty(Protocol.PING);Protocol.write(socket.getOutputStream(),p);if(System.nanoTime()-lastReceiveNanos>20_000_000_000L)throw new IOException("Сервер не отвечает");}
    }catch(Exception e){fail(e);}}
    private void fail(Exception e){if(!closed){failure=e.getClass().getSimpleName()+": "+String.valueOf(e.getMessage());close();}}
    public String failure(){return failure;}
    public boolean closed(){return closed;}
    public Protocol.Packet poll(){return incoming.poll();}
    public void send(Protocol.Packet p){if(closed)throw new IllegalStateException("Соединение закрыто: "+failure);if(!outgoing.offer(p)){fail(new IOException("Outgoing network queue overflow"));throw new IllegalStateException(failure);}}
    public void create(String build,String character,String stage,int wins,int time){send(Protocol.packet(Protocol.CREATE,o->{o.writeInt(Protocol.VERSION);o.writeUTF(build);o.writeUTF(character);o.writeUTF(stage);o.writeInt(wins);o.writeInt(time);}));}
    public void join(String build,String character,String code){send(Protocol.packet(Protocol.JOIN,o->{o.writeInt(Protocol.VERSION);o.writeUTF(build);o.writeUTF(character);o.writeUTF(code);}));}
    @Override public void close(){closed=true;Socket s=socket;if(s!=null)try{s.close();}catch(IOException e){if(failure.isEmpty())failure="Socket close: "+e;}outgoing.clear();}
}
