package telegrambot.model;

public enum UserState {
    NEW,
    WAITING_FOR_NAME,
    GENERAL_MENU,
    ACCOUNT,
    GENRE_SELECTION;

    public static UserState fromString(String stateString) {
        if (stateString == null) {
            return UserState.NEW;
        }

        try {
            return UserState.valueOf(stateString.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
    }
}
