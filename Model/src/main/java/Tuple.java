import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Tuple implements Serializable {
    String table;
    List<TableData> tableData = new ArrayList<>();

    public Tuple() {
    }

    public Tuple(String table, List<TableData> tableData) {
        this.table = table;
        this.tableData = tableData;
    }

    public void add(String fieldName, String fieldValue){
        TableData tableData = new TableData();
        tableData.setFieldValue(fieldValue);
        tableData.setFieldName(fieldName);
        this.tableData.add(tableData);
    }

    public void add(TableData tableData){
        this.tableData.add(tableData);
    }

    public String get(String fieldName){
        for(TableData tableData : tableData){
            if(tableData.getFieldName().compareTo(fieldName)==0){
                return tableData.getFieldValue();
            }
        }
        return null;
    }

    public void setTable(String table){
        this.table = table;
    }

    public String getTable(){
        return table;
    }

    public void setTableData(List<TableData> tableData){
        this.tableData = tableData;
    }

    public boolean containsNull(){
        for(TableData tableData : this.tableData){
            if(tableData.getFieldValue().compareTo("null")==0){
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tuple that = (Tuple) o;
        if(this.table.compareTo(that.table)!=0){
            return false;
        }
        if(this.tableData.size() != that.tableData.size()){
            return false;
        }
        for(TableData tableDataThis : this.tableData){
            boolean exists = false;
            for(TableData tableDataThat : that.tableData){
                if(tableDataThat.equals(tableDataThis)){
                    exists = true;
                    break;
                }
            }
            if(!exists){
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {

        return Objects.hash(table, tableData);
    }
}
