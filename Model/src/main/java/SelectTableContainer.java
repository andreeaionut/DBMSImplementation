import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SelectTableContainer implements Serializable {
    private String database;
    private String table;
    private List<JoinClause> joinClauses = new ArrayList<>();
    private List<TableField> projectionFields = new ArrayList<TableField>();
    private List<WhereClause> whereClauses;
    private GroupByClause groupByClause;
    private List<HavingClause> havingClauses;

    public void addHavingClause(HavingClause havingClause){
        this.havingClauses.add(havingClause);
    }

    public void addHavingClause(String aggregateOperator, String operator, String value){
        HavingClause havingClause = new HavingClause(aggregateOperator, operator, value);
        this.addHavingClause(havingClause);
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public void addJoinTable(String joinTable, JoinType joinType){
        JoinClause joinClause = new JoinClause();
        joinClause.setTable(joinTable);
        joinClause.setJoinType(joinType);
        this.joinClauses.add(joinClause);
    }

    public void addJoinClause(JoinClause joinClause){
        this.joinClauses.add(joinClause);
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public List<TableField> getProjectionFields() {
        return projectionFields;
    }

    public void setProjectionFields(List<TableField> projectionFields) {
        this.projectionFields = projectionFields;
    }

    public List<WhereClause> getWhereClauses() {
        return whereClauses;
    }

    public void setWhereClauses(List<WhereClause> whereClauses) {
        this.whereClauses = whereClauses;
    }

    public List<JoinClause> getJoinClauses() {
        return joinClauses;
    }

    public GroupByClause getGroupByClause() {
        return groupByClause;
    }

    public void setGroupByClause(GroupByClause groupByClause) {
        this.groupByClause = groupByClause;
    }

    public List<HavingClause> getHavingClauses() {
        return havingClauses;
    }

    public void setHavingClauses(List<HavingClause> havingClauses) {
        this.havingClauses = havingClauses;
    }
}
