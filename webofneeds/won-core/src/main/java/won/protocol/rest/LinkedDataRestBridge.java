package won.protocol.rest;

import org.apache.http.ssl.TrustStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import won.cryptography.keymanagement.KeyPairAliasDerivationStrategy;
import won.cryptography.service.CryptographyUtils;
import won.cryptography.service.TrustStoreService;
import won.cryptography.service.keystore.KeyStoreService;
import won.cryptography.ssl.PrivateKeyAliasContextStrategy;

import javax.annotation.PostConstruct;
import java.lang.invoke.MethodHandles;

/**
 * User: ypanchenko Date: 02.02.2016
 */
public class LinkedDataRestBridge {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private RestTemplate restTemplateWithDefaultWebId;
    private Integer readTimeout;
    private Integer connectionTimeout;
    private KeyStoreService keyStoreService;
    private TrustStoreService trustStoreService;
    private TrustStrategy trustStrategy;
    private KeyPairAliasDerivationStrategy keyPairAliasDerivationStrategy;

    public LinkedDataRestBridge(KeyStoreService keyStoreService, TrustStoreService trustStoreService,
                    TrustStrategy trustStrategy, KeyPairAliasDerivationStrategy keyPairAliasDerivationStrategy) {
        this.readTimeout = 10000;
        this.connectionTimeout = 10000; // DEF. TIMEOUT IS 10sec
        this.keyStoreService = keyStoreService;
        this.trustStoreService = trustStoreService;
        this.trustStrategy = trustStrategy;
        this.keyPairAliasDerivationStrategy = keyPairAliasDerivationStrategy;
    }

    @PostConstruct
    public void initialize() {
        String defaultAlias = keyPairAliasDerivationStrategy.getAliasForAtomUri(null);
        if (defaultAlias != null) {
            // we are using a fixed alias strategy (or at least, there is a default alias
            // set)
            try {
                // passing null here will cause the default alias to be used
                this.restTemplateWithDefaultWebId = createRestTemplateForReadingLinkedData(null);
            } catch (Exception e) {
                throw new RuntimeException("could not create rest template for default alias " + defaultAlias);
            }
        } else {
            restTemplateWithDefaultWebId = new RestTemplate();
        }
    }

    public RestTemplate getRestTemplate() {
        return restTemplateWithDefaultWebId;
    }

    public RestTemplate getRestTemplate(String requesterWebID) {
        RestTemplate restTemplate;
        try {
            restTemplate = getRestTemplateForReadingLinkedData(requesterWebID);
        } catch (Exception e) {
            logger.error("Failed to create ssl tofu rest template", e);
            throw new RuntimeException(e);
        }
        return restTemplate;
    }

    private RestTemplate getRestTemplateForReadingLinkedData(String webID) throws Exception {
        if (webID == null) {
            PrivateKeyAliasContext.setPrivateKeyAlias(keyPairAliasDerivationStrategy.getAliasForAtomUri(null));
            return restTemplateWithDefaultWebId;
        }
        return createRestTemplateForReadingLinkedData(webID);
    }

    private RestTemplate createRestTemplateForReadingLinkedData(String webID) throws Exception {
        PrivateKeyAliasContext.setPrivateKeyAlias(keyPairAliasDerivationStrategy.getAliasForAtomUri(webID));
        RestTemplate template = CryptographyUtils.createSslRestTemplate(this.keyStoreService.getUnderlyingKeyStore(),
                        this.keyStoreService.getPassword(),
                        new PrivateKeyAliasContextStrategy(),
                        this.trustStoreService.getUnderlyingKeyStore(), this.trustStrategy, readTimeout,
                        connectionTimeout, true);
        // prevent the RestTemplate from throwing an exception when the server responds
        // with 4xx or 5xx status
        // because we want to hand the orginal response back to the original caller in
        // BridgeForLinkedDataController
        template.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            protected boolean hasError(final HttpStatus statusCode) {
                return false;
            }
        });
        return template;
    }
}
