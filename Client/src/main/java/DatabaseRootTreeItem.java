import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;

import java.rmi.RemoteException;
import java.util.Optional;

public class DatabaseRootTreeItem extends AbstractTreeItem {

    private IServer server;
    private MainController controller;

    public DatabaseRootTreeItem(String name) {
        this.setValue(name);
    }

    private void addNewDbMenuItem(MenuItem menuItem){
        menuItem.setOnAction(new EventHandler() {
            public void handle(Event t) {
                TextInputDialog dialog = new TextInputDialog("");
                dialog.setTitle("New database");
                dialog.setHeaderText("Database name:");
                Optional<String> result = dialog.showAndWait();
                if(!result.isPresent()){
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Please enter a name");
                    alert.show();
                }
                result.ifPresent(name -> {
                    try {
                        DatabaseRootTreeItem.this.server.createDatabase(name);
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Database created!");
                        alert.show();
                        DatabaseRootTreeItem.this.controller.addNewDatabaseItem(name);
                    } catch (ManagerException e) {
                        Alert alert = new Alert(Alert.AlertType.ERROR, e.getMessage());
                        alert.show();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                });
            }
        });
    }

    @Override
    public ContextMenu getMenu(){
        MenuItem addDb = new MenuItem("Add new database");
        addNewDbMenuItem(addDb);
        return new ContextMenu(addDb);
    }

    public void setServer(IServer server) {
        this.server = server;
    }

    public void setController(MainController controller) {
        this.controller = controller;
    }
}
