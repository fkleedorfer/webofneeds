package won.protocol.service.impl;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.apache.jena.query.Dataset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import won.cryptography.service.RandomNumberService;
import won.protocol.exception.IllegalAtomURIException;
import won.protocol.message.WonMessageUtils;
import won.protocol.service.WonNodeInfo;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.WonMessageUriHelper;
import won.protocol.util.WonRdfUtils;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

/**
 * User: fsalcher Date: 17.09.2014
 */
public class WonNodeInformationServiceImpl implements WonNodeInformationService {
    private static final int RANDOM_ID_STRING_LENGTH = 20;
    private final Ehcache wonNodeInfoCache;
    @Autowired
    private RandomNumberService randomNumberService;
    @Autowired
    private LinkedDataSource linkedDataSource;
    @Value(value = "${uri.node.default}")
    private URI defaultWonNodeUri;

    public WonNodeInformationServiceImpl() {
        CacheManager manager = CacheManager.getInstance();
        this.wonNodeInfoCache = new Cache("wonNodeInformationServiceImpl", 100, false, false, 3600, 3600);
        manager.addCache(wonNodeInfoCache);
    }

    private WonNodeInfo getFromCache(URI wonNodeURI) {
        Element e = wonNodeInfoCache.get(wonNodeURI);
        if (e == null) {
            return null;
        }
        return (WonNodeInfo) e.getObjectValue();
    }

    @Override
    public WonNodeInfo getWonNodeInformation(URI wonNodeURI) {
        Objects.requireNonNull(wonNodeURI);
        WonNodeInfo info = getFromCache(wonNodeURI);
        if (info != null) {
            return info;
        }
        Dataset nodeDataset = linkedDataSource.getDataForPublicResource(wonNodeURI);
        info = WonRdfUtils.WonNodeUtils.getWonNodeInfo(wonNodeURI, nodeDataset);
        if (info == null) {
            throw new IllegalStateException("Could not obtain WonNodeInformation for URI " + wonNodeURI);
        }
        wonNodeInfoCache.put(new Element(wonNodeURI, info));
        return info;
    }

    @Override
    public URI generateAtomURI() {
        return generateAtomURI(getDefaultWonNodeURI());
    }

    @Override
    public boolean isValidEventURI(URI eventURI) {
        return WonMessageUriHelper.isGenericMessageURI(eventURI);
    }

    @Override
    public boolean isValidEventURI(URI eventURI, URI wonNodeURI) {
        return isValidEventURI(eventURI);
    }

    @Override
    public URI generateConnectionURI(URI atomURI) {
        if (!isValidAtomURI(atomURI)) {
            throw new IllegalAtomURIException(
                            "Atom URI " + atomURI + " does not conform to this WoN node's URI patterns");
        }
        return URI.create(WonMessageUtils.stripFragment(atomURI).toString() + "/c/" + generateRandomID());
    }

    @Override
    public boolean isValidConnectionURI(URI connectionURI) {
        return isValidConnectionURI(connectionURI, getDefaultWonNodeURI());
    }

    @Override
    public boolean isValidConnectionURI(URI connectionURI, URI wonNodeURI) {
        WonNodeInfo wonNodeInformation = getWonNodeInformation(wonNodeURI);
        return isValidURI(connectionURI, wonNodeInformation.getConnectionURIPrefix());
    }

    @Override
    public URI generateAtomURI(URI wonNodeURI) {
        WonNodeInfo wonNodeInformation = getWonNodeInformation(wonNodeURI);
        return URI.create(wonNodeInformation.getAtomURIPrefix() + "/" + generateRandomID());
    }

    @Override
    public boolean isValidAtomURI(URI atomURI) {
        return isValidAtomURI(atomURI, getDefaultWonNodeURI());
    }

    @Override
    public boolean isValidAtomURI(URI atomURI, URI wonNodeURI) {
        WonNodeInfo wonNodeInformation = getWonNodeInformation(wonNodeURI);
        return isValidURI(atomURI, wonNodeInformation.getAtomURIPrefix());
    }

    private boolean isValidURI(URI uri, String prefix) {
        return uri != null && uri.toString().startsWith(prefix);
    }

    @Override
    public URI getWonNodeUri(final URI resourceURI) {
        URI wonNodeURI = WonLinkedDataUtils.getWonNodeURIForAtomOrConnectionURI(resourceURI, linkedDataSource);
        if (wonNodeURI == null) {
            throw new IllegalStateException("Could not obtain WoN node URI for resource " + resourceURI);
        }
        return wonNodeURI;
    }

    @Override
    public URI getDefaultWonNodeURI() {
        return defaultWonNodeUri;
    }

    public WonNodeInfo getDefaultWonNodeInfo() {
        return getWonNodeInformation(defaultWonNodeUri);
    }

    public void setDefaultWonNodeUri(final URI defaultWonNodeUri) {
        this.defaultWonNodeUri = defaultWonNodeUri;
    }

    public void setLinkedDataSource(final LinkedDataSource linkedDataSource) {
        this.linkedDataSource = linkedDataSource;
    }

    public void setRandomNumberService(RandomNumberService randomNumberService) {
        this.randomNumberService = randomNumberService;
    }

    /**
     * Returns a random string that does not start with a number. We do this so that
     * we generate URIs for which prefixing will always work with N3.js
     * https://github.com/RubenVerborgh/N3.js/issues/121
     * 
     * @return
     */
    private String generateRandomID() {
        return randomNumberService.generateRandomString(RANDOM_ID_STRING_LENGTH);
    }

    @Override
    public Optional<WonNodeInfo> getWonNodeInformationForURI(URI someURI, Optional<URI> requesterWebID) {
        return WonLinkedDataUtils.findWonNode(someURI, requesterWebID, linkedDataSource);
    }
}
