package telegrambot.bot;

public class BotMessages {
    // TODO: messages' text
    public static final String START_MESSAGE = "";
    public static final String GENERAL_MENU_MESSAGE = "";
    public static final String HELP_MESSAGE = "";

    public static String userGreetingMessage(String username) {
        return String.format("%s", username);
    }


}
