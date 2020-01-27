import java.io.Serializable;

public class WhereClause implements Serializable {
    private TableField tableField;
    private String operator;
    private String value;

    private boolean wasIndexUsed = false;

    public TableField getTableField() {
        return tableField;
    }

    public void setTableField(TableField tableField) {
        this.tableField = tableField;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isWasIndexUsed() {
        return wasIndexUsed;
    }

    public void setWasIndexUsed(boolean wasIndexUsed) {
        this.wasIndexUsed = wasIndexUsed;
    }
}
