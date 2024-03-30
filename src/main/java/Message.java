import java.io.Serializable;
import java.util.List;

public class Message implements Serializable {
    static final long serialVersionUID = 42L;

    //message types
    public static final int MESSAGE = 0;
    public static final int GROUP_MESSAGE = 1;
    public static final int INDIVIDUAL_MESSAGE = 2;

    private int type; //type of message
    private String sender; //sender of message
    private List<String> receiver; //receiver of message
    private String message; //message content
    private long timestamp; //time message was sent

    public Message(String sender, List<String>reciever, )
}