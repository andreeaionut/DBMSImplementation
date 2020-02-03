import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.rmi.RemoteException;
import java.util.List;

public class ClientRpcReflectionWorker implements Runnable, IObserver {
    private IServer server;
    private Socket connection;

    private ObjectInputStream input;
    private ObjectOutputStream output;
    private volatile boolean connected;

    public ClientRpcReflectionWorker(IServer server, Socket connection) {
        this.server = server;
        this.connection = connection;
        try{
            output=new ObjectOutputStream(connection.getOutputStream());
            output.flush();
            input=new ObjectInputStream(connection.getInputStream());
            connected=true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        while(connected){
            try {
                Object request=input.readObject();
                Response response=handleRequest((Request)request);
                if (response!=null){
                    sendResponse(response);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            input.close();
            output.close();
            connection.close();
        } catch (IOException e) {
            System.out.println("Error "+e);
        }
    }

    private Response handleRequest(Request request){
        Response response=null;
        String handlerName="handle"+(request).type();
        System.out.println("HandlerName "+handlerName);
        try {
            Method method=this.getClass().getDeclaredMethod(handlerName, Request.class);
            response=(Response)method.invoke(this, request);
            System.out.println("Method "+handlerName+ " invoked");
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return response;
    }

    private static Response okResponse=new Response.Builder().type(ResponseType.OK).build();

    private Response handleGET_DATABASES(Request request) throws RemoteException {
        List<String> dbs = null;
        try {
            dbs = server.getDatabases();
        } catch (ManagerException e) {
            return new Response.Builder().type(ResponseType.ERROR).data(e.getMessage()).build();
        }
        return new Response.Builder().type(ResponseType.GET_DATABASES).data(dbs).build();
    }

    private Response handleGET_TABLES(Request request) throws RemoteException {
        List<String> tables = null;
        try {
            tables = server.getTables(request.data().toString());
        } catch (ManagerException e) {
            return new Response.Builder().type(ResponseType.ERROR).data(e.getMessage()).build();
        }
        return new Response.Builder().type(ResponseType.GET_TABLES).data(tables).build();
    }

    private Response handleCREATE_TABLE(Request request) throws RemoteException {
        try {
            CreateTableContainer createTableContainer = (CreateTableContainer) request.data();
            server.createTable(createTableContainer);
        } catch (ManagerException e) {
            return new Response.Builder().type(ResponseType.ERROR).data(e.getMessage()).build();
        }
        return okResponse;
    }

    private Response handleDELETE_TABLE(Request request) throws RemoteException {
        try {
            NetworkingContainer networkingContainer = (NetworkingContainer) request.data();
            server.deleteTable(networkingContainer.getDatabase(), networkingContainer.getTable());
        } catch (ManagerException e) {
            return new Response.Builder().type(ResponseType.ERROR).data(e.getMessage()).build();
        }
        return new Response.Builder().type(ResponseType.DELETE_TABLE).build();
    }

    private Response handleCREATE_DATABASE(Request request) throws RemoteException {
        try {
            server.createDatabase(request.data().toString());
            return okResponse;
        } catch (ManagerException e) {
            return new Response.Builder().type(ResponseType.ERROR).data("Database already exists").build();
        }
    }

    private Response handleDELETE_DATABASE(Request request) throws RemoteException {
        try {
            server.deleteDatabase(request.data().toString());
            return okResponse;
        } catch (ManagerException e) {
            return new Response.Builder().type(ResponseType.ERROR).data("Database does not exist").build();
        }
    }

    private Response handleADD_FOREIGN_KEY(Request request) throws RemoteException {
        NetworkingContainer networkingContainer = (NetworkingContainer) request.data();
        try {
            server.addForeignKey(networkingContainer.getDatabase(), networkingContainer.getTable(), networkingContainer.getPkTable());
            return okResponse;
        } catch (ManagerException e) {
            return new Response.Builder().type(ResponseType.ERROR).data(e.getMessage()).build();
        }
    }

    private Response handleGET_TABLE_FIELDS(Request request) throws RemoteException {
        NetworkingContainer networkingContainer = (NetworkingContainer) request.data();
        try {
            List<TableField> tableFields = server.getTableFields(networkingContainer.getDatabase(), networkingContainer.getTable());
            return new Response.Builder().type(ResponseType.GET_TABLE_FIELDS).data(tableFields).build();
        } catch (ManagerException e) {
            return new Response.Builder().type(ResponseType.ERROR).data(e.getMessage()).build();
        }
    }

    private Response handleCREATE_INDEX(Request request) throws RemoteException {
        IndexContainer indexContainer = (IndexContainer) request.data();
        try {
            server.createIndex(indexContainer);
            return okResponse;
        } catch (ManagerException e) {
            return new Response.Builder().type(ResponseType.ERROR).data(e.getMessage()).build();
        }
    }

    private Response handleINSERT_DATA(Request request) throws RemoteException {
        InsertDeleteDataContainer insertDeleteDataContainer = (InsertDeleteDataContainer) request.data();
        try {
            server.insertData(insertDeleteDataContainer.getDatabase(), insertDeleteDataContainer.getTable(), insertDeleteDataContainer.getData());
            return okResponse;
        } catch (ManagerException e) {
            return new Response.Builder().type(ResponseType.ERROR).data(e.getMessage()).build();
        }
    }

    private Response handleDELETE_DATA(Request request) throws RemoteException {
        InsertDeleteDataContainer insertDeleteDataContainer = (InsertDeleteDataContainer) request.data();
        try {
            server.deleteData(insertDeleteDataContainer.getDatabase(), insertDeleteDataContainer.getTable(), insertDeleteDataContainer.getData(), insertDeleteDataContainer.getLogicalOperator());
            return okResponse;
        } catch (ManagerException e) {
            return new Response.Builder().type(ResponseType.ERROR).data(e.getMessage()).build();
        }
    }

    private Response handleUPDATE_DATA(Request request) throws RemoteException {
        InsertDeleteDataContainer insertDeleteDataContainer = (InsertDeleteDataContainer) request.data();
        try {
            server.updateData(insertDeleteDataContainer.getDatabase(), insertDeleteDataContainer.getTable(), insertDeleteDataContainer.getData(), insertDeleteDataContainer.getLogicalOperator(), insertDeleteDataContainer.getUpdateData());
            return okResponse;
        } catch (ManagerException e) {
            return new Response.Builder().type(ResponseType.ERROR).data(e.getMessage()).build();
        }
    }

    private Response handleSELECT(Request request) throws RemoteException {
        SelectTableContainer selectTableContainer = (SelectTableContainer) request.data();
        List<TableRowResult> selectResult = null;
        try {
            selectResult = server.select(selectTableContainer);
        } catch (ManagerException e) {
            return new Response.Builder().type(ResponseType.ERROR).data(e.getMessage()).build();
        }
        return new Response.Builder().type(ResponseType.SELECT).data(selectResult).build();
    }

    private Response handleSELECT_DATE(Request request) throws RemoteException {
        String dateQuery = (String) request.data();
        String selectResult = null;
        try {
            selectResult = server.selectDateQuery(dateQuery);
        } catch (ManagerException e) {
            return new Response.Builder().type(ResponseType.ERROR).data(e.getMessage()).build();
        }
        return new Response.Builder().type(ResponseType.SELECT_DATE).data(selectResult).build();
    }

    private Response handleGET_REF_TABLES(Request request) throws RemoteException {
        NetworkingContainer networkingContainer = (NetworkingContainer) request.data();
        List<String> refTables = null;
        try {
            refTables = server.getReferencedTables(networkingContainer.getDatabase(), networkingContainer.getTable());
        } catch (ManagerException e) {
            return new Response.Builder().type(ResponseType.ERROR).data(e.getMessage()).build();
        }
        return new Response.Builder().type(ResponseType.GET_REF_TABLES).data(refTables).build();
    }

    private Response handleEXT_SORT_JOIN(Request request) throws RemoteException {
        String database = request.data().toString();
        try {
            server.externalSortJoin(database);
        } catch (ManagerException e) {
            return new Response.Builder().type(ResponseType.ERROR).data(e.getMessage()).build();
        }
        return new Response.Builder().type(ResponseType.EXT_SORT_JOIN).build();
    }

    private Response handleLOGIN(Request request) throws RemoteException {
        LoginRequest loginRequest = (LoginRequest) request.data();
        boolean result;
        try {
            result = server.login(loginRequest.getUsername(), loginRequest.getPassword());
        } catch (ManagerException e) {
            return new Response.Builder().type(ResponseType.ERROR).data(e.getMessage()).build();
        }
        return new Response.Builder().type(ResponseType.LOGIN).data(result).build();
    }

    private Response handleREGISTER(Request request) {
        RegisterRequest registerRequest = (RegisterRequest) request.data();
        boolean result;
        try {
            result = server.register(registerRequest.getEmail(), registerRequest.getUsername(), registerRequest.getPassword(), registerRequest.getAccessCode());
        } catch (ManagerException e) {
            return new Response.Builder().type(ResponseType.ERROR).data(e.getMessage()).build();
        }
        return new Response.Builder().type(ResponseType.REGISTER).data(result).build();
    }

    private Response handleCHANGE_PASSWORD(Request request) {
        ChangePasswordRequest changePasswordRequest = (ChangePasswordRequest) request.data();
        boolean result;
        try {
            result = server.changePassword(changePasswordRequest.getUsername(), changePasswordRequest.getOldPassword(), changePasswordRequest.getNewPassword());
        } catch (ManagerException e) {
            return new Response.Builder().type(ResponseType.ERROR).data(e.getMessage()).build();
        }
        return new Response.Builder().type(ResponseType.CHANGE_PASSWORD).data(result).build();
    }


    private void sendResponse(Response response) throws IOException{
        System.out.println("sending response "+response);
        output.writeObject(response);
        output.flush();
    }

    @Override
    public void eventHappened() throws RemoteException {

    }
}
