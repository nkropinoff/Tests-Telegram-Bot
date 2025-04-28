import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

public class TestsTelegramBot implements LongPollingSingleThreadUpdateConsumer {
    private final TelegramClient telegramClient;
    private UserStates userStates = new UserStates();

    public TestsTelegramBot(String botToken) {
        telegramClient = new OkHttpTelegramClient(botToken);
    }

    @Override
    public void consume(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {

            String message_text = update.getMessage().getText();
            long chat_id = update.getMessage().getChatId();

            if (update.getMessage().getText().equals("/start")) {
                SendMessage message = registrationMessage(chat_id);
                userStates.setUserState(chat_id, "waitingUserName");

                try {
                    telegramClient.execute(message);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else {

                // В зависимости от состояния, присылаем сообщение с определенным функционалом
                switch (userStates.getUserState(chat_id)) {
                    case "waitingUserName": {
                        userStates.setUserState(chat_id, "defaultState");

                        //Сообщение об успешной регистрации
                        SendMessage successfulRegistrationMsg = successfulRegistrationMessage(chat_id);
                        try {
                            telegramClient.execute(successfulRegistrationMsg);
                        } catch (TelegramApiException e) {
                            e.printStackTrace();
                        }

                        //Вывод меню с кнопками
                        SendMessage menuMsg = menuMessage(chat_id);
                        try {
                            telegramClient.execute(menuMsg);
                        } catch (TelegramApiException e) {
                            e.printStackTrace();
                        }

                    }
                }

            }

        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long chat_id = update.getCallbackQuery().getMessage().getChatId();

            SendMessage message = SendMessage
                    .builder()
                    .chatId(chat_id)
                    .text("Услышал, родный, но пока такой хуйни нет(")
                    .build();

            try {
                telegramClient.execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

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
}
