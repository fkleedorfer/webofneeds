import won from "../../app/service/won.js";

import { complain } from "./uc-complain.js";
import { handleComplaint } from "./uc-handle-complaint.js";
import { customUseCase } from "./uc-custom.js";
import { contactPaymentBot } from "./uc-contact-payment-bot.js";

import { details, mergeInEmptyDraft } from "../detail-definitions.js";

export const otherGroup = {
  identifier: "othergroup",
  label: "More...",
  icon: undefined,
  subItems: {
    complain: complain,
    handleComplaint: handleComplaint,
    customUseCase: customUseCase,
    groupChat: {
      identifier: "groupChat",
      label: "New Groupchat Post",
      draft: {
        ...mergeInEmptyDraft({
          content: {
            sockets: {
              "#groupSocket": won.WON.GroupSocketCompacted,
              "#holdableSocket": won.WON.HoldableSocketCompacted,
            },
            defaultSocket: { "#groupSocket": won.WON.GroupSocketCompacted },
          },
        }),
      },
      details: details,
      seeksDetails: details,
    },
    contactPaymentBot: contactPaymentBot,
  },
};
