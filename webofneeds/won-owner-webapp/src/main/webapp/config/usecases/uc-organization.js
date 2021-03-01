/**
 * Created by fsuda on 18.09.2018.
 */
import {
  details,
  mergeInEmptyDraft,
  defaultReactions,
  onlyMembersSeeMembersAuthorization,
  defaultPublicAtomAuthorization,
  connectedConnectionsAuthorization,
  connectToSocketsAuthorization,
} from "../detail-definitions.js";
import vocab from "../../app/service/vocab.js";
import ico36_uc_wtf from "../../images/won-icons/ico36_uc_wtf.svg";

export const organization = {
  identifier: "organization",
  label: "Organization",
  icon: ico36_uc_wtf, //TODO: Find better Icon
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: ["s:Organization"],
        sockets: {
          // TODO: Currently not in use in favour of more generic member -> Role -> member relation
          // "#worksForInverseSocket":
          //   vocab.WXSCHEMA.WorksForInverseSocketCompacted,
          "#memberSocket": vocab.WXSCHEMA.MemberSocketCompacted,
          "#associatedArticleSocket":
            vocab.WXSCHEMA.AssociatedArticleSocketCompacted,
          "#parentOrgSocket": vocab.WXSCHEMA.ParentOrganizationSocketCompacted,
          "#subOrgSocket": vocab.WXSCHEMA.SubOrganizationSocketCompacted,
          "#sReviewSocket": vocab.WXSCHEMA.ReviewSocketCompacted,
          "#sEventSocket": vocab.WXSCHEMA.EventSocketCompacted,
        },
        acl: [
          defaultPublicAtomAuthorization,
          connectedConnectionsAuthorization,
          connectToSocketsAuthorization,
          onlyMembersSeeMembersAuthorization,
        ],
      },
    }),
  },
  reactions: {
    ...defaultReactions,
    // TODO: Currently not in use in favour of more generic member -> Role -> member relation
    // [vocab.WXSCHEMA.WorksForInverseSocketCompacted]: {
    //   [vocab.WXSCHEMA.WorksForSocketCompacted]: {
    //     useCaseIdentifiers: ["persona"],
    //   },
    // },
    [vocab.WXSCHEMA.MemberSocketCompacted]: {
      [vocab.WXSCHEMA.MemberOfSocketCompacted]: {
        useCaseIdentifiers: ["persona"],
      },
      [vocab.WXSCHEMA.OrganizationRoleOfSocketCompacted]: {
        useCaseIdentifiers: ["role"],
        refuseNonOwned: true,
      },
    },
    [vocab.WXSCHEMA.AssociatedArticleSocketCompacted]: {
      [vocab.WXSCHEMA.AssociatedArticleInverseSocketCompacted]: {
        useCaseIdentifiers: ["newsarticle"],
      },
    },
    [vocab.WXSCHEMA.ParentOrganizationSocketCompacted]: {
      [vocab.WXSCHEMA.SubOrganizationSocketCompacted]: {
        useCaseIdentifiers: ["organization"],
      },
    },
    [vocab.WXSCHEMA.SubOrganizationSocketCompacted]: {
      [vocab.WXSCHEMA.ParentOrganizationSocketCompacted]: {
        useCaseIdentifiers: ["organization"],
      },
    },
  },
  details: {
    title: { ...details.title },
    description: { ...details.description },
    location: { ...details.location },
    tags: { ...details.tags },
    images: { ...details.images },
    website: { ...details.website },
  },
  seeksDetails: undefined,
};