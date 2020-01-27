public enum DateFunction {
    GETDATE, DATEADD, DATEDIFF, DATEFROMPARTS, DATENAME, DATEPART, DAY, MONTH, YEAR, ISDATE;

    public static boolean contains(String dateFunction){
        for(DateFunction df : DateFunction.values()){
            if(df.toString().equals(dateFunction.toUpperCase())){
                return true;
            }
        }
        return false;
    }

    public static DateFunction getDateFunctionByText(String dateFunction){
        String upperCaseFunction = dateFunction.toUpperCase();
        switch (upperCaseFunction){
            case "GETDATE":
                return GETDATE;
            case "DATEADD":
                return DATEADD;
            case "DATEDIFF":
                return DATEDIFF;
            case "DATEFROMPARTS":
                return DATEFROMPARTS;
            case "DATENAME":
                return DATENAME;
            case "DATEPART":
                return DATEPART;
            case "DAY":
                return DAY;
            case "MONTH":
                return MONTH;
            case "YEAR":
                return YEAR;
            case "ISDATE":
                return ISDATE;
        }
        return null;
    }

    public static int getNumberOfParametersForFunction(String stringFunction){
        DateFunction dateFunction = DateFunction.getDateFunctionByText(stringFunction);
        switch(dateFunction){
            case GETDATE:
                return 0;
            case DATEADD:
                return 3;
            case DATEDIFF:
                return 3;
            case DATEFROMPARTS:
                return 3;
            case DATENAME:
                return 2;
            case DATEPART:
                return 2;
            case DAY:
                return 1;
            case MONTH:
                return 1;
            case YEAR:
                return 1;
            case ISDATE:
                return 1;
        }
        return -1;
    }

    public static String getParameterType(String dateFunction, int parameterIndex){
        DateFunction df = DateFunction.getDateFunctionByText(dateFunction);
        switch (parameterIndex){
            case 1:
                return DateFunction.getFirstParameterType(df);
            case 2:
                return DateFunction.getSecondParameterType(df);
            case 3:
                return DateFunction.getThirdParameterType(df);
        }
        return null;
    }

    private static String getFirstParameterType(DateFunction dateFunction){
        switch(dateFunction){
            case DATEADD:
                return "DatePart";
            case DATEDIFF:
                return "DatePart";
            case DATEFROMPARTS:
                return "int";
            case DATENAME:
                return "DatePart";
            case DATEPART:
                return "DatePart";
            case DAY:
                return "LocalDate";
            case MONTH:
                return "LocalDate";
            case YEAR:
                return "LocalDate";
            case ISDATE:
                return "String";
        }
        return null;
    }

    private static String getSecondParameterType(DateFunction dateFunction){
        switch(dateFunction){
            case DATEADD:
                return "int";
            case DATEDIFF:
                return "LocalDate";
            case DATEFROMPARTS:
                return "int";
            case DATENAME:
                return "LocalDate";
            case DATEPART:
                return "LocalDate";
        }
        return null;
    }

    private static String getThirdParameterType(DateFunction dateFunction){
        switch(dateFunction){
            case DATEADD:
                return "LocalDate";
            case DATEDIFF:
                return "LocalDate";
            case DATEFROMPARTS:
                return "int";
        }
        return null;
    }
}
