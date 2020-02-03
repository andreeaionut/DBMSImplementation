public class HoneyData {
    private int user_id;
    private int password_id;

    public HoneyData(int user_id, int password_id) {
        this.user_id = user_id;
        this.password_id = password_id;
    }

    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

    public int getPassword_id() {
        return password_id;
    }

    public void setPassword_id(int password_id) {
        this.password_id = password_id;
    }
}
