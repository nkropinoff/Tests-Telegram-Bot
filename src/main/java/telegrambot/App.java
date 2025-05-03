package telegrambot;

import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import telegrambot.bot.TestsTelegramBot;
import telegrambot.db.DataBaseManager;

public class App {
    public static void main(String[] args) {
        TestsTelegramBot testsTelegramBot = new TestsTelegramBot();

        try (TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication()) {
            botsApplication.registerBot(testsTelegramBot.getBotToken(), testsTelegramBot);

            System.out.println("telegrambot.bot.TestsTelegramBot successfully started!");
            Thread.currentThread().join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
