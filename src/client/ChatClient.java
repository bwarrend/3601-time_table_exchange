package client;

import java.net.*;
import java.io.*;
import java.util.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.stream.Collectors;
import server.ChatServer;

public class ChatClient {
    private String hostname;
    private int port;
//    private String userName;
    private Logger log;
    public int[] tVector = {0, 0, 0, 0, 0};
    public int ID;
    public int events;
    public boolean waitingForAnID;
    public boolean stillReading;

    public ChatClient(String hostname, int port, Logger log) {
        this.hostname = hostname;
        this.port = port;
        this.log = log;
        this.ID = 1;
        this.events = 0;
        this.waitingForAnID = true;
        this.stillReading = true;
    }
 
    public void execute() {
        try {            
            //Create socket and set up a read and write thread 
            Socket socket = new Socket(hostname, port); 
            System.out.println("Connected to the chat server");
            log.log("Connected to the chat server");

            new ReadThread(socket, this, log).start();
            
            //let's wait until we get the go ahead before we begin the write thread
            //Get the ID from the readthread
            new WriteThread(socket, this, log).start();

        } catch (UnknownHostException ex) {
            System.err.println("Server not found: " + ex.getMessage());
            log.log("Server not found: " + ex.getMessage());
        } catch (IOException ex) {
            System.err.println("Error: " + ex.getMessage());
            log.log("Error: " + ex.getMessage());
        }

    }
    
    public void updateVector(String asString){
        int[] vec = Arrays.stream(asString.split(",")).mapToInt(Integer::parseInt).toArray();
        
        for(int i = 0; i < vec.length; ++i){
            if(vec[i] > this.tVector[i]){
                tVector[i] = vec[i];
            }
        }
    }

//    //Set user name
//    void setUserName(String userName) {
//        this.userName = userName;
//    }
    
//    //Return the user name
//    String getUserName() {
//        return this.userName;
//    }


    public static void main(String[] args) {
        if(args.length > 0){
            if(args[0].equals("server")){
                ChatServer.StartServer();
            }
        }
        
        
        String hostname = "127.0.0.1";
        int port = 27015;
//        Scanner in = new Scanner(System.in);        
        Logger log = new Logger("clientlog.txt");

        //Create a chatclient based on user inputted IP and port
//        System.out.println("Enter IP: "); 
//        hostname = in.nextLine();
        log.log(hostname + " entered as hostname.");

//        try{
//            System.out.println("Enter port: ");
//            port = in.nextInt();
//            log.log(port + " entered as port.");
//        }catch(Exception e){
//            System.err.println("Invalid port entered.");
//            log.log("Invalid port entered.");
//            System.exit(0);
//        }

        ChatClient client = new ChatClient(hostname, port, log);
        client.execute();
    }
}

class ReadThread extends Thread {
    private BufferedReader reader;
    private Socket socket;
    private ChatClient client;
    private Logger log;

    public ReadThread(Socket socket, ChatClient client, Logger log) {
        this.socket = socket;
        this.client = client;
        this.log = log;

        try {
            InputStream input = socket.getInputStream();
            reader = new BufferedReader(new InputStreamReader(input));
        } catch (IOException ex) {
            System.err.println("Error getting input stream: " + ex.getMessage());
            log.log("Error getting input stream: " + ex.getMessage());
        }
    }

    @Override
    public void run() {        
        while (true) {
            try {
                String response = reader.readLine();
                
                if(response.equals(("stop"))){
                    client.stillReading = false;
                    log.log("Received stop from server; breaking.");
                    break;
                }
                
                if(client.waitingForAnID){
                    client.ID = Integer.parseInt((response));
                    System.out.println("Your ID is :" + client.ID);
                    log.log("Your ID is :" + client.ID);
                }else{
                    
                    
                    
                    
                    
//                    System.out.println("\n" + response);
//                    log.log(response);
                }                


            } catch (IOException ex) {
                System.err.println("Error reading from server: " + ex.getMessage());
                log.log("Error reading from server: " + ex.getMessage());
                break;
            }
        }
    }
}

class WriteThread extends Thread {
    private PrintWriter writer;
    private Socket socket;
    private ChatClient client;
    private Logger log;

    public WriteThread(Socket socket, ChatClient client, Logger log) {
        this.socket = socket;
        this.client = client;
        this.log = log;

        try {
            OutputStream output = socket.getOutputStream();
            writer = new PrintWriter(output, true);
        } catch (IOException ex) {
            System.err.println("Error getting output stream: " + ex.getMessage());
            log.log("Error getting output stream: " + ex.getMessage());
        }
    }

    @Override
    public void run() {

        Scanner in = new Scanner(System.in);

//        System.out.println("Enter username: ");
//        String userName = in.nextLine();
//        log.log(userName + " is now your username.");
//        client.setUserName(userName);
//        writer.println(userName);

//        String text;
        Random r = new Random();
        
        
        while(client.waitingForAnID);

        do {
//            text = in.nextLine();
//            writer.println(userName + ": " + text);
//            log.log("YOU: " + text);
//            if(text.equals("bye")){
//                System.out.println("Disconnecting from server.");
//                log.log("Disconnecting from server.");
//                System.exit(0);
//            }


            client.events++;
            int outGoing = r.nextInt(5)+1;
            
            

            log.log("Event: " + client.events);
            client.tVector[client.ID]++;
            String fromArrayToString = Arrays.stream(client.tVector).mapToObj(String::valueOf).collect(Collectors.joining(","));
            client.updateVector(fromArrayToString);
            
            log.log("From: " + client.ID + " to: " + outGoing + "    Vector: " + client.tVector);
            if(outGoing != client.ID){
                writer.println(outGoing + "," + fromArrayToString);
            }
            
            client.events++;

        

        } while (client.events < 100);
        
        while(client.stillReading);
        

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
