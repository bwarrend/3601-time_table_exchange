package client;

import com.sun.javafx.css.parser.DeriveColorConverter;
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
        this.ID = -1;
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
            
            //Start the read and write threads
            new ReadThread(socket, this, log).start();
            new WriteThread(socket, this, log).start();

        } catch (UnknownHostException ex) {
            System.err.println("Server not found: " + ex.getMessage());
            log.log("Server not found: " + ex.getMessage());
        } catch (IOException ex) {
            System.err.println("Error: " + ex.getMessage());
            log.log("Error: " + ex.getMessage());
        }

    }
    
    
    //Given an array, compare all values with that of the local tVector,
    //the highest updates the local tVector.
    public void updateVector(int[] vec){        
        for(int i = 0; i < vec.length; ++i){
            if(vec[i] > tVector[i]){
                tVector[i] = vec[i];
            }
        }
    }
    
    //Take in an array of ints and spit out a comma separated string
    public String arrayToString(int[] array){
        return Arrays.stream(array).mapToObj(String::valueOf).collect(Collectors.joining(","));
    }
     
    //Take in a comma separated string and spit out an array of ints
    public int[] stringToArray(String string){
        return Arrays.stream(string.split(",")).mapToInt(Integer::parseInt).toArray();
    }
    
    

    public static void main(String[] args) {
        if(args.length > 0){
            if(args[0].equals("server")){
                ChatServer.StartServer();
            }
        }
        
        //use localhost and an open port (27015 for me), establish the 
        //log, and then create a new ChatClient that we will call client.
        String hostname = "127.0.0.1";
        int port = 27015;      
        Logger log = new Logger("clientlog.txt");
        log.log("Connecting to " + hostname + ": " +port);
        ChatClient client = new ChatClient(hostname, port, log);
        client.execute();
    }
}

class ReadThread extends Thread {
    private BufferedReader reader;
    private ChatClient client;
    private Logger log;

    public ReadThread(Socket socket, ChatClient client, Logger log) {
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
                
                //Receive a response from the server
                String response = reader.readLine();
                
                //Don't bother doing anything if server message is empty
                if(response.isEmpty()){
                    continue;
                }
                
                //CURRENTLY NOT IN USE
                //Will stop thread if "stop" is rx from server
                if(response.equals(("stop"))){
                    client.stillReading = false;
                    log.log("Received stop from server; breaking.");
                    break;
                }
                
                //Get an ID from the server (0-4) if we don't have one.
                //Otherwise, rx message from server, turn it into a 5 capacity
                //array, then compare it with the current stored array.  Keep
                //the highest value for each element.
                if(client.waitingForAnID){
                    client.ID = Integer.parseInt((response));
                    System.out.println("Your ID is :" + client.ID);
                    log.log("Your ID is :" + client.ID);
                    client.waitingForAnID = false;
                }else{
                    int[] recVector = client.stringToArray(response);

                    String asString = client.arrayToString(client.tVector);
                    System.out.println("\nClient:" + client.ID + " Received vector: " + response +
                            "\nClient:" + client.ID + " Current vector:  " + asString);
                    log.log("\nClient:" + client.ID + " Received vector: " + response +
                            "\nClient:" + client.ID + " Current vector:  " + asString);
                    
                    client.updateVector(recVector);
                    asString = client.arrayToString(client.tVector);
                    

                    
                    System.out.println("\nClient:" + client.ID + " Updated vector: " + asString);
                    log.log("\nClient:" + client.ID + " Updated vector: " + asString);
                    
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
    private ChatClient client;
    private Logger log;

    public WriteThread(Socket socket, ChatClient client, Logger log) {
        this.client = client;
        this.log = log;
        
        //Create a writer so we can snd messages to server
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
        Random r = new Random();
        
        final int EVENTS = 100;
        final int DELAY_MS = 50;        
        
        //Do not begin writing anything until we have an ID
        //I HAVE NO IDEA WHY IT IS NECESSARY TO PRINT AWAITING ID WHILE WE WAIT
        //If we don't do this, the rest of the program doesn't seem to work.
        log.log("Write thread waiting ID");
        while(client.waitingForAnID){
            System.out.println("Awaiting ID");
        }
        
        System.out.println("Write thread got an ID:  " + client.ID);
        log.log("Write thread got an ID:  " + client.ID);

        do {
            //Generate recipient of event
            int outGoing = r.nextInt(20);               
            if(outGoing > 4){
                outGoing = client.ID;
            }
            
            //Add 1 to our ID element in the local vector
            System.out.println("Client " + client.ID + " is generating its " + client.events + "th event.");
            log.log("Client " + client.ID + " is generating its " + client.events + "th event.");
            client.tVector[client.ID]++;
            
            //Turn our local vector into a string to prepare sending
            String fromArrayToString = client.arrayToString(client.tVector);
            
                        
            System.out.println("Generated event - From: " + client.ID + " to: " + outGoing + "    Vector: " + fromArrayToString);
            log.log("Generated event - From: " + client.ID + " to: " + outGoing + "    Vector: " + fromArrayToString);
            
            //If we are not the recipient of our own generated event, then send
            //the array(now a string) to the server with our ID in the front
            if(outGoing != client.ID){
                writer.println(outGoing + "," + fromArrayToString);
            }
            
            //Increment events
            client.events++;  
            
            if(DELAY_MS > 0){
                try{
                    Thread.sleep(DELAY_MS);
                }catch(InterruptedException ex){
                    Thread.currentThread().interrupt();
                }
            }
            

        //Break out once we generate 100 events
        } while (client.events < EVENTS);
        
        //Send stop to the server
        writer.println("stop");
        
        //Stay here until read thread tells us that it's not reading anymore
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
