import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;

public class ServiceChat implements Runnable {

    private final Socket socket;
    private Scanner in;
    private PrintWriter out;
    private String username;

    private static final String LOGOUT = "LOGOUT";
    private static final String LIST = "LIST";
    private static final String PRIVMSG = "PRIVMSG";
    private static final String MSG = "MSG";


    public static final Map<String, PrintWriter> clients = new HashMap<>();

    public ServiceChat(Socket socket) {
        this.socket = socket;
    }

    private void initialisation() throws IOException {
        this.in = new Scanner(socket.getInputStream());
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    private void broadcastMessage(String username, String msg, boolean system){
        if (system)
            username = "<" + username + ">";
        else
            username = "[" + username + "]";

        for(Map.Entry<String, PrintWriter> client : clients.entrySet()) {
            client.getValue().println(username + " " + msg);
        }
    }

    private void register(){
        this.out.println("Welcome!");
        this.out.print("Connected users: ");
        listClients();

        this.out.println("Welcome! Enter your Username: ");
        this.username = this.in.nextLine();
    }

    private void login(){
        this.out.println("Connected as: " + this.username);
        System.out.println("<SYSTEM> " + this.username + " is connected!");
        this.out.println("-----------------------------");
        broadcastMessage("SYSTEM", this.username + " is connected!", true);

        clients.put(this.username, this.out);
    }

    private void logout(){
        clients.remove(this.username);

        this.out.println("Disconnecting...");
        System.out.println("<SYSTEM> " + this.username + " is disconnected!");
        this.out.println("-----------------------------");
        broadcastMessage("SYSTEM", this.username + " is disconnected!", true);
    }

    private void listClients(){
        this.out.println("-----------------------------");
        for(Map.Entry<String, PrintWriter> client : clients.entrySet()) {
            String key = client.getKey();
            this.out.print(key + " ");
        }
        this.out.println();
        this.out.println("-----------------------------");
    }

    private String commandParser(String text){
        switch (text.split(" ")[0]){
            case "/exit", "/logout" -> {return LOGOUT;}
            case "/list" -> {return LIST;}
            case "/msg" -> {return PRIVMSG;}
            default -> {return MSG;}
        }
    }

    @Override
    public void run() {
        try {
            initialisation();
            register();
            login();

            while(true) {
                if (this.in.hasNextLine()) {
                    String raw = this.in.nextLine();
                    String command = commandParser(raw);

                    switch (command){
                        case LOGOUT -> {
                            logout();
                            this.socket.close();
                            return;
                        }
                        case LIST -> listClients();
                        case PRIVMSG -> {

                        }
                        case MSG -> broadcastMessage(this.username, raw, false);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

