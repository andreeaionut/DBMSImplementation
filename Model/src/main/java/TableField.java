import java.io.Serializable;

public class TableField implements Serializable {
    private String name;
    private String fieldType;
    private Long length;
    private boolean isPrimaryKey;
    private boolean isForeignKey;
    private String foreignKeyTable;
    private boolean isUnique;
    private int fieldIndex;

    private String table;

    public TableField(String name, String fieldType, boolean isPrimaryKey, String table) {
        this.name = name;
        this.fieldType = fieldType;
        this.isPrimaryKey = isPrimaryKey;
        this.table = table;
    }

    public TableField(){}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getLength() {
        return length;
    }

    public void setLength(Long length) {
        this.length = length;
    }

    public boolean getIsPrimaryKey() {
        return isPrimaryKey;
    }

    public void setPrimaryKey(boolean primaryKey) {
        isPrimaryKey = primaryKey;
    }

    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String String) {
        this.fieldType = String;
    }

    public boolean getIsUnique() {
        return isUnique;
    }

    public void setUnique(boolean unique) {
        isUnique = unique;
    }

    public int getFieldIndex() {
        return fieldIndex;
    }

    public void setFieldIndex(int fieldIndex) {
        this.fieldIndex = fieldIndex;
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
}
