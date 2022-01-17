import java.net.*;
import java.io.*;

public class ServerChat {

    private final static int NBMAXUSER = 3;

    public ServerChat(final int port){

        try(ServerSocket listener = new ServerSocket(port)) {
            while (true) {
                if (ServiceChat.clients.size() < NBMAXUSER)
                    new Thread(new ServiceChat(listener.accept())).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] argv)
    {
        new ServerChat(1234);
    }
}