import java.net.*;
import java.io.*;

public class ServerChat {

    private final static int NBMAXUSERCONNECTED = 2;

    public ServerChat(final int port){

        try(ServerSocket listener = new ServerSocket(port)) {
            new Thread(new ServiceChat(NBMAXUSERCONNECTED)).start();
            while (true) new Thread(new ServiceChat(listener.accept(), NBMAXUSERCONNECTED)).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] argv)
    {
        new ServerChat(1234);
    }
}