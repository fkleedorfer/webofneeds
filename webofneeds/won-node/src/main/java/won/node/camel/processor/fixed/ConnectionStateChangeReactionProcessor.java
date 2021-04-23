package won.node.camel.processor.fixed;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageReactionProcessor;
import won.node.camel.processor.general.ConnectionStateChangeBuilder;
import won.node.service.nodebehaviour.ConnectionStateChange;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.Atom;
import won.protocol.model.AtomState;
import won.protocol.model.Connection;

import java.util.Optional;

import static won.node.camel.service.WonCamelHelper.getConnection;

/**
 * Configured to react to any message, checking whether the message caused a
 * connection state change, then Compares the connection state found in the
 * header of the 'in' message with the state the connection is in now and
 * triggers the data derivation.
 */
@Component
@FixedMessageReactionProcessor()
public class ConnectionStateChangeReactionProcessor extends AbstractCamelProcessor {
    Logger logger = LoggerFactory.getLogger(getClass());

    public ConnectionStateChangeReactionProcessor() {
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        WonMessage wonMessage = (WonMessage) exchange.getIn().getHeader(WonCamelConstants.MESSAGE_HEADER);
        String msgTypeDir = "[message type: "
                        + wonMessage.getMessageType()
                        + ", direction: " + wonMessage.getEnvelopeType() + "]";
        ConnectionStateChangeBuilder stateChangeBuilder = (ConnectionStateChangeBuilder) exchange.getIn()
                        .getHeader(WonCamelConstants.CONNECTION_STATE_CHANGE_BUILDER_HEADER);
        if (stateChangeBuilder == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("no stateChangeBuilder found in exchange header, cannot check for state change "
                                + msgTypeDir);
            }
            return;
        }
        Optional<Connection> con = getConnection(exchange, connectionService);
        if (con.isPresent()) {
            if (!stateChangeBuilder.canBuild()) {
                stateChangeBuilder.newState(con.get().getState());
            }
        }
        // only if there is enough data to make a connectionStateChange object, make it
        // and pass it to the data
        // derivation service.
        if (stateChangeBuilder.canBuild()) {
            ConnectionStateChange connectionStateChange = stateChangeBuilder.build();
            Atom atom = atomService.getAtomRequired(con.get().getAtomURI());
            if ((connectionStateChange.isConnect() || connectionStateChange.isDisconnect())
                            && atom.getState() == AtomState.ACTIVE) {
                // trigger rematch
                matcherProtocolMatcherClient.atomModified(atom.getAtomURI(), null);
                logger.debug("matchers notified of connection state change {}", msgTypeDir);
            } else {
                logger.debug("no relevant connection state change, not notifying matchers {}", msgTypeDir);
            }
        } else {
            logger.debug("Could not collect ConnectionStateChange information, not checking for state change {}",
                            msgTypeDir);
        }
    }
}
