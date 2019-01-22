/**
 * Created by quasarchimaere on 22.01.2019.
 *
 * This Utils-File contains methods to check the status/vars stored in our view state.
 * These methods should be used in favor of accessing the view-state directly in the component
 */

//import won from "./won-es6.js";
import { get } from "./utils.js";

/**
 * Check if showSlideIns is true
 * @param state (view-state)
 * @returns {*}
 */
export function showSlideIns(viewState) {
  return get(viewState, "showSlideIns");
}
