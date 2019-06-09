package org.maksim.chakur.network;

import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;

public class TCPConnection {
    private final Socket socket;
    /*
     Поток, слушающий входящие сообщения. У нас имеется один поток на каждом клиенте, который слушает входящее соединение,
     постоянно читает поток ввода, и если получена строка, то поток генерирует событие (у нас событийная система)
      */
    private final Thread threadListener;
    // слушатель событий
    private final TCPConnectionListener eventListener;
    private final BufferedReader in;
    private final BufferedWriter out;


    // Конструктор для создания сокета внутри, на основании IP-адреса и номера порта.
    public TCPConnection(TCPConnectionListener eventListener, String host, int port) throws IOException {
        this(eventListener, new Socket(host, port));
    }

    /*
    Конструктор принимает готовый объект сокета и создает с ним соединение. Принимает экземпляр слушателя событий,
    создающий соединение
     */
    public TCPConnection(TCPConnectionListener eventListener, Socket socket) throws IOException {
        this.eventListener = eventListener;
        this.socket = socket;
        // у объекта класса Socket получаем входящий поток, чтобы принимать байты
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), Charset.forName("UTF-8")));
        // у объекта класса Socket получаем исходящий поток, чтобы писать байты
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), Charset.forName("UTF-8")));
        // создаем новый поток, слушающий все входящие сообщения
        threadListener = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // когда поток стартовал, вызываем у объекта eventListener метод connectionReady и передаем в него себя
                    eventListener.connectionReady(TCPConnection.this, null);
                    // принимаем строки в бесконечном цикле
                    while (!threadListener.isInterrupted()) {
                        // получаем строку из потока ввода и передаем в метод receiveMessage
                        eventListener.receiveMessage(TCPConnection.this, in.readLine());
                    }
                } catch (IOException e) {
                    eventListener.occurException(TCPConnection.this, e);
                } finally {
                    eventListener.disConnect(TCPConnection.this);
                }

            }
        });
        threadListener.start();
    }

    // Метод для отправки сообщениий
    public synchronized void sendMessage(String msg) {
        try {
            out.write(msg + "\r\n");
            out.flush();
        } catch (IOException e) {
            eventListener.occurException(TCPConnection.this, e);
            disconnect();
        }
    }

    // метод для возможности разрыва соединения снаружи
    public synchronized void disconnect() {
        threadListener.interrupt();
        try {
            socket.close();
        } catch (IOException e) {
            eventListener.occurException(TCPConnection.this, e);
        }
    }

    @Override
    public String toString() {
        return "org.maksim.chakur.network.TCPConnection: " + socket.getInetAddress() + ": " + socket.getPort();
    }
}
