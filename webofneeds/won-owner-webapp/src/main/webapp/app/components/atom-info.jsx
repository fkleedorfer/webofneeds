import React from "react";
import PropTypes from "prop-types";
import { useSelector } from "react-redux";
import { get } from "../utils.js";
import WonAtomHeaderBig from "./atom-header-big.jsx";
import WonAtomMenu from "./atom-menu.jsx";
import WonAtomFooter from "./atom-footer.jsx";
import WonAtomContent from "./atom-content.jsx";
import * as generalSelectors from "../redux/selectors/general-selectors";
import * as atomUtils from "../redux/utils/atom-utils";
import * as viewUtils from "../redux/utils/view-utils";
import * as processSelectors from "../redux/selectors/process-selectors";

import "~/style/_atom-info.scss";

export default function WonAtomInfo({ atom, className, defaultTab }) {
  const atomUri = get(atom, "uri");
  const isOwned = useSelector(state =>
    generalSelectors.isAtomOwned(state, atomUri)
  );

  const viewState = useSelector(state => get(state, "view"));
  const visibleTab = viewUtils.getVisibleTabByAtomUri(viewState, atomUri);

  const atomLoading = useSelector(
    state => !atom || processSelectors.isAtomLoading(state, atomUri)
  );

  const showFooter =
    !atomLoading &&
    visibleTab === "DETAIL" &&
    (atomUtils.isInactive(atom) ||
      (isOwned && atomUtils.hasEnabledUseCases(atom)) ||
      (!isOwned && atomUtils.hasReactionUseCases(atom)) ||
      (!isOwned &&
        (atomUtils.hasGroupSocket(atom) || atomUtils.hasChatSocket(atom))));

  return (
    <won-atom-info
      class={
        (className ? className : "") + (atomLoading ? " won-is-loading " : "")
      }
    >
      <WonAtomHeaderBig atom={atom} />
      <WonAtomMenu atom={atom} defaultTab={defaultTab} />
      <WonAtomContent atom={atom} defaultTab={defaultTab} />
      {showFooter ? <WonAtomFooter atom={atom} /> : undefined}
    </won-atom-info>
  );
}
WonAtomInfo.propTypes = {
  atom: PropTypes.object,
  defaultTab: PropTypes.string,
  className: PropTypes.string,
};
