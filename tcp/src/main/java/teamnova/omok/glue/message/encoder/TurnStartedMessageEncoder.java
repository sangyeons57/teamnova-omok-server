package teamnova.omok.glue.message.encoder;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import teamnova.omok.glue.data.model.UserData;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionAccess;
import teamnova.omok.glue.game.session.model.dto.TurnSnapshot;
import teamnova.omok.glue.rule.api.RuleId;

public final class TurnStartedMessageEncoder {
    private TurnStartedMessageEncoder() { }

    public static byte[] encode(GameSessionAccess session, TurnSnapshot snapshot) {
        StringBuilder sb = new StringBuilder(512);
        sb.append('{')
          .append("\"sessionId\":\"").append(session.sessionId().asUuid()).append('\"')
          .append(',')
          .append("\"serverTime\":\"").append(System.currentTimeMillis()).append('\"')
          .append(',')
          .append("\"meta\":{")
          .append("\"board\":{")
          .append("\"width\":").append(session.width()).append(',')
          .append("\"height\":").append(session.height())
          .append('}')
          .append(',')
          .append("\"rules\":");
        appendRuleIds(sb, session.getRuleIds());
        sb.append(',')
          .append("\"users\":[");
        appendUsers(sb, session);
        sb.append(']')
          .append('}')
          .append(',')
          .append("\"turn\":");

        if (snapshot == null) {
            sb.append("null");
        } else {
            sb.append('{')
                    .append("\"number\":").append(snapshot.turnNumber())
                    .append(',')
                    .append("\"round\":").append(snapshot.roundNumber())
                    .append(',')
                    .append("\"position\":").append(snapshot.positionInRound())
                    .append(',')
                    .append("\"playerIndex\":").append(snapshot.currentPlayerIndex())
                    .append(',')
                    .append("\"currentPlayerId\":");
            if (snapshot.currentPlayerId() == null) {
                sb.append("null");
            } else {
                sb.append('\"').append(MessageEncodingUtil.escape(snapshot.currentPlayerId())).append('\"');
            }
            sb.append(',')
                    .append("\"startAt\":").append(snapshot.turnStartAt())
                    .append(',')
                    .append("\"endAt\":").append(snapshot.turnEndAt())
                    .append('}');

        }
        sb.append('}');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void appendRuleIds(StringBuilder sb, List<RuleId> ruleIds) {
        sb.append('[');
        if (ruleIds != null) {
            for (int i = 0; i < ruleIds.size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append('\"').append(ruleIds.get(i).name()).append('\"');
            }
        }
        sb.append(']');
    }

    private static void appendUsers(StringBuilder sb, GameSessionAccess session) {
        List<UserData> users = session.getUsers();
        Set<String> disconnected = session.disconnectedUsersView();
        for (int i = 0; i < users.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            UserData user = users.get(i);
            boolean connected = !disconnected.contains(user.id());
            sb.append('{')
                    .append("\"userId\":\"").append(MessageEncodingUtil.escape(user.id())).append('\"')
                    .append(',')
                    .append("\"displayName\":\"").append(MessageEncodingUtil.escape(user.name())).append('\"')
                    .append(',')
                    .append("\"profileIconCode\":").append(user.profileIconCode())
                    .append(',')
                    .append("\"connected\":").append(connected)
                    .append('}');
        }
    }
}
