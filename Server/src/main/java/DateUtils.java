import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateUtils {

    public static LocalDate GETDATE() {
        return LocalDate.now();
    }

    public static LocalDate DATEADD(DatePart datePart, int number, LocalDate localDate) {
        switch (datePart) {
            case year:
                return localDate.plusYears(number);
            case month:
                return localDate.plusMonths(number);
            case week:
                return localDate.plusWeeks(number);
            case day:
                return localDate.plusDays(number);
        }
        return localDate;
    }

    public static long DATEDIFF(DatePart datePart, LocalDate startDate, LocalDate endDate) {
        switch (datePart) {
            case year:
                return ChronoUnit.YEARS.between(startDate, endDate);
            case month:
                return ChronoUnit.MONTHS.between(startDate, endDate);
            case week:
                return ChronoUnit.WEEKS.between(startDate, endDate);
            case day:
                return ChronoUnit.DAYS.between(startDate, endDate);
        }
        return 0;
    }

    public static LocalDate DATEFROMPARTS(int year, int month, int day) throws ManagerException {
        try {
            return LocalDate.of(year, month, day);
        } catch (DateTimeException e) {
            throw new ManagerException("Invalid date");
        }
    }

    public static String DATENAME(DatePart datePart, LocalDate localDate) {
        switch (datePart) {
            case year:
                return String.valueOf(localDate.getYear());
            case month:
                return String.valueOf(localDate.getMonthValue());
            case day:
                return String.valueOf(localDate.getDayOfMonth());
            case dayofyear:
                return String.valueOf(localDate.getDayOfYear());
            case weekday:
                return String.valueOf(localDate.getDayOfWeek());
            case week: {
                WeekFields weekFields = WeekFields.of(Locale.getDefault());
                return String.valueOf(localDate.get(weekFields.weekOfWeekBasedYear()));
            }
        }
        return null;
    }

    public static int DATEPART(DatePart datePart, LocalDate localDate) {
        switch (datePart) {
            case year:
                return localDate.getYear();
            case month:
                return localDate.getMonthValue();
            case day:
                return localDate.getDayOfMonth();
            case dayofyear:
                return localDate.getDayOfYear();
            case weekday:
                return localDate.getDayOfWeek().getValue();
            case week: {
                WeekFields weekFields = WeekFields.of(Locale.getDefault());
                return localDate.get(weekFields.weekOfWeekBasedYear());
            }
        }
        return Integer.MAX_VALUE;
    }

    public static int DAY(LocalDate localDate) {
        return localDate.getDayOfMonth();
    }

    public static int MONTH(LocalDate localDate) {
        return localDate.getMonthValue();
    }

    public static int YEAR(LocalDate localDate) {
        return localDate.getYear();
    }

    public static int ISDATE(String date) {
        try {
            LocalDate.parse(date);
            return 1;
        } catch (DateTimeException e) {
            return 0;
        }
    }
}