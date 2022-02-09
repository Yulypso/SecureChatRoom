package Client;

import java.io.IOException;

public class ClientChat {

    public ClientChat(String host, int port) throws IOException {
       new ClientService(host, port);
    }

    public static void main(String[] args) throws IOException {
        new ClientChat("localhost", 1234);
    }
}
