/**
 * Created by fsuda on 18.09.2018.
 */
import {
  details,
  mergeInEmptyDraft,
  defaultReactions,
} from "../detail-definitions.js";
import { interestsDetail, skillsDetail } from "../details/person.js";
import * as jsonLdUtils from "../../app/service/jsonld-utils.js";
import vocab from "../../app/service/vocab.js";
import ico36_uc_consortium_search from "../../images/won-icons/ico36_uc_consortium-search.svg";

export const consortiumSearch = {
  identifier: "consortiumSearch",
  label: "Find a project consortium to join",
  icon: ico36_uc_consortium_search,
  doNotMatchAfter: jsonLdUtils.findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: ["demo:ConsortiumSearch"],
        tags: ["search-consortium"],
        searchString: "offer-consortium",
      },
    }),
  },
  reactions: {
    ...defaultReactions,
    [vocab.CHAT.ChatSocketCompacted]: {
      [vocab.CHAT.ChatSocketCompacted]: {
        useCaseIdentifiers: ["consortiumOffer", "persona"],
        refuseOwned: true,
      },
    },
  },
  details: {
    title: { ...details.title },
    description: { ...details.description },
    location: { ...details.location },
    skills: { ...skillsDetail, placeholder: "" }, // TODO: find good placeholders
    interests: { ...interestsDetail, placeholder: "" },
  },
  seeksDetails: {
    description: { ...details.description },
    location: { ...details.location },
  },
};
