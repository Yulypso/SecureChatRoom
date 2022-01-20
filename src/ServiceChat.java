import Models.Client;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ServiceChat implements Runnable {

    private Socket socket;

    private Client client;
    private final Scanner in;

    private static final String LOGOUT = "LOGOUT";
    private static final String LIST = "LIST";
    private static final String PRIVMSG = "PRIVMSG";
    private static final String MSG = "MSG";

    private final int NBMAXUSERCONNECTED;


    public static final Map<String, PrintWriter> connectedClients = new HashMap<>();
    public static final List<Client> registeredClients = new LinkedList<>();

    public ServiceChat(Socket socket, int NBMAXUSERCONNECTED) throws IOException { // User
        this.socket = socket;
        this.NBMAXUSERCONNECTED = NBMAXUSERCONNECTED;
        this.in = new Scanner(socket.getInputStream());
        this.client = new Client(new PrintWriter(socket.getOutputStream(), true), false);
    }

    public ServiceChat(int NBMAXUSERCONNECTED) { // Admin
        this.NBMAXUSERCONNECTED = NBMAXUSERCONNECTED;
        this.in = new Scanner(System.in);
        this.client = new Client(new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true), true);
    }

    private boolean userConnectedLimitReachedCheck() throws IOException {
        if (ServiceChat.connectedClients.size() >= NBMAXUSERCONNECTED){
            this.client.getOut().println("<SYSTEM> User connected limit reached");
            this.socket.close();
            return true;
        }
        return false;
    }

    private synchronized void broadcastMessage(String username, String msg, boolean system){
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

    private void authentication() throws IOException {
        this.client.getOut().println("<SYSTEM> Welcome!");
        this.client.getOut().println("<SYSTEM> Enter your username");
        String username = this.in.nextLine().trim();

        if (!usernameExists(username)) {
            register(username);
        } else {
            login(username);
        }
    }

    private void register(String username) throws IOException {
        this.client.getOut().println("<SYSTEM> Register");

        this.client.setUsername(username);

        String password = "";
        String confirmPassword = " ";
        while(!password.equals(confirmPassword)){
            this.client.getOut().println("Enter password: ");
            password = this.in.nextLine().trim();
            this.client.getOut().println("Confirm Password: ");
            confirmPassword = this.in.nextLine().trim();
        }
        this.client.setPassword(password);

        registeredClients.add(this.client);

        System.out.println("<SYSTEM> " + this.client.getUsername() + " has successfully registered");
        this.client.getOut().println("<SYSTEM> Registration Successful");
        this.socket.close();
    }

    private void login(String username) throws IOException {

        this.client.getOut().println("<SYSTEM> Connecting...");
        boolean isLogged = false;

        while (!isLogged) {
            this.client.getOut().println("Enter password: ");
            String password = this.in.nextLine();

            for (Client client : registeredClients) {
                if (client.getUsername().equals(username) && client.getPassword().equals(password)) {
                    client.setOut(this.client.getOut()); // update printwriter
                    this.client = client;
                    isLogged = true;
                }
            }

            if (!isLogged)
                this.client.getOut().println("<SYSTEM> Username or password is incorrect");
        }

        if(!userConnectedLimitReachedCheck()) {
            if (connectedClients.containsKey(username) && !userConnectedLimitReachedCheck()) {
                this.client.getOut().println("<SYSTEM> User already connected");
                this.socket.close();
            }

            this.client.getOut().println("<SYSTEM> Connected as: " + this.client.getUsername());
            System.out.println("<SYSTEM> " + this.client.getUsername() + " is now connected!");
            broadcastMessage("SYSTEM", this.client.getUsername() + " is now connected!", true);

            connectedClients.put(this.client.getUsername(), this.client.getOut());

            listClients();
        }
    }

    private void logout() throws IOException {
        connectedClients.remove(this.client.getUsername());

        this.client.getOut().println("<SYSTEM> Disconnecting...");
        System.out.println("<SYSTEM> " + this.client.getUsername() + " is now disconnected!");
        broadcastMessage("SYSTEM", this.client.getUsername() + " is now disconnected!", true);
        this.socket.close();
    }

    private synchronized void listClients(){
        this.client.getOut().println("<SYSTEM> Connected users: ");
        this.client.getOut().println("-----------------------------");
        for(Map.Entry<String, PrintWriter> client : connectedClients.entrySet()) {
            String key = client.getKey();
            this.client.getOut().print(key + " ");
        }
        this.client.getOut().println();
        this.client.getOut().println("-----------------------------");
    }

    private synchronized void privateMessage(String raw) {
        String[] splitRaw = raw.split(" ");
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
            if (!this.client.isAdmin())
                authentication();

            while (true) {
                if (this.in.hasNextLine()) {
                    String raw = this.in.nextLine().trim();
                    System.out.println(raw);
                    String command = commandParser(raw);

                    switch (command) {
                        case LOGOUT -> {
                            if (!this.client.isAdmin())
                                logout();
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

