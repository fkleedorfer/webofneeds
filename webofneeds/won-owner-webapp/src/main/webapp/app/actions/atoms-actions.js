/**
 * Created by ksinger on 19.02.2016.
 */

import won from "../won-es6.js";
import vocab from "../service/vocab.js";

import { actionTypes, actionCreators } from "./actions.js";
import Immutable from "immutable";

import {
  buildConnectMessage,
  buildCloseAtomMessage,
  buildOpenAtomMessage,
  buildDeleteAtomMessage,
  buildCreateMessage,
  buildEditMessage,
} from "../won-message-utils.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as accountUtils from "../redux/utils/account-utils.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as stateStore from "../redux/state-store.js";
import * as ownerApi from "../api/owner-api.js";
import { getUri, extractAtomUriBySocketUri } from "../utils.js";
import { ensureLoggedIn } from "./account-actions.js";
import * as processUtils from "~/app/redux/utils/process-utils";

export function fetchUnloadedAtom(atomUri) {
  return (dispatch, getState) =>
    stateStore.fetchAtomAndDispatch(atomUri, dispatch, getState);
}

export function fetchUnloadedConnectionsContainer(atomUri) {
  return (dispatch, getState) => {
    const state = getState();
    const processState = generalSelectors.getProcessState(state);

    if (processUtils.isConnectionContainerLoaded(processState, atomUri)) {
      console.debug(
        "Omit Fetch of ConnectionContainer<",
        atomUri,
        ">, it is already loaded..."
      );
      if (processUtils.isConnectionContainerToLoad(processState, atomUri)) {
        dispatch({
          type: actionTypes.atoms.markConnectionContainerAsLoaded,
          payload: Immutable.fromJS({ uri: atomUri }),
        });
      }
      return Promise.resolve();
    } else if (
      processUtils.isConnectionContainerLoading(processState, atomUri)
    ) {
      console.debug(
        "Omit Fetch of ConnectionContainer<",
        atomUri,
        ">, it is currently loading..."
      );
      return Promise.resolve();
    } else if (processUtils.isProcessingInitialLoad(processState)) {
      console.debug(
        "Omit Fetch of ConnectionContainer<",
        atomUri,
        ">, initial Load still in progress..."
      );
      return Promise.resolve();
    }

    const isOwned = generalSelectors.isAtomOwned(atomUri)(state);
    console.debug(
      "Proceed Fetch of",
      isOwned ? "Owned" : "",
      "ConnectionContainer<",
      atomUri,
      ">"
    );

    const requesterWebId = stateStore.determineRequesterWebId(
      state,
      atomUri,
      isOwned
    );

    //Fetch All MetaConnections Of NonOwnedAtomAndDispatch

    dispatch({
      type: actionTypes.atoms.storeConnectionContainerInLoading,
      payload: Immutable.fromJS({ uri: atomUri }),
    });

    const connectionContainerPromise = isOwned
      ? stateStore.fetchConnectionsOfOwnedAtomAndDispatch(
          atomUri,
          requesterWebId,
          dispatch
        )
      : stateStore.fetchConnectionsOfNonOwnedAtomAndDispatch(
          atomUri,
          requesterWebId,
          dispatch
        );

    return connectionContainerPromise.catch(error => {
      if (error.status && error.status === 410) {
        dispatch({
          type: actionTypes.atoms.delete,
          payload: Immutable.fromJS({ uri: atomUri }),
        });
      } else {
        dispatch({
          type: actionTypes.atoms.storeConnectionContainerFailed,
          payload: Immutable.fromJS({
            uri: atomUri,
            status: {
              code: error.status,
              message: error.message,
              requesterWebId: requesterWebId,
            },
          }),
        });
      }
    });
  };
}

export function connectSockets(
  senderSocketUri,
  targetSocketUri,
  connectMessage
) {
  return (dispatch, getState) => {
    if (!senderSocketUri) {
      throw new Error("SenderSocketUri not present");
    }

    if (!targetSocketUri) {
      throw new Error("TargetSocketUri not present");
    }

    const accountState = generalSelectors.getAccountState(getState());
    if (
      accountUtils.isAtomOwned(
        accountState,
        extractAtomUriBySocketUri(senderSocketUri)
      ) &&
      accountUtils.isAtomOwned(
        accountState,
        extractAtomUriBySocketUri(targetSocketUri)
      )
    ) {
      if (!senderSocketUri) {
        throw new Error("SenderSocketUri not present");
      }

      if (!targetSocketUri) {
        throw new Error("TargetSocketUri not present");
      }

      return ownerApi
        .serverSideConnect(senderSocketUri, targetSocketUri)
        .then(async response => {
          if (!response.ok) {
            const errorMsg = await response.text();
            throw new Error(
              `Could not connect sockets(${senderSocketUri}<->${targetSocketUri}): ${errorMsg}`
            );
          }
        });
    } else {
      const cnctMsg = buildConnectMessage({
        connectMessage: connectMessage,
        socketUri: senderSocketUri,
        targetSocketUri: targetSocketUri,
      });

      return ownerApi.sendMessage(cnctMsg).then(jsonResp =>
        won
          .wonMessageFromJsonLd(
            jsonResp.message,
            vocab.WONMSG.uriPlaceholder.event
          )
          .then(wonMessage =>
            dispatch({
              type: actionTypes.atoms.connectSockets,
              payload: {
                eventUri: jsonResp.messageUri,
                message: jsonResp.message,
                optimisticEvent: wonMessage,
                senderSocketUri: senderSocketUri,
                targetSocketUri: targetSocketUri,
              },
            })
          )
      );
    }
  };
}

export function close(atomUri) {
  return (dispatch, getState) => {
    buildCloseAtomMessage(atomUri)
      .then(message => ownerApi.sendMessage(message))
      .then(jsonResp => {
        dispatch(
          actionCreators.messages__send({
            eventUri: jsonResp.messageUri,
            message: jsonResp.message,
          })
        );

        const atom = generalSelectors.getAtom(atomUri)(getState());

        //Close all the open connections of the atom
        atomUtils.getConnectedConnections(atom).map(conn => {
          dispatch(actionCreators.connections__close(getUri(conn)));
        });
      })
      .then(() =>
        // assume close went through successfully, update GUI
        dispatch({
          type: actionTypes.atoms.close,
          payload: {
            ownedAtomUri: atomUri,
          },
        })
      );
  };
}

export function open(atomUri) {
  return dispatch => {
    buildOpenAtomMessage(atomUri)
      .then(message => ownerApi.sendMessage(message))
      .then(jsonResp => {
        dispatch(
          actionCreators.messages__send({
            eventUri: jsonResp.messageUri,
            message: jsonResp.message,
          })
        );
      })
      .then(() =>
        // assume close went through successfully, update GUI
        dispatch({
          type: actionTypes.atoms.reopen,
          payload: {
            ownedAtomUri: atomUri,
          },
        })
      );
  };
}

export function closedBySystem(event) {
  return (dispatch, getState) => {
    //first check if we really have the 'own' atom in the state - otherwise we'll ignore the message
    const atom = getState().getIn(["atoms", event.getAtom()]);
    if (!atom) {
      console.debug(
        "ignoring deactivateMessage for an atom that is not ours:",
        event.getAtom()
      );
    }
    dispatch({
      type: actionTypes.atoms.closedBySystem,
      payload: {
        atomUri: event.getAtom(),
        humanReadable: atomUtils.getTitle(
          atom,
          generalSelectors.getExternalDataState(getState())
        ),
        message: event.getTextMessage(),
      },
    });
  };
}

export function deleteAtom(atomUri) {
  return dispatch => {
    buildDeleteAtomMessage(atomUri)
      .then(message => ownerApi.sendMessage(message))
      .then(jsonResp => {
        dispatch(
          actionCreators.messages__send({
            eventUri: jsonResp.messageUri,
            message: jsonResp.message,
          })
        );
      })
      .then(() =>
        // assume close went through successfully, update GUI
        dispatch({
          type: actionTypes.atoms.delete,
          payload: Immutable.fromJS({
            uri: atomUri,
          }),
        })
      );
  };
}

export function edit(draft, oldAtom, callback) {
  return (dispatch, getState) => {
    return ensureLoggedIn(dispatch, getState).then(() => {
      const { message, atomUri } = buildEditMessage(draft, oldAtom);
      return ownerApi.sendMessage(message).then(jsonResp => {
        dispatch({
          type: actionTypes.atoms.edit,
          payload: {
            eventUri: jsonResp.messageUri,
            message: jsonResp.message,
            atomUri: atomUri,
            atom: draft,
            oldAtom,
          },
        });
        callback();
      });
    });
  };
}

export function create(draft, personaUri, nodeUri) {
  return (dispatch, getState) => {
    const state = getState();

    if (!nodeUri) {
      nodeUri = generalSelectors.getDefaultNodeUri(state);
    }

    return ensureLoggedIn(dispatch, getState).then(() => {
      const { message, atomUri } = buildCreateMessage(draft, nodeUri);

      return ownerApi
        .sendMessage(message)
        .then(jsonResp => {
          dispatch({
            type: actionTypes.atoms.create,
            payload: {
              eventUri: jsonResp.messageUri,
              message: jsonResp.message,
              atomUri: atomUri,
              atom: draft,
            },
          });
        })
        .then(() => {
          const persona = generalSelectors.getAtom(personaUri)(state);
          if (persona) {
            const senderSocketUri = atomUtils.getSocketUri(
              persona,
              vocab.HOLD.HolderSocketCompacted
            );
            const targetSocketUri = `${atomUri}#holdableSocket`;

            return ownerApi
              .serverSideConnect(senderSocketUri, targetSocketUri, false, true)
              .then(async response => {
                if (!response.ok) {
                  const errorMsg = await response.text();
                  throw new Error(`Could not connect identity: ${errorMsg}`);
                }
              });
          }
        });
    });
  };
}
