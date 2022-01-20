import java.net.*;
import java.io.*;

public class ServerChat {

    private final static int NBMAXUSERCONNECTED = 3;

    public ServerChat(final int port){

        try(ServerSocket listener = new ServerSocket(port)) {
            while (true) {
                Socket s = listener.accept();
                if (ServiceChat.connectedClients.size() < NBMAXUSERCONNECTED)
                    new Thread(new ServiceChat(s)).start();
                else
                    s.close();
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