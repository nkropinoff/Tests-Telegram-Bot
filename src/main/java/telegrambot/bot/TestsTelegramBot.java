package telegrambot.bot;

import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
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

    public SendMessage registrationMessage(long chat_id) {
        return SendMessage
                .builder()
                .chatId(chat_id)
                .text("Здарова орел, введи имя, путник, епта!!!")
                .build();
    }

    public SendMessage successfulRegistrationMessage(long chat_id) {
        return SendMessage
                .builder()
                .chatId(chat_id)
                .text("Твое имя принято, петух")
                .build();
    }

    public SendMessage menuMessage(long chat_id) {
        return SendMessage
                .builder()
                .chatId(chat_id)
                .text("Вот твое меню, петушара)")
                .replyMarkup(InlineKeyboardMarkup
                        .builder()
                        .keyboardRow(
                                new InlineKeyboardRow(
                                        InlineKeyboardButton
                                                .builder()
                                                .text("Аккаунт")
                                                .callbackData("update_msg_account")
                                                .build(),

                                        InlineKeyboardButton
                                                .builder()
                                                .text("Выбор теста")
                                                .callbackData("update_msg_chooseTest")
                                                .build()
                                )
                        )
                        .build())
                .build();
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
