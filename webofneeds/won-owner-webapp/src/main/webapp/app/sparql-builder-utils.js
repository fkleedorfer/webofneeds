/**
 * Module for utility-functions for string-building sparql-queries.
 * NOTE: This is a super-hacky/-fragile approach and should be replaced by a proper lib / ast-utils
 */

import won from "./won-es6.js";
import { is } from "./utils.js";

/**
 * returns e.g.:
 * ```
 * {
 *   prefixes: { s: "http://schema.org/", ...},
 *   filterStrings: [ "FILTER (?currency = 'EUR')",... ],
 *   basicGraphPattern: [ "?is s:priceSpecification ?pricespec .", ...],
 * }
 * ```
 * The defaults are empty objects and arrays as properties.
 *
 * @param {*} returnValue
 */
function wellFormedFilterReturn(returnValue) {
  return Object.assign(
    {
      prefixes: {},
      filterStrings: [],
      basicGraphPattern: [],
    },
    returnValue
  );
}
/**
 *
 * @param {*} location: an object containing `lat` and `lng`
 * @param {Number} radius: distance in km that matches can be away from the location
 * @returns see wellFormedFilterReturn
 */
export function filterInVicinity(location, radius = 10) {
  if (!location || !location.lat || !location.lng) {
    return wellFormedFilterReturn();
  } else {
    return wellFormedFilterReturn({
      prefixes: {
        s: won.defaultContext["s"],
        won: won.defaultContext["won"],
        geo: "http://www.bigdata.com/rdf/geospatial#",
        geoliteral: "http://www.bigdata.com/rdf/geospatial/literals/v1#",
      },
      filterStrings: [
        `?result won:is/won:hasLocation/s:geo ?geo
SERVICE geo:search {
  ?geo geo:search "inCircle" .
  ?geo geo:searchDatatype geoliteral:lat-lon .
  ?geo geo:predicate won:geoSpatial .
  ?geo geo:spatialCircleCenter "${location.lat}#${location.lng}" .
  ?geo geo:spatialCircleRadius "${radius}" .
  ?geo geo:distanceValue ?geoDistance .
}`,
      ],
    });
  }
}

export function filterFloorSizeRange(min, max) {
  const basicGraphPattern = [];
  const filterStrings = [];
  const minIsNum = is("Number", min);
  const maxIsNum = is("Number", max);
  if (minIsNum || maxIsNum) {
    basicGraphPattern.push("?is s:floorSize/s:value ?floorSize.");
  }
  if (minIsNum) {
    filterStrings.push("FILTER (?floorSize >= " + min + " )");
  }
  if (maxIsNum) {
    filterStrings.push("FILTER (?floorSize <= " + max + " )");
  }
  return wellFormedFilterReturn({ basicGraphPattern, filterStrings });
}

export function filterNumOfRoomsRange(min, max) {
  const basicGraphPattern = [];
  const filterStrings = [];
  const minIsNum = is("Number", min);
  const maxIsNum = is("Number", max);
  if (minIsNum || maxIsNum) {
    basicGraphPattern.push("?is s:numberOfRooms ?numberOfRooms.");
  }
  if (minIsNum) {
    filterStrings.push("FILTER (?numberOfRooms >= " + min + " )");
  }
  if (maxIsNum) {
    filterStrings.push("FILTER (?numberOfRooms <= " + max + " )");
  }
  return wellFormedFilterReturn({ basicGraphPattern, filterStrings });
}

export function filterRentRange(min, max, currency) {
  const basicGraphPattern = [];
  const filterStrings = [];
  const minIsNum = is("Number", min);
  const maxIsNum = is("Number", max);
  if ((minIsNum || maxIsNum) && currency) {
    filterStrings.push("FILTER (?currency = '" + currency + "') ");
    basicGraphPattern.concat([
      "?is s:priceSpecification ?pricespec .",
      "?pricespec s:price ?price .",
      "?pricespec s:priceCurrency ?currency .",
    ]);
  }
  if (minIsNum) {
    filterStrings.push("FILTER (?price >= " + min + " )");
  }
  if (maxIsNum) {
    filterStrings.push("FILTER (?price <= " + max + " )");
  }

  return wellFormedFilterReturn({ basicGraphPattern, filterStrings });
}

/**
 * @param {*} prefixes an object which' keys are the prefixes
 *  and values the long-form URIs.
 * @returns {String} in the form of e.g.
 * ```
 * prefix s: <http://schema.org/>
 * prefix won: <http://purl.org/webofneeds/model#>
 * ```
 */
export function prefixesString(prefixes) {
  if (!prefixes) {
    return "";
  } else {
    const prefixesStrings = Object.entries(prefixes).map(
      ([prefix, uri]) => `prefix ${prefix}: <${uri}>\n`
    );
    return prefixesStrings.join("");
  }
}
//TODO should return a context-def as well
