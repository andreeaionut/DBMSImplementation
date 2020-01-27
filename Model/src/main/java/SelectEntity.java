import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SelectEntity implements Serializable {
    private List<Tuple> tuples = new ArrayList<>();

    public void add(String table, String fieldName, String fieldValue){
        Tuple tuple = new Tuple();
        tuple.setTable(table);
        tuple.add(fieldName, fieldValue);
        this.tuples.add(tuple);
    }

    public void add(Tuple tuple){
        this.tuples.add(tuple);
    }

    public void add(SelectEntity selectEntity){
        this.tuples.addAll(selectEntity.tuples);
    }

    public List<Tuple> getTuplesByTable(String table){
        List<Tuple> tuplesByTable = new ArrayList<>();
        for(Tuple tuple : this.tuples){
            if(tuple.getTable().compareTo(table) == 0){
                tuplesByTable.add(tuple);
            }
        }
        return tuplesByTable;
    }

    public Tuple getTupleByTable(String table){
        for(Tuple tuple : this.tuples){
            if(tuple.getTable().compareTo(table) == 0){
                return tuple;
            }
        }
        return null;
    }

    public String get(String table, String fieldName){
        for(Tuple tuple : this.tuples){
            if(tuple.getTable().compareTo(table)==0){
                for(TableData tableData : tuple.tableData){
                    if(tableData.getFieldName().compareTo(fieldName)==0){
                        return tableData.getFieldValue();
                    }
                }
            }
        }
        return "";
    }

    public List<TableData> getTableData(String table){
        for(Tuple tuple : this.tuples){
            if(tuple.getTable().compareTo(table) == 0){
                return tuple.tableData;
            }
        }
        return null;
    }

    public void keepProjectionFields(List<TableField> projectionFields) {
        for (Tuple tuple : this.tuples) {
            String table = tuple.getTable();
            List<TableData> projectedTableData = new ArrayList<>();
            for(TableField projectionField : projectionFields){
                if(projectionField.getTable().compareTo(table) == 0){
                    for(TableData tableData : tuple.tableData){
                        if(projectionField.getName().compareTo(tableData.getFieldName()) == 0){
                            projectedTableData.add(tableData);
                        }
                    }
                }
            }
            tuple.setTableData(projectedTableData);
        }
    }

    public List<Tuple> getTuples() {
        return tuples;
    }

    public void setTuples(List<Tuple> tuples) {
        this.tuples = tuples;
    }

    public boolean contains(String table, TableData tableData) {
        for(Tuple tuple : this.tuples){
            if(tuple.getTable().compareTo(table)==0){
                for(TableData tableDataThis : tuple.tableData){
                    if(tableDataThis.equals(tableData)){
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SelectEntity that = (SelectEntity) o;
        for(Tuple tuple : this.tuples){
            boolean exists = false;
            for(Tuple thatTuple : that.tuples){
                if(tuple.equals(thatTuple)){
                    exists = true;
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

        return Objects.hash(tuples);
    }
}
