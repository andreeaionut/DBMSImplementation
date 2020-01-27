import java.util.List;

public class JoinEntity {
    private String table;
    private List<SelectEntity> selectEntities;
    private JoinType joinType;

    public JoinEntity(String table, List<SelectEntity> selectEntities) {
        this.table = table;
        this.selectEntities = selectEntities;
    }

    public JoinEntity(String table, List<SelectEntity> selectEntities, JoinType joinType) {
        this.table = table;
        this.selectEntities = selectEntities;
        this.joinType = joinType;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public List<SelectEntity> getSelectEntities() {
        return selectEntities;
    }

    public void setSelectEntities(List<SelectEntity> selectEntities) {
        this.selectEntities = selectEntities;
    }

    public JoinType getJoinType() {
        return joinType;
    }

    public void setJoinType(JoinType joinType) {
        this.joinType = joinType;
    }
}
