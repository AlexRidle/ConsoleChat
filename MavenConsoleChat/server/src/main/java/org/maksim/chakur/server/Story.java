package org.maksim.chakur.server;

import java.util.LinkedList;

// Класс для хранения пропущенных от клиентов сообщений
public class Story { //Wrong name of class. Must be named "MessageHistory".
    private LinkedList<String> store = new LinkedList<>();//Wrong name of variable. Must be named like "messageList".

    //Why do we keep only last 5 messages from user? Is it not better to keep them all?
    public void addStory(String msg) {//Wrong method name. Must be named like "addMessageToHistory".
        if (store.size() >= 5) {
            store.removeFirst();
            store.add(msg);
        } else {
            store.add(msg);
        }
    }

    public String printStory() {//Wrong method name because of wrong variable name.
        StringBuffer stringBuffer = new StringBuffer();//Why do we using stringBuffer?
        for (int i = 0; i < store.size(); i++) {
            stringBuffer.append(store.get(i));
            if (i < store.size()-1) {
                stringBuffer.append("\n");
            }
        }
        return stringBuffer.toString();
    }

    public LinkedList<String> getStore() {//Wrong method name because of wrong variable name.
        return store;
    }
}
