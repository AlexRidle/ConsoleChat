import java.util.LinkedList;

// Класс для хранения пропущенных от клиентов сообщений
public class Story {
    private LinkedList<String> store = new LinkedList<>();

    public void addStory(String msg) {
        if (store.size() >= 5) {
            store.removeFirst();
            store.add(msg);
        } else {
            store.add(msg);
        }
    }

    public String printStory() {
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0; i < store.size(); i++) {
            stringBuffer.append(store.get(i));
            if (i < store.size()-1) {
                stringBuffer.append("\n");
            }
        }
        return stringBuffer.toString();
    }

    public LinkedList<String> getStore() {
        return store;
    }
}
