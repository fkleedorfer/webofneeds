import React, { useEffect, useState } from "react";

import PropTypes from "prop-types";
import { actionCreators } from "../actions/actions.js";
import { useDispatch, useSelector } from "react-redux";
import * as generalSelectors from "../redux/selectors/general-selectors";
import { get, getIn, toAbsoluteURL, generateLink } from "../utils";
import * as connectionUtils from "../redux/utils/connection-utils";
import * as processUtils from "../redux/utils/process-utils";
import { ownerBaseUrl } from "~/config/default.js";

import "~/style/_context-dropdown.scss";
import ico16_contextmenu from "~/images/won-icons/ico16_contextmenu.svg";
import { Link, useHistory } from "react-router-dom";
import * as atomUtils from "../redux/utils/atom-utils";

export default function WonConnectionContextDropdown({
  connection,
  showPetriNetDataField,
  showAgreementDataField,
  className,
}) {
  const history = useHistory();
  const dispatch = useDispatch();
  const [contextMenuOpen, setContextMenuOpen] = useState(false);
  let thisNode;

  useEffect(() => {
    function handleClick(e) {
      if (!thisNode.contains(e.target) && contextMenuOpen) {
        setContextMenuOpen(false);

        return;
      }
    }

    document.addEventListener("mousedown", handleClick, false);

    return function cleanup() {
      document.removeEventListener("mousedown", handleClick, false);
    };
  });

  const connectionUri = get(connection, "uri");
  const targetAtomUri = getIn(connection, ["targetAtomUri"]);
  const targetAtom = useSelector(state =>
    getIn(state, ["atoms", targetAtomUri])
  );

  let linkToPost;
  if (ownerBaseUrl && targetAtomUri) {
    const path = "#!/post" + `?postUri=${encodeURI(targetAtomUri)}`;

    linkToPost = toAbsoluteURL(ownerBaseUrl).toString() + path;
  }
  const process = useSelector(generalSelectors.getProcessState);
  const adminEmail = useSelector(state =>
    getIn(state, ["config", "theme", "adminEmail"])
  );
  const isConnectionToGroup =
    atomUtils.getGroupSocket(targetAtom) === get(connection, "targetSocketUri");
  const showAgreementData = get(connection, "showAgreementData");
  const showPetriNetData = get(connection, "showPetriNetData");
  const isClosed = connectionUtils.isClosed(connection);
  const isConnected = connectionUtils.isConnected(connection);
  const isSentRequest = connectionUtils.isRequestSent(connection);
  const isReceivedRequest = connectionUtils.isRequestReceived(connection);
  const isSuggested = connectionUtils.isSuggested(connection);
  const isTargetAtomUsableAsTemplate = useSelector(state =>
    generalSelectors.isAtomUsableAsTemplate(state, targetAtomUri)
  );
  const isTargetAtomEditable = useSelector(state =>
    generalSelectors.isAtomEditable(state, targetAtomUri)
  );
  const connectionLoading =
    !connection || processUtils.isConnectionLoading(process, connectionUri);

  function generateReportPostMailParams() {
    const subject = `[Report Post] - ${targetAtomUri}`;
    const body = `Link to Post: ${linkToPost}%0D%0AReason:%0D%0A`; //hint: %0D%0A adds a linebreak

    return `subject=${subject}&body=${body}`;
  }

  function generateCloseConnectionLabel() {
    if (isConnected) {
      return "Close Connection";
    } else if (isSuggested) {
      return "Remove Connection";
    } else if (isSentRequest) {
      return "Cancel Request";
    } else if (isReceivedRequest) {
      return "Deny Request";
    } else if (isClosed) {
      return "Connection closed";
    }
  }

  function closeConnection() {
    const payload = {
      caption: "Attention!",
      text: "Do you want to close the connection?",
      buttons: [
        {
          caption: "Yes",
          callback: () => {
            dispatch(actionCreators.connections__close(connectionUri));
            history.push(
              generateLink(history.location, {
                useCase: undefined,
                connectionUri: undefined,
              })
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

  const iconElement = connectionLoading ? (
    <svg className="cdd__icon__small">
      <use xlinkHref={ico16_contextmenu} href={ico16_contextmenu} />
    </svg>
  ) : (
    <svg
      className="cdd__icon__small clickable"
      onClick={() => setContextMenuOpen(true)}
    >
      <use xlinkHref={ico16_contextmenu} href={ico16_contextmenu} />
    </svg>
  );

  let dropdownElement;

  if (contextMenuOpen) {
    const buttons = [];

    !isSuggested &&
      buttons.push(
        <button
          key="details"
          className="won-button--outlined thin red"
          onClick={() =>
            history.push(
              generateLink(
                history.location,
                {
                  postUri: targetAtomUri,
                },
                "/post"
              )
            )
          }
        >
          Show Details
        </button>
      );

    !isConnectionToGroup &&
      isConnected &&
      !showAgreementData &&
      buttons.push(
        <button
          key="agrdata"
          className="won-button--outlined thin red"
          onClick={() => showAgreementDataField()}
        >
          Show Agreement Data
        </button>
      );

    !isConnectionToGroup &&
      isConnected &&
      !showPetriNetData &&
      buttons.push(
        <button
          key="petrinetdata"
          className="won-button--outlined thin red"
          onClick={() => showPetriNetDataField()}
        >
          Show PetriNet Data
        </button>
      );
    isTargetAtomUsableAsTemplate &&
      buttons.push(
        <Link
          key="duplicate"
          className="won-button--outlined thin red"
          to={location =>
            generateLink(
              location,
              {
                fromAtomUri: targetAtomUri,
                mode: "DUPLICATE",
              },
              "/create"
            )
          }
        >
          Post this too!
        </Link>
      );
    isTargetAtomEditable &&
      buttons.push(
        <Link
          key="edit"
          className="won-button--outlined thin red"
          to={location =>
            generateLink(
              location,
              {
                fromAtomUri: targetAtomUri,
                mode: "EDIT",
              },
              "/create"
            )
          }
        >
          Edit
        </Link>
      );
    adminEmail &&
      buttons.push(
        <a
          key="report"
          className="won-button--outlined thin red"
          href={"mailto:" + adminEmail + "?" + generateReportPostMailParams()}
        >
          Report
        </a>
      );
    buttons.push(
      <button
        key="close"
        className="won-button--filled red"
        onClick={() => closeConnection()}
        disabled={isClosed}
      >
        {generateCloseConnectionLabel()}
      </button>
    );

    dropdownElement = (
      <div className="cdd__contextmenu">
        <div
          className="cdd__contextmenu__content"
          onClick={() => setContextMenuOpen(false)}
        >
          <div className="topline">
            <svg className="cdd__icon__small__contextmenu clickable">
              <use xlinkHref={ico16_contextmenu} href={ico16_contextmenu} />
            </svg>
          </div>
          {/* Buttons when connection is available -->*/}
          {buttons}
        </div>
      </div>
    );
  }

  return (
    <won-connection-context-dropdown
      ref={node => (thisNode = node)}
      class={className ? className : ""}
    >
      {iconElement}
      {dropdownElement}
    </won-connection-context-dropdown>
  );
}
WonConnectionContextDropdown.propTypes = {
  connection: PropTypes.object,
  showPetriNetDataField: PropTypes.func,
  showAgreementDataField: PropTypes.func,
  className: PropTypes.string,
};
