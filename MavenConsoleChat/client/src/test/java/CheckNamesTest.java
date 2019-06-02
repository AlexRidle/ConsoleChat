import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.*;

public class CheckNamesTest {
    private static final String PATH = TCPConnectionListener.NAMESFILEPATH;

    @Before
    @After
    public void setDown() throws Exception {
        File file = new File(PATH);
        StringBuilder pathBuilder = new StringBuilder();
        pathBuilder.append(file.getAbsolutePath().substring(0, file.getAbsolutePath().indexOf("/MavenConsoleChat")));
        pathBuilder.append(PATH.substring(PATH.indexOf("/MavenConsoleChat")));
        byte[] buff = "".getBytes();
        try(BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(Paths.get(pathBuilder.toString())))) {
            bos.write(buff);
            bos.flush();

        } catch (InvalidPathException e) {
            System.out.println("Incorrect path to file: " +e);
        } catch (IOException e) {
            System.out.println("Output exception" +e);
        }

    }

    @Test
    public void addName() {
        HashSet<String> names = new HashSet<>();
        StringBuilder actualStringBuilder = new StringBuilder();
        for (int i = 1; i <= 10; i+=3) {
            names.add("Name_" + i);
            CheckNames.addName("Name_" + i);
            actualStringBuilder.append("Name_" + i + " ");
        }

        ArrayList<Boolean> expectedList = new ArrayList<>();
        ArrayList<Boolean> actualList = new ArrayList<>();

        for (int i = 1; i <= 10; i++) {
            expectedList.add(CheckNames.addName("Name_" + i));
            String name = "Name_" + i;
            if (names.contains(name)) {
                actualList.add(true);
            } else {
                actualList.add(false);
                actualStringBuilder.append(name + " ");
            }
            names.add(name);
        }

        Assert.assertEquals(expectedList, actualList);

        String actualString = actualStringBuilder.toString().trim();
        String expectedString = CheckNames.readNames();

        Assert.assertEquals(expectedString, actualString);
    }

    @Test
    public void removeName() {
        LinkedList<String> namesList = new LinkedList<>();
        for (int i = 1; i <= 5; i++) {
            CheckNames.addName("Name_" + i);
            namesList.add("Name_" + i);
        }

        for (int i = 1; i < namesList.size(); i+=2) {
            CheckNames.removeName(namesList.remove(i));
        }
        CheckNames.removeName(namesList.remove(0));

        StringBuilder actualStringBuilder = new StringBuilder();
        for (String s : namesList) {
            actualStringBuilder.append(s);
            actualStringBuilder.append(" ");
        }

        String actualString = actualStringBuilder.toString().trim();
        String expectedString = CheckNames.readNames();

        Assert.assertEquals(expectedString, actualString);
    }
}