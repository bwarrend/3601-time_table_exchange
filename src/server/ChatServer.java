package server;
import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
 
public class ChatServer {
    private int port;
    private Set<String> userNames = new HashSet<>();
    private Set<UserThread> userThreads = new HashSet<>();
    private Logger log;
    public int currentID;
    public boolean receivingNewConnections;
 
    public ChatServer(int port, Logger log) {
        this.port = port;
        this.log = log;
        currentID = 0;
        receivingNewConnections = true;
    }
    
    
    public void execute() {
        
        //Runs the server, creates a new thread for every user and adds
        // the user to a hash set of new users
        try (ServerSocket serverSocket = new ServerSocket(port)) { 
            System.out.println("Server is listening on port " + port);
            log.log("Server is listening on port " + port);
 
            while (true) {
                //Accept incomming socket connection requests
                Socket socket = serverSocket.accept();
                System.out.println("New client connected: " + currentID);
                log.log("New client connected: " + currentID);
                
                //Create a new user thread for socket connections
                UserThread newUser = new UserThread(socket, this, log, currentID);
                
                //Increment which ID we are giving out, add user thread to hash
                currentID++;
                userThreads.add(newUser);
                
                
                //Once we have enough Clients, start the userthreads and send 
                //each client an ID
                if(currentID > 4 && receivingNewConnections){                    
                    System.out.println("Five clients connected, let's start them up.");
                    log.log("Five clients connected, let's start them up.");
                    for(UserThread user : userThreads){
                        System.out.println("STARTING CLIENT: " + user.ID);
                        log.log("STARTING CLIENT: " + user.ID);
                        user.start();
                    }
                    //Send ID to each user.  String.format needed to be used instead
                    //of Integer.parseInt() because that for some reason was only
                    //sending "" strings and causing problems
                    for(UserThread user : userThreads){
                        System.out.println("Send ID to user: " + user.ID);
                        log.log("Send ID to user: " + user.ID);
                        user.sendMessage(String.format("%d",user.ID));
                    }
                    
                    receivingNewConnections = false;
                }
                
 
            }
 
        } catch (IOException ex) {
            System.err.println("Error in the server: " + ex.getMessage());
            log.log("Error in the server: " + ex.getMessage());
        }
    }
    
    
    //MAIN METHOD
    //Enter a port number and the server will start listening on that port.
    public static void StartServer() {        
        //Create server log and set port
        Logger log = new Logger("serverlog.txt");        
        int port = 27015;
        
        //Begin server
        ChatServer server = new ChatServer(port, log);
        server.execute();
    }
    
    
    //Send string message that contains an array to the user with provided ID    
    void sendVectorToWhom(String sendyMessage, int id){
        for(UserThread user : userThreads){
            if (user.ID == id){
                user.sendMessage(sendyMessage);
            }
        }
    }
    
}


//Handles each user in a seperate thread
class UserThread extends Thread {
    private Socket socket;
    private ChatServer server;
    private PrintWriter writer;
    private Logger log;
    public int ID;
    public static int finishedWriting;
 
    public UserThread(Socket socket, ChatServer server, Logger log, int ID) {
        this.socket = socket;
        this.server = server;
        this.log = log;
        this.ID = ID;
        finishedWriting = 0;
    }
 
    @Override
    public void run() {
        try {
            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
 
            OutputStream output = socket.getOutputStream();
            writer = new PrintWriter(output, true);
 
            String clientMessage;
 
            
            do {
                clientMessage = reader.readLine();
                
                if(clientMessage.equals("stop")){
                    break;
                }
                
                //Extract recipient and prepare message to be sent to them                
                int outGoingID = Character.getNumericValue(clientMessage.charAt(0));
                String justTheMessage = clientMessage.substring(2);
                
                
                System.out.println("_Server received event_" + 
                        "\n<From " + ID + "  TO> " + outGoingID +
                        "\nMessage: " + justTheMessage);
                
                log.log("_Server received event_" + 
                        "\n<From " + ID + "  TO> " + outGoingID +
                        "\nMessage: " + justTheMessage);                
                
                //Send the vector to the recipient
                server.sendVectorToWhom(justTheMessage, outGoingID);

                
            }while(true);
            
            finishedWriting++;
            
            //If we receive stop from a client, wait until we receive stop
            //from all clients
            while(finishedWriting <= 5){
                
            }
            
            socket.close();
            
 
        } catch (Exception ex) {
            try{
                System.err.println(ID + " has disconnected" + ((ex.equals(null)) ? (": " + ex.getMessage()) : "."));
                log.log(ID + " has disconnected" + ((ex.equals(null)) ? (": " + ex.getMessage()) : "."));
            }catch(Exception e){
                System.err.println("User has disconnected: " + ex.getMessage());
                log.log("User has disconnected: " + ex.getMessage());
            }
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

