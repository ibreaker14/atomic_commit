import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

/*****************************//**
* \class Transaction
* \brief It implements Serializable
**********************************/
public class Transaction implements Serializable{
    private int transactionID;
    private long fileGUID;
    private boolean vote;       // yes or no
    private String operation;   // write or delete
    private long timestamp;
    private String filepath;

    /*****************************//**
    * Transaction Constuctor
    * \param guid id of file
    * \param op operation type
    * \param timestamp timestamp
    * \param path file path
    **********************************/  
    public Transaction(long guid, String op, long timestamp, String path){
        fileGUID = guid;
        operation = op;
        vote = false;
        this.timestamp = timestamp;

        // creates an id based on the object's hascode
        transactionID = this.hashCode();
        filepath = path;
    }

    /*****************************//**
    * retrieve getter for transaction id
    **********************************/
    public int getID(){
        return transactionID;
    }

    /*****************************//**
    * getter for guid
    **********************************/
    public long getFileGUID(){
        return fileGUID;
    }

    /*****************************//**
    * sets vote based on boolean
    **********************************/
    public void setVote(boolean v){
        vote = v;
    }

    /*****************************//**
    * getter for vote
    **********************************/
    public boolean getVote(){
        return vote;
    }

    /*****************************//**
    * getter for timestamp
    **********************************/
    public long getTimestamp(){
        return timestamp;
    }

    /*****************************//**
    * getter for operation type
    **********************************/
    public String getOperation(){
        return operation;
    }

    /*****************************//**
    * getter for filestream
    **********************************/
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
