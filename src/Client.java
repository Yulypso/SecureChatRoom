import java.io.PrintWriter;

public class Client {

    private PrintWriter out;
    private String username;
    private String password;

    public Client(PrintWriter out){
        this.out = out;
    }

    public PrintWriter getOut() {
        return out;
    }

    public void setOut(PrintWriter out) {
        this.out = out;
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
}
