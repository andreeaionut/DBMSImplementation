import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.TableRow;
import javafx.scene.control.cell.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MainController implements IObserver, IController {

    @FXML
    private Pane paneDbmsTreeView;
    @FXML
    private TableView tableViewFields;
    @FXML
    private ComboBox cBoxTables;
    @FXML
    private Pane paneTableFields;
    @FXML
    private Pane paneForeignKey;
    @FXML
    private Pane paneIndexFields;
    @FXML
    private Pane paneTableData;
    @FXML
    private VBox vBoxIndexFields;
    @FXML
    private RadioButton rBtnUniqueIndex;

    @FXML
    private VBox vBoxInsertData;
    @FXML
    private Button btnInsertDeleteData;
    @FXML
    private Pane paneRBtnAndOr;
    @FXML
    private RadioButton rBtnOr;
    @FXML
    private RadioButton rBtnAnd;

    @FXML
    private Pane paneUpdateData;
    @FXML
    private VBox vBoxUpdateData;

    @FXML
    private Pane paneQuery;
    @FXML
    private ComboBox cBoxFrom;
    @FXML
    private VBox vBoxSelect;
    @FXML
    private Pane paneQueryResult;
    @FXML
    private TableView tvQueryResult;

    @FXML
    private VBox vBoxWhere;

    @FXML
    private TextField tfDateQuery;

    @FXML
    private VBox vBoxSelectResult;
    @FXML
    private ComboBox cBoxJoin;
    @FXML
    private CheckBox chkJoin;
    @FXML
    private VBox vbJoin;
    @FXML
    private VBox vbHaving;

    private String lastJoinTable;

    private IServer server;
    private Stage stage;
    private List<String> selectedTableFields = new ArrayList<>();

    @FXML
    private TreeView<String> tree;

    private RadioButton rBtnIndex;

    @FXML
    private VBox vbGroupBy;
    @FXML
    private ComboBox cbGroupBy;

    public MainController(){
        try {
            UnicastRemoteObject.exportObject(this,0);
        } catch (RemoteException e) {
            System.out.println("Error exporting object "+e);
        }
    }

    protected void initView() {
        this.paneTableFields.setVisible(false);
        this.paneForeignKey.setVisible(false);
        this.paneIndexFields.setVisible(false);
        this.paneTableData.setVisible(false);
        TreeItem treeRoot = new TreeItem();
        treeRoot.setExpanded(true);
        DatabaseRootTreeItem dbs = new DatabaseRootTreeItem("Databases", this);
        dbs.setServer(this.server);
        dbs.setController(this);
        try {
            List<String> databases = this.server.getDatabases();
            if(databases == null || databases.size() == 0){
                return;
            }
            for (String db : databases) {
                DatabaseTreeItem database = new DatabaseTreeItem (db, this);
                database.setServer(this.server);
                database.setController(this);

                List<String> tables = this.server.getTables(db);
                for(String table : tables){
                    TableTreeItem tableTreeItem = new TableTreeItem(table);
                    tableTreeItem.setParentDatabase(database);

                    database.getChildren().add(tableTreeItem);
                }
                dbs.getChildren().add(database);
            }
        } catch (RemoteException | ManagerException e) {
            e.printStackTrace();
        }
        treeRoot.getChildren().add(dbs);
        TreeView<String> treeView = new TreeView<String>(treeRoot);
        treeView.setShowRoot(false);

        treeView.setCellFactory(new Callback<TreeView<String>,TreeCell<String>>(){
            public TreeCell<String> call(TreeView<String> p) {
                return new TreeCellImpl();
            }
        });

        this.tree = treeView;
        this.paneDbmsTreeView.getChildren().add(treeView);

        this.createTableFields();
        this.paneQuery.setVisible(false);

        this.cBoxFrom.valueProperty().addListener((obs, oldVal, newVal) -> {
            MainController.this.lastJoinTable = newVal.toString();
            MainController.this.vbJoin.getChildren().clear();
        });

        this.cbGroupBy.valueProperty().addListener((obs, oldVal, newVal) -> {
            MainController.this.addProjectionFieldsFromGroupBy(newVal.toString());
        });

        this.chkJoin.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if(newValue == true){
                    MainController.this.vbJoin.setVisible(true);
                    MainController.this.vbJoin.getChildren().add(MainController.this.getHBoxJoin());
                }else{
                    MainController.this.vbJoin.getChildren().clear();
                    MainController.this.lastJoinTable = (MainController.this.cBoxFrom.getSelectionModel().getSelectedItem().toString());
                    MainController.this.vbJoin.setVisible(false);
                }
            }
        });
    }

    private void addProjectionFieldsFromGroupBy(String groupByValue) {
        String table = groupByValue.split("\\.")[0];
        DatabaseTreeItem databaseTreeItem = (DatabaseTreeItem)this.tree.getSelectionModel().getSelectedItem();
        List<TableField> tableFields = null;
        try {
            tableFields = this.server.getTableFields(databaseTreeItem.getValue().toString(), table);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (ManagerException e) {
            e.printStackTrace();
        }
        for(TableField tableField : tableFields){
            if(tableField.getName().compareTo(groupByValue.split("\\.")[1])==0){
                for(AggregateOperators operators : AggregateOperators.values()){
                    CheckBox checkBox = new CheckBox();
                    checkBox.setText(operators + "(" + tableField.getName() + ")");
                    checkBox.setId("#" + table + "#" + System.currentTimeMillis());
                    this.vBoxSelect.getChildren().add(checkBox);
                }
                return;
            }
        }
    }

    private HBox getHBoxJoin(){
        HBox hbJoin = new HBox();
        ComboBox cbJoin = new ComboBox();
        for(JoinType joinType : JoinType.values()){
            cbJoin.getItems().addAll(joinType);
        }
        cbJoin.setId("#"+DateUtils.GETDATE().toString());
        ComboBox cbTables = new ComboBox();
        DatabaseTreeItem databaseTreeItem = (DatabaseTreeItem)this.tree.getSelectionModel().getSelectedItem();
        try {
            cbTables.setItems(FXCollections.observableArrayList(this.server.getReferencedTables(databaseTreeItem.getValue().toString(), this.lastJoinTable)));
            cbTables.setId(DateUtils.GETDATE().toString());
            cbTables.valueProperty().addListener((obs, oldVal, newVal) -> {
                MainController.this.lastJoinTable = newVal.toString();
                MainController.this.addProjectionFields(newVal.toString());
            });
        } catch (ManagerException | RemoteException e) {
            e.printStackTrace();
        }
        hbJoin.getChildren().addAll(cbJoin, cbTables);
        return hbJoin;
    }

    private void addProjectionFields(String table){
        DatabaseTreeItem databaseTreeItem = (DatabaseTreeItem)this.tree.getSelectionModel().getSelectedItem();
        try {
            List<TableField> tableFields = this.server.getTableFields(databaseTreeItem.getValue().toString(), table);
            for(TableField tableField : tableFields){
                CheckBox checkBox = new CheckBox();
                checkBox.setText(tableField.getName());
                checkBox.setId(table + "#" + System.currentTimeMillis());
                this.vBoxSelect.getChildren().add(checkBox);
            }
        } catch (RemoteException | ManagerException e) {
            e.printStackTrace();
        }
    }

    public void handleAddNewJoin(){
        this.vbJoin.getChildren().add(this.getHBoxJoin());
    }

    private void createTableFields() {
        Callback<TableColumn, TableCell> numericFactory = new Callback<TableColumn, TableCell>() {
            @Override
            public TableCell call(TableColumn p) {
                return new NumericEditableTableCell();
            }
        };
        TableColumn<TableField, String> nameColumn = new TableColumn<>("Column Name");
        TableColumn<TableField, FieldType> typeColumn = new TableColumn<>("Data Type");
        TableColumn lengthColumn = createLengthColumn(numericFactory);
        TableColumn<TableField, Boolean> pkColumn = new TableColumn<>("Primary key");
        TableColumn<TableField, Boolean> uniqueColumn = new TableColumn<>("Unique");
        TableColumn<TableField, TableField> actionCol = new TableColumn<>("Delete");

        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        lengthColumn.setCellValueFactory(new PropertyValueFactory<>("length"));
        ObservableList<FieldType> fieldTypesList = FXCollections.observableArrayList(FieldType.values());
        typeColumn.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<TableField, FieldType>, ObservableValue<FieldType>>() {
            @Override
            public ObservableValue<FieldType> call(TableColumn.CellDataFeatures<TableField, FieldType> param) {
                TableField tableField = param.getValue();
                // INT, CHAR, DATE
                String fieldCode = tableField.getFieldType();
                FieldType fieldType = FieldType.getByCode(fieldCode);
                return new SimpleObjectProperty<>(fieldType);
            }
        });
        typeColumn.setCellFactory(ComboBoxTableCell.forTableColumn(fieldTypesList));
        typeColumn.setOnEditCommit((TableColumn.CellEditEvent<TableField, FieldType> event) -> {
            TablePosition<TableField, FieldType> pos = event.getTablePosition();

            FieldType newGender = event.getNewValue();

            int row = pos.getRow();
            TableField person = event.getTableView().getItems().get(row);

            person.setFieldType(newGender.getCode());
        });
        typeColumn.setMinWidth(120);

        pkColumn.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<TableField, Boolean>, ObservableValue<Boolean>>() {
            @Override
            public ObservableValue<Boolean> call(TableColumn.CellDataFeatures<TableField, Boolean> param) {
                TableField person = param.getValue();
                SimpleBooleanProperty booleanProp = new SimpleBooleanProperty(person.getIsPrimaryKey());
                booleanProp.addListener(new ChangeListener<Boolean>() {
                    @Override
                    public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue,
                                        Boolean newValue) {
                        person.setPrimaryKey(newValue);
                    }
                });
                return booleanProp;
            }
        });
        pkColumn.setCellFactory(new Callback<TableColumn<TableField, Boolean>, //
                TableCell<TableField, Boolean>>() {
            @Override
            public TableCell<TableField, Boolean> call(TableColumn<TableField, Boolean> p) {
                CheckBoxTableCell<TableField, Boolean> cell = new CheckBoxTableCell<TableField, Boolean>();
                cell.setAlignment(Pos.CENTER);
                return cell;
            }
        });

        uniqueColumn.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<TableField, Boolean>, ObservableValue<Boolean>>() {
            @Override
            public ObservableValue<Boolean> call(TableColumn.CellDataFeatures<TableField, Boolean> param) {
                TableField person = param.getValue();
                SimpleBooleanProperty booleanProp = new SimpleBooleanProperty(person.getIsUnique());
                booleanProp.addListener(new ChangeListener<Boolean>() {
                    @Override
                    public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue,
                                        Boolean newValue) {
                        person.setUnique(newValue);
                    }
                });
                return booleanProp;
            }
        });
        uniqueColumn.setCellFactory(new Callback<TableColumn<TableField, Boolean>, //
                TableCell<TableField, Boolean>>() {
            @Override
            public TableCell<TableField, Boolean> call(TableColumn<TableField, Boolean> p) {
                CheckBoxTableCell<TableField, Boolean> cell = new CheckBoxTableCell<TableField, Boolean>();
                cell.setAlignment(Pos.CENTER);
                return cell;
            }
        });

        this.tableViewFields.getColumns().addAll(nameColumn, typeColumn, lengthColumn, pkColumn, uniqueColumn, actionCol);
        this.tableViewFields.setEditable(true);
        tableViewFields.setRowFactory(new Callback<TableView<TableField>, TableRow<TableField>>() {
            @Override
            public TableRow<TableField> call(TableView<TableField> param) {
                TableRow<TableField> row = new TableRow<TableField>();
                row.setOnMouseClicked(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        if ((event.getClickCount() == 2)) {
                            TableField myItem = new TableField();
                            myItem.setFieldType(FieldType.INT.getCode());
                            myItem.setPrimaryKey(false);
                            tableViewFields.getItems().add(myItem);
                        }
                    }
                });
                return row;
            }
        });

        nameColumn.setCellFactory(TextFieldTableCell.<TableField>forTableColumn());
        nameColumn.setOnEditCommit(new EventHandler<TableColumn.CellEditEvent<TableField, String>>() {
            @Override
            public void handle(TableColumn.CellEditEvent t) {
                ((TableField) t.getTableView().getItems().get(
                        t.getTablePosition().getRow())
                ).setName((String) t.getNewValue());
            }
        });

        actionCol.setCellValueFactory(
                param -> new ReadOnlyObjectWrapper<>(param.getValue())
        );
        actionCol.setCellFactory(param -> new TableCell<TableField, TableField>() {
            private Button deleteButton = new Button("X");
            @Override
            protected void updateItem(TableField person, boolean empty) {
                super.updateItem(person, empty);
                if (person == null) {
                    setGraphic(null);
                    return;
                }
                setGraphic(deleteButton);
                deleteButton.setOnAction(
                        event -> getTableView().getItems().remove(person)
                );
            }
        });
    }

    private void initFieldsTable(){
        this.paneQuery.setVisible(false);
        this.tableViewFields.getItems().clear();
        TableField tableField = new TableField();
        tableField.setPrimaryKey(true);
        tableField.setFieldType(FieldType.INT.getCode());
        tableField.setName("id");
        ObservableList<TableField> list = FXCollections.observableArrayList(tableField);
        this.tableViewFields.setItems(list);
    }

    private TableColumn createLengthColumn(Callback<TableColumn,TableCell> numericFactory) {
        TableColumn ageCol = new TableColumn("Length");
        ageCol.setMinWidth(50);
        ageCol.setCellValueFactory(new PropertyValueFactory<TableField, String>("length"));
        ageCol.setCellFactory(numericFactory);
        ageCol.setOnEditCommit(new EventHandler<TableColumn.CellEditEvent<TableField, Long>>() {
            @Override
            public void handle(TableColumn.CellEditEvent<TableField, Long> t) {
                long newAge = t.getNewValue().longValue();
                ((TableField) t.getTableView().getItems().get(t.getTablePosition().getRow())).setLength(newAge);
            }
        });
        return ageCol;
    }

    public IServer getServer() {
        return server;
    }

    public void setServer(IServer server) {
        this.server = server;
    }

    public void eventHappened() throws RemoteException {
        this.initView();
    }

    public void removeSelectedItem() {
        TreeItem c = (TreeItem)this.tree.getSelectionModel().getSelectedItem();
        c.getParent().getChildren().remove(c);
        this.paneUpdateData.setVisible(false);
        this.paneTableData.setVisible(false);
        this.paneQuery.setVisible(false);
        this.paneIndexFields.setVisible(false);
        this.paneForeignKey.setVisible(false);
    }

    public void addNewDatabaseItem(String name){
        this.paneQuery.setVisible(false);
        DatabaseTreeItem databaseTreeItem = new DatabaseTreeItem(name, this);
        databaseTreeItem.setServer(server);
        databaseTreeItem.setController(this);
        this.tree.getRoot().getChildren().get(0).getChildren().add(databaseTreeItem);
    }

    public void handleSaveNewTable(){
        this.paneQuery.setVisible(false);
        if(!this.checkValidFields()){
            Alert alert = new Alert(Alert.AlertType.ERROR, "Please complete all fields!");
            alert.show();
            return;
        }
        TextInputDialog dialog = new TextInputDialog("");
        dialog.setTitle("New table");
        dialog.setHeaderText("Table name:");
        Optional<String> result = dialog.showAndWait();

        result.ifPresent(name -> {
            CreateTableContainer createTableContainer = new CreateTableContainer();
            createTableContainer.setTable(name);
            DatabaseTreeItem c = (DatabaseTreeItem)this.tree.getSelectionModel().getSelectedItem();
            createTableContainer.setDatabase(c.getValue().toString());
            ObservableList<TableField> tableFields = this.tableViewFields.getItems();
            for (TableField tableField : tableFields) {
                createTableContainer.addTableField(tableField);
            }
            try {
                this.server.createTable(createTableContainer);
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Table created!");
                alert.show();
                this.newTableAdded(name);
                this.tableViewFields.getItems().clear();
                this.paneTableFields.setVisible(false);
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (ManagerException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, e.getMessage());
                alert.show();
            }
        });
    }

    private boolean checkValidFields() {
        ObservableList<TableField> tableFields = this.tableViewFields.getItems();
        for (TableField tableField : tableFields) {
            if(tableField.getName() == null || tableField.getName().compareTo("") == 0){
                return false;
            }
            if(tableField.getFieldType().compareTo("C") == 0 && (tableField.getLength() == null || tableField.getLength() == 0 || tableField.getLength() < 0)){
                return false;
            }
        }
        return true;
    }

    public void addNewTable(){
        this.paneQuery.setVisible(false);
        //this.tableViewTableData.getItems().clear();
        //this.tableViewTableData.getColumns().clear();
        this.paneTableData.setVisible(false);

        this.initFieldsTable();
        this.paneTableFields.setVisible(true);
    }

    public void newTableAdded(String name) {
        this.paneQuery.setVisible(false);
        DatabaseTreeItem c = (DatabaseTreeItem)this.tree.getSelectionModel().getSelectedItem();
        TableTreeItem tableTreeItem = new TableTreeItem(name);
        tableTreeItem.setParentDatabase(c);
        c.getChildren().add(tableTreeItem);
    }

    public void handleNewForeignKey(){
        this.paneQuery.setVisible(false);
        this.cBoxTables.getItems().clear();
        this.paneForeignKey.setVisible(true);
        this.cBoxTables.getItems().clear();
        TableTreeItem tableTreeItem = (TableTreeItem)this.tree.getSelectionModel().getSelectedItem();
        List<String> tables = null;
        try {
            tables = server.getTables((String) tableTreeItem.getParentDatabase().getValue());
            this.cBoxTables.getItems().addAll(tables);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (ManagerException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, e.getMessage());
            alert.show();
        }
    }

    public void handleSaveForeignKey(){
        this.paneQuery.setVisible(false);
        TableTreeItem tableTreeItem = (TableTreeItem)this.tree.getSelectionModel().getSelectedItem();
        try {
            this.server.addForeignKey(tableTreeItem.getParentDatabase().getValue().toString(), tableTreeItem.getValue().toString(), this.cBoxTables.getValue().toString());
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Foreign key saved!");
            alert.show();
            this.paneForeignKey.setVisible(false);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (ManagerException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, e.getMessage());
            alert.show();
        }
    }

    public void handleNewIndex() {
        this.paneQuery.setVisible(false);
        this.paneUpdateData.setVisible(false);
        this.paneForeignKey.setVisible(false);
        this.paneTableFields.setVisible(false);
        this.paneTableData.setVisible(false);
        this.paneRBtnAndOr.setVisible(false);

        this.paneIndexFields.setVisible(true);
        TableTreeItem tableTreeItem = (TableTreeItem)this.tree.getSelectionModel().getSelectedItem();
        List<TableField> tableFields = new ArrayList<>();
        try {
            tableFields = this.server.getTableFields(tableTreeItem.getParentDatabase().getValue().toString(), tableTreeItem.getValue().toString());
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (ManagerException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, e.getMessage());
            alert.show();
        }
        if(tableFields == null || tableFields.size() == 0){
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Table has no fields!");
            alert.show();
            this.vBoxIndexFields.getChildren().clear();
            this.paneIndexFields.setVisible(false);
            this.selectedTableFields.clear();
            return;
        }
        ToggleGroup group = new ToggleGroup();
        for(TableField tableField : tableFields){
            RadioButton rbTableField = new RadioButton();
            rbTableField.setText(tableField.getName());
            rbTableField.setId(tableField.getName() + "#" + tableField.getFieldIndex());
            rbTableField.setToggleGroup(group);
            EventHandler<ActionEvent> event = new EventHandler<ActionEvent>() {
                public void handle(ActionEvent e) {
                    MainController.this.rBtnIndex = rbTableField;
                }
            };
            rbTableField.setOnAction(event);
            this.vBoxIndexFields.getChildren().add(rbTableField);
        }
    }

    public void handleSaveIndex(){
        TextInputDialog dialog = new TextInputDialog("");
        dialog.setTitle("New index");
        dialog.setHeaderText("Index name:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            System.out.println(name);
            TableTreeItem tableTreeItem = (TableTreeItem)this.tree.getSelectionModel().getSelectedItem();
            try {
                IndexContainer indexContainer = new IndexContainer();
                indexContainer.setDatabase(tableTreeItem.getParentDatabase().getValue().toString());
                indexContainer.setTable(tableTreeItem.getValue().toString());
                indexContainer.setIndexName(name);
                indexContainer.setUnique(this.rBtnUniqueIndex.isSelected());
                TableField tableField = new TableField();
                tableField.setName(this.rBtnIndex.getId().split("#")[0]);
                tableField.setFieldIndex(Integer.valueOf(this.rBtnIndex.getId().split("#")[1]));
                indexContainer.setTableField(tableField);
                this.server.createIndex(indexContainer);
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Index saved!");
                alert.show();
                this.vBoxIndexFields.getChildren().clear();
                this.paneIndexFields.setVisible(false);
                this.selectedTableFields.clear();
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (ManagerException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, e.getMessage());
                alert.show();
            }
        });
    }

    private int getNumberOfFields(VBox vBox){
        ObservableList<Node> hBoxes = vBox.getChildren();
        int data = 0;
        for(Node hBox : hBoxes){
            HBox child = (HBox) hBox;
            ObservableList<Node> hBoxChildren = child.getChildren();
            for(Node hBoxChild : hBoxChildren){
                if(hBoxChild instanceof TextField || hBoxChild instanceof DatePicker){
                    data++;
                }
            }
        }
        return data;
    }

    private List<TableData> getDataFromVbox(VBox vBox){
        ObservableList<Node> hBoxes = vBox.getChildren();
        List<TableData> insertionData = new ArrayList<>();
        int data = 0;
        for(Node hBox : hBoxes){
            HBox child = (HBox) hBox;
            ObservableList<Node> hBoxChildren = child.getChildren();
            for(Node hBoxChild : hBoxChildren){
                if(hBoxChild instanceof TextField){
                    if(((TextField) hBoxChild).getText() != null && ((TextField) hBoxChild).getText().compareTo("")!=0){
                        TableData tableData = new TableData();
                        if(hBoxChild.getId().contains("#")){
                            tableData.setFieldName(hBoxChild.getId().replace("#", ""));
                        }else{
                            tableData.setFieldName(hBoxChild.getId());
                        }
                        tableData.setFieldValue(((TextField) hBoxChild).getText());
                        tableData.setFieldIndex(data);
                        tableData.setUnique(hBoxChild.getId().contains("#"));
                        insertionData.add(tableData);
                    }
                    data++;
                }else{
                    if(hBoxChild instanceof DatePicker){
                        DatePicker datePicker = (DatePicker) hBoxChild;
                        LocalDate localDate = datePicker.getValue();
                        if(localDate!=null){
                            TableData tableData = new TableData();
                            if(hBoxChild.getId().contains("#")){
                                tableData.setFieldName(hBoxChild.getId().replace("#", ""));
                            }else{
                                tableData.setFieldName(hBoxChild.getId());
                            }
                            tableData.setFieldValue(localDate.toString());
                            tableData.setFieldIndex(data);
                            tableData.setUnique(hBoxChild.getId().contains("#"));
                            insertionData.add(tableData);
                        }
                        data++;
                    }
                }
            }
        }
        return insertionData;
    }

    public void handleInsertDeleteData(){
        List<TableData> insertionData = this.getDataFromVbox(this.vBoxInsertData);
        int data = this.getNumberOfFields(this.vBoxInsertData);
        TableTreeItem tableTreeItem = (TableTreeItem)this.tree.getSelectionModel().getSelectedItem();
        try {
            switch(this.btnInsertDeleteData.getText()){
                case "Insert":{
                    if(insertionData.size() == data) {
                        this.server.insertData(tableTreeItem.getParentDatabase().getValue().toString(), tableTreeItem.getValue().toString(), insertionData);
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Data saved!");
                        alert.show();
                        this.paneRBtnAndOr.setVisible(false);
                        this.paneTableData.setVisible(false);
                        this.paneUpdateData.setVisible(false);
                    }else{
                        Alert alert = new Alert(Alert.AlertType.WARNING, "Please complete all fields!");
                        alert.show();
                    }
                    return;
                }
                case "Delete":{
                    if(insertionData.size() > 0){
                        if(rBtnAnd.isSelected()){
                            this.server.deleteData(tableTreeItem.getParentDatabase().getValue().toString(), tableTreeItem.getValue().toString(), insertionData, LogicalOperator.AND);
                        }else{
                            this.server.deleteData(tableTreeItem.getParentDatabase().getValue().toString(), tableTreeItem.getValue().toString(), insertionData, LogicalOperator.OR);
                        }
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Data deleted!");
                        alert.show();
                        this.paneRBtnAndOr.setVisible(false);
                        this.paneTableData.setVisible(false);
                        this.paneUpdateData.setVisible(false);
                    }else{
                        Alert alert = new Alert(Alert.AlertType.WARNING, "Please complete at least one field!");
                        alert.show();
                    }
                    return;
                }
                case "Update":{
                    List<TableData> updateData = this.getDataFromVbox(this.vBoxUpdateData);
                    if(insertionData.size() > 0 && updateData.size() > 0){
                        if(rBtnAnd.isSelected()){
                            this.server.updateData(tableTreeItem.getParentDatabase().getValue().toString(), tableTreeItem.getValue().toString(), insertionData, LogicalOperator.AND, updateData);
                        }else{
                            this.server.updateData(tableTreeItem.getParentDatabase().getValue().toString(), tableTreeItem.getValue().toString(), insertionData, LogicalOperator.OR, updateData);
                        }
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Data updated!");
                        alert.show();
                        this.paneRBtnAndOr.setVisible(false);
                        this.paneTableData.setVisible(false);
                        this.paneUpdateData.setVisible(false);
                    }else{
                        Alert alert = new Alert(Alert.AlertType.WARNING, "Please complete at least one field!");
                        alert.show();
                    }
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (ManagerException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, e.getMessage());
            alert.show();
        }
    }

    public void handleInsertData() {
        this.vBoxInsertData.setVisible(true);
        this.paneForeignKey.setVisible(false);
        this.paneQuery.setVisible(false);
        this.paneRBtnAndOr.setVisible(false);
        this.paneUpdateData.setVisible(false);
        this.vBoxInsertData.getChildren().clear();
        this.paneTableData.setVisible(true);
        this.btnInsertDeleteData.setText("Insert");
        this.createVBoxInsertDeleteData(this.vBoxInsertData, true);
    }

    public void handleDeleteData() {
        this.vBoxInsertData.setVisible(true);
        this.paneQuery.setVisible(false);
        this.paneRBtnAndOr.setVisible(true);
        this.vBoxInsertData.getChildren().clear();
        this.paneTableData.setVisible(true);
        this.btnInsertDeleteData.setText("Delete");
        ToggleGroup group = new ToggleGroup();
        rBtnAnd.setToggleGroup(group);
        rBtnAnd.setSelected(true);
        rBtnOr.setToggleGroup(group);
        this.createVBoxInsertDeleteData(this.vBoxInsertData, true);
    }

    private void createVBoxInsertDeleteData(VBox vBox, boolean withPrimaryKey){
        TableTreeItem tableTreeItem = (TableTreeItem)this.tree.getSelectionModel().getSelectedItem();
        try {
            List<TableField> tableFields = this.server.getTableFields(tableTreeItem.getParentDatabase().getValue().toString(), tableTreeItem.getValue().toString());
            for(TableField tableField : tableFields){
                this.createVboxByTableField(tableField, vBox, withPrimaryKey);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (ManagerException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, e.getMessage());
            alert.show();
        }
    }

    private void createVboxByTableField(TableField tableField, VBox vBox, boolean withPrimaryKey){
        Label label = new Label(tableField.getName());
        label.setMinWidth(100);
        if(tableField.getFieldType().equals("date")){
            final DatePicker datePicker = new DatePicker();
            datePicker.setOnAction(new EventHandler() {
                public void handle(Event t) {
                    LocalDate date = datePicker.getValue();
                    System.err.println("Selected date: " + date);
                }
            });
            if(tableField.getIsUnique()){
                datePicker.setId(tableField.getName()+"#");
            }else{
                datePicker.setId(tableField.getName());
            }
            HBox hBox = new HBox();
            hBox.getChildren().addAll(label, datePicker);
            vBox.getChildren().addAll(hBox);
            return;
        }
        TextField textField = new TextField();
        textField.setMinWidth(100);
        if(tableField.getIsUnique()){
            textField.setId(tableField.getName()+"#");
        }else{
            textField.setId(tableField.getName());
        }
        if(tableField.getFieldType().equals("int")){
            textField.textProperty().addListener(new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> observable, String oldValue,
                                    String newValue) {
                    if (!newValue.matches("\\d*")) {
                        textField.setText(newValue.replaceAll("[^\\d]", ""));
                    }
                }
            });
        }
        if((!withPrimaryKey &&( tableField.getIsPrimaryKey() || tableField.isForeignKey()))){
            textField.setDisable(true);
        }
        HBox hBox = new HBox();
        hBox.getChildren().addAll(label, textField);
        vBox.getChildren().addAll(hBox);
    }

    public void handleUpdateData() {
        this.vBoxInsertData.setVisible(true);
        this.paneQuery.setVisible(false);
        this.paneRBtnAndOr.setVisible(true);
        this.vBoxInsertData.getChildren().clear();
        this.paneTableData.setVisible(true);
        this.btnInsertDeleteData.setText("Update");
        ToggleGroup group = new ToggleGroup();
        rBtnAnd.setToggleGroup(group);
        rBtnAnd.setSelected(true);
        rBtnOr.setToggleGroup(group);
        this.vBoxUpdateData.getChildren().clear();
        this.createVBoxInsertDeleteData(vBoxInsertData, true);
        this.createVBoxInsertDeleteData(vBoxUpdateData, false);
        this.paneUpdateData.setVisible(true);
    }

    public void handleNewQuery() {
        this.paneUpdateData.setVisible(false);
        this.paneTableData.setVisible(false);
        this.vBoxInsertData.setVisible(false);
        this.paneQuery.setVisible(true);
        this.vBoxWhere.getChildren().clear();
        this.cBoxFrom.getItems().clear();
        this.cbGroupBy.getItems().clear();
        this.vbHaving.getChildren().clear();
        this.vBoxSelectResult.getChildren().clear();
        this.vBoxSelect.getChildren().clear();
        DatabaseTreeItem databaseTreeItem = (DatabaseTreeItem)this.tree.getSelectionModel().getSelectedItem();
        try {
            List<String> tables = this.server.getTables(databaseTreeItem.getValue().toString());
            this.cBoxFrom.getItems().addAll(tables);
            cBoxFrom.valueProperty().addListener(new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue ov, String oldValue, String newValue) {
                    MainController.this.createVboxSelect(databaseTreeItem.getValue().toString(), newValue);
                    MainController.this.vBoxWhere.getChildren().clear();
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (ManagerException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, e.getMessage());
            alert.show();
        }
    }

    private void createVboxSelect(String database, String table) {
        this.vBoxSelect.getChildren().clear();
        try {
            List<TableField> tableFields = this.server.getTableFields(database, table);
            if(tableFields.size()>0){
                CheckBox checkBox = new CheckBox("*");
                checkBox.selectedProperty().addListener(new ChangeListener<Boolean>() {
                    public void changed(ObservableValue<? extends Boolean> ov,
                                        Boolean old_val, Boolean new_val) {
                        List<Node> cbs = vBoxSelect.getChildren();
                        for(Node cb : cbs){
                            if(cb instanceof CheckBox){
                                ((CheckBox) cb).setSelected(new_val);
                            }
                        }
                    }
                });
                this.vBoxSelect.getChildren().add(checkBox);
            }
            for(TableField tableField : tableFields){
                CheckBox checkBox = new CheckBox(tableField.getName());
                checkBox.setId(table + "#" + System.currentTimeMillis());
                this.vBoxSelect.getChildren().add(checkBox);
            }
        } catch (RemoteException | ManagerException e) {
            e.printStackTrace();
        }
    }

    public void handleSelect(){
        List<Node> cbs = this.vBoxSelect.getChildren();
        String selectedTable = this.cBoxFrom.getSelectionModel().getSelectedItem().toString();
        DatabaseTreeItem databaseTreeItem = (DatabaseTreeItem)this.tree.getSelectionModel().getSelectedItem();
        List<String> aggregateProjectionFields = new ArrayList<>();
        try {
            List<TableField> tableFields = this.server.getTableFields(databaseTreeItem.getValue().toString(), selectedTable);
            this.addTableFieldsFromJoinTables(tableFields);
            List<TableField> projectionFields = new ArrayList<>();
            for(Node cb : cbs){
                if(cb instanceof CheckBox){
                    if(((CheckBox) cb).isSelected() && ((CheckBox) cb).getText().compareTo("*")!=0){
                        for(TableField tableField : tableFields){
                            if(!((CheckBox) cb).getId().startsWith("#") && ((CheckBox) cb).getText().compareTo(tableField.getName())==0 && cb.getId().split("#")[0].compareTo(tableField.getTable())==0){
                                projectionFields.add(tableField);
                                break;
                            }
                            if(((CheckBox) cb).getId().startsWith("#") && ((CheckBox) cb).getText().contains(tableField.getName())){
                                aggregateProjectionFields.add(((CheckBox) cb).getText());
                            }
                        }
                    }
                }
            }
            if(projectionFields.size()==0 && aggregateProjectionFields.size() == 0){
                Alert alert = new Alert(Alert.AlertType.WARNING, "Please select at least one field");
                alert.show();
                return;
            }
            SelectTableContainer selectTableContainer = new SelectTableContainer();
            selectTableContainer.setDatabase(databaseTreeItem.getValue().toString());
            selectTableContainer.setTable(selectedTable);
            selectTableContainer.setProjectionFields(projectionFields);
            selectTableContainer.setWhereClauses(this.getWhereClauses(tableFields));
            selectTableContainer.setGroupByClause(this.getGroupByClause(databaseTreeItem.getValue().toString(),  aggregateProjectionFields));
            selectTableContainer.setHavingClauses(this.getHavingClauses());
            this.addJoinTables(selectTableContainer);
            List<TableRowResult> result = this.server.select(selectTableContainer);
            this.createTableQueryResult(selectTableContainer, result);
        } catch (RemoteException | ManagerException e) {
            e.printStackTrace();
        }
    }

    private List<HavingClause> getHavingClauses() {
        List<HavingClause> havingClauses = new ArrayList<>();
        for(Node node : this.vbHaving.getChildren()){
            if(node instanceof HBox) {
                HavingClause havingClause = new HavingClause();
                int valuesSet = 0;
                for (Node child : ((HBox) node).getChildren()) {
                    if(child instanceof TextField && ((TextField) child).getText() != null){
                        havingClause.setValue(((TextField) child).getText());
                        valuesSet++;
                    }
                    if(child instanceof ComboBox && ((ComboBox) child).getValue() != null){
                        havingClause.setOperator(((ComboBox) child).getValue().toString());
                        valuesSet++;
                    }
                    if(child instanceof Label){
                        havingClause.setAggregateOperator(((Label) child).getText());
                        valuesSet++;
                    }
                }
                if(valuesSet == 3){
                    havingClauses.add(havingClause);
                }
            }
        }
        return havingClauses;
    }

    private GroupByClause getGroupByClause(String database, List<String> aggregateProjectionFields) {
        GroupByClause groupByClause = new GroupByClause();
        if(this.cbGroupBy.getValue() != null){
            try {
                String table = this.cbGroupBy.getValue().toString().split("\\.")[0];
                String fieldName = this.cbGroupBy.getValue().toString().split("\\.")[1];
                List<TableField> tableFields = this.server.getTableFields(database, table);
                for(TableField tableField : tableFields){
                    if(tableField.getName().compareTo(fieldName) == 0){
                        groupByClause.setTable(table);
                        groupByClause.setTableField(tableField);
                    }
                }
                for(String projectionField : aggregateProjectionFields){
                    String aggrOp = projectionField.split("\\(")[0];
                    groupByClause.addAggregateOperator(aggrOp);
                }
                return groupByClause;
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (ManagerException e) {
                e.printStackTrace();
            }
        }
        return groupByClause;
    }

    private void addJoinTables(SelectTableContainer selectTableContainer) {
        for(Node node : this.vbJoin.getChildren()){
            if(node instanceof HBox) {
                JoinClause joinClause = new JoinClause();
                for (Node child : ((HBox) node).getChildren()) {
                    if (child instanceof ComboBox && !child.getId().contains("#")) {
                        joinClause.setTable(((ComboBox) child).getSelectionModel().getSelectedItem().toString());
                    }
                    if(child instanceof ComboBox && child.getId().contains("#")){
                        joinClause.setJoinType(JoinType.valueOf(((ComboBox) child).getSelectionModel().getSelectedItem().toString()));
                    }
                }
                selectTableContainer.addJoinClause(joinClause);
            }
        }
    }

    private void createTableQueryResult(SelectTableContainer selectTableContainer, List<TableRowResult> result) {
        this.vBoxSelectResult.getChildren().clear();
        HBox hBox = new HBox();
        for(TableField projectionField : selectTableContainer.getProjectionFields()){
            hBox.setSpacing(100);
            Label label = new Label(projectionField.getName());
            hBox.getChildren().add(label);
        }
        this.vBoxSelectResult.getChildren().add(hBox);
        for(TableRowResult tableRow : result){
            hBox = new HBox();
            List<TableData> tableDataList;
            tableDataList = tableRow.getTableData(selectTableContainer.getTable());
            String processedTable = "";
            if(tableDataList == null || tableDataList.size() == 0){
                tableDataList = tableRow.getTableData(selectTableContainer.getGroupByClause().getTable());
                if(tableDataList != null && tableDataList.size() != 0){
                    processedTable = selectTableContainer.getGroupByClause().getTable();
                }
            }
            hBox.setSpacing(150);
            for(TableData tableData : tableDataList){
                Label label = new Label(tableData.getFieldValue());
                hBox.getChildren().add(label);
            }
            for(JoinClause joinClause : selectTableContainer.getJoinClauses()){
                String table = joinClause.getTable();
                if(table.compareTo(processedTable)==0){
                    break;
                }
                tableDataList = tableRow.getTableData(table);
                if(tableDataList != null){
                    for(TableData tableData : tableRow.getTableData(table)){
                        Label label = new Label(tableData.getFieldValue());
                        hBox.getChildren().add(label);
                    }
                }
            }
            this.vBoxSelectResult.getChildren().add(hBox);
        }
    }

    private List<WhereClause> getWhereClauses(List<TableField> tableFields) {
        List<WhereClause> whereClauses = new ArrayList<>();
        List<Node> children = this.vBoxWhere.getChildren();
        for(Node child : children){
            if(child instanceof HBox){
                whereClauses.add(this.getWhereClauseFromHBox((HBox) child, tableFields));
            }
        }
        return whereClauses;
    }

    private WhereClause getWhereClauseFromHBox(HBox hBox, List<TableField> tableFields) {
        WhereClause whereClause = new WhereClause();
        List<Node> children = hBox.getChildren();
        for(Node child : children){
            if(child instanceof TextField){
                whereClause.setValue(((TextField) child).getText());
            }
            if(child instanceof ComboBox){
                if(child.getId().contains("field")){
                    whereClause.setTableField(this.getTableFieldFromName(((ComboBox) child).getValue().toString(), tableFields));
                }else{
                    whereClause.setOperator(((ComboBox) child).getValue().toString());
                }
            }
        }
        return whereClause;
    }

    private TableField getTableFieldFromName(String value, List<TableField> tableFields) {
        for(TableField tableField : tableFields){
            if(tableField.getName().compareTo(value)==0){
                return tableField;
            }
        }
        return null;
    }

    public void handleWhere(){
        this.vBoxWhere.setVisible(true);
        this.vBoxWhere.getChildren().clear();
        String selectedTable = this.cBoxFrom.getSelectionModel().getSelectedItem().toString();
        DatabaseTreeItem databaseTreeItem = (DatabaseTreeItem)this.tree.getSelectionModel().getSelectedItem();
        try {
            List<TableField> tableFields = this.server.getTableFields(databaseTreeItem.getValue().toString(), selectedTable);
            this.addTableFieldsFromJoinTables(tableFields);
            if(tableFields.size()>0){
                HBox hbWhere = this.getHBoxWhere(tableFields);
                hbWhere.setSpacing(10);
                this.vBoxWhere.getChildren().addAll(hbWhere);
            }
        } catch (RemoteException | ManagerException e) {
            e.printStackTrace();
        }
    }

    private void addTableFieldsFromJoinTables(List<TableField> tableFields) {
        DatabaseTreeItem databaseTreeItem = (DatabaseTreeItem)this.tree.getSelectionModel().getSelectedItem();
        for(Node node : this.vbJoin.getChildren()){
            if(node instanceof HBox){
                for(Node child : ((HBox) node).getChildren()){
                    if(child instanceof ComboBox && !child.getId().contains("#")){
                        String table = ((ComboBox) child).getSelectionModel().getSelectedItem().toString();
                        try {
                            List<TableField> joinTableFields = this.server.getTableFields(databaseTreeItem.getValue().toString(), table);
                            tableFields.addAll(joinTableFields);
                        } catch (RemoteException | ManagerException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    public void handleWhereAnd(){
        String selectedTable = this.cBoxFrom.getSelectionModel().getSelectedItem().toString();
        DatabaseTreeItem databaseTreeItem = (DatabaseTreeItem)this.tree.getSelectionModel().getSelectedItem();
        List<TableField> tableFields = null;
        try {
            tableFields = this.server.getTableFields(databaseTreeItem.getValue().toString(), selectedTable);
            this.addTableFieldsFromJoinTables(tableFields);
            if(tableFields.size()>0){
                HBox hbWhere = this.getHBoxWhere(tableFields);
                hbWhere.setSpacing(10);
                this.vBoxWhere.getChildren().add(hbWhere);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (ManagerException e) {
            e.printStackTrace();
        }
    }

    private HBox getHBoxWhere(List<TableField> tableFields){
        ComboBox cbFields = new ComboBox();
        cbFields.setId("field" + Math.random());
        for(TableField tf : tableFields){
            cbFields.getItems().add(tf.getName());
        }
        ComboBox cbOperators = this.getComboBoxOperators();
        TextField textField = new TextField();
        textField.setPromptText("Value");

        HBox hBox = new HBox();
        hBox.setSpacing(10);
        hBox.getChildren().addAll(cbFields, cbOperators, textField);
        return hBox;
    }

    private ComboBox getComboBoxOperators(){
        ComboBox cbOperators = new ComboBox();
        cbOperators.setId("operators" + Math.random());
        cbOperators.getItems().add("=");
        cbOperators.getItems().add("!=");
        cbOperators.getItems().add(">");
        cbOperators.getItems().add(">=");
        cbOperators.getItems().add("<");
        cbOperators.getItems().add("<=");
        return cbOperators;
    }

    public void handleGoDate(){
        String dateQuery = this.tfDateQuery.getText();
        if(dateQuery == null || dateQuery.compareTo("")==0){
            return;
        }
        if(Utils.isDateQuerySyntax(dateQuery)){
            String dateResult = null;
            try {
                dateResult = this.server.selectDateQuery(dateQuery);
                this.createDateQueryResult(dateResult);
            } catch (ManagerException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, e.getMessage());
                alert.show();
            }
        }else{
            Alert alert = new Alert(Alert.AlertType.ERROR, "Syntax error");
            alert.show();
        }
    }

    private void createDateQueryResult(String dateResult) {
        this.tvQueryResult.getItems().clear();
        this.tvQueryResult.getColumns().clear();
        TableColumn tcDate = new TableColumn("Result");
        tcDate.setCellValueFactory(new PropertyValueFactory<DateResult,String>("result"));
        ObservableList<DateResult> data = FXCollections.observableArrayList(new DateResult(dateResult));
        this.tvQueryResult.setItems(data);
        this.tvQueryResult.getColumns().add(tcDate);
    }

    public void handleExternalJoin(){
        DatabaseTreeItem databaseTreeItem = (DatabaseTreeItem)this.tree.getSelectionModel().getSelectedItem();
        try {
            this.server.externalSortJoin(databaseTreeItem.getValue().toString());
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Task done");
            alert.show();
        } catch (ManagerException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, e.getMessage());
            alert.show();
        }
    }

    public void handleGroupBy(){
        this.cbGroupBy.getItems().clear();
        DatabaseTreeItem databaseTreeItem = (DatabaseTreeItem)this.tree.getSelectionModel().getSelectedItem();
        String selectedTable = this.cBoxFrom.getSelectionModel().getSelectedItem().toString();
        List<String> joinTables = this.getJoinTables(selectedTable);
        try {
            List<TableField> tableFields = this.getTableFields(databaseTreeItem.getValue().toString(), joinTables);
            for(TableField tableField : tableFields){
                cbGroupBy.getItems().add(tableField.getTable() + "." + tableField.getName());
            }
        } catch (RemoteException | ManagerException e) {
            e.printStackTrace();
        }
    }

    private List<TableField> getTableFields(String database, List<String> tables) throws ManagerException, RemoteException {
        List<TableField> tableFields = new ArrayList<>();
        for(String table : tables){
            List<TableField> tableFieldList = this.server.getTableFields(database, table);
            tableFields.addAll(tableFieldList);
        }
        return tableFields;
    }

    private List<String> getJoinTables(String selectedTable){
        List<String> joinTables = new ArrayList<>();
        joinTables.add(selectedTable);
        for(Node node : this.vbJoin.getChildren()){
            if(node instanceof HBox) {
                for (Node child : ((HBox) node).getChildren()) {
                    if (child instanceof ComboBox && !child.getId().contains("#")) {
                        joinTables.add(((ComboBox) child).getSelectionModel().getSelectedItem().toString());
                    }
                }
            }
        }
        return joinTables;
    }

    public void handleHaving(){
        this.vbHaving.getChildren().clear();
        if(this.cbGroupBy.getValue() != null){
            String field = cbGroupBy.getValue().toString();
            for(AggregateOperators aggregateOperators : AggregateOperators.values()){
                Label label = new Label(aggregateOperators + "(" + field + ")");
                ComboBox cbOperators = this.getComboBoxOperators();
                TextField textField = new TextField();
                textField.setPromptText("Value");
                HBox hBox = new HBox();
                hBox.setSpacing(10);
                hBox.getChildren().addAll(label, cbOperators, textField);
                this.vbHaving.getChildren().add(hBox);
            }
        }
    }

    public void handleLogout(){
        Parent root;
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("LoginView.fxml"));
            root = loader.load();
            LoginRegisterController loginRegisterController = loader.getController();
            loginRegisterController.setServer(server);
            Stage stage = new Stage();
            stage.setTitle("Login");
            stage.setScene(new Scene(root, 600, 400));
            stage.show();
            loginRegisterController.setStage(stage);
            this.stage.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }
}
