import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.rmi.RemoteException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ServerImplementation implements IServer {

    private String file = "D:\\2019-2020\\DBMS\\dbms.xml";

    private RedisServer redisServer;
    private LoginRegisterServer loginRegisterServer;

    public ServerImplementation(){
        this.redisServer = new RedisServer();
        this.redisServer.setCatalogServer(this);
        Properties serverProps=new Properties();
        this.loginRegisterServer = new LoginRegisterServer(serverProps);
        System.out.println(loginRegisterServer.size() + "SIZEEEEEEEEEEEEEEEEEEEEEEEE");
    }

    public void createDatabase(String name) throws ManagerException{
        if(existsDatabase(name)){
            throw new ManagerException("Database name already exists");
        }
        try {
            File inputFile = new File(file);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = null;
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            Element root = doc.getDocumentElement();

            Element newDatabase = doc.createElement("database");
            newDatabase.setAttribute("dataBaseName", name);
            newDatabase.setAttribute("dataBaseIndex", String.valueOf(this.getNextDatabaseIndex()));

            Element newRootTable = doc.createElement("tables");
            newDatabase.appendChild(newRootTable);

            root.appendChild(newDatabase);
            this.docToString(doc);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteDatabase(String name) throws RemoteException, ManagerException {
        try {
            File inputFile = new File(file);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = null;
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();
            Element root = doc.getDocumentElement();
            NodeList targets = doc.getElementsByTagName("database");
            for (int temp = 0; temp < targets.getLength(); temp++) {
                Node toRemove = targets.item(temp);
                String node = targets.item(temp).getAttributes().getNamedItem("dataBaseName").getNodeValue();
                if (node.equals(name)) {
                    root.removeChild(toRemove);
                    doc.normalize();
                    this.docToString(doc);
                    prettyPrint(doc);
                    Element db = (Element) targets.item(temp);
                    this.redisServer.deleteDatabase(Integer.valueOf(db.getAttribute("dataBaseIndex")));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<String> getTables(String database) throws RemoteException, ManagerException {
        List<String> tables = new ArrayList<>();
        try {
            File inputFile = new File(file);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();
            Element root = doc.getDocumentElement();
            NodeList nList = doc.getElementsByTagName("database");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node node = nList.item(temp);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) node;
                    if(eElement.getAttribute("dataBaseName").compareTo(database) == 0){
                        NodeList tablesList = eElement.getElementsByTagName("table");
                        for(int counter=0;counter<tablesList.getLength();counter++){
                            Node tableNode = tablesList.item(counter);
                            Element tableElement = (Element) tableNode;
                            tables.add(tableElement.getAttribute("tableName"));
                        }
                    }
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        return tables;
    }

    private static void prettyPrint(Document xml) throws Exception {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        StreamResult result = new StreamResult(new StringWriter());
        DOMSource source = new DOMSource(xml);
        transformer.transform(source, result);
    }

    public void addAttributes(String database, String table, List<TableField> tableFields) throws ManagerException {
        for(TableField tableField : tableFields ){
            this.addAttribute(database, table, tableField);
        }
    }

    private void addAttribute(String database, String table, TableField tableField) throws ManagerException {
        try {
            File inputFile = new File(file);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = null;
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();
            Element root = doc.getDocumentElement();
            NodeList nList = doc.getElementsByTagName("database");

            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node node = nList.item(temp);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) node;
                    if(eElement.getAttribute("dataBaseName").compareTo(database) == 0){
                        NodeList tablesList = eElement.getElementsByTagName("table");
                        for(int counter=0;counter<tablesList.getLength();counter++){
                            Node tableNode = tablesList.item(counter);
                            Element tableElement = (Element) tableNode;
                            if(tableElement.getAttribute("tableName").compareTo(table)==0){
                                Node structure = tableElement.getElementsByTagName("structure").item(0);
                                NodeList attributes = structure.getChildNodes();
                                for(int pkCounter = 0; pkCounter < attributes.getLength(); pkCounter++){
                                    Node currentChildNode = attributes.item(pkCounter) ;
                                    if(currentChildNode.getNodeType() == Node.ELEMENT_NODE && currentChildNode.hasAttributes() && currentChildNode.getAttributes().getNamedItem("attributeName").toString().compareTo(tableField.getName()) == 0){
                                        throw new ManagerException("Field name already exists!");
                                    }
                                }
                                Element attribute = doc.createElement("attribute");
                                attribute.setAttribute("attributeName", tableField.getName());
                                attribute.setAttribute("type",tableField.getFieldType());
                                if(tableField.getLength() != null && tableField.getFieldType().equals("char")){
                                    attribute.setAttribute("length", tableField.getLength().toString());
                                }
                                if(tableField.getIsUnique()){
                                    attribute.setAttribute("isUnique", "1");
                                }else{
                                    attribute.setAttribute("isUnique", "0");
                                }
                                attribute.setAttribute("isnull", "0");
                                structure.appendChild(attribute);
                                this.docToString(doc);
                                return;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            if(e instanceof ManagerException){
                throw new ManagerException(e.getMessage());
            }
            e.printStackTrace();
        }
    }

    @Override
    public void createTable(CreateTableContainer createTableContainer) throws RemoteException, ManagerException {
        if(existsTable(createTableContainer.getDatabase(), createTableContainer.getTable())){
            throw new ManagerException("Table name already exists");
        }
        try {
            File inputFile = new File(file);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            Element root = doc.getDocumentElement();
            NodeList nList = doc.getElementsByTagName("database");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node node = nList.item(temp);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) node;
                    if(eElement.getAttribute("dataBaseName").compareTo(createTableContainer.getDatabase()) == 0){
                        Element tablesNode = (Element) eElement.getElementsByTagName("tables").item(0);

                        Element newTable = doc.createElement("table");
                        newTable.setAttribute("tableName", createTableContainer.getTable());
                        newTable.setAttribute("fileName", createTableContainer.getTable()+".kv");

                        Node structureNode = doc.createElement("structure");

                        for(TableField tableField : createTableContainer.getTableFields()){
                            Element attribute = doc.createElement("attribute");
                            attribute.setAttribute("attributeName", tableField.getName());
                            attribute.setAttribute("type",tableField.getFieldType());
                            if(tableField.getLength() != null && tableField.getFieldType().equals("char")){
                                attribute.setAttribute("length", tableField.getLength().toString());
                            }
                            if(tableField.getIsUnique()){
                                attribute.setAttribute("isUnique", "1");
                                this.redisServer.createIndex(this.getDatabaseIndex(createTableContainer.getDatabase()), createTableContainer.getTable(), tableField, true);
                            }else{
                                attribute.setAttribute("isUnique", "0");
                            }
                            attribute.setAttribute("isnull", "0");
                            structureNode.appendChild(attribute);
                        }

                        newTable.appendChild(structureNode);

                        Node pkNode = doc.createElement("primaryKey");
                        for(TableField tableField : createTableContainer.getTableFields()){
                            if(tableField.getIsPrimaryKey()){
                                Element pkAttribute = doc.createElement("pkAttribute");
                                pkAttribute.appendChild(doc.createTextNode(tableField.getName()));
                                pkNode.appendChild(pkAttribute);
                            }
                        }
                        newTable.appendChild(pkNode);
                        tablesNode.appendChild(newTable);
                        this.docToString(doc);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteTable(String database, String table) throws RemoteException, ManagerException {
        try {
            File inputFile = new File(file);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = null;
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();
            Element root = doc.getDocumentElement();
            NodeList nList = doc.getElementsByTagName("database");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node node = nList.item(temp);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) node;
                    if(eElement.getAttribute("dataBaseName").compareTo(database) == 0){
                        Element tables = (Element) eElement.getElementsByTagName("tables").item(0);
                        NodeList tablesList = tables.getElementsByTagName("table");
                        for(int counter=0;counter<tablesList.getLength();counter++){
                            Node tableNode = tablesList.item(counter);
                            Element tableElement = (Element) tableNode;
                            if(tableElement.getAttribute("tableName").compareTo(table)==0){
                                this.redisServer.deleteTable(this.getDatabaseIndex(eElement.getAttribute("dataBaseName")), table);
                                tables.removeChild(tableNode);
                                doc.normalize();
                                this.docToString(doc);
                                prettyPrint(doc);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            if(e instanceof ManagerException){
                throw new ManagerException(e.getMessage());
            }
            e.printStackTrace();
        }
    }

    private String getAttributeType(String database, String table, String attribute) {
        try {
            File inputFile = new File(file);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = null;
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();
            Element root = doc.getDocumentElement();
            NodeList nList = doc.getElementsByTagName("database");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node node = nList.item(temp);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) node;
                    if (eElement.getAttribute("dataBaseName").compareTo(database) == 0) {
                        NodeList tablesList = eElement.getElementsByTagName("table");
                        for (int counter = 0; counter < tablesList.getLength(); counter++) {
                            Node tableNode = tablesList.item(counter);
                            Element tableElement = (Element) tableNode;
                            if (tableElement.getAttribute("tableName").compareTo(table) == 0) {
                                Node structure = tableElement.getElementsByTagName("structure").item(0);
                                NodeList attributes = structure.getChildNodes();
                                for (int pkCounter = 0; pkCounter < attributes.getLength(); pkCounter++) {
                                    Node currentChildNode = attributes.item(pkCounter);
                                    if (currentChildNode.getNodeType() == Node.ELEMENT_NODE && currentChildNode.hasAttributes()) {
                                        return currentChildNode.getAttributes().getNamedItem("type").getNodeValue();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    List<TableField> getForeignKeysFromTable(String database, String table){
        List<TableField> tableFields = new ArrayList<>();
        try {
            File inputFile = new File(file);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = null;
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();
            Element root = doc.getDocumentElement();
            NodeList nList = doc.getElementsByTagName("database");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node node = nList.item(temp);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) node;
                    if(eElement.getAttribute("dataBaseName").compareTo(database) == 0){
                        NodeList tablesList = eElement.getElementsByTagName("table");
                        for(int counter=0;counter<tablesList.getLength();counter++){
                            Node tableNode = tablesList.item(counter);
                            Element tableElement = (Element) tableNode;
                            if(tableElement.getAttribute("tableName").compareTo(table)==0){
                                Element fks = (Element) tableElement.getElementsByTagName("foreignKeys").item(0);
                                if(fks == null){
                                    return null;
                                }
                                NodeList foreignKeysList = fks.getElementsByTagName("foreignKey");
                                if(foreignKeysList == null || foreignKeysList.getLength() == 0){
                                    return tableFields;
                                }
                                for(int pkCounter = 0; pkCounter < foreignKeysList.getLength(); pkCounter++){
                                    TableField tableField = new TableField();
                                    Element crtFks = (Element)foreignKeysList.item(pkCounter);
                                    String value = crtFks.getFirstChild().getFirstChild().getNodeValue();
                                    tableField.setName(value);
                                    Element references = (Element)(crtFks.getElementsByTagName("references").item(0));
                                    tableField.setForeignKey(true);
                                    tableField.setForeignKeyTable(references.getFirstChild().getFirstChild().getNodeValue());
                                    tableFields.add(tableField);
                                }
                            }
                        }
                    }
                }
            }
            return tableFields;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    List<TableField> getPrimaryKeysFromTable(String database, String table){
        List<TableField> tableFields = new ArrayList<>();
        try {
            File inputFile = new File(file);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = null;
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();
            Element root = doc.getDocumentElement();
            NodeList nList = doc.getElementsByTagName("database");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node node = nList.item(temp);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) node;
                    if(eElement.getAttribute("dataBaseName").compareTo(database) == 0){
                        NodeList tablesList = eElement.getElementsByTagName("table");
                        for(int counter=0;counter<tablesList.getLength();counter++){
                            Node tableNode = tablesList.item(counter);
                            Element tableElement = (Element) tableNode;
                            if(tableElement.getAttribute("tableName").compareTo(table)==0){
                                NodeList pk = tableElement.getElementsByTagName("primaryKey");
                                if(pk == null || pk.getLength() == 0){
                                    return tableFields;
                                }
                                Element pkNode = (Element) pk.item(0);
                                NodeList pks = pkNode.getElementsByTagName("pkAttribute");
                                for(int pkCounter = 0; pkCounter < pks.getLength(); pkCounter++){
                                    TableField tableField = new TableField();
                                    tableField.setName(pks.item(pkCounter).getFirstChild().getNodeValue());
                                    tableField.setFieldType(this.getAttributeType(database, table, pks.item(pkCounter).getFirstChild().getNodeValue()));
                                    tableFields.add(tableField);
                                }
                            }
                        }
                    }
                }
            }
            return tableFields;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void addForeignKey(String database, String table, String pkTable) throws RemoteException, ManagerException {
        try {
            File inputFile = new File(file);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();
            Element root = doc.getDocumentElement();
            NodeList nList = doc.getElementsByTagName("database");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node node = nList.item(temp);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) node;
                    if(eElement.getAttribute("dataBaseName").compareTo(database) == 0){
                        NodeList tablesList = eElement.getElementsByTagName("table");
                        for(int counter=0;counter<tablesList.getLength();counter++){
                            Node tableNode = tablesList.item(counter);
                            Element tableElement = (Element) tableNode;
                            if(tableElement.getAttribute("tableName").compareTo(table)==0){
                                NodeList nodeList = tableElement.getElementsByTagName("foreignKeys");
                                Node newFk = this.getForeignKeyNode(doc, database, pkTable);
                                if(nodeList==null || nodeList.getLength() == 0){
                                    Node fkNode = doc.createElement("foreignKeys");
                                    fkNode.appendChild(newFk);
                                    tableElement.appendChild(fkNode);
                                }else{
                                    Element fkElement = (Element) nodeList.item(0);
                                    NodeList fks = fkElement.getElementsByTagName("foreignKey");
                                    for(int lCounter = 0; lCounter < fks.getLength(); lCounter++){
                                        Element currentFk = (Element) fks.item(lCounter);
                                        Element references = (Element) currentFk.getElementsByTagName("references").item(0);
                                        NodeList refTables = references.getElementsByTagName("refTable");
                                        for(int refCounter = 0; refCounter < refTables.getLength(); refCounter++){
                                            Element refTable = (Element) refTables.item(refCounter);
                                            if(refTable.getFirstChild().getNodeValue().equals(pkTable)){
                                                throw new ManagerException("Foreign key already exists");
                                            }
                                        }
                                    }
                                    nodeList.item(0).appendChild(newFk);
                                }
                                doc.normalize();
                                this.docToString(doc);
                                prettyPrint(doc);
                                List<TableField> pks = this.getPrimaryKeysFromTable(database, pkTable);
                                this.addAttributes(database, table, pks);
                                this.redisServer.addForeignKeyIndex(this.getDatabaseIndex(database), table, pks);
                            }
                        }
                    }
                }
            }
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (Exception e) {
            if(e instanceof ManagerException){
                if(e.getMessage() != null){
                    throw new ManagerException(e.getMessage());
                }
                throw new ManagerException("Foreign key already exists");
            }
            e.printStackTrace();
        }
    }

    public List<TableField> getTableFields(int databaseIndex, String table) throws RemoteException, ManagerException {
        String database = this.getDatabaseByIndex(databaseIndex);
        return this.getTableFields(database, table);
    }

    @Override
    public List<TableField> getTableFields(String database, String table) throws RemoteException, ManagerException {
        List<TableField> tableFields = new ArrayList<>();
        try {
            File inputFile = new File(file);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = null;
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();
            Element root = doc.getDocumentElement();
            NodeList nList = doc.getElementsByTagName("database");

            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node node = nList.item(temp);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) node;
                    if(eElement.getAttribute("dataBaseName").compareTo(database) == 0){
                        NodeList tablesList = eElement.getElementsByTagName("table");
                        for(int counter=0;counter<tablesList.getLength();counter++){
                            Node tableNode = tablesList.item(counter);
                            Element tableElement = (Element) tableNode;
                            if(tableElement.getAttribute("tableName").compareTo(table)==0){
                                Node structure = tableElement.getElementsByTagName("structure").item(0);
                                NodeList attributes = structure.getChildNodes();
                                if(attributes == null || attributes.getLength() == 0){
                                    return tableFields;
                                }
                                for(int pkCounter = 0; pkCounter < attributes.getLength(); pkCounter++){
                                    TableField tableField = new TableField();
                                    tableField.setTable(table);
                                    Node currentChildNode = attributes.item(pkCounter) ;
                                    if(node.getNodeType() == Node.ELEMENT_NODE && currentChildNode.hasAttributes() && attributes.item(pkCounter).getAttributes().getNamedItem("attributeName") != null){
                                        tableField.setName(attributes.item(pkCounter).getAttributes().getNamedItem("attributeName").getNodeValue());
                                        tableField.setFieldType(attributes.item(pkCounter).getAttributes().getNamedItem("type").getNodeValue());
                                        Element item = (Element) attributes.item(pkCounter);
                                        if(item.getAttribute("isUnique").compareTo("1") == 0){
                                            tableField.setUnique(true);
                                        }else{
                                            tableField.setUnique(false);
                                        }
                                        if(this.isPrimaryKey(database, table, item.getAttribute("attributeName"))){
                                            tableField.setPrimaryKey(true);
                                        }else{
                                            tableField.setPrimaryKey(false);
                                        }
                                        tableField.setFieldIndex(pkCounter);
                                        String foreignKeyTable = this.getForeignKeyTable(database, table, item.getAttribute("attributeName"));
                                        if(foreignKeyTable != null){
                                            tableField.setForeignKey(true);
                                            tableField.setForeignKeyTable(foreignKeyTable);
                                        }
                                        tableFields.add(tableField);
                                    }
                                }
                                return tableFields;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tableFields;
    }

    private String getForeignKeyTable(String database, String table, String attributeName) {
        List<TableField> foreignKeys = this.getForeignKeysFromTable(database, table);
        if(foreignKeys == null || foreignKeys.size() == 0){
            return null;
        }
        for(TableField tableField : foreignKeys){
            if(tableField.getName().compareTo(attributeName)==0){
                return tableField.getForeignKeyTable();
            }
        }
        return null;
    }

    private boolean isPrimaryKey(String database, String table, String attributeName){
        List<TableField> pks = this.getPrimaryKeysFromTable(database, table);
        for(TableField tableField : pks){
            if(tableField.getName().compareTo(attributeName) == 0){
                return true;
            }
        }
        return false;
    }

    @Override
    public void insertData(String database, String table, List<TableData> data) throws RemoteException, ManagerException {
        List<TableField> tableFields = this.getTableFields(database, table);
        this.redisServer.insertData(this.getDatabaseIndex(database), table, tableFields, data);
    }

    @Override
    public void deleteData(String database, String table, List<TableData> data, LogicalOperator logicalOperator) throws RemoteException, ManagerException {
        this.redisServer.deleteData(this.getDatabaseIndex(database), table, this.getTableFields(database, table), data, logicalOperator);
    }

    @Override
    public void updateData(String database, String table, List<TableData> whereData, LogicalOperator logicalOperator, List<TableData> updatedData) throws RemoteException, ManagerException {
        this.redisServer.updateData(this.getDatabaseIndex(database), table, whereData, logicalOperator, updatedData);
    }

    private int getDatabaseIndex(String database) {
        try {
            File inputFile = new File(file);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = null;
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();
            Element root = doc.getDocumentElement();
            NodeList nList = doc.getElementsByTagName("database");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node node = nList.item(temp);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) node;
                    if(eElement.getAttribute("dataBaseName").compareTo(database) == 0){
                        return Integer.valueOf(eElement.getAttribute("dataBaseIndex"));
                    }
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public void createIndex(IndexContainer indexContainer) throws RemoteException, ManagerException {
        try {
            File inputFile = new File(file);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = null;
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            Element root = doc.getDocumentElement();

            NodeList nList = doc.getElementsByTagName("database");

            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node node = nList.item(temp);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) node;
                    if(eElement.getAttribute("dataBaseName").compareTo(indexContainer.getDatabase()) == 0){
                        NodeList tablesList = eElement.getElementsByTagName("table");
                        for(int counter=0;counter<tablesList.getLength();counter++){
                            Node tableNode = tablesList.item(counter);
                            Element tableElement = (Element) tableNode;
                            if(tableElement.getAttribute("tableName").compareTo(indexContainer.getTable())==0){
                                NodeList nodeList = tableElement.getElementsByTagName("indexFiles");
                                Node newIndexFile = this.getIndexFileNode(doc, indexContainer);
                                if(nodeList==null || nodeList.getLength() == 0){
                                    Node indexFilesNode = doc.createElement("indexFiles");
                                    indexFilesNode.appendChild(newIndexFile);
                                    tableElement.appendChild(indexFilesNode);
                                }else{
                                    Element indexFiles = (Element) nodeList.item(0);
                                    NodeList idxs = indexFiles.getElementsByTagName("indexFile");
                                    for(int lCounter = 0; lCounter < idxs.getLength(); lCounter++){
                                        Element currentIdx = (Element) idxs.item(lCounter);
                                        if(currentIdx.getAttribute("indexName").replace(".ind", "").compareTo(indexContainer.getIndexName()) == 0){
                                            throw new ManagerException("Index name already exists");
                                        }
                                    }
                                    nodeList.item(0).appendChild(newIndexFile);
                                }
                                doc.normalize();
                                this.docToString(doc);
                                prettyPrint(doc);

                                this.redisServer.createIndex(this.getDatabaseIndex(indexContainer.getDatabase()), indexContainer.getTable(), indexContainer.getTableField(), indexContainer.isUnique());
                            }
                        }
                    }
                }
            }
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (Exception e) {
            if(e instanceof ManagerException){
                throw new ManagerException("Index name already exists");
            }
            e.printStackTrace();
        }
    }

    @Override
    public List<TableRowResult> select(SelectTableContainer selectTableContainer) throws ManagerException, RemoteException {
        return this.redisServer.select(this.getDatabaseIndex(selectTableContainer.getDatabase()), selectTableContainer.getTable(), selectTableContainer.getWhereClauses(), selectTableContainer.getProjectionFields(), this.getTableFields(selectTableContainer.getDatabase(), selectTableContainer.getTable()), selectTableContainer.getJoinClauses(), selectTableContainer.getGroupByClause(), selectTableContainer.getHavingClauses());
    }

    @Override
    public String selectDateQuery(String dateQuery) throws ManagerException {
        String dateFunction = dateQuery.split("\\(")[0];
        DateFunction df = DateFunction.getDateFunctionByText(dateFunction);
        String date;
        String datePart;
        switch(df){
            case GETDATE:
                return DateUtils.GETDATE().toString();
            case DATEADD:
                datePart = dateQuery.split("\\(")[1].split("\\)")[0].split(",")[0];
                int nr = Integer.parseInt(dateQuery.split("\\(")[1].split("\\)")[0].split(",")[1]);
                date = dateQuery.split("\\(")[1].split("\\)")[0].split(",")[2];
                return DateUtils.DATEADD(DatePart.getByText(datePart), nr, LocalDate.parse(date)).toString();
            case DATEDIFF:
                datePart = dateQuery.split("\\(")[1].split("\\)")[0].split(",")[0];
                date = dateQuery.split("\\(")[1].split("\\)")[0].split(",")[1];
                String dateEnd = dateQuery.split("\\(")[1].split("\\)")[0].split(",")[2];
                return String.valueOf(DateUtils.DATEDIFF(DatePart.getByText(datePart), LocalDate.parse(date), LocalDate.parse(dateEnd)));
            case DATEFROMPARTS:
                int p1 = Integer.parseInt(dateQuery.split("\\(")[1].split("\\)")[0].split(",")[0]);
                int p2 = Integer.parseInt(dateQuery.split("\\(")[1].split("\\)")[0].split(",")[1]);
                int p3 = Integer.parseInt(dateQuery.split("\\(")[1].split("\\)")[0].split(",")[2]);
                return DateUtils.DATEFROMPARTS(p1, p2, p3).toString();
            case DATENAME:
                datePart = dateQuery.split("\\(")[1].split("\\)")[0].split(",")[0];
                date = dateQuery.split("\\(")[1].split("\\)")[0].split(",")[1];
                return DateUtils.DATENAME(DatePart.getByText(datePart), LocalDate.parse(date));
            case DATEPART:
                datePart = dateQuery.split("\\(")[1].split("\\)")[0].split(",")[0];
                date = dateQuery.split("\\(")[1].split("\\)")[0].split(",")[1];
                return String.valueOf(DateUtils.DATEPART(DatePart.getByText(datePart), LocalDate.parse(date)));
            case DAY:
                date = dateQuery.split("\\(")[1].split("\\)")[0].split(",")[0];
                return String.valueOf(DateUtils.DAY(LocalDate.parse(date)));
            case MONTH:
                date = dateQuery.split("\\(")[1].split("\\)")[0].split(",")[0];
                return String.valueOf(DateUtils.MONTH(LocalDate.parse(date)));
            case YEAR:
                date = dateQuery.split("\\(")[1].split("\\)")[0].split(",")[0];
                return String.valueOf(DateUtils.YEAR(LocalDate.parse(date)));
            case ISDATE:
                date = dateQuery.split("\\(")[1].split("\\)")[0].split(",")[0];
                return String.valueOf(DateUtils.ISDATE(date));
        }
        return LocalDate.now().toString();
    }

    @Override
    public List<String> getReferencedTables(String database, String table) throws ManagerException, RemoteException {
        List<String> tables = this.getTablesReferencedAsForeignKeyByName(database, table);
        List<TableField> tableFields = this.getForeignKeysFromTable(database, table);
        if(tableFields == null){
            return tables;
        }
        for(TableField tableField : tableFields){
            tables.add(tableField.getForeignKeyTable());
        }
        return tables;
    }

    @Override
    public void externalSortJoin(String database) throws ManagerException {
        int dbIndex = this.getDatabaseIndex(database);
        this.redisServer.externalSortJoin(dbIndex);
    }

    @Override
    public boolean register(String email, String username, String password, String accessCode) throws ManagerException {
        return this.loginRegisterServer.register(email, username, password, accessCode);
    }

    @Override
    public boolean changePassword(String email, String oldPassword, String newPassword) throws ManagerException {
        return false;
    }

    private Node getIndexFileNode(Document doc, IndexContainer indexContainer) {
        Node newIndexFile = doc.createElement("indexFile");
        ((Element) newIndexFile).setAttribute("indexName", indexContainer.getIndexName() + ".ind");
        ((Element) newIndexFile).setAttribute("keyLength", "64");
        ((Element) newIndexFile).setAttribute("getIsUnique", "0");
        ((Element) newIndexFile).setAttribute("indexType", "BTree");

        Node indexAttributes = doc.createElement("indexAttributes");
            Node idx = doc.createElement("iAttribute");
            idx.appendChild(doc.createTextNode(indexContainer.getTableField().getName()));
            indexAttributes.appendChild(idx);
        newIndexFile.appendChild(indexAttributes);
        return newIndexFile;
    }

    private Node getForeignKeyNode(Document doc, String database, String pkTable) throws ManagerException {
        Node newFk = doc.createElement("foreignKey");
        Node refNode = doc.createElement("references");
        List<TableField> pks = this.getPrimaryKeysFromTable(database, pkTable);

        if(pks == null || pks.size() == 0){
            throw new ManagerException("Primary key table has no primary keys!");
        }

        Node refTable = doc.createElement("refTable");
        refTable.appendChild(doc.createTextNode(pkTable));
        refNode.appendChild(refTable);

        for(TableField tableField : pks){
            Node fk = doc.createElement("fkAttribute");
            fk.appendChild(doc.createTextNode(tableField.getName()));
            newFk.appendChild(fk);

            Node rf = doc.createElement("refAttribute");
            rf.appendChild(doc.createTextNode(tableField.getName()));
            refNode.appendChild(rf);
        }
        newFk.appendChild(refNode);
        return newFk;
    }

    private boolean existsTable(String database, String table){
        try {
            File inputFile = new File(file);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();
            Element root = doc.getDocumentElement();
            NodeList nList = doc.getElementsByTagName("database");

            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node node = nList.item(temp);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) node;
                    if(eElement.getAttribute("dataBaseName").compareTo(database) == 0){
                        NodeList tablesList = eElement.getElementsByTagName("table");
                        for(int counter=0;counter<tablesList.getLength();counter++){
                            Node tableNode = tablesList.item(counter);
                            Element tableElement = (Element) tableNode;
                            if(tableElement.getAttribute("tableName").compareTo(table) == 0){
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean existsDatabase(String databaseName) throws ManagerException {
        List<String> dbs = this.getDatabases();
        for(String db : dbs){
            if(db.compareTo(databaseName)==0){
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean login(String username, String password) throws ManagerException, RemoteException {
        return this.loginRegisterServer.login(username, password);
    }

    public void changeObserver(IObserver client) throws ManagerException, RemoteException {

    }

    public List<String> getDatabases(){
        List<String> dbs = new ArrayList<String>();
        try {
            File inputFile = new File(file);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = null;
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();
            Element root = doc.getDocumentElement();
            System.out.println(root.getNodeName());
            NodeList nList = doc.getElementsByTagName("database");
            System.out.println("============================");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node node = nList.item(temp);
                System.out.println("");
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) node;
                    System.out.println("Employee id : " + eElement.getAttribute("dataBaseName"));
                    dbs.add(eElement.getAttribute("dataBaseName"));
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        return dbs;
    }

    private void docToString(Document doc) throws Exception{
        DOMSource source = new DOMSource(doc);
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        StreamResult result = new StreamResult(file);
        transformer.transform(source, result);
    }

    private int getNextDatabaseIndex(){
        int dbIndex = 0;
        try {
            File inputFile = new File(file);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = null;
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();
            Element root = doc.getDocumentElement();
            NodeList nList = doc.getElementsByTagName("database");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node node = nList.item(temp);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) node;
                    dbIndex = Integer.valueOf(eElement.getAttribute("dataBaseIndex"));
                }
            }
            return dbIndex + 1;
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private String getDatabaseByIndex(int index){
        try {
            File inputFile = new File(file);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = null;
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();
            Element root = doc.getDocumentElement();
            NodeList nList = doc.getElementsByTagName("database");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node node = nList.item(temp);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) node;
                    if(eElement.getAttribute("dataBaseIndex").compareTo(String.valueOf(index)) == 0){
                        return eElement.getAttribute("dataBaseName");
                    }
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected List<String> getTablesReferencedAsForeignKeyByName(String database, String table) throws ManagerException, RemoteException {
        List<String> tables = this.getTables(database);
        List<String> fkTables = new ArrayList<>();
        for(String crtTable : tables){
            List<TableField> tableFields = this.getForeignKeysFromTable(database, crtTable);
            if(tableFields!=null){
                for(TableField tableField : tableFields){
                    if(tableField.getForeignKeyTable().compareTo(table)==0){
                        fkTables.add(crtTable);
                    }
                }
            }
        }
        return fkTables;
    }

    protected List<String> getTablesReferencedAsForeignKey(int databaseIndex, String table) throws ManagerException, RemoteException {
        String database = this.getDatabaseByIndex(databaseIndex);
        List<String> tables = this.getTables(database);
        List<String> fkTables = new ArrayList<>();
        for(String crtTable : tables){
            List<TableField> tableFields = this.getForeignKeysFromTable(database, crtTable);
            if(tableFields!=null){
                for(TableField tableField : tableFields){
                    if(tableField.getForeignKeyTable().compareTo(table)==0){
                        fkTables.add(crtTable);
                    }
                }
            }
        }
        return fkTables;
    }

    public TableField getCommonTableField(int databaseIndex, String table, String joinTable) {
        String database = this.getDatabaseByIndex(databaseIndex);
        List<TableField> fks = this.getForeignKeysFromTable(database, table);
        if(fks != null){
            for(TableField tableField : fks){
                if(tableField.getForeignKeyTable().compareTo(joinTable)==0){
                    return tableField;
                }
            }
        }
        fks = this.getForeignKeysFromTable(database, joinTable);
        for(TableField tableField : fks){
            if(tableField.getForeignKeyTable().compareTo(table)==0){
                return tableField;
            }
        }
        return null;
    }
}
