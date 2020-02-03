import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Utils {

    public static String shuffle(String input){
        List<Character> characters = new ArrayList<Character>();
        for(char c:input.toCharArray()){
            characters.add(c);
        }
        StringBuilder output = new StringBuilder(input.length());
        while(characters.size()!=0){
            int randPicker = (int)(Math.random()*characters.size());
            output.append(characters.remove(randPicker));
        }
        return output.toString();
    }

    public static String getValueString(List<TableData> data){
        String result = "";
        for(TableData tableData : data){
            if(!tableData.isPrimaryKey()){
                result = result.concat(tableData.getFieldValue() + "#");
            }
        }
        return result.substring(0, result.length()-1);
    }

    public static boolean isDateQuerySyntax(String dateQuery){
        String dateFunction = dateQuery.split("\\(")[0];
        if(!DateFunction.contains(dateFunction)){
            return false;
        }
        if(DateFunction.getDateFunctionByText(dateFunction).equals(DateFunction.GETDATE)){
            return true;
        }
        if(dateQuery.split("\\(")[1].split("\\)").length == 0){
            return false;
        }
        String[] params = dateQuery.split("\\(")[1].split("\\)")[0].split(",");
        int nrOfParams = DateFunction.getNumberOfParametersForFunction(dateFunction);
        if(params.length != nrOfParams){
            return false;
        }
        return Utils.areParameters(dateFunction, params);
    }

    private static boolean areParameters(String dateFunction, String[] params) {
        int paramIndex = 1;
        for(String param : params){
            String paramType = DateFunction.getParameterType(dateFunction, paramIndex);
            switch (paramType){
                case "int":
                    try{
                        Integer.parseInt(param);
                    }catch(NumberFormatException e){
                        return false;
                    }
                    break;
                case "DatePart":
                    if(!DatePart.contains(param)){
                        return false;
                    }
                    break;
                case "LocalDate":
                    if(DateUtils.ISDATE(param)==0){
                        return false;
                    }
                    break;
            }
            paramIndex++;
        }
        return true;
    }

    public static String getRandomString(int n) {
        String AlphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                + "0123456789"
                + "abcdefghijklmnopqrstuvxyz";
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            int index = (int)(AlphaNumericString.length() * Math.random());
            sb.append(AlphaNumericString
                    .charAt(index));
        }
        return sb.toString();
    }


}
