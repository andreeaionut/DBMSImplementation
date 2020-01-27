import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class InsertDeleteDataContainer implements Serializable {
    private String database;
    private String table;
    private List<TableData> data = new ArrayList<TableData>();
    private List<TableData> updateData = new ArrayList<TableData>();
    private LogicalOperator logicalOperator;

    public InsertDeleteDataContainer() {
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

    public List<TableData> getData() {
        return data;
    }

    public void setData(List<TableData> data) {
        this.data.addAll(data);
    }

    public LogicalOperator getLogicalOperator() {
        return logicalOperator;
    }

    public void setLogicalOperator(LogicalOperator logicalOperator) {
        this.logicalOperator = logicalOperator;
    }

    public List<TableData> getUpdateData() {
        return updateData;
    }

    public void setUpdateData(List<TableData> updateData) {
        this.updateData.addAll(updateData);
    }
}
