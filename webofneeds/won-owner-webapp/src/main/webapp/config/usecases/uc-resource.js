import {
  details,
  mergeInEmptyDraft,
  defaultReactions,
} from "../detail-definitions.js";
import {
  name,
  accountingQuantity,
  onhandQuantity,
} from "../details/resource.js";
import vocab from "../../app/service/vocab.js";
import ico36_uc_transport_demand from "~/images/won-icons/ico36_uc_transport_demand.svg";

export const resource = {
  identifier: "resource",
  label: "Thing",
  icon: ico36_uc_transport_demand,
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: ["vf:EconomicResource"],
        sockets: {
          "#PrimaryAccountableInverseSocket":
            vocab.WXVALUEFLOWS.PrimaryAccountableInverseSocketCompacted,
          "#CustodianInverseSocket":
            vocab.WXVALUEFLOWS.CustodianInverseSocketCompacted,
        },
      },
      seeks: {},
    }),
  },
  reactions: {
    ...defaultReactions,
    [vocab.WXVALUEFLOWS.PrimaryAccountableInverseSocketCompacted]: {
      [vocab.WXVALUEFLOWS.PrimaryAccountableSocketCompacted]: {
        useCaseIdentifiers: ["persona"],
      },
    },
    [vocab.WXVALUEFLOWS.CustodianInverseSocketCompacted]: {
      [vocab.WXVALUEFLOWS.CustodianSocketCompacted]: {
        useCaseIdentifiers: ["persona"],
      },
    },
  },
  details: {
    title: { ...details.title },
    name: { ...name },
    accountingQuantity: { ...accountingQuantity },
    onhandQuantity: { ...onhandQuantity },
    location: { ...details.location },
    classifiedAs: { ...details.classifiedAs },
    images: { ...details.images },
  },
  seeksDetails: {},
};
