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
import java.util.*;

// Класс для проверки совпадения имен при регистрации пользователя
public class CheckNames {
    private static final String PATH = TCPConnectionListener.NAMESFILEPATH;

    public static String readNames() {
        StringBuilder builder = new StringBuilder();

        try (FileChannel fileChannel = (FileChannel) Files.newByteChannel(Paths.get(getPath()))) {

            long fileSize = fileChannel.size();

            MappedByteBuffer byteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileSize);

            for (int i = 0; i < fileSize; i++) {
                builder.append((char) byteBuffer.get(i));
            }

        } catch (InvalidPathException e) {
            System.out.println("File is not founded" +e);

        } catch (IOException e) {
            System.out.println("Input exception" +e);
        }

        return builder.toString().trim();
    }

    private static void writeNames(String names) {
        try (FileChannel fileChannel = (FileChannel) Files.newByteChannel(Paths.get(getPath()),
                StandardOpenOption.WRITE, StandardOpenOption.READ, StandardOpenOption.CREATE)) {

            MappedByteBuffer byteBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE,0, names.length());

            byteBuffer.put(names.getBytes());
            byteBuffer.force();

        } catch (InvalidPathException e) {
            System.out.println("Incorrect path to file: " +e);

        } catch (IOException e) {
            System.out.println("Output exception" +e);
        }
    }

    public static synchronized boolean addName(String name) {
        StringBuilder builder = new StringBuilder(readNames());
        Set<String> namesSet = new HashSet<>(Arrays.asList(builder.toString().split(" ")));
        boolean isExisted = namesSet.contains(name);

        if (!isExisted) {
            builder.append(" ");
            builder.append(name);
            writeNames(builder.toString().trim());
        }

        return isExisted;
    }

    public static synchronized void removeName(String name) {
        StringBuilder namesBuilder = new StringBuilder(readNames());

        // Вариант удаления строки, переданной в качестве параметра
        /*LinkedList<String> namesList = new LinkedList<>(Arrays.asList(builder.toString().split(" ")));
        namesList.remove(name);
        builder.delete(0, builder.length());
        for (String s : namesList) {
            builder.append(s + " ");
        }*/
        
        if (namesBuilder != null && !namesBuilder.toString().equals("")) {
        	namesBuilder.delete(namesBuilder.indexOf(name), namesBuilder.indexOf(name)+name.length()+1);
            byte[]buff = namesBuilder.toString().getBytes();

            try(BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(Paths.get(getPath())))) {
                bos.write(buff);
                bos.flush();

            } catch (InvalidPathException e) {
                System.out.println("Incorrect path to file: " +e);
            } catch (IOException e) {
                System.out.println("Output exception" +e);
            }	
        }
    }

    private static String getPath() {
        File file = new File(PATH);
        StringBuilder pathBuilder = new StringBuilder();
        pathBuilder.append(file.getAbsolutePath().substring(0, file.getAbsolutePath().indexOf("/MavenConsoleChat")));
        pathBuilder.append(PATH.substring(PATH.indexOf("/MavenConsoleChat")));
        return pathBuilder.toString();
    }
}
