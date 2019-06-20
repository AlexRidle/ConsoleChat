package org.maksim.chakur.client;

import org.maksim.chakur.network.TCPConnectionListener;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*; //import of all package is a bad practice. Should import only what you need, nothing more.

//This class contains method to check is user name already in list. Also It adds a new user name into file or deletes it after use.
public class CheckNames { //name of class should be a noun (RegisterService)

    private static final String PATH = TCPConnectionListener.NAMESFILEPATH;

    //too much empty lines in method (almost every second line), not sure we need them
    //Return type of value should be stringBuilder, not string. In "removeName" and "addName" method we convert returned value back to stringBuilder.
    //method reads file and returns user names
    public static String readNames() {//Wrong name of method. I think we need add "FromFile" because we are working with it.
        StringBuilder builder = new StringBuilder();//Name of value should be more obvious.

        try (FileChannel fileChannel = (FileChannel) Files.newByteChannel(Paths.get(getPath()))) {

            long fileSize = fileChannel.size();

            MappedByteBuffer byteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileSize);

            for (int i = 0; i < fileSize; i++) {
                builder.append((char) byteBuffer.get(i));
            }

        } catch (InvalidPathException e) {
            System.out.println("File is not founded" +e);//Formatting! I don't think we need next empty line

        } catch (IOException e) {
            System.out.println("Input exception" +e);//Formatting!
        }

        return builder.toString().trim();
    }

    //method adds user name into file
    private static void writeNames(String names) {//Name of method. I think we need add "InFile" because we are working with it.
        try (FileChannel fileChannel = (FileChannel) Files.newByteChannel(Paths.get(getPath()),
                StandardOpenOption.WRITE, StandardOpenOption.READ, StandardOpenOption.CREATE)) {

            MappedByteBuffer byteBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE,0, names.length());//Formatting!

            byteBuffer.put(names.getBytes());
            byteBuffer.force();

        } catch (InvalidPathException e) {
            System.out.println("Incorrect path to file: " +e);//Formatting!

        } catch (IOException e) {
            System.out.println("Output exception" +e);//Formatting!
        }
    }

    //method adds new user name if it's unique
    public static synchronized boolean addName(String name) {
        StringBuilder builder = new StringBuilder(readNames());
        Set<String> namesSet = new HashSet<>(Arrays.asList(builder.toString().split(" ")));
        boolean isExisted = namesSet.contains(name);//name of value "isNonUnique" is more correct

        if (!isExisted) {
            builder.append(" ");
            builder.append(name);
            writeNames(builder.toString().trim());
        }

        return isExisted;
    }

    //method removes user name from file
    public static synchronized void removeName(String name) {
        StringBuilder namesBuilder = new StringBuilder(readNames());//I think name of value should be "namesFromFile"

        //All unused code must be deleted or if its really necessary code it should be commented.
        // Вариант удаления строки, переданной в качестве параметра
        /*LinkedList<String> namesList = new LinkedList<>(Arrays.asList(builder.toString().split(" ")));
        namesList.remove(name);
        builder.delete(0, builder.length());
        for (String s : namesList) {
            builder.append(s + " ");
        }*/

        if (namesBuilder != null && !namesBuilder.toString().equals("")) {//null checker is useless
        	namesBuilder.delete(namesBuilder.indexOf(name), namesBuilder.indexOf(name)+name.length()+1);//Formatting! Also can be easier to replace this line with regular expression.
            byte[]buff = namesBuilder.toString().getBytes();//Formatting!

            try(BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(Paths.get(getPath())))) {//Formatting!
                bos.write(buff);
                bos.flush();

            } catch (InvalidPathException e) {
                System.out.println("Incorrect path to file: " +e);//Formatting!
            } catch (IOException e) {
                System.out.println("Output exception" +e);//Formatting!
            }
        }
    }

    //method gets path of file that contains user names
    private static String getPath() {//what path? Name of the method should be more obvious, like "getPathOf...". This name would be good in another case or if we passed File obj. in params of this method.
        File file = new File(PATH);
        StringBuilder pathBuilder = new StringBuilder(); //stringBuilder is not necessary here. Can be a string
        //method of getting absolute path used twice. Think we need to store it in string variable. Unnecessary substring because of stringBuilder
        pathBuilder.append(file.getAbsolutePath().substring(0, file.getAbsolutePath().indexOf("\\MavenConsoleChat")));
        pathBuilder.append(PATH.substring(PATH.indexOf("/MavenConsoleChat")));
        return pathBuilder.toString();
    }
}
