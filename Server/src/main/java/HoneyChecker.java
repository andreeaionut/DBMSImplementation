import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class HoneyChecker {
    private JdbcUtils dbUtils;

    public HoneyChecker(JdbcUtils jdbcUtils) {
        this.dbUtils = jdbcUtils;
    }

    public boolean checkIndex(int userId, int sweetWordIndex){
        Connection con=dbUtils.getConnection();
        try(PreparedStatement preStmt=con.prepareStatement("select * from indexes WHERE user_id=" + userId+ ";")) {
            try(ResultSet result=preStmt.executeQuery()) {
                while (result.next()) {
                    int trueIndex = result.getInt(3);
                    return trueIndex == sweetWordIndex;
                }
            }
        } catch (SQLException e) {
            System.out.println("Error DB " + e);
        }
        return false;
    }


}
