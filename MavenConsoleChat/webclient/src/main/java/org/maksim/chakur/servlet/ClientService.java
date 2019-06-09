package org.maksim.chakur.servlet;

import org.maksim.chakur.client.CheckNames;
import org.maksim.chakur.network.TCPConnection;
import org.maksim.chakur.network.TCPConnectionListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class ClientService implements TCPConnectionListener {
    private static final String HOST = "localhost";
    private static final int PORT = 19000;
    TCPConnection writer;
    private String register;
    private String name;
    private ArrayList<String> messages;
    private String prefix = "";
    private boolean valueSet = false;
    private boolean valueTake = false;

    public ClientService(String register, String name) {
        this.register = register;
        this.name = name;
        messages = new ArrayList<>();
    }

    @Override
    public void connectionReady(TCPConnection connection, String reg) {
        connection.sendMessage("/register " + this.register + " " + this.name);
        createWriter(connection);
    }

    @Override
    public void receiveMessage(TCPConnection connection, String msg) {
        if (msg.equals("/exit")) {
            CheckNames.removeName(this.name); // Метод работает только при указании полного пути к файлу
            disConnect(connection);
            return;
        }
        // Если строка получена от клиента
        if (msg.startsWith("/client")) {
            if (!"".equals(msg.substring(msg.indexOf(':')+1))) {
            	// Формируем префиксную строку для идентификации нашего клиента на сервере
                String formatAgent = msg.substring(0, msg.indexOf(':')) + String.format("/%s %s: ", register, name);
                prefix = formatAgent;
                synchronized(messages) {
                	while (valueSet) {
            			try {
        					messages.wait(1000);
        					if(valueTake == false) valueSet = false;
        				} catch (InterruptedException e) {
        					e.printStackTrace();
        				}
            		}
                	messages.add(msg);
                	valueSet = true;
                	messages.notify();
                }
            }
            
            // Если строка получена от агента
        } else if (msg.startsWith("/agent")) {
            if (!"".equals(msg.substring(msg.indexOf(':')+1))) {
            	// Формируем префиксную строку для идентификации нашего агента на сервере
                String formatClient = msg.substring(0, msg.indexOf(':')) + String.format("/%s %s: ", register, name);
                prefix = formatClient;
                synchronized(messages) {
                	while (valueSet) {
            			try {
        					messages.wait(1000);
        					if(valueTake == false) valueSet = false;
        				} catch (InterruptedException e) {
        					e.printStackTrace();
        				}
            		}
                	messages.add(msg);
                	valueSet = true;
                	messages.notify();
                }
            }
        } else {
        	prefix = "";
        	synchronized(messages) {
            	while (valueSet) {
        			try {
    					messages.wait(1000);
    					if(valueTake == false) valueSet = false;
    				} catch (InterruptedException e) {
    					e.printStackTrace();
    				}
        		}
            	messages.add(msg);
            	valueSet = true;
            	messages.notify();
            }
        }
    }

    @Override
    public void disConnect(TCPConnection connection) {
    	connection.disconnect();
    }

    @Override
    public void occurException(TCPConnection connection, Exception e) {
    	System.out.println("WebClient TCPConnection Exception: " +e);
    }

    public void createConnection() {
        try {
            new TCPConnection(this, HOST, PORT);
        } catch (IOException e) {
            System.out.println("TCPConnection Exception: " +e);
        }
    }

    public String getMessage() {
    	StringBuffer inputMessage = new StringBuffer();
    	synchronized (messages) {
    		valueTake = true;
    		while(!valueSet) {
    			try {
					messages.wait(TimeUnit.SECONDS.toMillis(20));
					if(messages.size() == 0) {
						messages.add("We are sorry, but no one can answer at this moment.");
						valueSet = true;
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
    		}
    		int i = 0;
        	while (i < messages.size()) {
        		inputMessage.append(messages.remove(i));
        		inputMessage.append("\r\n");
        	}
    		valueSet = false;
    		valueTake = false;
    		messages.notify();
    	}
        return inputMessage.toString();
    }
    
    private void createWriter(TCPConnection connection) {
    	this.writer = connection;
    }
    
    public void sendOutputMessage(String msg) {
    	this.writer.sendMessage(prefix + msg.trim());
    }
}
