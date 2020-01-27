import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GroupByClause implements Serializable {
    private String table;
    private TableField tableField;
    private List<AggregateOperators> aggregateOperators = new ArrayList<>();

    public void addAggregateOperator(String aggregateStringOperator){
        switch (aggregateStringOperator){
            case "COUNT":
                if(!this.aggregateOperators.contains(AggregateOperators.COUNT)){
                    this.aggregateOperators.add(AggregateOperators.COUNT);
                }
                break;
            case "SUM":
                if(!this.aggregateOperators.contains(AggregateOperators.SUM)){
                    this.aggregateOperators.add(AggregateOperators.SUM);
                }
                break;
            case "AVG":
                if(!this.aggregateOperators.contains(AggregateOperators.AVG)){
                    this.aggregateOperators.add(AggregateOperators.AVG);
                }
                break;
        }
    }

    public List<AggregateOperators> getAggregateOperators() {
        return aggregateOperators;
    }

    public void setAggregateOperators(List<AggregateOperators> aggregateOperators) {
        this.aggregateOperators = aggregateOperators;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public TableField getTableField() {
        return tableField;
    }

    public void setTableField(TableField tableField) {
        this.tableField = tableField;
    }

    public GroupByClause(String table, TableField tableField) {
        this.table = table;
        this.tableField = tableField;
    }

    public GroupByClause(){}
}
