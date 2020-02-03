import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class Server implements IServer {
    @Override
    public boolean login(String username, String password) throws ManagerException, RemoteException {
        return false;
    }

    @Override
    public void changeObserver(IObserver client) throws ManagerException, RemoteException {

    }

    public List<String> getDatabases() {
        List<String> dbs = new ArrayList<>();
        dbs.add("dbTest");
        dbs.add("companyData");
        dbs.add("companyPrivateData");
        return dbs;
    }

    @Override
    public void createDatabase(String name) throws RemoteException, ManagerException {

    }

    @Override
    public void deleteDatabase(String name) throws RemoteException, ManagerException {

    }


    public List<String> getTables(String db) {
        List<String> tables = new ArrayList<>();
        if(db.compareTo("companyData")==0){
            tables.add("employees");
            tables.add("sales");
            tables.add("departments");
            tables.add("categories");
        }else{
            if(db.equals("companyPrivateData")){
                tables.add("salaries");
                tables.add("profits_per_year");
            }else{
                if(db.equals("dbTest")){
                    tables.add("test");
                }
            }
        }
        return tables;
    }

    @Override
    public void createTable(CreateTableContainer createTableContainer) throws RemoteException, ManagerException {

    }

    @Override
    public void deleteTable(String database, String table) throws RemoteException, ManagerException {

    }

    @Override
    public void addForeignKey(String database, String table, String pkTable) throws RemoteException, ManagerException {

    }

    public List<TableField> getTableFields(String database, String table) {
        List<TableField> tableFields = new ArrayList<>();
        TableField tableField1;
        TableField tableField2;
        TableField tableField3;
        if(database.compareTo("companyData")==0){
            switch (table){
                case "employees":
                    tableField1 = new TableField("emp_id", "INT", true, "employees");
                    tableField2 = new TableField("emp_name", "STRING", false, "employees");
                    tableField3 = new TableField("dept_id", "INT", false, "employees");
                    tableFields.add(tableField1);
                    tableFields.add(tableField2);
                    tableFields.add(tableField3);
                    break;
                case "departments":
                    tableField1 = new TableField("dept_id", "INT", true, "departments");
                    tableField2 = new TableField("dept_name", "STRING", false, "departments");
                    tableFields.add(tableField1);
                    tableFields.add(tableField2);
                    break;
                case "sales":
                    tableField1 = new TableField("sale_id", "INT", true, "sales");
                    tableField2 = new TableField("emp_id", "INT", false, "sales");
                    tableField3 = new TableField("date", "DATE", false, "sales");
                    tableFields.add(tableField1);
                    tableFields.add(tableField2);
                    tableFields.add(tableField3);
                    break;
                case "categories":
                    tableField1 = new TableField("dept_id", "INT", true, "departments");
                    tableField2 = new TableField("dept_name", "STRING", false, "departments");
                    tableFields.add(tableField1);
                    tableFields.add(tableField2);
            }
        }else{
            if(database.compareTo("companyPrivateData")==0){
                switch (table){
                    case "salaries":
                        tableField1 = new TableField("sal_id", "INT", true, "salaries");
                        tableField2 = new TableField("emp_id", "INT", false, "salaries");
                        tableField3 = new TableField("value", "INT", false, "salaries");
                        TableField tableField4 = new TableField("date", "DATE", false, "salaries");
                        tableFields.add(tableField1);
                        tableFields.add(tableField2);
                        tableFields.add(tableField3);
                        tableFields.add(tableField4);
                        break;
                    case "profits_per_year":
                        tableField1 = new TableField("profit_id", "INT", true, "profits_per_year");
                        tableField2 = new TableField("year", "INT", false, "profits_per_year");
                        tableField3 = new TableField("value", "INT", false, "profits_per_year");
                        tableFields.add(tableField1);
                        tableFields.add(tableField2);
                        tableFields.add(tableField3);
                        break;
                }
            }
        }
        return tableFields;
    }

    @Override
    public void insertData(String database, String table, List<TableData> data) throws RemoteException, ManagerException {

    }

    @Override
    public void deleteData(String database, String table, List<TableData> data, LogicalOperator logicalOperator) throws RemoteException, ManagerException {

    }

    @Override
    public void updateData(String database, String table, List<TableData> whereData, LogicalOperator logicalOperator, List<TableData> updatedData) throws RemoteException, ManagerException {

    }

    @Override
    public void createIndex(IndexContainer indexContainer) throws RemoteException, ManagerException {

    }

    @Override
    public List<TableRowResult> select(SelectTableContainer selectTableContainer) throws ManagerException, RemoteException {
        return null;
    }

    @Override
    public String selectDateQuery(String dateQuery) throws ManagerException {
        return null;
    }

    @Override
    public List<String> getReferencedTables(String database, String table) throws ManagerException, RemoteException {
        return null;
    }

    @Override
    public void externalSortJoin(String database) throws ManagerException {

    }

    @Override
    public boolean register(String email, String username, String password, String accesCode) throws ManagerException {
        return false;
    }

    @Override
    public boolean changePassword(String email, String oldPassword, String newPassword) throws ManagerException {
        return false;
    }
}
