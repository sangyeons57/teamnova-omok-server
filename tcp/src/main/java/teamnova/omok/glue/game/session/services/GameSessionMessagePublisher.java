package teamnova.omok.glue.game.session.services;

import java.util.List;

import teamnova.omok.glue.client.session.services.ClientSessionDirectory;
import teamnova.omok.glue.game.session.interfaces.GameSessionMessenger;
import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.model.GameSession;
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
import teamnova.omok.glue.game.session.model.messages.BoardSnapshotUpdate;
import teamnova.omok.glue.game.session.model.result.MoveResult;
import teamnova.omok.glue.game.session.model.messages.PostGameDecisionPrompt;
import teamnova.omok.glue.game.session.model.result.PostGameDecisionResult;
import teamnova.omok.glue.game.session.model.messages.PostGameDecisionUpdate;
import teamnova.omok.glue.game.session.model.result.ReadyResult;
import teamnova.omok.glue.game.session.model.result.TurnTimeoutResult;

/**
 * Encodes/dispatches messages that relate to in-game session events.
 */
public final class GameSessionMessagePublisher implements GameSessionMessenger {
    private final ClientSessionDirectory directory;

    public GameSessionMessagePublisher(ClientSessionDirectory directory) {
        this.directory = directory;
    }

    @Override
    public void broadcastJoin(GameSession session) {
        directory.broadcast(session.getUserIds(), Type.JOIN_IN_GAME_SESSION, JoinSessionMessageEncoder.encode(session));
    }

    @Override
    public void broadcastReady(ReadyResult result) {
        directory.broadcast(result.session().getUserIds(), Type.READY_IN_GAME_SESSION, ReadyStateMessageEncoder.encode(result));
    }

    @Override
    public void broadcastGameStart(GameSession session, GameTurnService.TurnSnapshot turn) {
        directory.broadcast(session.getUserIds(), Type.GAME_SESSION_STARTED, GameSessionStartedMessageEncoder.encode(session, turn));
    }

    @Override
    public void broadcastStonePlaced(MoveResult result) {
        directory.broadcast(result.session().getUserIds(), Type.STONE_PLACED, StonePlacedMessageEncoder.encode(result));
    }

    @Override
    public void broadcastTurnTimeout(GameSession session, TurnTimeoutResult result) {
        directory.broadcast(session.getUserIds(), Type.TURN_TIMEOUT, TurnTimeoutMessageEncoder.encode(result));
    }

    @Override
    public void broadcastBoardSnapshot(BoardSnapshotUpdate update) {
        directory.broadcast(update.session().getUserIds(), Type.BOARD_SNAPSHOT, BoardSnapshotMessageEncoder.encode(update));
    }

    @Override
    public void broadcastGameCompleted(GameSession session) {
        directory.broadcast(session.getUserIds(), Type.GAME_SESSION_COMPLETED, GameSessionCompletedMessageEncoder.encode(session));
    }

    @Override
    public void broadcastPostGamePrompt(PostGameDecisionPrompt prompt) {
        directory.broadcast(prompt.session().getUserIds(), Type.GAME_POST_DECISION_PROMPT, PostGameDecisionPromptMessageEncoder.encode(prompt));
    }

    @Override
    public void broadcastPostGameDecisionUpdate(PostGameDecisionUpdate update) {
        directory.broadcast(update.session().getUserIds(), Type.GAME_POST_DECISION_UPDATE, PostGameDecisionUpdateMessageEncoder.encode(update));
    }

    @Override
    public void broadcastSessionTerminated(GameSession session, List<String> disconnected) {
        directory.broadcast(session.getUserIds(), Type.GAME_SESSION_TERMINATED, GameSessionTerminatedMessageEncoder.encode(session, disconnected));
    }

    @Override
    public void broadcastRematchStarted(GameSession previous, GameSession rematch, List<String> participants) {
        directory.broadcast(previous.getUserIds(), Type.GAME_SESSION_REMATCH_STARTED, GameSessionRematchStartedMessageEncoder.encode(previous, rematch, participants));
    }

    @Override
    public void broadcastPlayerDisconnected(GameSession session, String userId, String reason) {
        directory.broadcast(session.getUserIds(), Type.GAME_SESSION_PLAYER_DISCONNECTED, GameSessionPlayerDisconnectedMessageEncoder.encode(session, userId, reason));
    }

    @Override
    public void respondReady(String userId, long requestId, ReadyResult result) {
        directory.send(userId, Type.READY_IN_GAME_SESSION, requestId, ReadyStateMessageEncoder.encode(result));
    }

    @Override
    public void respondMove(String userId, long requestId, MoveResult result) {
        directory.send(userId, Type.PLACE_STONE, requestId, MoveAckMessageEncoder.encode(result));
    }

    @Override
    public void respondPostGameDecision(String userId, long requestId, PostGameDecisionResult result) {
        directory.send(userId, Type.POST_GAME_DECISION, requestId, PostGameDecisionAckMessageEncoder.encode(result));
    }

    @Override
    public void respondError(String userId, Type type, long requestId, String message) {
        directory.send(userId, type, requestId, ErrorMessageEncoder.encode(message));
    }
}
