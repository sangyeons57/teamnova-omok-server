package teamnova.omok.glue.game.session.interfaces.session;

import teamnova.omok.glue.game.session.model.vo.GameSessionId;

import java.util.concurrent.locks.ReentrantLock;

public interface GameSessionAccessInterface {
    GameSessionId sessionId();
    ReentrantLock lock();
}
