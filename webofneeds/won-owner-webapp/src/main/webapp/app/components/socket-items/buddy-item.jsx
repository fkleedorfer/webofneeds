/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import { get, generateLink } from "../../utils.js";
import { actionCreators } from "../../actions/actions.js";
import { useDispatch } from "react-redux";
import { useHistory } from "react-router-dom";
import * as atomUtils from "../../redux/utils/atom-utils";
import * as connectionUtils from "../../redux/utils/connection-utils";
import vocab from "../../service/vocab.js";
import WonAtomContextSwipeableView from "../atom-context-swipeable-view";
import "~/style/_socket-item.scss";
import VisibilitySensor from "react-visibility-sensor";
import PropTypes from "prop-types";

import ico32_buddy_accept from "~/images/won-icons/ico32_buddy_accept.svg";
import ico32_buddy_deny from "~/images/won-icons/ico32_buddy_deny.svg";
import ico32_buddy_waiting from "~/images/won-icons/ico32_buddy_waiting.svg";
import ico36_message from "~/images/won-icons/ico36_message.svg";

export default function WonBuddyItem({
  connection,
  atom,
  isOwned,
  targetAtom,
  flip,
}) {
  const atomUri = get(atom, "uri");

  const hasBuddySocket = atomUtils.hasBuddySocket(atom);
  const addActionButtons = isOwned || flip;

  const dispatch = useDispatch();
  const history = useHistory();

  function markAsRead(conn) {
    if (connectionUtils.isUnread(conn)) {
      setTimeout(() => {
        dispatch(
          actionCreators.connections__markAsRead({
            connectionUri: get(conn, "uri"),
            atomUri: atomUri,
          })
        );
      }, 1500);
    }
  }

  function openRequest() {
    if (connectionUtils.isUnread(connection)) {
      dispatch(
        actionCreators.connections__markAsRead({
          connectionUri: get(connection, "uri"),
          atomUri: atomUri,
        })
      );
    }

    const senderSocketUri = get(connection, "socketUri");
    const targetSocketUri = get(connection, "targetSocketUri");
    dispatch(
      actionCreators.atoms__connectSockets(senderSocketUri, targetSocketUri)
    );
  }

  function requestBuddy() {
    if (!addActionButtons || !hasBuddySocket) {
      console.warn("Trying to request a non-owned or non buddySocket atom");
      return;
    }

    const payload = {
      caption: "Persona",
      text: "Send Buddy Request?",
      buttons: [
        {
          caption: "Yes",
          callback: () => {
            const senderSocketUri = get(connection, "socketUri");
            const targetSocketUri = get(connection, "targetSocketUri");

            dispatch(
              actionCreators.atoms__connectSockets(
                senderSocketUri,
                targetSocketUri
              )
            );
            dispatch(actionCreators.view__hideModalDialog());
          },
        },
        {
          caption: "No",
          callback: () => {
            dispatch(actionCreators.view__hideModalDialog());
          },
        },
      ],
    };
    dispatch(actionCreators.view__showModalDialog(payload));
  }

  function closeConnection(dialogText = "Remove Buddy?") {
    const payload = {
      caption: "Persona",
      text: dialogText,
      buttons: [
        {
          caption: "Yes",
          callback: () => {
            if (connectionUtils.isUnread(connection)) {
              dispatch(
                actionCreators.connections__markAsRead({
                  connectionUri: get(connection, "uri"),
                  atomUri: atomUri,
                })
              );
            }

            dispatch(actionCreators.connections__close(get(connection, "uri")));
            dispatch(actionCreators.view__hideModalDialog());
          },
        },
        {
          caption: "No",
          callback: () => {
            dispatch(actionCreators.view__hideModalDialog());
          },
        },
      ],
    };
    dispatch(actionCreators.view__showModalDialog(payload));
  }

  function sendChatMessage() {
    const senderSocketUri = atomUtils.getSocketUri(
      atom,
      vocab.CHAT.ChatSocketCompacted
    );
    const targetSocketUri = atomUtils.getSocketUri(
      targetAtom,
      vocab.CHAT.ChatSocketCompacted
    );

    const chatConnection = atomUtils.getConnectionBySocketUris(
      atom,
      vocab.CHAT.ChatSocketCompacted,
      vocab.CHAT.ChatSocketCompacted
    );

    if (
      !chatConnection ||
      connectionUtils.isSuggested(chatConnection) ||
      connectionUtils.isClosed(chatConnection) ||
      connectionUtils.isRequestReceived(chatConnection)
    ) {
      dispatch(
        actionCreators.atoms__connectSockets(senderSocketUri, targetSocketUri)
      );
    }

    if (chatConnection) {
      history.push(
        generateLink(
          history.location,
          { postUri: atomUri, connectionUri: get(chatConnection, "uri") },
          "/connections"
        )
      );
    } else {
      history.push("/connections");
    }
  }

  if (!addActionButtons) {
    return (
      <div className="si ">
        <WonAtomContextSwipeableView
          atom={flip ? atom : targetAtom}
          toLink={generateLink(
            history.location,
            {
              postUri: flip ? get(atom, "uri") : get(targetAtom, "uri"),
            },
            "/post"
          )}
        />
      </div>
    );
  } else {
    let actionButtons;
    let headerClassName;

    if (connectionUtils.isRequestReceived(connection)) {
      headerClassName = "status--received";
      actionButtons = (
        <div className="si__actions">
          <svg
            className="si__actions__icon request won-icon"
            onClick={openRequest}
          >
            <use xlinkHref={ico32_buddy_accept} href={ico32_buddy_accept} />
          </svg>
          <svg
            className="si__actions__icon primary won-icon"
            onClick={() => closeConnection("Reject Buddy Request?")}
          >
            <use xlinkHref={ico32_buddy_deny} href={ico32_buddy_deny} />
          </svg>
        </div>
      );
    } else if (connectionUtils.isSuggested(connection)) {
      headerClassName = "status--suggested";
      actionButtons = (
        <div className="si__actions">
          <svg
            className="si__actions__icon request won-icon"
            onClick={requestBuddy}
          >
            <use xlinkHref={ico32_buddy_accept} href={ico32_buddy_accept} />
          </svg>
          <svg
            className="si__actions__icon primary won-icon"
            onClick={() => closeConnection("Reject Buddy Suggestion?")}
          >
            <use xlinkHref={ico32_buddy_deny} href={ico32_buddy_deny} />
          </svg>
        </div>
      );
    } else if (connectionUtils.isRequestSent(connection)) {
      headerClassName = "status--sent";
      actionButtons = (
        <div className="si__actions">
          <svg className="si__actions__icon disabled won-icon">
            <use xlinkHref={ico32_buddy_waiting} href={ico32_buddy_waiting} />
          </svg>
          <svg
            className="si__actions__icon secondary won-icon"
            onClick={() => closeConnection("Cancel Buddy Request?")}
          >
            <use xlinkHref={ico32_buddy_deny} href={ico32_buddy_deny} />
          </svg>
        </div>
      );
    } else if (connectionUtils.isConnected(connection)) {
      actionButtons = (
        <div className="si__actions">
          {!flip && atomUtils.hasChatSocket(targetAtom) ? (
            <svg
              className="si__actions__icon primary won-icon"
              onClick={sendChatMessage}
            >
              <use xlinkHref={ico36_message} href={ico36_message} />
            </svg>
          ) : (
            undefined
          )}
          <svg
            className="si__actions__icon secondary won-icon"
            onClick={() => closeConnection(connection)}
          >
            <use xlinkHref={ico32_buddy_deny} href={ico32_buddy_deny} />
          </svg>
        </div>
      );
    } else if (connectionUtils.isClosed(connection)) {
      headerClassName = "status--closed";
      actionButtons = <div className="si__actions">Buddy has been removed</div>;
    } else {
      actionButtons = <div className="si__actions">Unknown State</div>;
    }

    return (
      <VisibilitySensor
        onChange={isVisible => {
          isVisible &&
            connectionUtils.isUnread(connection) &&
            markAsRead(connection);
        }}
        intervalDelay={2000}
      >
        <div
          className={
            "si " + (connectionUtils.isUnread(connection) ? " won-unread " : "")
          }
        >
          <WonAtomContextSwipeableView
            className={headerClassName}
            actionButtons={actionButtons}
            atom={flip ? atom : targetAtom}
            toLink={generateLink(
              history.location,
              {
                postUri: flip ? get(atom, "uri") : get(targetAtom, "uri"),
              },
              "/post"
            )}
          />
        </div>
      </VisibilitySensor>
    );
  }
}
WonBuddyItem.propTypes = {
  connection: PropTypes.object.isRequired,
  atom: PropTypes.object.isRequired,
  isOwned: PropTypes.bool.isRequired,
  targetAtom: PropTypes.object.isRequired,
  flip: PropTypes.bool,
};
