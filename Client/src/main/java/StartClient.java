import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class StartClient extends Application {

    private static Scene scene;
    private static Stage stage;

    public static void setScene(Scene scene) {
        StartClient.scene = scene;
    }

    public static void setStage(Stage stage) {
        StartClient.stage = stage;
    }

    public static Scene getScene() {
        return scene;
    }

    public static Stage getStage() {
        return stage;
    }

    public void start(Stage primaryStage) throws Exception {
//        String serverIP = defaultServer;
//        int serverPort = defaultChatPort;
//        try {
//            serverPort = 55552;
//        } catch (NumberFormatException ex) {
//            System.err.println("Wrong port number " + ex.getMessage());
//            System.out.println("Using default port: " + defaultChatPort);
//        }
//        System.out.println("Using server IP " + serverIP);
//        System.out.println("Using server port " + serverPort);
//
//        IServer server = new ServerRpcProxy(serverIP, serverPort);

        stage = primaryStage;

        double width = 600;
        double height = 400;

        primaryStage.setMinWidth(width);
        primaryStage.setMinHeight(height);

        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("LoginView.fxml"));
        Parent root = loader.load();

        LoginRegisterController controller = loader.getController();
        //controller.setServer(server);
        //controller.initView();
        stage.setTitle("Login");
        controller.setStage(stage);
        scene = new Scene(root,width,height);

        primaryStage.setScene(scene);
        primaryStage.show();

//        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
//            public void handle(WindowEvent we) {
//                System.out.println("Stage is closing");
//                ((ServerRpcProxy) server).closeConnection();
//            }
//        });

    }

    public static void main(String[] args){ launch(args); }


}
