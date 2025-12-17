import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommonCharacterBypasses {
    public static void main(String[] args) {

        // String u = "'"; // m.matches(): true
        // String u = "test'input'here"; // m.matches(): true
        // String u = "test'inputhere"; // m.matches(): false
        String u = "test'--"; // m.matches(): false

        // Define a regular expression pattern to detect single quotes or suspicious input like SQL injection attempts
        Pattern p = Pattern.compile("'|(.*'.*'.*)");
        
        // Match the input username 'u' against the pattern
        Matcher m = p.matcher(u);

        // !!! m.matches(): Just true if the whole string is single or at least two single quotes !!!
        System.err.println("m.matches(): " + m.matches());
    }
}
