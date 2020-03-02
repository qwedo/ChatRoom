package com.javarush.task.Projects.ChatRoom;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>();

    public static void main(String[] args){
        int port = ConsoleHelper.readInt();
        try(ServerSocket ss = new ServerSocket(port)) {
            System.out.println("Сервер запущен.");
            while (true){
                Socket socket = ss.accept();
                new Handler(socket).start();
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public static void sendBroadcastMessage(Message message){
        for (Map.Entry<String, Connection> pair : connectionMap.entrySet()){
            try {
                pair.getValue().send(message);
            } catch (IOException e) {
                System.out.println("Не смогли отправить сообщение.");;
            }
        }
    }


    private static class Handler extends Thread {
        private Socket socket;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            ConsoleHelper.writeMessage("Установлено новое соединение с удаленным адресом: " + socket.getRemoteSocketAddress());
            String userName;
            try {
                Connection newConnection = new Connection(socket);
                userName = serverHandshake(newConnection);
                sendBroadcastMessage(new Message(MessageType.USER_ADDED, userName));
                notifyUsers(newConnection, userName);
                serverMainLoop(newConnection, userName);

                if(userName != null){
                    connectionMap.remove(userName);
                    sendBroadcastMessage(new Message(MessageType.USER_REMOVED, userName));
                }
                ConsoleHelper.writeMessage("Соединение с удаленным адресом закрыто.");
            } catch (IOException | ClassNotFoundException e) {
                ConsoleHelper.writeMessage("Произошла ошибка при обмене данными с удаленным адресом.");
            }
        }

        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException{
            Message messageFromClient = null;
            String clientName = null;
            do{
                connection.send(new Message(MessageType.NAME_REQUEST));
                messageFromClient = connection.receive();
                clientName = messageFromClient.getData();
            }
            while (messageFromClient.getType() != MessageType.USER_NAME || clientName.isEmpty() || connectionMap.containsKey(clientName));

            connectionMap.put(clientName, connection);
            connection.send(new Message(MessageType.NAME_ACCEPTED));
            return clientName;
        }

        private void notifyUsers(Connection connection, String userName) throws IOException{
            for (Map.Entry<String, Connection> pair : connectionMap.entrySet()){
                String nameClient = pair.getKey();
                if(!userName.equals(nameClient)){
                    Message message = new Message(MessageType.USER_ADDED, nameClient);
                    connection.send(message);
                }
            }
        }

        private void serverMainLoop(Connection connection, String userName) throws IOException, ClassNotFoundException{
            while (true){
                Message messageFromUser = connection.receive();
                if (messageFromUser.getType() == MessageType.TEXT){
                    Message messageToUsers = new Message(MessageType.TEXT, userName + ": " + messageFromUser.getData());
                    sendBroadcastMessage(messageToUsers);
                }
                else
                   ConsoleHelper.writeMessage("Something went wrong..");
            }
        }
    }
}
