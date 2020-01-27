import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import javax.xml.soap.Text;
import java.awt.*;
import java.io.IOException;
import java.rmi.RemoteException;

public class LoginRegisterController {
    private static int defaultChatPort = 55550;
    private static String defaultServer = "localhost";

    private String serverIP = defaultServer;
    private int serverPort = defaultChatPort;

    private IServer server;

    private Stage stage;

    @FXML
    private TextField txtUser;
    @FXML
    private PasswordField txtPass;
    @FXML
    private TextField txtEmail;
    @FXML
    private TextField txtUserRegister;
    @FXML
    private PasswordField txtPassRegister;
    @FXML
    private TextField txtAccessCode;

    public LoginRegisterController(){
        try {
            serverPort = 55552;
        } catch (NumberFormatException ex) {
            System.err.println("Wrong port number " + ex.getMessage());
            System.out.println("Using default port: " + defaultChatPort);
        }
        System.out.println("Using server IP " + serverIP);
        System.out.println("Using server port " + serverPort);

        IServer server = new ServerRpcProxy(serverIP, serverPort);
        this.server = server;
    }

    public void handleLogin(){
        String username = this.txtUser.getText();
        String password = this.txtPass.getText();
        if(username == null ||
                password == null || username.compareTo("")==0 || password.compareTo("")==0){
            Alert alert = new Alert(Alert.AlertType.ERROR, "Please complete all fields");
            alert.show();
            return;
        }
        try {
            if(server.login(username, password)){
                this.startDBMS();
            }else{
                Alert alert = new Alert(Alert.AlertType.ERROR, "Incorrect username or password");
                alert.show();
            }
        } catch (ManagerException | RemoteException e) {
            this.showFakeStartPage();
        }
    }

    private void showFakeStartPage() {
        Parent root;
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("FakeMainView.fxml"));
            root = loader.load();
            FakeMainController mainController = loader.getController();
            mainController.initView();
            Stage stage = new Stage();
            stage.setTitle("Databases Management Studio");
            stage.setScene(new Scene(root, 1800, 700));
            stage.show();
            mainController.setStage(stage);
            this.stage.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleRegister(){
        String email = this.txtEmail.getText();
        String username = this.txtUserRegister.getText();
        String password = this.txtPassRegister.getText();
        String accessCode = this.txtAccessCode.getText();
        if(email == null ||
                username == null ||
                password == null ||
                accessCode == null || email.compareTo("")==0 || username.compareTo("")==0 || password.compareTo("")==0 || accessCode.compareTo("")==0){
            Alert alert = new Alert(Alert.AlertType.ERROR, "Please complete all fields");
            alert.show();
            return;
        }
        try {
            if(server.register(email, username, password, accessCode)){
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Registration completed!");
                alert.show();
            }
        } catch (ManagerException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, e.getMessage());
            alert.show();
        }
    }

    private void startDBMS() {
        Parent root;
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("MainView.fxml"));
            root = loader.load();
            MainController mainController = loader.getController();
            mainController.setServer(server);
            mainController.initView();
            Stage stage = new Stage();
            stage.setTitle("Databases Management Studio");
            stage.setScene(new Scene(root, 1800, 700));
            stage.show();
            mainController.setStage(stage);
            this.stage.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setServer(IServer server) {
        this.server = server;
    }
}