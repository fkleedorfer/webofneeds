package won.cryptography.service;

import org.apache.http.ssl.TrustStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import won.cryptography.service.keystore.KeyStoreService;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Collections;

/**
 * User: ypanchenko Date: 08.10.2015
 */
public class RegistrationRestClientHttps implements RegistrationClient {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private String registrationQuery;
    private String keyAlias;
    private KeyStoreService keyStoreService;
    private TrustStoreService trustStoreService;
    private TrustStrategy trustStrategy;
    private Integer connectionTimeout;
    private Integer readTimeout;
    private RestTemplate restTemplate;
    private HttpEntity<String> entity;

    @PostConstruct
    public void initialize() {
        // the rest template and entity can be reused since context is always the same
        // (app certificate doesn't change) in
        // case of registration (intended for app registration)
        try {
            restTemplate = CryptographyUtils.createSslRestTemplate(this.keyAlias,
                            this.keyStoreService.getUnderlyingKeyStore(),
                            this.keyStoreService.getPassword(),
                            this.trustStoreService.getUnderlyingKeyStore(), this.trustStrategy, this.readTimeout,
                            this.connectionTimeout, false);
        } catch (Exception e) {
            String msg = "Could not create Rest Template for registration";
            logger.error(msg);
            throw new RuntimeException(msg, e);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.TEXT_PLAIN));
        entity = new HttpEntity<>("parameters", headers);
    }

    public RegistrationRestClientHttps(KeyStoreService keyStoreService, String keyAlias,
                    TrustStoreService trustStoreService, TrustStrategy trustStrategy, String registrationQuery) {
        this.keyStoreService = keyStoreService;
        this.keyAlias = keyAlias;
        this.trustStoreService = trustStoreService;
        this.trustStrategy = trustStrategy;
        this.registrationQuery = registrationQuery;
        this.readTimeout = 10000;
        this.connectionTimeout = 10000; // DEF. TIMEOUT IS 10sec
    }

    @Override
    public String register(final String remoteNodeUri) throws IOException {
        ResponseEntity<String> result = restTemplate.exchange(remoteNodeUri + registrationQuery, HttpMethod.POST,
                        entity, String.class);
        logger.info("Registration status: " + result.getStatusCode());
        if (!result.getStatusCode().is2xxSuccessful()) {
            throw new IOException("Registration by remote node " + remoteNodeUri + " failed: " + result.toString());
        }
        return result.getBody();
    }
}
