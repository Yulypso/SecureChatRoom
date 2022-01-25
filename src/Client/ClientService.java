package Client;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Locale;
import java.util.Scanner;

public class ClientService extends Thread {

    private Socket socket;

    private Scanner inConsole, inNetwork;
    private PrintWriter outConsole, outNetwork;

    // Client command
    private static final String MSG = "MSG"; // msg
    private static final String SENDFILE = "SENDFILE"; // sendFile login filename
    private static final String LOGOUT = "LOGOUT"; // /logout or /exit

    // Server command
    private static final String CONNECTED = "CONNECTED";
    private static final String ALREADYCONNECTED = "ALREADYCONNECTED";
    private static final String REGISTERED = "REGISTERED";
    private static final String ERR_REGISTERED = "ERR_REGISTERED";
    private static final String DISCONNECTED = "DISCONNECTED";
    private static final String USERLIMITREACHED = "USERLIMITREACHED";

    private boolean isClientConnected = false;

    public ClientService(String host, int port) throws IOException {
        initStream(host, port);
        //TODO: Authentication if log
        start();
        listenConsole();
    }

    /* Server init & close */
    public void initStream(String host, int port) throws IOException {
        this.inConsole = new Scanner(System.in);
        this.outConsole = new PrintWriter(System.out);

        try {
            this.socket = new Socket(host, port);
            this.inNetwork = new Scanner(socket.getInputStream());
            this.outNetwork = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeNetwork() throws IOException {
        this.outNetwork.close();
        this.socket.close();
        System.exit(0);
    }

    private void closeConsole() throws IOException {
        this.outConsole.close();
    }
    /*********/

    /* Tools */
    private void displayConsole(String raw) {
        this.outConsole.println(raw);
        this.outConsole.flush();
    }

    private void sendServer(String raw) {
        this.outNetwork.println(raw);
        this.outNetwork.flush();
    }
    /*********/

    /* Features */
    private void sendFile() {
        System.out.println("sending file ...");
    }

    private void logout() throws IOException {
        sendServer("/logout");
    }
    /*********/

    private String serverParser(String text){
        switch (text) {
            case "<SYSTEM> Connected as:" -> {return CONNECTED;}
            case "<SYSTEM> Registration Successful" -> {return REGISTERED;}
            case "<SYSTEM> Disconnecting..." -> {return DISCONNECTED;}
            case "<SYSTEM> Username or password is incorrect" -> {return ERR_REGISTERED;}
            case "<SYSTEM> User connected limit reached" -> {return USERLIMITREACHED;}
            case "<SYSTEM> User already connected" -> {return ALREADYCONNECTED;}
            default -> {return MSG;}
        }
    }

    private void listenNetwork() throws IOException {
        while(this.inNetwork.hasNextLine()){
            String raw = this.inNetwork.nextLine().trim();
            displayConsole(raw);
            switch(serverParser(raw)) {
                case CONNECTED -> this.isClientConnected = true;
                case REGISTERED, ERR_REGISTERED, DISCONNECTED, USERLIMITREACHED, ALREADYCONNECTED -> {closeConsole(); closeNetwork();}
            }
        }
    }

    private String commandParser(String text){
        switch (text.split(" ")[0].toLowerCase(Locale.ROOT)) {
            case "/sendfile" -> {return isClientConnected ? SENDFILE : MSG;}
            case "/exit", "/logout" -> {return isClientConnected ? LOGOUT : MSG;}
            default -> {return MSG;}
        }
    }

    private void listenConsole() throws IOException {
        while(this.inConsole.hasNextLine()){
            String raw = this.inConsole.nextLine().trim();
            switch (commandParser(raw)) {
                case SENDFILE -> sendFile();
                case MSG -> sendServer(raw);
                case LOGOUT -> logout();
            }
        }
    }

    @Override
    public void run() {
        try {
            listenNetwork();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
