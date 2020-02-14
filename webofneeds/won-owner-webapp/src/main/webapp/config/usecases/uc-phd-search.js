/**
 * Created by fsuda on 18.09.2018.
 */
import { details, mergeInEmptyDraft } from "../detail-definitions.js";
import { interestsDetail, skillsDetail } from "../details/person.js";
import * as jsonLdUtils from "../../app/service/jsonld-utils.js";
import won from "../../app/service/won.js";

export const phdSearch = {
  identifier: "phdSearch",
  label: "Find a PhD position",
  icon: "#ico36_uc_phd",
  doNotMatchAfter: jsonLdUtils.findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: ["demo:PhdPositionSearch"],
        title: "I'm looking for a PhD position!",
        tags: ["search-phd"],
        searchString: "offer-phd",
      },
      seeks: {
        type: ["demo:PhdPosition"],
      },
    }),
  },
  reactionUseCases: [
    {
      identifier: "phdOffer",
      senderSocketType: won.CHAT.ChatSocketCompacted,
      targetSocketType: won.CHAT.ChatSocketCompacted,
    },
  ],
  details: {
    title: { ...details.title },
    description: { ...details.description },
    person: { ...details.person },
    skills: { ...skillsDetail, placeholder: "" }, // TODO: find good placeholders
    interests: { ...interestsDetail, placeholder: "" },
  },
  seeksDetails: {
    description: { ...details.description },
    location: { ...details.location },
  },
};
