package secureApp.server.Models;

import java.io.PrintWriter;
import java.net.Socket;

public class Client {

    private boolean isAdmin;
    private PrintWriter out;
    private String username;
    private String password;
    private String RSAModulus = null;
    private String RSAExponent = null;
    private Socket socket;

    public Client(PrintWriter out, Socket socket){ // User
        this.out = out;
        this.isAdmin = false;
        this.socket = socket;
    }

    public Client(String username, String password){ // User
        this.username = username;
        this.password = password;
        this.isAdmin = false;
    }

    public Client(String username, String RSAExponent, String RSAModulus){ // User
        this.username = username;
        this.RSAExponent = RSAExponent;
        this.RSAModulus = RSAModulus;
        this.isAdmin = false;
    }

    public Client(PrintWriter out){ // Admin
        this.out = out;
        this.isAdmin = true;
        this.username = "ADMIN";
    }

    public String getRSAModulus() {
        return this.RSAModulus;
    }

    public String getRSAExponent() {
        return this.RSAExponent;
    }

    public void setRSAModulus(String RSAModulus) {
        this.RSAModulus = RSAModulus;
    }

    public void setRSAExponent(String RSAExponent) {
        this.RSAExponent = RSAExponent;
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
