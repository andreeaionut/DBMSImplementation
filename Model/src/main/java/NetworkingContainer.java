import java.io.Serializable;

public class NetworkingContainer implements Serializable {
    private String database;
    private String table;
    private String pkTable;
    private String indexName;

    public NetworkingContainer() {
    }

    public NetworkingContainer(String database, String table) {
        this.database = database;
        this.table = table;
    }


    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public String getPkTable() {
        return pkTable;
    }

    public void setPkTable(String pkTable) {
        this.pkTable = pkTable;
    }

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }
}
