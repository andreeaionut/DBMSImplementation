import java.util.ArrayList;
import java.util.List;

public class Server {
    public List<String> getDatabases() {
        List<String> dbs = new ArrayList<>();
        dbs.add("dbTest");
        dbs.add("companyData");
        dbs.add("companyPrivateData");
        return dbs;
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

    public List<TableField> getTableFields(String database, String table) {
        List<TableField> tableFields = new ArrayList<>();
        if(database.compareTo("companyData")==0){
            switch (table){
                case "employees":
                    TableField tableField1 = new TableField("emp_id", "INT", true, "employees");
                    TableField tableField2 = new TableField("emp_name", "STRING", false, "employees");
                    TableField tableField3 = new TableField("dept_id", "INT", false, "employees");
                    tableFields.add(tableField1);
                    tableFields.add(tableField2);
                    tableFields.add(tableField3);
            }
        }
    }
}
