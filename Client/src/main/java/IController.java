public interface IController {
    void addNewTable();
    void removeSelectedItem();
    void handleNewQuery();
    void addNewDatabaseItem(String name);

    void handleInsertData();
    void handleDeleteData();
    void handleUpdateData();
    void handleNewForeignKey();

    void handleNewIndex();

}
