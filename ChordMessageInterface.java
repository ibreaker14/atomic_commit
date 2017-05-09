import java.rmi.*;
import java.io.*;
import java.security.Timestamp;
import java.util.Date;

public interface ChordMessageInterface extends Remote {
    public ChordMessageInterface getPredecessor()  throws RemoteException;
    ChordMessageInterface locateSuccessor(long key) throws RemoteException;
    ChordMessageInterface closestPrecedingNode(long key) throws RemoteException;
    public void joinRing(String Ip, int port)  throws RemoteException;
    public void notify(ChordMessageInterface j) throws RemoteException;
    public boolean isAlive() throws RemoteException;
    public long getId() throws RemoteException;
    public void setPredecessor(ChordMessageInterface predecessor) throws RemoteException;
    public void setSuccessor(ChordMessageInterface successor) throws RemoteException;

    public void put(long guidObject, InputStream file) throws IOException, RemoteException;
    public InputStream get(long guidObject) throws IOException, RemoteException;
    public void delete(long guidObject) throws IOException, RemoteException;

    public boolean canCommit(Transaction t) throws RemoteException;
    public void doCommit(Transaction t) throws RemoteException;
    public void abortCommit(Transaction t) throws RemoteException;
    public void atomicWrite(long guid, String filename) throws RemoteException;
    public void atomicRead(long guid, String filename) throws RemoteException;
    public long createTimeStamp() throws RemoteException;
    public void newReadTimestamp(long guid, long timestamp) throws RemoteException;
    public void newWriteTimestamp(long guid, long timestamp) throws RemoteException;
}
