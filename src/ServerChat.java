import java.net.*;
import java.io.*;
import java.util.HashSet;
import java.util.Set;

public class ServerChat {

    public ServerChat(final int port){

        try(ServerSocket listener = new ServerSocket(port)) {
            while (true) new Thread(new ServiceChat(listener.accept())).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] argv)
    {
        new ServerChat(1234);
    }
}