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
package won.protocol.message;

import won.protocol.exception.WonMessageProcessingException;
import won.protocol.util.linkeddata.uriresolver.WonRelativeUriHelper;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

/**
 * Utilities for working with wonMessage objects.
 */
public class WonMessageUtils {
    public static Optional<URI> getSenderAtomURI(WonMessage msg) {
        URI atomUri = msg.getSenderAtomURI();
        if (atomUri == null) {
            atomUri = getSenderAtomURIFromSenderSocketURI(msg, atomUri);
        }
        return Optional.ofNullable(atomUri);
    }

    public static URI getSenderAtomURIFromSenderSocketURI(WonMessage msg, URI atomUri) {
        URI socketUri = msg.getSenderSocketURI();
        if (socketUri != null) {
            atomUri = WonRelativeUriHelper.stripFragment(socketUri);
        }
        return atomUri;
    }

    public static URI getSenderAtomURIRequired(WonMessage msg) {
        return getSenderAtomURI(msg)
                        .orElseThrow(() -> new WonMessageProcessingException("Could not obtain sender atom uri", msg));
    }

    public static Optional<URI> getRecipientAtomURI(WonMessage msg) {
        URI atomUri = msg.getRecipientAtomURI();
        if (atomUri == null) {
            atomUri = getRecipientAtomURIFromRecipientSocketURI(msg);
        }
        return Optional.ofNullable(atomUri);
    }

    public static URI getRecipientAtomURIFromRecipientSocketURI(WonMessage msg) {
        URI socketUri = msg.getRecipientSocketURI();
        if (socketUri != null) {
            return WonRelativeUriHelper.stripFragment(socketUri);
        }
        return null;
    }

    public static URI getRecipientAtomURIRequired(WonMessage msg) {
        return getRecipientAtomURI(msg).orElseThrow(
                        () -> new WonMessageProcessingException("Could not obtain recipient atom uri", msg));
    }

    public static Optional<URI> getOwnAtomFromIncomingMessage(WonMessage wonMessage) {
        if (wonMessage.getMessageTypeRequired().isSocketHintMessage()) {
            return Optional.of(WonMessageUtils.getRecipientAtomURIRequired(wonMessage));
        } else if (wonMessage.getMessageTypeRequired().isAtomHintMessage()) {
            return Optional.empty();
        }
        if (wonMessage.isMessageWithBothResponses()) {
            // message with both responses is an incoming message from another atom.
            // the head message is out partner's, so we are the recipient
            return Optional.of(WonMessageUtils.getRecipientAtomURIRequired(wonMessage));
        } else if (wonMessage.isMessageWithResponse()) {
            // message with onlny one response is our node's response plus the echo
            // the head message is the message we sent, so we are the sender
            return Optional.of(WonMessageUtils.getSenderAtomURIRequired(wonMessage));
        } else if (wonMessage.isRemoteResponse()) {
            // only a remote response. we are the recipient
            return Optional.of(WonMessageUtils.getRecipientAtomURIRequired(wonMessage));
        }
        return Optional.empty();
    }

    public static Optional<URI> getOwnAtomFromOutgoingMessage(WonMessage wonMessage) {
        if (wonMessage.getMessageTypeRequired().isSocketHintMessage()) {
            return Optional.of(WonMessageUtils.getSenderAtomURIRequired(wonMessage));
        } else if (wonMessage.getMessageTypeRequired().isAtomHintMessage()) {
            return Optional.empty();
        }
        return Optional.of(WonMessageUtils.getSenderAtomURIRequired(wonMessage));
    }

    /**
     * Returns the atom that this message belongs to.
     *
     * @param message
     * @return
     */
    public static Optional<URI> getParentAtomUri(final WonMessage message, WonMessageDirection direction) {
        if (direction.isFromExternal()) {
            return Optional.of(message.getRecipientAtomURI());
        } else {
            return Optional.of(message.getSenderAtomURI());
        }
    }

    /**
     * Get the targetAtom of a hint (even if it is a SocketHint).
     * 
     * @param wonMessage
     * @return
     */
    public static URI getHintTargetAtomURIRequired(WonMessage wonMessage) {
        return getHintTargetAtomURI(wonMessage).orElseThrow(
                        () -> new WonMessageProcessingException("Could not obtain target atom uri", wonMessage));
    }

    /**
     * Get the targetAtom of a hint (even if it is a SocketHint).
     * 
     * @param wonMessage
     * @return
     */
    public static Optional<URI> getHintTargetAtomURI(WonMessage wonMessage) {
        URI atomUri = wonMessage.getHintTargetAtomURI();
        if (atomUri == null) {
            URI socketUri = wonMessage.getHintTargetSocketURI();
            if (socketUri != null) {
                atomUri = WonRelativeUriHelper.stripFragment(socketUri);
            }
        }
        return Optional.ofNullable(atomUri);
    }

    public static boolean isValidMessageUri(URI messageURI) {
        if (messageURI == null) {
            return false;
        }
        if (Objects.equals(messageURI, WONMSG.MESSAGE_SELF)) {
            return false;
        }
        return messageURI.toString().startsWith(WONMSG.MESSAGE_URI_PREFIX);
    }
}
