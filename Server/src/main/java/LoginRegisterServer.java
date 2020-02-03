import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Service;

import javax.validation.Valid;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class LoginRegisterServer {
    private String accessCode = "JDBC";
    private JdbcUtils dbUtils;
    private HoneyChecker honeyChecker;

    //private IHoneyServer honeyServer;
    private static int defaultChatPort = 55550;
    private static String defaultServer = "localhost";

    private int numberOfHoneyWords = 20;

    public LoginRegisterServer(Properties properties){
        this.dbUtils = new JdbcUtils(properties);
        this.honeyChecker = new HoneyChecker(dbUtils);
//        System.out.println("Using server IP " + defaultServer);
//        System.out.println("Using server port " + defaultChatPort);
//
//        IHoneyServer server = new HoneyServerRpcProxy(defaultServer, defaultChatPort);
//        this.honeyServer = server;
    }

    public boolean register(String email, String username, String password, String accessCode) throws ManagerException {
        if(accessCode.compareTo(this.accessCode)!=0){
            throw new ManagerException("Invalid acces code!");
        }
        if(!Validator.validatePassword(password)){
            throw new ManagerException("Password must contain at least 6 characters, one number and one capital letter!");
        }
        if(existsEmail(email)){
            throw new ManagerException("Email already exists!");
        }
        if(existsUsername(username)){
            throw new ManagerException("Username already exists!");
        }
        this.saveUser(email, username);
        this.saveUserPasswordData(username, password);
        return true;
    }

    private void saveUserPasswordData(String username, String password){
        List<String> sweetWordsList = this.getSweetWordsList(password);
        int currentIndex = 0;
        for(String sweetword : sweetWordsList){
            if(sweetword.compareTo(password)==0){
                this.saveIndex(username, currentIndex);
            }
            this.saveSweetWord(username, sweetword, currentIndex);
            currentIndex++;
        }
    }

    private void saveSweetWord(String username, String sweetWord, int currentIndex) {
        int userId = this.findUserId(username);
        Connection con = dbUtils.getConnection();
        try(PreparedStatement preStmt = con.prepareStatement("insert into SWEETWORDS(user_id, sweetword, index) VALUES (?,?,?)")) {
            preStmt.setInt(1, userId);
            preStmt.setString(2, sweetWord);
            preStmt.setInt(3, currentIndex);
            preStmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error DB" + e);
        }
        try {
            con.close();
        } catch (SQLException e) {
            System.out.println("Error DB" + e);
        }
    }

    private void saveIndex(String username, int passwordIndex) {
        int userId = this.findUserId(username);
        Connection con = dbUtils.getConnection();
        try(PreparedStatement preStmt = con.prepareStatement("insert into indexes(user_id, user_true_index) VALUES (?,?)")) {
            preStmt.setInt(1, userId);
            preStmt.setInt(2, passwordIndex);
            preStmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error DB" + e);
        }
        try {
            con.close();
        } catch (SQLException e) {
            System.out.println("Error DB" + e);
        }
    }

    private int findUserId(String username){
        Connection con=dbUtils.getConnection();
        try(PreparedStatement preStmt=con.prepareStatement("select * from users")) {
            try(ResultSet result=preStmt.executeQuery()) {
                while (result.next()) {
                    int IDuser = result.getInt("user_id");
                    String crtUsername = result.getString("username");
                    if (crtUsername.compareTo(username)==0)
                        return IDuser;
                }
            }
        } catch (SQLException e) {
            System.out.println("Error DB " + e);
        }
        return -1;
    }

    private void saveUser(String email, String username) {
        Connection con = dbUtils.getConnection();
        try(PreparedStatement preStmt = con.prepareStatement("insert into USERS(username, email) VALUES (?,?)")) {
            preStmt.setString(1,username);
            preStmt.setString(2,email);
            preStmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error DB" + e);
        }
        try {
            con.close();
        } catch (SQLException e) {
            System.out.println("Error DB" + e);
        }
    }

    private boolean existsEmail(String email){
        Connection con=dbUtils.getConnection();
        try(PreparedStatement preStmt=con.prepareStatement("select * from USERS WHERE email='" + email+ "';")) {
            try(ResultSet result=preStmt.executeQuery()) {
                while (result.next()) {
                    return true;
                }
            }
        } catch (SQLException e) {
            System.out.println("Error DB " + e);
        }
        return false;
    }

    private boolean existsUsername(String username){
        Connection con=dbUtils.getConnection();
        try(PreparedStatement preStmt=con.prepareStatement("select * from USERS WHERE username='" + username+ "';")) {
            try(ResultSet result=preStmt.executeQuery()) {
                while (result.next()) {
                    return true;
                }
            }
        } catch (SQLException e) {
            System.out.println("Error DB " + e);
        }
        return false;
    }

    private List<String> getSweetWordsList(String password){
        List<String> sweetWords = new ArrayList<>();
        sweetWords.add(password);
        for(int counter = 0; counter < this.numberOfHoneyWords; counter++){
            sweetWords.add(Utils.shuffle(password));
        }
        Collections.shuffle(sweetWords);
        return sweetWords;
    }

    public int size(){
        Connection con=dbUtils.getConnection();
        try(PreparedStatement preStmt=con.prepareStatement("select count(*) from USERS")) {
            try(ResultSet result = preStmt.executeQuery()) {
                if (result.next()) {
                    return result.getInt(1);
                }
            }
        } catch(SQLException ex){
            System.out.println(ex.getMessage());
        }
        return 0;
    }

    public int getSweetWordIndex(int userId, String password){
        Connection con=dbUtils.getConnection();
        try(PreparedStatement preStmt=con.prepareStatement("select * from sweetwords where user_id=" + userId + ";")) {
            try(ResultSet result=preStmt.executeQuery()) {
                while (result.next()) {
                    int sweetWordIndex = result.getInt("index");
                    String sweetWord = result.getString("sweetword");
                    if(sweetWord.compareTo(password)==0){
                        return sweetWordIndex;
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Error DB " + e);
        }
        return -1;
    }

    public boolean login(String username, String password) throws ManagerException {
        int userId = this.findUserId(username);
        if(userId > 0){
            int passIndex = this.getSweetWordIndex(userId, password);
            if(passIndex < 0){
                return false;
            }
            if(honeyChecker.checkIndex(userId, passIndex)){
                return true;
            }
            this.sendAlertEmail(userId);
            throw new ManagerException("");
        }
        return false;
    }

    private void sendAlertEmail(int userId) {
        String email = this.getEmailById(userId);
        EmailService.sendEmail(email);
    }

    private String getEmailById(int userId) {
        Connection con=dbUtils.getConnection();
        try(PreparedStatement preStmt=con.prepareStatement("select * from USERS where user_id=" + userId + ";")) {
            try(ResultSet result=preStmt.executeQuery()) {
                while (result.next()) {
                    return result.getString("email");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error DB " + e);
        }
        return "";
    }

    public boolean changePassword(String username, String oldPassword, String newPassword) throws ManagerException{
        int userId = this.findUserId(username);
        int passIndex = this.getSweetWordIndex(userId, oldPassword);
        if(passIndex == -1){
            throw new ManagerException("Incorrect password!");
        }
        if(honeyChecker.checkIndex(userId, passIndex)){
            if(!Validator.validatePassword(newPassword)){
                throw new ManagerException("Password must contain at least 6 characters, one number and one capital letter!");
            }
            this.deleteOldUserPasswordData(userId);
            this.saveUserPasswordData(username, newPassword);
        }else{
            this.sendAlertEmail(userId);
        }
        return true;
    }

    private void deleteOldUserPasswordData(int userId) {
        this.deleteUserSweetwords(userId);
        this.deleteUserIndex(userId);
    }

    private void deleteUserSweetwords(int userId) {
        Connection con = dbUtils.getConnection();
        try (PreparedStatement preStmt = con.prepareStatement("DELETE from SWEETWORDS WHERE user_id=?")) {
            preStmt.setInt(1,userId);
            preStmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error DB" + e);
        }
        try {
            con.close();
        } catch (SQLException e) {
            System.out.println("Error DB" + e);
        }
    }

    private void deleteUserIndex(int userId) {
        Connection con = dbUtils.getConnection();
        try (PreparedStatement preStmt = con.prepareStatement("DELETE from indexes WHERE user_id=?")) {
            preStmt.setInt(1,userId);
            preStmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error DB" + e);
        }
        try {
            con.close();
        } catch (SQLException e) {
            System.out.println("Error DB" + e);
        }
    }

}
