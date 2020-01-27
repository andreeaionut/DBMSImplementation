import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface IServer extends Remote {
    boolean login(String username, String password) throws  ManagerException, RemoteException;

    void changeObserver(IObserver client) throws  ManagerException, RemoteException;
    List<String> getDatabases() throws RemoteException, ManagerException;
    void createDatabase(String name) throws RemoteException, ManagerException;
    void deleteDatabase(String name) throws RemoteException, ManagerException;
    List<String> getTables(String database) throws RemoteException, ManagerException;
    void createTable(CreateTableContainer createTableContainer) throws RemoteException, ManagerException;
    void deleteTable(String database, String table) throws RemoteException, ManagerException;
    void addForeignKey(String database, String table, String pkTable) throws RemoteException, ManagerException;
    List<TableField> getTableFields(String database, String table)throws RemoteException, ManagerException;

    void insertData(String database, String table, List<TableData> data) throws RemoteException, ManagerException;
    void deleteData(String database, String table, List<TableData> data, LogicalOperator logicalOperator) throws RemoteException, ManagerException;
    void updateData(String database, String table, List<TableData> whereData, LogicalOperator logicalOperator, List<TableData> updatedData) throws RemoteException, ManagerException;

    void createIndex(IndexContainer indexContainer) throws RemoteException, ManagerException;

    List<TableRowResult> select(SelectTableContainer selectTableContainer) throws ManagerException, RemoteException;
    String selectDateQuery(String dateQuery) throws ManagerException;

    List<String> getReferencedTables(String database, String table) throws ManagerException, RemoteException;

    void externalSortJoin(String database) throws ManagerException;

    boolean register(String email, String username, String password, String accesCode) throws ManagerException;
}
