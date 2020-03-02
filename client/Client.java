package com.javarush.task.Projects.ChatRoom.client;

import com.javarush.task.Projects.ChatRoom.Connection;
import com.javarush.task.Projects.ChatRoom.ConsoleHelper;
import com.javarush.task.Projects.ChatRoom.Message;
import com.javarush.task.Projects.ChatRoom.MessageType;

import java.io.IOException;
import java.net.Socket;

public class Client {
    protected Connection connection;
    private volatile boolean clientConnected = false;

    public static void main(String[] args) {
        new Client().run();
    }

    public void run(){
        SocketThread socketThread = getSocketThread();
        socketThread.setDaemon(true);
        socketThread.start();
        synchronized (this){
            try {
                wait();
            } catch (InterruptedException e) {
                ConsoleHelper.writeMessage("Во время ожидания произошла ошибка.");
            }
        }

        if(clientConnected)
            ConsoleHelper.writeMessage("Соединение установлено. Для выхода наберите команду 'exit'.");
        else
            ConsoleHelper.writeMessage("Произошла ошибка во время работы клиента.");

        while (clientConnected){
            String textFormUser = ConsoleHelper.readString();
            if(textFormUser.equals("exit")) {
                break;
            }

            if(shouldSendTextFromConsole()) {
                sendTextMessage(textFormUser);
            }
        }
    }

    protected String getServerAddress(){
        ConsoleHelper.writeMessage("Введите адрес сервера:");
        return ConsoleHelper.readString();
    }

    protected int getServerPort(){
        ConsoleHelper.writeMessage("Введите порт сервера:");
        return ConsoleHelper.readInt();
    }

    protected String getUserName(){
        ConsoleHelper.writeMessage("Введите имя пользователя:");
        return ConsoleHelper.readString();
    }
    
    protected boolean shouldSendTextFromConsole(){
        return true;
    }

    protected SocketThread getSocketThread(){
        return new SocketThread();
    }

    protected void sendTextMessage(String text){
        try {
            connection.send(new Message(MessageType.TEXT, text));
        } catch (IOException e) {
            ConsoleHelper.writeMessage("Некорректный текст");
            clientConnected = false;
        }
    }

    //отвечает за поток, устанавливающий сокетное соединение и читающий сообщения сервера.
    public class SocketThread extends Thread{

        public void run(){
            try {
                Socket socket = new Socket(getServerAddress(), getServerPort());
                connection = new Connection(socket);
                clientHandshake();
                clientMainLoop();
            } catch (IOException | ClassNotFoundException e) {
                notifyConnectionStatusChanged(false);
            }

        }

        protected void clientHandshake() throws IOException, ClassNotFoundException{
            while (true){
                Message receiveMessage = connection.receive();
                if (receiveMessage.getType() == MessageType.NAME_REQUEST){
                    String userName = getUserName();
                    connection.send(new Message(MessageType.USER_NAME, userName));
                }
                else if (receiveMessage.getType() == MessageType.NAME_ACCEPTED){
                    notifyConnectionStatusChanged(true);
                    break;
                }
                else
                    throw new IOException("Unexpected MessageType");
            }
        }

        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            while (true) {
                Message messageFormServer = connection.receive();
                if (messageFormServer.getType() == MessageType.TEXT)
                    processIncomingMessage(messageFormServer.getData());
                else if (messageFormServer.getType() == MessageType.USER_ADDED)
                    informAboutAddingNewUser(messageFormServer.getData());
                else if (messageFormServer.getType() == MessageType.USER_REMOVED)
                    informAboutDeletingNewUser(messageFormServer.getData());
                else
                    throw new IOException("Unexpected MessageType");
            }
            
        }

        protected void processIncomingMessage(String message){
            ConsoleHelper.writeMessage(message);
        }

        protected void informAboutAddingNewUser(String userName){
            ConsoleHelper.writeMessage(userName + " подключился к чату");
        }

        protected void informAboutDeletingNewUser(String userName){ ConsoleHelper.writeMessage(userName + " покинул чат"); }

        protected void notifyConnectionStatusChanged(boolean clientConnected){
            Client.this.clientConnected = clientConnected;
            synchronized (Client.this){
                Client.this.notify();
            }
        }
    }
}
