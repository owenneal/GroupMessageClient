import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Message implements Serializable {
    private static final long serialVersionUID = 42L;
    private String sender; //sender of message
    private List<String> receiver; //receiver of message
    private String message; //message content
    private long timestamp; //time message was sent
    private String type; //type of message server or client

    private String group; //group name

    //message constructor, only need one because we are using a list even if only one recipient
    public Message(String sender, List<String>recipients, String message, String type, String groupName) {
        this.sender = sender;
        this.receiver = recipients;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
        this.type = type;
        this.group = groupName;
    }

    //getters
    public String getSender() {
        return sender;
    }

    public List<String> getReceiver() {
        return receiver;
    }

    public String getGroupName() {
        return group;
    }
    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getType() {
        return type;
    }

    //toString method
    public String toString() {
        return "From: " + sender + " To: " + receiver + " Message: " + message + " Timestamp: " + timestamp;
    }

    //setters
    public void setType(String type) {
        this.type = type;
    }
    public void setSender(String sender) {
        this.sender = sender;
    }

    public void setReceiver(ArrayList<String> receiver) {
        this.receiver = receiver;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}