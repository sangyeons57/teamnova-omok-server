package teamnova.omok.glue.game.session.services;

import java.util.List;
import java.util.Objects;

import teamnova.omok.glue.client.session.services.ClientSessionStore;
import teamnova.omok.glue.game.session.interfaces.GameBoardService;
import teamnova.omok.glue.game.session.interfaces.GameSessionMessenger;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionAccess;
import teamnova.omok.glue.game.session.GameSessionManager;
import teamnova.omok.glue.game.session.log.GameSessionLogger;
import teamnova.omok.glue.game.session.model.dto.TurnSnapshot;
import teamnova.omok.glue.game.session.model.messages.BoardSnapshotUpdate;
import teamnova.omok.glue.game.session.model.messages.PostGameDecisionPrompt;
import teamnova.omok.glue.game.session.model.messages.PostGameDecisionUpdate;
import teamnova.omok.glue.game.session.model.result.ReadyResult;
import teamnova.omok.glue.game.session.model.runtime.TurnPersonalFrame;
import teamnova.omok.glue.handler.register.Type;
import teamnova.omok.glue.message.encoder.BoardSnapshotMessageEncoder;
import teamnova.omok.glue.message.encoder.GameSessionCompletedMessageEncoder;
import teamnova.omok.glue.message.encoder.GameSessionPlayerDisconnectedMessageEncoder;
import teamnova.omok.glue.message.encoder.GameSessionRematchStartedMessageEncoder;
import teamnova.omok.glue.message.encoder.GameSessionStartedMessageEncoder;
import teamnova.omok.glue.message.encoder.GameSessionTerminatedMessageEncoder;
import teamnova.omok.glue.message.encoder.JoinSessionMessageEncoder;
import teamnova.omok.glue.message.encoder.PostGameDecisionPromptMessageEncoder;
import teamnova.omok.glue.message.encoder.PostGameDecisionUpdateMessageEncoder;
import teamnova.omok.glue.message.encoder.ReadyStateMessageEncoder;
import teamnova.omok.glue.message.encoder.TurnEndedMessageEncoder;
import teamnova.omok.glue.message.encoder.TurnStartedMessageEncoder;

/**
 * Encodes and dispatches messages that relate to in-game session events.
 * All outbound traffic should flow through this publisher so we can maintain consistent logging.
 */
public final class GameSessionMessagePublisher implements GameSessionMessenger {
    private final ClientSessionStore store;
    private final GameBoardService boardService;
    private final RuleService ruleService;

    public GameSessionMessagePublisher(ClientSessionStore store,
                                       teamnova.omok.glue.game.session.interfaces.GameBoardService boardService) {
        this.store = Objects.requireNonNull(store, "store");
        this.boardService = Objects.requireNonNull(boardService, "boardService");
        this.ruleService = RuleService.getInstance();
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
        byte[] payload = GameSessionStartedMessageEncoder.encode(session);
        broadcast(session, Type.GAME_SESSION_STARTED, payload,
            turn != null ? "player=" + turn.currentPlayerId() : "player=-");
    }

    @Override
    public void broadcastTurnStarted(GameSessionAccess session, TurnSnapshot snapshot) {
        byte[] payload = TurnStartedMessageEncoder.encode(session, snapshot);
        broadcast(session, Type.TURN_STARTED, payload,
            snapshot != null ? "player=" + snapshot.currentPlayerId() : "player=-");
    }

    @Override
    public void broadcastTurnEnded(GameSessionAccess session, TurnPersonalFrame frame) {
        byte[] payload = TurnEndedMessageEncoder.encode(session, frame);
        String detail;
        if (frame.timeoutTimedOut()) {
            detail = String.format("cause=TIMEOUT user=%s", frame.userId());
        } else if (frame.outcomeStatus() != null) {
            detail = String.format("cause=MOVE user=%s status=%s", frame.userId(), frame.outcomeStatus());
        } else {
            detail = "cause=UNKNOWN";
        }
        broadcast(session, Type.TURN_ENDED, payload, detail);
    }


    @Override
    public void broadcastBoardSnapshot(GameSessionAccess session) {
        byte[] boardBytes = boardService.snapshot(session);
        boardBytes = ruleService.transformBoard(session, boardBytes);
        BoardSnapshotUpdate update = new BoardSnapshotUpdate(boardBytes, System.currentTimeMillis());
        byte[] payload = BoardSnapshotMessageEncoder.encode(session, update);
        broadcast(session, Type.BOARD_UPDATED, payload);
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

    private void broadcast(GameSessionAccess session,
                           Type type,
                           byte[] payload,
                           String... details) {
        List<String> recipients = session != null ? session.getUserIds() : List.of();
        logOutbound(session, type, "broadcast", 0L, recipients, details);
        if (session != null) {
            broadcastToGame(session, recipients, type, payload);
        } else {
            broadcastDirect(recipients, type, payload);
        }
    }

    private void broadcastToGame(GameSessionAccess session,
                                 List<String> participants,
                                 Type type,
                                 byte[] payload) {
        if (participants == null || participants.isEmpty()) {
            return;
        }
        for (String uid : participants) {
            deliverInGame(session, uid, type, payload);
        }
    }

    private void broadcastDirect(List<String> recipients,
                                 Type type,
                                 byte[] payload) {
        if (recipients == null || recipients.isEmpty()) {
            return;
        }
        for (String uid : recipients) {
            deliver(uid, type, payload);
        }
    }

    private void deliver(String userId, Type type, byte[] payload) {
        store.findByUser(userId)
            .ifPresent(session -> session.enqueueResponse(type, 0L, payload));
    }

    private void deliverInGame(GameSessionAccess gameSession,
                               String userId,
                               Type type,
                               byte[] payload) {
        if (userId == null) {
            return;
        }
        store.findByUser(userId).ifPresent(handle -> {
            if (gameSession == null || gameSession.sessionId().equals(handle.currentGameSessionId())) {
                handle.enqueueResponse(type, 0L, payload);
            } else {
                GameSessionManager.getInstance().leaveByUser(userId);
            }
        });
    }

    private void logOutbound(GameSessionAccess session,
                             Type type,
                             String channel,
                             long requestId,
                             List<String> recipients,
                             String... details) {
        GameSessionLogger.outbound(session, type, channel, requestId, recipients, details);
    }
}
