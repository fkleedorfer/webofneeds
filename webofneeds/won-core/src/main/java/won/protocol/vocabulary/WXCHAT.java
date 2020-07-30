package won.protocol.vocabulary;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;

public class WXCHAT {
    public static final String BASE_URI = "https://w3id.org/won/ext/chat#";
    public static final String DEFAULT_PREFIX = "wx-chat";
    public static final ResourceWrapper ChatSocket = ResourceWrapper.create(BASE_URI + "ChatSocket");
}
