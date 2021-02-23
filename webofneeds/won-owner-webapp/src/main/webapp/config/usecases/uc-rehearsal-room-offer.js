/**
 * Created by fsuda on 18.09.2018.
 */
import {
  details,
  mergeInEmptyDraft,
  defaultReactions,
} from "../detail-definitions.js";
import {
  realEstateFloorSizeDetail,
  realEstateNumberOfRoomsDetail,
  realEstateFeaturesDetail,
} from "../details/real-estate.js";
import won from "../../app/service/won.js";
import vocab from "../../app/service/vocab.js";
import { perHourRentDetail } from "../details/musician.js";
import * as jsonLdUtils from "../../app/service/jsonld-utils.js";
import {
  filterNumericProperty,
  filterPrice,
  concatenateFilters,
  sparqlQuery,
} from "../../app/sparql-builder-utils.js";
import ico36_uc_realestate from "../../images/won-icons/ico36_uc_realestate.svg";

export const rehearsalRoomOffer = {
  identifier: "rehearsalRoomOffer",
  label: "Offer Rehearsal Room",
  icon: ico36_uc_realestate,
  timeToLiveMillisDefault: 1000 * 60 * 60 * 24 * 30 * 3,
  doNotMatchAfter: jsonLdUtils.findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: ["demo:RehearsalRoomRentOffer"],
      },
      seeks: {
        type: ["demo:RehearsalRoomRentDemand"],
      },
    }),
  },
  reactions: {
    ...defaultReactions,
    [vocab.CHAT.ChatSocketCompacted]: {
      [vocab.CHAT.ChatSocketCompacted]: {
        useCaseIdentifiers: ["rehearsalRoomSearch"],
        refuseOwned: true,
      },
    },
  },
  details: {
    title: { ...details.title },
    description: { ...details.description },
    location: {
      ...details.location,
      mandatory: true,
    },
    floorSize: {
      ...realEstateFloorSizeDetail,
      mandatory: true,
    },
    numberOfRooms: {
      ...realEstateNumberOfRoomsDetail,
      mandatory: true,
    },
    features: {
      ...realEstateFeaturesDetail,
      placeholder: "e.g. PA, Drumkit",
    },
    rent: {
      ...perHourRentDetail,
      mandatory: true,
    },
    fromDatetime: { ...details.fromDatetime },
    throughDatetime: { ...details.throughDatetime },
    images: { ...details.images },
  },

  seeksDetails: undefined,
  generateQuery: (draft, resultName) => {
    const draftContent = draft && draft.content;
    const location = draftContent && draftContent.location;
    const rent = draftContent && draftContent.rent;
    const floorSize = draftContent && draftContent.floorSize;

    let filter;

    if (location && location.lat && location.lng) {
      const filters = [
        {
          // to select is-branch
          prefixes: {
            won: won.defaultContext["won"],
            rdf: won.defaultContext["rdf"],
            sh: won.defaultContext["sh"], //needed for the filterNumericProperty calls
            s: won.defaultContext["s"],
            geo: "http://www.bigdata.com/rdf/geospatial#",
            xsd: "http://www.w3.org/2001/XMLSchema#",
            match: won.defaultContext["match"],
            demo: won.defaultContext["demo"],
          },
          operations: [
            `${resultName} a won:Atom.`,
            `${resultName} match:seeks ?seeks.`,
            `${resultName} rdf:type demo:RehearsalRoomRentDemand.`,
            "?seeks (won:location|s:location) ?location.",
            "?location s:geo ?location_geo.",
            "?location_geo s:latitude ?location_lat;",
            "s:longitude ?location_lon;",
            `bind (abs(xsd:decimal(?location_lat) - ${
              location.lat
            }) as ?latDiffRaw)`,
            `bind (abs(xsd:decimal(?location_lon) - ${
              location.lng
            }) as ?lonDiff)`,
            "bind (if ( ?latDiffRaw > 180, 360 - ?latDiffRaw, ?latDiffRaw ) as ?latDiff)",
            "bind ( ?latDiff * ?latDiff + ?lonDiff * ?lonDiff as ?location_geoDistanceScore)",
            "bind (?location_geoDistanceScore as ?distScore)",
          ],
        },
        rent && filterPrice("?seeks", rent.amount, rent.currency, "rent"),
        floorSize &&
          filterNumericProperty("?seeks", floorSize, "s:floorSize", "size"),
      ];

      filter = concatenateFilters(filters);
    } else {
      //Location is set to mandatory, hence this clause will never get called
      const filters = [
        {
          // to select is-branch
          prefixes: {
            won: won.defaultContext["won"],
            rdf: won.defaultContext["rdf"],
            sh: won.defaultContext["sh"], //needed for the filterNumericProperty calls
            match: won.defaultContext["match"],
            demo: won.defaultContext["demo"],
          },
          operations: [
            `${resultName} a won:Atom.`,
            `${resultName} match:seeks ?seeks.`,
            `${resultName} rdf:type demo:RehearsalRoomRentDemand.`,
          ],
        },
        rent && filterPrice("?seeks", rent.amount, rent.currency, "rent"),
        floorSize &&
          filterNumericProperty("?seeks", floorSize, "s:floorSize", "size"),
      ];

      filter = concatenateFilters(filters);
    }

    return sparqlQuery({
      prefixes: filter.prefixes,
      distinct: true,
      variables: [resultName],
      where: filter.operations,
      orderBy: [
        {
          order: "ASC",
          variable: "?distScore",
        },
      ],
    });
  },
};
