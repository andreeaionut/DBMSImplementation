import javafx.scene.control.Tab;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TableRowResult implements Serializable {
    private Map<String, List<TableData>> data = new HashMap<>();

    public void add(String table, TableData tableData){
        if(this.data.get(table) == null) {
            this.data.put(table, new ArrayList<TableData>());
        }
        this.data.get(table).add(tableData);
    }

    public void add(String table, String fieldName, String fieldValue){
        TableData tableData = new TableData();
        tableData.setFieldValue(fieldValue);
        tableData.setFieldName(fieldName);
        tableData.setTable(table);
        this.add(table, tableData);
    }

    public void addAll(String table, List<TableData> tableData){
        for(TableData tableDataEntity : tableData){
            if(!this.contains(table, tableDataEntity)){
                this.add(table, tableDataEntity);
            }
        }
    }

    private boolean contains(String table, TableData tableDataEntity){
        if(this.data.get(table) == null){
            return false;
        }
        List<TableData> tableDataList = this.data.get(table);
        for(TableData tableData : tableDataList){
            if(tableData.getFieldName().compareTo(tableDataEntity.getFieldName())==0){
                return true;
            }
        }
        return false;
    }

    public String get(String table, String fieldName){
        List<TableData> tableData = this.data.get(table);
            for(TableData tableDataEntity : tableData){
                if(tableDataEntity.getFieldName().compareTo(fieldName)==0){
                    return tableDataEntity.getFieldValue();
                }
            }
        return null;
    }

    public List<TableData> getTableData(String table) {
        return this.data.get(table);
    }

    public boolean contains(String table, List<TableData> tableDatas){
        List<TableData> tableData = this.data.get(table);
        for(TableData tableDataThis : tableData){
                for(TableData tableDataThat : tableDatas){
                    if(tableDataThis.getFieldName().compareTo(tableDataThat.getFieldName())==0 &&
                            tableDataThis.getFieldValue().compareTo(tableDataThat.getFieldValue()) != 0){
                        return false;
                    }
                }
        }
        return true;
    }

    public Map<String, List<TableData>> getData(){
        return this.data;
    }

}
