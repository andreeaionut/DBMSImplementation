import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ContextMenuBuilder;
import javafx.scene.control.MenuItemBuilder;
import javafx.scene.control.TreeCell;

public class MyTreeCell extends TreeCell<String> {
    private ContextMenu rootContextMenu;

    public MyTreeCell() {
        // instantiate the root context menu
        rootContextMenu =
                ContextMenuBuilder.create()
                        .items(
                                MenuItemBuilder.create()
                                        .text("Menu Item")
                                        .onAction(
                                                new EventHandler<ActionEvent>() {
                                                    public void handle(ActionEvent arg0) {
                                                        System.out.println("Menu Item Clicked!");
                                                    }
                                                }
                                        )
                                        .build()
                        )
                        .build();
    }


}
