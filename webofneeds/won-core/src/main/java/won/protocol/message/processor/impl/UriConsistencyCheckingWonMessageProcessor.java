package won.protocol.message.processor.impl;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.springframework.beans.factory.annotation.Autowired;

import won.protocol.exception.UriNodePathException;
import won.protocol.exception.WonMessageNotWellFormedException;
import won.protocol.exception.WonMessageProcessingException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessage.EnvelopePropertyCheckResult;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.WonMessageType;
import won.protocol.message.WonMessageUtils;
import won.protocol.message.processor.WonMessageProcessor;
import won.protocol.service.MessageRoutingInfoService;
import won.protocol.service.WonNodeInfo;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.WonRdfUtils;

/**
 * Checks if the event, graph or atom uri is well-formed according the node's
 * domain and its path conventions. Used on incoming messages. User: ypanchenko
 * Date: 23.04.2015
 */
public class UriConsistencyCheckingWonMessageProcessor implements WonMessageProcessor {
    @Autowired
    protected WonNodeInformationService wonNodeInformationService;
    @Autowired
    MessageRoutingInfoService messageRoutingInfoService;

    @Override
    public WonMessage process(WonMessage message) {
        if (message == null) {
            throw new WonMessageProcessingException("No WonMessage object found in exchange");
        }
        if (!WonMessageUtils.isValidMessageUri(message.getMessageURIRequired())) {
            throw new WonMessageNotWellFormedException("Not a valid message URI: " + message.getMessageURI());
        }
        EnvelopePropertyCheckResult result = message.checkEnvelopeProperties();
        if (!result.isValid()) {
            throw new WonMessageNotWellFormedException(result.getMessage());
        }
        Optional<URI> senderNode = messageRoutingInfoService.senderNode(message);
        Optional<URI> recipientNode = messageRoutingInfoService.recipientNode(message);
        if (!senderNode.isPresent()) {
            throw new WonMessageProcessingException(
                            "Cannot determine sender node for " + message.toShortStringForDebug());
        }
        if (!recipientNode.isPresent()) {
            throw new WonMessageProcessingException(
                            "Cannot determine recipient node for " + message.toShortStringForDebug());
        }
        WonNodeInfo senderNodeInfo = null;
        WonNodeInfo recipientNodeInfo = null;
        if (senderNode != null && !message.getMessageType().isHintMessage()) {
            // do not check the sender node for a hint
            // TODO: change this behaviour as soon as a matcher uses a WoN node
            senderNodeInfo = wonNodeInformationService.getWonNodeInformation(senderNode.get());
        }
        if (recipientNode != null) {
            recipientNodeInfo = wonNodeInformationService.getWonNodeInformation(recipientNode.get());
        }
        checkAtomUri(message.getSenderAtomURI(), senderNodeInfo);
        checkSocketUri(message.getSenderSocketURI(), senderNodeInfo);
        checkAtomUri(message.getRecipientAtomURI(), recipientNodeInfo);
        checkSocketUri(message.getRecipientSocketURI(), recipientNodeInfo);
        // there is no way atom or connection uri can be on the recipient node and the
        // recipient node is different from the sender node
        checkAtomUri(message.getAtomURI(), senderNodeInfo);
        checkConnectionUri(message.getConnectionURI(), senderNodeInfo);
        // Check that atom URI for create_atom message corresponds to local pattern
        checkCreateMsgAtomURI(message, senderNodeInfo);
        WonMessageDirection statedDirection = message.getEnvelopeType();
        if (statedDirection.isFromOwner()) {
            if (!Objects.equals(message.getSenderAtomURIRequired(), message.getSignerURIRequired())) {
                RDFDataMgr.write(System.out, message.getCompleteDataset(), Lang.TRIG);
                throw new WonMessageNotWellFormedException("WonMessage " + message.toShortStringForDebug()
                                + " is FROM_OWNER but not signed by its atom");
            }
        }
        if (statedDirection.isFromSystem()) {
            if (!Objects.equals(message.getSenderNodeURIRequired(), message.getSignerURIRequired())) {
                RDFDataMgr.write(System.out, message.getCompleteDataset(), Lang.TRIG);
                throw new WonMessageNotWellFormedException("WonMessage " + message.toShortStringForDebug()
                                + " is FROM_SYSTEM but not signed by its node");
            }
        }
        return message;
    }

    private void checkHasNode(final WonMessage message, URI localNode) {
        if (!localNode.equals(message.getSenderNodeURI()) && !localNode.equals(message.getRecipientNodeURI())) {
            throw new UriNodePathException("neither sender nor receiver is " + localNode);
        }
    }

    private void checkCreateMsgAtomURI(final WonMessage message, final WonNodeInfo nodeInfo) {
        // check only for create message
        if (message.getMessageType() == WonMessageType.CREATE_ATOM) {
            URI atomURI = WonRdfUtils.AtomUtils.getAtomURI(message.getCompleteDataset());
            checkAtomUri(atomURI, nodeInfo);
        }
    }

    private void checkAtomUri(URI atom, WonNodeInfo info) {
        if (atom == null) {
            return;
        }
        if (!atom.toString().startsWith(info.getAtomURIPrefix())) {
            throw new WonMessageNotWellFormedException(
                            atom + " is not a valid atom URI on node " + info.getWonNodeURI());
        }
    }

    private void checkSocketUri(URI socket, WonNodeInfo info) {
        if (socket == null) {
            return;
        }
        if (!WonMessageUtils.stripFragment(socket).toString().startsWith(info.getAtomURIPrefix())) {
            throw new WonMessageNotWellFormedException(
                            socket + " is not a valid socket URI on node " + info.getWonNodeURI());
        }
    }

    private void checkConnectionUri(URI connection, WonNodeInfo info) {
        if (connection == null) {
            return;
        }
        if (!connection.toString().startsWith(info.getConnectionURIPrefix())) {
            throw new WonMessageNotWellFormedException(
                            connection + " is not a valid connection URI on node " + info.getWonNodeURI());
        }
    }
}
