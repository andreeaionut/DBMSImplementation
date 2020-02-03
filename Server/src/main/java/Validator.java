import java.util.regex.Pattern;

public class Validator {

    public static boolean validatePassword(String password){
        if(password.length()<6){
            return false;
        }
        if(!Pattern.compile( "[0-9]" ).matcher(password).find()){
            return false;
        }
        return Pattern.compile( "[A-Z]" ).matcher(password).find();
    }
}
