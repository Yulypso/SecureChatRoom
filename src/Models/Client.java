package Models;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    private boolean isAdmin;
    private PrintWriter out;
    private String username;
    private String password;
    private Socket socket;

    public Client(PrintWriter out, Socket socket){ // User
        this.out = out;
        this.isAdmin = false;
        this.socket = socket;
    }

    public Client(PrintWriter out){ // Admin
        this.out = out;
        this.isAdmin = true;
        this.username = "ADMIN";
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

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }
}
