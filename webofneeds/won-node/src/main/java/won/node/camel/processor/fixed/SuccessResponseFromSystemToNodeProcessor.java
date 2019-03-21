/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package won.node.camel.processor.fixed;

import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;
import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.node.camel.processor.general.OutboundMessageFactoryProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.message.processor.exception.WonMessageProcessingException;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;

/**
 * Processes responses to generated by the system and directed at the remote
 * side.
 */
@Component
@FixedMessageProcessor(direction = WONMSG.TYPE_FROM_SYSTEM_STRING, messageType = WONMSG.TYPE_SUCCESS_RESPONSE_STRING)
public class SuccessResponseFromSystemToNodeProcessor extends AbstractCamelProcessor {
  @Override
  public void process(Exchange exchange) throws Exception {
    WonMessage wonMessage = (WonMessage) exchange.getIn().getHeader(WonCamelConstants.MESSAGE_HEADER);
    // prepare the message to pass to the remote node
    URI remoteMessageUri = wonNodeInformationService.generateEventURI(wonMessage.getReceiverNodeURI());
    // add the information about the corresponding message to the local one
    wonMessage.addMessageProperty(WONMSG.HAS_CORRESPONDING_REMOTE_MESSAGE, remoteMessageUri);

    // the persister will pick it up later

    // put the factory into the outbound message factory header. It will be used to
    // generate the outbound message
    // after the wonMessage has been processed and saved, to make sure that the
    // outbound message contains
    // all the data that we also store locally
    OutboundMessageFactory outboundMessageFactory = new OutboundMessageFactory(remoteMessageUri);
    exchange.getIn().setHeader(WonCamelConstants.OUTBOUND_MESSAGE_FACTORY_HEADER, outboundMessageFactory);
  }

  private class OutboundMessageFactory extends OutboundMessageFactoryProcessor {

    public OutboundMessageFactory(URI messageURI) {
      super(messageURI);
    }

    @Override
    public WonMessage process(WonMessage message) throws WonMessageProcessingException {
      return WonMessageBuilder.setPropertiesForPassingMessageToRemoteNode(message, getMessageURI()).build();
    }
  }
}
