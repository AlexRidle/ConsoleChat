package org.maksim.chakur.client;

import org.maksim.chakur.network.*;//import of all package is a bad practice. Should import only what you need, nothing more.

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Client implements TCPConnectionListener {
    //Host and port variables should be located in properties file.
    private static final String HOST = "localhost";
    private static final int PORT = 19000;
    private String register;//Very suspicious name. Must be named like "roleType", or use enum role type.
    private String name;
    private BufferedReader reader;//Not really good name for variable.
    private Thread threadWriter;//In this case better to use ExecutorService.
    private boolean isExecuted;
    private String prefix = "";

    //Application run method must be located in a different class.
    public static void main(String[] args) {
        new Client();
    }

    private Client() {
        try {
            reader = new BufferedReader(new InputStreamReader(System.in));
            new TCPConnection(this, HOST, PORT);
        } catch (IOException e) {
            System.out.println("TCPConnection Exception: " +e);//Formatting.
        }
    }

    @Override
    public void connectionReady(TCPConnection connection, String reg) {
        try {
            //This code should be in a separate method (registerName).
            //There is no checker for entered with spaces name.
            //The absence of a verification method can lead to errors.
            while (true) {
                System.out.print("Enter your name: ");
                reg = reader.readLine();
                //Better to use switch construction.
                if (reg.equals("")) {
                    System.out.println("Name can't be empty value, try again or finish the session - /exit");
                } else if (reg.trim().equals("/exit")) {
                    disConnect(connection);
                    return;
                } else {
                    if (CheckNames.addName(reg)) {
                        System.out.println("Such name is Existed, try again or finish the session - /exit");
                    } else {
                        this.name = reg;
                        break;
                    }
                }
            }

            //This code should be in a separate method (registerRole).
            while (true) {
                System.out.print("Are you agent - Y/N: ");
                reg = reader.readLine();
                //Better to use switch construction.
                if (reg.trim().equalsIgnoreCase("Y")) {
                    register = Register.AGENT.getTitle();
                    break;
                } else if (reg.trim().equalsIgnoreCase("N")) {
                    register = Register.CLIENT.getTitle();
                    break;
                } else if (reg.trim().equals("/exit")) {
                    CheckNames.removeName(this.name);
                    disConnect(connection);
                    return;
                } else {
                    System.out.println("Incorrect value, try again or finish the session - /exit");
                }
            }

            connection.sendMessage("/register " + this.register + " " + this.name);//Better to use String.format method.
            createThreadWriter(connection);
        } catch (IOException e) {
            System.out.println("Console reading exception: " +e);//Formatting.
        }
    }

    @Override
    public void receiveMessage(TCPConnection connection, String msg) {
        //Code duplicate. Should create method for /exit command.
        if (msg.equals("/exit")) {
            CheckNames.removeName(this.name);
            disConnect(connection);
            return;
        }
        // Если строка получена от клиента
        //Logic with prefix is not clear
        if (msg.startsWith("/client")) {
            //If message is not empty we will not printing it? Why not to
            //make checker on client side, to not let him send empty message?
            if (!"".equals(msg.substring(msg.indexOf(':')+1))) {
                System.out.println(msg);
            }
            // Формируем префиксную строку для идентификации нашего клиента на сервере
            //Don't need to create new string variable. Can be simplified.
            ///Value of this string will be like "client clientName/agent agentName: ".
            //Not clear, why is the prefix repeats companion prefix.
            String formatAgent = msg.substring(0, msg.indexOf(':')) + String.format("/%s %s: ", register, name);
            prefix = formatAgent;
            // Если строка получена от агента
        } else if (msg.startsWith("/agent")) {
            //Code duplicate. Check line 94.
            if (!"".equals(msg.substring(msg.indexOf(':')+1))) {
                System.out.println(msg);
            }
            // Формируем префиксную строку для идентификации нашего агента на сервере
            //Don't need to create new string variable. Can be simplified.
            String formatClient = msg.substring(0, msg.indexOf(':')) + String.format("/%s %s: ", register, name);
            prefix = formatClient;
        } else {
            System.out.println(msg);
            prefix = "";
        }
    }

    @Override
    public void disConnect(TCPConnection connection) {
        connection.disconnect();
    }

    @Override
    public void occurException(TCPConnection connection, Exception e) {
        System.out.println("Client TCPConnection Exception: " +e);//Formatting
    }

    //Method should be located in different class that implements runnable, and must be named like MessageSender.
    private void createThreadWriter(TCPConnection connection) {//Suspicious name of method.
        isExecuted = true;
        threadWriter = new Thread(new Runnable() {
            @Override
            public void run() {
                while (isExecuted) {
                    try {
                        String msg = reader.readLine();
                        //You should use something like JSON to send messages and other parameters.
                        //It is additional difficulty to parse it, if you writing properties in regular string line.
                        connection.sendMessage(prefix + msg.trim());
                        if (msg.equals("/exit")) {
                            isExecuted = false;
                        } else if (msg.equals("/leave")) {
                            prefix = "";
                        }
                    } catch (IOException e) {
                        isExecuted = false;
                        System.out.println("Console reading exception: " +e);//Formatting
                    }
                }
            }
        });
        threadWriter.start();
    }
}
