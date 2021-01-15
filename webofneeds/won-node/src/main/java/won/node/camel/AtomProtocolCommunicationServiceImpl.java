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
package won.node.camel;

import org.apache.camel.component.activemq.ActiveMQComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.cryptography.service.RegistrationClient;
import won.cryptography.service.RegistrationRestClientHttps;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.jms.*;

import java.lang.invoke.MethodHandles;
import java.net.URI;

/**
 * User: syim Date: 27.01.14
 */
public class AtomProtocolCommunicationServiceImpl implements AtomProtocolCommunicationService {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Autowired
    private AtomProtocolCamelConfigurator atomProtocolCamelConfigurator;
    @Autowired
    private ActiveMQService activeMQService;
    private RegistrationClient registrationClient;

    public void setRegistrationClient(final RegistrationRestClientHttps registrationClient) {
        this.registrationClient = registrationClient;
    }

    public synchronized CamelConfiguration configureCamelEndpoint(URI wonNodeUri) throws Exception {
        String atomProtocolQueueName;
        CamelConfiguration camelConfiguration = new CamelConfiguration();
        logger.debug("ensuring camel is configured for remote wonNodeUri {}", wonNodeUri);
        URI remoteNodeBrokerUri = activeMQService.getBrokerEndpoint(wonNodeUri);
        if (atomProtocolCamelConfigurator.getBrokerComponentNameWithBrokerUri(remoteNodeBrokerUri) != null) {
            logger.debug("broker component name is already known");
            camelConfiguration.setEndpoint(atomProtocolCamelConfigurator.getEndpoint(wonNodeUri));
            camelConfiguration.setBrokerComponentName(
                            atomProtocolCamelConfigurator.getBrokerComponentNameWithBrokerUri(remoteNodeBrokerUri));
            // HINT: we may have to handle routes that were shut down automatically after a
            // timeout here...
        } else {
            logger.debug("broker component name unknown - setting up a new component for the remote broker");
            URI resourceUri;
            URI brokerUri;
            resourceUri = wonNodeUri;
            brokerUri = remoteNodeBrokerUri;
            atomProtocolQueueName = activeMQService.getProtocolQueueNameWithResource(resourceUri);
            registrationClient.register(wonNodeUri.toString());
            String endpoint = atomProtocolCamelConfigurator.configureCamelEndpointForAtomUri(resourceUri, brokerUri,
                            atomProtocolQueueName);
            camelConfiguration.setEndpoint(endpoint);
            camelConfiguration.setBrokerComponentName(
                            atomProtocolCamelConfigurator.getBrokerComponentNameWithBrokerUri(brokerUri));
            ActiveMQComponent activeMQComponent = (ActiveMQComponent) atomProtocolCamelConfigurator.getCamelContext()
                            .getComponent(atomProtocolCamelConfigurator.getBrokerComponentNameWithBrokerUri(brokerUri));
            activeMQComponent.setTransacted(false);
            activeMQComponent.setUsePooledConnection(true);
            logger.info("ActiveMQ Service Status : {}", activeMQComponent.getStatus().toString());
            activeMQComponent.start();
        }
        return camelConfiguration;
    }

    @Override
    public URI getBrokerUri(URI resourceUri) throws NoSuchConnectionException {
        return activeMQService.getBrokerEndpoint(resourceUri);
    }

    @Override
    public ActiveMQService getActiveMQService() {
        return activeMQService;
    }

    @Override
    public void setActiveMQService(ActiveMQService activeMQService) {
        this.activeMQService = activeMQService;
    }

    @Override
    public CamelConfigurator getProtocolCamelConfigurator() {
        return atomProtocolCamelConfigurator;
    }
}
