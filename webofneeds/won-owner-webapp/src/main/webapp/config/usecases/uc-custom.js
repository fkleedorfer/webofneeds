import { details, emptyDraft } from "../detail-definitions.js";
import { findLatestIntervallEndInJsonLdOrNowAndAddMillis } from "../../app/won-utils.js";

export const customUseCase = {
  identifier: "customUseCase",
  label: "New custom post",
  icon: "#ico36_uc_custom",
  doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: { ...emptyDraft },
  details: details,
  seeksDetails: details,
};