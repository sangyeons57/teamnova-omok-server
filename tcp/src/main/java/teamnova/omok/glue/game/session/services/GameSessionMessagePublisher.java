package teamnova.omok.glue.game.session.services;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import teamnova.omok.glue.client.session.services.ClientSessionDirectory;
import teamnova.omok.glue.game.session.interfaces.GameSessionMessenger;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionAccess;
import teamnova.omok.glue.game.session.log.GameSessionLogger;
import teamnova.omok.glue.game.session.model.dto.TurnSnapshot;
import teamnova.omok.glue.game.session.model.messages.BoardSnapshotUpdate;
import teamnova.omok.glue.game.session.model.messages.PostGameDecisionPrompt;
import teamnova.omok.glue.game.session.model.messages.PostGameDecisionUpdate;
import teamnova.omok.glue.game.session.model.result.PostGameDecisionResult;
import teamnova.omok.glue.game.session.model.result.ReadyResult;
import teamnova.omok.glue.game.session.model.runtime.TurnPersonalFrame;
import teamnova.omok.glue.handler.register.Type;
import teamnova.omok.glue.message.encoder.BoardSnapshotMessageEncoder;
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

/**
 * Encodes and dispatches messages that relate to in-game session events.
 * All outbound traffic should flow through this publisher so we can maintain consistent logging.
 */
public final class GameSessionMessagePublisher implements GameSessionMessenger {
    private final ClientSessionDirectory directory;

    public GameSessionMessagePublisher(ClientSessionDirectory directory) {
        this.directory = Objects.requireNonNull(directory, "directory");
    }

    @Override
    public void broadcastJoin(GameSessionAccess session) {
        byte[] payload = JoinSessionMessageEncoder.encode(session);
        broadcast(session, Type.JOIN_IN_GAME_SESSION, payload);
    }

    @Override
    public void broadcastReady(GameSessionAccess session, ReadyResult result) {
        byte[] payload = ReadyStateMessageEncoder.encode(session, result);
        broadcast(session, Type.READY_IN_GAME_SESSION, payload);
    }

    @Override
    public void broadcastGameStart(GameSessionAccess session, TurnSnapshot turn) {
        byte[] payload = GameSessionStartedMessageEncoder.encode(session, turn);
        broadcast(session, Type.GAME_SESSION_STARTED, payload,
            turn != null ? "player=" + turn.currentPlayerId() : "player=-");
    }

    @Override
    public void broadcastStonePlaced(GameSessionAccess session, TurnPersonalFrame frame) {
        byte[] payload = StonePlacedMessageEncoder.encode(session, frame);
        broadcast(session, Type.STONE_PLACED, payload, String.format("user=%s x=%d y=%d", frame.userId(), frame.x(), frame.y()));
    }

    @Override
    public void broadcastTurnTimeout(GameSessionAccess session, TurnPersonalFrame frame) {
        byte[] payload = TurnTimeoutMessageEncoder.encode(session, frame);
        broadcast(session, Type.TURN_TIMEOUT, payload, "timedOut=" + frame.timeoutTimedOut());
    }

    @Override
    public void broadcastBoardSnapshot(GameSessionAccess session, BoardSnapshotUpdate update) {
        byte[] payload = BoardSnapshotMessageEncoder.encode(session, update);
        broadcast(session, Type.BOARD_SNAPSHOT, payload);
    }

    @Override
    public void broadcastGameCompleted(GameSessionAccess session) {
        byte[] payload = GameSessionCompletedMessageEncoder.encode(session);
        broadcast(session, Type.GAME_SESSION_COMPLETED, payload, "event=game-complete");
    }

    @Override
    public void broadcastPostGamePrompt(GameSessionAccess session, PostGameDecisionPrompt prompt) {
        byte[] payload = PostGameDecisionPromptMessageEncoder.encode(session, prompt);
        broadcast(session, Type.GAME_POST_DECISION_PROMPT, payload);
    }

    @Override
    public void broadcastPostGameDecisionUpdate(GameSessionAccess session, PostGameDecisionUpdate update) {
        byte[] payload = PostGameDecisionUpdateMessageEncoder.encode(session, update);
        broadcast(session, Type.GAME_POST_DECISION_UPDATE, payload);
    }

    @Override
    public void broadcastSessionTerminated(GameSessionAccess session, List<String> disconnected) {
        byte[] payload = GameSessionTerminatedMessageEncoder.encode(session, disconnected);
        String detail = !disconnected.isEmpty()
            ? "disconnected=" + String.join(",", disconnected)
            : "disconnected=none";
        broadcast(session, Type.GAME_SESSION_TERMINATED, payload, detail);
    }

    @Override
    public void broadcastRematchStarted(GameSessionAccess previous,
                                        GameSessionAccess rematch,
                                        List<String> participants) {
        byte[] payload = GameSessionRematchStartedMessageEncoder.encode(previous, rematch, participants);
        String detail = !participants.isEmpty()
            ? "participants=" + String.join(",", participants)
            : "participants=none";
        broadcast(previous, Type.GAME_SESSION_REMATCH_STARTED, payload, detail);
    }

    @Override
    public void broadcastPlayerDisconnected(GameSessionAccess session, String userId, String reason) {
        byte[] payload = GameSessionPlayerDisconnectedMessageEncoder.encode(session, userId, reason);
        broadcast(session, Type.GAME_SESSION_PLAYER_DISCONNECTED, payload,
            String.format("user=%s reason=%s", userId, reason));
    }

    @Override
    public void respondReady(String userId,
                             long requestId,
                             GameSessionAccess session,
                             ReadyResult result) {
        byte[] payload = ReadyStateMessageEncoder.encode(session, result);
        send(session, userId, Type.READY_IN_GAME_SESSION, requestId, payload);
    }

    @Override
    public void respondMove(String userId,
                            long requestId,
                            GameSessionAccess session,
                            TurnPersonalFrame frame) {
        byte[] payload = MoveAckMessageEncoder.encode(session, frame);
        String detail = String.format("status=%s x=%d y=%d", frame.outcomeStatus(), frame.x(), frame.y());
        send(session, userId, Type.PLACE_STONE, requestId, payload, detail);
    }

    @Override
    public void respondPostGameDecision(String userId,
                                        long requestId,
                                        PostGameDecisionResult result) {
        byte[] payload = PostGameDecisionAckMessageEncoder.encode(result);
        String detail = result != null
            ? "status=" + result.status()
            : "status=unknown";
        send(null, userId, Type.POST_GAME_DECISION, requestId, payload, detail);
    }

    @Override
    public void respondError(String userId, Type type, long requestId, String message) {
        byte[] payload = ErrorMessageEncoder.encode(message);
        String detail = message != null ? "error=" + message : "error=unknown";
        send(null, userId, type, requestId, payload, detail);
    }

    private void broadcast(GameSessionAccess session,
                           Type type,
                           byte[] payload,
                           String... details) {
        List<String> recipients = session != null ? session.getUserIds() : List.of();
        logOutbound(session, type, "broadcast", 0L, recipients, details);
        directory.broadcast(recipients, type, payload);
    }

    private void send(GameSessionAccess session,
                      String userId,
                      Type type,
                      long requestId,
                      byte[] payload,
                      String... details) {
        Collection<String> recipients =
            userId != null ? Collections.singletonList(userId) : Collections.emptyList();
        logOutbound(session, type, "send", requestId, recipients, details);
        directory.send(userId, type, requestId, payload);
    }

    private void logOutbound(GameSessionAccess session,
                             Type type,
                             String channel,
                             long requestId,
                             Collection<String> recipients,
                             String... details) {
        GameSessionLogger.outbound(session, type, channel, requestId, recipients, details);
    }
}
