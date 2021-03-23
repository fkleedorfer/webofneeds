/*
 * Copyright 2012 Research Studios Austria Forschungsges.m.b.H. Licensed under
 * the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package won.protocol.rest;

import org.apache.http.ssl.TrustStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import won.cryptography.keymanagement.KeyPairAliasDerivationStrategy;
import won.cryptography.service.CryptographyUtils;
import won.cryptography.service.TrustStoreService;
import won.cryptography.service.keystore.KeyStoreService;

import javax.annotation.PostConstruct;
import java.lang.invoke.MethodHandles;
import java.net.URI;

/**
 * User: ypanchenko Date: 07.10.15
 */
public class LinkedDataRestClientHttps extends LinkedDataRestClient {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    protected String acceptHeaderValue = null;
    private HttpMessageConverter datasetConverter;
    private Integer readTimeout;
    private Integer connectionTimeout;
    private KeyStoreService keyStoreService;
    private TrustStoreService trustStoreService;
    private TrustStrategy trustStrategy;
    private KeyPairAliasDerivationStrategy keyPairAliasDerivationStrategy;
    private RestTemplate restTemplateWithoutWebId;

    public LinkedDataRestClientHttps(KeyStoreService keyStoreService, TrustStoreService trustStoreService,
                    TrustStrategy trustStrategy, KeyPairAliasDerivationStrategy keyPairAliasDerivationStrategy) {
        this.readTimeout = 5000;
        this.connectionTimeout = 5000; // DEF. TIMEOUT IS 5 sec
        this.keyStoreService = keyStoreService;
        this.trustStoreService = trustStoreService;
        this.trustStrategy = trustStrategy;
        this.keyPairAliasDerivationStrategy = keyPairAliasDerivationStrategy;
    }

    @PostConstruct
    public void initialize() throws Exception {
        datasetConverter = new RdfDatasetConverter();
        // HttpHeaders headers = new HttpHeaders();
        this.acceptHeaderValue = MediaType.toString(datasetConverter.getSupportedMediaTypes());
        this.restTemplateWithoutWebId = CryptographyUtils
                        .createSslRestTemplate(this.trustStrategy,
                                        readTimeout,
                                        connectionTimeout);
        restTemplateWithoutWebId.getMessageConverters().add(0, datasetConverter);
    }

    protected RestTemplate createRestTemplateForReadingLinkedData(String webID) {
        RestTemplate template;
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("creating rest template for webID {} ", webID == null ? "[none provided]" : webID);
            }
            String actualPrivateKeyAlias = keyPairAliasDerivationStrategy.getAliasForAtomUri(webID);
            if (actualPrivateKeyAlias == null) {
                return restTemplateWithoutWebId;
            }
            template = CryptographyUtils.createSslRestTemplate(actualPrivateKeyAlias,
                            this.keyStoreService.getUnderlyingKeyStore(),
                            this.keyStoreService.getPassword(),
                            this.trustStoreService.getUnderlyingKeyStore(), this.trustStrategy, readTimeout,
                            connectionTimeout, true);
            if (logger.isDebugEnabled()) {
                logger.debug("rest template for webID {} created", webID == null ? "[none provided]" : webID);
            }
            // we add our DatasetConverter before any other converter because the jackson
            // converter feels responsible for "application/*+json" (which matches
            // "application/ld+json") and is confident it can produce a jena Dataset - but
            // then of course fails to instantiate one. By putting our converter first, we
            // can be sure it is used when a jena Dataset is requested.
            template.getMessageConverters().add(0, datasetConverter);
            return template;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create rest template for webID '" + webID + "'", e);
        }
    }

    @Override
    public DatasetResponseWithStatusCodeAndHeaders readResourceDataWithHeaders(final URI resourceURI) {
        return readResourceDataWithHeaders(resourceURI, (URI) null);
    }

    @Override
    public DatasetResponseWithStatusCodeAndHeaders readResourceDataWithHeaders(URI resourceURI,
                    final URI requesterWebID) {
        HttpMessageConverter datasetConverter = new RdfDatasetConverter();
        RestTemplate restTemplate;
        try {
            restTemplate = getRestTemplateForReadingLinkedData(
                            requesterWebID == null ? null : requesterWebID.toString());
        } catch (Exception e) {
            logger.error("Failed to create ssl tofu rest template", e);
            throw new RuntimeException(e);
        }
        restTemplate.getMessageConverters().add(datasetConverter);
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add(HttpHeaders.ACCEPT, this.acceptHeaderValue);
        return super.readResourceData(resourceURI, restTemplate, requestHeaders);
    }

    @Override
    public DatasetResponseWithStatusCodeAndHeaders readResourceDataWithHeaders(final URI resourceURI,
                    final URI requesterWebID, final HttpHeaders requestHeaders) {
        requestHeaders.add(HttpHeaders.ACCEPT, this.acceptHeaderValue);
        return super.readResourceData(resourceURI,
                        getRestTemplateForReadingLinkedData(requesterWebID == null ? null : requesterWebID.toString()),
                        requestHeaders);
    }

    protected RestTemplate getRestTemplateForReadingLinkedData(String webID) {
        return createRestTemplateForReadingLinkedData(webID);
    }

    @Override
    public DatasetResponseWithStatusCodeAndHeaders readResourceDataWithHeaders(final URI resourceURI,
                    HttpHeaders requestHeaders) {
        return readResourceDataWithHeaders(resourceURI, null, requestHeaders);
    }

    public void setReadTimeout(final Integer readTimeout) {
        this.readTimeout = readTimeout;
    }

    public void setConnectionTimeout(final Integer connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }
}
