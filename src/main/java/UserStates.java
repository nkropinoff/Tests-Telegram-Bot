import java.util.HashMap;
import java.util.Map;

public class UserStates {
    Map<Long, String> states;

    public UserStates() {
        states = new HashMap<>();
    }

    public String getUserState(long chat_id) {
        return states.get(chat_id);
    }

    public void setUserState(long chat_id, String state) {
        states.put(chat_id, state);
    }

}
