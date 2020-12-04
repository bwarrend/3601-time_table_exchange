package server;
import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
 
public class ChatServer {
    private int port;
    private Set<String> userNames = new HashSet<>();
    private Set<UserThread> userThreads = new HashSet<>();
    private Logger log;
 
    public ChatServer(int port, Logger log) {
        this.port = port;
        this.log = log;
    }
    
    
    public void execute() {
        
        //Runs the server, creates a new thread for every user and adds
        // the user to a hash set of new users
        try (ServerSocket serverSocket = new ServerSocket(port)) { 
            System.out.println("Server is listening on port " + port);
            log.log("Server is listening on port " + port);
 
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New user connected");
                log.log("New user connected");
 
                UserThread newUser = new UserThread(socket, this, log);
                userThreads.add(newUser);
                newUser.start();
 
            }
 
        } catch (IOException ex) {
            System.err.println("Error in the server: " + ex.getMessage());
            log.log("Error in the server: " + ex.getMessage());
        }
    }
    
    
    //MAIN METHOD
    //Enter a port number and the server will start listening on that port.
    public static void StartServer() {
        Scanner in = new Scanner(System.in);
        
        Logger log = new Logger("serverlog.txt");

//        System.out.println("Enter a port number: ");
        
        int port = 27015;
        
//        try {
//            port = in.nextInt();
//        }catch(Exception e){ 
//            System.out.println("An invalid port was entered.");
//            log.log("An invalid port was entered.");
//            System.exit(0);
//        }
        
        ChatServer server = new ChatServer(port, log);
        server.execute();
    }
    
    
    
 
    //Broadcast message to all users but the sending user
    void broadcast(String message, UserThread excludeUser) {
        for (UserThread aUser : userThreads) {
            if (aUser != excludeUser) {
                aUser.sendMessage(message);
            }
        }
    }
 
    //Adds user name to list of users
    void addUserName(String userName) {
        userNames.add(userName);
    }
 
    //Removes user from userThreads hash set
    void removeUser(String userName, UserThread aUser) {
        boolean removed = userNames.remove(userName);
        if (removed) {
            userThreads.remove(aUser);
            System.out.println("The user " + userName + " has quit");
            log.log("The user " + userName + " has quit");
        }
    }
    
    
    //Return connected users
    Set<String> getUserNames() {
        return this.userNames;
    }
 
    //Return whether we have any connected users
    boolean hasUsers() {
        return !this.userNames.isEmpty();
    }
}


//Handles each user in a seperate thread
class UserThread extends Thread {
    private Socket socket;
    private ChatServer server;
    private PrintWriter writer;
    private Logger log;
    private String userName;
 
    public UserThread(Socket socket, ChatServer server, Logger log) {
        this.socket = socket;
        this.server = server;
        this.log = log;
    }
 
    @Override
    public void run() {
        try {
            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
 
            OutputStream output = socket.getOutputStream();
            writer = new PrintWriter(output, true);
 
            printUsers();
 
            userName = reader.readLine();
            server.addUserName(userName);
 
            String serverMessage = "User set their name to " + userName;
            server.broadcast(serverMessage, this);

            log.log("User set their name to " + userName);
 
            String clientMessage;
 
            do {
                clientMessage = reader.readLine();
                serverMessage = clientMessage;
                server.broadcast(serverMessage, this);
                log.log(serverMessage);
 
            } while (!clientMessage.equals("bye"));
 
            server.removeUser(userName, this);
            socket.close();
 
            serverMessage = userName + " has quit.";
            server.broadcast(serverMessage, this);
            log.log(serverMessage);
 
        } catch (Exception ex) {
            try{
                System.err.println(userName + " has disconnected" + ((ex.equals(null)) ? (": " + ex.getMessage()) : "."));
                log.log(userName + " has disconnected" + ((ex.equals(null)) ? (": " + ex.getMessage()) : "."));
            }catch(Exception e){
                System.err.println("User has disconnected: " + ex.getMessage());
                log.log("User has disconnected: " + ex.getMessage());
            }
        }
    }
 
    //Sends a list of all connected users
    void printUsers() {
        if (server.hasUsers()) {
            writer.println("Connected users: " + server.getUserNames());
        } else {
            writer.println("No other users connected");
        }
    }
 
    //Sends a message
    void sendMessage(String message) {
        writer.println(message);
    }
}

class Logger{
    String fileName;
    BufferedWriter writer;
    DateTimeFormatter dtFormat;
    LocalDateTime currentTime;    
      
    /**
     * Constructor: Take file name as a string.  Create a Buffered Writer and
     * and a date formatter to log with.
     */
    public Logger(String fileName){
        this.fileName = fileName;
        try{
            //Set the FileWriter to true so it appends to the file
            writer = new BufferedWriter(new FileWriter(fileName, true));
            dtFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        }catch(Exception e){
            System.out.println("Unable to begin logging");
        }
    }
    
    /**
     * Take a string argument and print it to the log with the date appended to
     * the beginning.
     */
    public void log(String toLog){        
        try{
            writer.newLine();
            currentTime = LocalDateTime.now();
            writer.write("["+dtFormat.format(currentTime)+"]");
            writer.write(toLog);
            writer.flush();
        }catch(Exception e){
            System.out.println("Unable to log last message");
        }
    }
    
    //Close the writer to finalize the log
    public void close(){
        try{
            writer.close();
        }catch(Exception e){
            System.out.println("Could not close log");
        }
    }
}

