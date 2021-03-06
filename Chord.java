import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.net.*;
import java.security.Timestamp;
import java.sql.Time;
import java.util.*;
import java.io.*;
/*****************************//**
* \brief This program is the Chord P2P server for atomic commit
* It creates a ring of nodes that copies files to multiple nodes
* \author Mingtau Li, 011110539
* \author Kevin Duong, 011715000
* \author Mark Spencer Tan, 012192282
**********************************/

/*****************************//**
* \class Chord class "Chord.java" 
* \brief It implements Chord P2P
**********************************/ 
public class Chord extends java.rmi.server.UnicastRemoteObject implements ChordMessageInterface {
    public static final int M = 2;

    Registry registry;    // rmi registry for lookup the remote objects.
    ChordMessageInterface successor;
    ChordMessageInterface predecessor;
    ChordMessageInterface[] finger;
    // Hashmap key: guid of the file, value: timestamp
    HashMap<Long, Long> lastRead;
    HashMap<Long, Long> lastWrite;
    int nextFinger;
    long guid; // GUID (i)
    Timer timer;

    /*****************************//**
    * Implements Chord Message Interface
    * \param ip Ip address of Node in network
    * \param port where node is located
    **********************************/  
    public ChordMessageInterface rmiChord(String ip, int port) {
        ChordMessageInterface chord = null;
        try{
            Registry registry = LocateRegistry.getRegistry(ip, port);
            chord = (ChordMessageInterface)(registry.lookup("Chord"));
        } catch (RemoteException | NotBoundException e){
            e.printStackTrace();
        }
        return chord;
    }

    /*****************************//**
    * Checks if key is in semi close interval
    * \param key 1st key to be compared
    * \param key2 2nd key to be compared
    * \param key3 3rd key to be compared
    **********************************/  
    public Boolean isKeyInSemiCloseInterval(long key, long key1, long key2) {
       if (key1 < key2)
           return (key > key1 && key <= key2);
      else
          return (key > key1 || key <= key2);
    }

    /*****************************//**
    * Checks if key is in open interval
    * \param key 1st key to be compared
    * \param key2 2nd key to be compared
    * \param key3 3rd key to be compared
    **********************************/  
    public Boolean isKeyInOpenInterval(long key, long key1, long key2) {
      if (key1 < key2)
          return (key > key1 && key < key2);
      else
          return (key > key1 || key < key2);
    }

    /*****************************//**
    * puts file into directory
    * \param guidObject file
    * \param stream input stream of file
    **********************************/  
    public void put(long guidObject, InputStream stream) throws RemoteException {
          try {
          //sets directory of repository and puts file in it
          String fileName = "./"+guid+"/repository/" + guidObject;
          FileOutputStream output = new FileOutputStream(fileName);
          while (stream.available() > 0)
              output.write(stream.read());
          output.close();
      }
      catch (IOException e) {
          System.out.println(e);
      }
    }


    /*****************************//**
    * retrieves file from directory
    * \param guidObject file
    **********************************/  
    public InputStream get(long guidObject) throws RemoteException { //IB
        FileStream file = null;
        String fileName = "./"+guid+"/repository/"+guidObject;
            try {
                file = new FileStream(fileName);
            }catch(Exception e){
                e.printStackTrace();
            }
            return file;
    }

    /*****************************//**
    * deletes file from repository
    * \param guidObject file
    **********************************/  
    public void delete(long guidObject) throws RemoteException {
        try {
            String fileName = "./"+guid+"/repository/" + guidObject;
            Files.delete(Paths.get(fileName));
        }
        catch (IOException e) {
            System.out.println(e);
        }
    }


    /*****************************//**
    * retrieve guid
    **********************************/  
    public long getId() throws RemoteException {
        return guid;
    }

    /*****************************//**
    * keeps connection alive
    **********************************/  
    public boolean isAlive() throws RemoteException {
	    return true;
    }

    /*****************************//**
    * sets successor of node
    * /param successor successor of node
    **********************************/  
    public void setSuccessor(ChordMessageInterface successor) throws RemoteException{
        this.successor = successor;
    }

    /*****************************//**
    * sets predecessor of node
    * /param predecessor predecessor of node
    **********************************/  
    public void setPredecessor(ChordMessageInterface predecessor) throws RemoteException{
        this.predecessor = predecessor;
    }

    /*****************************//**
    * retrieves successor of node
    **********************************/  
    public ChordMessageInterface getPredecessor() throws RemoteException {
	    return predecessor;
    }

    /*****************************//**
    * locates successor of node based on key
    * /param key distinct file key
    **********************************/  
    public ChordMessageInterface locateSuccessor(long key) throws RemoteException {
	    if (key == guid)
            throw new IllegalArgumentException("Key must be distinct that  " + guid);
	    if (successor.getId() != guid) {
	      if (isKeyInSemiCloseInterval(key, guid, successor.getId()))
	        return successor;
	      ChordMessageInterface j = closestPrecedingNode(key);

          if (j == null)
	        return null;
	      return j.locateSuccessor(key);
        }
        return successor;
    }

    /*****************************//**
    * gets closest preceding node 
    * /param key distinct file key
    **********************************/  
    public ChordMessageInterface closestPrecedingNode(long key) throws RemoteException {
        //M is number of nodes. iterate thorugh it number of nodes -1 times
        //so u dont hit urself
        for(int x = 0; x <= M-1;x++) {
          if(finger[x] != null) {
            if(isKeyInSemiCloseInterval(key, this.getId(), finger[x].getId())) {
              return finger[x];
            }
          }
        }
        return successor;

    }

    /*****************************//**
    * allows node to join ring
    * /param ip IP address of node
    * /param port node port
    **********************************/  
    public void joinRing(String ip, int port)  throws RemoteException {
        try{
            System.out.println("Get Registry to joining ring");
            Registry registry = LocateRegistry.getRegistry(ip, port);
            ChordMessageInterface chord = (ChordMessageInterface)(registry.lookup("Chord"));
            predecessor = null;
            successor = chord.locateSuccessor(this.getId());
            System.out.println("Joining ring");
        }
        catch(RemoteException | NotBoundException e){
            successor = this;
        }
    }

    /*****************************//**
    * leaves ring
    **********************************/  
    public void leaveRing() throws RemoteException, NotBoundException{
        timer.cancel();

        predecessor.setSuccessor(successor);
        successor.setPredecessor(predecessor);

        Path path = Paths.get("./" + guid + "/repository");
        //get nearest chord neighbor that will inherit the file

        //Copy the files to the nearest chord's repo
        Path neighbor_path = Paths.get("./" + successor.getId() + "/repository");

        copyFolder(path, neighbor_path);

        successor = null;
        predecessor = null;
    }

    /*****************************//**
    * finds the next successor of node
    **********************************/  
    public void findingNextSuccessor() {
        int i;
        successor = this;
        for (i = 0;  i< M; i++) {
            try {
                if (finger[i].isAlive()) {
                    successor = finger[i];
                }
            }
            catch(RemoteException | NullPointerException e) {
                finger[i] = null;
            }
        }
    }

    /*****************************//**
    * stabilizes connection
    **********************************/  
    public void stabilize() {
      try {
          if (successor != null)
          {
              ChordMessageInterface x = successor.getPredecessor();

              if (x != null && x.getId() != this.getId() && isKeyInOpenInterval(x.getId(), this.getId(), successor.getId()))
              {
                  successor = x;
              }
              if (successor.getId() != getId())
              {
                  successor.notify(this);
              }
          }
      } catch(RemoteException | NullPointerException e1) {
          findingNextSuccessor();

      }
    }

    /*****************************//**
    * notifies j, the previous predecessor
    * /param j previous predecessor
    **********************************/  
    public void notify(ChordMessageInterface j) throws RemoteException {
        String mRepoPath = "./"+guid+"/repository";

        if (predecessor == null || (predecessor != null
                    && isKeyInOpenInterval(j.getId(), predecessor.getId(), guid))) {

            //transfer keys in the range [j,i) to j;
            File src_dir = Paths.get(mRepoPath).toFile();
            // Array of filenames within the i's repo
            String files[] = src_dir.list();
            for (String file : files) {
                long file_key = Long.parseLong(file);
                // see if file k is within range of [j,i)
                if(isKeyInSemiCloseInterval(file_key, j.getId(), this.guid)){
                    try {
                        String srcPath = mRepoPath + "/" + file;
                        //insert file to j if it is
                        j.put(file_key, new FileStream(srcPath));
                        //delete key in i
                        this.delete(file_key);
                    } catch (IOException e) {
                        System.out.println("Invalid Folder paths");
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
            // set j as predecessor of i
            predecessor = j;
        }
    }

    /*****************************//**
    * fixes fingers
    **********************************/  
    public void fixFingers() {
        long id= guid;
        try {
            long nextId;
            if (nextFinger == 0)
                nextId = (this.getId() + (1 << nextFinger));
            else
                nextId = finger[nextFinger -1].getId();
            finger[nextFinger] = locateSuccessor(nextId);

            if (finger[nextFinger].getId() == guid)
                finger[nextFinger] = null;
            else
                nextFinger = (nextFinger + 1) % M;
        }
        catch(RemoteException | NullPointerException e){
            //finger[nextFinger] = null;
            e.printStackTrace();
        }
    }

    /*****************************//**
    * checks predecessor
    **********************************/  
    public void checkPredecessor() {
      try {
          if (predecessor != null && !predecessor.isAlive())
              predecessor = null;
      }
      catch(RemoteException e)
      {
          predecessor = null;
      }
    }

    /*****************************//**
    * Chord Constructor
    * /param port port of node
    * /param guid guid of node
    **********************************/  
    public Chord(int port, long guid) throws RemoteException {
        int j;
	    finger = new ChordMessageInterface[M];
        for (j=0;j<M; j++){
	       finger[j] = null;
     	}
        this.guid = guid;

        lastWrite = new HashMap<>();
        lastRead = new HashMap<>();

        predecessor = null;
        successor = this;
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
	    @Override
	    public void run() {
            stabilize();
            fixFingers();
            checkPredecessor();
            }
        }, 500, 500);
        try{
            // create the registry and bind the name and object.
            System.out.println(guid + " is starting RMI at port="+port);
            registry = LocateRegistry.createRegistry( port );
            registry.rebind("Chord", this);
        }
        catch(RemoteException e){
	       throw e;
        }
    }


    /*****************************//**
    * prints relevant info
    **********************************/  
    void Print() {
        int i;
        try {
            if (successor != null)
                System.out.println("successor "+ successor.getId());
            if (predecessor != null)
                System.out.println("predecessor "+ predecessor.getId());
            for (i=0; i<M; i++) {
                try {
                    if (finger != null)
                        System.out.println("Finger "+ i + " " + finger[i].getId());
                } catch(NullPointerException e) {
                    finger[i] = null;
                }
            }
        }
        catch(RemoteException e){
	       System.out.println("Cannot retrive id");
        }
    }

    /*****************************//**
    * verifies that files can commit
    * /param transaction transaction object for commit
    **********************************/  
    public boolean canCommit(Transaction t) throws RemoteException{
        if(t.getOperation().equals("Write")) {
            long transTimestamp = t.getTimestamp();

            if(lastRead.containsKey(t.getFileGUID())) {
                long lastWriteTimestamp = lastWrite.get(t.getFileGUID());

                if (lastWriteTimestamp > transTimestamp) {
                    System.out.println(this.getId() + " votes no for transaction:" + t.getID());
                    return false;
                } else
                    System.out.println(this.getId() + " votes yes for transaction:" + t.getID());
                return true;
            }
            else{
                System.out.println(this.getId() + " votes yes for transaction:" + t.getID());
                return true;
            }
        }
        System.out.println(this.getId() + " votes yes for transaction:" + t.getID());
        return true;
    }

    /*****************************//**
    * commits files
    * /param transaction transaction object for commit
    **********************************/  
    public void doCommit(Transaction t) throws RemoteException{
        try {
            // call the put method which will grab the file from the server to your local storage
            this.put(t.getFileGUID(), t.getFileStream());

            // update your lastWrite hashmap
            newWriteTimestamp(t.getFileGUID(), t.getTimestamp());
            newReadTimestamp(t.getFileGUID(), t.getTimestamp());
        }catch(RemoteException ex){
            ex.printStackTrace();
        }
    }

    /*****************************//**
    * aborts current transaction 
    * /param transaction transaction object for commit
    **********************************/  
    public void abortCommit(Transaction t) throws RemoteException{
        // abort stuff? for now dont do anything
    }

    /*****************************//**
    * atomic writes file in repository
    * /param guid id of file
    * /param filename name of file
    **********************************/  
    public void atomicWrite(long guid, String filename) throws RemoteException{
        try {
            // create the 3 guids for the 3 acceptors
            long guidObject1 = ChordUser.md5(filename + 1);
            long guidObject2 = ChordUser.md5(filename + 2);
            long guidObject3 = ChordUser.md5(filename + 3);
            // If you are using windows you have to use
            // path = ".\\"+  guid +"\\"+fileName; // path to file
            String path = "./"+  guid +"/"+filename; // path to file
//            FileStream file = new FileStream(path);
            ChordMessageInterface peer1 = this.locateSuccessor(guidObject1);
            ChordMessageInterface peer2 = this.locateSuccessor(guidObject2);
            ChordMessageInterface peer3 = this.locateSuccessor(guidObject3);

            // get the lastread timestamps for all 3 versions of the file.
            long timestamp = createTimeStamp();
            long ts1, ts2, ts3;
            if(!lastRead.containsKey(guidObject1))
                ts1 = timestamp;
            else
                ts1 = lastRead.get(guidObject1);
            if(!lastRead.containsKey(guidObject2))
                ts2 = timestamp;
            else
                ts2 = lastRead.get(guidObject2);
            if(!lastRead.containsKey(guidObject3))
                ts3 = timestamp;
            else
                ts3 = lastRead.get(guidObject3);

            // send a canCommit request to the 3 participants passing the Transaction
            Transaction t1 = new Transaction(guidObject1, "Write", ts1, path);
            boolean v1 = peer1.canCommit(t1);
            Transaction t2 = new Transaction(guidObject2, "Write", ts2, path);
            boolean v2 = peer2.canCommit(t2);
            Transaction t3 = new Transaction(guidObject3, "Write", ts3, path);
            boolean v3 = peer3.canCommit(t3);

            // if everyone agrees send doCommit request
            if(v1 && v2 && v3) {
                peer1.doCommit(t1); // put file into ring
                peer2.doCommit(t2); // put file into ring
                peer3.doCommit(t3); // put file into ring
                System.out.println("AtomicWrite Successful, Pushing Commit");
            }
            else{
                peer1.abortCommit(t1);
                peer2.abortCommit(t2);
                peer3.abortCommit(t3);
                System.out.println("AtomicWrite failed, aborting all commits...");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /*****************************//**
    * atomic reads file in repository
    * /param guid id of file
    * /param filename name of file
    **********************************/  
    public void atomicRead(long guid, String filename) throws RemoteException{
        try {
            Path path = Paths.get("./" + guid + "/" + filename);
            // get the guid of the file+i
            long guidObject1 = ChordUser.md5(filename + 1);
            long guidObject2 = ChordUser.md5(filename + 2);
            long guidObject3 = ChordUser.md5(filename + 3);

            // get a chord that is responsible for the file
            ChordMessageInterface peer1 = this.locateSuccessor(guidObject1);
            ChordMessageInterface peer2 = this.locateSuccessor(guidObject2);
            ChordMessageInterface peer3 = this.locateSuccessor(guidObject3);

            // open a stream to copy content to stream
            InputStream stream = peer1.get(guidObject1);
            // Outputs stream content to a file
            String fileName = path.toString();
            FileOutputStream output = new FileOutputStream(fileName);
            while (stream.available() > 0)
                output.write(stream.read());
            output.close();

            //Files.copy(stream, path);

            long timestamp = createTimeStamp();

            // update the other's lastRead hashmap with the current timestamp
//            peer1.newReadTimestamp(guidObject1, timestamp);
//            peer2.newReadTimestamp(guidObject2, timestamp);
//            peer3.newReadTimestamp(guidObject3, timestamp);

            // update your lastRead hashmap
            newReadTimestamp(guidObject1, timestamp);
            newReadTimestamp(guidObject2, timestamp);
            newReadTimestamp(guidObject3, timestamp);
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    /*****************************//**
    * copies contents of folder
    **********************************/  
    public static void copyFolder(Path src, Path dest) {
        File src_dir = src.toFile();
        String files[] = src_dir.list();
        for (String file : files) {
            try {
                Path srcPath = Paths.get(src + "/" + file);
                Path destPath = Paths.get(dest + "/" + file);
                Files.copy(srcPath, destPath);
            } catch (IOException e) {
                System.out.println("Invalid Folder paths");
            }
        }
    }

    /*****************************//**
    * helper function for creating timestamps
    **********************************/  
    public long createTimeStamp() throws RemoteException{
        return (new Date().getTime()/1000);
    }

    /*****************************//**
    * read new timestamp
    * /param guid id of file
    * /param timestamp timestamp
    **********************************/  
    public void newReadTimestamp(long guid, long timestamp) throws RemoteException{
        lastRead.put(guid, timestamp);
    }

    /*****************************//**
    * write a new timstamp
    * /param guid id of file
    * /param timestamp timestamp
    **********************************/  
    public void newWriteTimestamp(long guid, long timestamp) throws RemoteException{
        lastWrite.put(guid, timestamp);
    }

}
