package won.owner.web.rest;


import com.google.common.util.concurrent.ListenableFuture;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import org.apache.commons.collections.map.LRUMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import won.owner.linkeddata.NeedPojoNeedModelBuilder;
import won.owner.model.DraftState;
import won.owner.model.User;
import won.owner.pojo.ConnectionPojo;
import won.owner.pojo.DraftPojo;
import won.owner.pojo.NeedPojo;
import won.owner.repository.DraftStateRepository;
import won.owner.service.impl.DataReloadService;
import won.owner.service.impl.URIService;
import won.owner.service.impl.WONUserDetailService;
import won.protocol.exception.ConnectionAlreadyExistsException;
import won.protocol.exception.IllegalMessageForNeedStateException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.model.*;
import won.protocol.owner.OwnerProtocolNeedServiceClientSide;
import won.protocol.repository.ChatMessageRepository;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.MatchRepository;
import won.protocol.repository.NeedRepository;
import won.protocol.repository.rdfstorage.RDFStorageService;
import won.protocol.rest.LinkedDataRestClient;
import won.protocol.util.DataAccessUtils;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.util.linkeddata.LinkedDataSource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Controller
@RequestMapping("/rest/needs")
public class RestNeedController {

	final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
  @Qualifier("default")
	private OwnerProtocolNeedServiceClientSide ownerService;

	@Autowired
	private NeedRepository needRepository;


  @Autowired
  private DraftStateRepository draftStateRepository;

	@Autowired
	private MatchRepository matchRepository;

  @Autowired
  private RDFStorageService rdfStorage;

	@Autowired
	private ChatMessageRepository chatMessageRepository;

	@Autowired
	private ConnectionRepository connectionRepository;

	@Autowired
	private URIService uriService;

	@Autowired
	private WONUserDetailService wonUserDetailService;

	@Autowired
	private DataReloadService dataReloadService;

  @Autowired
  private LinkedDataSource linkedDataSource;

	//TODO: this is a quick fix and the only reason for us to use commons-collections. Rework to use ehcache!
	private LRUMap cachedNeeds = new LRUMap(200, 1000);


	public void setDataReloadService(DataReloadService dataReloadService) {
		this.dataReloadService = dataReloadService;
	}

	public URIService getUriService() {
		return uriService;
	}

	public void setUriService(final URIService uriService) {
		this.uriService = uriService;
	}

	public void setOwnerService(OwnerProtocolNeedServiceClientSide ownerService) {
		this.ownerService = ownerService;
	}

	public void setConnectionRepository(ConnectionRepository connectionRepository) {
		this.connectionRepository = connectionRepository;
	}

	public void setMatchRepository(MatchRepository matchRepository) {
		this.matchRepository = matchRepository;
	}

	public void setNeedRepository(NeedRepository needRepository) {
		this.needRepository = needRepository;
	}

  /**
   * find matches of a need and return a list of matches
   * @param needId id of the need for which the matches shall be retrieved
   * @return JSON object list of matching needs.
   */
	@GET
	@Path("/{needId}/matches")
	@Produces(MediaType.APPLICATION_JSON)
	@ResponseBody
	@RequestMapping(
			value = "/{needId}/matches",
			produces = MediaType.APPLICATION_JSON,
			method = RequestMethod.GET)
    public List<NeedPojo> findMatches(@PathVariable("needId") long needId) {

		logger.debug("Looking for matches for Need: " + needId);

		List<NeedPojo> returnList = new ArrayList<NeedPojo>();

		List<Need> needs = needRepository.findById(needId);
		if (needs.isEmpty()) {
			logger.warn("Need not found in db: " + needId);
			return returnList;
		}

		logger.debug("Found need in DB: ");
		Need need = needs.get(0);


		LinkedDataRestClient linkedDataRestClient = new LinkedDataRestClient();
		NeedPojo fullNeed = new NeedPojo(need.getNeedURI(), linkedDataRestClient.readResourceData(need.getNeedURI()));

		//NeedPojo fullNeed = NeedFetcher.getNeedInfo(need);

		logger.debug("Looking for matches for: " + need.getNeedURI());
		List<Match> matches = matchRepository.findByFromNeed(need.getNeedURI());

		logger.debug("Found Matches: " + matches.size());
		for (Match match : matches) {
			URI matchUri;
			logger.debug("using match: {} ", match);
			if (!match.getFromNeed().equals(need.getNeedURI()))
				matchUri = match.getFromNeed();
			else
				matchUri = match.getToNeed();
			logger.debug("using needUri: {} ", matchUri);
			NeedPojo matchedNeed = (NeedPojo) this.cachedNeeds.get(matchUri);
			if (matchedNeed == null) {
				List<Need> matchNeeds = needRepository.findByNeedURI(matchUri);
				logger.debug("found {} needs for uri {} in repo", matchNeeds.size(), matchUri);
				logger.debug("matchUri:{}, needURi:{}", matchUri, matchNeeds.get(0));
				logger.debug("fetching need {} from WON node", matchUri);
				matchedNeed = new NeedPojo(matchNeeds.get(0).getNeedURI(), linkedDataRestClient.readResourceData(matchNeeds.get(0).getNeedURI()));
				//matchedNeed = new NeedPojo(matchUri, linkedDataRestClient.readResourceData(matchUri));
				//NeedPojo matchedNeed = NeedFetcher.getNeedInfo(matchNeeds.get(0));
				this.cachedNeeds.put(matchUri, matchedNeed);
			}
			logger.debug("matched need's state: {}", matchedNeed.getState());
			if (matchedNeed != null && !NeedState.INACTIVE.equals(matchedNeed.getState())) {
				logger.debug("adding need {}", matchedNeed.getNeedURI());
				returnList.add(matchedNeed);
			}
		}

		return returnList;

	}

  /**
   * returns a List containing needs belonging to the user
   * @return JSON List of need objects
   */
  @ResponseBody
  @RequestMapping(
    value = "/",
    produces = MediaType.APPLICATION_JSON,
    method = RequestMethod.GET
  )
  public List<NeedPojo> getAllNeedsOfUser() {
    logger.info("Getting all needs of user: ");

    User user = (User) wonUserDetailService.loadUserByUsername(SecurityContextHolder.getContext().getAuthentication().getName());

    LinkedDataRestClient linkedDataRestClient = new LinkedDataRestClient();
    List<NeedPojo> returnList = new ArrayList<NeedPojo>();

    Iterable<Need> needs = user.getNeeds();
    for (Need need : needs) {
      NeedPojo needPojo = new NeedPojo(need.getNeedURI(), linkedDataRestClient.readResourceData(need.getNeedURI()));
      needPojo.setNeedId(need.getId());
      returnList.add(needPojo);
    }
    return returnList;
  }
  /**
   * this method creates need and returns created need with its needID
   * @param needPojo object containing information needed for need creation
   * @return JSON object of the created need.
   */
	@ResponseBody
	@RequestMapping(
			value = "/",
			consumes = MediaType.APPLICATION_JSON,
			produces = MediaType.APPLICATION_JSON,
			method = RequestMethod.POST
	)
  //TODO: move transactionality annotation into the service layer
  @Transactional(propagation = Propagation.SUPPORTS)
	public NeedPojo createNeed(@RequestBody NeedPojo needPojo) {
		User user = (User) wonUserDetailService.loadUserByUsername(SecurityContextHolder.getContext().getAuthentication().getName());

		logger.info("New Need:" + needPojo.getTextDescription() + "/" + needPojo.getCreationDate() + "/" +
				needPojo.getLongitude() + "/" + needPojo.getLatitude() + "/" + (needPojo.getState() == NeedState.ACTIVE));
    //TODO: using fixed Facets - change this
    needPojo.setFacetTypes(new String[]{FacetType.OwnerFacet.getURI().toString()});
    NeedPojo createdNeedPojo = resolve(needPojo);
		List<Need> needs = needRepository.findByNeedURI(URI.create(createdNeedPojo.getNeedURI()));
		user.getNeeds().add(needs.get(0));
		wonUserDetailService.save(user);

		return createdNeedPojo;
	}

  @ResponseBody
  @RequestMapping(
    value = "/drafts",
    produces = MediaType.APPLICATION_JSON,
    method = RequestMethod.GET
  )
  //TODO: move transactionality annotation into the service layer
  @Transactional(propagation = Propagation.SUPPORTS)
  public List<DraftPojo> getAllDrafts() {
    User user = (User) wonUserDetailService.loadUserByUsername(SecurityContextHolder.getContext().getAuthentication().getName());
    List<DraftState> draftStates = draftStateRepository.findByUserName(user.getUsername());
    Iterator<DraftState> draftIterator =  draftStates.iterator();
    List<URI> draftURIs = new ArrayList<>();
    while(draftIterator.hasNext()){
      draftURIs.add(draftIterator.next().getDraftURI());
    }

    LinkedDataRestClient linkedDataRestClient = new LinkedDataRestClient();
    List<DraftPojo> returnList = new ArrayList<DraftPojo>();

    Iterable<Need> needs = user.getNeeds();
    for (Need need : needs) {

      if (draftURIs.contains(need.getNeedURI())){
        DraftPojo draftPojo = new DraftPojo(need.getNeedURI(), rdfStorage.loadContent(need.getNeedURI()),
                                            draftStateRepository.findByDraftURI(need.getNeedURI()).get(0));
        draftPojo.setNeedId(need.getId());
        returnList.add(draftPojo);
      }
    }

    return returnList;
  }
  /**
   * saves draft of a draft
   * @param draftPojo an object containing information of the need draft
   * @return a JSON object of the draft with its temprory id.
   */
  @ResponseBody
  @RequestMapping(
    value = "/drafts",
    consumes = MediaType.APPLICATION_JSON,
    produces = MediaType.APPLICATION_JSON,
    method = RequestMethod.POST
  )
  //TODO: move transactionality annotation into the service layer
  @Transactional(propagation = Propagation.SUPPORTS)
  public DraftPojo createDraft(@RequestBody DraftPojo draftPojo) {

    User user = (User) wonUserDetailService.loadUserByUsername(SecurityContextHolder.getContext().getAuthentication().getName());

    user.getNeeds().size();

    DraftPojo createdDraftPojo = resolveDraft(draftPojo,user);
    NeedPojo createdNeedPojo = (NeedPojo) createdDraftPojo;

    List<Need> drafts = needRepository.findByNeedURI(URI.create(createdNeedPojo.getNeedURI()));

    user.getNeeds().add(drafts.get(0));
    wonUserDetailService.save(user);

    int currentStep = draftPojo.getCurrentStep();
    String userName = createdDraftPojo.getUserName();
    DraftState draftState = new DraftState(URI.create(draftPojo.getNeedURI()),currentStep, userName);
    draftStateRepository.save(draftState);
    return createdDraftPojo;

  }

  @ResponseBody
  @RequestMapping(
    value = "/drafts",
    method = RequestMethod.DELETE
  )
  //TODO: move transactionality annotation into the service layer
  @Transactional(propagation = Propagation.SUPPORTS)
  public ResponseEntity deleteDrafts() {
    try{
      User user = (User) wonUserDetailService.loadUserByUsername(SecurityContextHolder.getContext().getAuthentication().getName());
      List<DraftState> draftStates = draftStateRepository.findByUserName(user.getUsername());
      Iterator<DraftState> draftIterator =  draftStates.iterator();
      List<URI> draftURIs = new ArrayList<>();
      while(draftIterator.hasNext()){
        draftURIs.add(draftIterator.next().getDraftURI());
      }
      List<Need> needs = user.getNeeds();
      List<Need> toDelete = new ArrayList<>();
      for (Need need : needs){
        if (draftURIs.contains(need.getNeedURI())){
          toDelete.add(need);
        }
      }
      user.removeNeeds(toDelete);
      wonUserDetailService.save(user);
      needRepository.delete(toDelete);
      draftStateRepository.delete(draftStates);
    }catch (Exception e){
      return new ResponseEntity(HttpStatus.CONFLICT);
    }
    return new ResponseEntity(HttpStatus.OK);
  }

  @ResponseBody
  @RequestMapping(
    value ="/drafts/{draftId}",
    produces = MediaType.APPLICATION_JSON,
    method = RequestMethod.GET
  )
  public DraftPojo getDraft(@PathVariable("draftId") long draftId){
    logger.debug("getting draft: "+draftId);

    List<Need> draftList = needRepository.findById(draftId);
    Need need = draftList.get(0);

    DraftPojo draftPojo = new DraftPojo(need.getNeedURI(),rdfStorage.loadContent(need.getNeedURI()),
                                        draftStateRepository.findByDraftURI(need.getNeedURI()).get(0));
    draftPojo.setNeedURI(need.getNeedURI().toString());
    return draftPojo;
  }

  @ResponseBody
  @RequestMapping(
    value ="/drafts/{draftId}",
    method = RequestMethod.DELETE
  )
  public ResponseEntity deleteDraft(@PathVariable long draftId){
    logger.debug("deleting draft: "+draftId);

    List<Need> draftList = needRepository.findById(draftId);
    if (draftList.size()==0){
      return new ResponseEntity(HttpStatus.CONFLICT);
    }
    Need need = draftList.get(0);
    User user = (User)wonUserDetailService.loadUserByUsername(SecurityContextHolder.getContext().getAuthentication()
                                                                             .getName());
    try{
      user.removeNeeds(draftList);
      wonUserDetailService.save(user);
      List<DraftState> draftStates = draftStateRepository.findByDraftURI(need.getNeedURI());
      needRepository.delete(draftId);
      draftStateRepository.delete(draftStates);
      rdfStorage.removeContent(need.getNeedURI());
    }catch (Exception e){
      return new ResponseEntity(HttpStatus.CONFLICT);
    }
    return new ResponseEntity(HttpStatus.OK);


  }

  /**
   *
   * @param needId id of the need for which information shall be retrieved
   * @return a JSON need object
   */
	@ResponseBody
	@RequestMapping(
			value = "/{needId}",
			produces = MediaType.APPLICATION_JSON,
			method = RequestMethod.GET
	)
	public NeedPojo getNeed(@PathVariable("needId") long needId) {
		logger.info("Getting need: " + needId);

		LinkedDataRestClient linkedDataRestClient = new LinkedDataRestClient();
		List<NeedPojo> returnList = new ArrayList<NeedPojo>();

		Iterable<Need> needs = needRepository.findById(needId);
		Need need = needs.iterator().next();

		NeedPojo needPojo = new NeedPojo(need.getNeedURI(), linkedDataRestClient.readResourceData(need.getNeedURI()));
		needPojo.setNeedId(need.getId());

		return needPojo;
	}
  @ResponseBody
  @RequestMapping(
    value = "/{needId}",
    consumes = MediaType.APPLICATION_JSON,
    produces = MediaType.APPLICATION_JSON,
    method = RequestMethod.PUT
  )
  public NeedPojo updateNeed(@PathVariable("needId") long needId, @RequestBody NeedPojo needPojo) {
    //TODO: won node currently doesn't support updates.

    return needPojo;
  }


	// Matching and connecting

  /**
   * connects two needs of a match with matchId
   * @param matchId the id of the match, for which the needs shall be connected
   * @return a string. "noNeedFound" or ""
   */
  /*
	@RequestMapping(
			value = "/match/{matchId}/connections",
			method = RequestMethod.POST
	)
  //TODO: move transactionality annotation into the service layer
  @Transactional(propagation = Propagation.SUPPORTS)
	public String connect(@PathVariable String matchId) {
		String ret = "noNeedFound";

		try {
			List<Match> matches = matchRepository.findById(Long.valueOf(matchId));
			if (!matches.isEmpty()) {
				Match match = matches.get(0);
				List<Need> needs = needRepository.findByNeedURI(match.getFromNeed());
				if (!needs.isEmpty())
					ret = "";
        //TODO: this connects only ownerFacets!!!
				ownerService.connect(match.getFromNeed(), match.getToNeed(),
          WonRdfUtils.FacetUtils.createFacetModelForHintOrConnect(FacetType.OwnerFacet.getURI(),
            FacetType.OwnerFacet.getURI()));
			}
		} catch (ConnectionAlreadyExistsException e) {
			logger.warn("caught ConnectionAlreadyExistsException:", e);
		} catch (IllegalMessageForNeedStateException e) {
			logger.warn("caught IllegalMessageForNeedStateException:", e);
		} catch (NoSuchNeedException e) {
			logger.warn("caught NoSuchNeedException:", e);
		} catch (Exception e) {
      logger.warn("caught Exception", e);
    }

		return ret;
	}
   */
  /**
   * returns List of matches of a need with the needId
   * @param needId id of the need, for which list of matches shall be retrieved
   * @return a JSON List of matches
   */
  /*
	@ResponseBody
	@RequestMapping(
			value = "/{needId}/matches",
			method = RequestMethod.GET,
			produces = MediaType.APPLICATION_JSON
	)
	public List<Match> listMatches(@PathVariable String needId) {
		List<Need> needs = needRepository.findById(Long.valueOf(needId));
		if (needs.isEmpty())
			return new ArrayList<>();

		Need need = needs.get(0);

		return matchRepository.findByFromNeed(need.getNeedURI());
	}
       */
  /**
   * returns List of connections of a need with the needId
   * @param needId id of the need, for which list of connections shall be retrieved
   * @return a List of connections
   */
	@ResponseBody
	@RequestMapping(
			value = "/{needId}/connections",
			method = RequestMethod.GET,
			produces = MediaType.APPLICATION_JSON
	)
	public List<Connection> listConnections(@PathVariable String needId) {

		List<Need> needs = needRepository.findById(Long.valueOf(needId));
		if (needs.isEmpty())
			return new ArrayList<>();

		Need need = needs.get(0);

		return connectionRepository.findByNeedURI(need.getNeedURI());
	}

  /**
   *
   *
   *
   * @param needId
   * @param connectionPojo
   * @return
   */
  @ResponseBody
  @RequestMapping(
    value = "/{needId}/connections",
    method = RequestMethod.POST,
    consumes = MediaType.APPLICATION_JSON,
    produces = MediaType.APPLICATION_JSON
  )
  public ConnectionPojo connect(@PathVariable String needId, @RequestBody ConnectionPojo connectionPojo) {

    ConnectionPojo fullConnection = null;
    try {
      ListenableFuture<URI> futureResult = ownerService.connect(URI.create(connectionPojo.getNeedURI()),
                                     URI.create(connectionPojo.getRemoteNeedURI()),
                           WonRdfUtils.FacetUtils.createFacetModelForHintOrConnect(FacetType.OwnerFacet.getURI(),
                                                                                   FacetType.OwnerFacet.getURI()));
      URI connectionURI = futureResult.get();
      Connection connection = DataAccessUtils.loadConnection(connectionRepository,connectionURI);
      if (connection != null){

        fullConnection = new ConnectionPojo(connectionURI, linkedDataSource.getModelForResource
          (connection.getConnectionURI()));
        fullConnection.setConnectionId(connection.getId());
        logger.debug("Added connection id:" + fullConnection.getConnectionId() + "uri: " + connectionURI);
      }
    } catch (ConnectionAlreadyExistsException e) {
      logger.warn("caught ConnectionAlreadyExistsException:", e);
    } catch (IllegalMessageForNeedStateException e) {
      logger.warn("caught IllegalMessageForNeedStateException:", e);
    } catch (NoSuchNeedException e) {
      logger.warn("caught NoSuchNeedException:", e);
    } catch (Exception e) {
      logger.warn("caught Exception", e);
    }
    return fullConnection;
  }

  private DraftPojo resolveDraft(DraftPojo draftPojo, User user){
    URI needURI;
    //Draft needDraft2 = new Draft();

    Need needDraft = new Need();

    URI ownerURI = this.uriService.getOwnerProtocolOwnerServiceEndpointURI();
    int draftNumber = user.getNeeds().size()+1;
    URI draftId = URI.create(ownerURI.toString()+"/"+user.getUsername()+"/"+ draftNumber);

    NeedPojoNeedModelBuilder needPojoNeedModelBuilder = new NeedPojoNeedModelBuilder(draftPojo);
    needPojoNeedModelBuilder.setUri(draftId);
    Model needModel = needPojoNeedModelBuilder.build();

    needDraft.setNeedURI(draftId);

    needDraft.setState(NeedState.INACTIVE);
    needDraft.setOwnerURI(ownerURI);
    try{
      needRepository.save(needDraft);
      rdfStorage.storeContent(needDraft.getNeedURI(),needModel);
    } catch(Exception e){
      e.printStackTrace();
      int size = needRepository.findByNeedURI(needDraft.getNeedURI()).size();
      logger.debug(String.valueOf(size));
    }

    draftPojo.setNeedURI(draftId.toString());

    return draftPojo;
  }

  private NeedPojo resolve(NeedPojo needPojo) {
    if (needPojo.getNeedId() >= 0) {
      List<Need> needs = needRepository.findById(needPojo.getNeedId());
      if (!needs.isEmpty()) {
        logger.warn("Deactivating old need");
        try {
          ownerService.deactivate(needs.get(0).getNeedURI());
        } catch (Exception e) {
          logger.warn("Could not deactivate old Need: " + needs.get(0).getNeedURI());
        }
      }
    }
    URI needURI;

    try {
      URI ownerURI = this.uriService.getOwnerProtocolOwnerServiceEndpointURI();

      NeedPojoNeedModelBuilder needPojoNeedModelBuilder = new NeedPojoNeedModelBuilder(needPojo);
      needPojoNeedModelBuilder.setUri("no:uri");
      Model needModel = needPojoNeedModelBuilder.build();
      needModel.setNsPrefix("","no:uri");

      if (needPojo.getWonNode().equals("")) {
        ListenableFuture<URI> futureResult = ownerService.createNeed(ownerURI, needModel, needPojo.getState() == NeedState.ACTIVE);
        needURI = futureResult.get();
      } else {
        ListenableFuture<URI> futureResult = ownerService.createNeed(ownerURI, needModel, needPojo.getState() == NeedState.ACTIVE, URI.create(needPojo.getWonNode()));
        needURI = futureResult.get();
      }

      Need need = DataAccessUtils.loadNeed(needRepository, needURI);
      if (need != null) {
        NeedPojo fullNeed = new NeedPojo(needURI, linkedDataSource.getModelForResource(need.getNeedURI()));
        fullNeed.setNeedId(need.getId());
        logger.info("Added need id:" + fullNeed.getNeedId() + "uri: " + needURI);
        return fullNeed;
      }
    } catch (Exception e) {
      logger.warn("Caught exception", e);
    }
    return new NeedPojo();
  }

  private void attachRdfToModelViaBlanknode(final String rdfAsString, final String rdfLanguage, final Resource resourceToLinkTo,
                                            final Property propertyToLinkThrough, final com.hp.hpl.jena.rdf.model.Model modelToModify) {
    com.hp.hpl.jena.rdf.model.Model model = RdfUtils.readRdfSnippet(rdfAsString, rdfLanguage);
    RdfUtils.attachModelByBaseResource(resourceToLinkTo, propertyToLinkThrough, model);
  }

}
