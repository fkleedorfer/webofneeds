/**
 * Created by ksinger on 03.12.2015.
 */

import won from "./won-es6.js";
import vocab from "./service/vocab.js";

import * as wonUtils from "./won-utils.js";
import * as useCaseUtils from "./usecase-utils.js";

/**
 * Checks if a wonMessage contains content/references that make it necessary for us to check which effects
 * this message has caused (in relation to other messages, necessary e.g. AgreementData
 * @param wonMsg
 * @returns {*}
 */
export function isFetchMessageEffectsNeeded(wonMsg) {
  return (
    wonMsg &&
    (wonMsg.getProposedMessageUris() ||
      wonMsg.getRetractsMessageUris() ||
      wonMsg.getRejectsMessageUris() ||
      wonMsg.getAcceptsMessageUris() ||
      wonMsg.getProposedToCancelMessageUris() ||
      wonMsg.getClaimsMessageUris())
  );
}

export function buildRateMessage(
  msgToRateFor,
  ownedAtomUri,
  theirAtomUri,
  ownNodeUri,
  theirNodeUri,
  theirConnectionUri,
  rating,
  socketUri,
  targetSocketUri
) {
  return new Promise(resolve => {
    const buildMessage = function() {
      const message = new won.MessageBuilder(vocab.WONMSG.feedbackMessage) //TODO: Looks like a copy-paste-leftover from connect
        .protocolVersion("1.0")
        .eventURI(vocab.WONMSG.uriPlaceholder.event)
        .ownerDirection()
        .senderSocket(socketUri)
        .targetSocket(targetSocketUri)
        .timestamp(new Date().getTime().toString())
        .addRating(rating, msgToRateFor.connection.uri)
        .build();

      return message;
    };

    //fetch all datan needed
    won
      .validateEnvelopeDataForConnection(socketUri, targetSocketUri)
      .then(function() {
        resolve(buildMessage(msgToRateFor.event));
      }, won.reportError(
        "cannot open connection " + msgToRateFor.connection.uri
      ));
  });
}

export function buildCloseMessage(socketUri, targetSocketUri) {
  const buildMessage = function() {
    const message = new won.MessageBuilder(vocab.WONMSG.closeMessage)
      .protocolVersion("1.0")
      .eventURI(vocab.WONMSG.uriPlaceholder.event)
      .senderSocket(socketUri)
      .targetSocket(targetSocketUri)
      .ownerDirection()
      .timestamp(new Date().getTime().toString())
      .build();

    return message;
  };

  //fetch all datan needed
  return won
    .validateEnvelopeDataForConnection(socketUri, targetSocketUri)
    .then(() => buildMessage())
    .catch(err => {
      won.reportError("cannot close connection " + JSON.stringify(err));
      throw err;
    });
}
export function buildCloseAtomMessage(atomUri) {
  const buildMessage = function() {
    return new won.MessageBuilder(vocab.WONMSG.closeAtomMessage)
      .protocolVersion("1.0")
      .atom(atomUri)
      .eventURI(vocab.WONMSG.uriPlaceholder.event)
      .ownerDirection()
      .timestamp(new Date().getTime().toString())
      .build();
  };

  return won
    .validateEnvelopeDataForAtom(atomUri)
    .then(
      () => buildMessage(),
      () => won.reportError("cannot close atom " + atomUri)
    );
}

export function buildDeleteAtomMessage(atomUri) {
  const buildMessage = function() {
    return new won.MessageBuilder(vocab.WONMSG.deleteAtomMessage)
      .protocolVersion("1.0")
      .atom(atomUri)
      .eventURI(vocab.WONMSG.uriPlaceholder.event)
      .ownerDirection()
      .timestamp(new Date().getTime().toString())
      .build();
  };

  return won
    .validateEnvelopeDataForAtom(atomUri)
    .then(
      () => buildMessage(),
      () => won.reportError("cannot delete atom " + atomUri)
    );
}

export function buildOpenAtomMessage(atomUri) {
  const buildMessage = function() {
    return new won.MessageBuilder(vocab.WONMSG.activateAtomMessage)
      .protocolVersion("1.0")
      .atom(atomUri)
      .eventURI(vocab.WONMSG.uriPlaceholder.event)
      .ownerDirection()
      .timestamp(new Date().getTime().toString())
      .build();
  };

  return won
    .validateEnvelopeDataForAtom(atomUri)
    .then(
      () => buildMessage(),
      () => won.reportError("cannot close atom " + atomUri)
    );
}

/**
 * Builds json-ld for a connect-message in reaction to an atom.
 * @param connectionUri
 * @param textMessage
 * @returns message
 */
export function buildConnectMessage({
  connectMessage,
  socketUri,
  targetSocketUri,
}) {
  if (!socketUri || !targetSocketUri) {
    throw new Error("buildConnectMessage is missing socketUris");
  }

  const messageBuilder = new won.MessageBuilder(vocab.WONMSG.connectMessage)
    .protocolVersion("1.0")
    .eventURI(vocab.WONMSG.uriPlaceholder.event)
    .senderSocket(socketUri)
    .targetSocket(targetSocketUri)
    .ownerDirection()
    .timestamp(new Date().getTime().toString());

  if (connectMessage && typeof connectMessage === "string") {
    messageBuilder.textMessage(connectMessage);
  } else if (connectMessage) {
    messageBuilder.mergeIntoContentGraph(connectMessage);
  }

  return messageBuilder.build();
}

export function buildChatMessage({
  chatMessage,
  additionalContent,
  referencedContentUris, //this is a map of corresponding uris to be e.g. proposes or retracted...
  isTTL,
  socketUri,
  targetSocketUri,
}) {
  let jsonldGraphPayloadP = isTTL
    ? won.ttlToJsonLd(won.defaultTurtlePrefixes + "\n" + chatMessage)
    : Promise.resolve();

  const envelopeDataP = won.validateEnvelopeDataForConnection(
    socketUri,
    targetSocketUri
  );

  return Promise.all([envelopeDataP, jsonldGraphPayloadP]).then(
    ([envelopeData, graphPayload]) => {
      envelopeData; //TODO remove this

      /*
             * Build the json-ld message that's signed on the owner-server
             * and then send to the won-node.
             */
      const wonMessageBuilder = new won.MessageBuilder(
        vocab.WONMSG.connectionMessage
      )
        .protocolVersion("1.0")
        .ownerDirection()
        .senderSocket(socketUri)
        .targetSocket(targetSocketUri)
        .timestamp(new Date().getTime().toString());

      if (isTTL && graphPayload) {
        wonMessageBuilder.mergeIntoContentGraph(graphPayload);
      } else if (
        !isTTL &&
        (chatMessage || additionalContent || referencedContentUris)
      ) {
        //add the chatMessage as normal text message
        if (chatMessage) {
          wonMessageBuilder.addContentGraphData(vocab.WONCON.text, chatMessage);
        }

        if (additionalContent) {
          const contentNode = wonMessageBuilder.getContentGraphNode();
          const contentNodes = wonMessageBuilder.getContentGraphNodes();
          const detailList = useCaseUtils.getAllDetails();
          additionalContent.forEach((value, key) => {
            const detail = detailList[key];
            const detailRDF =
              detail &&
              detail.parseToRDF({
                value: value,
                identifier: detail.identifier,
                contentUri: vocab.WONMSG.uriPlaceholder.event,
              });

            if (detailRDF) {
              const detailRDFArray = Array.isArray(detailRDF)
                ? detailRDF
                : [detailRDF];

              for (const i in detailRDFArray) {
                const detailRDFToAdd = detailRDFArray[i];

                if (detailRDFToAdd["@id"]) {
                  contentNodes.push(detailRDFToAdd);
                } else {
                  for (const key in detailRDFToAdd) {
                    //if contentNode[key] and detailRDF[key] both have values we ommit adding new content (until we implement a merge function)
                    if (contentNode[key]) {
                      if (!Array.isArray(contentNode[key]))
                        contentNode[key] = Array.of(contentNode[key]);

                      contentNode[key] = contentNode[key].concat(
                        detailRDFToAdd[key]
                      );
                    } else {
                      contentNode[key] = detailRDFToAdd[key];
                    }
                  }
                }
              }
            }
          });
        }

        if (referencedContentUris) {
          const contentNode = wonMessageBuilder.getContentGraphNode();
          referencedContentUris.forEach((uris, key) => {
            if (uris && uris.length > 0) {
              switch (key) {
                case "retracts":
                  contentNode[vocab.MOD.retracts] = uris;
                  break;
                case "rejects":
                  contentNode[vocab.AGR.rejects] = uris;
                  break;
                case "proposes":
                  contentNode[vocab.AGR.proposes] = uris;
                  break;
                case "claims":
                  contentNode[vocab.AGR.claims] = uris;
                  break;
                case "proposesToCancel":
                  contentNode[vocab.AGR.proposesToCancel] = uris;
                  break;
                case "accepts":
                  contentNode[vocab.AGR.accepts] = uris;
                  break;
                default:
                  console.error(
                    "key[",
                    key,
                    "] is not a valid reference omitting uris[",
                    uris,
                    "] in message"
                  );
                  break;
              }
            }
          });
        }
      } else {
        throw new Error(
          "No textmessage or valid graph as payload of chat message:" +
            JSON.stringify(chatMessage) +
            " " +
            JSON.stringify(graphPayload)
        );
      }

      wonMessageBuilder.eventURI(vocab.WONMSG.uriPlaceholder.event); // replace placeholders with proper event-uri
      return wonMessageBuilder.build();
    }
  );
}

export function buildEditMessage(editedAtomData, oldAtom) {
  const atomUriToEdit = oldAtom && oldAtom.get("uri");

  const msgJson = won.buildMessageRdf(
    buildContentRdf(editedAtomData, atomUriToEdit),
    {
      msgType: vocab.WONMSG.replaceMessage, //mandatory
      publishedContentUri: atomUriToEdit, //mandatory
      msgUri: vocab.WONMSG.uriPlaceholder.event,
    }
  );
  //add the @base definition to the @context so we can use #fragments in the atom structure
  msgJson["@context"]["@base"] = atomUriToEdit;

  return {
    message: msgJson,
    atomUri: atomUriToEdit,
  };
}

/**
 *
 * @param atomData
 * @param wonNodeUri
 * @return {{
 *    message: (
 *      {
 *          @id,
 *          msg:hasDestinationUri,
 *          msg:hasAttachmentGraphUri
 *      }|
 *      {@id}|
 *      {@graph, @context}
 *    ),
 *    atomUri: string
 * }}
 */
export function buildCreateMessage(atomData, wonNodeUri) {
  //Check for is and seeks
  /*
    if(!atomData.type || !atomData.title)
        throw new Error('Tried to create post without type or title. ', atomData);
    */

  const publishedContentUri = wonNodeUri + "/atom/" + wonUtils.getRandomWonId();

  const msgJson = won.buildMessageRdf(
    buildContentRdf(atomData, publishedContentUri),
    {
      atom: publishedContentUri, //mandatory
      msgType: vocab.WONMSG.createMessage, //mandatory
      publishedContentUri: publishedContentUri, //mandatory
      msgUri: vocab.WONMSG.uriPlaceholder.event,
    }
  );
  //add the @base definition to the @context so we can use #fragments in the atom structure
  msgJson["@context"]["@base"] = publishedContentUri;
  return {
    message: msgJson,
    atomUri: publishedContentUri,
  };
}

function buildContentRdf(atomData, publishedContentUri) {
  //if type  create -> use atomBuilder as well
  const prepareContentNodeData = atomData => ({
    // Adds all fields from atomDataIsOrSeeks:
    // title, description, tags, location,...
    ...atomData,

    publishedContentUri: publishedContentUri, //mandatory
  });

  let contentRdf = won.buildAtomRdf({
    content: atomData.content
      ? prepareContentNodeData(atomData.content)
      : undefined,
    seeks: atomData.seeks ? prepareContentNodeData(atomData.seeks) : undefined,
    useCase: atomData.useCase ? atomData.useCase : undefined, //only needed for atom building
    // FIXME: find a better way to include atom details that are not part of is or seeks
    socket: atomData.socket,
  });

  return contentRdf;
}
