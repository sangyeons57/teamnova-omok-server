package teamnova.omok.glue.game.session.interfaces.session;

import teamnova.omok.glue.data.model.UserData;

import java.util.List;
import java.util.Set;

public interface GameSessionParticipantsAccess extends  GameSessionAccessInterface {
    List<UserData> getUsers();
    List<String> getUserIds();
    boolean isReady(String userId);
    boolean markReady(String userId);
    boolean allReady();
    boolean markDisconnected(String userId);
    void clearDisconnected(String userId);
    Set<String> disconnectedUsersView();
    int playerIndexOf(String userId);
    boolean containsUser(String userId);
}
