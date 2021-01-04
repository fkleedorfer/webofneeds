package won.owner.model;

import java.security.Provider;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.security.KeyStore;
import won.cryptography.service.BCProvider;

@Entity
@Table(name = "keystore")
public class KeystoreHolder {
    private static final int DEFAULT_BYTE_ARRAY_SIZE = 500;
    private static final String KEY_STORE_TYPE = "UBER";
    @Id
    @GeneratedValue
    @Column(name = "id")
    private Long id;
    @Transient
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    // the keystore as a byte array
    @Lob
    @Column(name = "keystore_data", nullable = false, length = 10000000)
    private byte[] keystoreBytes;

    public KeystoreHolder() {
        super();
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setKeystoreBytes(byte[] keystoreBytes) {
        this.keystoreBytes = keystoreBytes;
    }

    public byte[] getKeystoreBytes() {
        return keystoreBytes;
    }

    /**
     * Careful, expensive operation: writes dataset to string.
     */
    public synchronized void setKeystore(KeyStore store, String password) throws KeyStoreIOException {
        synchronized (this) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(DEFAULT_BYTE_ARRAY_SIZE);
            if (outputStream != null) {
                try {
                    store.store(outputStream, password.toCharArray());
                    this.keystoreBytes = outputStream.toByteArray();
                } catch (Exception e) {
                    logger.error("Could not save key store " + getId(), e);
                    throw new KeyStoreIOException("Could not save keystore " + getId(), e);
                } finally {
                    try {
                        outputStream.close();
                    } catch (Exception e) {
                        logger.error("Error closing stream of keystore " + getId(), e);
                        throw new KeyStoreIOException("Error closing stream of keystore " + getId(), e);
                    }
                }
            }
        }
    }

    public synchronized KeyStore getKeystore(String password) throws KeyStoreIOException {
        KeyStore store = null;
        InputStream inputStream = null;
        byte[] keystoreData = getKeystoreBytes();
        if (keystoreData == null || keystoreData.length == 0) {
            // return a new, empty key store if there is no key store yet.
            try {
                store = java.security.KeyStore.getInstance(KEY_STORE_TYPE, BCProvider.getInstance());
                store.load(null, password.toCharArray());
                // also set this key store so we can save it to db. (hence the synchronized
                // methods)
                setKeystore(store, password);
                return store;
            } catch (Exception e) {
                throw new KeyStoreIOException("Could not load keystore " + getId(), e);
            }
        }
        inputStream = new ByteArrayInputStream(getKeystoreBytes());
        try {
            store = java.security.KeyStore.getInstance(KEY_STORE_TYPE, BCProvider.getInstance());
            store.load(inputStream, password.toCharArray());
        } catch (Exception e) {
            logger.error("Could not load key store " + getId(), e);
            throw new KeyStoreIOException("Could not load keystore " + getId(), e);
        } finally {
            try {
                inputStream.close();
            } catch (Exception e) {
                logger.error("Error closing stream of keystore " + getId(), e);
                throw new KeyStoreIOException("Error closing stream of keystore " + getId(), e);
            }
        }
        return store;
    }
}
