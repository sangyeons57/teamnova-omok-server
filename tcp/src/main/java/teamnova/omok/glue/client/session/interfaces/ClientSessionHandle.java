package teamnova.omok.glue.client.session.interfaces;

import teamnova.omok.glue.client.session.interfaces.transport.ManagedSessionTransport;
import teamnova.omok.glue.client.session.interfaces.view.ClientSessionView;

public interface ClientSessionHandle extends ManagedSessionTransport, ClientSessionView {
}
