package Models;

import java.io.PrintWriter;
import java.util.Scanner;

public class Client {

    private boolean isAdmin;
    private PrintWriter out;
    private String username;
    private String password;

    public Client(PrintWriter out, boolean isAdmin){
        this.out = out;
        this.isAdmin = isAdmin;

        if(isAdmin){
            this.username = "ADMIN";
        }
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

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }
}
