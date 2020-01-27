import java.net.Socket;

public class RpcConcurrentServer extends AbsConcurrentServer {
    private IServer server;

    public RpcConcurrentServer(int port, IServer server) {
        super(port);
        this.server = server;
        System.out.println("RPC Concurrent Server");
    }

    @Override
    protected Thread createWorker(Socket client) {
        ClientRpcReflectionWorker worker=new ClientRpcReflectionWorker(server, client);
        Thread tw=new Thread(worker);
        return tw;
    }
}
