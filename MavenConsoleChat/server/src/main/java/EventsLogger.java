import org.apache.log4j.Logger;

public class EventsLogger {
    private static final Logger log = Logger.getLogger(ConsoleChatServer.class);

    public void logConnection(String name) {
        String msg = String.format("%s registered in the system.", name);
        log.info(msg);
    }

    public void logDisconnect(String name) {
        String msg = String.format("%s quitted the system.", name);
        log.info(msg);
    }

    public void logStartingChat(String agentName, String customsName) {
        String msg = String.format("%s started chatting with %s.", agentName, customsName);
        log.info(msg);
    }

    public void logLostMessages (String customsName, String message) {
        String msg = String.format("Messages from the %s remained unresponded: %s.", customsName, message);
        log.warn(msg);
    }

    public void logFinishingChat(String agentName, String customsName) {
        String msg = String.format("%s finished chatting with %s.", agentName, customsName);
        log.info(msg);
    }
}
