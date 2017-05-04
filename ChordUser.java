import java.rmi.*;
import java.sql.Time;
import java.util.*;
import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.nio.file.*;

/*****************************//**
* \brief This is the User Interface for Chord P2P files
* It allows users to start Nodes and connect to othere 
* \author Mingtau Li, 011110539
* \author Kevin Duong, 011715000
* \author Mark Spencer Tan, 012192282
**********************************/
//**/


/*****************************//**
* \class ChordUser class "Chorduser.java" 
* \brief It implements ChordUser Interface
**********************************/ 
public class ChordUser
{
     HashMap<Long, Timestamp> lastRead;
     int port;

    /** Creates an md5 hashed key for the guid **/
    private long md5(String objectName)
    {
        try
        {
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.reset();
            m.update(objectName.getBytes());
            BigInteger bigInt = new BigInteger(1,m.digest());
            return Math.abs(bigInt.longValue());
        }
        catch(NoSuchAlgorithmException e)
        {
                e.printStackTrace();
                
        }
        return 0;
    }
    
    /*****************************//**
    * Starts Constuctor
    * \param p node port
    **********************************/  
     public ChordUser(int p) {
         lastRead = new HashMap<>();
         port = p;
        
         Timer timer1 = new Timer();
         timer1.scheduleAtFixedRate(new TimerTask() {
            @Override
             public void run() {
                 try {
                     long guid = md5("" + port);
                     Chord chord = new Chord(port, guid);
                     try{
                         Files.createDirectories(Paths.get(guid+"/repository"));
                     }
                     catch(IOException e)
                     {
                         e.printStackTrace();
                         
                     }
                     System.out.println("Usage: \n\tjoin <ip> <port>\n\twrite <file> (the file must be an integer stored in the working directory, i.e, ./"+guid+"/file");
                     System.out.println("\tread <file>\n\tdelete <file>\n\tprint\n\tleave");
        
                     Scanner scan= new Scanner(System.in);
                     String delims = "[ ]+";
                     String command = "";
                     while (true)
                     {
                         String text= scan.nextLine();
                         String[] tokens = text.split(delims);
                         if (tokens[0].equals("join") && tokens.length == 3) {
                             try {
                                 int portToConnect = Integer.parseInt(tokens[2]);
                                 
                                 chord.joinRing(tokens[1], portToConnect);
                             } catch (IOException e) {
                                 e.printStackTrace();
                             }
                         }
                         if (tokens[0].equals("print")) {
                             chord.Print();
                         }
                         if  (tokens[0].equals("write") && tokens.length == 2) {
                             try {
                                 String path;
                                 String fileName = tokens[1];
                                 long guidObject = md5(fileName);
                                 // If you are using windows you have to use
                                 // path = ".\\"+  guid +"\\"+fileName; // path to file
                                 path = "./"+  guid +"/"+fileName; // path to file
                                 FileStream file = new FileStream(path);
                                 ChordMessageInterface peer = chord.locateSuccessor(guidObject);
                                 peer.put(guidObject, file); // put file into ring
                             } catch (IOException e) {
                                 e.printStackTrace();
                             } catch (Exception e){
                                 System.out.println("File not found");
                             }
                         }
                         if  (tokens[0].equals("AtomicWrite") && tokens.length == 2) {
                             atomicWrite(guid, tokens, chord);
                         }
                         // Downloads a file from a chord to the local filesystem
                         if  (tokens[0].equals("read") && tokens.length == 2) {
                             try {
                                 String filename = tokens[1];
                                 Path path = Paths.get("./" + guid + "/" + filename);
                                 long guidObject = md5(filename);
                                 // get a chord that is responsible for the file
                                 ChordMessageInterface peer = chord.locateSuccessor(guidObject);
                                 // open a stream to copy content to stream
                                 InputStream stream = peer.get(guidObject);
                                 // Outputs stream content to a file
                                 Files.copy(stream, path);
                             }catch(IOException e){
                                 e.printStackTrace();
                             }
                        }
                        // Deletes a file stored from a Chord
                        if  (tokens[0].equals("delete") && tokens.length == 2) {
                            try {
                                String filename = tokens[1];
                                long guidObject = md5(filename);
                                // get a chord that is responsible for the file
                                ChordMessageInterface peer = chord.locateSuccessor(guidObject);
                                peer.delete(guidObject);
                            }catch(IOException e){
                                e.printStackTrace();
                            }
                        }

                        // Leaves the ring and passes all files to nearest chord
                        if (tokens[0].equals("leave") && tokens.length == 1){
                            try {
                                chord.leaveRing();
                            } catch (NotBoundException e) {
                                e.printStackTrace();
                            }
                            System.exit(0);
                        }
                     }
                 }
                 catch(RemoteException e)
                 {
                        System.out.println(e);
                 }
             }
         }, 1000, 1000);
    }


    public void atomicWrite(long guid, String[] tokens, ChordMessageInterface chord){
        try {
            String path;
            String fileName = tokens[1];

            // create the 3 guids for the 3 acceptors
            long guidObject1 = md5(fileName + 1);
            long guidObject2 = md5(fileName + 2);
            long guidObject3 = md5(fileName + 3);
            // If you are using windows you have to use
            // path = ".\\"+  guid +"\\"+fileName; // path to file
            path = "./"+  guid +"/"+fileName; // path to file
            FileStream file = new FileStream(path);
            ChordMessageInterface peer1 = chord.locateSuccessor(guidObject1);
            ChordMessageInterface peer2 = chord.locateSuccessor(guidObject2);
            ChordMessageInterface peer3 = chord.locateSuccessor(guidObject3);

            // create a transaction
            Transaction t = new Transaction(guid, "Write");
            // send a canCommit request
            peer1.canCommit(t, timestamp1);
            peer2.canCommit(t, timestamp2);
            peer3.canCommit(t, timestamp3);

            // wait until everyone responds

            // if everyone agrees send doCommit request

            // else abortCommit

            peer.put(guidObject, file); // put file into ring
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e){
            System.out.println("File not found");
        }
    }

    public void atomicRead(long guid, String[] tokens, ChordMessageInterface chord){
        try {
            String filename = tokens[1];
            Path path = Paths.get("./" + guid + "/" + filename);
            long guidObject = md5(filename);
            // get a chord that is responsible for the file
            ChordMessageInterface peer = chord.locateSuccessor(guidObject);
            // open a stream to copy content to stream
            InputStream stream = peer.get(guidObject);
            // Outputs stream content to a file
            Files.copy(stream, path);
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public void atomicDelete(){

    }

    /*****************************//**
    * main program
    * \param args[] node port
    **********************************/  
    public static void main(String args[])
    {
        if (args.length < 1 ) {
            throw new IllegalArgumentException("Parameter: <port>");
        }
        try{
            ChordUser chordUser=new ChordUser( Integer.parseInt(args[0]));
        }
        catch (Exception e) {
           e.printStackTrace();
           System.exit(1);
        }
     } 
}
