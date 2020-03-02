package com.javarush.task.Projects.ChatRoom.client;

import com.javarush.task.Projects.ChatRoom.ConsoleHelper;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class BotClient extends Client {

    public static void main(String[] args) {
        new BotClient().run();
    }

    @Override
    protected SocketThread getSocketThread() {
        return new BotSocketThread();
    }

    @Override
    protected boolean shouldSendTextFromConsole() {
        return false;
    }

    @Override
    protected String getUserName() {
        return "date_bot_" + (int) (Math.random() * 100);
    }

    public class BotSocketThread extends SocketThread{
        @Override
        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            sendTextMessage("Привет чату. Я бот. Понимаю команды: дата, день, месяц, год, время, час, минуты, секунды.");
            super.clientMainLoop();
        }

        @Override
        protected void processIncomingMessage(String message) {
            ConsoleHelper.writeMessage(message);
            if (!message.contains(": ")) return;
            String userName = message.substring(0, message.indexOf(':'));
            String textFromUser = message.substring(message.indexOf(":") + 2);
            SimpleDateFormat sdf = null;
            switch (textFromUser){
                case "дата":
                    sdf = new SimpleDateFormat("d.MM.YYYY");
                    break;
                case "день":
                    sdf = new SimpleDateFormat("d");
                    break;
                case "месяц":
                    sdf = new SimpleDateFormat("MMMM");
                    break;
                case "год":
                    sdf = new SimpleDateFormat("YYYY");
                    break;
                case "время":
                    sdf = new SimpleDateFormat("H:mm:ss");
                    break;
                case "час":
                    sdf = new SimpleDateFormat("H");
                    break;
                case "минуты":
                    sdf = new SimpleDateFormat("m");
                    break;
                case "секунды":
                    sdf = new SimpleDateFormat("s");
                    break;
            }
            if (sdf != null) {
                String respond = "Информация для " + userName + ": " + sdf.format(Calendar.getInstance().getTime());
                sendTextMessage(respond);
            }

        }
    }
}
