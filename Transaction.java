import java.nio.file.Path;
import java.util.Date;

public class Transaction {
    private int transactionID;
    private long collabGUID;
    private String vote;        // yes or no
    private String operation;   // write or delete
    private Path dataSrcPath;       // Path of your src file/files that you are transferring if any
    private long timestamp;

    public Transaction(long c, String op){
        collabGUID = c;
        operation = op;
        vote = "Yes";
        timestamp = createTimeStamp();

        // creates an id based on the object's hascode
        transactionID = this.hashCode();
    }

    public int getID(){
        return transactionID;
    }

    public void setVote(boolean v){
        if(v)
            vote = "Yes";
        else
            vote = "False";
    }

    public long createTimeStamp(){
        return (long) (new Date().getTime()/1000);
    }
}
