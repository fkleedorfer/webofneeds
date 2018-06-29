import won from "../won-es6.js";
import Immutable from "immutable";
import angular from "angular";
import chatTextFieldSimpleModule from "./chat-textfield-simple.js";
import connectionMessageModule from "./connection-message.js";
import connectionHeaderModule from "./connection-header.js";
import labelledHrModule from "./labelled-hr.js";
import connectionContextDropdownModule from "./connection-context-dropdown.js";

import { ownerBaseUrl } from "config";
import urljoin from "url-join";

import { connect2Redux } from "../won-utils.js";
import { attach, delay } from "../utils.js";
import {
  callAgreementsFetch,
  callAgreementEventFetch,
} from "../won-message-utils.js";
import { actionCreators } from "../actions/actions.js";
import {
  selectOpenConnectionUri,
  selectNeedByConnectionUri,
} from "../selectors.js";
import autoresizingTextareaModule from "../directives/textarea-autogrow.js";
import { classOnComponentRoot } from "../cstm-ng-utils.js";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];

function genComponentConf() {
  let template = `
        <div class="pm__header" ng-if="!self.showAgreementData">
            <a class="pm__header__back clickable show-in-responsive"
               ng-click="self.router__stateGoCurrent({connectionUri : undefined})">
                <svg style="--local-primary:var(--won-primary-color);"
                     class="pm__header__back__icon clickable">
                    <use xlink:href="#ico36_backarrow" href="#ico36_backarrow"></use>
                </svg>
            </a>
            <won-connection-header
                connection-uri="self.connection.get('uri')"
                timestamp="self.lastUpdateTimestamp"
                hide-image="::false">
            </won-connection-header>
            <won-connection-context-dropdown ng-if="self.isConnected || self.isSentRequest || self.isReceivedRequest" show-agreement-data-field="::self.showAgreementDataField()"></won-connection-context-dropdown>
        </div>
        <div class="pm__header" ng-if="self.showAgreementData">
            <a class="pm__header__back clickable"
                ng-click="self.setShowAgreementData(false)">
                <svg style="--local-primary:var(--won-primary-color);"
                     class="pm__header__back__icon clickable">
                    <use xlink:href="#ico36_backarrow" href="#ico36_backarrow"></use>
                </svg>
            </a>
            <div class="pm__header__title clickable"
                ng-click="self.setShowAgreementData(false)">
              Showing Agreement Data
            </div>
            <won-connection-context-dropdown ng-if="self.isConnected || self.isSentRequest || self.isReceivedRequest" show-agreement-data-field="::self.showAgreementDataField()"></won-connection-context-dropdown>
        </div>
        <div class="pm__content" ng-class="{'won-agreement-content': self.showAgreementData}">
            <div class="pm__content__loadspinner"
                ng-if="self.connection.get('isLoadingMessages')">
                <img src="images/spinner/on_white.gif"
                    alt="Loading&hellip;"
                    class="hspinner"/>
            </div>
            <button class="pm__content__loadbutton won-button--outlined thin red"
                ng-if="!self.showAgreementData && !self.connection.get('isLoadingMessages') && !self.allLoaded"
                ng-click="self.loadPreviousMessages()">
                Load previous messages
            </button>
            <won-connection-message
                ng-if="!self.showAgreementData"
                ng-repeat="msg in self.sortedMessages"
                connection-uri="self.connectionUri"
                message-uri="msg.get('uri')"
                on-update="self.setShowAgreementData(false)"
                on-send-proposal="[self.addProposal(proposalUri), self.setShowAgreementData(false)]"
                on-remove-data="[self.filterMessages(proposalUri), self.setShowAgreementData(false)]">
            </won-connection-message>

            <div class="pm__content__agreement__title" ng-if="self.showAgreementData && self.hasAgreementUris && !self.connection.get('isLoadingMessages')">
              Agreements
            </div>
            <won-connection-message
              ng-if="self.showAgreementData && !self.connection.get('isLoadingMessages')"
              ng-repeat="agreement in self.agreementUrisArray"
              connection-uri="self.connectionUri"
              message-uri="agreement.get('stateUri')"
              on-update="self.setShowAgreementData(false)"
              on-send-proposal="[self.addProposal(proposalUri), self.setShowAgreementData(false)]"
              on-remove-data="[self.filterMessages(proposalUri), self.setShowAgreementData(false)]">
            </won-connection-message>
            <div class="pm__content__agreement__title" ng-if="self.showAgreementData && self.hasCancellationPendingAgreementUris && !self.connection.get('isLoadingMessages')">
              Agreements with Pending Cancellation
            </div>
            <won-connection-message
              ng-if="self.showAgreementData && !self.connection.get('isLoadingMessages')"
              ng-repeat="proposeToCancel in self.cancellationPendingAgreementUrisArray"
              connection-uri="self.connectionUri"
              message-uri="proposeToCancel.get('stateUri')"
              on-update="self.setShowAgreementData(false)"
              on-send-proposal="[self.addProposal(proposalUri), self.setShowAgreementData(false)]"
              on-remove-data="[self.filterMessages(proposalUri), self.setShowAgreementData(false)]">
            </won-connection-message>
            <div class="pm__content__agreement__title" ng-if="self.showAgreementData && self.hasPendingProposalUris && !self.connection.get('isLoadingMessages')">
              Open Proposals
            </div>
            <won-connection-message
              ng-if="self.showAgreementData && !self.connection.get('isLoadingMessages')"
              ng-repeat="proposal in self.pendingProposalUrisArray"
              connection-uri="self.connectionUri"
              message-uri="proposal.get('stateUri')"
              on-update="self.setShowAgreementData(false)"
              on-send-proposal="[self.addProposal(proposalUri), self.setShowAgreementData(false)]"
              on-remove-data="[self.filterMessages(proposalUri), self.setShowAgreementData(false)]">
            </won-connection-message>
            <a class="rdflink clickable"
               ng-if="self.shouldShowRdf"
               target="_blank"
               href="{{ self.connection.get('uri') }}">
                    <svg class="rdflink__small">
                        <use xlink:href="#rdf_logo_1" href="#rdf_logo_1"></use>
                    </svg>
                    <span class="rdflink__label">Connection</span>
            </a>
        </div>
        <div class="pm__footer" ng-if="self.isConnected">
            <chat-textfield-simple
                class="pm__footer__chattexfield"
                placeholder="self.shouldShowRdf? 'Enter TTL...' : 'Your message...'"
                submit-button-label="self.shouldShowRdf? 'Send RDF' : 'Send'"
                on-submit="self.send(value, self.shouldShowRdf)"
                help-text="self.shouldShowRdf? self.rdfTextfieldHelpText : ''"
                allow-empty-submit="::false"
                is-code="self.shouldShowRdf? 'true' : ''"
            >
            </chat-textfield-simple>
        </div>
        <div class="pm__footer" ng-if="self.isSentRequest">
            Waiting for them to accept your chat request.
        </div>

        <div class="pm__footer" ng-if="self.isReceivedRequest">
            <chat-textfield-simple
                class="pm__footer__chattexfield"
                placeholder="::'Message (optional)'"
                on-submit="::self.openRequest(value)"
                allow-empty-submit="::true"
                submit-button-label="::'Accept Chat'"
            >
            </chat-textfield-simple>
            <won-labelled-hr label="::'Or'" class="pm__footer__labelledhr"></won-labelled-hr>
            <button class="pm__footer__button won-button--filled black" ng-click="self.closeConnection()">
                Decline
            </button>
        </div>
    `;

  class Controller {
    constructor(/* arguments = dependency injections */) {
      attach(this, serviceDependencies, arguments);
      window.pm4dbg = this;

      this.agreementHead = this.cloneDefaultData();
      this.agreementLoading = this.cloneDefaultStateData();

      this.rdfTextfieldHelpText =
        "Expects valid turtle. " +
        `<${won.WONMSG.uriPlaceholder.event}> will ` +
        "be replaced by the uri generated for this message. " +
        "Use it, so your TTL can be found when parsing the messages. " +
        "See `won.defaultTurtlePrefixes` " +
        "for prefixes that will be added automatically. E.g." +
        `\`<${
          won.WONMSG.uriPlaceholder.event
        }> won:hasTextMessage "hello world!". \``;

      this.scrollContainer().addEventListener("scroll", e => this.onScroll(e));

      const selectFromState = state => {
        const connectionUri = selectOpenConnectionUri(state);
        const ownNeed = selectNeedByConnectionUri(state, connectionUri);
        const connection =
          ownNeed && ownNeed.getIn(["connections", connectionUri]);

        const theirNeed =
          connection && state.getIn(["needs", connection.get("remoteNeedUri")]);
        const chatMessages = connection && connection.get("messages");
        const allLoaded =
          chatMessages &&
          chatMessages.filter(
            msg => msg.get("messageType") === won.WONMSG.connectMessage
          ).size > 0;

        const agreementData = connection && connection.get("agreementData");
        const agreementUris =
          agreementData && agreementData.get("agreementUris");
        const cancellationPendingAgreementUris =
          agreementData &&
          agreementData.get("cancellationPendingAgreementUris");
        const pendingProposalUris =
          agreementData && agreementData.get("pendingProposalUris");

        //Filter already accepted proposals
        let sortedMessages = chatMessages && chatMessages.toArray();
        if (sortedMessages) {
          const msgSet = new Set(sortedMessages);

          // TODO: Optimization
          for (const msg of msgSet) {
            if (
              msg.get("isProposeMessage") ||
              msg.get("isProposeToCancel") ||
              msg.get("isAcceptMessage")
            ) {
              if (msg.get("isRelevant") && this.isOldAgreementMsg(msg)) {
                this.messages__markAsRelevant({
                  messageUri: msg.get("uri"),
                  connectionUri: connectionUri,
                  needUri: ownNeed.get("uri"),
                  relevant: false,
                });
              }
            } else if (this.agreementHead.get("retractedMessageUris").size) {
              //TODO: filter out retracted messages faster
              if (msg.get("isRelevant") && this.isOldAgreementMsg(msg)) {
                this.messages__markAsRelevant({
                  messageUri: msg.get("uri"),
                  connectionUri: connectionUri,
                  needUri: ownNeed.get("uri"),
                  relevant: false,
                });
              }
            }
          }

          sortedMessages = Array.from(msgSet);
          sortedMessages.sort(function(a, b) {
            return a.get("date").getTime() - b.get("date").getTime();
          });
        }

        return {
          ownNeed,
          theirNeed,
          connectionUri,
          connection,

          sortedMessages: sortedMessages,
          chatMessages,
          isLoadingMessages: connection && connection.get("isLoadingMessages"),
          showAgreementData: connection && connection.get("showAgreementData"),
          lastUpdateTimestamp: connection && connection.get("lastUpdateDate"),
          isSentRequest:
            connection && connection.get("state") === won.WON.RequestSent,
          isReceivedRequest:
            connection && connection.get("state") === won.WON.RequestReceived,
          isConnected:
            connection && connection.get("state") === won.WON.Connected,
          debugmode: won.debugmode,
          shouldShowRdf: state.get("showRdf"),
          // if the connect-message is here, everything else should be as well
          allLoaded,
          //agreementUrisToDisplay
          agreementData,
          hasAgreementUris: agreementUris && agreementUris.size > 0,
          hasCancellationPendingAgreementUris:
            cancellationPendingAgreementUris &&
            cancellationPendingAgreementUris.size > 0,
          hasPendingProposalUris:
            pendingProposalUris && pendingProposalUris.size > 0,
          agreementUrisArray: agreementUris && agreementUris.toArray(),
          cancellationPendingAgreementUrisArray:
            cancellationPendingAgreementUris &&
            cancellationPendingAgreementUris.toArray(),
          pendingProposalUrisArray:
            pendingProposalUris && pendingProposalUris.toArray(),
        };
      };

      connect2Redux(selectFromState, actionCreators, [], this);

      this.snapToBottom();

      this.$scope.$watchGroup(["self.connection"], () =>
        this.ensureMessagesAreLoaded()
      );

      this.$scope.$watch(
        () => this.sortedMessages && this.sortedMessages.length, // trigger if there's messages added (or removed)
        () =>
          delay(0).then(() =>
            // scroll to bottom directly after rendering, if snapped
            this.updateScrollposition()
          )
      );

      classOnComponentRoot("won-is-loading", () => this.isLoading(), this);
    }

    isLoading() {
      return (
        !this.connection ||
        !this.theirNeed ||
        !this.ownNeed ||
        this.ownNeed.get("isLoading") ||
        this.theirNeed.get("isLoading") ||
        this.connection.get("isLoading")
      );
    }

    ensureMessagesAreLoaded() {
      delay(0).then(() => {
        // make sure latest messages are loaded
        const INITIAL_MESSAGECOUNT = 15;
        if (
          this.connection &&
          !this.connection.get("isLoadingMessages") &&
          !(this.allLoaded || this.connection.get("messages").size > 0)
        ) {
          this.connections__showLatestMessages(
            this.connection.get("uri"),
            INITIAL_MESSAGECOUNT
          );
        }
      });
    }

    getAgreementData() {
      this.connections__setLoadingMessages({
        connectionUri: this.connectionUri,
        isLoadingMessages: true,
      });

      this.getAgreementDataUris();
    }

    loadPreviousMessages() {
      delay(0).then(() => {
        const MORE_MESSAGECOUNT = 5;
        if (this.connection && !this.connection.get("isLoadingMessages")) {
          this.connections__showMoreMessages(
            this.connection.get("uri"),
            MORE_MESSAGECOUNT
          );
        }
      });
    }

    snapToBottom() {
      this._snapBottom = true;
      this.scrollToBottom();
    }
    unsnapFromBottom() {
      this._snapBottom = false;
    }
    updateScrollposition() {
      if (this._snapBottom) {
        this.scrollToBottom();
      }
    }
    scrollToBottom() {
      this._programmaticallyScrolling = true;

      this.scrollContainer().scrollTop = this.scrollContainer().scrollHeight;
    }
    onScroll() {
      if (!this._programmaticallyScrolling) {
        //only unsnap if the user scrolled themselves
        this.unsnapFromBottom();
      }

      const sc = this.scrollContainer();
      const isAtBottom = sc.scrollTop + sc.offsetHeight >= sc.scrollHeight;
      if (isAtBottom) {
        this.snapToBottom();
      }

      this._programmaticallyScrolling = false;
    }
    scrollContainerNg() {
      return angular.element(this.scrollContainer());
    }
    scrollContainer() {
      if (!this._scrollContainer) {
        this._scrollContainer = this.$element[0].querySelector(".pm__content");
      }
      return this._scrollContainer;
    }

    send(chatMessage, isTTL = false) {
      this.setShowAgreementData(false);
      const trimmedMsg = chatMessage.trim();
      if (trimmedMsg) {
        this.connections__sendChatMessage(
          trimmedMsg,
          this.connection.get("uri"),
          isTTL
        );
      }
    }

    showAgreementDataField() {
      this.getAgreementData();
      this.setShowAgreementData(true);
    }

    setShowAgreementData(value) {
      this.connections__showAgreementData({
        connectionUri: this.connectionUri,
        showAgreementData: value,
      });
    }

    encodeParam(param) {
      return encodeURIComponent(param);
    }

    getAgreementDataUris() {
      const url = urljoin(
        ownerBaseUrl,
        "/rest/agreement/getAgreementProtocolUris",
        `?connectionUri=${this.encodeParam(this.connection.get("uri"))}`
      );
      let hasChanged = false;
      callAgreementsFetch(url)
        .then(response => {
          this.agreementHead = this.transformDataToSet(response);

          const agreementUris = this.agreementHead.get("agreementUris");
          const pendingProposalUris = this.agreementHead.get(
            "pendingProposalUris"
          );
          const cancellationPendingAgreementUris = this.agreementHead.get(
            "cancellationPendingAgreementUris"
          );

          agreementUris.map(data => {
            this.addAgreementDataToState(data, "agreementUris");
            hasChanged = true;
          });
          pendingProposalUris.map(data => {
            this.addAgreementDataToState(data, "pendingProposalUris");
            hasChanged = true;
          });
          cancellationPendingAgreementUris.map(data => {
            this.addAgreementDataToState(
              data,
              "cancellationPendingAgreementUris"
            );
            hasChanged = true;
          });

          //no data found for keyset: no relevant agreementData to show in GUI - clean state data
          if (!hasChanged) {
            this.connections__clearAgreementData({
              connectionUri: this.connectionUri,
            });
          }
          //Remove all retracted/rejected messages
          else if (
            this.agreementData &&
            (this.agreementHead.get("rejectedMessageUris") ||
              this.agreementHead.get("retractedMessageUris"))
          ) {
            const pendingProposalUris = this.agreementData.get(
              "pendingProposalUris"
            );
            const pendingProposalUrisWithout =
              pendingProposalUris &&
              pendingProposalUris
                .subtract(this.agreementHead.get("rejectedMessageUris"))
                .subtract(this.agreementHead.get("retractedMessageUris"));

            this.agreementData = this.agreementData.set(
              "pendingProposalUris",
              pendingProposalUrisWithout
            );

            if (
              pendingProposalUris &&
              pendingProposalUrisWithout &&
              pendingProposalUris.size != pendingProposalUrisWithout.size
            ) {
              hasChanged = true;
              this.connections__clearAgreementData({
                connectionUri: this.connectionUri,
              });
            }
          }
        })
        .then(() => {
          if (!hasChanged) {
            this.connections__setLoadingMessages({
              connectionUri: this.connectionUri,
              isLoadingMessages: false,
            });
          }
        })
        .catch(error => {
          console.error("Error:", error);
          this.connections__setLoadingMessages({
            connectionUri: this.connectionUri,
            isLoadingMessages: false,
          });
        });
    }

    transformDataToSet(response) {
      const tmpAgreementData = Immutable.fromJS({
        agreementUris: Immutable.Set(response.agreementUris),
        pendingProposalUris: Immutable.Set(response.pendingProposalUris),
        pendingProposals: Immutable.Set(response.pendingProposals),
        acceptedCancellationProposalUris: Immutable.Set(
          response.acceptedCancellationProposalUris
        ),
        cancellationPendingAgreementUris: Immutable.Set(
          response.cancellationPendingAgreementUris
        ),
        pendingCancellationProposalUris: Immutable.Set(
          response.pendingCancellationProposalUris
        ),
        cancelledAgreementUris: Immutable.Set(response.cancelledAgreementUris),
        rejectedMessageUris: Immutable.Set(response.rejectedMessageUris),
        retractedMessageUris: Immutable.Set(response.retractedMessageUris),
      });

      return this.filterAgreementSet(tmpAgreementData);
    }

    filterAgreementSet(tmpAgreementData) {
      const cancellationPendingAgreementUris =
        tmpAgreementData &&
        tmpAgreementData.get("cancellationPendingAgreementUris");
      const agreementUrisWithout =
        tmpAgreementData &&
        tmpAgreementData
          .get("agreementUris")
          .subtract(cancellationPendingAgreementUris);

      return tmpAgreementData.set("agreementUris", agreementUrisWithout);
    }

    addAgreementDataToState(eventUri, key, obj) {
      const ownNeedUri = this.ownNeed.get("uri");
      return callAgreementEventFetch(ownNeedUri, eventUri).then(response => {
        won.wonMessageFromJsonLd(response).then(msg => {
          let agreementObject = obj;

          if (msg.isFromOwner() && msg.getReceiverNeed() === ownNeedUri) {
            /*if we find out that the receiverneed of the crawled event is actually our
                         need we will call the method again but this time with the correct eventUri
                         */
            if (!agreementObject) {
              agreementObject = Immutable.fromJS({
                stateUri: undefined,
                headUri: undefined,
              });
            }
            agreementObject = agreementObject.set(
              "headUri",
              msg.getMessageUri()
            );
            this.addAgreementDataToState(
              msg.getRemoteMessageUri(),
              key,
              agreementObject
            );
          } else {
            if (!agreementObject) {
              agreementObject = Immutable.fromJS({
                stateUri: undefined,
                headUri: undefined,
              });
              agreementObject = agreementObject.set(
                "headUri",
                msg.getMessageUri()
              );
            }
            agreementObject = agreementObject.set(
              "stateUri",
              msg.getMessageUri()
            );

            //this.agreementLoadingJS[key].add(agreementObject);
            //TODO: does the line below do the same?
            this.agreementLoading = this.agreementLoading.set(
              key,
              this.agreementLoading.get(key).add(agreementObject)
            );

            //If message isnt in the state we add it
            if (this.chatMessages.get(agreementObject.get("stateUri"))) {
              this.messages__connectionMessageReceived(msg);
            }

            //Update agreementData in State
            this.connections__updateAgreementData({
              connectionUri: this.connectionUri,
              agreementData: this.agreementLoading,
            });
          }
        });
      });
    }

    checkObject(key, agreementObject, del) {
      const foundKeys = this.agreementData.get(key);

      const foundObjects =
        foundKeys &&
        foundKeys.filter(
          object => object.get("stateUri") === agreementObject.get("stateUri")
        );

      if (foundKeys && del) {
        this.agreementData = this.agreementData.set(
          key,
          foundKeys.filter(
            object => object.get("stateUri") !== agreementObject.get("stateUri")
          )
        );
      }

      return foundObjects && foundObjects.size > 0;
    }

    filterMessages(stateUri) {
      const object = Immutable.fromJS({
        stateUri: stateUri,
        headUri: undefined,
      });

      this.checkObject("agreementUris", object, true);
      this.checkObject("pendingProposalUris", object, true);
      this.checkObject("cancellationPendingAgreementUris", object, true);
    }

    isOldAgreementMsg(msg) {
      if (
        !this.agreementHead &&
        (this.agreementHead.getIn(["agreementUris", msg.get("uri")]) ||
          this.agreementHead.getIn(["agreementUris", msg.get("remoteUri")]) ||
          this.agreementHead.getIn([
            "cancellationPendingAgreementUris",
            msg.get("uri"),
          ]) ||
          this.agreementHead.getIn([
            "cancellationPendingAgreementUris",
            msg.get("remoteUri"),
          ]) ||
          this.agreementHead.getIn([
            "cancelledAgreementUris",
            msg.get("uri"),
          ]) ||
          this.agreementHead.getIn([
            "cancelledAgreementUris",
            msg.get("remoteUri"),
          ]) ||
          this.agreementHead.getIn([
            "acceptedCancellationProposalUris",
            msg.get("uri"),
          ]) ||
          this.agreementHead.getIn([
            "acceptedCancellationProposalUris",
            msg.get("remoteUri"),
          ]) ||
          this.agreementHead.getIn(["retractedMessageUris", msg.get("uri")]) ||
          this.agreementHead.getIn([
            "retractedMessageUris",
            msg.get("remoteUri"),
          ]) ||
          this.agreementHead.getIn(["rejectedMessageUris", msg.get("uri")]) ||
          this.agreementHead.getIn([
            "rejectedMessageUris",
            msg.get("remoteUri"),
          ]))
      ) {
        return true;
      }
      return false;
    }

    cloneDefaultData() {
      return Immutable.fromJS({
        agreementUris: Immutable.Set(),
        pendingProposalUris: Immutable.Set(),
        pendingProposals: Immutable.Set(),
        acceptedCancellationProposalUris: Immutable.Set(),
        cancellationPendingAgreementUris: Immutable.Set(),
        pendingCancellationProposalUris: Immutable.Set(),
        cancelledAgreementUris: Immutable.Set(),
        rejectedMessageUris: Immutable.Set(),
        retractedMessageUris: Immutable.Set(),
      });
    }

    cloneDefaultStateData() {
      return Immutable.fromJS({
        pendingProposalUris: Immutable.Set(),
        agreementUris: Immutable.Set(),
        cancellationPendingAgreementUris: Immutable.Set(),
      });
    }

    openRequest(message) {
      this.connections__open(this.connectionUri, message);
    }

    closeConnection() {
      this.connections__close(this.connection.get("uri"));
      this.router__stateGoCurrent({ connectionUri: null });
    }
  }
  Controller.$inject = serviceDependencies;

  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    scope: {},
    template: template,
  };
}

export default angular
  .module("won.owner.components.postMessages", [
    autoresizingTextareaModule,
    chatTextFieldSimpleModule,
    connectionMessageModule,
    connectionHeaderModule,
    labelledHrModule,
    connectionContextDropdownModule,
  ])
  .directive("wonPostMessages", genComponentConf).name;
