import java.net.*;
import java.io.*;

public class ServerChat {

    private final static int NBMAXUSERCONNECTED = 2;
    protected static boolean runServer = true;

    public ServerChat(final int port){
        try(ServerSocket listener = new ServerSocket(port)) {
            new Thread(new ServiceChat(NBMAXUSERCONNECTED)).start();
            while (runServer) new Thread(new ServiceChat(listener.accept(), NBMAXUSERCONNECTED)).start();
            System.out.println("Server closed");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] argv){
        new ServerChat(1234);
    }
}