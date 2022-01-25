package Server;

import Server.Models.Client;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.*;
import java.util.stream.Collectors;

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
    public static List<Client> registeredClients = new LinkedList<>();

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
        ServerChat.logger.log(Level.INFO, x);
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

    private synchronized boolean authentication() throws IOException {
        this.client.getOut().println("<SYSTEM> Welcome!");
        this.client.getOut().println("<SYSTEM> Enter your username");
        String username = this.in.nextLine().trim();

        if(!username.toLowerCase(Locale.ROOT).equals("admin")) {
            if (!usernameExists(username)) {
                register(username);
                return false; // false = register
            } else {
                login(username);
                return true;
            }

        } else {
            this.client.setUsername("HACKING DETECTED");
            logout(this.client);
            return false;
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

        ServerChat.logger.log(Level.INFO, "<SYSTEM> [REGISTER]: Registering " + this.client.getUsername());
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
            registeredClients.get(isFound).setOut(this.client.getOut()); // update printwriter
            registeredClients.get(isFound).setSocket(this.client.getSocket()); // update socket
            this.client = registeredClients.get(isFound);

            if (connectedClients.containsKey(username)) {
                this.client.getOut().println("<SYSTEM> User already connected");
                ServerChat.logger.log(Level.INFO, "<SYSTEM> [LOGIN]: User " + this.client.getUsername() + " is already connected");
                this.client.getOut().close();
                this.socket.close();
            } else {
                this.client.getOut().println("<SYSTEM> Connected as: " + this.client.getUsername());
                ServerChat.logger.log(Level.INFO, "<SYSTEM> [LOGIN]: Connecting " + this.client.getUsername());
                broadcastMessage("SYSTEM", this.client.getUsername() + " is now connected!", true);

                connectedClients.put(this.client.getUsername(), this.client.getOut());
                listClients();
            }
        }
    }

    private synchronized void logout(Client client) throws IOException {
        connectedClients.remove(client.getUsername());

        client.getOut().println("<SYSTEM> Disconnecting...");
        ServerChat.logger.log(Level.INFO, "<SYSTEM> [LOGOUT]: Disconnecting " + client.getUsername());
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
        ServerChat.logger.log(Level.INFO, "<SYSTEM> [LIST] [" + this.client.getUsername() + "]");
    }

    private synchronized void privateMessage(String raw) {
        String[] splitRaw = raw.split(" ");

        if(splitRaw.length == 3 && !splitRaw[1].equals(this.client.getUsername())) {
            for (Client rc: registeredClients) {
                if (splitRaw[1].equals(rc.getUsername())) {
                    for (Map.Entry<String, PrintWriter> client : connectedClients.entrySet()) {
                        if (rc.getUsername().equals(client.getKey())) {
                            client.getValue().println("[From: " + this.client.getUsername() + "]" + " " + splitRaw[2]);
                            ServerChat.logger.log(Level.INFO, "<SYSTEM> [PRIVMSG] [" + this.client.getUsername() + " -> " + client.getKey() + "]" + " " + splitRaw[2]);
                            return;
                        }
                    }
                    this.client.getOut().println("<SYSTEM> [PRIVMSG]: Private message cancelled, " + rc.getUsername() + " is not connected");
                    return;
                }
            }
        }
        this.client.getOut().println("<SYSTEM> [PRIVMSG]: Private message cancelled");
    }

    private synchronized void saveBdd(String raw) {
        String bddFile = "./src/Server/Databases/" + raw.split(" ")[1];
        try {
            new File(bddFile).delete();

            BufferedWriter writer = new BufferedWriter(new FileWriter(bddFile));
            int cpt = 0;
            for (Client client: registeredClients) {
                ++cpt;
                writer.write(client.getUsername() + ":" + client.getPassword() + "\n");
            }
            writer.close();
            ServerChat.logger.log(Level.INFO, "<SYSTEM> [SAVEBDD]: Database saved, saved " + cpt + " new users");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void loadBdd(String raw) {
        String bddFile = "./src/Server/Databases/" + raw.split(" ")[1];
        try {
            BufferedReader reader = new BufferedReader(new FileReader(bddFile));
            String line; int cpt = 0;
            while ((line = reader.readLine()) != null) {
                ++cpt;
                String[] splitLine = line.split(":");
                registeredClients.add(new Client(splitLine[0], splitLine[1]));
            }
            registeredClients = registeredClients.stream().distinct().collect(Collectors.toList());
            reader.close();
            ServerChat.logger.log(Level.INFO, "<SYSTEM> [LOADBDD]: Database loaded, registered " + cpt + " new users");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void addAccount(String raw) {
        String[] splitRaw = raw.split(" ");
        Optional<Client> c = registeredClients.stream().filter(cl -> cl.getUsername().equals(splitRaw[1])).findFirst();
        if (c.isPresent())
            ServerChat.logger.log(Level.INFO, "<SYSTEM> [ADDACCOUNT]: Account already exists, user " + splitRaw[1]);
        else {
            registeredClients.add(new Client(splitRaw[1], splitRaw[2]));
            ServerChat.logger.log(Level.INFO, "<SYSTEM> [ADDACCOUNT]: Account added, registered " + splitRaw[1]);
        }
    }

    private synchronized void deleteAccount(String raw) throws IOException {
        String[] splitRaw = raw.split(" ");

        for (Client client: registeredClients) {
            if (splitRaw[1].equals(client.getUsername())){
                if(connectedClients.containsKey(client.getUsername()))
                    logout(client);
                registeredClients.remove(client);
                ServerChat.logger.log(Level.INFO, "<SYSTEM> [DELETEACCOUNT]: Account deleted, deleted user " + splitRaw[1]);

                return;
            }
        }
        ServerChat.logger.log(Level.INFO, "<SYSTEM> [DELETEACCOUNT]: Account not found, user " + splitRaw[1]);

    }

    private synchronized void haltServer() throws IOException {
        killAll();
        ServerChat.logger.log(Level.INFO, "<SYSTEM> [HALT]: Server halted");

        this.client.getOut().close();
        System.exit(0);
    }

    private synchronized void killAll() throws IOException {
        int i = 0;
        for (String username: connectedClients.keySet().stream().toList()) {
            ServerChat.logger.log(Level.INFO, "<SYSTEM> [KILLALL]: Disconnecting " + username + " (" + ++i + "/" + connectedClients.size() + ")");

            Optional<Client> c = registeredClients.stream().filter(cl -> cl.getUsername().equals(username)).findFirst();
            if (c.isPresent())
                logout(c.get());
        }
        ServerChat.logger.log(Level.INFO, "<SYSTEM> [KILLALL]: All users are logged out");
    }

    private synchronized void killUser(String raw) throws IOException {
        String[] splitRaw = raw.split(" ");

        for(Map.Entry<String, PrintWriter> client : connectedClients.entrySet())
            if (splitRaw[1].equals(client.getKey())) {
                ServerChat.logger.log(Level.INFO, "<SYSTEM> [KILLUSER]: Disconnecting " + client.getKey());

                Optional<Client> c = registeredClients.stream().filter(cl -> cl.getUsername().equals(client.getKey())).findFirst();
                if(c.isPresent())
                    logout(c.get());
                return;
            }
        ServerChat.logger.log(Level.INFO, "<SYSTEM> [KILLUSER]: User " + splitRaw[1] + " not found");
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
            if(this.client.isAdmin() || authentication()) {
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
                        case DELETEACCOUNT -> deleteAccount(raw);
                        case ADDACCOUNT -> addAccount(raw);
                        case LOADBDD -> loadBdd(raw);
                        case SAVEBDD -> saveBdd(raw);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

