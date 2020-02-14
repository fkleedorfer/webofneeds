/**
 * Created by fsuda on 18.09.2018.
 */
import { details, mergeInEmptyDraft } from "../detail-definitions.js";
import * as jsonLdUtils from "../../app/service/jsonld-utils.js";
import won from "../../app/service/won.js";

export const handleComplaint = {
  identifier: "handleComplaint",
  label: "Handle complaints",
  icon: "#ico36_uc_wtf_interest",
  timeToLiveMillisDefault: 1000 * 60 * 60 * 24 * 30,
  doNotMatchAfter: jsonLdUtils.findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: ["demo:HandleComplaint"],
        title: "I'll discuss complaints",
        searchString: "wtf",
      },
      seeks: {
        type: ["demo:Complaint"],
      },
    }),
  },
  reactionUseCases: [
    {
      identifier: "complain",
      senderSocketType: won.CHAT.ChatSocketCompacted,
      targetSocketType: won.CHAT.ChatSocketCompacted,
    },
  ],
  details: {
    title: { ...details.title },
    description: { ...details.description },
    location: { ...details.location },
    tags: { ...details.tags },
  },
  seeksDetails: undefined,
};
