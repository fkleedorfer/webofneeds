import React from "react";
import PropTypes from "prop-types";
import Immutable from "immutable";
import { connect } from "react-redux";
import { get, getIn } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import WonAtomHeaderBig from "./atom-header-big.jsx";
import WonAtomHeader from "./atom-header.jsx";
import WonAtomMenu from "./atom-menu.jsx";
import WonAtomContent from "./atom-content.jsx";
import ChatTextfield from "./chat-textfield.jsx";
import * as generalSelectors from "../redux/selectors/general-selectors";
import * as atomUtils from "../redux/utils/atom-utils";
import * as connectionUtils from "../redux/utils/connection-utils";
import * as viewUtils from "../redux/utils/view-utils";
import * as processSelectors from "../redux/selectors/process-selectors";
import * as accountUtils from "../redux/utils/account-utils";
import * as useCaseUtils from "../usecase-utils.js";
import vocab from "../service/vocab.js";

import "~/style/_atom-info.scss";

const mapStateToProps = (state, ownProps) => {
  const ownedAtoms = generalSelectors.getOwnedAtoms(state);
  const atom = getIn(state, ["atoms", ownProps.atomUri]);

  const isOwned = generalSelectors.isAtomOwned(state, ownProps.atomUri);

  //checks for active and chatSocket || groupSocket
  const isConnectible = atomUtils.isConnectible(atom);
  const hasReactionUseCases = atomUtils.hasReactionUseCases(atom);
  const hasEnabledUseCases = atomUtils.hasEnabledUseCases(atom);

  const showEnabledUseCases = isConnectible && isOwned && hasEnabledUseCases;
  const showReactionUseCases = isConnectible && !isOwned && hasReactionUseCases;

  const reactionUseCases = atomUtils.getReactionUseCases(atom);
  const enabledUseCases = atomUtils.getEnabledUseCases(atom);

  const ownedReactionAtoms =
    ownedAtoms &&
    ownedAtoms.filter(
      atom =>
        atomUtils.matchesDefinitions(atom, reactionUseCases) ||
        atomUtils.matchesDefinitions(atom, enabledUseCases)
    );

  const showAdHocRequestField =
    !isOwned && isConnectible && !showEnabledUseCases && !showReactionUseCases;

  const viewState = get(state, "view");
  const visibleTab = viewUtils.getVisibleTabByAtomUri(
    viewState,
    ownProps.atomUri
  );

  const chatSocketUri = atomUtils.getChatSocket(atom);
  const groupSocketUri = atomUtils.getGroupSocket(atom);

  const atomLoading =
    !atom || processSelectors.isAtomLoading(state, ownProps.atomUri);

  const holderUri = atomUtils.getHeldByUri(atom);

  const ownedChatSocketAtoms =
    ownedAtoms && ownedAtoms.filter(atom => atomUtils.hasChatSocket(atom));

  const isInactive = atomUtils.isInactive(atom);
  return {
    className: ownProps.className,
    atomUri: ownProps.atomUri,
    defaultTab: ownProps.defaultTab,
    loggedIn: accountUtils.isLoggedIn(get(state, "account")),
    isInactive: isInactive,
    isOwned: isOwned,
    showAdHocRequestField,
    showEnabledUseCases,
    showReactionUseCases,
    reactionUseCasesArray: showReactionUseCases
      ? reactionUseCases.toArray()
      : [],
    ownedReactionAtomsArray: ownedReactionAtoms
      ? ownedReactionAtoms.toArray()
      : [],
    enabledUseCasesArray: showEnabledUseCases ? enabledUseCases.toArray() : [],
    atomLoading,
    showFooter:
      !atomLoading &&
      visibleTab === "DETAIL" &&
      (showEnabledUseCases ||
        showReactionUseCases ||
        showAdHocRequestField ||
        isInactive),
    addHolderUri: showEnabledUseCases ? holderUri : undefined,
    holderUri,
    chatSocketUri,
    groupSocketUri,
    ownedChatSocketAtoms,
    ownedAtoms,
  };
};

const mapDispatchToProps = dispatch => {
  return {
    viewRemoveAddMessageContent: () => {
      dispatch(actionCreators.view__removeAddMessageContent());
    },
    routerGo: (path, props) => {
      dispatch(actionCreators.router__stateGo(path, props));
    },
    routerGoResetParams: path => {
      dispatch(actionCreators.router__stateGoResetParams(path));
    },
    hideModalDialog: () => {
      dispatch(actionCreators.view__hideModalDialog());
    },
    showModalDialog: payload => {
      dispatch(actionCreators.view__showModalDialog(payload));
    },
    showTermsDialog: payload => {
      dispatch(actionCreators.view__showTermsDialog(payload));
    },
    connectionsConnectAdHoc: (
      connectToAtomUri,
      message,
      connectToSocketUri,
      persona
    ) => {
      dispatch(
        actionCreators.connections__connectAdHoc(
          connectToAtomUri,
          message,
          connectToSocketUri,
          persona
        )
      );
    },
    connect: (
      ownedAtomUri,
      connectionUri,
      targetAtomUri,
      message,
      ownSocket,
      targetSocket
    ) => {
      dispatch(
        actionCreators.atoms__connect(
          ownedAtomUri,
          connectionUri,
          targetAtomUri,
          message,
          ownSocket,
          targetSocket
        )
      );
    },
    sendChatMessage: (
      trimmedMsg,
      additionalContent,
      referencedContent,
      senderSocketUri,
      targetSocketUri,
      connectionUri,
      isTTL
    ) => {
      dispatch(
        actionCreators.connections__sendChatMessage(
          trimmedMsg,
          additionalContent,
          referencedContent,
          senderSocketUri,
          targetSocketUri,
          connectionUri,
          isTTL
        )
      );
    },
    connectSockets: (senderSocketUri, targetSocketUri, message) => {
      dispatch(
        actionCreators.atoms__connectSockets(
          senderSocketUri,
          targetSocketUri,
          message
        )
      );
    },
    atomReopen: atomUri => dispatch(actionCreators.atoms__reopen(atomUri)),
  };
};

class AtomInfo extends React.Component {
  render() {
    let footerElement;

    if (this.props.showFooter) {
      const reactionUseCaseElements =
        this.props.showReactionUseCases &&
        this.props.reactionUseCasesArray &&
        this.props.reactionUseCasesArray.map((useCase, index) =>
          this.getUseCaseTypeButton(useCase, index)
        );

      const enabledUseCaseElements =
        this.props.showEnabledUseCases &&
        this.props.enabledUseCasesArray &&
        this.props.enabledUseCasesArray.map((useCase, index) =>
          this.getUseCaseTypeButton(useCase, index)
        );

      footerElement = (
        <div className="atom-info__footer">
          {this.props.isInactive &&
            (this.props.isOwned ? (
              <React.Fragment>
                <div className="atom-info__footer__infolabel">
                  This Atom is inactive. Others will not be able to interact
                  with it.
                </div>
                <button
                  className="won-publish-button red won-button--filled"
                  onClick={() => this.props.atomReopen(this.props.atomUri)}
                >
                  Reopen
                </button>
              </React.Fragment>
            ) : (
              <div className="atom-info__footer__infolabel">
                Atom is inactive, no requests allowed
              </div>
            ))}
          {this.props.showAdHocRequestField && (
            <React.Fragment>
              {this.props.chatSocketUri && (
                <ChatTextfield
                  placeholder="Message (optional)"
                  allowEmptySubmit={true}
                  showPersonas={true}
                  submitButtonLabel="Ask&#160;to&#160;Chat"
                  onSubmit={({ value, selectedPersona }) =>
                    this.sendAdHocRequest(
                      value,
                      this.props.chatSocketUri,
                      selectedPersona && selectedPersona.personaId
                    )
                  }
                />
              )}
              {this.props.groupSocketUri && (
                <ChatTextfield
                  placeholder="Message (optional)"
                  allowEmptySubmit={true}
                  showPersonas={true}
                  submitButtonLabel="Join&#160;Group"
                  onSubmit={({ value, selectedPersona }) =>
                    this.sendAdHocRequest(
                      value,
                      this.props.groupSocketUri,
                      selectedPersona && selectedPersona.personaId
                    )
                  }
                />
              )}
            </React.Fragment>
          )}
          {reactionUseCaseElements}
          {enabledUseCaseElements}
        </div>
      );
    }

    return (
      <won-atom-info
        class={
          (this.props.className ? this.props.className : "") +
          (this.props.atomLoading ? " won-is-loading " : "")
        }
      >
        <WonAtomHeaderBig atomUri={this.props.atomUri} />
        <WonAtomMenu
          atomUri={this.props.atomUri}
          defaultTab={this.props.defaultTab}
        />
        <WonAtomContent
          atomUri={this.props.atomUri}
          defaultTab={this.props.defaultTab}
        />
        {footerElement}
      </won-atom-info>
    );
  }

  getUseCaseTypeButton(useCase, index) {
    console.debug("useCase: ", useCase);
    const ucIdentifier = get(useCase, "identifier");
    const ucSenderSocketType = get(useCase, "senderSocketType");
    const ucTargetSocketType = get(useCase, "targetSocketType");

    const atomElements = this.props.ownedReactionAtomsArray
      .filter(atom => atomUtils.matchesDefinition(atom, useCase))
      .map((atom, index) => {
        return (
          <WonAtomHeader
            key={get(atom, "uri") + "-" + index}
            atomUri={get(atom, "uri")}
            hideTimestamp={true}
            onClick={() => {
              this.connectAtomSockets(
                get(atom, "uri"),
                ucSenderSocketType,
                ucTargetSocketType
              );
            }}
          />
        );
      });

    return (
      <React.Fragment>
        <div className="atom-info__footer__header">
          Connect {ucSenderSocketType} with {ucTargetSocketType}:
        </div>
        <div className="atom-info__footer__matches">
          <div className="atom-info__footer__matches__header">
            These existing Atoms you own would match:
          </div>
          <div className="atom-info__footer__matches__list">
            {atomElements}
            <div
              key={ucIdentifier + "-" + index}
              className="atom-info__footer__adhocbutton"
              onClick={() =>
                this.selectUseCase(
                  ucIdentifier,
                  ucSenderSocketType,
                  ucTargetSocketType
                )
              }
            >
              <div className="atom-info__footer__adhocbutton__icon">
                {useCaseUtils.getUseCaseIcon(ucIdentifier) ? (
                  <svg className="atom-info__footer__adhocbutton__icon__svg">
                    <use
                      xlinkHref={useCaseUtils.getUseCaseIcon(ucIdentifier)}
                      href={useCaseUtils.getUseCaseIcon(ucIdentifier)}
                    />
                  </svg>
                ) : (
                  <svg className="atom-info__footer__adhocbutton__icon__svg">
                    <use xlinkHref="ico36_plus" href="ico36_plus" />
                  </svg>
                )}
              </div>
              <div className="atom-info__footer__adhocbutton__right">
                <div className="atom-info__footer__adhocbutton__right__topline">
                  <div className="atom-info__footer__adhocbutton__right__topline__notitle">
                    + Connect with new Atom
                  </div>
                </div>
                <div className="atom-info__footer__adhocbutton__right__subtitle">
                  <span className="atom-info__footer__adhocbutton__right__subtitle__type">
                    <span>{useCaseUtils.getUseCaseLabel(ucIdentifier)}</span>
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </React.Fragment>
    );
  }

  selectUseCase(ucIdentifier, ucSenderSocketType, ucTargetSocketType) {
    this.props.routerGo("create", {
      useCase: ucIdentifier,
      useCaseGroup: undefined,
      connectionUri: undefined,
      fromAtomUri: this.props.atomUri,
      senderSocketType: ucSenderSocketType,
      targetSocketType: ucTargetSocketType,
      viewConnUri: undefined,
      mode: "CONNECT",
      holderUri: this.props.addHolderUri ? this.props.holderUri : undefined,
    });
  }

  connectAtomSockets(
    selectedOwnedAtomUri,
    senderSocketType,
    targetSocketType,
    message = ""
  ) {
    const connectToOwnedAtom = get(this.props.ownedAtoms, this.props.atomUri);

    if (connectToOwnedAtom) {
      //TODO: SERVER SIDE CONNECT OF TWO ATOMS THAT ARE OWNED
    } else {
      const dialogText = "Connect with this Atom?";

      const payload = {
        caption: "Connect",
        text: dialogText,
        buttons: [
          {
            caption: "Yes",
            callback: () => {
              this.props.connect(
                selectedOwnedAtomUri,
                undefined,
                this.props.atomUri,
                message,
                senderSocketType,
                targetSocketType
              );
              this.props.hideModalDialog();

              if (senderSocketType === vocab.CHAT.ChatSocketCompacted) {
                this.props.routerGoResetParams("connections");
              }
            },
          },
          {
            caption: "No",
            callback: () => {
              this.props.hideModalDialog();
            },
          },
        ],
      };
      this.props.showModalDialog(payload);
    }
  }

  sendAdHocRequest(message, connectToSocketUri, personaUri) {
    const _atomUri = this.props.atomUri;

    if (this.props.loggedIn) {
      if (_atomUri) {
        const personaAtom = get(this.props.ownedChatSocketAtoms, personaUri);

        if (personaAtom) {
          const targetSocketType =
            connectToSocketUri === this.props.chatSocketUri
              ? vocab.CHAT.ChatSocketCompacted
              : vocab.GROUP.GroupSocketCompacted;

          // if the personaAtom already contains a chatSocket we will just use the persona as the Atom that connects
          const personaConnections = get(personaAtom, "connections")
            .filter(conn => get(conn, "targetAtomUri") === _atomUri)
            .filter(conn => get(conn, "targetSocketUri") === connectToSocketUri)
            .filter(
              conn =>
                get(conn, "socketUri") === atomUtils.getChatSocket(personaAtom)
            );

          if (personaConnections.size == 0) {
            this.props.connect(
              personaUri,
              undefined,
              _atomUri,
              message,
              vocab.CHAT.ChatSocketCompacted,
              targetSocketType
            );
            this.props.routerGoResetParams("connections");
          } else if (personaConnections.size == 1) {
            const personaConnection = personaConnections.first();
            const personaConnectionUri = get(personaConnection, "uri");

            if (
              connectionUtils.isSuggested(personaConnection) ||
              connectionUtils.isClosed(personaConnection)
            ) {
              this.props.connectSockets(
                get(personaConnection, "socketUri"),
                get(personaConnection, "targetSocketUri"),
                message
              );
            } else if (connectionUtils.isRequestSent(personaConnection)) {
              // Just go to the connection without sending another request
              /*
              //Send another Request with a new message if there is a message present
              this.props.connect(
                personaUri,
                personaConnectionUri,
                _atomUri,
                message,
                vocab.CHAT.ChatSocketCompacted,
                targetSocketType
              );
              */
            } else if (connectionUtils.isRequestReceived(personaConnection)) {
              const senderSocketUri = get(personaConnection, "socketUri");
              const targetSocketUri = get(personaConnection, "targetSocketUri");
              this.props.connectSockets(
                senderSocketUri,
                targetSocketUri,
                message
              );
            } else if (connectionUtils.isConnected(personaConnection)) {
              const senderSocketUri = get(personaConnection, "socketUri");
              const targetSocketUri = get(personaConnection, "targetSocketUri");
              this.props.sendChatMessage(
                message,
                undefined,
                undefined,
                senderSocketUri,
                targetSocketUri,
                personaConnectionUri,
                false
              );
            }

            this.props.routerGo("connections", {
              connectionUri: personaConnectionUri,
            });
          } else {
            console.error(
              "more than one connection stored between two atoms that use the same exact sockets",
              personaAtom,
              connectToSocketUri
            );
          }
        } else {
          this.props.routerGoResetParams("connections");

          this.props.connectionsConnectAdHoc(
            _atomUri,
            message,
            connectToSocketUri,
            personaUri
          );
        }
      }
    } else {
      this.props.showTermsDialog(
        Immutable.fromJS({
          acceptCallback: () => {
            this.props.hideModalDialog();
            this.props.routerGoResetParams("connections");

            if (_atomUri) {
              this.props.connectionsConnectAdHoc(
                _atomUri,
                message,
                connectToSocketUri,
                personaUri
              );
            }
          },
          cancelCallback: () => {
            this.props.hideModalDialog();
          },
        })
      );
    }
  }
}

AtomInfo.propTypes = {
  atomUri: PropTypes.string,
  defaultTab: PropTypes.string,
  loggedIn: PropTypes.bool,
  isInactive: PropTypes.bool,
  isOwned: PropTypes.bool,
  showAdHocRequestField: PropTypes.bool,
  showEnabledUseCases: PropTypes.bool,
  showReactionUseCases: PropTypes.bool,
  reactionUseCasesArray: PropTypes.arrayOf(PropTypes.object),
  enabledUseCasesArray: PropTypes.arrayOf(PropTypes.object),
  ownedReactionAtomsArray: PropTypes.arrayOf(PropTypes.object),
  atomLoading: PropTypes.bool,
  showFooter: PropTypes.bool,
  addHolderUri: PropTypes.string,
  holderUri: PropTypes.string,
  className: PropTypes.string,
  routerGo: PropTypes.func,
  routerGoResetParams: PropTypes.func,
  hideModalDialog: PropTypes.func,
  showModalDialog: PropTypes.func,
  showTermsDialog: PropTypes.func,
  connectionsConnectAdHoc: PropTypes.func,
  ownedAtoms: PropTypes.object,
  ownedChatSocketAtoms: PropTypes.object,
  chatSocketUri: PropTypes.string,
  groupSocketUri: PropTypes.string,
  connect: PropTypes.func,
  connectSockets: PropTypes.func,
  sendChatMessage: PropTypes.func,
  atomReopen: PropTypes.func,
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(AtomInfo);
