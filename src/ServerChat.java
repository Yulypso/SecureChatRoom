import java.net.*;
import java.io.*;

public class ServerChat {

    public ServerChat(final int port){

        try(ServerSocket listener = new ServerSocket(port)) {

            while (true){
                new Thread(new ServiceChat(listener.accept())).start();


                Socket socket = listener.accept();

                // pour envoyer un message aux clients
                PrintStream out = new PrintStream(socket.getOutputStream());

                // pour lire ce que le client nous envoie
                InputStreamReader inputStream = new InputStreamReader(socket.getInputStream());
                BufferedReader input = new BufferedReader(inputStream);

                String message = input.readLine();

                System.out.println(message);
                String verb = message.split(" ")[0];
                String res = message.split(" ")[1];

                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String argv[]) throws Exception
    {
        new ServerChat(1234);
    }
}