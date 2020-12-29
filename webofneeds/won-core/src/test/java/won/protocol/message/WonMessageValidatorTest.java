package won.protocol.message;

import java.io.IOException;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.vocabulary.RDF;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.validation.WonMessageValidator;
import won.protocol.vocabulary.WONMSG;

/**
 * User: ypanchenko Date: 28.04.2015
 */
public class WonMessageValidatorTest {
    private static final String ATOM_LOCAL_URI = "http://localhost:8080/won/resource/atom/o1ybhchandwvg6c8pv81";
    private static final String NODE_LOCAL_URI = "http://localhost:8080/won/resource";
    private static final String NODE_REMOTE_URI = "http://remotehost:8080/won/resource";
    // create-atom message
    private static final String RESOURCE_FILE_CREATE_MSG_VALID = "/validation/valid/create_msg2.trig";
    private static final String CREATE_MSG_URI = "wm:/jq15ga3aacsxbvl9nngw";
    private static final String CREATE_CONTENT_NAME = CREATE_MSG_URI + "#content-d0zo";
    private static final String CREATE_CONTENT_NAME_SIG = CREATE_MSG_URI + "#content-d0zo-sig";
    private static final String CREATE_ENV_NAME = CREATE_MSG_URI + "#envelope";
    private static final String CREATE_SIG_NAME = CREATE_MSG_URI + "#signature";
    private Dataset createMessageDataset;
    // response message local
    private static final String RESOURCE_FILE_RESPONSE_MSG_VALID = "/validation/valid/response_msg_local.trig";
    private static final String RESPONSE_LOCAL_ENV1_SIG_NAME = "http://localhost:8080/won/resource/event/kpft39z0ladmp3cqm4ju#envelope-c37o-sig";
    private Dataset responseMessageDataset;
    // text message (i.e. connection message) sent from external node
    private static final String RESOURCE_FILE_TEXT_MSG_VALID = "/validation/valid/text_msg_remote.trig";
    private static final String RESOURCE_FILE_TEXT_MSG_VALID_WITH_MISLEADING_CONTENT = "/validation/valid/conversation_msg_with_potentially_misleading_content.trig";
    private static final String TEXT_ENV2_SIG_NAME = "http://localhost:8080/won/resource/event/z7rgjxnyvjpo3l9m0d79#envelope-qdpq-sig";
    private static final String TEXT_ENV3_NAME = "http://localhost:8080/won/resource/event/m8cjzr6892213okiek04#envelope-5t8c";
    private static final String TEXT_ENV3_SIG_NAME = "http://localhost:8080/won/resource/event/m8cjzr6892213okiek04#envelope-5t8c-sig";
    private static final String TEXT_ENV4_SIG_NAME = "http://localhost:8080/won/resource/event/m8cjzr6892213okiek04#envelope-ojt8-sig";
    private Dataset textMessageDataset;
    private Dataset misleadingMessageDataset;

    @Before
    public void init() throws IOException {
        createMessageDataset = Utils.createTestDataset(RESOURCE_FILE_CREATE_MSG_VALID);
        responseMessageDataset = Utils.createTestDataset(RESOURCE_FILE_RESPONSE_MSG_VALID);
        textMessageDataset = Utils.createTestDataset(RESOURCE_FILE_TEXT_MSG_VALID);
        misleadingMessageDataset = Utils.createTestDataset(RESOURCE_FILE_TEXT_MSG_VALID_WITH_MISLEADING_CONTENT);
    }

    @Test
    @Ignore
    public void testValidCreateMessage() throws IOException {
        WonMessageValidator validator = new WonMessageValidator();
        StringBuilder message = new StringBuilder();
        boolean valid = validator.validate(createMessageDataset, message);
        Assert.assertTrue("validation is expected not to fail at " + message, valid);
    }

    @Test
    @Ignore
    public void testValidResponseLocalMessage() throws IOException {
        WonMessageValidator validator = new WonMessageValidator();
        StringBuilder message = new StringBuilder();
        boolean valid = validator.validate(responseMessageDataset, message);
        Assert.assertTrue("validation is expected not to fail at " + message, valid);
    }

    @Test
    @Ignore
    public void testValidTextRemoteMessage() throws IOException {
        WonMessageValidator validator = new WonMessageValidator();
        StringBuilder message = new StringBuilder();
        boolean valid = validator.validate(textMessageDataset, message);
        Assert.assertTrue("validation is expected not to fail at " + message, valid);
    }

    @Test
    @Ignore
    public void testValidMessageWithMisleadingContent() throws IOException {
        WonMessageValidator validator = new WonMessageValidator();
        StringBuilder message = new StringBuilder();
        boolean valid = validator.validate(misleadingMessageDataset, message);
        Assert.assertTrue("validation is not expected to fail at " + message, valid);
    }

    @Test
    public void testValidHintMessage() throws IOException {
        WonMessageValidator validator = new WonMessageValidator();
        StringBuilder message = new StringBuilder();
        boolean valid = validator.validate(Utils.createTestDataset("/validation/valid/hint_msg.trig"), message);
        Assert.assertTrue("validation is expected not to fail at " + message, valid);
    }

    @Test
    public void testValidCreateMessage2() throws IOException {
        WonMessageValidator validator = new WonMessageValidator();
        StringBuilder message = new StringBuilder();
        boolean valid = validator.validate(Utils.createTestDataset("/validation/valid/create_msg2.trig"), message);
        Assert.assertTrue("validation is expected not to fail at " + message, valid);
    }

    @Test
    @Ignore
    public void testValidFailureResponseMessage() throws IOException {
        WonMessageValidator validator = new WonMessageValidator();
        StringBuilder message = new StringBuilder();
        boolean valid = validator.validate(Utils.createTestDataset("/validation/valid/failure_response_msg.trig"),
                        message);
        Assert.assertTrue("validation is expected not to fail at " + message, valid);
    }

    @Test
    @Ignore
    public void testValidForwardToRecipientMessage() throws IOException {
        WonMessageValidator validator = new WonMessageValidator();
        StringBuilder message = new StringBuilder();
        boolean valid = validator.validate(
                        Utils.createTestDataset("/validation/valid/conversation_msg_with_forward.trig"), message);
        Assert.assertTrue("validation is expected not to fail at " + message, valid);
    }

    @Test
    @Ignore
    public void testInvalidForwardToRecipientMessage() throws IOException {
        WonMessageValidator validator = new WonMessageValidator();
        StringBuilder message = new StringBuilder();
        boolean valid = validator.validate(Utils.createTestDataset("/validation/invalid/create_msg_with_forward.trig"),
                        message);
        Assert.assertFalse("validation is expected to fail at " + message, valid);
    }

    @Test
    public void testInvalidDefaultGraph() throws IOException {
        // create invalid dataset by adding a triple into the default graph
        Dataset invalidDataset = WonRdfUtils.MessageUtils
                        .copyByDatasetSerialization(WonMessage.of(createMessageDataset)).getCompleteDataset();
        Model defaultModel = invalidDataset.getDefaultModel();
        Statement stmt = defaultModel.createStatement(ResourceFactory.createResource(),
                        ResourceFactory.createProperty("test:property:uri"),
                        ResourceFactory.createPlainLiteral("test literal"));
        defaultModel.add(stmt);
        // validate this invalid dataset
        WonMessageValidator validator = new WonMessageValidator();
        StringBuilder message = new StringBuilder();
        boolean valid = validator.validate(invalidDataset, message);
        Assert.assertFalse(valid);
        Assert.assertEquals("Default graph is not empty", message.toString());
    }

    @Test
    @Ignore
    public void testMissingAndInvalidMessageDirection() throws IOException {
        // create invalid dataset by removing a triple with message direction
        Dataset invalidDataset = WonRdfUtils.MessageUtils
                        .copyByDatasetSerialization(WonMessage.of(createMessageDataset)).getCompleteDataset();
        Model env1Model = invalidDataset.getNamedModel(CREATE_ENV_NAME);
        Model env2Model = invalidDataset.getNamedModel(CREATE_ENV_NAME);
        Statement stmtOld = env2Model.createStatement(ResourceFactory.createResource(CREATE_MSG_URI),
                        RDF.type, WONMSG.FromOwner);
        env1Model.remove(stmtOld);
        env2Model.remove(stmtOld);
        // validate this invalid dataset
        WonMessageValidator validator = new WonMessageValidator();
        StringBuilder message = new StringBuilder();
        boolean valid = validator.validate(invalidDataset, message);
        Assert.assertFalse(valid);
        Assert.assertTrue(message.toString().contains("missing_direction"));
        // create invalid dataset by adding a triple with invalid message direction
        Statement stmtNew = env2Model.createStatement(ResourceFactory.createResource(CREATE_MSG_URI),
                        RDF.type, ResourceFactory.createProperty("test:property:uri"));
        env2Model.add(stmtNew);
        // validate this invalid dataset
        valid = validator.validate(invalidDataset, message);
        Assert.assertFalse(valid);
        Assert.assertTrue(message.toString().contains("invalid_direction"));
    }

    @Test
    @Ignore
    public void testMissingAndInvalidMessageType() throws IOException {
        // create invalid dataset by removing a triple with message type
        Dataset invalidDataset = WonRdfUtils.MessageUtils
                        .copyByDatasetSerialization(WonMessage.of(createMessageDataset)).getCompleteDataset();
        Model env1Model = invalidDataset.getNamedModel(CREATE_ENV_NAME);
        Statement stmtOld = env1Model.createStatement(ResourceFactory.createResource(CREATE_MSG_URI),
                        WONMSG.messageType, WONMSG.CreateMessage);
        env1Model.remove(stmtOld);
        // validate this invalid dataset
        WonMessageValidator validator = new WonMessageValidator();
        StringBuilder message = new StringBuilder();
        boolean valid = validator.validate(invalidDataset, message);
        Assert.assertFalse(valid);
        Assert.assertTrue(message.toString().contains("missing_type"));
        // create invalid dataset by adding a triple with invalid message type
        Statement stmtNew = env1Model.createStatement(ResourceFactory.createResource(CREATE_MSG_URI),
                        WONMSG.messageType, ResourceFactory.createProperty("test:property:uri"));
        env1Model.add(stmtNew);
        // validate this invalid dataset
        valid = validator.validate(invalidDataset, message);
        Assert.assertFalse(valid);
        Assert.assertTrue(message.toString().contains("invalid_type"));
    }

    @Test
    @Ignore
    public void testMissingTimestamp() throws IOException {
        // create invalid dataset by removing a triple with received timestamp
        Dataset invalidDataset = WonRdfUtils.MessageUtils
                        .copyByDatasetSerialization(WonMessage.of(createMessageDataset)).getCompleteDataset();
        Model env1Model = invalidDataset.getNamedModel(CREATE_ENV_NAME);
        Statement stmt1Old = env1Model.createStatement(ResourceFactory.createResource(CREATE_MSG_URI),
                        WONMSG.timestamp, ResourceFactory.createTypedLiteral("1433774711093", XSDDatatype.XSDlong));
        env1Model.remove(stmt1Old);
        // validate this invalid dataset
        WonMessageValidator validator = new WonMessageValidator();
        StringBuilder message = new StringBuilder();
        boolean valid = validator.validate(invalidDataset, message);
        Assert.assertFalse(valid);
        Assert.assertTrue(message.toString().contains("missing_timestamp"));
    }

    @Test
    @Ignore
    public void testInvalidContentChain() throws IOException {
        Dataset invalidDataset = WonRdfUtils.MessageUtils
                        .copyByDatasetSerialization(WonMessage.of(createMessageDataset)).getCompleteDataset();
        Model env1Model = invalidDataset.getNamedModel(CREATE_ENV_NAME);
        // test 4
        // create invalid dataset by adding a triple that references a content from the
        // second envelope (additionally to
        // the first envelope)
        // validate this invalid dataset
        WonMessageValidator validator = new WonMessageValidator();
        StringBuilder message = new StringBuilder();
        boolean valid = validator.validate(invalidDataset, message);
        Assert.assertFalse(valid);
        Assert.assertTrue(message.toString().contains("validation/05_sign/signature_chain.rq"));
        Statement stmtOld = env1Model.createStatement(ResourceFactory.createResource(CREATE_MSG_URI),
                        WONMSG.content, ResourceFactory.createResource(CREATE_CONTENT_NAME));
        env1Model.remove(stmtOld);
        // validate this invalid dataset
        valid = validator.validate(invalidDataset, message);
        Assert.assertFalse(valid);
        Assert.assertTrue(message.toString().contains("validation/05_sign/signature_chain.rq"));
        // reset for further testing
        env1Model.add(stmtOld);
    }

    @Test
    public void testMetaAndSignerConsistencyFromSystem() throws IOException {
        // Test fromSystem envelopes signer consistency
        Dataset invalidDataset = WonRdfUtils.MessageUtils
                        .copyByDatasetSerialization(WonMessage.of(responseMessageDataset)).getCompleteDataset();
        // Model env1Model = invalidDataset.getNamedModel(RESPONSE_LOCAL_ENV1_NAME);
        Model env1sigModel = invalidDataset.getNamedModel(RESPONSE_LOCAL_ENV1_SIG_NAME);
        // Model env2Model = invalidDataset.getNamedModel(RESPONSE_LOCAL_ENV2_NAME);
        // create invalid dataset by replacing a signer
        Statement stmtOld = env1sigModel.createStatement(ResourceFactory.createResource(RESPONSE_LOCAL_ENV1_SIG_NAME),
                        WONMSG.signer, ResourceFactory.createResource(NODE_LOCAL_URI));
        env1sigModel.remove(stmtOld);
        Statement stmtNew = env1sigModel.createStatement(ResourceFactory.createResource(RESPONSE_LOCAL_ENV1_SIG_NAME),
                        WONMSG.signer, ResourceFactory.createResource(ATOM_LOCAL_URI));
        env1sigModel.add(stmtNew);
        WonMessageValidator validator = new WonMessageValidator();
        StringBuilder message = new StringBuilder();
        // validate this invalid dataset
        boolean valid = validator.validate(invalidDataset, message);
        Assert.assertFalse(valid);
        // reset for further testing:
        env1sigModel.remove(stmtNew);
        env1sigModel.add(stmtOld);
    }

    @Test
    @Ignore
    public void testMetaAndSignerConsistencyFromOwner() throws IOException {
        // Test fromOwner leaf envelope signer consistency
        Dataset invalidDataset = WonRdfUtils.MessageUtils
                        .copyByDatasetSerialization(WonMessage.of(createMessageDataset)).getCompleteDataset();
        Model env1sigModel = invalidDataset.getNamedModel(CREATE_CONTENT_NAME_SIG);
        // create invalid dataset by replacing a signer in leaf envelope
        Statement stmtOld = env1sigModel.createStatement(ResourceFactory.createResource(CREATE_CONTENT_NAME_SIG),
                        WONMSG.signer, ResourceFactory.createResource(ATOM_LOCAL_URI));
        env1sigModel.remove(stmtOld);
        Statement stmtNew = env1sigModel.createStatement(ResourceFactory.createResource(CREATE_CONTENT_NAME_SIG),
                        WONMSG.signer, ResourceFactory.createResource(NODE_LOCAL_URI));
        env1sigModel.add(stmtNew);
        WonMessageValidator validator = new WonMessageValidator();
        StringBuilder message = new StringBuilder();
        // validate this invalid dataset
        boolean valid = validator.validate(invalidDataset, message);
        Assert.assertFalse(valid);
        Assert.assertTrue(message.toString().contains("invalid_from_owner_signer"));
        // reset for further testing:
        env1sigModel.remove(stmtNew);
        env1sigModel.add(stmtOld);
        // Test fromOwner non-leaf envelopes signer consistency
        invalidDataset = WonRdfUtils.MessageUtils.copyByDatasetSerialization(WonMessage.of(textMessageDataset))
                        .getCompleteDataset();
        Model env2sigModel = invalidDataset.getNamedModel(TEXT_ENV2_SIG_NAME);
        // create invalid dataset by replacing a signer in non-leaf envelope
        stmtOld = env2sigModel.createStatement(ResourceFactory.createResource(TEXT_ENV2_SIG_NAME),
                        WONMSG.signer, ResourceFactory.createResource(NODE_LOCAL_URI));
        env2sigModel.remove(stmtOld);
        stmtNew = env2sigModel.createStatement(ResourceFactory.createResource(TEXT_ENV2_SIG_NAME),
                        WONMSG.signer, ResourceFactory.createResource(ATOM_LOCAL_URI));
        env2sigModel.add(stmtNew);
        validator = new WonMessageValidator();
        message = new StringBuilder();
        // validate this invalid dataset
        valid = validator.validate(invalidDataset, message);
        Assert.assertFalse(valid);
        Assert.assertTrue(message.toString().contains("invalid_from_owner_signer"));
        // reset for further testing:
        env1sigModel.remove(stmtNew);
        env1sigModel.add(stmtOld);
    }

    @Test
    @Ignore
    public void testMetaAndSignerConsistencyFromExternal() throws IOException {
        // Test fromExternal close to leaf envelope signer consistency
        Dataset invalidDataset = WonRdfUtils.MessageUtils.copyByDatasetSerialization(WonMessage.of(textMessageDataset))
                        .getCompleteDataset();
        Model env3sigModel = invalidDataset.getNamedModel(TEXT_ENV3_SIG_NAME);
        // create invalid dataset by replacing a signer - sender node - in close to leaf
        // envelope
        Statement stmtOld = env3sigModel.createStatement(ResourceFactory.createResource(TEXT_ENV3_SIG_NAME),
                        WONMSG.signer, ResourceFactory.createResource(NODE_LOCAL_URI));
        env3sigModel.remove(stmtOld);
        Statement stmtNew = env3sigModel.createStatement(ResourceFactory.createResource(TEXT_ENV3_SIG_NAME),
                        WONMSG.signer, ResourceFactory.createResource(NODE_REMOTE_URI));
        env3sigModel.add(stmtNew);
        WonMessageValidator validator = new WonMessageValidator();
        StringBuilder message = new StringBuilder();
        // validate this invalid dataset
        boolean valid = validator.validate(invalidDataset, message);
        Assert.assertFalse(valid);
        Assert.assertTrue(message.toString().contains("invalid_from_external_signer"));
        // reset for further testing:
        env3sigModel.remove(stmtNew);
        env3sigModel.add(stmtOld);
        // Test fromExternal non-leaf envelopes signer consistency
        Model env4sigModel = invalidDataset.getNamedModel(TEXT_ENV4_SIG_NAME);
        // create invalid dataset by replacing a signer in non-leaf envelope
        stmtOld = env4sigModel.createStatement(ResourceFactory.createResource(TEXT_ENV4_SIG_NAME),
                        WONMSG.signer, ResourceFactory.createResource(NODE_LOCAL_URI));
        env4sigModel.remove(stmtOld);
        stmtNew = env4sigModel.createStatement(ResourceFactory.createResource(TEXT_ENV4_SIG_NAME),
                        WONMSG.signer, ResourceFactory.createResource(ATOM_LOCAL_URI));
        env4sigModel.add(stmtNew);
        validator = new WonMessageValidator();
        message = new StringBuilder();
        // validate this invalid dataset
        valid = validator.validate(invalidDataset, message);
        Assert.assertFalse(valid);
        Assert.assertTrue(message.toString().contains("invalid_from_external_signer"));
        // reset for further testing:
        env4sigModel.remove(stmtNew);
        env4sigModel.add(stmtOld);
    }

    @Test
    @Ignore
    public void testSignatureReferenceValues() throws IOException {
        // Test signature of the 1st envelope: replace value of the signature with some
        // dummy value
        Dataset invalidDataset = WonRdfUtils.MessageUtils
                        .copyByDatasetSerialization(WonMessage.of(createMessageDataset)).getCompleteDataset();
        Model env1sigModel = invalidDataset.getNamedModel(CREATE_CONTENT_NAME_SIG);
        StmtIterator iter = env1sigModel.listStatements(ResourceFactory.createResource(CREATE_CONTENT_NAME_SIG),
                        WONMSG.signatureValue, RdfUtils.EMPTY_RDF_NODE);
        Statement stmtOld = iter.removeNext();
        Statement stmtNew = env1sigModel.createStatement(ResourceFactory.createResource(CREATE_CONTENT_NAME_SIG),
                        WONMSG.signatureValue, ResourceFactory.createPlainLiteral("eve's value"));
        env1sigModel.add(stmtNew);
        WonMessageValidator validator = new WonMessageValidator();
        StringBuilder message = new StringBuilder();
        // validate this invalid dataset
        boolean valid = validator.validate(invalidDataset, message);
        Assert.assertFalse(valid);
        Assert.assertTrue(message.toString().contains("signature_reference_values"));
        // reset for further testing:
        env1sigModel.add(stmtOld);
    }

    @Test
    public void testGraphUris() throws IOException {
        // create a dataset with invalid content uris - i.e. replace valid content graph
        // name with
        // the one that does not start with the corresponding event uri: replace the uri
        // in the
        // respective envelope content and content signature reference, as well as in
        // the content signature
        Dataset invalidDataset = WonRdfUtils.MessageUtils
                        .copyByDatasetSerialization(WonMessage.of(createMessageDataset)).getCompleteDataset();
        Model contModel = invalidDataset.getNamedModel(CREATE_CONTENT_NAME);
        invalidDataset.removeNamedModel(CREATE_CONTENT_NAME);
        String dummyName = "test:graph:uri";
        invalidDataset.addNamedModel(dummyName, contModel);
        Model env1Model = invalidDataset.getNamedModel(CREATE_ENV_NAME);
        StmtIterator iter = env1Model.listStatements(null, WONMSG.content, RdfUtils.EMPTY_RDF_NODE);
        Statement stmtOld = iter.removeNext();
        Statement stmtNew = env1Model.createStatement(stmtOld.getSubject(), stmtOld.getPredicate(),
                        ResourceFactory.createResource(dummyName));
        env1Model.add(stmtNew);
        iter = env1Model.listStatements(null, WONMSG.signedGraph, ResourceFactory.createResource(CREATE_CONTENT_NAME));
        Statement stmtOld2 = iter.removeNext();
        Statement stmtNew2 = env1Model.createStatement(stmtOld2.getSubject(), stmtOld2.getPredicate(),
                        ResourceFactory.createResource(dummyName));
        env1Model.add(stmtNew2);
        Model sigModel = invalidDataset.getNamedModel(CREATE_SIG_NAME);
        iter = sigModel.listStatements(null, WONMSG.signedGraph, RdfUtils.EMPTY_RDF_NODE);
        Statement stmtOld3 = iter.removeNext();
        Statement stmtNew3 = sigModel.createStatement(stmtOld3.getSubject(), stmtOld3.getPredicate(),
                        ResourceFactory.createResource(dummyName));
        sigModel.add(stmtNew3);
        // String test = RdfUtils.writeDatasetToString(invalidDataset, Lang.TRIG);
        // System.out.println("OUT:\n" + test);
        WonMessageValidator validator = new WonMessageValidator();
        StringBuilder message = new StringBuilder();
        // validate this invalid dataset
        boolean valid = validator.validate(invalidDataset, message);
        Assert.assertFalse(valid);
        // actually
        Assert.assertTrue(message.toString().contains("graph_uris"));
        // reset for further testing:
        // env2sigModel.add(stmtOld);
        // env2sigModel.remove(stmtNew);
    }

    @Test
    @Ignore
    public void testEventConsistency() throws IOException {
        // create a dataset with invalid remoteEvent uri by replacing the original
        // remote event uri
        // with the dummy uri
        Dataset invalidDataset = WonRdfUtils.MessageUtils.copyByDatasetSerialization(WonMessage.of(textMessageDataset))
                        .getCompleteDataset();
        Model envModel = invalidDataset.getNamedModel(TEXT_ENV3_NAME);
        String dummyName = TEXT_ENV3_NAME;
        String test = RdfUtils.writeDatasetToString(invalidDataset, Lang.TRIG);
        System.out.println("OUT:\n" + test);
        WonMessageValidator validator = new WonMessageValidator();
        StringBuilder message = new StringBuilder();
        // validate this invalid dataset
        boolean valid = validator.validate(invalidDataset, message);
        Assert.assertFalse(valid);
        // actually
        Assert.assertTrue(message.toString().contains("number_of_events"));
        // reset for further testing:
        // env2sigModel.add(stmtOld);
        // env2sigModel.remove(stmtNew);
    }
}
