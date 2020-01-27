import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class IndexContainer implements Serializable {
    private String database;
    private String table;
    private String indexName;

    private boolean isUnique;
    private TableField tableField;

    //private List<TableField> indexFields = new ArrayList<TableField>();

    public IndexContainer() {
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

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

//    public void addTableField(String name){
//        TableField tableField = new TableField();
//        tableField.setName(name);
//        this.indexFields.add(tableField);
//    }

//    public boolean existsIndex(TableField tableField){
//        for(TableField indexField:this.indexFields){
//            if(indexField.getName().compareTo(tableField.getName())==0){
//                return true;
//            }
//        }
//        return false;
//    }

//    public boolean existsIndex(TableData tableData){
//        for(TableField indexField:this.indexFields){
//            if(indexField.getName().compareTo(tableData.getFieldName())==0){
//                return true;
//            }
//        }
//        return false;
//    }
//
//    public List<TableField> getIndexFields(){
//        return this.indexFields;
//    }

    public boolean isUnique() {
        return isUnique;
    }

    public void setUnique(boolean unique) {
        isUnique = unique;
    }

    public TableField getTableField() {
        return tableField;
    }

    public void setTableField(TableField tableField) {
        this.tableField = tableField;
    }
}
