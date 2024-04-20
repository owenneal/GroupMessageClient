
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class GuiClient extends Application{


	//GUI components
	TextField messageField, groupNameField;
	Button submitButton, createGroupButton, newGroupButton, seeSentToUserButton;
	HashMap<String, Scene> sceneMap;
	Client clientConnection;
	ComboBox<String> recipientListComboBox;
	ComboBox<String> groupListComboBox;
	ListView<String> listItems2;
	List<String> currentRecipients = new ArrayList<>();
	ObservableList<String> connectedClients = FXCollections.observableArrayList();
	ObservableList<String> allGroupChats = FXCollections.observableArrayList();
	ComboBox<String> groupComboBox;
	Label recipientsLabel = new Label();
	Label groupLabel;
	ObservableList<String> groupList = FXCollections.observableArrayList();
	String currentGroup = "";
	boolean usernameAssigned;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		//create client connection
		clientConnection = new Client(data->{
				Platform.runLater(()->{
					//handle messages incoming
					if (data instanceof Message) {
						Message message = (Message) data;
						if (message.getType().equals("connectedClients")) {
							handleConnectedUsernames(message.getReceiver());
							if (currentGroup.equals("Global")) {
								listItems2.getItems().add("Connected clients: " + message.getReceiver());
							}
						}
						if (message.getType().equals("joined")) {
							if (currentGroup.equals("Global")) {
								listItems2.getItems().add(message.getSender() + message.getMessage());
							}
						}
						if (message.getType().equals("group")) {
							handleNewGroup(message.getGroupName());
						}
						if (message.getType().equals("message")) {
							if (currentGroup.equals(message.getGroupName())) {
								listItems2.getItems().add(message.getSender() + ": " + message.getMessage());
							}
						}
						if (message.getType().equals("usernameCheck")) {
                            usernameAssigned = message.getMessage().equals("Username available");
						}
						if (message.getType().equals("groupList")) {
							List<String> groupsCopy = new ArrayList<>(message.getReceiver());
							handleAllGroups(groupsCopy);
						}
					} else {
						listItems2.getItems().add(data.toString());
					}
			});
		});

		clientConnection.start();

		listItems2 = new ListView<>();
		messageField = new TextField();
		submitButton = new Button("Send");

		submitButton.setOnAction(e->{
			clientConnection.send(messageField.getText(), currentRecipients, groupLabel.getText());
			clientConnection.updateGroupChatHistory(groupLabel.getText(), messageField.getText());
			messageField.clear();
		});

		messageField.setPromptText("Enter message");

		TranslateTransition tt = new TranslateTransition(Duration.millis(200), messageField);
		tt.setByY(5);
		tt.setCycleCount(2);
		tt.setAutoReverse(true);

		messageField.focusedProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue) {
				tt.play();
			}
		});

		sceneMap = new HashMap<>();
		sceneMap.put("username", createUsernameScene(primaryStage));
		primaryStage.setOnCloseRequest(t -> {
            Platform.exit();
            System.exit(0);
        });

		primaryStage.setScene(sceneMap.get("username"));

		primaryStage.setTitle("Client");
		primaryStage.setResizable(false);
		primaryStage.show();
	}

	public Scene createClientGui(String username) {
		BorderPane borderPane = new BorderPane();

		Label usernameLabel = new Label(username);
		usernameLabel.setId("usernameLabel");
		recipientListComboBox = new ComboBox<>();
		groupListComboBox = new ComboBox<>();
		groupComboBox = new ComboBox<>();
		groupComboBox.setPromptText("All active groups");
		groupListComboBox.setPromptText("Select group");
		groupNameField = new TextField();
		groupNameField.setPromptText("Enter group name");
		recipientListComboBox.setPromptText("Select recipient");
		groupLabel = new Label();
		createGroupButton = new Button("Create Group");
		seeSentToUserButton = new Button("See messages sent to user");
		newGroupButton = new Button("New Group");

		//create property bindings for button disable
		SimpleBooleanProperty isGroupLabelEmpty = new SimpleBooleanProperty(true);
		SimpleBooleanProperty isGroupNameFieldEmpty = new SimpleBooleanProperty(true);
		SimpleBooleanProperty isRecipientListEmpty = new SimpleBooleanProperty(true);

		//listeners for labels/fields to determine if they are empty
		groupLabel.textProperty().addListener((observable, oldValue, newValue) -> {
			isGroupLabelEmpty.set(newValue.isEmpty());
		});
		groupNameField.textProperty().addListener((observable, oldValue, newValue) -> {
			isGroupNameFieldEmpty.set(newValue.isEmpty());
		});
		recipientsLabel.textProperty().addListener((observable, oldValue, newValue) -> {
			isRecipientListEmpty.set(newValue.isEmpty());
		});

		//disable buttons if fields are empty
		submitButton.disableProperty().bind(isGroupLabelEmpty);
		createGroupButton.disableProperty().bind(isGroupNameFieldEmpty);

		//active users combo box
		recipientListComboBox.setOnAction(e->{
			if (!currentRecipients.contains(username)) {
				currentRecipients.add(username);
			}
			String recipient = recipientListComboBox.getSelectionModel().getSelectedItem();
			if (recipient != null && !currentRecipients.contains(recipient)) {
				currentRecipients.add(recipient);
				updateRecipientLabel();
			}
		});

		groupListComboBox.setOnAction(e->{
			String group = groupListComboBox.getSelectionModel().getSelectedItem();
			if (group != null) {
				groupLabel.setText(group);
				currentGroup = group;
				currentRecipients.clear();
				currentRecipients.addAll(clientConnection.getGroupRecipients(group));
				updateRecipientLabel();
				populateGroupChatHistory(group);
				if (group.equals("Global")) {
					recipientsLabel.setText("Recipients: All");
				}
			}
		});

		//button to see messages sent to a user
		seeSentToUserButton.setOnAction(e->{
			List<String> history = clientConnection.getMessagesSentToUser();
			groupLabel.setText("Sent to user history");
			currentRecipients.clear();
			currentGroup = "History";
			recipientsLabel.setText("Recipient: user");
			groupListComboBox.getSelectionModel().clearSelection();
			if (!history.isEmpty()) {
				listItems2.getItems().clear();
				for (String message : history) {
					Platform.runLater(()->{
						listItems2.getItems().add(message);
					});
				}
			}
		});

		//creates a new group and saves the group + recipients
		createGroupButton.setOnAction(e->{
			String group = groupNameField.getText();
			if (!group.isEmpty()) {
				groupNameField.clear();
				groupLabel.setText(group);
				currentGroup = group;
				clientConnection.sendGroup(currentGroup, currentRecipients); //send group to server
				clientConnection.updateAvailableGroupChats(currentGroup, currentRecipients); //store group in client hashmap
				handleNewGroup(group);
				updateRecipientLabel();
			}
		});

		//clears the group name field and group label
		newGroupButton.setOnAction(e->{
			groupNameField.setDisable(false);
			groupLabel.setText("");
			currentGroup = "";
			currentRecipients.clear();
			updateRecipientLabel();
			recipientListComboBox.getSelectionModel().clearSelection();
			groupListComboBox.getSelectionModel().clearSelection();
		});

		HBox groupButtonBox = new HBox(10, createGroupButton, newGroupButton, seeSentToUserButton);
		groupButtonBox.setAlignment(Pos.CENTER);

		HBox topHBox = new HBox(10, usernameLabel, groupComboBox);
		topHBox.setAlignment(Pos.CENTER);
		topHBox.setPadding(new Insets(10));
		borderPane.setTop(topHBox);

		HBox groupHBox = new HBox(10, groupLabel, recipientsLabel);
		groupHBox.setAlignment(Pos.CENTER);
		VBox centerBox = new VBox(10, messageField, groupNameField, groupHBox, groupButtonBox, listItems2);
		centerBox.setPadding(new Insets(10));
		centerBox.setAlignment(Pos.CENTER);
		borderPane.setCenter(centerBox);

		HBox bottomHBox = new HBox(10, submitButton, recipientListComboBox, groupListComboBox);
		bottomHBox.setAlignment(Pos.CENTER);
		bottomHBox.setPadding(new Insets(10));
		borderPane.setBottom(bottomHBox);

		borderPane.setPadding(new Insets(20));
		Scene clientScene = new Scene(borderPane, 500, 500);
		clientScene.getStylesheets().add("file:src/main/style.css");
		return clientScene;
	}

	public Scene createUsernameScene(Stage primaryStage) {
		Label usernameLabel = new Label("Enter your username: ");
		TextField usernameField = new TextField();
		usernameField.setPromptText("Username");
		usernameField.setMaxWidth(200);
		Button userNameButton = new Button("Submit");
		VBox userBox = new VBox(10, usernameLabel, usernameField, userNameButton);
		userBox.setAlignment(Pos.CENTER);
		userNameButton.setOnAction(e->{
			String username = usernameField.getText();
			if (!username.isEmpty()) {
				CountDownLatch latch = new CountDownLatch(1);
				clientConnection.checkAvailableUserNames(username, latch);
				try {
					latch.await();
				} catch (InterruptedException interruptedException) {
					interruptedException.printStackTrace();
				}
			}
			if (clientConnection.getUserNameAssigned()) {
				clientConnection.updateUsername(username);
				clientConnection.sendUsername(username);
				sceneMap.put("client", createClientGui(username));
				primaryStage.setScene(sceneMap.get("client"));

			} else {
				usernameField.clear();
				Alert alert = new Alert(Alert.AlertType.ERROR);
				alert.setTitle("Error");
				alert.setHeaderText("Username already taken");
				alert.setContentText("Please enter a different username");
				alert.showAndWait();
			}
		});

		BorderPane pane = new BorderPane();
		pane.setCenter(userBox);
		Scene userScene = new Scene(pane, 300, 300);
		userScene.getStylesheets().add("file:src/main/style.css");


		return userScene;
	}

	public void handleConnectedUsernames(List<String> u) {
		Platform.runLater(()->{
			if (!u.isEmpty()) {
				this.connectedClients.clear();
				this.connectedClients.addAll(u);
				recipientListComboBox.setItems(connectedClients);
			}
		});
	}

	public void handleNewGroup(String groupName) {
		Platform.runLater(()->{
			if (!this.groupList.contains(groupName)) {
				this.groupList.add(groupName);
				groupListComboBox.setItems(groupList);
			}
		});
	}

	public void handleAllGroups(List<String> groups) {
		Platform.runLater(()->{
			if (!groups.isEmpty()) {
				this.allGroupChats.clear();
				this.allGroupChats.addAll(groups);
				groupComboBox.setItems(allGroupChats);
			}
		});
	}

	private void updateRecipientLabel() {
		StringBuilder recipientsText = new StringBuilder();
		for (String recipient : currentRecipients) {
			recipientsText.append(recipient).append(", ");
		}
		if (recipientsText.length() > 0) {
			recipientsText.deleteCharAt(recipientsText.length() - 2);//remove last comma and space
		}
		recipientsLabel.setText("Recipients: " + recipientsText);
	}

	public void populateGroupChatHistory(String groupName) {
		List<String> groupMessages = clientConnection.getGroupChatHistory(groupName);
		Platform.runLater(()->{
			listItems2.getItems().clear();
			for (String message : groupMessages) {
				listItems2.getItems().add(message);
			}
		});
	}

}

