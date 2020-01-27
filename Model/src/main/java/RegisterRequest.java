import java.io.Serializable;

public class RegisterRequest implements Serializable {
    private String email;
    private String username;
    private String password;
    private String accessCode;

    public RegisterRequest(String email, String username, String password, String accessCode) {
        this.email = email;
        this.username = username;
        this.password = password;
        this.accessCode = accessCode;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAccessCode() {
        return accessCode;
    }

    public void setAccessCode(String accessCode) {
        this.accessCode = accessCode;
    }
}
