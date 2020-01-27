public enum DatePart {
    year, month, week, day, dayofyear, weekday;

    public static boolean contains(String stringDatePart){
        for(DatePart datePart : DatePart.values()){
            if(datePart.toString().equals(stringDatePart.toLowerCase())){
                return true;
            }
        }
        return false;
    }

    public static DatePart getByText(String stringDatePart){
        for(DatePart datePart : DatePart.values()){
            if(datePart.toString().equals(stringDatePart.toLowerCase())){
                return datePart;
            }
        }
        return null;
    }

}
