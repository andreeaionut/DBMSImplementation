import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ServerRpcProxy implements IServer {

    private String host;
    private int port;

    private IObserver client;

    private ObjectInputStream input;
    private ObjectOutputStream output;
    private Socket connection;

    private BlockingQueue<Response> qresponses;
    private volatile boolean finished;

    public ServerRpcProxy(String host, int port) {
        this.host = host;
        this.port = port;
        qresponses = new LinkedBlockingQueue<Response>();
        initializeConnection();
    }

    public void closeConnection() {
        finished=true;
        try {
            output.close();
            input.close();
//            connection.close();
            client=null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Response readResponse() {
        Response response=null;
        try{
            response=qresponses.take();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return response;
    }

    private void initializeConnection() {
        try {
            connection = new Socket(host, port);
            output = new ObjectOutputStream(connection.getOutputStream());
            output.flush();
            input = new ObjectInputStream(connection.getInputStream());
            finished = false;
            startReader();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startReader() {
        Thread tw = new Thread(new ReaderThread());
        tw.start();
    }

    private void handleUpdate(Response response) {
//        if (response.type() == ResponseType.SELL_TICKETS) {
//
//            Sale sale = (Sale) response.data();
//            System.out.println("Ticket bought " + sale);
//            try {
//                System.out.println("Am apelat metoda ticketsBought a clientului " + client);
//                client.ticketsBought(sale);
//            } catch (ManagerException e) {
//                e.printStackTrace();
//            } catch (RemoteException e) {
//                e.printStackTrace();
//            }
//        }
    }

    private void sendRequest(Request request) throws ManagerException {
        try {
            output.writeObject(request);
            output.flush();
        } catch (IOException e) {
            throw new ManagerException("Error sending object "+e);
        }
    }

    private boolean isUpdate(Response response) {
        return response.type() == ResponseType.SELL_TICKETS;
    }

    private class ReaderThread implements Runnable {
        public void run() {
            while (!finished) {
                try {
                    Object response = input.readObject();
                    System.out.println("response received " + response);
                    if (isUpdate((Response) response)) {
                        handleUpdate((Response) response);
                    } else {

                        try {
                            qresponses.put((Response) response);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Reading error " + e);
                } catch (ClassNotFoundException e) {
                    System.out.println("Reading error " + e);
                }
            }
        }
    }

    @Override
    public boolean login(String username, String password) throws ManagerException, RemoteException {
        LoginRequest loginRequest = new LoginRequest(username, password);
        Request req = new Request.Builder().type(RequestType.LOGIN).data(loginRequest).build();
        System.out.println(req);
        sendRequest(req);
        Response response=readResponse();
        if (response.type()== ResponseType.ERROR){
            String err=response.data().toString();
            throw new ManagerException(err);
        }
        if(response.type() == ResponseType.LOGIN){
            return (boolean) response.data();
        }
        return false;
    }

    @Override
    public void changeObserver(IObserver client) throws ManagerException, RemoteException { }

    public List<String> getDatabases() throws ManagerException {
        Request req = new Request.Builder().type(RequestType.GET_DATABASES).build();
        System.out.println(req);
        sendRequest(req);
        Response response=readResponse();
        if (response.type() == ResponseType.GET_DATABASES){
            //this.client=client;
            return (List<String>) response.data();
        }
        if (response.type()== ResponseType.ERROR){
            String err=response.data().toString();
            closeConnection();
            throw new ManagerException(err);
        }
        //closeConnection();
        return null;
    }

    public void createDatabase(String name) throws ManagerException {
        Request req = new Request.Builder().type(RequestType.CREATE_DATABASE).data(name).build();
        System.out.println(req);
        sendRequest(req);
        Response response=readResponse();
        if (response.type()== ResponseType.ERROR){
            String err=response.data().toString();
            //closeConnection();
            throw new ManagerException(err);
        }
    }

    @Override
    public void deleteDatabase(String name) throws RemoteException, ManagerException {
        Request req = new Request.Builder().type(RequestType.DELETE_DATABASE).data(name).build();
        System.out.println(req);
        sendRequest(req);
        Response response=readResponse();
        if (response.type()== ResponseType.ERROR){
            throw new ManagerException(response.data().toString());
        }
    }

    @Override
    public List<String> getTables(String database) throws RemoteException, ManagerException {
        Request req = new Request.Builder().type(RequestType.GET_TABLES).data(database).build();
        System.out.println(req);
        sendRequest(req);
        Response response=readResponse();
        if (response.type() == ResponseType.GET_TABLES){
            return (List<String>) response.data();
        }
        if (response.type()== ResponseType.ERROR){
            String err=response.data().toString();
            closeConnection();
            throw new ManagerException(err);
        }
        return null;
    }

    @Override
    public void createTable(CreateTableContainer createTableContainer) throws RemoteException, ManagerException {
        Request req = new Request.Builder().type(RequestType.CREATE_TABLE).data(createTableContainer).build();
        System.out.println(req);
        sendRequest(req);
        Response response=readResponse();
        if (response.type()== ResponseType.ERROR){
            throw new ManagerException(response.data().toString());
        }
    }

    @Override
    public void deleteTable(String database, String table) throws RemoteException, ManagerException {
        NetworkingContainer networkingContainer = new NetworkingContainer(database, table);
        Request req = new Request.Builder().type(RequestType.DELETE_TABLE).data(networkingContainer).build();
        System.out.println(req);
        sendRequest(req);
        Response response=readResponse();
        if (response.type()== ResponseType.ERROR){
            throw new ManagerException(response.data().toString());
        }
    }

    @Override
    public void addForeignKey(String database, String table, String pkTable) throws RemoteException, ManagerException {
        NetworkingContainer networkingContainer = new NetworkingContainer(database, table);
        networkingContainer.setPkTable(pkTable);
        Request req = new Request.Builder().type(RequestType.ADD_FOREIGN_KEY).data(networkingContainer).build();
        System.out.println(req);
        sendRequest(req);
        Response response=readResponse();
        if (response.type()== ResponseType.ERROR){
            throw new ManagerException(response.data().toString());
        }
    }

    @Override
    public List<TableField> getTableFields(String database, String table) throws RemoteException, ManagerException {
        NetworkingContainer networkingContainer = new NetworkingContainer(database, table);
        Request req = new Request.Builder().type(RequestType.GET_TABLE_FIELDS).data(networkingContainer).build();
        System.out.println(req);
        sendRequest(req);
        Response response=readResponse();
        if (response.type() == ResponseType.GET_TABLE_FIELDS){
            return (List<TableField>) response.data();
        }
        if (response.type()== ResponseType.ERROR){
            throw new ManagerException(response.data().toString());
        }
        return null;
    }

    @Override
    public void insertData(String database, String table, List<TableData> data) throws RemoteException, ManagerException {
        InsertDeleteDataContainer insertDeleteDataContainer = new InsertDeleteDataContainer();
        insertDeleteDataContainer.setDatabase(database);
        insertDeleteDataContainer.setTable(table);
        insertDeleteDataContainer.setData(data);
        Request req = new Request.Builder().type(RequestType.INSERT_DATA).data(insertDeleteDataContainer).build();
        System.out.println(req);
        sendRequest(req);
        Response response=readResponse();
        if (response.type()== ResponseType.ERROR){
            throw new ManagerException(response.data().toString());
        }
    }

    @Override
    public void deleteData(String database, String table, List<TableData> data, LogicalOperator logicalOperator) throws RemoteException, ManagerException {
        InsertDeleteDataContainer insertDeleteDataContainer = new InsertDeleteDataContainer();
        insertDeleteDataContainer.setDatabase(database);
        insertDeleteDataContainer.setTable(table);
        insertDeleteDataContainer.setData(data);
        insertDeleteDataContainer.setLogicalOperator(logicalOperator);
        Request req = new Request.Builder().type(RequestType.DELETE_DATA).data(insertDeleteDataContainer).build();
        System.out.println(req);
        sendRequest(req);
        Response response=readResponse();
        if (response.type()== ResponseType.ERROR){
            throw new ManagerException(response.data().toString());
        }
    }

    @Override
    public void updateData(String database, String table, List<TableData> whereData, LogicalOperator logicalOperator, List<TableData> updatedData) throws RemoteException, ManagerException {
        InsertDeleteDataContainer insertDeleteDataContainer = new InsertDeleteDataContainer();
        insertDeleteDataContainer.setDatabase(database);
        insertDeleteDataContainer.setTable(table);
        insertDeleteDataContainer.setData(whereData);
        insertDeleteDataContainer.setUpdateData(updatedData);
        insertDeleteDataContainer.setLogicalOperator(logicalOperator);
        Request req = new Request.Builder().type(RequestType.UPDATE_DATA).data(insertDeleteDataContainer).build();
        System.out.println(req);
        sendRequest(req);
        Response response=readResponse();
        if (response.type()== ResponseType.ERROR){
            throw new ManagerException(response.data().toString());
        }
    }

    @Override
    public void createIndex(IndexContainer indexContainer) throws RemoteException, ManagerException {
        Request req = new Request.Builder().type(RequestType.CREATE_INDEX).data(indexContainer).build();
        System.out.println(req);
        sendRequest(req);
        Response response=readResponse();
        if (response.type()== ResponseType.ERROR){
            throw new ManagerException(response.data().toString());
        }
    }

    @Override
    public List<TableRowResult> select(SelectTableContainer selectTableContainer) throws ManagerException {
        Request req = new Request.Builder().type(RequestType.SELECT).data(selectTableContainer).build();
        System.out.println(req);
        sendRequest(req);
        Response response=readResponse();
        if (response.type() == ResponseType.SELECT){
            return (List<TableRowResult>) response.data();

        }
        if (response.type()== ResponseType.ERROR){
            throw new ManagerException(response.data().toString());
        }
        return null;
    }

    @Override
    public String selectDateQuery(String dateQuery) throws ManagerException {
        Request req = new Request.Builder().type(RequestType.SELECT_DATE).data(dateQuery).build();
        System.out.println(req);
        sendRequest(req);
        Response response=readResponse();
        if (response.type() == ResponseType.SELECT_DATE){
            return (String) response.data();
        }
        if (response.type()== ResponseType.ERROR){
            throw new ManagerException(response.data().toString());
        }
        return null;
    }

    @Override
    public List<String> getReferencedTables(String database, String table) throws ManagerException, RemoteException {
        NetworkingContainer networkingContainer = new NetworkingContainer(database, table);
        Request req = new Request.Builder().type(RequestType.GET_REF_TABLES).data(networkingContainer).build();
        System.out.println(req);
        sendRequest(req);
        Response response=readResponse();
        if (response.type() == ResponseType.GET_REF_TABLES){
            return (List<String>) response.data();
        }
        if (response.type()== ResponseType.ERROR){
            throw new ManagerException(response.data().toString());
        }
        return null;
    }

    @Override
    public void externalSortJoin(String database) throws ManagerException {
        Request req = new Request.Builder().data(database).type(RequestType.EXT_SORT_JOIN).build();
        System.out.println(req);
        sendRequest(req);
        Response response=readResponse();
        if (response.type() == ResponseType.EXT_SORT_JOIN){
            return;
        }
        if (response.type()== ResponseType.ERROR){
            throw new ManagerException(response.data().toString());
        }
    }

    @Override
    public boolean register(String email, String username, String password, String accesCode) throws ManagerException {
        RegisterRequest registerRequest = new RegisterRequest(email, username, password, accesCode);
        Request req = new Request.Builder().type(RequestType.REGISTER).data(registerRequest).build();
        System.out.println(req);
        sendRequest(req);
        Response response=readResponse();
        if (response.type()== ResponseType.ERROR){
            String err=response.data().toString();
            throw new ManagerException(err);
        }
        return true;
    }
}
