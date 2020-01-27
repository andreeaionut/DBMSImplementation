import java.io.Serializable;
import java.util.Objects;

public class TableData implements Serializable {
    private String fieldName;
    private String fieldValue;
    private int fieldIndex;

    private boolean isPrimaryKey;
    private boolean isUnique;
    private boolean isForeignKey;
    private String foreignKeyTable;

    private String table;

    public TableData() {
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldValue() {
        return fieldValue;
    }

    public void setFieldValue(String fieldValue) {
        this.fieldValue = fieldValue;
    }

    public boolean isPrimaryKey() {
        return isPrimaryKey;
    }

    public void setPrimaryKey(boolean primaryKey) {
        isPrimaryKey = primaryKey;
    }

    public int getFieldIndex() {
        return fieldIndex;
    }

    public void setFieldIndex(int fieldIndex) {
        this.fieldIndex = fieldIndex;
    }

    public boolean isUnique() {
        return isUnique;
    }

    public void setUnique(boolean unique) {
        isUnique = unique;
    }

    public boolean isForeignKey() {
        return isForeignKey;
    }

    public void setForeignKey(boolean foreignKey) {
        isForeignKey = foreignKey;
    }

    public String getForeignKeyTable() {
        return foreignKeyTable;
    }

    public void setForeignKeyTable(String foreignKeyTable) {
        this.foreignKeyTable = foreignKeyTable;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public TableData copy(){
        TableData tableData = new TableData();
        tableData.setFieldValue(fieldValue);
        tableData.setFieldName(fieldName);
        tableData.setTable(table);
        tableData.setForeignKey(isForeignKey);
        tableData.setUnique(isUnique);
        tableData.setFieldIndex(fieldIndex);
        tableData.setForeignKeyTable(foreignKeyTable);
        return tableData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TableData tableData = (TableData) o;
        return fieldName.compareTo(tableData.fieldName) == 0 &&
                fieldValue.compareTo(tableData.fieldValue) == 0 &&
                table.compareTo(tableData.table) == 0;
    }

    @Override
    public int hashCode() {

        return Objects.hash(fieldName, fieldValue, table);
    }
}
