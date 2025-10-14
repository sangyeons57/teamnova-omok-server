package teamnova.omok.application;

import java.util.List;

import teamnova.omok.handler.register.Type;
import teamnova.omok.message.encoder.ErrorMessageEncoder;
import teamnova.omok.message.encoder.GameSessionCompletedMessageEncoder;
import teamnova.omok.message.encoder.GameSessionPlayerDisconnectedMessageEncoder;
import teamnova.omok.message.encoder.GameSessionRematchStartedMessageEncoder;
import teamnova.omok.message.encoder.GameSessionStartedMessageEncoder;
import teamnova.omok.message.encoder.GameSessionTerminatedMessageEncoder;
import teamnova.omok.message.encoder.JoinSessionMessageEncoder;
import teamnova.omok.message.encoder.MoveAckMessageEncoder;
import teamnova.omok.message.encoder.PostGameDecisionAckMessageEncoder;
import teamnova.omok.message.encoder.PostGameDecisionPromptMessageEncoder;
import teamnova.omok.message.encoder.PostGameDecisionUpdateMessageEncoder;
import teamnova.omok.message.encoder.ReadyStateMessageEncoder;
import teamnova.omok.message.encoder.StonePlacedMessageEncoder;
import teamnova.omok.message.encoder.TurnTimeoutMessageEncoder;
import teamnova.omok.message.encoder.BoardSnapshotMessageEncoder;
import teamnova.omok.service.dto.MoveResult;
import teamnova.omok.service.dto.BoardSnapshotUpdate;
import teamnova.omok.service.dto.PostGameDecisionPrompt;
import teamnova.omok.service.dto.PostGameDecisionResult;
import teamnova.omok.service.dto.PostGameDecisionUpdate;
import teamnova.omok.service.dto.ReadyResult;
import teamnova.omok.service.dto.TurnTimeoutResult;
import teamnova.omok.domain.session.game.GameSession;
import teamnova.omok.domain.session.game.entity.turn.TurnSnapshot;

/**
 * Centralises encoding and delivery of session-scoped responses and broadcasts.
 */
public final class SessionMessagePublisher {
    private volatile SessionMessenger messenger = null;

    public void setMessageMessenger(SessionMessenger messenger) {
        this.messenger = messenger;
    }
    private boolean isReady() {
        return messenger != null;
    }

    boolean broadcastJoin(GameSession session) {
        messenger.broadcast(session, Type.JOIN_IN_GAME_SESSION, JoinSessionMessageEncoder.encode(session));
    }

    void broadcastReady(ReadyResult result) {
        messenger.broadcast(result.session(), Type.READY_IN_GAME_SESSION, ReadyStateMessageEncoder.encode(result));
    }

    void broadcastGameStart(GameSession session, TurnSnapshot turn) {
        messenger.broadcast(session, Type.GAME_SESSION_STARTED, GameSessionStartedMessageEncoder.encode(session, turn));
    }

    void broadcastStonePlaced(MoveResult result) {
        messenger.broadcast(result.session(), Type.STONE_PLACED, StonePlacedMessageEncoder.encode(result));
    }

    void broadcastTurnTimeout(GameSession session, TurnTimeoutResult result) {
        messenger.broadcast(session, Type.TURN_TIMEOUT, TurnTimeoutMessageEncoder.encode(result));
    }

    void broadcastBoardSnapshot(BoardSnapshotUpdate update) {
        messenger.broadcast(update.session(), Type.BOARD_SNAPSHOT, BoardSnapshotMessageEncoder.encode(update));
    }

    void broadcastGameCompleted(GameSession session) {
        messenger.broadcast(session, Type.GAME_SESSION_COMPLETED, GameSessionCompletedMessageEncoder.encode(session));
    }

    void broadcastPostGamePrompt(PostGameDecisionPrompt prompt) {
        messenger.broadcast(prompt.session(), Type.GAME_POST_DECISION_PROMPT, PostGameDecisionPromptMessageEncoder.encode(prompt));
    }

    void broadcastPostGameDecisionUpdate(PostGameDecisionUpdate update) {
        messenger.broadcast(update.session(), Type.GAME_POST_DECISION_UPDATE, PostGameDecisionUpdateMessageEncoder.encode(update));
    }

    void broadcastSessionTerminated(GameSession session, List<String> disconnected) {
        messenger.broadcast(session, Type.GAME_SESSION_TERMINATED, GameSessionTerminatedMessageEncoder.encode(session, disconnected));
    }

    void broadcastRematchStarted(GameSession previous, GameSession rematch, List<String> participants) {
        messenger.broadcast(previous, Type.GAME_SESSION_REMATCH_STARTED, GameSessionRematchStartedMessageEncoder.encode(previous, rematch, participants));
    }

    void broadcastPlayerDisconnected(GameSession session, String userId, String reason) {
        messenger.broadcast(session, Type.GAME_SESSION_PLAYER_DISCONNECTED, GameSessionPlayerDisconnectedMessageEncoder.encode(session, userId, reason));
    }

    void respondReady(String userId, long requestId, ReadyResult result) {
        messenger.respond(userId, Type.READY_IN_GAME_SESSION, requestId, ReadyStateMessageEncoder.encode(result));
    }

    void respondMove(String userId, long requestId, MoveResult result) {
        messenger.respond(userId, Type.PLACE_STONE, requestId, MoveAckMessageEncoder.encode(result));
    }

    void respondPostGameDecision(String userId, long requestId, PostGameDecisionResult result) {
        messenger.respond(userId, Type.POST_GAME_DECISION, requestId, PostGameDecisionAckMessageEncoder.encode(result));
    }

    void respondError(String userId, Type type, long requestId, String message) {
        messenger.respond(userId, type, requestId, ErrorMessageEncoder.encode(message));
    }
}
