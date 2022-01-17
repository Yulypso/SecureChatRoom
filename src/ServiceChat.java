import javax.naming.InvalidNameException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;

public class ServiceChat implements Runnable {

    private final Socket socket;
    private Scanner in;
    private PrintWriter out;
    private String username;

    private Client client;

    private static final String LOGOUT = "LOGOUT";
    private static final String LIST = "LIST";
    private static final String PRIVMSG = "PRIVMSG";
    private static final String MSG = "MSG";


    public static final Map<String, PrintWriter> connectedClients = new HashMap<>();
    public static final List<Client> registeredClients = new LinkedList<>();

    public ServiceChat(Socket socket) {
        this.socket = socket;
    }

    private void initialisation() throws IOException {
        this.in = new Scanner(socket.getInputStream());
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.client = new Client(this.out);
    }

    private void broadcastMessage(String username, String msg, boolean system){
        if (system)
            username = "<" + username + ">";
        else
            username = "[" + username + "]";

        for(Map.Entry<String, PrintWriter> client : connectedClients.entrySet()) {
            client.getValue().println(username + " " + msg);
        }
    }

    private boolean usernameExists(String username){
        boolean exist = false;

        for (Client client: registeredClients)
            if (client.getUsername().equals(username)) {
                exist = true;
                break;
            }
        return exist;
    }

    private void register(String username) {
        this.out.println("Register...");

        this.client.setUsername(username);

        String password = "";
        String confirmPassword = " ";
        while(!password.equals(confirmPassword)){
            this.out.println("Enter password: ");
            password = this.in.nextLine().trim();
            this.out.println("Confirm Password: ");
            confirmPassword = this.in.nextLine().trim();
        }
        this.client.setPassword(password);

        registeredClients.add(this.client);
    }

    private void login(String username){
        if (!registeredClients.contains(this.client)) { // Must connect
            this.out.println("Connecting...");

            boolean isLogged = false;
            while (!isLogged) {
                this.out.println("Enter password: ");
                String password = this.in.nextLine();

                for (Client client : registeredClients) {
                    if (client.getUsername().equals(username) && client.getPassword().equals(password)) {
                        client.setOut(this.out); // update printwriter
                        this.client = client;
                        isLogged = true;
                    }
                }

                if (!isLogged)
                    this.out.println("[Error]: username or password incorrect. Try again. :)");
            }
        }

        this.out.println("Connected as: " + this.client.getUsername());
        System.out.println("<SYSTEM> " + this.client.getUsername() + " is connected!");
        this.out.println("-----------------------------");
        broadcastMessage("SYSTEM", this.client.getUsername() + " is connected!", true);

        connectedClients.put(this.client.getUsername(), this.client.getOut());
    }

    private void logout(){
        connectedClients.remove(this.client.getUsername());

        this.out.println("Disconnecting...");
        System.out.println("<SYSTEM> " + this.client.getUsername() + " is disconnected!");
        this.out.println("-----------------------------");
        broadcastMessage("SYSTEM", this.client.getUsername() + " is disconnected!", true);
    }

    private void listClients(){
        this.out.println("Connected users: ");
        this.out.println("-----------------------------");
        for(Map.Entry<String, PrintWriter> client : connectedClients.entrySet()) {
            String key = client.getKey();
            this.out.print(key + " ");
        }
        this.out.println();
        this.out.println("-----------------------------");
    }

    private void privateMessage(String raw) {
        String[] splitRaw = raw.split(" ");

        System.out.println("debug");
        System.out.println(splitRaw[0]);
        System.out.println(splitRaw[1]);
        System.out.println(splitRaw[2]);

        for(Map.Entry<String, PrintWriter> client : connectedClients.entrySet()) {
            if (splitRaw[1].equals(client.getKey()))
                client.getValue().println("[From: " + this.client.getUsername() + "]" + " " + splitRaw[2]);
        }
    }

    private String commandParser(String text){

        switch (text.split(" ")[0]) {
            case "/exit", "/logout" -> {
                return LOGOUT;
            }
            case "/list" -> {
                return LIST;
            }
            case "/msg" -> {
                return PRIVMSG;
            }
            default -> {
                return MSG;
            }
        }
    }

    @Override
    public void run() {
        try {
            initialisation();

            this.out.println("Enter your username: ");
            username = this.in.nextLine().trim();

            if (!usernameExists(username))
                register(username);
            login(username);

            while(true) {
                if (this.in.hasNextLine()) {
                    String raw = this.in.nextLine().trim();
                    System.out.println(raw);
                    String command = commandParser(raw);

                    switch (command){
                        case LOGOUT -> {
                            logout();
                            this.socket.close();
                            return;
                        }
                        case LIST -> listClients();
                        case PRIVMSG -> privateMessage(raw);
                        case MSG -> broadcastMessage(this.client.getUsername(), raw, false);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

