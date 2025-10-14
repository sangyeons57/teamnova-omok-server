package teamnova.omok.glue.service;

import java.util.List;

import teamnova.omok.glue.handler.register.Type;
import teamnova.omok.glue.message.encoder.ErrorMessageEncoder;
import teamnova.omok.glue.message.encoder.GameSessionCompletedMessageEncoder;
import teamnova.omok.glue.message.encoder.GameSessionPlayerDisconnectedMessageEncoder;
import teamnova.omok.glue.message.encoder.GameSessionRematchStartedMessageEncoder;
import teamnova.omok.glue.message.encoder.GameSessionStartedMessageEncoder;
import teamnova.omok.glue.message.encoder.GameSessionTerminatedMessageEncoder;
import teamnova.omok.glue.message.encoder.JoinSessionMessageEncoder;
import teamnova.omok.glue.message.encoder.MoveAckMessageEncoder;
import teamnova.omok.glue.message.encoder.PostGameDecisionAckMessageEncoder;
import teamnova.omok.glue.message.encoder.PostGameDecisionPromptMessageEncoder;
import teamnova.omok.glue.message.encoder.PostGameDecisionUpdateMessageEncoder;
import teamnova.omok.glue.message.encoder.ReadyStateMessageEncoder;
import teamnova.omok.glue.message.encoder.StonePlacedMessageEncoder;
import teamnova.omok.glue.message.encoder.TurnTimeoutMessageEncoder;
import teamnova.omok.glue.message.encoder.BoardSnapshotMessageEncoder;
import teamnova.omok.glue.service.dto.MoveResult;
import teamnova.omok.glue.service.dto.BoardSnapshotUpdate;
import teamnova.omok.glue.service.dto.PostGameDecisionPrompt;
import teamnova.omok.glue.service.dto.PostGameDecisionResult;
import teamnova.omok.glue.service.dto.PostGameDecisionUpdate;
import teamnova.omok.glue.service.dto.ReadyResult;
import teamnova.omok.glue.service.dto.TurnTimeoutResult;
import teamnova.omok.glue.store.GameSession;

/**
 * Centralises encoding and delivery of session-scoped responses and broadcasts.
 */
final class SessionMessagePublisher {
    private final SessionMessenger messenger;

    SessionMessagePublisher(SessionMessenger messenger) {
        this.messenger = messenger;
    }

    void broadcastJoin(GameSession session) {
        messenger.broadcast(session, Type.JOIN_IN_GAME_SESSION, JoinSessionMessageEncoder.encode(session));
    }

    void broadcastReady(ReadyResult result) {
        messenger.broadcast(result.session(), Type.READY_IN_GAME_SESSION, ReadyStateMessageEncoder.encode(result));
    }

    void broadcastGameStart(GameSession session, TurnService.TurnSnapshot turn) {
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
