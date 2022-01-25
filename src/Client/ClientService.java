package Client;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ClientService extends Thread {

    private Scanner inConsole, inNetwork;
    private PrintWriter outConsole, outNetwork;

    public ClientService(String host, int port) throws IOException {
        initStream(host, port);
        start();
        listenConsole();
    }

    public void initStream(String host, int port) throws IOException {
        this.inConsole = new Scanner(System.in);
        this.outConsole = new PrintWriter(System.out);

        Socket socket = new Socket(host, port);
        this.inNetwork = new Scanner(socket.getInputStream());
        this.outNetwork = new PrintWriter(socket.getOutputStream(), true);
    }

    private void listenNetwork(){
        while(this.inNetwork.hasNextLine()){
            String raw = this.inNetwork.nextLine().trim();
            this.outConsole.println(raw);
            this.outConsole.flush();
        }
    }

    private void listenConsole(){
        while(this.inConsole.hasNextLine()){
            String raw = this.inConsole.nextLine().trim();
            this.outNetwork.println(raw);
            this.outNetwork.flush();
        }
    }

    @Override
    public void run() {
        listenNetwork();
    }
}
