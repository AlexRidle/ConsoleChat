import java.io.IOException;
import java.net.ServerSocket;
import java.text.SimpleDateFormat;
import java.util.*;

public class ConsoleChatServer implements TCPConnectionListener {
    private static final int PORT = 19000;
    private HashMap<TCPConnection, String> customersConnections;
    private HashMap<TCPConnection, String> agentsConnections;
    private Deque<TCPConnection> customsDeque;
    private HashMap<TCPConnection, Story> missedMessages;
    private EventsLogger eventsLogger;

    public static void main(String[] args) throws IOException {
        new ConsoleChatServer();
    }

    private ConsoleChatServer () {
        System.out.println("Server is Running ...");
        customersConnections = new HashMap<>();
        agentsConnections = new HashMap<>();
        customsDeque = new LinkedList<>();
        missedMessages = new HashMap<>();
        eventsLogger = new EventsLogger();
        try (ServerSocket socket = new ServerSocket(PORT)) {
            // Принимаем входящие соединения в бесконечном цикле
            while (true) {
                try {
                    new TCPConnection(this, socket.accept());
                } catch (IOException e) {
                    System.out.println("TCPConnection exception: " +e);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized void connectionReady(TCPConnection connection, String reg) {
        if (reg != null && reg.startsWith("/register")) {
            if (reg.split(" ")[1].equals("client")) {
                String dtime = "(" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + ")";
                System.out.println(dtime + " Customer " + reg.split(" ")[2] + " is connected. Connection: " +connection);
                customersConnections.put(connection, reg.substring(10));
                // Логирование появления в сети клиента
                eventsLogger.logConnection(reg.substring(10));
                // поиск свободного агента
                TCPConnection agent = getAgent();
                if (agent != null) {
                    String agentName = agentsConnections.get(agent).substring(0, agentsConnections.get(agent).indexOf(" false"));
                    connection.sendMessage(String.format("/%s: Hello %s! You have been connected to console chat at %s. " +
                            "How can I help you? For quit the currently chat - /leave", agentName, reg.split(" ")[2], dtime));
                    // Логирование начала чата со свободным агентом
                    eventsLogger.logStartingChat(agentName, reg.substring(10));
                } else {
                    connection.sendMessage(dtime + " Congratulations, " + reg.split(" ")[2] + "! " +
                            "You are connected to console chat.");
                }
            } else if (reg != null && reg.split(" ")[1].equals("agent")) {
                String dtime = "(" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + ")";
                System.out.println(dtime + " Agent " + reg.split(" ")[2] + " is connected. Connection: " +connection);
                // Логирование появления в сети агента
                eventsLogger.logConnection(reg.substring(10));
                if (customsDeque.size() > 0) {
                    TCPConnection customer = customsDeque.removeFirst();
                    agentsConnections.put(connection, reg.substring(10) + " false");
                    String agentName = agentsConnections.get(connection).substring(0, agentsConnections.get(connection).indexOf(" false"));
                    connection.sendMessage(dtime + " Congratulations, " + reg.split(" ")[2] + "! " +
                            "You are connected like an agent. You have already had some history messages:");
                    Story story = missedMessages.get(customer);
                    connection.sendMessage(story.printStory());
                    customer.sendMessage("/" + agentName + ":");
                    missedMessages.remove(customer);
                    // Логирование начала чата с ожидающим клиентом
                    eventsLogger.logStartingChat(agentName, reg.substring(10));
                } else {
                    agentsConnections.put(connection, reg.substring(10) + " true");
                    connection.sendMessage(dtime + " Congratulations, " + reg.split(" ")[2] + "! " +
                            "You are connected like an agent.");
                }
            }
        }
    }

    @Override
    public synchronized void receiveMessage(TCPConnection connection, String msg) {
        if (msg != null) {
            if (msg.equals("/exit")) {
                if (customersConnections.containsKey(connection)) {
                    // логирование выхода клиента из системы
                    eventsLogger.logDisconnect(customersConnections.get(connection));
                    if (customsDeque.contains(connection)) {
                        Story story = missedMessages.get(connection);
                        // логирование пропущенных от клиента сообщений
                        eventsLogger.logLostMessages(customersConnections.get(connection), story.printStory());
                        customsDeque.remove(connection);
                        missedMessages.remove(connection);
                    }
                } else if (agentsConnections.containsKey(connection)) {
                    // логирование выхода агента из системы
                    eventsLogger.logDisconnect(agentsConnections.get(connection).replace(" true", ""));
                }
                disConnect(connection);
                connection.sendMessage(msg);
            } else if (msg.startsWith("/register")) {
                connectionReady(connection, msg);
            } else if (msg.startsWith("/client")) {
                for (Map.Entry<TCPConnection, String> pair : customersConnections.entrySet()) {
                    if (pair.getValue().equals(msg.substring(1, msg.indexOf("/agent")))) {
                        if (msg.endsWith("/exit")) {
                            msg = String.format("Unfortunately, %s %s has been disconnected. Try to connect with other agent " +
                                            "or input - /exit", agentsConnections.get(connection).split(" ")[0],
                                    agentsConnections.get(connection).split(" ")[1]);
                            pair.getKey().sendMessage(msg);
                            String agentsName = agentsConnections.get(connection).replace(" false", "");
                            // логирование выхода агента из системы
                            eventsLogger.logDisconnect(agentsName);
                            // логирование завершения чата с клиентом
                            eventsLogger.logFinishingChat(agentsName, customersConnections.get(pair.getKey()));

                            connection.sendMessage("/exit");
                            disConnect(connection);
                            return;
                        } else {
                            System.out.println(msg.substring(msg.indexOf("/agent")));
                            pair.getKey().sendMessage(msg.substring(msg.indexOf("/agent")));
                        }
                    }
                }
            } else if (msg.startsWith("/agent")) {
                // проходим в цикле по всем соединениям для агентов
                for (Map.Entry<TCPConnection, String> pair : agentsConnections.entrySet()) {
                    /*
                     если первая часть отправленного сообщения равна одному из значений из коллекции, значит сообщение,
                     будет адресовано данному агенту
                      */
                    if (pair.getValue().equals(msg.substring(1, msg.indexOf("/client")) + " false")) {
                        if (msg.endsWith("/exit")) {
                            // Агенту направляется уведомление о разрыве соединения с конкретным клиентом
                            msg = String.format("Unfortunately, %s %s has been disconnected. Try to connect with other client " +
                                            "or input - /exit", customersConnections.get(connection).split(" ")[0],
                                    customersConnections.get(connection).split(" ")[1]);
                            pair.getKey().sendMessage(msg);
                            String agentsName = agentsConnections.get(pair.getKey()).replace(" false", "");
                            // логирование выхода клиента из системы
                            eventsLogger.logDisconnect(customersConnections.get(connection));
                            // логирование завершения чата с клиентом
                            eventsLogger.logFinishingChat(agentsName, customersConnections.get(connection));

                            // Клиенту направляется ключевое слово exit для возможности остановки потока-слушателя клиента
                            connection.sendMessage("/exit");
                            // из клиентской коллекции удаляется информация о соединении с этим клиентом, от которого пришло письмо
                            disConnect(connection);
                            if (customsDeque.size() > 0) {
                                // Получаем первого клиента из очереди, от которого были пропущены сообщения
                                TCPConnection customer = customsDeque.removeFirst();
                                // Получаем экземпляр класса, в котором содержатся пропущенные сообщения
                                Story story = missedMessages.get(customer);
                                String agentName = pair.getValue().substring(0, pair.getValue().indexOf(" false"));
                                pair.getKey().sendMessage("You have had some history messages:");
                                // Направляем агенту пропущенные сообщения
                                pair.getKey().sendMessage(story.printStory());
                                // Клиенту направляем ссылку на агента для установления связи
                                customer.sendMessage("/" + agentName + ":");
                                // Удаляем из списка пропущенных сообщений
                                missedMessages.remove(customer);
                            } else {
                                // Агенту в коллекции устанавливается статус true (доступен)
                                pair.setValue(pair.getValue().replace("false", "true"));
                            }
                            return;

                        } else if (msg.endsWith("/leave")) {
                            connection.sendMessage("We were glad to help. For disconnect - /exit");
                            msg = String.format("Unfortunately, %s %s has finished this chat. Try to connect to other client " +
                                            "or input - /exit", customersConnections.get(connection).split(" ")[0],
                                    customersConnections.get(connection).split(" ")[1]);
                            pair.getKey().sendMessage(msg);
                            String agentsName = agentsConnections.get(pair.getKey()).replace(" false", "");
                            // логирование завершения чата с клиентом
                            eventsLogger.logFinishingChat(agentsName, customersConnections.get(connection));

                            if (customsDeque.size() > 0) {
                                TCPConnection customer = customsDeque.removeFirst();
                                Story story = missedMessages.get(customer);
                                String agentName = pair.getValue().substring(0, pair.getValue().indexOf(" false"));
                                pair.getKey().sendMessage("You have had some history messages:");
                                pair.getKey().sendMessage(story.printStory());
                                customer.sendMessage("/" + agentName + ":");
                                missedMessages.remove(customer);
                            } else {
                                pair.setValue(pair.getValue().replace("false", "true"));
                            }
                            return;

                        } else {
                            System.out.println(msg.substring(msg.indexOf("/client")));
                            pair.getKey().sendMessage(msg.substring(msg.indexOf("/client")));
                        }
                    }
                }
            } else {
                if (customersConnections.containsKey(connection)) {
                    System.out.println(customersConnections.get(connection)+ ": " + msg);
                    TCPConnection agent = getAgent();
                    if (agent != null) {
                        agent.sendMessage("/" + customersConnections.get(connection) + ": " + msg);
                        String agentName = agentsConnections.get(agent).substring(0, agentsConnections.get(agent).indexOf(" false"));
                        connection.sendMessage("/" +agentName + ":");
                        // Логирование начала чата со свободным агентом
                        eventsLogger.logStartingChat(agentName, customersConnections.get(connection));

                    } else {
                        if (!customsDeque.contains(connection)) {
                            // добавляем клиента в очередь, при отсутствии свободных клиентов
                            customsDeque.add(connection);
                            // добавляем клиента в колекцию, содержащую пропущенные от него сообщения
                            missedMessages.put(connection, new Story());
                        }
                        Story story = missedMessages.get(connection);
                        story.addStory("/" + customersConnections.get(connection) + ": " + msg);
                        // заносим в коллекцию пропущенные от клиента сообщения
                        missedMessages.put(connection, story);
                        connection.sendMessage("Unfortunately each of the agents are busy, you will be answered " +
                                "the first released operator. To finish the session - /exit");
                    }
                } else if (agentsConnections.containsKey(connection)) {
                    System.out.println(agentsConnections.get(connection).substring(0,
                            agentsConnections.get(connection).indexOf(" true")) + ": " + msg);
                    connection.sendMessage("There is' no available customers. To finish the session - /exit");
                }
            }
        }
    }

    @Override
    public synchronized void disConnect(TCPConnection connection) {
        if (customersConnections.containsKey(connection)) {
            String dtime = "(" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + ")";
            System.out.println(dtime + " User " + customersConnections.get(connection) + " is disconnected " +connection);
            customersConnections.remove(connection);
        } else if (agentsConnections.containsKey(connection)) {
            String dtime = "(" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + ")";
            System.out.println(dtime + " User " + agentsConnections.get(connection) + " is disconnected " +connection);
            agentsConnections.remove(connection);
        }
    }

    @Override
    public synchronized void occurException(TCPConnection connection, Exception e) {
        System.out.println("TCPConnection Exception: " +e);
    }

    private TCPConnection getAgent() {
        for (Map.Entry<TCPConnection, String> pair : agentsConnections.entrySet()) {
            if (pair.getValue().endsWith("true")) {
                pair.setValue(pair.getValue().replace("true", "false"));
                return pair.getKey();
            }
        }
        return null;
    }
}
