import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;

public class ServiceChat implements Runnable {

    private Socket socket;
    private Scanner in;
    private PrintWriter out;
    private String username;

    private static final Map<String, PrintWriter> printWriters = new HashMap<>();

    public ServiceChat(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            this.in = new Scanner(socket.getInputStream());
            this.out = new PrintWriter(socket.getOutputStream(), true);

            this.out.println("Enter your Username: ");
            this.username = this.in.nextLine();

            this.out.println("Connected as: " + this.username);
            this.out.println("-----------------------------");

            printWriters.put(username, this.out);

            while(true) {
                if (this.in.hasNextLine()) {
                    String text = this.in.nextLine();

                    for(Map.Entry<String, PrintWriter> pw : printWriters.entrySet()) {
                        System.out.println("[" + this.username + "] " + text);
                        pw.getValue().println("[" + this.username + "] " + text);
                    }

                    if (text.equals("exit")) {
                        this.socket.close();
                        return;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

