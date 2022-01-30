package Client;

import java.io.*;
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
    private static final String SENDFILESTOP = "SENDFILESTOP";
    private static final String FILETRANSFERMODEON = "FILETRANSFERMODEON";
    private static final String FILETRANSFERMODEOFF = "FILETRANSFERMODEOFF";
    private static final String ISUSERCONNECTED = "ISUSERCONNECTED";
    private static final String RECEIVERISCONNECTED = "USERISCONNECTED";
    private static final String RECEIVERISNOTCONNECTED = "USERISNOTCONNECTED";

    private boolean isClientConnected = false;

    private boolean fileTransferMode = false;
    private boolean isReceiverConnected = false;
    private boolean checkReceiverState = false;

    private FileOutputStream fout = null;

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

    private static byte[] shortToByteArray(short s) {
        return new byte[] { (byte) ((s & (short) 0xff00) >> 8), (byte) (s & (short) 0x00ff) };
    }
    /*********/

    private void isUserconnected(String raw){
        String[] splitRaw = raw.split(" ");
        sendServer("<SYSTEM> [SENDFILE]: " + ISUSERCONNECTED + " " + splitRaw[1].trim() + " " + splitRaw[2].trim());
    }

    /* Features */
    private void sendFile(String raw) throws IOException {
        if(!checkReceiverState){
            isUserconnected(raw);
            this.checkReceiverState = true;
        } else {
            if (this.isReceiverConnected) {
                String[] splitRaw = raw.split(" ");
                raw = "/sendfile " + splitRaw[5] + " " + splitRaw[7];
                splitRaw = raw.split(" ");

                if (splitRaw.length == 3) {

                    File f = new File("./src/Client/Files/" + splitRaw[2].trim());
                    if (!f.exists()) {
                        displayConsole("<SYSTEM> [SENDFILE]: File doesn't exist");
                    } else {
                        sendServer("/sendfile " + splitRaw[1] + " " + splitRaw[2]);

                        FileInputStream fin = new FileInputStream(f);
                        int by = 0;
                        while ((by = fin.read()) != -1) {
                            sendServer(String.valueOf(by));
                        }

                        sendServer("<SYSTEM> [SENDFILE]: " + SENDFILESTOP);
                    }

                    this.checkReceiverState = false;
                    this.isReceiverConnected = false;
                } else
                    displayConsole("<SYSTEM> [SENDFILE]: Bad arguments");
            }
        }
    }

    private synchronized void retrieveFile(String raw) throws IOException {
        String[] splitRaw = raw.split(" ");
        if(raw.startsWith("<SYSTEM> [SENDFILE]: SENDFILESTART")){
            this.fout = new FileOutputStream("./src/Client/Files/retrieved_" + splitRaw[4]);
        } else if (raw.startsWith("<SYSTEM> [SENDFILE]: SENDFILESTOP")) {
            this.fileTransferMode = false;
            this.fout.close();
        } else {
            this.fout.write(Byte.parseByte(String.valueOf(shortToByteArray(Short.parseShort(raw))[1]), 10));
        }
    }

    private void logout() {
        sendServer("/logout");
    }/*********/

    private String serverParser(String text){
        if(text.startsWith("<SYSTEM> Connected as:"))
            return CONNECTED;
        else if(text.startsWith("<SYSTEM> Registration Successful"))
            return REGISTERED;
        else if(text.startsWith("<SYSTEM> Disconnecting..."))
            return DISCONNECTED;
        else if(text.startsWith("<SYSTEM> Username or password is incorrect"))
            return ERR_REGISTERED;
        else if(text.startsWith("<SYSTEM> User connected limit reached"))
            return USERLIMITREACHED;
        else if(text.startsWith("<SYSTEM> User already connected"))
            return ALREADYCONNECTED;
        else if(text.startsWith("<SYSTEM> [SENDFILE]: SENDFILESTART"))
            return FILETRANSFERMODEON;
        else if(text.startsWith("<SYSTEM> [SENDFILE]: SENDFILESTOP"))
            return FILETRANSFERMODEOFF;
        else if(text.startsWith("<SYSTEM> [SENDFILE]: User is connected")) // for sendfile receiver
            return RECEIVERISCONNECTED;
        else if(text.startsWith("<SYSTEM> [SENDFILE]: User is not connected")) //for sendfile receiver
            return RECEIVERISNOTCONNECTED;
        else
            return MSG;
    }

    private void listenNetwork() throws IOException {
        while(this.inNetwork.hasNextLine()) {
            String raw = this.inNetwork.nextLine().trim();
            if (!this.fileTransferMode)
                displayConsole(raw);
            switch (serverParser(raw)) {
                case CONNECTED -> this.isClientConnected = true;
                case FILETRANSFERMODEON -> this.fileTransferMode = true;
                case RECEIVERISCONNECTED -> {
                    this.isReceiverConnected = true;
                    sendFile(raw);
                }
                case RECEIVERISNOTCONNECTED -> {
                    this.isReceiverConnected = false;
                    this.checkReceiverState = false;
                }
                case REGISTERED, ERR_REGISTERED, DISCONNECTED, USERLIMITREACHED, ALREADYCONNECTED -> {
                    closeConsole();
                    closeNetwork();
                }
            }
            if(this.fileTransferMode){
                retrieveFile(raw);
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
                case SENDFILE -> sendFile(raw);
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
