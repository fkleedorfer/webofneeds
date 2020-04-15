/**
 * Created by fsuda on 05.11.2018.
 */

import vocab from "../../service/vocab.js";
import { get } from "../../utils.js";

/**
 * Determines if a given message can be Proposed
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageProposable(msg) {
  //TODO: should a message be proposable if it was already proposed? or even accepted? and what if the ref are only forwardedMessages?
  return (
    msg &&
    msg.get("hasContent") &&
    msg.get("messageType") !== vocab.WONMSG.connectMessage &&
    msg.get("messageType") !== vocab.WONMSG.changeNotificationMessage &&
    !msg.get("hasReferences") &&
    !isMessageRetracted(msg) &&
    !isMessageRejected(msg)
  );
}

/**
 * Determines if a given message can be Claimed
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageClaimable(msg) {
  //TODO: should a message be claimable if it was already claimed or proposed or even accepted? what if the ref are only forwardedMessages?
  return (
    msg &&
    msg.get("hasContent") &&
    msg.get("messageType") !== vocab.WONMSG.connectMessage &&
    msg.get("messageType") !== vocab.WONMSG.changeNotificationMessage &&
    !msg.get("hasReferences") &&
    !isMessageRetracted(msg) &&
    !isMessageRejected(msg)
  );
}

/**
 * Determines if a given message can be agreed on
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageAgreeable(msg) {
  return (
    msg &&
    msg.get("hasContent") &&
    (hasClaimsReferences(msg) ||
      hasProposesReferences(msg) ||
      hasProposesToCancelReferences(msg)) &&
    !isMessageAccepted(msg) &&
    !isMessageCancelled(msg) &&
    !isMessageCancellationPending(msg) &&
    !isMessageRetracted(msg) &&
    !isMessageRejected(msg)
  );
}

/**
 * Determines if a given message can be Canceled
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageCancelable(msg) {
  return (
    msg &&
    (hasClaimsReferences(msg) ||
      hasProposesReferences(msg) ||
      hasProposesToCancelReferences(msg)) &&
    isMessageAccepted(msg) &&
    !isMessageCancelled(msg) &&
    !isMessageCancellationPending(msg)
  );
}

/**
 * Determines if a given message can be Retracted
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageRetractable(msg) {
  return (
    msg &&
    msg.get("outgoingMessage") &&
    !isMessageAccepted(msg) &&
    !isMessageAgreedOn(msg) &&
    !isMessageCancelled(msg) &&
    !isMessageCancellationPending(msg) &&
    !isMessageRetracted(msg) &&
    !isMessageRejected(msg)
  );
}

/**
 * Determines if a given message can be Accepted
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageAcceptable(msg) {
  return (
    msg &&
    (hasClaimsReferences(msg) ||
      hasProposesReferences(msg) ||
      hasProposesToCancelReferences(msg)) &&
    !msg.get("outgoingMessage") &&
    !isMessageAccepted(msg) &&
    !isMessageAgreedOn(msg) &&
    !isMessageCancelled(msg) &&
    !isMessageCancellationPending(msg) &&
    !isMessageRetracted(msg) &&
    !isMessageRejected(msg)
  );
}

/**
 * Determines if a given message can be Rejected
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageRejectable(msg) {
  return (
    msg &&
    (hasClaimsReferences(msg) ||
      hasProposesReferences(msg) ||
      hasProposesToCancelReferences(msg)) &&
    !msg.get("outgoingMessage") &&
    !isMessageAccepted(msg) &&
    !isMessageAgreedOn(msg) &&
    !isMessageCancelled(msg) &&
    !isMessageCancellationPending(msg) &&
    !isMessageRetracted(msg) &&
    !isMessageRejected(msg)
  );
}

/**
 * Determines if a given message has proposes references
 * @param msg
 * @returns {*|boolean}
 */
export function hasProposesReferences(msg) {
  const references = msg && msg.get("references");
  return (
    references &&
    references.get("proposes") &&
    references.get("proposes").size > 0
  );
}

/**
 * Determines if a given message has claims references
 * @param msg
 * @returns {*|boolean}
 */
export function hasClaimsReferences(msg) {
  const references = msg && msg.get("references");
  return (
    references && references.get("claims") && references.get("claims").size > 0
  );
}

/**
 * Determines if a given message has proposesToCancel references
 * @param msg
 * @returns {*|boolean}
 */
export function hasProposesToCancelReferences(msg) {
  const references = msg && msg.get("references");
  return (
    references &&
    references.get("proposesToCancel") &&
    references.get("proposesToCancel").size > 0
  );
}

/**
 * Determines if a given message is in the state proposed
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageProposed(msg) {
  const messageStatus = msg && msg.get("messageStatus");
  return messageStatus && messageStatus.get("isProposed");
}

/**
 * Determines if a given message is in the state claimed
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageClaimed(msg) {
  const messageStatus = msg && msg.get("messageStatus");
  return messageStatus && messageStatus.get("isClaimed");
}

/**
 * Determines if a given message is in the state rejected
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageRejected(msg) {
  const messageStatus = msg && msg.get("messageStatus");
  return messageStatus && messageStatus.get("isRejected");
}

/**
 * Determines if a given message is in the state accepted
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageAccepted(msg) {
  const messageStatus = msg && msg.get("messageStatus");
  return messageStatus && messageStatus.get("isAccepted");
}

/**
 * Determines if a given message is in the state agreed
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageAgreedOn(msg) {
  const messageStatus = msg && msg.get("messageStatus");
  return messageStatus && messageStatus.get("isAgreed");
}

/**
 * Determines if a given message is in the state retracted
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageRetracted(msg) {
  const messageStatus = msg && msg.get("messageStatus");
  return messageStatus && messageStatus.get("isRetracted");
}

/**
 * Determines if a given message is in the state Cancelled
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageCancelled(msg) {
  const messageStatus = msg && msg.get("messageStatus");
  return messageStatus && messageStatus.get("isCancelled");
}

/**
 * Determines if a given message is in the state CancellationPending
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageCancellationPending(msg) {
  const messageStatus = msg && msg.get("messageStatus");
  return messageStatus && messageStatus.get("isCancellationPending");
}

/**
 * determines whether the given message is unread or not
 * @param msg
 * @returns {*}
 */
export function isMessageUnread(msg) {
  return msg && msg.get("unread");
}

/**
 * determines whether the given message is currently selected or not
 * @param msg
 * @returns {*|any}
 */
export function isMessageSelected(msg) {
  return msg && msg.getIn(["viewState", "isSelected"]);
}

/**
 * Determines if a given message is considered a proposal
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageProposal(msg) {
  return (
    (hasProposesToCancelReferences(msg) || hasProposesReferences(msg)) &&
    !(
      isMessageAccepted(msg) ||
      isMessageCancellationPending(msg) ||
      isMessageCancelled(msg) ||
      isMessageRejected(msg) ||
      isMessageRetracted(msg)
    )
  );
}

/**
 * Determines if a given message is considered a claim
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageClaim(msg) {
  return (
    hasClaimsReferences(msg) &&
    !(
      isMessageAccepted(msg) ||
      isMessageCancellationPending(msg) ||
      isMessageCancelled(msg) ||
      isMessageRejected(msg) ||
      isMessageRetracted(msg)
    )
  );
}

/**
 * Determines if a given message is an agreement
 * @param msg
 * @returns {*|boolean}
 */
export function isMessageAgreement(msg) {
  return isMessageAccepted(msg) && !isMessageCancellationPending(msg);
}

export function isAtomHintMessage(msg) {
  return get(msg, "messageType") === vocab.WONMSG.atomHintMessage;
}

export function isSocketHintMessage(msg) {
  return get(msg, "messageType") === vocab.WONMSG.socketHintMessage;
}

export function isConnectionMessage(msg) {
  return get(msg, "messageType") === vocab.WONMSG.connectionMessage;
}

export function isChangeNotificationMessage(msg) {
  return get(msg, "messageType") === vocab.WONMSG.changeNotificationMessage;
}

export function isParsable(msg) {
  return get(msg, "isParsable");
}
