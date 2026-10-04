package org.elfen.engine;

import org.elfen.net.*;import org.elfen.server.RoomServer;
import java.io.*;import java.net.*;import java.nio.file.*;import java.security.*;import java.security.cert.*;import java.util.*;import javax.net.ssl.*;

/** Real TLS sockets with the exact client/coordinator compiled into Android. */
public final class NetworkTest {
    static int checks;static Map<String,Pack> packs;static SSLSocketFactory trust;static int port;
    static void check(boolean v,String why){checks++;if(!v)throw new AssertionError(why);}
    interface Until {boolean ready()throws Exception;}
    static void waitFor(Until until)throws Exception{long deadline=System.nanoTime()+15_000_000_000L;while(!until.ready()){if(System.nanoTime()>deadline)throw new AssertionError("Network deadline");Thread.sleep(1);}}
    static RoomClient connect(){return RoomClient.open("localhost",port,trust);}
    static String waitCode(RoomClient c)throws Exception{final String[] code={null};waitFor(()->{Protocol.Packet p=c.poll();if(p!=null){check(p.type==Protocol.CREATED,"Created response");code[0]=Protocol.text(p.data(),8);}return code[0]!=null;});return code[0];}
    static void reject(RoomClient client,String message)throws Exception{waitFor(()->{Protocol.Packet p=client.poll();return p!=null&&p.type==Protocol.ERROR||!client.failure().isEmpty();});client.close();System.out.println("PASS rejected "+message);}
    static void play(boolean endMatch)throws Exception{
        RoomClient hostWire=connect(),guestWire=connect();
        OnlineMatch host=new OnlineMatch(hostWire,packs),guest=new OnlineMatch(guestWire,packs);
        try{
            host.create("0170","0080",endMatch?1:9,endMatch?1:999);waitFor(()->{host.pump();return !host.code().isEmpty();});guest.join("0104",host.code());
            waitFor(()->{host.pump();guest.pump();if(host.state()==OnlineMatch.FAILED||guest.state()==OnlineMatch.FAILED)throw new AssertionError(host.message()+" / "+guest.message());return host.state()==OnlineMatch.RUNNING&&guest.state()==OnlineMatch.RUNNING;});
            check(host.rollback().localPlayer()==0&&guest.rollback().localPlayer()==1,"Assigned players");
            MatchController truth=host.config().create(packs);ArrayList<String> hashes=new ArrayList<>();hashes.add(truth.stateHash());
            int ticks=endMatch?10000:1800;for(int f=0;f<ticks&&!truth.state().finished;f++){truth.step(new InputFrame(f,RollbackPeerTest.samples(f,0),RollbackPeerTest.samples(f,1)));hashes.add(truth.stateHash());}final int total=truth.frame();
            OnlineMatch[] peers={host,guest};long deadline=System.nanoTime()+30_000_000_000L;int iterations=0;
            while(host.rollback().confirmedFrame()<total||guest.rollback().confirmedFrame()<total||(endMatch&&(host.state()!=OnlineMatch.COMPLETE||guest.state()!=OnlineMatch.COMPLETE))){
                for(int p=0;p<2;p++){
                    OnlineMatch peer=peers[p];
                    // Burst reads model delayed delivery at the consumer boundary, without changing bytes.
                    if(iterations%(p==0?3:5)==0)peer.pump();
                    if(peer.state()==OnlineMatch.FAILED||peer.state()==OnlineMatch.DISCONNECTED)throw new AssertionError(peer.message());
                    int confirmed=peer.rollback().confirmedFrame();check(peer.rollback().confirmedHash(confirmed).equals(hashes.get(confirmed)),"TLS confirmed prefix "+p+":"+confirmed);
                    if(peer.canAdvance()&&peer.rollback().match().frame()<total){int f=peer.rollback().match().frame();peer.advance(RollbackPeerTest.samples(f,p));}
                    peer.rollback().drainConfirmedEvents();
                }
                if(System.nanoTime()>deadline)throw new AssertionError("TLS simulation deadline "+host.message()+" / "+guest.message());iterations++;Thread.sleep(1);
            }
            for(OnlineMatch peer:peers){check(peer.rollback().match().stateHash().equals(truth.stateHash()),"TLS final hash");MatchReplay tape=MatchReplay.decode(peer.rollback().replay().encode());MatchController replay=peer.config().create(packs);tape.restoreStart(replay);for(int n=0;n<tape.length();n++)replay.step(tape.input(n));check(replay.stateHash().equals(truth.stateHash()),"TLS confirmed replay");}
            System.out.println("PASS TLS Create/Join + READY/hash + rollback frames="+total+" finished="+endMatch+" hashes="+truth.stateHash()+" rollbacks="+host.rollback().rollbackCount()+":"+guest.rollback().rollbackCount());
            if(!endMatch){host.close();waitFor(()->{guest.pump();return guest.state()==OnlineMatch.DISCONNECTED;});check(!guest.canAdvance(),"Disconnect never substitutes AI");System.out.println("PASS disconnect stops peer explicitly");}
            else {host.close();waitFor(()->{guest.pump();return guestWire.closed();});guest.pump();check(guest.state()==OnlineMatch.COMPLETE,"Confirmed result survives peer departure");check(guest.rollback().match().stateHash().equals(truth.stateHash()),"Departure preserves final state");System.out.println("PASS confirmed result survives peer departure");}
        }finally{host.close();guest.close();}
    }
    public static void main(String[] args)throws Exception{
        StoryDataTest.load(args[0]);packs=StoryDataTest.packs;
        KeyStore store=KeyStore.getInstance(KeyStore.getDefaultType());store.load(null,null);try(InputStream in=Files.newInputStream(Paths.get(args[2]))){store.setCertificateEntry("test-local",CertificateFactory.getInstance("X.509").generateCertificate(in));}
        TrustManagerFactory tm=TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());tm.init(store);SSLContext clientContext=SSLContext.getInstance("TLS");clientContext.init(null,tm.getTrustManagers(),null);trust=clientContext.getSocketFactory();
        SSLContext serverContext=RoomServer.tls(Paths.get(args[1]),"local-test-only".toCharArray());
        try(RoomServer server=new RoomServer((SSLServerSocket)serverContext.getServerSocketFactory().createServerSocket(0,50,InetAddress.getLoopbackAddress()))){
            port=server.port();server.start();play(false);play(true);
            String build=GameConfig.contentIdentity(packs);
            try(RoomClient host=connect()){host.create(build,"0170","0080",1,60);String code=waitCode(host);try(RoomClient wrong=connect()){wrong.join("0000000000000000000000000000000000000000000000000000000000000000","0104",code);reject(wrong,"different content build");}}
            try(RoomClient absent=connect()){absent.join(build,"0104","AAAAAAAA");reject(absent,"missing room");}
            try(RoomClient malformed=connect()){malformed.send(Protocol.empty(Protocol.INPUT));reject(malformed,"input before room");}
            try(RoomClient badHost=RoomClient.open("127.0.0.1",port,trust)){waitFor(()->!badHost.failure().isEmpty());check(badHost.failure().contains("SSL"),"TLS hostname verification");System.out.println("PASS TLS rejects wrong hostname");}
            try(RoomClient untrusted=RoomClient.open("localhost",port,(SSLSocketFactory)SSLSocketFactory.getDefault())){waitFor(()->!untrusted.failure().isEmpty());check(untrusted.failure().contains("SSL"),"TLS certificate verification");System.out.println("PASS TLS rejects untrusted certificate");}
        }
        for(byte[] bad:new byte[][]{{0,0,0,0},{0,0,32,0},{0,0,0,5,1,0}}){boolean fail=false;try{Protocol.read(new ByteArrayInputStream(bad));}catch(IOException e){fail=true;}check(fail,"Malformed packet rejected");}
        System.out.println("PASS NetworkTest checks="+checks+" (real loopback TLS; no public server, no internet-device test)");
    }
}
