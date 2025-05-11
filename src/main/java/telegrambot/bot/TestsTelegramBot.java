package telegrambot.bot;

import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import telegrambot.db.DataBaseManager;
import telegrambot.model.UserState;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class TestsTelegramBot implements LongPollingSingleThreadUpdateConsumer {
    private final TelegramClient telegramClient;
    private final DataBaseManager dataBaseManager;
    private final String BOT_TOKEN;

    public TestsTelegramBot() {
        BOT_TOKEN = getBotTokenFromConfig();
        telegramClient = new OkHttpTelegramClient(BOT_TOKEN);
        dataBaseManager = new DataBaseManager();
    }

    @Override
    public void consume(Update update) {
        if (update.hasMessage() &&  update.getMessage().hasText()) {
            handleMessage(update.getMessage());

        } else if (update.hasCallbackQuery()) {
            handleCallback(update.getCallbackQuery());
        }
    }

    private void handleMessage(Message message) {
        long chat_id = message.getChatId();
        UserState state = dataBaseManager.getUserStateByChatId(chat_id);

        switch (state) {
            case UserState.NEW -> handleNewState(message);
            case UserState.WAITING_FOR_NAME -> handleWaitingForMessageState(message);
            case UserState.GENERAL_MENU -> handleGeneralMenuState(message);
            case UserState.ACCOUNT -> handleAccountState(message);
            case UserState.GENRE_SELECTION -> handleGenreSelectionState(message);
            // ...
            default -> handleUnknownState(message);
        }
    }

    private void handleCallback(CallbackQuery callbackQuery) {}

    private void handleNewState(Message message) {
        if (message.getText().equals(BotCommands.START_COMMAND)) {
            dataBaseManager.insertUserStateByChatId(message.getChatId(), UserState.WAITING_FOR_NAME);
            sendStartMessage(message.getChatId());
        } else {
            sendHelpMessage(message.getChatId());
        }
    }

    private void handleWaitingForMessageState(Message message) {
        String username = message.getText();
        dataBaseManager.updateUsernameByChatId(message.getChatId(), username);
        dataBaseManager.updateUserStateByChatId(message.getChatId(), UserState.GENERAL_MENU);
        sendGreetingMessage(message.getChatId(), username);
        // ... need to save messageId of last message of user in databse to delete it later
        // so need to create some methods and refactor db
    }

    private void sendStartMessage(long chat_id) {
        SendMessage message = SendMessage
                .builder()
                .chatId(chat_id)
                .text(BotMessages.START_MESSAGE)
                .build();
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendHelpMessage(long chat_id) {
        SendMessage message = SendMessage
                .builder()
                .chatId(chat_id)
                .text(BotMessages.HELP_MESSAGE)
                .build();
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendGreetingMessage(long chat_id, String username) {
        SendMessage message = SendMessage
                .builder()
                .chatId(chat_id)
                .text(BotMessages.userGreetingMessage(username))
                .build();
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public String getBotToken() {
        return BOT_TOKEN;
    }

    private String getBotTokenFromConfig() {
        String botToken = null;
        Properties properties = new Properties();

        try {
            FileInputStream fis = new FileInputStream("src/main/resources/config/bot.properties");
            properties.load(fis);
            botToken = properties.getProperty("bot.token");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return botToken;
    }
}
