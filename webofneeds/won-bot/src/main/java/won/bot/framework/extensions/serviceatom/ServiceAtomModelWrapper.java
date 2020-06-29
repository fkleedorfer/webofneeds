package won.bot.framework.extensions.serviceatom;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import won.bot.vocabulary.WXBOT;
import won.protocol.util.DefaultAtomModelWrapper;
import won.protocol.vocabulary.*;

import java.net.URI;
import java.util.Objects;

public class ServiceAtomModelWrapper extends DefaultAtomModelWrapper {
    // TODO: ADD MORE SERVICE BOT ATOM CONTENT MAKE SOCKETS CONFIGURABLE
    private ServiceAtomContent serviceAtomContent;

    public ServiceAtomModelWrapper(URI atomUri, ServiceAtomContent serviceAtomContent) {
        this(atomUri.toString(), serviceAtomContent);
    }

    public ServiceAtomModelWrapper(String atomUri, ServiceAtomContent serviceAtomContent) {
        super(atomUri);
        // SET CONTENT OBJECT
        this.serviceAtomContent = serviceAtomContent;
        // SET RDF STRUCTURE
        Resource atom = this.getAtomModel().createResource(atomUri);
        atom.addProperty(RDF.type, WXBOT.ServiceAtom);
        this.addSocket("#HolderSocket", WXHOLD.HolderSocketString);
        this.addSocket("#ChatSocket", WXCHAT.ChatSocketString);
        this.addSocket("#sReviewSocket", WXSCHEMA.ReviewSocketString);
        this.setDefaultSocket("#ChatSocket");
        if (Objects.nonNull(serviceAtomContent.getName())) {
            this.setName(serviceAtomContent.getName());
        }
        if (Objects.nonNull(serviceAtomContent.getDescription())) {
            this.setDescription(serviceAtomContent.getDescription());
        }
        if (Objects.nonNull(serviceAtomContent.getTermsOfService())) {
            atom.addProperty(SCHEMA.TERMS_OF_SERVICE, serviceAtomContent.getTermsOfService());
        }
    }

    public ServiceAtomModelWrapper(Dataset atomDataset) {
        super(atomDataset);
        serviceAtomContent = new ServiceAtomContent(this.getSomeName());
        serviceAtomContent.setDescription(this.getSomeDescription());
        serviceAtomContent.setTermsOfService(getSomeContentPropertyStringValue(SCHEMA.TERMS_OF_SERVICE));
    }

    public ServiceAtomContent getServiceAtomContent() {
        return serviceAtomContent;
    }
}
