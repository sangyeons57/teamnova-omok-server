package teamnova.omok.glue.handler;

import teamnova.omok.core.nio.FramedMessage;
import teamnova.omok.core.nio.NioReactorServer;
import teamnova.omok.glue.client.session.interfaces.ClientSessionHandle;
import teamnova.omok.glue.client.session.log.ClientMessageLogger;
import teamnova.omok.glue.data.model.JWTPayload;
import teamnova.omok.glue.data.model.JwtVerificationException;
import teamnova.omok.glue.handler.register.FrameHandler;
import teamnova.omok.glue.handler.register.Type;
import teamnova.omok.glue.manager.DataManager;
import teamnova.omok.glue.message.decoder.StringDecoder;

public class ReconnectingHandler implements FrameHandler {
    private final StringDecoder decoder;
    private final DataManager dataManager;

    public ReconnectingHandler(StringDecoder decoder, DataManager dataManager){
        this.decoder = decoder;
        this.dataManager = dataManager;
    }

    /*
    재연결 요청에 대해서 처리 하기위해 존제
    또한 요청 처리가 끝난 후에 극 결과를 전달하기위해 존제
     */
    @Override
    public void handle(NioReactorServer server, ClientSessionHandle session, FramedMessage frame) {
        ClientMessageLogger.inbound(session, Type.RECONNECTING, frame.requestId());
        String[] parts = decoder.decode(frame.payload()).split(":", 2);

        String jwt = parts[0];
        String gameSessionId = parts[1]; // 있는 경우 값이 있고 비어있을수도 있음

        if (jwt == null || jwt.isBlank()) {
            //ClientSessionManager.getInstance().onAuthenticationCleared(session);
            System.err.println("JWT payload missing");
            sendResult(server, session, frame, false);
            return;
        }

        try {
            JWTPayload payload = dataManager.verify(jwt.trim());

            sendResult(server, session, frame, true);
        } catch (JwtVerificationException e) {
            //ClientSessionManager.getInstance().onAuthenticationCleared(session);
            System.err.println("JWT verification failed: " + e.getMessage());
            sendResult(server, session, frame, false);
        }
    }

    private void sendResult(NioReactorServer server, ClientSessionHandle session, FramedMessage frame, boolean success) {
    }
}
