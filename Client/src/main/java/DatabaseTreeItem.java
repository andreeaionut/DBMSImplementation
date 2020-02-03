import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.*;

import java.rmi.RemoteException;

public class DatabaseTreeItem extends AbstractTreeItem {
    private IServer server;
    private IController controller;

    public DatabaseTreeItem(String name, IController controller) {
        this.setValue(name);
        this.controller = controller;
    }

    @Override
    public ContextMenu getMenu(){
        MenuItem addNewTable = new MenuItem("Add new table");
        addNewTable.setOnAction(new EventHandler() {
            public void handle(Event t) {
                DatabaseTreeItem.this.controller.addNewTable();
            }
        });

        MenuItem deleteDatabase = new MenuItem("Delete");
        deleteDatabase.setOnAction(new EventHandler() {
            public void handle(Event t) {
                try {
                    DatabaseTreeItem.this.server.deleteDatabase(DatabaseTreeItem.this.getValue().toString());
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Database deleted!");
                    alert.show();
                    DatabaseTreeItem.this.controller.removeSelectedItem();
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (ManagerException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, e.getMessage());
                    alert.show();
                }
            }
        });
        MenuItem newQuery = new MenuItem("New query");
        newQuery.setOnAction(new EventHandler() {
            public void handle(Event t) {
                DatabaseTreeItem.this.getController().handleNewQuery();
            }
        });

        return new ContextMenu(addNewTable, deleteDatabase, newQuery);
    }

    public void setServer(IServer server) {
        this.server = server;
    }

    public IServer getServer(){ return this.server; }

    public void setController(IController controller) {
        this.controller = controller;
    }

    public IController getController() {return this.controller;}
}
