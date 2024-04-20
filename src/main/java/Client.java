import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

//Owen Neal
//CS 342


public class Client extends Thread{
	Socket socketClient;
	ObjectOutputStream out;
	ObjectInputStream in;
	private String username;
	private Consumer<Serializable> callback;
	private final Object connectedClientsLock = new Object();
	private final ArrayList<String> connectedClients = new ArrayList<>();

	private final HashMap<String, List<String>> groupMessageHistory = new HashMap<>();
	private final List<String> sentToUserHistory = new ArrayList<>();
	private final HashMap<String, List<String>> availableGroupChats = new HashMap<>();
	private boolean userNameAssigned = true;
	
	Client(Consumer<Serializable> call){
		this.username = generateUsername();
		callback = call;
	}

	public boolean getUserNameAssigned() {
		return userNameAssigned;
	}

	public void updateUsername(String name) {
		this.username = name;
	}

	public String generateUsername() {
		Random rand = new Random();
		int randNum = rand.nextInt(1000);
        return "User" + randNum;
	}
	
	public void run() {
		try {
			socketClient= new Socket("127.0.0.1",5555);
			out = new ObjectOutputStream(socketClient.getOutputStream());
			in = new ObjectInputStream(socketClient.getInputStream());
			socketClient.setTcpNoDelay(true);
		}
		catch(Exception e) {
			System.err.println("An error occurred in the client run method: " + e.getMessage());
			callback.accept("Connection to server failed");
		}
		
		while(true) {
			try {
				Message message = (Message) in.readObject();
				if(message.getType().equals("joined")) {
					callback.accept(message.getSender() + message.getMessage());
				}
				if (message.getType().equals("message")) {
					callback.accept(message);
					if (message.getGroupName() != null) {
						updateGroupChatHistory(message.getGroupName(), message.getSender() + ": " + message.getMessage());
					}
					if (!message.getSender().equals(this.username)) {
						this.sentToUserHistory.add(message.getSender() + ":" + message.getMessage());
					}
				}
				if (message.getType().equals("left")) {
					callback.accept(message.getSender() + message.getMessage());
					connectedClients.remove(message.getSender());
				}
				if (message.getType().equals("connectedClients")) {
					synchronized (connectedClientsLock) {
						callback.accept(message);
					}
					for (String client : message.getReceiver()) {
						if (!connectedClients.contains(client)) {
							connectedClients.add(client);
						}
					}
				}
				if (message.getType().equals("group")) {
					callback.accept(message);
					updateAvailableGroupChats(message.getGroupName(), message.getReceiver());// update available group chats
				}

				if (message.getType().equals("groupList")) {
					callback.accept(message);
				}

				if (message.getType().equals("usernameCheck")){
					callback.accept(message);
				}

			}
			catch(Exception e) {
				System.err.println("An error occurred while processing messages: " + e.getMessage());
				e.printStackTrace();
			}
		}
    }
	
	public void send(String data, List<String> recipients, String group) {
		try {
			List<String> rec = new ArrayList<>(recipients);
			out.writeObject(new Message(username, rec, data, "message", group));
			out.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void sendUsername(String name) {
		try {
			out.writeObject(new Message(name, null, " has joined the chat", "joined", null));
			out.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void sendGroup(String groupName, List<String> recipients) {
		try {
			List<String> rec = new ArrayList<>(recipients);
			out.writeObject(new Message(null, rec, " has joined the group", "group", groupName));
			out.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void checkAvailableUserNames(String name, CountDownLatch latch) {
		try {
			out.writeObject(new Message(name, null, "checking available usernames", "usernameCheck", null));
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		//Save the original callback
		Consumer<Serializable> originalCallback = callback;
		callback = data -> {
			if (data instanceof Message) {
				Message message = (Message) data;
				if (message.getType().equals("usernameCheck")) {
					userNameAssigned = !message.getMessage().equals("Username taken");
					latch.countDown();
					//Restore the original callback
					callback = originalCallback;
				}
			}
		};
}

	public void updateAvailableGroupChats(String groupName, List<String> recipients) {
		//if (!availableGroupChats.containsKey(groupName)) {
			List<String> groupMessages = new ArrayList<>(recipients);
			availableGroupChats.put(groupName, groupMessages);
			System.out.println("Group: " + groupName + " has been created");
		//}
	}

	public void updateGroupChatHistory(String groupName, String message) {
		if (!groupMessageHistory.containsKey(groupName)) {
			groupMessageHistory.put(groupName, new ArrayList<>());
		}
		groupMessageHistory.get(groupName).add(message);
	}

	public List<String> getGroupRecipients(String groupName) {
		return availableGroupChats.get(groupName);
	}

	public List<String> getGroupChatHistory(String groupName) {
		return groupMessageHistory.getOrDefault(groupName, new ArrayList<>());
	}

	public List<String> getMessagesSentToUser() {
		List<String> history = new ArrayList<>(sentToUserHistory);
		List<String> filteredHistory = new ArrayList<>();
		for (String message : history) {
			if (!message.contains(this.username + ":")) {
				filteredHistory.add(message);
			}
		}
		return filteredHistory;
		}
}
