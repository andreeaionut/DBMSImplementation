import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

import java.rmi.RemoteException;

public class TableTreeItem extends AbstractTreeItem {
    private DatabaseTreeItem parentDatabase;

    public TableTreeItem(String name) {
        this.setValue(name);
    }

    @Override
    public ContextMenu getMenu() {
        MenuItem removeTable = new MenuItem("Delete table");
        removeTable.setOnAction(new EventHandler() {
            public void handle(Event t) {
                try {
                    TableTreeItem.this.parentDatabase.getServer().deleteTable(TableTreeItem.this.parentDatabase.getValue().toString(), TableTreeItem.this.getValue().toString());
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Table deleted!");
                    alert.show();
                    TableTreeItem.this.parentDatabase.getController().removeSelectedItem();
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (ManagerException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, e.getMessage());
                    alert.show();
                }
            }
        });

        MenuItem insertData = new MenuItem("Insert");
        insertData.setOnAction(new EventHandler() {
            public void handle(Event t) {
                TableTreeItem.this.parentDatabase.getController().handleInsertData();
            }
        });

        MenuItem deleteData = new MenuItem("Delete");
        deleteData.setOnAction(new EventHandler() {
            public void handle(Event t) {
                TableTreeItem.this.parentDatabase.getController().handleDeleteData();
            }
        });

        MenuItem updateData = new MenuItem("Update");
        updateData.setOnAction(new EventHandler() {
            public void handle(Event t) {
                TableTreeItem.this.parentDatabase.getController().handleUpdateData();
            }
        });

        MenuItem addForeignKey = new MenuItem("New foreign key");
        addForeignKey.setOnAction(new EventHandler() {
            public void handle(Event t) {
                TableTreeItem.this.parentDatabase.getController().handleNewForeignKey();
            }
        });

        MenuItem addIndex = new MenuItem("New index");
        addIndex.setOnAction(new EventHandler() {
            public void handle(Event t) {
                TableTreeItem.this.parentDatabase.getController().handleNewIndex();
            }
        });

        return new ContextMenu(removeTable, addForeignKey, addIndex, insertData, deleteData, updateData);
    }

    public void setParentDatabase(DatabaseTreeItem parentDatabase) {
        this.parentDatabase = parentDatabase;
    }

    public DatabaseTreeItem getParentDatabase() {
        return this.parentDatabase;
    }
}
