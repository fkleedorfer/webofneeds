package won.protocol.message.processor.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.cryptography.keymanagement.KeyPairAliasDerivationStrategy;
import won.cryptography.service.CryptographyService;
import won.protocol.exception.WonMessageProcessingException;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.WonMessageProcessor;
import won.protocol.util.Prefixer;
import won.protocol.util.RdfUtils;
import won.protocol.vocabulary.WONMSG;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * User: ypanchenko Date: 03.04.2015
 */
public class SignatureAddingWonMessageProcessor implements WonMessageProcessor {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Autowired
    private CryptographyService cryptographyService;
    @Autowired(required = false) // in production, there is a factory setting this value, for testing, we use
    // Autowired
    private KeyPairAliasDerivationStrategy keyPairAliasDerivationStrategy;

    public SignatureAddingWonMessageProcessor() {
    }

    @Override
    public WonMessage process(WonMessage message) throws WonMessageProcessingException {
        return signWithDefaultKey(message);
    }

    /**
     * Used by the WoN node. Uses its default key for signing any outgoing message.
     * 
     * @param message
     * @return
     * @throws WonMessageProcessingException
     */
    public WonMessage signWithDefaultKey(final WonMessage message) throws WonMessageProcessingException {
        // use default key for signing
        PrivateKey privateKey = cryptographyService.getDefaultPrivateKey();
        String webId = cryptographyService.getDefaultPrivateKeyAlias();
        PublicKey publicKey = cryptographyService.getPublicKey(webId);
        try {
            List<WonMessage> ret = new ArrayList<WonMessage>();
            for (WonMessage part : message.getAllMessages()) {
                ret.add(processWithKey(part, webId, privateKey, publicKey));
            }
            return WonMessage.of(ret);
        } catch (Exception e) {
            logger.error("Failed to sign", e);
            throw new WonMessageProcessingException("Failed to sign message " + message.getMessageURI().toString());
        }
    }

    /**
     * Used by owners - they find the key alias in the message and get the key from
     * the cryptography service.
     * 
     * @param wonMessage
     * @return
     * @throws WonMessageProcessingException
     */
    public WonMessage signWithAtomKey(final WonMessage wonMessage) throws WonMessageProcessingException {
        List<WonMessage> ret = new ArrayList<WonMessage>();
        for (WonMessage message : wonMessage.getAllMessages()) {
            // use senderAtom key for signing
            Optional<URI> senderAtomURI = Optional.of(message.getSenderAtomURIRequired());
            String alias = keyPairAliasDerivationStrategy.getAliasForAtomUri(senderAtomURI.get().toString());
            PrivateKey privateKey = cryptographyService.getPrivateKey(alias);
            PublicKey publicKey = cryptographyService.getPublicKey(alias);
            if (privateKey == null || publicKey == null) {
                throw new WonMessageProcessingException(
                                String.format("Cannot sign message with key for atom %s: key not found",
                                                senderAtomURI));
            }
            try {
                ret.add(processWithKey(message, senderAtomURI.get().toString(), privateKey, publicKey));
            } catch (Exception e) {
                logger.error("Failed to sign", e);
                throw new WonMessageProcessingException("Failed to sign message "
                                + message.getMessageURI().toString(), e);
            }
        }
        return WonMessage.of(ret);
    }

    public WonMessage signWithOtherKey(final WonMessage wonMessage, URI webId) throws WonMessageProcessingException {
        List<WonMessage> ret = new ArrayList<>();
        for (WonMessage message : wonMessage.getAllMessages()) {
            // use senderAtom key for signing
            String alias = keyPairAliasDerivationStrategy.getAliasForAtomUri(webId.toString());
            PrivateKey privateKey = cryptographyService.getPrivateKey(alias);
            PublicKey publicKey = cryptographyService.getPublicKey(alias);
            Objects.requireNonNull(publicKey);
            Objects.requireNonNull(publicKey);
            try {
                ret.add(processWithKey(message, webId.toString(), privateKey, publicKey));
            } catch (Exception e) {
                logger.error("Failed to sign", e);
                throw new WonMessageProcessingException("Failed to sign message "
                                + message.getMessageURI().toString(), e);
            }
        }
        return WonMessage.of(ret);
    }

    private WonMessage processWithKey(final WonMessage wonMessage, final String privateKeyUri,
                    final PrivateKey privateKey, final PublicKey publicKey) throws Exception {
        URI messageURI = wonMessage.getMessageURIRequired();
        if (!Objects.equals(messageURI, WONMSG.MESSAGE_SELF)) {
            // we only sign a message if it still has the self message uri
            return wonMessage;
        }
        WonMessage signed = WonMessageSignerVerifier.signAndSeal(privateKey, publicKey, privateKeyUri, wonMessage);
        if (logger.isDebugEnabled()) {
            logger.debug("SIGNED with key " + privateKeyUri + ":\n"
                            + RdfUtils.toString(Prefixer.setPrefixes(signed.getCompleteDataset())));
        }
        return signed;
    }

    public void setCryptographyService(final CryptographyService cryptoService) {
        this.cryptographyService = cryptoService;
    }

    public void setKeyPairAliasDerivationStrategy(KeyPairAliasDerivationStrategy keyPairAliasDerivationStrategy) {
        this.keyPairAliasDerivationStrategy = keyPairAliasDerivationStrategy;
    }
}
