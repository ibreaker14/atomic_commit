import java.nio.file.Path;
import java.util.Date;

public class Transaction {
    private int transactionID;
    private long collabGUID;
    private boolean vote;        // yes or no
    private String operation;   // write or delete
    private Path dataSrcPath;       // Path of your src file/files that you are transferring if any
    private long timestamp;

    public Transaction(long c, String op){
        collabGUID = c;
        operation = op;
        vote = true;
        timestamp = createTimeStamp();

        // creates an id based on the object's hascode
        transactionID = this.hashCode();
    }

    public int getID(){
        return transactionID;
    }

    public void setVote(boolean v){
        vote = v;
    }

    public long createTimeStamp(){
        return (long) (new Date().getTime()/1000);
    }
}
