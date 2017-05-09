import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

public class Transaction implements Serializable{
    private int transactionID;
    private long fileGUID;
    private boolean vote;       // yes or no
    private String operation;   // write or delete
    private long timestamp;
    private String filepath;

    public Transaction(long guid, String op, long timestamp, String path){
        fileGUID = guid;
        operation = op;
        vote = false;
        this.timestamp = timestamp;

        // creates an id based on the object's hascode
        transactionID = this.hashCode();
        filepath = path;
    }

    public int getID(){
        return transactionID;
    }

    public long getFileGUID(){
        return fileGUID;
    }

    public void setVote(boolean v){
        vote = v;
    }

    public boolean getVote(){
        return vote;
    }

    public long getTimestamp(){
        return timestamp;
    }

    public String getOperation(){
        return operation;
    }

    public FileStream getFileStream(){
        FileStream fstream = null;
        try {
            fstream = new FileStream(filepath);
        }catch(IOException ex){
            ex.printStackTrace();
        }
        return fstream;
    }
}
