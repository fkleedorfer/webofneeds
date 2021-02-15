/**
 * Created by sigpie on 21.09.2019.
 */
import React from "react";
import PropTypes from "prop-types";
import Immutable from "immutable";
import { getUri, generateLink } from "../utils.js";

import * as atomUtils from "../redux/utils/atom-utils.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";

import "~/style/_atom-connections-indicator.scss";
import ico36_message from "~/images/won-icons/ico36_message.svg";
import ico36_incoming from "~/images/won-icons/ico36_incoming.svg";
import ico36_match from "~/images/won-icons/ico36_match.svg";
import { useHistory } from "react-router-dom";
import vocab from "../service/vocab";

/**
 * Displays connection State indicators with link to the corresponding connection
 * @param atom
 * @param socketType - if present limit indicator only to a certain socketType, if the socketType is not empty or not ChatSocket ignore Connected ChatMessages
 * @returns {*}
 * @constructor
 */
export default function WonAtomConnectionsIndicator({ atom, socketType }) {
  const history = useHistory();

  const receivedRequests = atomUtils.getRequestReceivedConnections(
    atom,
    socketType
  );
  const receivedRequestsUnread = receivedRequests.filter(
    connectionUtils.isUnread
  );

  const connected =
    !socketType || socketType === vocab.CHAT.ChatSocketCompacted
      ? atomUtils.getConnectedConnections(atom, vocab.CHAT.ChatSocketCompacted)
      : Immutable.Map();
  const connectedUnread = connected.filter(connectionUtils.isUnread);

  const suggested = atomUtils.getSuggestedConnections(atom, socketType);
  const suggestedUnread = suggested.filter(connectionUtils.isUnread);

  function linkToConnectionSocketTab(connection) {
    const socketType = atomUtils.getSocketType(
      atom,
      connectionUtils.getSocketUri(connection)
    );

    const atomUri = getUri(atom);

    history.push(
      generateLink(
        history.location,
        { postUri: atomUri, tab: socketType, connectionUri: undefined },
        "/post"
      )
    );
  }

  function linkToRequests(connection) {
    if (
      atomUtils.getSocketType(
        atom,
        connectionUtils.getSocketUri(connection)
      ) === vocab.CHAT.ChatSocketCompacted
    ) {
      linkToChat(connection);
    } else {
      linkToConnectionSocketTab(connection);
    }
  }

  function linkToChat(connection) {
    history.push(
      generateLink(
        history.location,
        {
          postUri: getUri(atom),
          connectionUri: getUri(connection),
        },
        "/connections",
        false
      )
    );
  }

  const generateIconElement = (icon, unreads = true) => (
    <svg
      className={
        "asi__icon " + (unreads ? "asi__icon--unreads" : "asi__icon--reads")
      }
    >
      <use xlinkHref={icon} href={icon} />
    </svg>
  );

  const generateSubTitleElement = (label, connections, connectionsUnread) => (
    <div className="asi__right__subtitle">
      <div className="asi__right__subtitle__label">
        {connections && connectionsUnread ? (
          <React.Fragment>
            <span>{connections.size + " " + label}</span>
            {connectionsUnread.size > 0 ? (
              <span>{", " + connectionsUnread.size + " new"}</span>
            ) : (
              undefined
            )}
          </React.Fragment>
        ) : (
          <span>{label}</span>
        )}
      </div>
    </div>
  );

  if (connectedUnread.size > 0) {
    return (
      <won-atom-connections-indicator
        onClick={() => linkToChat(connectedUnread.first())}
      >
        {generateIconElement(ico36_message)}
        <div className="asi__right">
          <div className="asi__right__topline">
            <div className="asi__right__topline__title">Unread Messages</div>
          </div>
          {generateSubTitleElement("You have unread Chat Messages")}
        </div>
      </won-atom-connections-indicator>
    );
  } else if (receivedRequests.size > 0 && suggestedUnread.size === 0) {
    return (
      <won-atom-connections-indicator
        onClick={() =>
          linkToRequests(
            receivedRequestsUnread.first() || receivedRequests.first()
          )
        }
      >
        {generateIconElement(ico36_incoming, receivedRequestsUnread.size > 0)}
        <div className="asi__right">
          <div className="asi__right__topline">
            <div className="asi__right__topline__title">
              {"Connection Requests"}
            </div>
          </div>
          {generateSubTitleElement(
            "Requests",
            receivedRequests,
            receivedRequestsUnread
          )}
        </div>
      </won-atom-connections-indicator>
    );
  } else {
    return (
      <won-atom-connections-indicator
        class={suggested.size === 0 ? "won-no-connections" : ""}
        onClick={() =>
          linkToConnectionSocketTab(
            suggestedUnread.first() || suggested.first()
          )
        }
      >
        {generateIconElement(ico36_match, suggestedUnread.size > 0)}
        <div className="asi__right">
          <div className="asi__right__topline">
            <div className="asi__right__topline__title">Suggestions</div>
          </div>
          {generateSubTitleElement("Suggestions", suggested, suggestedUnread)}
        </div>
      </won-atom-connections-indicator>
    );
  }
}
WonAtomConnectionsIndicator.propTypes = {
  atom: PropTypes.object,
  socketType: PropTypes.string,
};
