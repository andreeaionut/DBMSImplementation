import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class JdbcUtils {
    private Properties jdbcProps;
    private Connection instance=null;

    public JdbcUtils(Properties props){
        try {
            props.load(new FileReader("D:\\2019-2020\\DBMS\\MiniDBMS\\Server\\src\\main\\resources\\bd.config"));
            jdbcProps=props;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Connection getNewConnection(){
        String driver=jdbcProps.getProperty("tasks.jdbc.driver");
        String url=jdbcProps.getProperty("tasks.jdbc.url");
        String user=jdbcProps.getProperty("tasks.jdbc.user");
        String pass=jdbcProps.getProperty("tasks.jdbc.pass");
//        String url="jdbc:postgresql://localhost:5432/Crypto";
//        String user="postgres";
//        String pass="javalogin";

        Connection con=null;
        try {
            Class.forName("org.postgresql.Driver");
            if (user!=null && pass!=null)
                con= DriverManager.getConnection(url,user,pass);
            else
                con=DriverManager.getConnection(url);
        } catch (ClassNotFoundException e) {
            System.out.println("Error loading driver "+e);
        } catch (SQLException e) {
            System.out.println("Error getting connection "+e);
        }
        return con;
    }

    public Connection getConnection(){
        try {
            if (instance==null || instance.isClosed())
                instance=getNewConnection();

        } catch (SQLException e) {
            System.out.println("Error DB "+e);
        }
        return instance;
    }
}
