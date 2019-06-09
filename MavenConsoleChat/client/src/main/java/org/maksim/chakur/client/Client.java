package org.maksim.chakur.client;

import org.maksim.chakur.network.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Client implements TCPConnectionListener {
    private static final String HOST = "localhost";
    private static final int PORT = 19000;
    private String register;
    private String name;
    private BufferedReader reader;
    private Thread threadWriter;
    private boolean isExecuted;
    private String prefix = "";

    public static void main(String[] args) {
        new Client();
    }

    private Client() {
        try {
            reader = new BufferedReader(new InputStreamReader(System.in));
            new TCPConnection(this, HOST, PORT);
        } catch (IOException e) {
            System.out.println("TCPConnection Exception: " +e);
        }
    }

    @Override
    public void connectionReady(TCPConnection connection, String reg) {
        try {
            while (true) {
                System.out.print("Enter your name: ");
                reg = reader.readLine();
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

            while (true) {
                System.out.print("Are you agent - Y/N: ");
                reg = reader.readLine();
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
            connection.sendMessage("/register " + this.register + " " + this.name);
            createThreadWriter(connection);
        } catch (IOException e) {
            System.out.println("Console reading exception: " +e);
        }
    }

    @Override
    public void receiveMessage(TCPConnection connection, String msg) {
        if (msg.equals("/exit")) {
            CheckNames.removeName(this.name);
            disConnect(connection);
            return;
        }
        // Если строка получена от клиента
        if (msg.startsWith("/client")) {
            if (!"".equals(msg.substring(msg.indexOf(':')+1))) {
                System.out.println(msg);
            }
            // Формируем префиксную строку для идентификации нашего клиента на сервере
            String formatAgent = msg.substring(0, msg.indexOf(':')) + String.format("/%s %s: ", register, name);
            prefix = formatAgent;
            // Если строка получена от агента
        } else if (msg.startsWith("/agent")) {
            if (!"".equals(msg.substring(msg.indexOf(':')+1))) {
                System.out.println(msg);
            }
            // Формируем префиксную строку для идентификации нашего агента на сервере
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
        System.out.println("Client TCPConnection Exception: " +e);
    }

    private void createThreadWriter(TCPConnection connection) {
        isExecuted = true;
        threadWriter = new Thread(new Runnable() {
            @Override
            public void run() {
                while (isExecuted) {
                    try {
                        String msg = reader.readLine();
                        connection.sendMessage(prefix + msg.trim());
                        if (msg.equals("/exit")) {
                            isExecuted = false;
                        } else if (msg.equals("/leave")) {
                            prefix = "";
                        }
                    } catch (IOException e) {
                        isExecuted = false;
                        System.out.println("Console reading exception: " +e);
                    }
                }
            }
        });
        threadWriter.start();
    }
}
