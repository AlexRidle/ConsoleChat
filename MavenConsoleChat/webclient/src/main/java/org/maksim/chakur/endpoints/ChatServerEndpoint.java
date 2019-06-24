package org.maksim.chakur.endpoints;

import javax.websocket.*;//Do not import all classes.
import javax.websocket.server.ServerEndpoint;

import org.maksim.chakur.servlet.ClientService;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

@ServerEndpoint("/websocket")
public class ChatServerEndpoint {
    private Session session;//This value is not using.
    private static List<Session> sessionList = new LinkedList<>();//This list is not using.
    private String name;
	private String character;//Suspicious name. Better to name it like "Role"
	private ClientService clientService;
	private boolean isExecuted;

    @OnOpen
    public void onOpen (Session session) {
        this.session = session;
        sessionList.add(session);
        System.out.println("Client connected");
    }

    @OnClose
    public void onClose(Session session) throws IOException {
    	isExecuted = false;
    	session.getBasicRemote().sendText("You finished the session");
        sessionList.remove(session);
        this.session = null;
        this.clientService = null;
        System.out.println("Connection closed");
    }

    @OnError
    public void onError (Session session, Throwable throwable) {
        throwable.printStackTrace();
    }

    @OnMessage
    public void onMessage(Session session, String message) throws IOException {
    	if (message.startsWith("/register")) {
    		this.character = message.split(" ")[1];
    		this.name = message.split(" ")[2];
    		if (!this.character.equals("null") && this.name != null) {//this. is not necessary.
    			clientService = new ClientService(character, name);
            	clientService.createConnection();
            	isExecuted = true;
            	receiveMessages(session);
        		System.out.println("Client registered");
    		}
    	} else if (message.equals("/exit")) {
    		clientService.sendOutputMessage(message);
    		onClose(session);
    	} else {
    		if (clientService != null) {
    			System.out.println("User input: " + message);
                session.getBasicRemote().sendText(String.format("/%s %s: %s", character, name, message));
                try {
                	clientService.sendOutputMessage(message);   	
                } catch (NullPointerException e) { 
                	session.getBasicRemote().sendText("We are sorry, but server is not executed");
                }
    		} else {
    			session.getBasicRemote().sendText("<a href=http://localhost:8080/webclient/Login.html>form for login</a>");
    		}
    	}
    }

    //This method should be in another class.
    private void receiveMessages(Session session) {
        Thread threadReceiver = new Thread(new Runnable() {
            @Override
            public void run() {
                while (isExecuted) {
                	if (clientService != null) {
                		String msg = clientService.getMessage();
                		try {
                			if (msg != null) {
                				session.getBasicRemote().sendText(msg);
                			}
    					} catch (IOException e1) {
    						e1.printStackTrace();
    					}
                	}
                }
            }
        });
        threadReceiver.start();
    }
}
