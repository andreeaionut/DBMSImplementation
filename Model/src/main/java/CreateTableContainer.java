import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CreateTableContainer implements Serializable {

    private String database;
    private String table;
    private List<TableField> tableFields;
    private List<String> foreignKeys;

    public CreateTableContainer(){
        this.tableFields = new ArrayList<TableField>();
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

    public List<TableField> getTableFields(){
        return this.tableFields;
    }

    private boolean existsFieldName(String name){
        for(TableField tableField : this.tableFields){
            if(tableField.getName().compareTo(name) == 0){
                return true;
            }
        }
        return false;
    }

    public boolean addTableField(String name, String type, boolean isPrimaryKey, boolean isUnique){
        if(existsFieldName(name)){
            return false;
        }
        TableField tableField = new TableField();
        tableField.setName(name);
        tableField.setPrimaryKey(isPrimaryKey);
        tableField.setFieldType(String.valueOf(FieldType.getByCode(type)));
        tableField.setUnique(isUnique);

        this.tableFields.add(tableField);
        return true;
    }

    public boolean addTableField(String name, String type, long length, boolean isPrimaryKey, boolean isUnique){
        if(existsFieldName(name)){
            return false;
        }
        TableField tableField = new TableField();
        tableField.setName(name);
        tableField.setPrimaryKey(isPrimaryKey);
        tableField.setFieldType(String.valueOf(FieldType.getByCode(type)));
        tableField.setLength(length);
        tableField.setUnique(isUnique);

        this.tableFields.add(tableField);
        return true;
    }

    public void addTableField(TableField tableField){
        if(tableField.getLength() != null){
            this.addTableField(tableField.getName(), tableField.getFieldType(), tableField.getLength(), tableField.getIsPrimaryKey(), tableField.getIsUnique());
        }else{
            this.addTableField(tableField.getName(), tableField.getFieldType(), tableField.getIsPrimaryKey(), tableField.getIsUnique());
        }
    }

    public boolean isPrimaryKeyComposed(){
        int pks = 0;
        for(TableField tableField : this.tableFields){
            if(tableField.getIsPrimaryKey()){
                pks++;
            }
            if(pks > 1){
                return true;
            }
        }
        return false;
    }
}
