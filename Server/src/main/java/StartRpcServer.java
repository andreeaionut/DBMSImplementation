import java.rmi.ServerException;

public class StartRpcServer {
    private static int defaultPort = 55552;

    public static void main(String[] args) {
        //Properties serverProps = new Properties();
        IServer serverImpl = new ServerImplementation();

        int chatServerPort = defaultPort;
        System.out.println("Starting server on port: " + chatServerPort);

        AbstractServer server = new RpcConcurrentServer(chatServerPort, serverImpl);
        try {
            server.start();
        } catch (ServerException e) {
            e.printStackTrace();
        }
    }
}
