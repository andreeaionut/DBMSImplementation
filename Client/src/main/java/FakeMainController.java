import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Pair;

import java.io.IOException;
import java.rmi.RemoteException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FakeMainController implements IController {

    private Stage stage;
    private IServer actualServer;
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
    private TreeView<String> tree;

    private Server server;

    public FakeMainController(){
        this.server = new Server();
    }

    protected void initView() {
        this.paneQuery.setVisible(false);
        this.paneTableFields.setVisible(false);
        this.paneForeignKey.setVisible(false);
        this.paneIndexFields.setVisible(false);
        this.paneTableData.setVisible(false);
        TreeItem treeRoot = new TreeItem();
        treeRoot.setExpanded(true);
        DatabaseRootTreeItem dbs = new DatabaseRootTreeItem("Databases", this);
            List<String> databases = this.server.getDatabases();
            if(databases == null || databases.size() == 0){
                return;
            }
            for (String db : databases) {
                DatabaseTreeItem database = new DatabaseTreeItem (db, this);
                database.setServer(this.server);
                List<String> tables = this.server.getTables(db);
                for(String table : tables){
                    TableTreeItem tableTreeItem = new TableTreeItem(table);
                    tableTreeItem.setParentDatabase(database);
                    database.getChildren().add(tableTreeItem);
                }
                dbs.getChildren().add(database);
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
        this.paneQuery.setVisible(false);
        this.vBoxInsertData.setVisible(false);
        this.createTableFields();
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
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Table created!");
                alert.show();
                this.newTableAdded(name);
                this.tableViewFields.getItems().clear();
                this.paneTableFields.setVisible(false);

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
            switch(this.btnInsertDeleteData.getText()){
                case "Insert":{
                    if(insertionData.size() == data) {
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
    }

    public void handleInsertData() {
        this.paneForeignKey.setVisible(false);
        this.paneQuery.setVisible(false);
        this.paneRBtnAndOr.setVisible(false);
        this.paneUpdateData.setVisible(false);
        this.vBoxInsertData.getChildren().clear();
        this.paneTableData.setVisible(true);
        this.btnInsertDeleteData.setText("Insert");
        this.createVBoxInsertDeleteData(this.vBoxInsertData, true);
        this.vBoxInsertData.setVisible(true);

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
            List<TableField> tableFields = this.server.getTableFields(tableTreeItem.getParentDatabase().getValue().toString(), tableTreeItem.getValue().toString());
            for(TableField tableField : tableFields){
                this.createVboxByTableField(tableField, vBox, withPrimaryKey);
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

    @Override
    public void handleNewForeignKey() {

    }

    @Override
    public void handleNewIndex() {

    }

    public void handleNewQuery() {
        this.paneUpdateData.setVisible(false);
        this.paneTableData.setVisible(false);
        this.vBoxInsertData.setVisible(false);
        this.paneQuery.setVisible(true);
        this.vBoxWhere.getChildren().clear();
        this.cBoxFrom.getItems().clear();
        this.vBoxSelectResult.getChildren().clear();
        this.vBoxSelect.getChildren().clear();
        DatabaseTreeItem databaseTreeItem = (DatabaseTreeItem)this.tree.getSelectionModel().getSelectedItem();
            List<String> tables = this.server.getTables(databaseTreeItem.getValue().toString());
            this.cBoxFrom.getItems().addAll(tables);
            cBoxFrom.valueProperty().addListener(new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue ov, String oldValue, String newValue) {
                    FakeMainController.this.createVboxSelect(databaseTreeItem.getValue().toString(), newValue);
                    FakeMainController.this.vBoxWhere.getChildren().clear();
                }
            });

    }

    private void createVboxSelect(String database, String table) {
        this.vBoxSelect.getChildren().clear();
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
            List<TableField> tableFields = this.server.getTableFields(databaseTreeItem.getValue().toString(), selectedTable);
            if(tableFields.size()>0){
                HBox hbWhere = this.getHBoxWhere(tableFields);
                hbWhere.setSpacing(10);
                this.vBoxWhere.getChildren().addAll(hbWhere);
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

    private List<TableField> getTableFields(String database, List<String> tables) throws ManagerException, RemoteException {
        List<TableField> tableFields = new ArrayList<>();
        for(String table : tables){
            List<TableField> tableFieldList = this.server.getTableFields(database, table);
            tableFields.addAll(tableFieldList);
        }
        return tableFields;
    }

    public void handleLogout(){
        Parent root;
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("LoginView.fxml"));
            root = loader.load();
            LoginRegisterController loginRegisterController = loader.getController();
            loginRegisterController.setServer(actualServer);
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

    public void handleChangePassword(){
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("TestName");
        ButtonType loginButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);
        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setPadding(new Insets(20, 150, 10, 10));

        PasswordField oldPassField = new PasswordField();
        oldPassField.setPromptText("old password");
        PasswordField newPassField = new PasswordField();
        newPassField.setPromptText("new password");

        gridPane.add(oldPassField, 0, 0);
        gridPane.add(newPassField, 0, 1);
        dialog.getDialogPane().setContent(gridPane);
        Platform.runLater(() -> oldPassField.requestFocus());

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return new Pair<>(oldPassField.getText(), newPassField.getText());
            }
            return null;
        });
        Optional<Pair<String, String>> result = dialog.showAndWait();
        result.ifPresent(pair -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Password changed successfully!");
            alert.show();
        });
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public IServer getActualServer() {
        return actualServer;
    }

    public void setActualServer(IServer actualServer) {
        this.actualServer = actualServer;
    }

    public void handleWhereAnd(ActionEvent actionEvent) {
    }

    public void handleSelect(ActionEvent actionEvent) {
    }

    public void handleGoDate(ActionEvent actionEvent) {
    }

    public void handleAddNewJoin(ActionEvent actionEvent) {
    }

    public void handleExternalJoin(ActionEvent actionEvent) {
    }

    public void handleGroupBy(ActionEvent actionEvent) {
    }

    public void handleHaving(ActionEvent actionEvent) {
    }

    public void handleSaveForeignKey(ActionEvent actionEvent) {
    }

    public void handleSaveIndex(ActionEvent actionEvent) {
    }
}
