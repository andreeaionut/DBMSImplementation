import java.time.LocalDate;

public class TestDate {
    public static void main(String[] args){
            System.out.println(DateUtils.DATENAME(DatePart.weekday, LocalDate.parse("2019-11-19")));

    }
}
