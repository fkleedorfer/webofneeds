package won.owner.service.impl;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import won.protocol.message.WonMessage;
import won.protocol.message.processor.WonMessageProcessor;
import won.protocol.message.sender.WonMessageSender;
import won.protocol.message.sender.exception.WonMessageSenderException;

/**
 * Service that connects client-side logic (e.g. the WonWebSocketHandler in
 * won-owner-webapp) with facilities for sending and receiving messages.
 */
public class OwnerApplicationService implements WonMessageProcessor, WonMessageSender {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Autowired
    @Qualifier("default")
    private WonMessageSender wonMessageSenderDelegate;
    // when the callback is a bean in a child context, it sets itself as a
    // dependency here
    // we don't do autowiring.
    private WonMessageProcessor messageProcessorDelegate = new NopOwnerApplicationServiceCallback();
    private Executor toOwnerExecutor = Executors.newSingleThreadExecutor();
    private Executor toNodeExecutor = Executors.newSingleThreadExecutor();

    @Override
    public WonMessage prepareMessage(WonMessage message) throws WonMessageSenderException {
        return wonMessageSenderDelegate.prepareMessage(message);
    }

    /**
     * Sends a message to the won node.
     * 
     * @param wonMessage
     */
    public void sendMessage(WonMessage wonMessage) {
        try {
            // send to node:
            toNodeExecutor.execute(() -> {
                wonMessageSenderDelegate.sendMessage(wonMessage);
            });
        } catch (Exception e) {
            // TODO: send error message back to client!
            logger.info("could not send WonMessage", e);
        }
    }

    @Override
    public void prepareAndSendMessage(WonMessage message) throws WonMessageSenderException {
        sendMessage(prepareMessage(message));
    }

    /**
     * Sends a message to the owner.
     * 
     * @param wonMessage
     * @return null - this is not a normal WonMessageProcessor, it does not return
     * an altered message.
     */
    @Override
    public WonMessage process(final WonMessage wonMessage) {
        toOwnerExecutor.execute(() -> {
            messageProcessorDelegate.process(wonMessage);
        });
        return null;
    }

    public void setMessageProcessorDelegate(final WonMessageProcessor messageProcessorDelegate) {
        this.messageProcessorDelegate = messageProcessorDelegate;
    }
}
