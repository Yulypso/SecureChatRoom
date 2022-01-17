import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ServiceChat implements Runnable{

    private Socket socket;
    private Scanner in;
    private PrintWriter out;

    public ServiceChat(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            this.in = new Scanner(socket.getInputStream());
            this.out = new PrintWriter(socket.getOutputStream(), true);

            if (this.in.hasNextLine()) {
                String text = this.in.nextLine();
                this.out.println("text received: " + text);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
