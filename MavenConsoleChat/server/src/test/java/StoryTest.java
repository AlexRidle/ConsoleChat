import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.maksim.chakur.server.Story;

import java.util.LinkedList;

public class StoryTest {
    private String[] arrayMsg;
    Story story;

    @Before
    public void setUp() throws Exception {
        arrayMsg = new String[7];
        for (int i = 0; i < arrayMsg.length; i++) {
            arrayMsg[i] = "/client Alex: " + (char)('A' + i);
        }

        story = new Story();
        for (String msg : arrayMsg) {
            story.addStory(msg);
        }
    }

    @Test
    public void addStory() {
        LinkedList<String> expected = story.getStore();

        LinkedList<String> actual = new LinkedList<>();
        for (int i = arrayMsg.length - 5; i < arrayMsg.length; i++) {
            actual.add(arrayMsg[i]);
        }

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void printStory() {
        String expected = story.printStory();

        StringBuilder stringBuilder = new StringBuilder();
        for (int i = arrayMsg.length - 5; i < arrayMsg.length; i++) {
            stringBuilder.append(arrayMsg[i] + "\n");
        }

        String actual = stringBuilder.substring(0, stringBuilder.length()-1);

        Assert.assertEquals(expected, actual);
    }
}