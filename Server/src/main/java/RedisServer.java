import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

import java.io.File;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.rmi.RemoteException;
import java.util.*;

public class RedisServer {

    private Jedis jedis;
    private ServerImplementation catalogServer;

    private String personsInputFile = "D:\\2019-2020\\DBMS\\MiniDBMS\\temp_files\\persons.txt";
    private String citiesInputFile = "D:\\2019-2020\\DBMS\\MiniDBMS\\temp_files\\cities.txt";

    private String personsOutputFile = "D:\\2019-2020\\DBMS\\MiniDBMS\\temp_files\\p_output.txt";
    private String citiesOutputFile = "D:\\2019-2020\\DBMS\\MiniDBMS\\temp_files\\c_output.txt";

    private String joinOutputFile = "D:\\2019-2020\\DBMS\\MiniDBMS\\temp_files\\join_output.txt";

    public RedisServer(){
        this.jedis = new Jedis("127.0.0.1", 6379);
        System.out.println("Connection to server successfully");
        System.out.println("Server is running: "+jedis.ping());
    }

    private String getIndex(String table, TableField tableField, boolean unique){
        Set<String> indexesForTable;
        if(unique){
            indexesForTable = jedis.smembers("indexesUQ#"+table);
        }else{
            indexesForTable = jedis.smembers("indexesNUQ#"+table);
        }
        if(indexesForTable.size() > 0){
            for(String index : indexesForTable){
                String indexField = index.split("#")[1];
                if(indexField.compareTo(tableField.getName())==0){
                    return index.split("#")[0];
                }
            }
        }
        return null;
    }

    private String getIndex(String table, TableData tableData, boolean unique){
        Set<String> indexesForTable;
        if(unique){
            indexesForTable = jedis.smembers("indexesUQ#"+table);
        }else{
            indexesForTable = jedis.smembers("indexesNUQ#"+table);
        }
        if(indexesForTable.size() > 0){
            for(String index : indexesForTable){
                String indexField = index.split("#")[1];
                if(indexField.compareTo(tableData.getFieldName())==0){
                    return index.split("#")[0];
                }
            }
        }
        return null;
    }

    private void scanTableUniqueValue(String table, int fieldIndex, TableData tableData) throws ManagerException {
        Map<String, String> fields = jedis.hgetAll(table);
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            String[] values = entry.getValue().split("#");
            int valueIndex = 0;
            for(String value : values){
                if(valueIndex == fieldIndex - 1){
                    if(value.compareTo(tableData.getFieldValue()) == 0){
                        throw new ManagerException("Unique constraint being violated!");
                    }
                }
                valueIndex++;
            }
        }
    }

    public void insertData(int databaseIndex, String table, List<TableField> tableFields, List<TableData> data) throws ManagerException{
        jedis.select(databaseIndex);
        String pk = "";
        String fieldValue = "";
        for(TableData tableData : data){
            boolean wasPk = false;
            int fieldIndex = 0;
            for(TableField tableField : tableFields){
                if(tableField.getName().compareTo(tableData.getFieldName()) == 0){
                    if(tableField.getIsPrimaryKey()){
                        pk = tableData.getFieldValue();
                        if(jedis.hget(table, pk) != null){
                            throw new ManagerException("Primary key already exists!");
                        }
                        wasPk = true;
                    }
                    if(tableField.isForeignKey()){
                        if(jedis.hget(tableField.getForeignKeyTable(), tableData.getFieldValue())==null){
                            throw new ManagerException("Foreign key does not exist!");
                        }
                        jedis.hset(table+"#"+tableData.getFieldName(), tableData.getFieldValue()+"#"+pk, "");
                        System.out.println("Foreign key index after insert--------------");
                    }
                    String uniqueIndexName = this.getIndex(table, tableField, true);
                    if(uniqueIndexName!=null){
                        System.out.println("UNIQUE INDEX BEING USED");
                        if(jedis.hget(table+"#"+uniqueIndexName+"#"+tableField.getName(), tableData.getFieldValue())!=null){
                            throw new ManagerException("Unique constraint being violated!");
                        }
                        jedis.hset(table+"#"+uniqueIndexName+"#"+tableField.getName(), tableData.getFieldValue(), pk);
                        jedis.sadd("indexesUQ#"+table, uniqueIndexName+"#"+tableField.getName());
                    }else{
                        if(tableField.getIsUnique()){
                            this.scanTableUniqueValue(table, fieldIndex, tableData);
                        }else{
                            String nonUniqueIndexName = this.getIndex(table, tableField, false);
                            System.out.println("ADDING TO NON UNIQUE INDEX");
                            jedis.hset(table+"#"+nonUniqueIndexName+"#"+tableField.getName(), tableData.getFieldValue()+"#"+pk, "");
                            //jedis.sadd("indexesNUQ#"+table, nonUniqueIndexName+"#"+tableField.getName());
                        }
                    }
                }
                fieldIndex++;
            }
            if(!wasPk){
                fieldValue = fieldValue.concat(tableData.getFieldValue() + "#");
            }
        }
        System.out.println("INSERT DATA FIELD VALUE " + fieldValue);
        String value = fieldValue.substring(0, fieldValue.length()-1);
        jedis.hset(table, pk, value);

        System.out.println("------------TABLE DATA AFTER INSERT--------------");
        Map<String, String> test = jedis.hgetAll(table);
        for (Map.Entry<String, String> entry : test.entrySet()) {
            System.out.println(entry.getKey() + "/" + entry.getValue());
        }

        System.out.println("------------FK INDEXES AFTER INSERT--------------");
        Set<String> idx = jedis.smembers("indexesFK#"+table);
        for(String index : idx){
            String indexKey = table+"#"+index;
            Map<String, String> indexValues = jedis.hgetAll(indexKey);
            System.out.println("----- index: " + indexKey + " --------/*");
            indexValues = jedis.hgetAll(indexKey);
            for (Map.Entry<String, String> indexEntry : indexValues.entrySet()) {
                System.out.println(indexEntry.getKey() + " *** " + indexEntry.getValue());
            }
        }

        System.out.println("------------NQ INDEXES AFTER INSERT--------------");
        Set<String> idxNQ = jedis.smembers("indexesNUQ#"+table);
        for(String index : idxNQ){
            String indexKey = table+"#"+index;
            Map<String, String> indexValues = jedis.hgetAll(indexKey);
            System.out.println("----- index: " + indexKey + " --------/*");
            indexValues = jedis.hgetAll(indexKey);
            for (Map.Entry<String, String> indexEntry : indexValues.entrySet()) {
                System.out.println(indexEntry.getKey() + " *** " + indexEntry.getValue());
            }
        }
    }

    public void deleteDatabase(int databaseIndex){
        jedis.select(databaseIndex);
        jedis.flushDB();
    }

    public void deleteTable(int databaseIndex, String table) throws RemoteException, ManagerException {
        jedis.select(databaseIndex);
        if(this.catalogServer.getTablesReferencedAsForeignKey(databaseIndex, table).size()>0){
            throw new ManagerException("Table referenced as FK!");
        }
        Map<String, String> fields = jedis.hgetAll(table);
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            jedis.hdel(table, entry.getKey());
            this.deleteKeyFromIndex(table, entry.getKey());
        }
        System.out.println("------------TABLE DATA AFTER DELETE TABLE--------------");
        Map<String, String> test = jedis.hgetAll(table);
        for (Map.Entry<String, String> entry : test.entrySet()) {
            System.out.println(entry.getKey() + "/" + entry.getValue());
        }
    }

    private void deleteFromIndexes(String table, String key, Set<String> indexes, boolean areUnique){
        System.out.println("INDEXES AFTER DELETE");
        if(indexes.size() > 0){
            for(String index : indexes){
                String indexKey = table+"#"+index;
                Map<String, String> indexValues = jedis.hgetAll(indexKey);
                for (Map.Entry<String, String> indexEntry : indexValues.entrySet()) {
                    if(areUnique){
                        if(indexEntry.getValue().compareTo(key)==0){
                            jedis.hdel(indexKey, indexEntry.getKey());
                        }
                    }else{
                        if(indexEntry.getKey().split("#")[1].compareTo(key)==0){
                            jedis.hdel(indexKey, indexEntry.getKey());
                        }
                    }
                }
                System.out.println("----- index: " + indexKey + " --------/*");
                indexValues = jedis.hgetAll(indexKey);
                for (Map.Entry<String, String> indexEntry : indexValues.entrySet()) {
                    System.out.println(indexEntry.getKey() + " *** " + indexEntry.getValue());
                }
            }
        }
    }

    private void deleteKeyFromIndex(String table, String key){
        this.deleteFromIndexes(table, key, jedis.smembers("indexesUQ#"+table), true);
        this.deleteFromIndexes(table, key, jedis.smembers("indexesNUQ#"+table), false);
        this.deleteFromIndexes(table, key, jedis.smembers("indexesFK#"+table), false);
    }

    private boolean checkForeignKey(String childTable, List<TableData> data){
        for(TableData tableData : data){
            String matchValue = tableData.getFieldValue().concat("#*");
            String key = childTable+"#"+tableData.getFieldName();
            ScanParams scanParams = new ScanParams().count(100).match(matchValue);
            String crtCursor = redis.clients.jedis.ScanParams.SCAN_POINTER_START;
            boolean cycleIsFinished = false;
            while(!cycleIsFinished) {
                ScanResult<Map.Entry<String, String>> scanResult =
                        jedis.hscan(key, crtCursor, scanParams);
                List<Map.Entry<String, String>> result = scanResult.getResult();
                if(result.size() > 0){
                    return false;
                }
                crtCursor = scanResult.getCursor();
                if (crtCursor.equals("0")) {
                    cycleIsFinished = true;
                }
            }
        }
        return true;
    }

    private void deleteKey(int databaseIndex, String table, String key, List<TableData> data) throws ManagerException, RemoteException {
        List<String> childTables = this.catalogServer.getTablesReferencedAsForeignKey(databaseIndex, table);
        if(childTables.size()==0){
            jedis.hdel(table, key);
            this.deleteKeyFromIndex(table,key);
            return;
        }
        for(String childTable : childTables){
            if(checkForeignKey(childTable, data)){
                jedis.hdel(table, key);
                this.deleteKeyFromIndex(table,key);
            }else{
                throw new ManagerException("Foreign key referenced in another table");
            }
        }
    }

    public void deleteData(int databaseIndex, String table, List<TableField> tableFields, List<TableData> data, LogicalOperator logicalOperator) throws ManagerException, RemoteException {
        jedis.select(databaseIndex);
        Map<String, String> fields = jedis.hgetAll(table);
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            String compareString = entry.getKey() + "#" + entry.getValue();
            if(logicalOperator.equals(LogicalOperator.AND)){
                if(checkAND(compareString.split("#"), data)){
                    this.deleteKey(databaseIndex, table, entry.getKey(), data);
                }
            }else{
                if(checkOR(compareString.split("#"), data)){
                    this.deleteKey(databaseIndex, table, entry.getKey(), data);
                }
            }
        }
        fields = jedis.hgetAll(table);
        System.out.println("--------TABLE DATA AFTER DELETE----------");
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            System.out.println(entry.getKey() + "/" + entry.getValue());
        }
    }

    private boolean checkOR(String[] fieldsValues, List<TableData> tableData){
        int counter = 0;
        for(TableData tData : tableData){
            if(fieldsValues[tData.getFieldIndex()].compareTo(tData.getFieldValue())==0){
                counter++;
            }
        }
        return counter > 0;
    }

    private boolean checkAND(String[] fieldsValues, List<TableData> tableData) {
        for(TableData tData : tableData){
            if(fieldsValues[tData.getFieldIndex()].compareTo(tData.getFieldValue())!=0){
                return false;
            }
        }
        return true;
    }

    public void updateData(int databaseIndex, String table, List<TableData> whereData, LogicalOperator logicalOperator, List<TableData> updatedData) throws ManagerException {
        jedis.select(databaseIndex);
        Map<String, String> fields = jedis.hgetAll(table);
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            String compareString = entry.getKey() + "#" + entry.getValue();
            if(logicalOperator.equals(LogicalOperator.AND)){
                if(checkAND(compareString.split("#"), whereData)){
                    String oldValue = entry.getValue();
                    String updatedValue = this.createValueFromList(oldValue, updatedData);
                    if(this.canUpdate(table, updatedData)){
                        jedis.hset(table, entry.getKey(), updatedValue);
                        this.updateKeyFromIndex(table, entry.getKey(), updatedData);
                    }else{
                        throw new ManagerException("Unique constraint being violated!");
                    }
                }
            }else{
                if(checkOR(compareString.split("#"), whereData)){
                    if(this.canUpdate(table, updatedData)) {
                        jedis.hset(table, entry.getKey(), this.createValueFromList(entry.getValue(), updatedData));
                        this.updateKeyFromIndex(table, entry.getKey(), updatedData);
                    }else{
                        throw new ManagerException("Unique constraint being violated!");
                    }
                }
            }
        }
        fields = jedis.hgetAll(table);
        System.out.println("--------TABLE DATA AFTER UPDATE----------");
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            System.out.println(entry.getKey() + "/" + entry.getValue());
        }
    }

    private boolean canUpdate(String table, List<TableData> updatedData) throws ManagerException {
        for(TableData tableData : updatedData){
            String indexName = this.getIndex(table, tableData, true);
            if(indexName!=null){
                if(jedis.hget(table+"#"+indexName+"#"+tableData.getFieldName(), tableData.getFieldValue())!=null){
                    return false;
                }
            }else{
                if(tableData.isUnique()){
                    this.scanTableUniqueValue(table, tableData.getFieldIndex()+1, tableData);
                }
            }
        }
        return true;
    }

    private void updateKeyFromIndex(String table, String key, List<TableData> updatedData) {
        this.updateFromIndexes(table, key, updatedData, jedis.smembers("indexesUQ#"+table), true);
        this.updateFromIndexes(table, key, updatedData, jedis.smembers("indexesNUQ#"+table), false);
    }

    private void updateFromIndexes(String table, String key, List<TableData> updatedData, Set<String> indexes, boolean areUnique) {
        System.out.println("INDEXES AFTER UPDATE");
        for(TableData tableData : updatedData){
            for(String index : indexes){
                if(tableData.getFieldName().compareTo(index.split("#")[1])==0){
                    String indexKey = table+"#"+index;
                    Map<String, String> indexValues = jedis.hgetAll(indexKey);
                    for (Map.Entry<String, String> indexEntry : indexValues.entrySet()) {
                        if(areUnique){
                            if(indexEntry.getValue().compareTo(key)==0){
                                jedis.hdel(indexKey, indexEntry.getKey());
                                jedis.hset(indexKey, tableData.getFieldValue(), indexEntry.getValue());
                            }
                        }else{
                            if(indexEntry.getKey().split("#")[1].compareTo(key)==0){
                                jedis.hdel(indexKey, indexEntry.getKey());
                                jedis.hset(indexKey, tableData.getFieldValue()+"#"+indexEntry.getKey().split("#")[1], "");
                            }
                        }
                    }
                    System.out.println("----- updated index: " + indexKey + " --------/*");
                    indexValues = jedis.hgetAll(indexKey);
                    for (Map.Entry<String, String> indexEntry : indexValues.entrySet()) {
                        System.out.println(indexEntry.getKey() + " *** " + indexEntry.getValue());
                    }
                }
            }
        }
    }

    private String createValueFromList(String oldValue, List<TableData> data) {
        String value = "";
        int counter = 1;
        String[] oldValues = oldValue.split("#");
        for(String oldVal : oldValues){
            boolean wasUsed = false;
            for(TableData tableData : data){
                if(tableData.getFieldIndex() == counter){
                    value = value.concat(tableData.getFieldValue() + "#");
                    wasUsed = true;
                    break;
                }
            }
            if(!wasUsed){
                value = value.concat(oldVal + "#");
            }
            counter++;
        }
        return value.substring(0, value.length()-1);
    }

    public void createIndex(int databaseIndex, String table, TableField tableField, boolean isUnique) {
        jedis.select(databaseIndex);
        if(isUnique){
            System.out.println("CREATING UNIQUE INDEX......");
            String indexName = "idx"+tableField.getName()+"UQ";
            jedis.sadd("indexesUQ#"+table, indexName+"#"+tableField.getName());
            this.addExistingValuesToIndex(table,indexName, tableField, true);
        }else{
            System.out.println("CREATING NON-UNIQUE INDEX......");
            String indexName = "idx"+tableField.getName()+"NUQ";
            jedis.sadd("indexesNUQ#"+table, indexName+"#"+tableField.getName());
            this.addExistingValuesToIndex(table, indexName, tableField, false);
        }
    }

    private void addExistingValuesToIndex(String table, String indexName, TableField tableField, boolean isUnique){
        Map<String, String> fields = jedis.hgetAll(table);
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            String valueToAdd = entry.getValue().split("#")[tableField.getFieldIndex()-1];
            if(isUnique){
                jedis.hset(table+"#"+indexName+"#"+tableField.getName(), valueToAdd, entry.getKey());
            }else{
                jedis.hset(table+"#"+indexName+"#"+tableField.getName(), valueToAdd+"#"+entry.getKey(), "");
            }
        }
    }

    public void setCatalogServer(ServerImplementation catalogServer) {
        this.catalogServer = catalogServer;
    }

    public void addForeignKeyIndex(int databaseIndex, String table, List<TableField> tableFields){
        jedis.select(databaseIndex);
        String value = "";
        for(TableField tableField : tableFields){
            value = value.concat(tableField.getName()+"#");
        }
        value = value.substring(0, value.length()-1);
        jedis.sadd("indexesFK#"+table, value);
    }

    private TableData getTableData(WhereClause whereClause, String value){
        TableData tableData = new TableData();
        tableData.setFieldName(whereClause.getTableField().getName());
        tableData.setFieldValue(value);
        tableData.setTable(whereClause.getTableField().getTable());
        return tableData;
    }

    private SelectEntity getSelectEntity(String table, String primaryKey, String entityValue, List<TableField> tableFields){
        SelectEntity selectEntity = new SelectEntity();
        String[] values = entityValue.split("#");
        for(TableField tableField : tableFields){
            if(tableField.getIsPrimaryKey()){
                selectEntity.add(table, tableField.getName(), primaryKey);
            }else{
                selectEntity.add(table, tableField.getName(), values[tableField.getFieldIndex()-1]);
            }
        }
        return selectEntity;
    }

    private List<SelectEntity> getDataFromUniqueIndex(String table, List<TableField> tableFields, WhereClause whereClause){
        String indexName = this.getIndex(table, whereClause.getTableField(), true);
        List<SelectEntity> result = new ArrayList<>();
        String primaryKey = jedis.hget(table+"#"+indexName+"#"+whereClause.getTableField().getName(), whereClause.getValue());
        if(primaryKey!=null){
            String entityValue = jedis.hget(table, primaryKey);
            result.add(this.getSelectEntity(table, primaryKey, entityValue, tableFields));
        }
        return result;
    }

    private String getRange(String operator, String value){
        int charValue = value.charAt(0);
        switch (operator){
            case "=":
                return value;
            case "!=":
                return "^(?!.*"+value+").*$";
            case "<=":
                return "[a-"+String.valueOf((char) (charValue))+"]";
            case "<":
                return "[a-"+String.valueOf((char) (charValue - 1))+"]";
            case ">=":
                return "["+String.valueOf((char) (charValue))+"-z]";
            case ">":
                return "["+String.valueOf((char) (charValue + 1))+"-z]";
        }
        return null;
    }

    private String getMatchValue(String rangeValue, String operator){
        switch (operator){
            case "=":
                return rangeValue.concat("#*");
            case "!=":
                return rangeValue;
            case "<=":
                return rangeValue.concat("*#*");
            case "<":
                return rangeValue.concat("*#*");
            case ">=":
                return rangeValue.concat("*#*");
            case ">":
                return rangeValue.concat("*#*");
        }
        return null;
    }

    private List<SelectEntity> getDataFromNonUniqueIndex(String table, List<TableField> tableFields, WhereClause whereClause, boolean isForeignKey){
        String indexName = this.getIndex(table, whereClause.getTableField(), false);
        List<SelectEntity> result = new ArrayList<>();
        String rangeValue = this.getRange(whereClause.getOperator(), whereClause.getValue());
        String matchValue = this.getMatchValue(rangeValue, whereClause.getOperator());
        String rediskey = "";
        if(isForeignKey){
            rediskey = table+"#"+whereClause.getTableField().getName();
        }else{
            rediskey = table+"#"+indexName+"#"+whereClause.getTableField().getName();
        }
        List<Map.Entry<String, String>> indexResult = this.patternResult(matchValue, rediskey);
        if(indexResult.size() > 0){
            for (Map.Entry<String, String> entry : indexResult) {
                String[] keys = entry.getKey().split("#");
                SelectEntity selectEntity = this.getSelectEntity(table, keys[1], jedis.hget(table, keys[1]), tableFields);
                result.add(selectEntity);
            }
        }
        return result;
    }

    private List<Map.Entry<String, String>> patternResult(String matchValue, String redisKey){
        List<Map.Entry<String, String>> res = new ArrayList<>();
        ScanParams scanParams = new ScanParams().count(100).match(matchValue);
        String crtCursor = redis.clients.jedis.ScanParams.SCAN_POINTER_START;
        boolean cycleIsFinished = false;
        while(!cycleIsFinished) {
            ScanResult<Map.Entry<String, String>> scanResult =
                    jedis.hscan(redisKey, crtCursor, scanParams);
            List<Map.Entry<String, String>> indexResult = scanResult.getResult();
            if(indexResult.size() > 0){
                return indexResult;
            }
            crtCursor = scanResult.getCursor();
            if (crtCursor.equals("0")) {
                cycleIsFinished = true;
            }
        }
        return res;
    }

    private List<SelectEntity> getEquivalentTableData(String table, List<SelectEntity> previousResult, List<SelectEntity> fromIndexResult){
        List<SelectEntity> result = new ArrayList<>();
        for(SelectEntity previousSelectEntity : previousResult){
            boolean exists = false;
            List<TableData> tableDataListFromPrevious = previousSelectEntity.getTableData(table);
            for(SelectEntity fromIndexSelectEntity : fromIndexResult){
                for(TableData tableData : previousSelectEntity.getTableData(table)){
                    if(fromIndexSelectEntity.contains(table, tableData)){
                        exists = true;
                    }
                }
                if(exists){
                    result.add(previousSelectEntity);
                }
            }
        }
        return result;
    }

    private boolean checkByOperator(String firstValue, String operator, String secondValue){
        switch (operator){
            case "=":
                return firstValue.compareTo(secondValue)==0;
            case "!=":
                return firstValue.compareTo(secondValue)!=0;
            case "<=":
                return firstValue.compareTo(secondValue)<=0;
            case "<":
                return firstValue.compareTo(secondValue)<0;
            case ">=":
                return firstValue.compareTo(secondValue)>=0;
            case ">":
                return firstValue.compareTo(secondValue)>0;
        }
        return false;
    }

    private boolean checkEntity(String table, SelectEntity selectEntity, WhereClause whereClause){
        String entityValue = selectEntity.get(table, whereClause.getTableField().getName());
        return this.checkByOperator(entityValue, whereClause.getOperator(), whereClause.getValue());

    }

    private List<SelectEntity> getFilteredEntitiesList(List<SelectEntity> currentResult, WhereClause whereClause){
        List<SelectEntity> result = new ArrayList<>();
        for(SelectEntity selectEntity : currentResult){
            if(this.checkEntity(whereClause.getTableField().getTable(), selectEntity, whereClause)){
                result.add(selectEntity);
            }
        }
        return result;
    }

    private List<SelectEntity> getByPk(String table, List<TableField> tableFields, WhereClause whereClause){
        List<SelectEntity> result = new ArrayList<>();
        String entityValue = jedis.hget(table, whereClause.getValue());
        if(entityValue == null){
            return result;
        }
        SelectEntity selectEntity = this.getSelectEntity(whereClause.getTableField().getTable(), whereClause.getValue(), entityValue, tableFields);
        result.add(selectEntity);
        return result;
    }

    public List<SelectEntity> getTuplesFromWhereClauses(int databaseIndex, String table, List<WhereClause> whereClauses, List<TableField> tableFields){
        jedis.select(databaseIndex);
        int iteration = 1;
        boolean indexUsed = false;
        List<SelectEntity> selectResult = new ArrayList<>();
        for(WhereClause whereClause : whereClauses){
            if(whereClause.getTableField().getTable().compareTo(table) == 0){
                List<SelectEntity> indexResult = new ArrayList<>();
                if(whereClause.getTableField().getIsPrimaryKey() && whereClause.getOperator().compareTo("=")==0){
                    indexResult = this.getByPk(table, tableFields, whereClause);
                    whereClause.setWasIndexUsed(true);
                    indexUsed = true;
                }
                if(whereClause.getTableField().isForeignKey()){
                    indexResult = this.getDataFromNonUniqueIndex(table, tableFields, whereClause, true);
                    whereClause.setWasIndexUsed(true);
                    indexUsed = true;
                }
                if(whereClause.getTableField().getIsUnique() && whereClause.getOperator().compareTo("=")==0) {
                    String indexName = this.getIndex(table, whereClause.getTableField(), true);
                    if(indexName != null){
                        indexResult = this.getDataFromUniqueIndex(table, tableFields, whereClause);
                        whereClause.setWasIndexUsed(true);
                        indexUsed = true;
                    }
                }else{
                    if(!whereClause.getTableField().isForeignKey()){
                        String indexName = this.getIndex(table, whereClause.getTableField(), false);
                        if(indexName!=null){
                            indexResult = this.getDataFromNonUniqueIndex(table, tableFields, whereClause, false);
                            whereClause.setWasIndexUsed(true);
                            indexUsed = true;
                        }
                    }
                }
                if(whereClause.isWasIndexUsed()){
                    if(iteration == 1 || selectResult.size()==0){
                        selectResult.addAll(indexResult);
                    }else{
                        selectResult = this.getEquivalentTableData(whereClause.getTableField().getTable(), selectResult, indexResult);
                    }
                }
                iteration++;
            }
        }
        if(indexUsed){
            for(WhereClause whereClause : whereClauses){
                if(!whereClause.isWasIndexUsed() && whereClause.getTableField().getTable().compareTo(table)==0){
                    selectResult = this.getFilteredEntitiesList(selectResult, whereClause);
                }
            }
        }else{
            selectResult = this.scanTable(table, tableFields, whereClauses);
        }
        return selectResult;
    }

    public List<TableRowResult> selectTest(int databaseIndex, String table, List<WhereClause> whereClauses, List<TableField> projectionFields, List<TableField> tableFields, List<JoinClause> joinClauses, GroupByClause groupByClause, List<HavingClause> havingOperators) throws ManagerException, RemoteException {
        jedis.select(databaseIndex);
        List<SelectEntity> selectResult = this.getTuplesFromWhereClauses(databaseIndex, table, whereClauses, tableFields);
        List<SelectEntity> queryResult;
        if(joinClauses.size() > 0){
            List<JoinEntity> joinEntities = new ArrayList<>();
            joinEntities.add(new JoinEntity(table, selectResult));
            for(JoinClause joinClause : joinClauses){
                List<TableField> tableFieldsJoin = this.catalogServer.getTableFields(databaseIndex, joinClause.getTable());
                List<SelectEntity> joinTableFiltered = this.getTuplesFromWhereClauses(databaseIndex, joinClause.getTable(), whereClauses, tableFieldsJoin);
                joinEntities.add(new JoinEntity(joinClause.getTable(), joinTableFiltered, joinClause.getJoinType()));
            }
            queryResult = this.join(databaseIndex, joinEntities, "smj");
            this.projection(queryResult, projectionFields);
            boolean wasIntersected = false;
            if(groupByClause.getTable() != null){
                if(groupByClause.getAggregateOperators().size() == 0){
                    if(havingOperators.size() > 0){
                        wasIntersected = true;
                        for(HavingClause havingClause : havingOperators){
                            groupByClause.addAggregateOperator(havingClause.getAggregateOperators().toString());
                        }
                    }
                }
                queryResult = this.groupBy(queryResult, groupByClause);
            }
            if(havingOperators.size() > 0){
                queryResult = this.filterByHaving(queryResult, groupByClause.getTableField(), havingOperators);
            }
            if(wasIntersected){
                this.projection(queryResult, projectionFields);
            }
            return this.getTableRows(queryResult);
        }else{
            this.projection(selectResult, projectionFields);
            if(groupByClause.getTable() != null){
                selectResult = this.groupBy(selectResult, groupByClause);
                selectResult = this.filterByHaving(selectResult, groupByClause.getTableField(), havingOperators);
            }
            return this.getTableRows(selectResult);
        }
    }

    private List<SelectEntity> filterByHaving(List<SelectEntity> queryResult, TableField tableField, List<HavingClause> havingOperators) {
        List<SelectEntity> selectEntities = new ArrayList<>();
        if(havingOperators.size() > 0){
            for(SelectEntity selectEntity : queryResult){
                if(this.checkHavingValue(selectEntity, tableField, havingOperators)){
                    selectEntities.add(selectEntity);
                }
            }
        }
        return selectEntities;
    }

    private boolean checkHavingValue(SelectEntity selectEntity, TableField tableField, List<HavingClause> havingOperators) {
        for(HavingClause havingClause : havingOperators){
            if(!this.checkByOperator(selectEntity.get(tableField.getTable(), havingClause.getAggregateOperators().toString()), havingClause.getOperator(), havingClause.getValue())){
                return false;
            }
        }
        return true;
    }

    private List<SelectEntity> groupBy(List<SelectEntity> queryResult, GroupByClause groupByClause) {
        List<SelectEntity> groupByResult = new ArrayList<>();
        Map<String, List<SelectEntity>> hmGroupBy = new HashMap<>();
        for(SelectEntity selectEntity : queryResult){
            String groupValue = selectEntity.get(groupByClause.getTable(), groupByClause.getTableField().getName());
            if(!hmGroupBy.containsKey(groupValue)){
                hmGroupBy.put(groupValue, new ArrayList<>());
            }
            hmGroupBy.get(groupValue).add(selectEntity);
        }
        for (Map.Entry<String, List<SelectEntity>> entry : hmGroupBy.entrySet()) {
            String groupValue = entry.getKey();
            List<SelectEntity> groupedSelectEntities = entry.getValue();
            HashMap<AggregateOperators, Float> aggregationResults = new HashMap<>();
            for(AggregateOperators aggregateOperator : groupByClause.getAggregateOperators()){
                float aggregationResult = this.getAggregationResult(groupByClause.getTableField(), aggregateOperator, groupedSelectEntities);
                aggregationResults.put(aggregateOperator, aggregationResult);
            }
            SelectEntity selectEntity = this.createGroupedSelectEntity(groupByClause.getTableField(), groupValue, aggregationResults);
            groupByResult.add(selectEntity);
        }
        return groupByResult;
    }

    private SelectEntity createGroupedSelectEntity(TableField tableField, String groupValue, HashMap<AggregateOperators, Float> aggregationResults) {
        SelectEntity result = new SelectEntity();
        result.add(tableField.getTable(), tableField.getName(), groupValue);
        for (Map.Entry<AggregateOperators, Float> entry : aggregationResults.entrySet()) {
            AggregateOperators aggregateOperators = entry.getKey();
            Float aggrResult = entry.getValue();
            result.add(tableField.getTable(), aggregateOperators.toString(), aggrResult.toString());
        }
        return result;
    }

    private float getAggregationResult(TableField tableField, AggregateOperators aggregateOperator, List<SelectEntity> groupedSelectEntities) {
        float result = 0;
        switch (aggregateOperator){
            case SUM:
                result = this.getSum(tableField, groupedSelectEntities);
                break;
            case COUNT:
                result = this.getCount(tableField, groupedSelectEntities);
                break;
            case AVG:
                result = this.getAvg(tableField, groupedSelectEntities);
                break;
        }
        return result;
    }

    private float getSum(TableField tableField, List<SelectEntity> groupedSelectEntities){
        float result = 0;
        for(SelectEntity selectEntity : groupedSelectEntities){
            result += Float.valueOf(selectEntity.get(tableField.getTable(), tableField.getName()));
        }
        return result;
    }

    private float getCount(TableField tableField, List<SelectEntity> groupedSelectEntities){
        return groupedSelectEntities.size();
    }

    private float getAvg(TableField tableField, List<SelectEntity> groupedSelectEntities){
        float result = 0;
        for(SelectEntity selectEntity : groupedSelectEntities){
            result += Float.valueOf(selectEntity.get(tableField.getTable(), tableField.getName()));
        }
        return result;
    }

    private List<SelectEntity> join(int databaseIndex, List<JoinEntity> joinEntities, String joinType) throws ManagerException, RemoteException {
        List<SelectEntity> joinResult = new ArrayList<>();
        JoinEntity firstJoinEntity = joinEntities.get(0);
        for(int counter = 1; counter < joinEntities.size(); counter++){
            JoinEntity secondJoinEntity = joinEntities.get(counter);
            List<SelectEntity> intermediateResult = new ArrayList<>();
            switch(joinType){
                case "inlj":
                    switch(secondJoinEntity.getJoinType()){
                        case INNER:
                            intermediateResult = this.innerINLJ(databaseIndex, firstJoinEntity, secondJoinEntity);
                            break;
                        case LEFT:
                            intermediateResult = this.leftINLJ(databaseIndex, firstJoinEntity, secondJoinEntity);
                            break;
                        case RIGHT:
                            intermediateResult = this.rightINLJ(databaseIndex, firstJoinEntity, secondJoinEntity);
                            break;
                        case FULL:
                            intermediateResult = this.fullINLJ(databaseIndex, firstJoinEntity, secondJoinEntity);
                            break;
                    }
                    break;
                case "smj":
                    switch(secondJoinEntity.getJoinType()){
                        case INNER:
                            intermediateResult = this.innerSMJ(databaseIndex, firstJoinEntity, secondJoinEntity);
                            break;
                        case LEFT:
                            intermediateResult = this.leftSMJ(databaseIndex, firstJoinEntity, secondJoinEntity);
                            break;
                        case RIGHT:
                            intermediateResult = this.rightSMJ(databaseIndex, firstJoinEntity, secondJoinEntity);
                            break;
                        case FULL:
                            intermediateResult = this.fullSMJ(databaseIndex, firstJoinEntity, secondJoinEntity);
                            break;
                    }
                    break;
            }
            if(counter+1 < joinEntities.size()){
                firstJoinEntity = new JoinEntity(secondJoinEntity.getTable(), intermediateResult, joinEntities.get(counter + 1).getJoinType());
            }else{
                joinResult = intermediateResult;
            }
        }
        return joinResult;
    }

    private List<SelectEntity> fullSMJ(int databaseIndex, JoinEntity firstJoinEntity, JoinEntity secondJoinEntity) throws ManagerException, RemoteException {
        List<SelectEntity> leftResult = this.leftSMJ(databaseIndex, firstJoinEntity, secondJoinEntity);
        List<SelectEntity> rightResult = this.rightSMJ(databaseIndex, firstJoinEntity, secondJoinEntity);
        this.intersectLists(leftResult, rightResult);
        return leftResult;
    }

    private void intersectLists(List<SelectEntity> leftResult, List<SelectEntity> rightResult){
        for(SelectEntity rightSelectEntity : rightResult){
            boolean exists = false;
            for(SelectEntity leftSelectEntity : leftResult){
                if(leftSelectEntity.equals(rightSelectEntity)){
                    exists = true;
                    break;
                }
            }
            if(!exists){
                leftResult.add(rightSelectEntity);
            }
        }
    }

    private List<SelectEntity> fullINLJ(int databaseIndex, JoinEntity firstJoinEntity, JoinEntity secondJoinEntity) throws ManagerException, RemoteException {
        List<SelectEntity> leftResult = this.leftINLJ(databaseIndex, firstJoinEntity, secondJoinEntity);
        List<SelectEntity> rightResult = this.rightINLJ(databaseIndex, firstJoinEntity, secondJoinEntity);
        this.intersectLists(leftResult, rightResult);
        return leftResult;
    }

    private List<SelectEntity> rightSMJ(int databaseIndex, JoinEntity firstJoinEntity, JoinEntity secondJoinEntity) throws ManagerException, RemoteException {
        return this.leftSMJ(databaseIndex, secondJoinEntity, firstJoinEntity);
    }

    private List<SelectEntity> leftSMJ(int databaseIndex, JoinEntity firstJoinEntity, JoinEntity secondJoinEntity) throws ManagerException, RemoteException {
        TableField commonTableField = this.catalogServer.getCommonTableField(databaseIndex, firstJoinEntity.getTable(), secondJoinEntity.getTable());
        List<SelectEntity> joinResult = new ArrayList<>();
        JoinEntity parentJoinEntity;
        JoinEntity childJoinEntity;
        if(commonTableField.getForeignKeyTable().compareTo(secondJoinEntity.getTable())==0){
            return this.innerSMJ(databaseIndex, firstJoinEntity, secondJoinEntity);
        }else{
            parentJoinEntity = firstJoinEntity;
            childJoinEntity = secondJoinEntity;
            this.sortJoinEntities(parentJoinEntity, childJoinEntity, commonTableField);
            int childIndex = 0;
            String parentTable = parentJoinEntity.getTable();
            String childTable = childJoinEntity.getTable();
            for(SelectEntity parentSelectEntity : parentJoinEntity.getSelectEntities()){
                Tuple parentJoinTuple = parentSelectEntity.getTupleByTable(parentTable);
                if(childIndex >= childJoinEntity.getSelectEntities().size()){
                    SelectEntity joinResultEntity = this.getNullJoinEntity(databaseIndex, parentSelectEntity, childTable);
                    joinResult.add(joinResultEntity);
                }
                if(childIndex < childJoinEntity.getSelectEntities().size() && parentJoinTuple.get(commonTableField.getName()).compareTo(childJoinEntity.getSelectEntities().get(childIndex).get(childTable, commonTableField.getName()))!=0){
                    SelectEntity joinResultEntity = this.getNullJoinEntity(parentSelectEntity, childJoinEntity.getSelectEntities().get(childIndex));
                    joinResult.add(joinResultEntity);
                    childIndex++;
                }
                while(childIndex < childJoinEntity.getSelectEntities().size() && parentJoinTuple.get(commonTableField.getName()).compareTo(childJoinEntity.getSelectEntities().get(childIndex).get(childTable, commonTableField.getName()))==0){
                    SelectEntity joinResultEntity = this.getJoinEntityFromTuples(parentSelectEntity, childJoinEntity.getSelectEntities().get(childIndex));
                    joinResult.add(joinResultEntity);
                    childIndex++;
                }
            }
            return joinResult;
        }
    }

    private SelectEntity getNullJoinEntity(SelectEntity parentSelectEntity, SelectEntity selectEntity) {
        SelectEntity result = new SelectEntity();
        result.add(parentSelectEntity);
        for(Tuple tuple : selectEntity.getTuples()){
            Tuple resultTuple = new Tuple();
            resultTuple.setTable(tuple.getTable());
            for(TableData tableData : tuple.tableData){
                TableData tableDataResult = tableData.copy();
                tableDataResult.setFieldValue("null");
                tuple.add(tableDataResult);
            }
            result.add(resultTuple);
        }
        return result;
    }

    private SelectEntity getNullJoinEntity(int databaseIndex, SelectEntity parentSelectEntity, String table) throws ManagerException, RemoteException {
        SelectEntity result = new SelectEntity();
        result.add(parentSelectEntity);
        List<TableField> tableFields = this.catalogServer.getTableFields(databaseIndex, table);
        for(TableField tableField : tableFields){
            Tuple resultTuple = new Tuple();
            resultTuple.setTable(table);
            resultTuple.add(tableField.getName(), "null");
            result.add(resultTuple);
        }
        return result;
    }

    private List<SelectEntity> rightINLJ(int databaseIndex, JoinEntity firstJoinEntity, JoinEntity secondJoinEntity) throws ManagerException, RemoteException {
        return this.leftINLJ(databaseIndex, secondJoinEntity, firstJoinEntity);
    }

    private List<SelectEntity> leftINLJ(int databaseIndex, JoinEntity firstJoinEntity, JoinEntity secondJoinEntity) throws ManagerException, RemoteException {
        TableField commonTableField = this.catalogServer.getCommonTableField(databaseIndex, firstJoinEntity.getTable(), secondJoinEntity.getTable());
        List<SelectEntity> joinResult = new ArrayList<>();
        JoinEntity parentJoinEntity;
        JoinEntity childJoinEntity;
        if(commonTableField.getForeignKeyTable().compareTo(secondJoinEntity.getTable())==0){
            //parentJoinEntity = secondJoinEntity;
            //childJoinEntity = firstJoinEntity;
            return this.innerINLJ(databaseIndex, firstJoinEntity, secondJoinEntity);
        }else{
            parentJoinEntity = firstJoinEntity;
            childJoinEntity = secondJoinEntity;
            for(SelectEntity parentSelectEntity : parentJoinEntity.getSelectEntities()){
                List<Tuple> tableTuples = parentSelectEntity.getTuplesByTable(parentJoinEntity.getTable());
                for(Tuple tuple : tableTuples){
                    for(TableData tableDataEntity : tuple.tableData){
                        if(tableDataEntity.getFieldName().compareTo(commonTableField.getName())==0){
                            HashMap<String, List<String>> values = new HashMap<>();
                            values.computeIfAbsent(tableDataEntity.getFieldValue(), k -> new ArrayList<>());
                            values = this.getDataFromChildTable(secondJoinEntity.getTable(), tableDataEntity.getFieldName(), tableDataEntity.getFieldValue());
                            if(values.get(tableDataEntity.getFieldValue()) == null){
                                List<TableField> tableFields = this.catalogServer.getTableFields(databaseIndex, childJoinEntity.getTable());
                                String res = "";
                                for(TableField tableField : tableFields){
                                    res = res.concat("null#");
                                }
                                res = res.substring(0, res.length()-1);
                                List<String> results = new ArrayList<>();
                                results.add(res);
                                values.put(tableDataEntity.getFieldValue(), results);
                            }
                            List<SelectEntity> joinEntities = this.getTuplesFromJoin(databaseIndex, parentSelectEntity, childJoinEntity.getTable(), values);
                            joinResult.addAll(joinEntities);
                        }
                    }
                }
            }
            return joinResult;
        }
    }

    private void sortJoinEntities(JoinEntity parentJoinEntity, JoinEntity childJoinEntity, TableField commonField){
        parentJoinEntity.getSelectEntities().sort(new Comparator<SelectEntity>() {
            @Override
            public int compare(SelectEntity o1, SelectEntity o2) {
                return Integer.valueOf(o1.get(parentJoinEntity.getTable(), commonField.getName())) - Integer.valueOf(o2.get(parentJoinEntity.getTable(), commonField.getName()));
            }
        });
        childJoinEntity.getSelectEntities().sort(new Comparator<SelectEntity>() {
            @Override
            public int compare(SelectEntity o1, SelectEntity o2) {
                return Integer.valueOf(o1.get(childJoinEntity.getTable(), commonField.getName())) - Integer.valueOf(o2.get(childJoinEntity.getTable(), commonField.getName()));
            }
        });
    }

    private List<SelectEntity> innerSMJ(int databaseIndex, JoinEntity firstJoinEntity, JoinEntity secondJoinEntity) {
        TableField commonTableField = this.catalogServer.getCommonTableField(databaseIndex, firstJoinEntity.getTable(), secondJoinEntity.getTable());
        List<SelectEntity> joinResult = new ArrayList<>();
        JoinEntity parentJoinEntity;
        JoinEntity childJoinEntity;
        if(commonTableField.getForeignKeyTable().compareTo(secondJoinEntity.getTable())==0){
            //first - copil, second - parinte
            parentJoinEntity = secondJoinEntity;
            childJoinEntity = firstJoinEntity;
        }else{
            parentJoinEntity = firstJoinEntity;
            childJoinEntity = secondJoinEntity;
        }
        this.sortJoinEntities(parentJoinEntity, childJoinEntity, commonTableField);
        int childIndex = 0;
        String parentTable = parentJoinEntity.getTable();
        String childTable = childJoinEntity.getTable();
        for(SelectEntity parentSelectEntity : parentJoinEntity.getSelectEntities()){
            Tuple parentJoinTuple = parentSelectEntity.getTupleByTable(parentTable);
            while(childIndex < childJoinEntity.getSelectEntities().size() &&
                    parentJoinTuple.get(commonTableField.getName()).compareTo(childJoinEntity.getSelectEntities().get(childIndex).get(childTable, commonTableField.getName()))!=0){
                childIndex++;
            }
            while(childIndex < childJoinEntity.getSelectEntities().size() && parentJoinTuple.get(commonTableField.getName()).compareTo(childJoinEntity.getSelectEntities().get(childIndex).get(childTable, commonTableField.getName()))==0){
                SelectEntity joinResultEntity = this.getJoinEntityFromTuples(parentSelectEntity, childJoinEntity.getSelectEntities().get(childIndex));
                joinResult.add(joinResultEntity);
                childIndex++;
            }
        }
        return joinResult;

    }

    private SelectEntity getJoinEntityFromTuples(SelectEntity parentSelectEntity, SelectEntity childSelectEntity) {
        SelectEntity resultSelectEntity = new SelectEntity();
        resultSelectEntity.add(parentSelectEntity);
        resultSelectEntity.add(childSelectEntity);
        return resultSelectEntity;
    }

    private List<SelectEntity> innerINLJ(int databaseIndex, JoinEntity firstJoinEntity, JoinEntity secondJoinEntity) {
        TableField commonTableField = this.catalogServer.getCommonTableField(databaseIndex, firstJoinEntity.getTable(), secondJoinEntity.getTable());
        List<SelectEntity> joinResult = new ArrayList<>();
        for(SelectEntity selectEntity : firstJoinEntity.getSelectEntities()){
            List<Tuple> tableTuples = selectEntity.getTuplesByTable(firstJoinEntity.getTable());
            for(Tuple tuple : tableTuples){
                for(TableData tableDataEntity : tuple.tableData){
                    if(tableDataEntity.getFieldName().compareTo(commonTableField.getName())==0){
                        HashMap<String, List<String>> values = new HashMap<>();
                        values.computeIfAbsent(tableDataEntity.getFieldValue(), k -> new ArrayList<>());
                        if(commonTableField.getForeignKeyTable().compareTo(secondJoinEntity.getTable())==0){
                            values.get(tableDataEntity.getFieldValue()).add(tableDataEntity.getFieldValue().concat("#"+this.jedis.hget(secondJoinEntity.getTable(), tableDataEntity.getFieldValue())));
                        }else{
                            values = this.getDataFromChildTable(secondJoinEntity.getTable(), tableDataEntity.getFieldName(), tableDataEntity.getFieldValue());
                        }
                        List<SelectEntity> joinEntities = this.getTuplesFromJoin(databaseIndex, selectEntity, secondJoinEntity.getTable(), values);
                        joinResult.addAll(joinEntities);
                    }
                }
            }
        }
        return joinResult;
    }

    public List<TableRowResult> select(int databaseIndex, String table, List<WhereClause> whereClauses, List<TableField> projectionFields, List<TableField> tableFields, List<JoinClause> joinClauses, GroupByClause groupByClause, List<HavingClause> havingClauses) throws ManagerException, RemoteException {
        jedis.select(databaseIndex);
        return this.selectTest(databaseIndex, table, whereClauses, projectionFields, tableFields, joinClauses, groupByClause, havingClauses);
    }

    private List<TableRowResult> getTableRows(List<SelectEntity> selectResult) {
        List<TableRowResult> result = new ArrayList<>();
        for(SelectEntity selectEntity : selectResult){
            TableRowResult tableRowResult = new TableRowResult();
            for(Tuple tuple : selectEntity.getTuples()){
                tableRowResult.addAll(tuple.getTable(), tuple.tableData);
            }
            result.add(tableRowResult);
        }
        return result;
    }

    private HashMap<String,List<String>> getDataFromChildTable(String foreignKeyTable, String fieldName, String fieldValue) {
        HashMap<String,List<String>> result = new HashMap<>();
        String rangeValue = this.getRange("=", fieldValue);
        String matchValue = this.getMatchValue(rangeValue, "=");
        String rediskey = foreignKeyTable + "#" + fieldName;
        List<Map.Entry<String, String>> indexResult = this.patternResult(matchValue, rediskey);
        if(indexResult.size() > 0){
            for (Map.Entry<String, String> entry : indexResult) {
                String[] keys = entry.getKey().split("#");
                String pk = keys[1];
                String values = jedis.hget(foreignKeyTable, pk);
                result.computeIfAbsent(fieldValue, k -> new ArrayList<>());
                result.get(fieldValue).add(pk+"#"+values);
            }
        }
        return result;
    }

    private List<SelectEntity> getTuplesFromJoin(int databaseIndex, SelectEntity selectEntity, String joinTable, HashMap<String, List<String>> values) {
        List<TableField> tableFields = null;
        try {
            tableFields = this.catalogServer.getTableFields(databaseIndex, joinTable);
            List<SelectEntity> joinEntities = new ArrayList<>();
            for(Map.Entry<String, List<String>> entry : values.entrySet()) {
                for(String value : entry.getValue()){
                    SelectEntity joinSelectEntity = new SelectEntity();
                    joinSelectEntity.add(selectEntity);
                    String[] splittedValues = value.split("#");
                    for(TableField tableField : tableFields){
                        if(tableField.getIsPrimaryKey()){
                            joinSelectEntity.add(joinTable, tableField.getName(), splittedValues[0]);
                        }else{
                            joinSelectEntity.add(joinTable, tableField.getName(), splittedValues[tableField.getFieldIndex()]);
                        }
                    }
                    joinEntities.add(joinSelectEntity);
                }
            }
            return joinEntities;
        } catch (RemoteException | ManagerException e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<SelectEntity> scanTable(String table, List<TableField> tableFields, List<WhereClause> whereClauses) {
        List<SelectEntity> result = new ArrayList<>();
        Map<String, String> fields = jedis.hgetAll(table);
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            String key = entry.getKey();
            String values = entry.getValue();
            if(this.checkValue(table, key, values, whereClauses)){
                result.add(this.getSelectEntity(table, key, values, tableFields));
            }
        }
        return result;
    }

    private boolean checkValue(String table, String primaryKey, String values, List<WhereClause> whereClauses){
        String[] entityValues = values.split("#");
        for(WhereClause whereClause : whereClauses){
            if(whereClause.getTableField().getTable().compareTo(table)==0){
                if(whereClause.getTableField().getIsPrimaryKey()){
                    if(!checkByOperator(primaryKey, whereClause.getOperator(), whereClause.getValue())){
                        return false;
                    }
                }else{
                    if(!checkByOperator(entityValues[whereClause.getTableField().getFieldIndex()-1], whereClause.getOperator(), whereClause.getValue())){
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public void projection(List<SelectEntity> selectResult, List<TableField> projectionFields){
        for(SelectEntity selectEntity : selectResult){
            selectEntity.keepProjectionFields(projectionFields);
        }
    }

    private void addLargeData(){
        this.prepareFiles();
        //adding data to Cities
        int numberOfRows = 100000;
        String cityName = "city";
        List<String> entities = new ArrayList<>();
        while(numberOfRows > 0){
            jedis.hset("Cities", String.valueOf(numberOfRows), cityName);
            entities.add(numberOfRows + "#" + cityName);
            numberOfRows--;
        }
        FileUtils.writeRaw(this.citiesInputFile, entities);

        //adding data to Persons
        numberOfRows = 1000000;
        String personName = "pname";
        entities = new ArrayList<>();
        Random random = new Random();
        while(numberOfRows > 0){
            int cityId = random.nextInt(100000) + 1;
            jedis.hset("Persons", String.valueOf(numberOfRows), personName+"#"+cityId);
            String buffer = numberOfRows + "#" + personName + "#" + cityId;
            entities.add(buffer);
            numberOfRows--;
        }
        FileUtils.writeRaw(this.personsInputFile, entities);
    }

    private void prepareFiles() {
        FileUtils.clearFile(this.personsInputFile);
        FileUtils.clearFile(this.citiesInputFile);
        FileUtils.clearFile(this.personsOutputFile);
        FileUtils.clearFile(this.citiesOutputFile);
        FileUtils.clearFile(this.joinOutputFile);
    }

    public void externalSortJoin(int databaseIndex) {
        jedis.select(databaseIndex);
        //this.addLargeData();
        Comparator<String> pComparator = new Comparator<String>() {
            public int compare(String r1, String r2){
                String comp1 = r1.split("#")[2];
                String comp2 = r2.split("#")[2];
                System.out.println("Persons: " + comp1 + " " + comp2);
                return Integer.valueOf(comp1) - Integer.valueOf(comp2);
            }
        };
        Comparator<String> cComparator = new Comparator<String>() {
            public int compare(String r1, String r2){
                String comp1 = r1.split("#")[0];
                String comp2 = r2.split("#")[0];
                return Integer.valueOf(comp1) - Integer.valueOf(comp2);
            }
        };
        List<File> l = null;
        List<File> r = null;
        try {
            r = ExternalSorter.sortInBatch(new File(this.citiesInputFile), cComparator);
            ExternalSorter.mergeSortedFiles(r, new File(this.citiesOutputFile), cComparator);

            l = ExternalSorter.sortInBatch(new File(this.personsInputFile), pComparator);
            ExternalSorter.mergeSortedFiles(l, new File(this.personsOutputFile), pComparator);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Comparator<String> joinComparator = new Comparator<String>() {
            public int compare(String r1, String r2){
                String comp1 = r1.split("#")[2];
                String comp2 = r2.split("#")[0];
                return Integer.valueOf(comp1) - Integer.valueOf(comp2);
            }
        };
        MergeJoiner.mergeJoinFiles(this.citiesOutputFile, this.personsOutputFile, this.joinOutputFile, joinComparator);
    }
}
