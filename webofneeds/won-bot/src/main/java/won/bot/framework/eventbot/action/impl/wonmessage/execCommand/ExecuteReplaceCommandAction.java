/*
 * Copyright 2012 Research Studios Austria Forschungsges.m.b.H. Licensed under
 * the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package won.bot.framework.eventbot.action.impl.wonmessage.execCommand;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.EventBotActionUtils;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.command.replace.ReplaceAbortedEvent;
import won.bot.framework.eventbot.event.impl.command.replace.ReplaceCommandEvent;
import won.bot.framework.eventbot.event.impl.command.replace.ReplaceCommandFailureEvent;
import won.bot.framework.eventbot.event.impl.command.replace.ReplaceCommandSuccessEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.FailureResponseEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.builder.WonMessageBuilder;
import won.protocol.util.AtomModelWrapper;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;

import java.lang.invoke.MethodHandles;
import java.net.URI;

/**
 * Action executing a CreateAtomCommandEvent, creating the specified atom.
 */
public class ExecuteReplaceCommandAction extends BaseEventBotAction {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public ExecuteReplaceCommandAction(final EventListenerContext eventListenerContext) {
        super(eventListenerContext);
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        if (!(event instanceof ReplaceCommandEvent)) {
            return;
        }
        ReplaceCommandEvent replaceCommandEvent = (ReplaceCommandEvent) event;
        Dataset atomDataset = replaceCommandEvent.getAtomDataset();
        if (atomDataset == null) {
            logger.warn("ReplaceCommandEvent did not contain an atom model, aborting atom creation");
            getEventListenerContext().getEventBus().publish(new ReplaceAbortedEvent(null, replaceCommandEvent,
                            "CreateAtomCommandEvent did not contain an atom model, aborting atom creation"));
            return;
        }
        Resource atomResource = WonRdfUtils.AtomUtils.getAtomResource(atomDataset);
        if (!atomResource.isURIResource()) {
            throw new IllegalArgumentException("atom resource in dataset is not an URI");
        }
        URI atomURI = URI.create(atomResource.getURI());
        RdfUtils.replaceBaseURI(atomDataset, atomResource.getURI(), true);
        RdfUtils.replaceBaseResource(atomDataset, atomResource, true);
        // AtomModelWrapper atomModelWrapper = new AtomModelWrapper(atomDataset);
        // URI wonNodeURI =
        // getEventListenerContext().getWonNodeInformationService().getWonNodeUri(atomURI);
        final URI wonNodeUri = getEventListenerContext().getWonNodeInformationService().getWonNodeUri(atomURI);
        logger.debug("replacing atom content on won node {} with content {} ", wonNodeUri,
                        StringUtils.abbreviate(RdfUtils.toString(atomDataset), 150));
        RdfUtils.renameResourceWithPrefix(atomDataset, atomResource.getURI(), atomURI.toString());
        WonMessage replaceMessage = createWonMessage(atomURI, wonNodeUri, atomDataset);
        replaceMessage = getEventListenerContext().getWonMessageSender().prepareMessage(replaceMessage);
        EventListener successCallback = event12 -> {
            logger.debug("atom content replacement successful for atom URI {}", atomURI);
            getEventListenerContext().getEventBus()
                            .publish(new ReplaceCommandSuccessEvent(atomURI, replaceCommandEvent));
        };
        EventListener failureCallback = event1 -> {
            String textMessage = WonRdfUtils.MessageUtils
                            .getTextMessage(((FailureResponseEvent) event1).getFailureMessage());
            logger.debug("atom content replacement failed for atom URI {}, original message URI {}: {}",
                            new Object[] { atomURI, ((FailureResponseEvent) event1).getOriginalMessageURI(),
                                            textMessage });
            getEventListenerContext().getEventBus()
                            .publish(new ReplaceCommandFailureEvent(atomURI, replaceCommandEvent, textMessage));
        };
        EventBotActionUtils.makeAndSubscribeResponseListener(replaceMessage, successCallback, failureCallback,
                        getEventListenerContext());
        logger.debug("registered listeners for response to message URI {}", replaceMessage.getMessageURI());
        getEventListenerContext().getWonMessageSender().sendMessage(replaceMessage);
        logger.debug("atom content replacement message sent with message URI {}", replaceMessage.getMessageURI());
    }

    private WonMessage createWonMessage(URI atomURI, URI wonNodeURI,
                    Dataset atomDataset) throws WonMessageBuilderException {
        RdfUtils.replaceBaseURI(atomDataset, atomURI.toString(), true);
        AtomModelWrapper atomModelWrapper = new AtomModelWrapper(atomDataset);
        return WonMessageBuilder
                        .replace()
                        .atom(atomURI)
                        .content().dataset(atomModelWrapper.copyDatasetWithoutSysinfo())
                        .build();
    }
}
