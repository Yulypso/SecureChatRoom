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

    /* User commands */
    private static final String LOGOUT = "LOGOUT"; // /logout or /exit
    private static final String LIST = "LIST"; // /list
    private static final String PRIVMSG = "PRIVMSG"; // /msg username msg
    private static final String MSG = "MSG"; // msg

    /* Admin commands */
    private static final String KILLUSER = "KILLUSER"; // /kill username
    private static final String KILLALL = "KILLALL"; // /killall
    private static final String HALT = "HALT"; // /halt
    private static final String DELETEACCOUNT = "DELETEACCOUNT"; // /deleteAccount username
    private static final String ADDACCOUNT = "ADDACCOUNT"; // /addAccount username password
    private static final String LOADBDD = "LOADBDD"; // /loadBDD bdd.txt
    private static final String SAVEBDD = "SAVEBDD"; // /saveBDD bdd.txt

    private final int NBMAXUSERCONNECTED;

    public static final Map<String, PrintWriter> connectedClients = new HashMap<>();
    public static final List<Client> registeredClients = new LinkedList<>();

    public ServiceChat(Socket socket, int NBMAXUSERCONNECTED) throws IOException { // User
        this.socket = socket;
        this.NBMAXUSERCONNECTED = NBMAXUSERCONNECTED;
        this.in = new Scanner(socket.getInputStream());
        this.client = new Client(new PrintWriter(socket.getOutputStream(), true), socket);
    }

    public ServiceChat(int NBMAXUSERCONNECTED) { // Admin
        this.NBMAXUSERCONNECTED = NBMAXUSERCONNECTED;
        this.in = new Scanner(System.in);
        this.client = new Client(new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true));
    }

    private synchronized boolean userConnectedLimitReachedCheck() throws IOException {
        if (ServiceChat.connectedClients.size() >= NBMAXUSERCONNECTED){
            this.client.getOut().println("<SYSTEM> User connected limit reached");
            this.socket.close();
            return true;
        }
        return false;
    }

    private synchronized void broadcastMessage(String username, String msg, boolean system){
        final String x = system ? "<" + username + ">" + " " + msg : "[" + username + "]" + " " + msg;
        for(Map.Entry<String, PrintWriter> client : connectedClients.entrySet())
            client.getValue().println(x);
        System.out.println(x);
    }

    private synchronized boolean usernameExists(String username){
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

    private synchronized void register(String username) throws IOException {
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

        System.out.println("<SYSTEM> [REGISTER]: " + this.client.getUsername() + " has successfully registered");
        this.client.getOut().println("<SYSTEM> Registration Successful");
        this.socket.close();
    }

    private synchronized void login(String username) throws IOException {
        this.client.getOut().println("<SYSTEM> Connecting...");
        int isFound = -1;


        this.client.getOut().println("Enter password: ");
        String password = this.in.nextLine();

        for (Client client : registeredClients)
            if (client.getUsername().equals(username) && client.getPassword().equals(password))
                isFound = registeredClients.indexOf(client);

        if (isFound == -1) {
            this.client.getOut().println("<SYSTEM> Username or password is incorrect");
            this.client.getOut().close();
            this.socket.close();
            return;
        }

        if(!userConnectedLimitReachedCheck()) {
            if (connectedClients.containsKey(username)) {
                this.client.getOut().println("<SYSTEM> User already connected");
                this.client.getOut().close();
                this.socket.close();
                return;
            } else {
                registeredClients.get(isFound).setOut(this.client.getOut()); // update printwriter
                this.client = registeredClients.get(isFound);

                this.client.getOut().println("<SYSTEM> Connected as: " + this.client.getUsername());
                System.out.println("<SYSTEM> [LOGIN]: " + this.client.getUsername() + " is now connected");
                broadcastMessage("SYSTEM", this.client.getUsername() + " is now connected!", true);

                connectedClients.put(this.client.getUsername(), this.client.getOut());
                listClients();
            }
        }
    }

    private synchronized void logout(Client client) throws IOException {
        connectedClients.remove(client.getUsername());

        client.getOut().println("<SYSTEM> Disconnecting...");
        System.out.println("<SYSTEM> [LOGOUT]: " + client.getUsername() + " is now disconnected");
        broadcastMessage("SYSTEM", client.getUsername() + " is now disconnected!", true);
        client.getOut().close();
        client.getSocket().close();
    }

    private synchronized void listClients(){
        this.client.getOut().println("<SYSTEM> [LIST]: Connected users: ");
        this.client.getOut().println("-----------------------------");
        for(Map.Entry<String, PrintWriter> client : connectedClients.entrySet()) {
            String key = client.getKey();
            this.client.getOut().print(key + " ");
        }
        this.client.getOut().println("\n-----------------------------");
    }

    private synchronized void privateMessage(String raw) {
        String[] splitRaw = raw.split(" ");

        for(Map.Entry<String, PrintWriter> client : connectedClients.entrySet())
            if (splitRaw[1].equals(client.getKey()))
                client.getValue().println("[From: " + this.client.getUsername() + "]" + " " + splitRaw[2]);
    }

    private void saveBdd() {
    }

    private void loadBdd() {
    }

    private void addAccount() {
    }

    private void deleteAccount() {
    }

    private void haltServer() {
    }

    private synchronized void killAll() throws IOException {
        int i = 0;
        for (String username: connectedClients.keySet().stream().toList()) {
            System.out.println("<SYSTEM> [KILLALL]: Disconnecting " + username + " (" + ++i + "/" + connectedClients.size() + ")");
            Optional<Client> c = registeredClients.stream().filter(cl -> cl.getUsername().equals(username)).findFirst();
            if (c.isPresent()) {
                System.out.println(c.get().getUsername());
                logout(c.get());
            } else {System.out.println("not found");}
        }
        System.out.println("<SYSTEM> [KILLALL]: All users are logged out");
    }

    private synchronized void killUser(String raw) throws IOException {
        String[] splitRaw = raw.split(" ");

        for(Map.Entry<String, PrintWriter> client : connectedClients.entrySet())
            if (splitRaw[1].equals(client.getKey())) {
                System.out.println("<SYSTEM> [KILLUSER]: Disconnecting " + client.getKey());
                Optional<Client> c = registeredClients.stream().filter(cl -> cl.getUsername().equals(client.getKey())).findFirst();
                if(c.isPresent())
                    logout(c.get());
                return;
            }
        System.out.println("<SYSTEM> [KILLUSER]: User " + splitRaw[1] + " not found");
    }

    private String commandParser(String text){
        switch (text.split(" ")[0].toLowerCase(Locale.ROOT)) {
            case "/exit", "/logout" -> {return LOGOUT;}
            case "/list" -> {return LIST;}
            case "/msg" -> {return PRIVMSG;}
            case "/kill" -> {return this.client.isAdmin() ? KILLUSER : MSG;}
            case "/killall" ->{return this.client.isAdmin() ? KILLALL : MSG;}
            case "/halt" ->{return this.client.isAdmin() ? HALT : MSG;}
            case "/deleteaccount"->{return this.client.isAdmin() ? DELETEACCOUNT : MSG;}
            case "/addaccount"->{return this.client.isAdmin() ? ADDACCOUNT : MSG;}
            case "/loadbdd"->{return this.client.isAdmin() ? LOADBDD : MSG;}
            case "/savebdd"->{return this.client.isAdmin() ? SAVEBDD : MSG;}
            default -> {return MSG;}
        }
    }

    @Override
    public void run() {

        try {
            if (!this.client.isAdmin())
                authentication();

            while (this.in.hasNextLine()) {
                String raw = this.in.nextLine().trim();

                switch (commandParser(raw)) {
                    case LOGOUT -> {
                        if (!this.client.isAdmin()) {
                            logout(this.client);
                            return;
                        }
                    }
                    case LIST -> listClients();
                    case PRIVMSG -> privateMessage(raw);
                    case MSG -> broadcastMessage(this.client.getUsername(), raw, false);
                    case KILLUSER -> killUser(raw);
                    case KILLALL -> killAll();
                    case HALT -> haltServer();
                    case DELETEACCOUNT -> deleteAccount();
                    case ADDACCOUNT -> addAccount();
                    case LOADBDD -> loadBdd();
                    case SAVEBDD -> saveBdd();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

