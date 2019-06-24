package org.maksim.chakur.server;

import org.maksim.chakur.network.TCPConnection;
import org.maksim.chakur.network.TCPConnectionListener;

import java.io.IOException;
import java.net.ServerSocket;
import java.text.SimpleDateFormat;
import java.util.*;

public class ConsoleChatServer implements TCPConnectionListener {
    //Host and port variables should be located in properties file.
    private static final int PORT = 19000;
    private volatile HashMap<TCPConnection, String> customersConnections;
    private volatile HashMap<TCPConnection, String> agentsConnections;
    private volatile Deque<TCPConnection> customsDeque;
    private volatile HashMap<TCPConnection, Story> missedMessages;
    private EventsLogger eventsLogger;

    public static void main(String[] args) throws IOException {
        new org.maksim.chakur.server.ConsoleChatServer();
    }

    public ConsoleChatServer () {
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
    //Method is too large. Must be split on some methods.
    //Bad code readability.
    public synchronized void connectionReady(TCPConnection connection, String reg) {
        if (reg != null && reg.startsWith("/register")) {
            if (reg.split(" ")[1].equals("client")) {
                //Needs to put this code in getTimestamp method to remove code duplication.
                String dtime = "(" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + ")";
                System.out.println(dtime + " Customer " + reg.split(" ")[2] + " is connected. Connection: " +connection);
                // добавление нового подключения для клиента
                customersConnections.put(connection, reg.substring(10));
                // Логирование появления в сети клиента
                eventsLogger.logConnection(reg.substring(10));
                // поиск свободного агента
                TCPConnection agent = getAgent();
                if (agent != null) {
                    String clientName = customersConnections.get(connection);
                    // Направляем свободному агенту ссылку для установления связи с клиентом
                    agent.sendMessage(String.format("You started chatting with %s", clientName));
                    agent.sendMessage(String.format("/%s:", clientName));
                    // Проверяем, не разорвал ли соединение агент
                    if (agentsConnections.containsKey(agent)) {
                        String agentName = agentsConnections.get(agent).substring(0, agentsConnections.get(agent).indexOf(" false"));
                        connection.sendMessage(String.format("/%s: Hello %s! You have been connected to console chat at %s. " +
                                "How can I help you? For quit the currently chat - /leave", agentName, reg.split(" ")[2], dtime));
                        // Логирование начала чата со свободным агентом
                        eventsLogger.logStartingChat(agentName, reg.substring(10));
                    } else {
                        connection.sendMessage(dtime + " Congratulations, " + reg.split(" ")[2] + "! " +
                                "You are connected to console chat.");
                    }

                } else {
                    connection.sendMessage(dtime + " Congratulations, " + reg.split(" ")[2] + "! " +
                            "You are connected to console chat.");
                }
            } else if (reg != null && reg.split(" ")[1].equals("agent")) { //reg null checker is useless. We already checked it in condition higher.
                //Code duplication. Line 50.
                String dtime = "(" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + ")";
                System.out.println(dtime + " Agent " + reg.split(" ")[2] + " is connected. Connection: " +connection);
                // Логирование появления в сети агента
                eventsLogger.logConnection(reg.substring(10));

                // проверяем, есть ли не отвеченные письма
                if (customsDeque.size() > 0) {
                    // устанавливаем статус для агента - занят
                    agentsConnections.put(connection, reg.substring(10) + " false");
                    // получаем из карты по ключу имя агента
                    String agentName = agentsConnections.get(connection).substring(0, agentsConnections.get(connection).indexOf(" false"));
                    // проверяем в цикле не остались ли висеть в очереди клиенты, которые некорректно завершили соединение
                    while (customsDeque.size() > 0) {
                        // Получаем первого клиента из очереди, от которого были пропущены сообщения
                        TCPConnection customer = customsDeque.removeFirst();
                        // направляем клиенту информацию о свободном агенте для установления связи с ним
                        customer.sendMessage(String.format("The %s will connect with you in no time.", agentName));
                        customer.sendMessage(String.format("/%s:", agentName));
                        // проверяем, доступен ли еще клиент для связи
                        if (missedMessages.containsKey(customer)) {
                            // направляем агенту информацию о пропущенных от клиента письмах
                            connection.sendMessage(dtime + " Congratulations, " + reg.split(" ")[2] + "! " +
                                    "You are connected like an agent. You have already had some history messages:");
                            Story story = missedMessages.get(customer);
                            connection.sendMessage(story.printStory());
                            missedMessages.remove(customer);
                            // Логирование начала чата с ожидающим клиентом
                            eventsLogger.logStartingChat(agentName, reg.substring(10));
                            break;
                        }

                        if (customsDeque.size() == 0) {//we can move this checker out of the loop to remove unnecessary operation.
                            // устанавливаем статус для агента - доступен, при отсутствии не отвеченных писем
                            agentsConnections.replace(connection, agentsConnections.get(connection), reg.substring(10) + " true");
                            connection.sendMessage(dtime + " Congratulations, " + reg.split(" ")[2] + "! " +
                                    "You are connected like an agent.");

                        }
                    }
                } else {
                    agentsConnections.put(connection, reg.substring(10) + " true");
                    connection.sendMessage(dtime + " Congratulations, " + reg.split(" ")[2] + "! " +
                            "You are connected like an agent.");
                }
            }
        }
    }

    @Override
    //Method is too large. Must be split on some methods.
    //Bad code readability.
    public synchronized void receiveMessage(TCPConnection connection, String msg) {
        if (msg != null) {
            if (msg.equals("/exit")) {
                if (customersConnections.containsKey(connection)) {
                    eventsLogger.logDisconnect(customersConnections.get(connection)); // логирование выхода клиента из системы
                    if (customsDeque.contains(connection)) {
                        Story story = missedMessages.get(connection); // логирование пропущенных от клиента сообщений
                        eventsLogger.logLostMessages(customersConnections.get(connection), story.printStory());
                        customsDeque.remove(connection);
                        missedMessages.remove(connection);
                    }
                } else if (agentsConnections.containsKey(connection)) {
                    eventsLogger.logDisconnect(agentsConnections.get(connection).replace(" true", "")); // логирование выхода агента из системы
                }
                disConnect(connection);
                connection.sendMessage(msg);
            } else if (msg.startsWith("/register")) {
                connectionReady(connection, msg);
                // сообщение направлено от агента
            } else if (msg.startsWith("/client")) {
                for (Map.Entry<TCPConnection, String> pair : customersConnections.entrySet()) {
                    // из мапы по имени находим клиента, которому адресовано сообщение
                    if (pair.getValue().equals(msg.substring(1, msg.indexOf("/agent")))) {
                        //Much /exit checkers. Remove code duplication.
                        if (msg.endsWith("/exit")) {
                            msg = String.format("Unfortunately, %s %s has been disconnected. Try to connect with other agent " +
                                            "or input - /exit", agentsConnections.get(connection).split(" ")[0],
                                    agentsConnections.get(connection).split(" ")[1]);
                            // направляем клиенту сообщение о разорванном соединении
                            pair.getKey().sendMessage(msg);
                            String agentName = agentsConnections.get(connection).replace(" false", "");
                            // логирование выхода агента из системы
                            eventsLogger.logDisconnect(agentName);
                            // Проверка на Null
                            if (customersConnections.containsKey(pair.getKey())) {
                                // логирование завершения чата с клиентом
                                String clientName = customersConnections.get(pair.getKey());
                                eventsLogger.logFinishingChat(agentName, clientName);
                            }
                            // направляем агенту ключевое слово для корректного завершения соединения и освобождения ресурсов
                            connection.sendMessage("/exit");
                            // удаляем из мапы информацию о агенте
                            disConnect(connection);
                            return;

                        } else {
                            System.out.println(msg.substring(msg.indexOf("/agent")));
                            // направляем клиенту полученное от агента сообщение
                            pair.getKey().sendMessage(msg.substring(msg.indexOf("/agent")));
                            // проверяем целостность соединения на случай не корректного его завершения клиентом
                            if (!customersConnections.containsKey(pair.getKey())) {
                                // Отправляем агенту сообщение о некорректно прерванном соединении
                                connection.sendMessage(String.format("Unfortunately, %s incorrect interrupted this connection",
                                        msg.substring(0, msg.indexOf("/agent"))));
                                // в случае некорректного завершения соединения клиентом, проверяем список неотвеченных сообщений
                                if (customsDeque.size() > 0) {
                                    String agentName = agentsConnections.get(connection).replace(" false", "");
                                    while (customsDeque.size() > 0) {
                                        // Получаем первого клиента из очереди, от которого были пропущены сообщения
                                        TCPConnection customer = customsDeque.removeFirst();
                                        // Клиенту направляем ссылку на агента для установления связи
                                        customer.sendMessage(String.format("The %s will connect with you in no time.", agentName));
                                        customer.sendMessage(String.format("/%s:", agentName));
                                        if (missedMessages.containsKey(customer)) {
                                            connection.sendMessage("You have had some history messages:");
                                            // Получаем экземпляр класса, в котором содержатся пропущенные сообщения
                                            Story story = missedMessages.get(customer);
                                            // Направляем агенту пропущенные сообщения
                                            connection.sendMessage(story.printStory());
                                            // Удаляем из списка пропущенных сообщений
                                            missedMessages.remove(customer);
                                            break;
                                        }
                                        if (customsDeque.size() == 0) {
                                            agentsConnections.replace(connection, agentsConnections.get(connection),
                                                    agentsConnections.get(connection).replace("false", "true"));
                                        }
                                    }
                                } else {
                                    // Меняем статус на доступный
                                    agentsConnections.replace(connection, agentsConnections.get(connection),
                                            agentsConnections.get(connection).replace("false", "true"));
                                }
                                return;

                            }
                        }
                    }
                }
                // сообщение направлено от клиента
            } else if (msg.startsWith("/agent")) {
                // проходим в цикле по всем соединениям для агентов
                for (Map.Entry<TCPConnection, String> pair : agentsConnections.entrySet()) {
                     /* если первая часть отправленного сообщения равна одному из значений из коллекции, значит сообщение,
                     будет адресовано данному агенту */
                    if (pair.getValue().equals(msg.substring(1, msg.indexOf("/client")) + " false")) {
                        if (msg.endsWith("/exit")) {
                        	// Удаляем пропущенные сообщения от данного клиента, при их наличии
                        	if(customsDeque.contains(connection)) {
                        		customsDeque.remove(connection);
                        	}
                        	if(missedMessages.containsKey(connection)) {
                        		missedMessages.remove(connection);
                        	}
                            // Агенту направляется уведомление о разрыве соединения с конкретным клиентом
                            msg = String.format("Unfortunately, %s %s has been disconnected. Try to connect with other client " +
                                            "or input - /exit", customersConnections.get(connection).split(" ")[0],
                                    customersConnections.get(connection).split(" ")[1]);
                            pair.getKey().sendMessage(msg);
                            // проверка на NullPointerException, если агент некорректно завершил соединение
                            if (agentsConnections.containsKey(pair.getKey())) {
                                String agentName = agentsConnections.get(pair.getKey()).replace(" false", "");
                                // логирование завершения чата с клиентом
                                eventsLogger.logFinishingChat(agentName, customersConnections.get(connection));
                            }
                            // логирование выхода клиента из системы
                            eventsLogger.logDisconnect(customersConnections.get(connection));
                            // Клиенту направляется ключевое слово exit для возможности остановки потока-слушателя клиента
                            connection.sendMessage("/exit");
                            // из клиентской коллекции удаляется информация о соединении с этим клиентом, от которого пришло письмо
                            disConnect(connection);

                            //Code duplicate. Must be in different method.
                            if (agentsConnections.containsKey(pair.getKey()) && customsDeque.size() > 0) {
                                String agentName = pair.getValue().substring(0, pair.getValue().indexOf(" false"));
                                while (customsDeque.size() > 0) {
                                    // Получаем первого клиента из очереди, от которого были пропущены сообщения
                                    TCPConnection customer = customsDeque.removeFirst();
                                    // Клиенту направляем ссылку на агента для установления связи
                                    customer.sendMessage(String.format("The %s will connect with you in no time.", agentName));
                                    customer.sendMessage(String.format("/%s:", agentName));
                                    if (missedMessages.containsKey(customer)) {
                                        pair.getKey().sendMessage("You have had some history messages:");
                                        // Получаем экземпляр класса, в котором содержатся пропущенные сообщения
                                        Story story = missedMessages.get(customer);
                                        // Направляем агенту пропущенные сообщения
                                        pair.getKey().sendMessage(story.printStory());
                                        // Удаляем из списка пропущенных сообщений
                                        missedMessages.remove(customer);
                                        break;
                                    }
                                    if (customsDeque.size() == 0) {
                                        pair.setValue(pair.getValue().replace("false", "true"));
                                    }
                                }
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

                            // проверка на NullPointerException, если агент некорректно завершил соединение
                            if (agentsConnections.containsKey(pair.getKey())) {
                                String agentName = agentsConnections.get(pair.getKey()).replace(" false", "");
                                // логирование завершения чата с клиентом
                                eventsLogger.logFinishingChat(agentName, customersConnections.get(connection));
                            }

                            //Code duplicate. Line 253.
                            if (agentsConnections.containsKey(pair.getKey()) && customsDeque.size() > 0) {
                                String agentName = pair.getValue().substring(0, pair.getValue().indexOf(" false"));
                                while (customsDeque.size() > 0) {
                                    // Получаем первого клиента из очереди, от которого были пропущены сообщения
                                    TCPConnection customer = customsDeque.removeFirst();
                                    // Клиенту направляем ссылку на агента для установления связи
                                    customer.sendMessage(String.format("The %s will connect with you in no time.", agentName));
                                    customer.sendMessage(String.format("/%s:", agentName));
                                    if (missedMessages.containsKey(customer)) {
                                        pair.getKey().sendMessage("You have had some history messages:");
                                        // Получаем экземпляр класса, в котором содержатся пропущенные сообщения
                                        Story story = missedMessages.get(customer);
                                        // Направляем агенту пропущенные сообщения
                                        pair.getKey().sendMessage(story.printStory());
                                        // Удаляем из списка пропущенных сообщений
                                        missedMessages.remove(customer);
                                        break;
                                    }
                                    if (customsDeque.size() == 0) {
                                        pair.setValue(pair.getValue().replace("false", "true"));
                                    }
                                }
                            } else {
                                // Агенту в коллекции устанавливается статус true (доступен)
                                pair.setValue(pair.getValue().replace("false", "true"));
                            }
                            return;

                        } else {
                            // выводим полученное от клиента сообщение
                            System.out.println(msg.substring(msg.indexOf("/client")));
                            // направляем агенту сообщение от клиента
                            pair.getKey().sendMessage(msg.substring(msg.indexOf("/client")));
                            // проверка доступности соединения с агентом
                            if (!agentsConnections.containsKey(pair.getKey())) {
                                // При выброшенном исключении о недоступности соединения клиенту отправляется об этом информация
                                connection.sendMessage(String.format("Unfortunately, %s incorrect interrupted this connection",
                                        msg.substring(0, msg.indexOf("/client"))));
                                return;
                            }
                        }
                    }
                }

            } else {
                if (customersConnections.containsKey(connection)) {
                    System.out.println(customersConnections.get(connection)+ ": " + msg);
                    TCPConnection agent = getAgent();
                    if (agent != null && agentsConnections.containsKey(agent)) {
                        agent.sendMessage("/" + customersConnections.get(connection) + ": " + msg);
                        String agentName = agentsConnections.get(agent).substring(0, agentsConnections.get(agent).indexOf(" false"));
                        connection.sendMessage("/" +agentName + ":");
                        // Логирование начала чата со свободным агентом
                        eventsLogger.logStartingChat(agentName, customersConnections.get(connection));

                    } else {
                        if (!customsDeque.contains(connection)) {
                            // добавляем клиента в очередь, при отсутствии свободных агентов
                            customsDeque.add(connection);
                            // добавляем клиента в коллекцию, содержащую пропущенные от него сообщения
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
        //Code duplicate. Variable dtime used twice for same result. Write it out of the condition body.
        //Suspicious "if-elseif" construction.
        if (customersConnections.containsKey(connection)) {
            String dtime = "(" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + ")"; //Should create special method for a timestamp.
            System.out.println(dtime + " User " + customersConnections.get(connection) + " is disconnected " +connection);
            customersConnections.remove(connection);
        } else if (agentsConnections.containsKey(connection)) {
            String dtime = "(" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + ")"; //Should create special method for a timestamp.
            System.out.println(dtime + " User " + agentsConnections.get(connection) + " is disconnected " +connection);//Use String.format.
            agentsConnections.remove(connection);
        }
    }

    //Don't need to comment every line. If methods and variables named correctly, it already will be readable code.
    @Override
    public synchronized void occurException(TCPConnection connection, Exception e) {
        //Suspicious "if-elseif" construction.
        if (customersConnections.containsKey(connection)) {
            // логируем выход клиента из системы в результате возникшего исключения
            eventsLogger.logExceptionDisconnect(customersConnections.get(connection));
            // удаляем клиента из карты соединений
            customersConnections.remove(connection);
            if (missedMessages.containsKey(connection)) {
                Story story = missedMessages.get(connection);
                // логируем пропущенные сообщения
                eventsLogger.logLostMessages(customersConnections.get(connection), story.printStory());
                // очищаем список пропущенных сообщений
                missedMessages.remove(connection);
            }
        } else if (agentsConnections.containsKey(connection)) {
            // логируем выход клиента из системы в результате возникшего исключения
            eventsLogger.logExceptionDisconnect(agentsConnections.get(connection));
            // удаляем агента из карты соединений
            agentsConnections.remove(connection);
        }
        System.out.println("Server TCPConnection Exception: " +e);
    }

    //Name of method should be "getFreeAgent".
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
