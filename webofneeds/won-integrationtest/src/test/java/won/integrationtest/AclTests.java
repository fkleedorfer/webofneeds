package won.integrationtest;

import org.apache.jena.query.Dataset;
import org.apache.jena.sparql.graph.GraphFactory;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.auth.linkeddata.AuthEnabledLinkedDataSource;
import won.auth.model.*;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.EventBotAction;
import won.bot.framework.eventbot.action.EventBotActionUtils;
import won.bot.framework.eventbot.behaviour.BehaviourBarrier;
import won.bot.framework.eventbot.behaviour.BotBehaviour;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.wonmessage.ConnectFromOtherAtomEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.MessageFromOtherAtomEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.SuccessResponseEvent;
import won.bot.framework.eventbot.filter.EventFilter;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnceAfterNEventsListener;
import won.protocol.message.WonMessage;
import won.protocol.message.builder.WonMessageBuilder;
import won.protocol.rest.LinkedDataFetchingException;
import won.protocol.util.linkeddata.CachingLinkedDataSource;
import won.protocol.vocabulary.*;
import won.shacl2java.Shacl2JavaInstanceFactory;
import won.utils.content.ContentUtils;
import won.utils.content.model.AtomContent;
import won.utils.content.model.RdfOutput;
import won.utils.content.model.Socket;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static won.auth.model.Individuals.*;

public class AclTests extends AbstractBotBasedTest {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Test(timeout = 60 * 1000)
    public void testCreateAtomWithEmptyACL_isPubliclyReadable() throws Exception {
        runTest(ctx -> {
            EventBus bus = ctx.getEventBus();
            final URI wonNodeUri = ctx.getNodeURISource().getNodeURI();
            final URI atomUri = ctx.getWonNodeInformationService().generateAtomURI(wonNodeUri);
            final String atomUriString = atomUri.toString();
            final AtomContent atomContent = new AtomContent(atomUriString);
            atomContent.addTitle("Unit Test Atom ACL Test 1");
            atomContent.addType(URI.create(WON.Atom.getURI()));
            atomContent.addSocket(Socket.builder(atomUri.toString() + "#chatSocket")
                            .setSocketDefinition(WXCHAT.ChatSocket.asURI()).build());
            WonMessage createMessage = WonMessageBuilder.createAtom()
                            .atom(atomUri)
                            .content()
                            /**/.graph(RdfOutput.toGraph(atomContent))
                            .content()
                            /**/.aclGraph(GraphFactory.createGraphMem()) // add an empty acl graph
                            .build();
            createMessage = ctx.getWonMessageSender().prepareMessage(createMessage);
            ctx.getBotContextWrapper().rememberAtomUri(atomUri);
            final String action = "Create Atom action";
            EventListener successCallback = event -> {
                ((CachingLinkedDataSource) ctx.getLinkedDataSource()).clear();
                boolean passed = true;
                passed = passed && testLinkedDataRequestOk(ctx, bus, "withWebid", atomUri, atomUri);
                passed = passed && testLinkedDataRequestOkNoWebId(ctx, bus, "withoutWebId", atomUri);
                if (passed) {
                    passTest(bus);
                }
            };
            EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                            action);
            EventBotActionUtils.makeAndSubscribeResponseListener(createMessage, successCallback,
                            failureCallback, ctx);
            ctx.getWonMessageSender().sendMessage(createMessage);
        });
    }

    @Test(timeout = 60 * 1000)
    public void testCreateAtomWithEmptyACLButSocketAuths_isNotPubliclyReadable() throws Exception {
        runTest(ctx -> {
            EventBus bus = ctx.getEventBus();
            final URI wonNodeUri = ctx.getNodeURISource().getNodeURI();
            final URI atomUri = ctx.getWonNodeInformationService().generateAtomURI(wonNodeUri);
            final String atomUriString = atomUri.toString();
            final AtomContent atomContent = new AtomContent(atomUriString);
            atomContent.addTitle("Unit Test Atom ACL Test 1");
            final Socket holderSocket = new Socket(atomUriString + "#holderSocket");
            holderSocket.setSocketDefinition(WXHOLD.HolderSocket.asURI());
            final Socket buddySocket = new Socket(atomUriString + "#buddySocket");
            buddySocket.setSocketDefinition(WXBUDDY.BuddySocket.asURI());
            atomContent.addSocket(holderSocket);
            atomContent.addSocket(buddySocket);
            atomContent.addType(URI.create(WON.Atom.getURI()));
            WonMessage createMessage = WonMessageBuilder.createAtom()
                            .atom(atomUri)
                            .content()
                            /**/.graph(RdfOutput.toGraph(atomContent))
                            .content()
                            /**/.aclGraph(GraphFactory.createGraphMem()) // add an empty acl graph
                            .build();
            createMessage = ctx.getWonMessageSender().prepareMessage(createMessage);
            ctx.getBotContextWrapper().rememberAtomUri(atomUri);
            final String action = "Create Atom action";
            EventListener successCallback = event -> {
                ((CachingLinkedDataSource) ctx.getLinkedDataSource()).clear();
                boolean passed = true;
                passed = passed && testLinkedDataRequestOk(ctx, bus, "withWebid", atomUri, atomUri);
                passed = passed && testLinkedDataRequestFailsNoWebId(ctx, bus, "withoutWebId",
                                LinkedDataFetchingException.Forbidden.class);
                if (passed) {
                    passTest(bus);
                }
            };
            EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                            action);
            EventBotActionUtils.makeAndSubscribeResponseListener(createMessage, successCallback,
                            failureCallback, ctx);
            ctx.getWonMessageSender().sendMessage(createMessage);
        });
    }

    @Test(timeout = 60 * 1000)
    public void testAtomWithoutACL_fallbackToLegacyImpl() throws Exception {
        runTest(ctx -> {
            EventBus bus = ctx.getEventBus();
            final URI wonNodeUri = ctx.getNodeURISource().getNodeURI();
            final URI atomUri = ctx.getWonNodeInformationService().generateAtomURI(wonNodeUri);
            final String atomUriString = atomUri.toString();
            final AtomContent atomContent = AtomContent.builder(atomUri)
                            .addTitle("Unit Test Atom ACL Test 2")
                            .addSocket(Socket.builder(atomUri.toString() + "#chatSocket")
                                            .setSocketDefinition(WXCHAT.ChatSocket.asURI()).build())
                            .addType(URI.create(WON.Atom.getURI()))
                            .build();
            WonMessage createMessage = WonMessageBuilder.createAtom()
                            .atom(atomUri)
                            .content()
                            /**/.graph(RdfOutput.toGraph(atomContent))
                            .build();
            createMessage = ctx.getWonMessageSender().prepareMessage(createMessage);
            ctx.getBotContextWrapper().rememberAtomUri(atomUri);
            final String action = "Create Atom action";
            EventListener successCallback = event -> {
                ((CachingLinkedDataSource) ctx.getLinkedDataSource()).clear();
                URI connContainerUri = uriService.createConnectionContainerURIForAtom(atomUri);
                URI createMessageUri = ((SuccessResponseEvent) event).getOriginalMessageURI();
                boolean passed = true;
                passed = passed && testLinkedDataRequestOk(ctx, bus, "test1.", atomUri, atomUri, createMessageUri);
                passed = passed && testLinkedDataRequestOk_emptyDataset(ctx, bus, "test2.", atomUri, connContainerUri);
                passed = passed && testLinkedDataRequestOkNoWebId(ctx, bus, "test3.", atomUri);
                passed = passed && testLinkedDataRequestOkNoWebId_emptyDataset(ctx, bus, "test4.", connContainerUri);
                passed = passed && testLinkedDataRequestFailsNoWebId(ctx, bus, "test5.",
                                LinkedDataFetchingException.class,
                                createMessageUri);
                if (passed) {
                    passTest(bus);
                }
            };
            EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                            action);
            EventBotActionUtils.makeAndSubscribeResponseListener(createMessage, successCallback,
                            failureCallback, ctx);
            ctx.getWonMessageSender().sendMessage(createMessage);
        });
    }

    @Test(timeout = 60 * 1000)
    public void testCreateAtomWithACL_isNotPubliclyReadable() throws Exception {
        runTest(ctx -> {
            EventBus bus = ctx.getEventBus();
            final URI wonNodeUri = ctx.getNodeURISource().getNodeURI();
            final URI atomUri = ctx.getWonNodeInformationService().generateAtomURI(wonNodeUri);
            final String atomUriString = atomUri.toString();
            final AtomContent atomContent = AtomContent.builder(atomUri)
                            .addTitle("Unit Test Atom ACL Test 3 (with acl)")
                            .addSocket(Socket.builder(atomUriString + "#holderSocket")
                                            .setSocketDefinition(WXHOLD.HolderSocket.asURI())
                                            .build())
                            .addSocket(Socket.builder(atomUriString + "#buddySocket")
                                            .setSocketDefinition(WXBUDDY.BuddySocket.asURI())
                                            .build())
                            .addType(URI.create(WON.Atom.getURI()))
                            .build();
            // create an acl allowing only the atom itself to read everything
            Authorization auth = Authorization.builder()
                            .addGrant(ase -> ase.addOperationsSimpleOperationExpression(OP_READ))
                            .addGranteesAtomExpression(ae -> ae.addAtomsURI(URI.create("https://example.com/nobody")))
                            .build();
            WonMessage createMessage = WonMessageBuilder.createAtom()
                            .atom(atomUri)
                            .content().graph(RdfOutput.toGraph(atomContent))
                            .content().aclGraph(won.auth.model.RdfOutput.toGraph(auth)) // add the acl graph
                            .build();
            createMessage = ctx.getWonMessageSender().prepareMessage(createMessage);
            ctx.getBotContextWrapper().rememberAtomUri(atomUri);
            final String action = "Create Atom action";
            EventListener successCallback = event -> {
                URI connContainerUri = uriService.createConnectionContainerURIForAtom(atomUri);
                URI createMessageUri = ((SuccessResponseEvent) event).getOriginalMessageURI();
                boolean passed = true;
                passed = passed && testLinkedDataRequestOk(ctx, bus, "test1.", atomUri, atomUri, createMessageUri);
                passed = passed && testLinkedDataRequestOk_emptyDataset(ctx, bus, "test2.", atomUri, connContainerUri);
                passed = passed && testLinkedDataRequestFailsNoWebId(ctx, bus, "test3.",
                                LinkedDataFetchingException.class,
                                atomUri, connContainerUri, createMessageUri);
                if (passed) {
                    passTest(bus);
                }
            };
            EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                            action);
            EventBotActionUtils.makeAndSubscribeResponseListener(createMessage, successCallback,
                            failureCallback, ctx);
            ctx.getWonMessageSender().sendMessage(createMessage);
        });
    }

    @Test(timeout = 60 * 1000)
    public void testImplicitOwnerToken() throws Exception {
        runTest(ctx -> {
            EventBus bus = ctx.getEventBus();
            final URI wonNodeUri = ctx.getNodeURISource().getNodeURI();
            final URI atomUri = ctx.getWonNodeInformationService().generateAtomURI(wonNodeUri);
            final String atomUriString = atomUri.toString();
            final AtomContent atomContent = AtomContent.builder(atomUri)
                            .addTitle("Unit test for implicit owner token")
                            .addSocket(Socket.builder(atomUriString + "#holderSocket")
                                            .setSocketDefinition(WXHOLD.HolderSocket.asURI())
                                            .build())
                            .addSocket(Socket.builder(atomUriString + "#buddySocket")
                                            .setSocketDefinition(WXBUDDY.BuddySocket.asURI())
                                            .build())
                            .addType(URI.create(WON.Atom.getURI()))
                            .build();
            // create an acl allowing only the atom itself to read everything
            Authorization auth = Authorization.builder()
                            .addGrant(ase -> ase.addOperationsSimpleOperationExpression(OP_READ))
                            .addGranteesAtomExpression(ae -> ae.addAtomsURI(URI.create("https://example.com/nobody")))
                            .build();
            WonMessage createMessage = WonMessageBuilder.createAtom()
                            .atom(atomUri)
                            .content().graph(RdfOutput.toGraph(atomContent))
                            .content().aclGraph(won.auth.model.RdfOutput.toGraph(auth)) // add the acl graph
                            .build();
            createMessage = ctx.getWonMessageSender().prepareMessage(createMessage);
            ctx.getBotContextWrapper().rememberAtomUri(atomUri);
            final String action = "Create Atom action";
            EventListener successCallback = event -> {
                URI connContainerUri = uriService.createConnectionContainerURIForAtom(atomUri);
                URI createMessageUri = ((SuccessResponseEvent) event).getOriginalMessageURI();
                boolean passed = true;
                URI tokenQuery = uriService
                                .createTokenRequestURIForAtomURIWithScopes(atomUri, WONAUTH.OwnerToken.toString());
                passed = testTokenRequest(ctx, bus, null, false,
                                atomUri, null, tokenQuery, "test1.1 - obtain token");
                Set<String> tokens = ((AuthEnabledLinkedDataSource) ctx.getLinkedDataSource())
                                .getAuthTokens(tokenQuery, atomUri);
                String token = tokens.stream().findFirst().get();
                passed = passed && testTokenRequest(ctx, bus, null, true,
                                null, token, tokenQuery, "test1.2 - request token using only token");
                passed = passed && testLinkedDataRequest(ctx, bus, null, false,
                                null, null, atomUri,
                                "test1.3 - request atom data without any auth (continues session with token)");
                passed = passed && testLinkedDataRequest(ctx, bus, null, true,
                                atomUri, null, atomUri, "test1.4 - request atom data with webid");
                passed = passed && testLinkedDataRequest(ctx, bus, null, true,
                                null, token, atomUri, "test1.5 - request atom data with token");
                if (passed) {
                    passTest(bus);
                }
            };
            EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                            action);
            EventBotActionUtils.makeAndSubscribeResponseListener(createMessage, successCallback,
                            failureCallback, ctx);
            ctx.getWonMessageSender().sendMessage(createMessage);
        });
    }

    @Test(timeout = 60 * 1000)
    public void testExplicitReadAuthorization() throws Exception {
        final AtomicBoolean atom1Created = new AtomicBoolean(false);
        final AtomicBoolean atom2Created = new AtomicBoolean(false);
        final CountDownLatch latch = new CountDownLatch(2);
        final AtomicReference<URI> createMessageUri1 = new AtomicReference();
        final AtomicReference<URI> createMessageUri2 = new AtomicReference();
        runTest(ctx -> {
            EventBus bus = ctx.getEventBus();
            final URI wonNodeUri = ctx.getNodeURISource().getNodeURI();
            final URI atomUri1 = ctx.getWonNodeInformationService().generateAtomURI(wonNodeUri);
            final URI atomUri2 = ctx.getWonNodeInformationService().generateAtomURI(wonNodeUri);
            {
                final String atomUriString = atomUri1.toString();
                final AtomContent atomContent = AtomContent.builder(atomUriString)
                                .addTitle("Granting atom")
                                .addSocket(Socket.builder(atomUriString + "#chatSocket")
                                                .setSocketDefinition(WXCHAT.ChatSocket.asURI())
                                                .build())
                                .addType(URI.create(WON.Atom.getURI()))
                                .build();
                Authorization auth = Authorization.builder()
                                .addGranteesAtomExpression(ae -> ae.addAtomsURI(atomUri2))
                                .addGrant(ase -> ase.addOperationsSimpleOperationExpression(OP_READ))
                                .build();
                WonMessage createMessage = WonMessageBuilder.createAtom()
                                .atom(atomUri1)
                                .content().graph(RdfOutput.toGraph(atomContent))
                                .content().aclGraph(won.auth.model.RdfOutput.toGraph(auth))
                                .build();
                createMessage = ctx.getWonMessageSender().prepareMessage(createMessage);
                ctx.getBotContextWrapper().rememberAtomUri(atomUri1);
                final String action = "Create granting atom";
                EventListener successCallback = event -> {
                    logger.debug("Granting atom created");
                    createMessageUri1.set(((SuccessResponseEvent) event).getOriginalMessageURI());
                    latch.countDown();
                };
                EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                action);
                EventBotActionUtils.makeAndSubscribeResponseListener(createMessage, successCallback,
                                failureCallback, ctx);
                ctx.getWonMessageSender().sendMessage(createMessage);
            }
            // create match source
            {
                final String atomUriString = atomUri2.toString();
                final AtomContent atomContent = AtomContent.builder(atomUriString)
                                .addTitle("Grantee atom")
                                .addSparqlQuery(
                                                "PREFIX won:<https://w3id.org/won/core#>\n"
                                                                + "PREFIX con:<https://w3id.org/won/content#>\n"
                                                                + "SELECT ?result (1.0 AS ?score) WHERE {"
                                                                + "?result a won:Atom ;"
                                                                + "    con:tag \"tag-to-match\"."
                                                                + "}")
                                .addSocket(Socket.builder(atomUriString + "#chatSocket")
                                                .setSocketDefinition(WXCHAT.ChatSocket.asURI())
                                                .build())
                                .addType(URI.create(WON.Atom.getURI()))
                                .build();
                Authorization auth = Authorization.builder()
                                .addGranteesAtomExpression(
                                                ae -> ae.addAtomsURI(URI.create("http://example.com/nobody")))
                                .addGrant(ase -> ase.addOperationsSimpleOperationExpression(OP_READ))
                                .build();
                WonMessage createMessage = WonMessageBuilder.createAtom()
                                .atom(atomUri2)
                                .content().graph(RdfOutput.toGraph(atomContent))
                                .content().aclGraph(won.auth.model.RdfOutput.toGraph(auth))
                                .build();
                createMessage = ctx.getWonMessageSender().prepareMessage(createMessage);
                ctx.getBotContextWrapper().rememberAtomUri(atomUri2);
                final String action = "Create grantee atom";
                EventListener successCallback = event -> {
                    logger.debug("Grantee atom created");
                    createMessageUri2.set(((SuccessResponseEvent) event).getOriginalMessageURI());
                    latch.countDown();
                };
                EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                action);
                EventBotActionUtils.makeAndSubscribeResponseListener(createMessage, successCallback,
                                failureCallback, ctx);
                ctx.getWonMessageSender().sendMessage(createMessage);
            }
            ctx.getExecutor().execute(() -> {
                try {
                    latch.await();
                    URI connContainerUri1 = uriService.createConnectionContainerURIForAtom(atomUri1);
                    URI connContainerUri2 = uriService.createConnectionContainerURIForAtom(atomUri2);
                    boolean passed = true;
                    passed = passed && testLinkedDataRequestOk(ctx, bus, "test1.", atomUri1, atomUri1,
                                    createMessageUri1.get());
                    passed = passed && testLinkedDataRequestOk(ctx, bus, "test2.", atomUri2, atomUri1,
                                    createMessageUri1.get());
                    passed = passed && testLinkedDataRequestFails(ctx, bus, "test3.", atomUri1,
                                    LinkedDataFetchingException.class,
                                    atomUri2, connContainerUri2, createMessageUri2.get());
                    passed = passed && testLinkedDataRequestOk_emptyDataset(ctx, bus, "test5.", atomUri1,
                                    connContainerUri1);
                    passed = passed && testLinkedDataRequestOk_emptyDataset(ctx, bus, "test6.", atomUri2,
                                    connContainerUri1, connContainerUri2);
                    passed = passed && testLinkedDataRequestFailsNoWebId(ctx, bus, "test7.",
                                    LinkedDataFetchingException.class,
                                    atomUri1, connContainerUri1, atomUri2, connContainerUri2, createMessageUri1.get(),
                                    createMessageUri2.get());
                    if (passed) {
                        passTest(bus);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            logger.debug("Finished initializing test 'testQueryBasedMatch()'");
        });
    }

    @Test(timeout = 60 * 1000)
    public void testModifyOnBehalf_fail() throws Exception {
        final AtomicBoolean atom1Created = new AtomicBoolean(false);
        final AtomicBoolean atom2Created = new AtomicBoolean(false);
        final CountDownLatch latch = new CountDownLatch(2);
        final AtomicReference<URI> createMessageUri1 = new AtomicReference();
        final AtomicReference<URI> createMessageUri2 = new AtomicReference();
        runTest(ctx -> {
            EventBus bus = ctx.getEventBus();
            final URI wonNodeUri = ctx.getNodeURISource().getNodeURI();
            final URI atomUri1 = ctx.getWonNodeInformationService().generateAtomURI(wonNodeUri);
            final URI atomUri2 = ctx.getWonNodeInformationService().generateAtomURI(wonNodeUri);
            BotBehaviour bbAtom1 = new BotBehaviour(ctx) {
                @Override
                protected void onActivate(Optional<Object> message) {
                    final String atomUriString = atomUri1.toString();
                    final AtomContent atomContent = AtomContent.builder(atomUriString)
                                    .addTitle("Granting atom")
                                    .addSocket(Socket.builder(atomUriString + "#chatSocket")
                                                    .setSocketDefinition(WXCHAT.ChatSocket.asURI())
                                                    .build())
                                    .addType(URI.create(WON.Atom.getURI()))
                                    .build();
                    Authorization auth = Authorization.builder()
                                    .addGranteesAtomExpression(ae -> ae.addAtomsURI(atomUri2))
                                    .addGrant(ase -> ase.addOperationsSimpleOperationExpression(OP_READ))
                                    .build();
                    WonMessage createMessage = WonMessageBuilder.createAtom()
                                    .atom(atomUri1)
                                    .content().graph(RdfOutput.toGraph(atomContent))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(auth))
                                    .build();
                    createMessage = ctx.getWonMessageSender().prepareMessage(createMessage);
                    ctx.getBotContextWrapper().rememberAtomUri(atomUri1);
                    final String action = "Create granting atom";
                    EventListener successCallback = event -> {
                        logger.debug("Granting atom created");
                        createMessageUri1.set(((SuccessResponseEvent) event).getOriginalMessageURI());
                        deactivate();
                    };
                    EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                    action);
                    EventBotActionUtils.makeAndSubscribeResponseListener(createMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(createMessage);
                }
            };
            BotBehaviour bbAtom2 = new BotBehaviour(ctx) {
                @Override
                protected void onActivate(Optional<Object> message) {
                    final String atomUriString = atomUri2.toString();
                    final AtomContent atomContent = AtomContent.builder(atomUriString)
                                    .addTitle("Grantee atom")
                                    .addSocket(Socket.builder(atomUriString + "#chatSocket")
                                                    .setSocketDefinition(WXCHAT.ChatSocket.asURI())
                                                    .build())
                                    .addType(URI.create(WON.Atom.getURI()))
                                    .build();
                    WonMessage createMessage = WonMessageBuilder.createAtom()
                                    .atom(atomUri2)
                                    .content().graph(RdfOutput.toGraph(atomContent))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(getNobodyMayDoNothing()))
                                    .build();
                    createMessage = ctx.getWonMessageSender().prepareMessage(createMessage);
                    ctx.getBotContextWrapper().rememberAtomUri(atomUri2);
                    final String action = "Create grantee atom";
                    EventListener successCallback = event -> {
                        logger.debug("Grantee atom created");
                        createMessageUri2.set(((SuccessResponseEvent) event).getOriginalMessageURI());
                        deactivate();
                    };
                    EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                    action);
                    EventBotActionUtils.makeAndSubscribeResponseListener(createMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(createMessage);
                }
            };
            BotBehaviour bbReplaceAtom1FromAtom2 = new BotBehaviour(ctx) {
                @Override
                protected void onActivate(Optional<Object> message) {
                    WonMessage replaceMessage = WonMessageBuilder.replace()
                                    .atom(atomUri1)
                                    .content().graph(RdfOutput.toGraph(AtomContent.builder()
                                                    .addTitle("Replaced title")
                                                    .addTag("ANewTag")
                                                    .addType(URI.create(WON.Persona.getURI()))
                                                    .addSocket(Socket.builder()
                                                                    .setSocketDefinition(WXBUDDY.BuddySocket.asURI())
                                                                    .build())
                                                    .addSocket(Socket.builder()
                                                                    .setSocketDefinition(WXHOLD.HolderSocket.asURI())
                                                                    .build())
                                                    .build()))
                                    .build();
                    EventListener passIfFailure = event -> {
                        logger.debug("Illegal replace denied successfully");
                        deactivate();
                        passTest(bus);
                    };
                    replaceMessage = ctx.getWonMessageSender().prepareMessageOnBehalf(replaceMessage, atomUri2);
                    EventListener failIfSuccess = makeSuccessCallbackToFailTest(bot, ctx, bus,
                                    "modifying atom1 using atom2's WebID");
                    EventBotActionUtils.makeAndSubscribeResponseListener(replaceMessage, failIfSuccess,
                                    passIfFailure, ctx);
                    ctx.getWonMessageSender().sendMessage(replaceMessage);
                }
            };
            BehaviourBarrier barrier = new BehaviourBarrier(ctx);
            barrier.waitFor(bbAtom1, bbAtom2);
            barrier.thenStart(bbReplaceAtom1FromAtom2);
            barrier.activate();
            bbAtom1.activate();
            bbAtom2.activate();
            logger.debug("Finished initializing test 'testQueryBasedMatch()'");
        });
    }

    @Test(timeout = 60 * 1000)
    public void testModifyOnBehalf() throws Exception {
        final AtomicBoolean atom1Created = new AtomicBoolean(false);
        final AtomicBoolean atom2Created = new AtomicBoolean(false);
        final CountDownLatch latch = new CountDownLatch(2);
        final AtomicReference<URI> createMessageUri1 = new AtomicReference();
        final AtomicReference<URI> createMessageUri2 = new AtomicReference();
        runTest(ctx -> {
            EventBus bus = ctx.getEventBus();
            final URI wonNodeUri = ctx.getNodeURISource().getNodeURI();
            final URI atomUri1 = ctx.getWonNodeInformationService().generateAtomURI(wonNodeUri);
            final URI atomUri2 = ctx.getWonNodeInformationService().generateAtomURI(wonNodeUri);
            BotBehaviour bbAtom1 = new BotBehaviour(ctx) {
                @Override
                protected void onActivate(Optional<Object> message) {
                    final String atomUriString = atomUri1.toString();
                    final AtomContent atomContent = AtomContent.builder(atomUriString)
                                    .addTitle("Granting atom")
                                    .addSocket(Socket.builder(atomUriString + "#chatSocket")
                                                    .setSocketDefinition(WXCHAT.ChatSocket.asURI())
                                                    .build())
                                    .addType(URI.create(WON.Atom.getURI()))
                                    .build();
                    Authorization auth = Authorization.builder()
                                    .addGranteesAtomExpression(ae -> ae.addAtomsURI(atomUri2))
                                    .addGrant(ase -> ase.addOperationsSimpleOperationExpression(OP_READ)
                                                    .addGraph(g -> g
                                                                    .addGraphType(GraphType.CONTENT_GRAPH)
                                                                    .addOperationsMessageOperationExpression(moe -> moe
                                                                                    .addMessageOnBehalfsMessageType(
                                                                                                    MessageType.REPLACE_MESSAGE))))
                                    .build();
                    WonMessage createMessage = WonMessageBuilder.createAtom()
                                    .atom(atomUri1)
                                    .content().graph("#content", RdfOutput.toGraph(atomContent))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(auth))
                                    .build();
                    createMessage = ctx.getWonMessageSender().prepareMessage(createMessage);
                    ctx.getBotContextWrapper().rememberAtomUri(atomUri1);
                    final String action = "Create granting atom";
                    EventListener successCallback = event -> {
                        logger.debug("Granting atom created");
                        createMessageUri1.set(((SuccessResponseEvent) event).getOriginalMessageURI());
                        deactivate();
                    };
                    EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                    action);
                    EventBotActionUtils.makeAndSubscribeResponseListener(createMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(createMessage);
                }
            };
            BotBehaviour bbAtom2 = new BotBehaviour(ctx) {
                @Override
                protected void onActivate(Optional<Object> message) {
                    final String atomUriString = atomUri2.toString();
                    final AtomContent atomContent = AtomContent.builder(atomUriString)
                                    .addTitle("Grantee atom")
                                    .addSocket(Socket.builder(atomUriString + "#chatSocket")
                                                    .setSocketDefinition(WXCHAT.ChatSocket.asURI())
                                                    .build())
                                    .addType(URI.create(WON.Atom.getURI()))
                                    .build();
                    WonMessage createMessage = WonMessageBuilder.createAtom()
                                    .atom(atomUri2)
                                    .content().graph(RdfOutput.toGraph(atomContent))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(getNobodyMayDoNothing()))
                                    .build();
                    createMessage = ctx.getWonMessageSender().prepareMessage(createMessage);
                    ctx.getBotContextWrapper().rememberAtomUri(atomUri2);
                    final String action = "Create grantee atom";
                    EventListener successCallback = event -> {
                        logger.debug("Grantee atom created");
                        createMessageUri2.set(((SuccessResponseEvent) event).getOriginalMessageURI());
                        deactivate();
                    };
                    EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                    action);
                    EventBotActionUtils.makeAndSubscribeResponseListener(createMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(createMessage);
                }
            };
            BotBehaviour bbReplaceAtom1FromAtom2 = new BotBehaviour(ctx) {
                @Override
                protected void onActivate(Optional<Object> message) {
                    WonMessage replaceMessage = WonMessageBuilder.replace()
                                    .atom(atomUri1)
                                    .content().graph("#content", RdfOutput.toGraph(AtomContent.builder(atomUri1)
                                                    .addTitle("Replaced Title")
                                                    .addTag("A-New-Tag")
                                                    .addType(URI.create(WON.Atom.getURI()))
                                                    .addType(URI.create(WON.Persona.getURI()))
                                                    .addSocket(Socket.builder(atomUri1.toString() + "#buddySocket")
                                                                    .setSocketDefinition(WXBUDDY.BuddySocket.asURI())
                                                                    .build())
                                                    .addSocket(Socket.builder(atomUri1.toString() + "#holderSocket")
                                                                    .setSocketDefinition(WXHOLD.HolderSocket.asURI())
                                                                    .build())
                                                    .build()))
                                    .build();
                    String action = "replace content of atom1 using webid of atom2";
                    EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus, action);
                    replaceMessage = ctx.getWonMessageSender().prepareMessageOnBehalf(replaceMessage, atomUri2);
                    EventListener successCallback = makeLoggingCallback(bot, bus, action, () -> deactivate());
                    EventBotActionUtils.makeAndSubscribeResponseListener(replaceMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(replaceMessage);
                }
            };
            BotBehaviour readModified = new BotBehaviour(ctx) {
                @Override
                protected void onActivate(Optional<Object> message) {
                    Dataset atomData = ctx.getLinkedDataSource().getDataForResource(atomUri1, atomUri1);
                    Set<String> expectedGraphNames = Set.of(atomUri1 + "#acl",
                                    atomUri1 + "#acl-sig",
                                    atomUri1 + "#socket-acl",
                                    atomUri1 + "#content",
                                    atomUri1 + "#content-sig",
                                    atomUri1 + "#key",
                                    atomUri1 + "#key-sig",
                                    atomUri1 + "#sysinfo");
                    Iterator<String> it = atomData.listNames();
                    Set<String> actualNames = new HashSet<>();
                    while (it.hasNext()) {
                        actualNames.add(it.next());
                    }
                    Assert.assertEquals(expectedGraphNames, actualNames);
                    Shacl2JavaInstanceFactory instanceFactory = ContentUtils.newInstanceFactory();
                    Shacl2JavaInstanceFactory.Accessor accessor = instanceFactory
                                    .accessor(atomData.getNamedModel(atomUri1 + "#content").getGraph());
                    Optional<AtomContent> atomContent = accessor
                                    .getInstanceOfType(atomUri1.toString(), AtomContent.class);
                    Assert.assertTrue("replacing content failed: replaced title not found",
                                    atomContent.get().getTitles().contains("Replaced Title"));
                    Assert.assertFalse("replacing content failed: old title still there",
                                    atomContent.get().getTitles().contains("Granting atom"));
                    passTest(bus);
                }
            };
            BehaviourBarrier barrier = new BehaviourBarrier(ctx);
            barrier.waitFor(bbAtom1, bbAtom2);
            barrier.thenStart(bbReplaceAtom1FromAtom2);
            barrier.activate();
            bbAtom1.activate();
            bbAtom2.activate();
            bbReplaceAtom1FromAtom2.onDeactivateActivate(readModified);
            logger.debug("Finished initializing test 'testQueryBasedMatch()'");
        });
    }

    @Test(timeout = 60 * 1000)
    public void testModifyOnBehalf_addGraph() throws Exception {
        final AtomicBoolean atom1Created = new AtomicBoolean(false);
        final AtomicBoolean atom2Created = new AtomicBoolean(false);
        final CountDownLatch latch = new CountDownLatch(2);
        final AtomicReference<URI> createMessageUri1 = new AtomicReference();
        final AtomicReference<URI> createMessageUri2 = new AtomicReference();
        runTest(ctx -> {
            EventBus bus = ctx.getEventBus();
            final URI wonNodeUri = ctx.getNodeURISource().getNodeURI();
            final URI atomUri1 = ctx.getWonNodeInformationService().generateAtomURI(wonNodeUri);
            final URI atomUri2 = ctx.getWonNodeInformationService().generateAtomURI(wonNodeUri);
            BotBehaviour bbAtom1 = new BotBehaviour(ctx) {
                @Override
                protected void onActivate(Optional<Object> message) {
                    final String atomUriString = atomUri1.toString();
                    final AtomContent atomContent = AtomContent.builder(atomUriString)
                                    .addTitle("Granting atom")
                                    .addSocket(Socket.builder(atomUriString + "#chatSocket")
                                                    .setSocketDefinition(WXCHAT.ChatSocket.asURI())
                                                    .build())
                                    .addType(URI.create(WON.Atom.getURI()))
                                    .build();
                    Authorization auth = Authorization.builder()
                                    .addGranteesAtomExpression(ae -> ae.addAtomsURI(atomUri2))
                                    .addGrant(ase -> ase.addOperationsSimpleOperationExpression(OP_READ)
                                                    .addGraph(g -> g
                                                                    .addGraphType(GraphType.CONTENT_GRAPH)
                                                                    .addOperationsMessageOperationExpression(moe -> moe
                                                                                    .addMessageOnBehalfsMessageType(
                                                                                                    MessageType.REPLACE_MESSAGE))))
                                    .build();
                    WonMessage createMessage = WonMessageBuilder.createAtom()
                                    .atom(atomUri1)
                                    .content().graph("#content", RdfOutput.toGraph(atomContent))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(auth))
                                    .build();
                    createMessage = ctx.getWonMessageSender().prepareMessage(createMessage);
                    ctx.getBotContextWrapper().rememberAtomUri(atomUri1);
                    final String action = "Create granting atom";
                    EventListener successCallback = event -> {
                        logger.debug("Granting atom created");
                        createMessageUri1.set(((SuccessResponseEvent) event).getOriginalMessageURI());
                        deactivate();
                    };
                    EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                    action);
                    EventBotActionUtils.makeAndSubscribeResponseListener(createMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(createMessage);
                }
            };
            BotBehaviour bbAtom2 = new BotBehaviour(ctx) {
                @Override
                protected void onActivate(Optional<Object> message) {
                    final String atomUriString = atomUri2.toString();
                    final AtomContent atomContent = AtomContent.builder(atomUriString)
                                    .addTitle("Grantee atom")
                                    .addSocket(Socket.builder(atomUriString + "#chatSocket")
                                                    .setSocketDefinition(WXCHAT.ChatSocket.asURI())
                                                    .build())
                                    .addType(URI.create(WON.Atom.getURI()))
                                    .build();
                    WonMessage createMessage = WonMessageBuilder.createAtom()
                                    .atom(atomUri2)
                                    .content().graph(RdfOutput.toGraph(atomContent))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(getNobodyMayDoNothing()))
                                    .build();
                    createMessage = ctx.getWonMessageSender().prepareMessage(createMessage);
                    ctx.getBotContextWrapper().rememberAtomUri(atomUri2);
                    final String action = "Create grantee atom";
                    EventListener successCallback = event -> {
                        logger.debug("Grantee atom created");
                        createMessageUri2.set(((SuccessResponseEvent) event).getOriginalMessageURI());
                        deactivate();
                    };
                    EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                    action);
                    EventBotActionUtils.makeAndSubscribeResponseListener(createMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(createMessage);
                }
            };
            BotBehaviour bbReplaceAtom1FromAtom2 = new BotBehaviour(ctx) {
                @Override
                protected void onActivate(Optional<Object> message) {
                    WonMessage replaceMessage = WonMessageBuilder.replace()
                                    .atom(atomUri1)
                                    .content().graph("#content", RdfOutput.toGraph(AtomContent.builder(atomUri1)
                                                    .addTitle("Replaced Title")
                                                    .addTag("A-New-Tag")
                                                    .addType(URI.create(WON.Atom.getURI()))
                                                    .addType(URI.create(WON.Persona.getURI()))
                                                    .addSocket(Socket.builder(atomUri1.toString() + "#buddySocket")
                                                                    .setSocketDefinition(WXBUDDY.BuddySocket.asURI())
                                                                    .build())
                                                    .addSocket(Socket.builder(atomUri1.toString() + "#holderSocket")
                                                                    .setSocketDefinition(WXHOLD.HolderSocket.asURI())
                                                                    .build())
                                                    .build()))
                                    .content().graph("#content2", RdfOutput.toGraph(AtomContent.builder(atomUri1)
                                                    .addTitle("New Title")
                                                    .addType(URI.create(WON.Atom.getURI()))
                                                    .addSocket(Socket.builder(atomUri1.toString() + "#chatSocket")
                                                                    .setSocketDefinition(WXCHAT.ChatSocket.asURI())
                                                                    .build())
                                                    .build()))
                                    .build();
                    String action = "replace content of atom1 using webid of atom2, adding a graph";
                    EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus, action);
                    replaceMessage = ctx.getWonMessageSender().prepareMessageOnBehalf(replaceMessage, atomUri2);
                    EventListener successCallback = makeLoggingCallback(bot, bus, action, () -> deactivate());
                    EventBotActionUtils.makeAndSubscribeResponseListener(replaceMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(replaceMessage);
                }
            };
            BotBehaviour readModified = new BotBehaviour(ctx) {
                @Override
                protected void onActivate(Optional<Object> message) {
                    Dataset atomData = ctx.getLinkedDataSource().getDataForResource(atomUri1, atomUri1);
                    Set<String> expectedGraphNames = Set.of(atomUri1 + "#acl",
                                    atomUri1 + "#acl-sig",
                                    atomUri1 + "#content",
                                    atomUri1 + "#content-sig",
                                    atomUri1 + "#content2",
                                    atomUri1 + "#content2-sig",
                                    atomUri1 + "#key",
                                    atomUri1 + "#key-sig",
                                    atomUri1 + "#sysinfo");
                    Iterator<String> it = atomData.listNames();
                    Set<String> actualNames = new HashSet<>();
                    while (it.hasNext()) {
                        actualNames.add(it.next());
                    }
                    Assert.assertEquals(expectedGraphNames, actualNames);
                    Shacl2JavaInstanceFactory instanceFactory = ContentUtils.newInstanceFactory();
                    Shacl2JavaInstanceFactory.Accessor accessor = instanceFactory
                                    .accessor(atomData.getNamedModel(atomUri1 + "#content").getGraph());
                    Optional<AtomContent> atomContent = accessor
                                    .getInstanceOfType(atomUri1.toString(), AtomContent.class);
                    assertTrue(bus, "replacing content failed: replaced title not found",
                                    atomContent.get().getTitles().contains("Replaced Title"));
                    assertFalse(bus, "replacing content failed: old title still there",
                                    atomContent.get().getTitles().contains("Granting atom"));
                    accessor = instanceFactory.accessor(atomData.getNamedModel(atomUri1 + "#content2").getGraph());
                    atomContent = accessor
                                    .getInstanceOfType(atomUri1.toString(), AtomContent.class);
                    assertTrue(bus, "adding content failed: new title not found",
                                    atomContent.get().getTitles().contains("New Title"));
                    passTest(bus);
                }
            };
            BehaviourBarrier barrier = new BehaviourBarrier(ctx);
            barrier.waitFor(bbAtom1, bbAtom2);
            barrier.thenStart(bbReplaceAtom1FromAtom2);
            barrier.activate();
            bbAtom1.activate();
            bbAtom2.activate();
            bbReplaceAtom1FromAtom2.onDeactivateActivate(readModified);
            logger.debug("Finished initializing test 'testQueryBasedMatch()'");
        });
    }

    @Test(timeout = 60 * 1000)
    public void testConnectionMessages() throws Exception {
        final AtomicReference<URI> createMessageUri1 = new AtomicReference();
        final AtomicReference<URI> createMessageUri2 = new AtomicReference();
        final AtomicReference<URI> connectionUri12 = new AtomicReference<>();
        final AtomicReference<URI> connectionUri21 = new AtomicReference<>();
        final AtomicReference<URI> connectMessageUri12 = new AtomicReference<>();
        final AtomicReference<URI> connectMessageUri21 = new AtomicReference<>();
        runTest(ctx -> {
            EventBus bus = ctx.getEventBus();
            final URI wonNodeUri = ctx.getNodeURISource().getNodeURI();
            final URI atomUri1 = ctx.getWonNodeInformationService().generateAtomURI(wonNodeUri);
            final URI atomUri2 = ctx.getWonNodeInformationService().generateAtomURI(wonNodeUri);
            final BotBehaviour bbCreateAtom1 = new BotBehaviour(ctx, "bbCreateAtom1") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    final String atomUriString = atomUri1.toString();
                    final AtomContent atomContent = AtomContent.builder(atomUriString)
                                    .addTitle("Connection initiating atom")
                                    .addSocket(Socket.builder(atomUriString + "#chatSocket")
                                                    .setSocketDefinition(WXCHAT.ChatSocket.asURI())
                                                    .build())
                                    .addType(URI.create(WON.Atom.getURI()))
                                    .build();
                    Authorization auth = Authorization.builder()
                                    .addGranteesAtomExpression(ae -> ae.addAtomsURI(atomUri2))
                                    .addGrant(ase -> ase
                                                    .addGraph(ge -> ge.addOperationsSimpleOperationExpression(OP_READ)))
                                    .build();
                    WonMessage createMessage = WonMessageBuilder.createAtom()
                                    .atom(atomUri1)
                                    .content().graph(RdfOutput.toGraph(atomContent))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(auth))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(getAnyoneMayConnectAuth()))
                                    .content()
                                    .aclGraph(won.auth.model.RdfOutput.toGraph(getConnectedMayCommunicateAuth()))
                                    .build();
                    createMessage = ctx.getWonMessageSender().prepareMessage(createMessage);
                    ctx.getBotContextWrapper().rememberAtomUri(atomUri1);
                    EventListener successCallback = event -> {
                        logger.debug("Connection initiating atom created");
                        createMessageUri1.set(((SuccessResponseEvent) event).getOriginalMessageURI());
                        deactivate();
                    };
                    EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                    "Create connection initiating atom");
                    EventBotActionUtils.makeAndSubscribeResponseListener(createMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(createMessage);
                }
            };
            // create match source
            final BotBehaviour bbCreateAtom2 = new BotBehaviour(ctx, "bbCreateAtom2") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    final String atomUriString = atomUri2.toString();
                    final AtomContent atomContent = AtomContent.builder(atomUriString)
                                    .addTitle("Grantee atom")
                                    .addSparqlQuery(
                                                    "PREFIX won:<https://w3id.org/won/core#>\n"
                                                                    + "PREFIX con:<https://w3id.org/won/content#>\n"
                                                                    + "SELECT ?result (1.0 AS ?score) WHERE {"
                                                                    + "?result a won:Atom ;"
                                                                    + "    con:tag \"tag-to-match\"."
                                                                    + "}")
                                    .addSocket(Socket.builder(atomUriString + "#chatSocket")
                                                    .setSocketDefinition(WXCHAT.ChatSocket.asURI())
                                                    .build())
                                    .addType(URI.create(WON.Atom.getURI()))
                                    .build();
                    Authorization auth = Authorization.builder()
                                    .addGranteesAtomExpression(ae -> ae.addAtomsURI(atomUri1))
                                    .addGrant(ase -> ase
                                                    .addGraph(ge -> ge.addOperationsSimpleOperationExpression(OP_READ)))
                                    .build();
                    WonMessage createMessage = WonMessageBuilder.createAtom()
                                    .atom(atomUri2)
                                    .content().graph(RdfOutput.toGraph(atomContent))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(auth))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(getAnyoneMayConnectAuth()))
                                    .content()
                                    .aclGraph(won.auth.model.RdfOutput.toGraph(getConnectedMayCommunicateAuth()))
                                    .build();
                    createMessage = ctx.getWonMessageSender().prepareMessage(createMessage);
                    ctx.getBotContextWrapper().rememberAtomUri(atomUri2);
                    EventListener successCallback = event -> {
                        logger.debug("Connection accepting atom created");
                        createMessageUri2.set(((SuccessResponseEvent) event).getOriginalMessageURI());
                        deactivate();
                    };
                    EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                    "Create connection accepting atom");
                    EventBotActionUtils.makeAndSubscribeResponseListener(createMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(createMessage);
                }
            };
            BotBehaviour bbSendConnect = new BotBehaviour(ctx, "bbSendConnect") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    WonMessage connectMessage = WonMessageBuilder.connect()
                                    .sockets()
                                    .sender(URI.create(atomUri1.toString() + "#chatSocket"))
                                    .recipient(URI.create(atomUri2.toString() + "#chatSocket"))
                                    .direction().fromOwner()
                                    .content().text("Hello there!")
                                    .build();
                    connectMessage = ctx.getWonMessageSender().prepareMessage(connectMessage);
                    connectMessageUri12.set(connectMessage.getMessageURIRequired());
                    EventListener successCallback = event -> {
                        logger.debug("Connection requested");
                        connectionUri12.set(((SuccessResponseEvent) event).getConnectionURI().get());
                        deactivate();
                    };
                    EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                    "Requesting connection");
                    EventBotActionUtils.makeAndSubscribeResponseListener(connectMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(connectMessage);
                }
            };
            BotBehaviour bbAcceptConnect = new BotBehaviour(ctx, "bbAcceptConnect") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    WonMessage connectMessage = WonMessageBuilder.connect()
                                    .sockets()
                                    .sender(URI.create(atomUri2.toString() + "#chatSocket"))
                                    .recipient(URI.create(atomUri1.toString() + "#chatSocket"))
                                    .direction().fromOwner()
                                    .content().text("Hello!")
                                    .build();
                    connectMessage = ctx.getWonMessageSender().prepareMessage(connectMessage);
                    connectMessageUri21.set(connectMessage.getMessageURIRequired());
                    EventListener successCallback = event -> {
                        logger.debug("Connection accepted");
                        connectionUri21.set(((SuccessResponseEvent) event).getConnectionURI().get());
                        deactivate();
                    };
                    EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                    "Accepting connection");
                    EventBotActionUtils.makeAndSubscribeResponseListener(connectMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(connectMessage);
                }
            };
            BotBehaviour bbSendMessage = new BotBehaviour(ctx, "bbSendMessage") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    WonMessage msg = WonMessageBuilder.connectionMessage()
                                    .sockets()
                                    .sender(URI.create(atomUri1.toString() + "#chatSocket"))
                                    .recipient(URI.create(atomUri2.toString() + "#chatSocket"))
                                    .direction().fromOwner()
                                    .content().text("Nice, this works")
                                    .build();
                    msg = ctx.getWonMessageSender().prepareMessage(msg);
                    final URI connectMessageUri = msg.getMessageURIRequired();
                    EventListener successCallback = event -> {
                        logger.debug("Message sent");
                        deactivate();
                    };
                    EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                    "Sending connection message");
                    EventBotActionUtils.makeAndSubscribeResponseListener(msg, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(msg);
                }
            };
            BotBehaviour bbTestLinkedDataAccess = new BotBehaviour(ctx, "bbTestLinkedDataAccess") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    URI connContainerUri1 = uriService.createConnectionContainerURIForAtom(atomUri1);
                    URI connContainerUri2 = uriService.createConnectionContainerURIForAtom(atomUri2);
                    boolean passed = true;
                    // both atoms allow each other read access
                    passed = passed && testLinkedDataRequestOk(ctx, bus, "test1.", atomUri1,
                                    atomUri1,
                                    atomUri2,
                                    // createMessageUri1.get(),
                                    connectionUri12.get(),
                                    connectMessageUri12.get(),
                                    connectMessageUri21.get(),
                                    connContainerUri1);
                    passed = passed && testLinkedDataRequestFails(ctx, bus, "test2.", atomUri1,
                                    LinkedDataFetchingException.class,
                                    // createMessageUri2.get(),
                                    connectionUri21.get(),
                                    connContainerUri2);
                    passed = passed && testLinkedDataRequestOk(ctx, bus, "test3.", atomUri2,
                                    atomUri2,
                                    atomUri1,
                                    // createMessageUri2.get(),
                                    connectionUri21.get(),
                                    connectMessageUri21.get(),
                                    connectMessageUri12.get(),
                                    connContainerUri2);
                    passed = passed && testLinkedDataRequestFails(ctx, bus, "test4.", atomUri2,
                                    LinkedDataFetchingException.class,
                                    // createMessageUri1.get(),
                                    connectionUri12.get(),
                                    connContainerUri1);
                    if (passed) {
                        passTest(bus);
                    }
                }
            };
            BehaviourBarrier barrier = new BehaviourBarrier(ctx);
            barrier.waitFor(bbCreateAtom1, bbCreateAtom2);
            barrier.thenStart(bbSendConnect);
            barrier.activate();
            bbSendConnect.onDeactivateActivate(bbAcceptConnect);
            bbAcceptConnect.onDeactivateActivate(bbSendMessage);
            bbSendMessage.onDeactivateActivate(bbTestLinkedDataAccess);
            bbCreateAtom1.activate();
            bbCreateAtom2.activate();
        });
    }

    @Test(timeout = 60 * 1000)
    public void testTokenExchange() throws Exception {
        final AtomicReference<URI> createMessageUri1 = new AtomicReference();
        final AtomicReference<URI> createMessageUri2 = new AtomicReference();
        final AtomicReference<URI> createMessageUri3 = new AtomicReference();
        final AtomicReference<URI> connectionUri12 = new AtomicReference<>();
        final AtomicReference<URI> connectionUri21 = new AtomicReference<>();
        final AtomicReference<URI> connectionUri23 = new AtomicReference<>();
        final AtomicReference<URI> connectionUri32 = new AtomicReference<>();
        final AtomicReference<URI> connectMessageUri12 = new AtomicReference<>();
        final AtomicReference<URI> connectMessageUri21 = new AtomicReference<>();
        final AtomicReference<URI> connectMessageUri23 = new AtomicReference<>();
        final AtomicReference<URI> connectMessageUri32 = new AtomicReference<>();
        runTest(ctx -> {
            EventBus bus = ctx.getEventBus();
            final URI wonNodeUri = ctx.getNodeURISource().getNodeURI();
            final URI atomUri1 = ctx.getWonNodeInformationService().generateAtomURI(wonNodeUri);
            final URI atomUri2 = ctx.getWonNodeInformationService().generateAtomURI(wonNodeUri);
            final URI atomUri3 = ctx.getWonNodeInformationService().generateAtomURI(wonNodeUri);
            final BotBehaviour bbCreateAtom1 = new BotBehaviour(ctx, "bbCreateAtom1") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    final String atomUriString = atomUri1.toString();
                    AtomContent atomContent = AtomContent.builder(atomUri1)
                                    .addTitle("atom 1/3")
                                    .addSocket(Socket.builder(atomUriString + "#buddySocket")
                                                    .setSocketDefinition(WXBUDDY.BuddySocket.asURI())
                                                    .build())
                                    .addType(URI.create(WON.Atom.getURI()))
                                    .build();
                    Authorization auth1 = getBuddiesAndBOfBuddiesReadAllGraphsAuth();
                    Authorization auth2 = getBuddiesReceiveBuddyTokenAuth();
                    Authorization auth3 = getAnyoneGetsAuthInfo();
                    WonMessage createMessage = WonMessageBuilder.createAtom()
                                    .atom(atomUri1)
                                    .content().graph(RdfOutput.toGraph(atomContent))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(auth1))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(auth2))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(auth3))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(getAnyoneMayConnectAuth()))
                                    .content()
                                    .aclGraph(won.auth.model.RdfOutput.toGraph(getConnectedMayCommunicateAuth()))
                                    .build();
                    createMessage = ctx.getWonMessageSender().prepareMessage(createMessage);
                    ctx.getBotContextWrapper().rememberAtomUri(atomUri1);
                    EventListener successCallback = event -> {
                        logger.debug("Connection initiating atom created");
                        createMessageUri1.set(((SuccessResponseEvent) event).getOriginalMessageURI());
                        deactivate();
                    };
                    EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                    "Create connection initiating atom");
                    EventBotActionUtils.makeAndSubscribeResponseListener(createMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(createMessage);
                }
            };
            final BotBehaviour bbCreateAtom2 = new BotBehaviour(ctx, "bbCreateAtom2") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    final String atomUriString = atomUri2.toString();
                    AtomContent atomContent = AtomContent.builder(atomUri2)
                                    .addTitle("atom 2/3")
                                    .addSocket(Socket.builder(atomUriString + "#buddySocket")
                                                    .setSocketDefinition(WXBUDDY.BuddySocket.asURI())
                                                    .build())
                                    .addType(URI.create(WON.Atom.getURI()))
                                    .build();
                    Authorization auth1 = getBuddiesAndBOfBuddiesReadAllGraphsAuth();
                    Authorization auth2 = getBuddiesReceiveBuddyTokenAuth();
                    Authorization auth3 = getAnyoneGetsAuthInfo();
                    WonMessage createMessage = WonMessageBuilder.createAtom()
                                    .atom(atomUri2)
                                    .content().graph(RdfOutput.toGraph(atomContent))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(auth1))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(auth2))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(auth3))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(getAnyoneMayConnectAuth()))
                                    .content()
                                    .aclGraph(won.auth.model.RdfOutput.toGraph(getConnectedMayCommunicateAuth()))
                                    .build();
                    createMessage = ctx.getWonMessageSender().prepareMessage(createMessage);
                    ctx.getBotContextWrapper().rememberAtomUri(atomUri2);
                    EventListener successCallback = event -> {
                        logger.debug("Connection accepting atom created");
                        createMessageUri2.set(((SuccessResponseEvent) event).getOriginalMessageURI());
                        deactivate();
                    };
                    EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                    "Create connection accepting atom");
                    EventBotActionUtils.makeAndSubscribeResponseListener(createMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(createMessage);
                }
            };
            final BotBehaviour bbCreateAtom3 = new BotBehaviour(ctx, "bbCreateAtom3") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    final String atomUriString = atomUri3.toString();
                    AtomContent atomContent = AtomContent.builder(atomUri3)
                                    .addTitle("atom 3/3")
                                    .addSocket(Socket.builder(atomUriString + "#buddySocket")
                                                    .setSocketDefinition(WXBUDDY.BuddySocket.asURI())
                                                    .build())
                                    .addType(URI.create(WON.Atom.getURI()))
                                    .build();
                    Authorization auth1 = getBuddiesAndBOfBuddiesReadAllGraphsAuth();
                    Authorization auth2 = getBuddiesReceiveBuddyTokenAuth();
                    Authorization auth3 = getAnyoneGetsAuthInfo();
                    WonMessage createMessage = WonMessageBuilder.createAtom()
                                    .atom(atomUri3)
                                    .content().graph(RdfOutput.toGraph(atomContent))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(auth1))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(auth2))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(auth3))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(getAnyoneMayConnectAuth()))
                                    .content()
                                    .aclGraph(won.auth.model.RdfOutput.toGraph(getConnectedMayCommunicateAuth()))
                                    .build();
                    createMessage = ctx.getWonMessageSender().prepareMessage(createMessage);
                    ctx.getBotContextWrapper().rememberAtomUri(atomUri3);
                    EventListener successCallback = event -> {
                        logger.debug("Connection accepting atom created");
                        createMessageUri3.set(((SuccessResponseEvent) event).getOriginalMessageURI());
                        deactivate();
                    };
                    EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                    "Create connection accepting atom");
                    EventBotActionUtils.makeAndSubscribeResponseListener(createMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(createMessage);
                }
            };
            BotBehaviour bbSendConnect12 = new BotBehaviour(ctx, "bbSendConnect12") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    WonMessage connectMessage = WonMessageBuilder.connect()
                                    .sockets()
                                    .sender(URI.create(atomUri1.toString() + "#buddySocket"))
                                    .recipient(URI.create(atomUri2.toString() + "#buddySocket"))
                                    .direction().fromOwner()
                                    .content().text("Hello there!")
                                    .build();
                    connectMessage = ctx.getWonMessageSender().prepareMessage(connectMessage);
                    connectMessageUri12.set(connectMessage.getMessageURIRequired());
                    EventListener successCallback = event -> {
                        logger.debug("Connection requested");
                        connectionUri12.set(((SuccessResponseEvent) event).getConnectionURI().get());
                        deactivate();
                    };
                    EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                    "Requesting connection");
                    EventBotActionUtils.makeAndSubscribeResponseListener(connectMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(connectMessage);
                }
            };
            BotBehaviour bbAcceptConnect21 = new BotBehaviour(ctx, "bbAcceptConnect") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    WonMessage connectMessage = WonMessageBuilder.connect()
                                    .sockets()
                                    .sender(URI.create(atomUri2.toString() + "#buddySocket"))
                                    .recipient(URI.create(atomUri1.toString() + "#buddySocket"))
                                    .direction().fromOwner()
                                    .content().text("Hello!")
                                    .build();
                    connectMessage = ctx.getWonMessageSender().prepareMessage(connectMessage);
                    connectMessageUri21.set(connectMessage.getMessageURIRequired());
                    EventListener successCallback = event -> {
                        logger.debug("Connection accepted");
                        connectionUri21.set(((SuccessResponseEvent) event).getConnectionURI().get());
                        deactivate();
                    };
                    EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                    "Accepting connection");
                    EventBotActionUtils.makeAndSubscribeResponseListener(connectMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(connectMessage);
                }
            };
            BotBehaviour bbSendConnect32 = new BotBehaviour(ctx, "bbSendConnect32") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    WonMessage connectMessage = WonMessageBuilder.connect()
                                    .sockets()
                                    .sender(URI.create(atomUri3.toString() + "#buddySocket"))
                                    .recipient(URI.create(atomUri2.toString() + "#buddySocket"))
                                    .direction().fromOwner()
                                    .content().text("Hello there!")
                                    .build();
                    connectMessage = ctx.getWonMessageSender().prepareMessage(connectMessage);
                    connectMessageUri32.set(connectMessage.getMessageURIRequired());
                    EventListener successCallback = event -> {
                        logger.debug("Connection requested");
                        connectionUri32.set(((SuccessResponseEvent) event).getConnectionURI().get());
                        deactivate();
                    };
                    EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                    "Requesting connection");
                    EventBotActionUtils.makeAndSubscribeResponseListener(connectMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(connectMessage);
                }
            };
            BotBehaviour bbAcceptConnect23 = new BotBehaviour(ctx, "bbAcceptConnect23") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    WonMessage connectMessage = WonMessageBuilder.connect()
                                    .sockets()
                                    .sender(URI.create(atomUri2.toString() + "#buddySocket"))
                                    .recipient(URI.create(atomUri3.toString() + "#buddySocket"))
                                    .direction().fromOwner()
                                    .content().text("Hello!")
                                    .build();
                    connectMessage = ctx.getWonMessageSender().prepareMessage(connectMessage);
                    connectMessageUri23.set(connectMessage.getMessageURIRequired());
                    EventListener successCallback = event -> {
                        logger.debug("Connection accepted");
                        connectionUri23.set(((SuccessResponseEvent) event).getConnectionURI().get());
                        deactivate();
                    };
                    EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                    "Accepting connection");
                    EventBotActionUtils.makeAndSubscribeResponseListener(connectMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(connectMessage);
                }
            };
            BotBehaviour bbTestLinkedDataAccess = new BotBehaviour(ctx, "bbTestLinkedDataAccess") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    URI connContainerUri1 = uriService.createConnectionContainerURIForAtom(atomUri1);
                    URI connContainerUri2 = uriService.createConnectionContainerURIForAtom(atomUri2);
                    URI connContainerUri3 = uriService.createConnectionContainerURIForAtom(atomUri3);
                    boolean passed = true;
                    // both atoms allow each other read access
                    passed = passed && testLinkedDataRequestOk(ctx, bus, "test1.", atomUri1,
                                    atomUri1,
                                    atomUri2,
                                    // createMessageUri1.get(),
                                    connContainerUri1,
                                    connectionUri12.get(),
                                    connectMessageUri12.get(),
                                    connectMessageUri21.get(),
                                    connectionUri21.get(),
                                    connContainerUri2);
                    passed = passed && testLinkedDataRequestFails(ctx, bus, "test2.", atomUri1,
                                    LinkedDataFetchingException.class,
                                    // createMessageUri2.get(),
                                    atomUri3,
                                    connContainerUri3);
                    passed = passed && testLinkedDataRequestOk(ctx, bus, "test3.", atomUri2,
                                    atomUri2,
                                    atomUri1,
                                    // createMessageUri2.get(),
                                    connContainerUri2,
                                    connectionUri21.get(),
                                    connectMessageUri21.get(),
                                    connectMessageUri12.get(),
                                    connectionUri12.get(),
                                    connContainerUri3,
                                    connContainerUri1,
                                    connectionUri32.get(),
                                    atomUri3);
                    passed = passed && testLinkedDataRequestFails(ctx, bus, "test4.", atomUri2,
                                    LinkedDataFetchingException.class,
                                    createMessageUri1.get(),
                                    createMessageUri3.get());
                    deactivate();
                }
            };
            BotBehaviour bbRequestBuddyToken = new BotBehaviour(ctx) {
                @Override
                protected void onActivate(Optional<Object> message) {
                    testLinkedDataRequest(ctx, bus, LinkedDataFetchingException.ForbiddenAuthMethodProvided.class,
                                    false, null, null, atomUri3,
                                    "test 5.1 - read atom3");
                    URI tokenrequest = uriService
                                    .createTokenRequestURIForAtomURIWithScopes(atomUri2,
                                                    WXBUDDY.BuddySocket.asString());
                    testTokenRequest(ctx, bus, IllegalArgumentException.class, false, null, null, tokenrequest,
                                    "test5.2 - request token");
                    Set<String> resultingTokens = new HashSet();
                    testTokenRequest(ctx, bus, null, false, atomUri1, null, tokenrequest,
                                    "test5.3 - request buddy socket token from atom2 using webID atom1",
                                    resultingTokens);
                    String token = resultingTokens.stream().findFirst().get();
                    testLinkedDataRequest(ctx, bus, null, false, null, token, atomUri3,
                                    "test 5.4- read atom3 with token issued for atom 1");
                    testLinkedDataRequest(ctx, bus, null, false, null, token, atomUri2,
                                    "test 5.5 - read atom2 with token issued for atom 1");
                    deactivate();
                }
            };
            BotBehaviour bbTestLinkedDataAccessWithToken = new BotBehaviour(ctx) {
                @Override
                protected void onActivate(Optional<Object> message) {
                    passTest(bus);
                }
            };
            BehaviourBarrier barrier = new BehaviourBarrier(ctx);
            barrier.waitFor(bbCreateAtom1, bbCreateAtom2, bbCreateAtom3);
            barrier.thenStart(bbSendConnect12);
            barrier.activate();
            bbSendConnect12.onDeactivateActivate(bbAcceptConnect21);
            bbAcceptConnect21.onDeactivateActivate(bbSendConnect32);
            bbSendConnect32.onDeactivateActivate(bbAcceptConnect23);
            bbAcceptConnect23.onDeactivateActivate(bbTestLinkedDataAccess);
            bbTestLinkedDataAccess.onDeactivateActivate(bbRequestBuddyToken);
            bbRequestBuddyToken.onDeactivateActivate(bbTestLinkedDataAccessWithToken);
            bbCreateAtom1.activate();
            bbCreateAtom2.activate();
            bbCreateAtom3.activate();
        });
    }

    @Test(timeout = 60 * 1000)
    public void testAutoConnectByAcl() throws Exception {
        runTest(ctx -> {
            EventBus bus = ctx.getEventBus();
            final URI wonNodeUri = ctx.getNodeURISource().getNodeURI();
            final URI atomUri1 = ctx.getWonNodeInformationService().generateAtomURI(wonNodeUri);
            final URI atomUri2 = ctx.getWonNodeInformationService().generateAtomURI(wonNodeUri);
            final BotBehaviour bbCreateAtom1 = new BotBehaviour(ctx, "bbCreateAtom1") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    final String atomUriString = atomUri1.toString();
                    final AtomContent atomContent = AtomContent.builder(atomUriString)
                                    .addTitle("Autoconnecting atom")
                                    .addSocket(Socket.builder(atomUriString + "#chatSocket")
                                                    .setSocketDefinition(WXCHAT.ChatSocket.asURI())
                                                    .build())
                                    .addType(URI.create(WON.Atom.getURI()))
                                    .build();
                    Authorization auth = Authorization.builder()
                                    .addGranteesAtomExpression(ae -> ae.addAtomsURI(atomUri2))
                                    .addGrant(ase -> ase
                                                    .addGraph(ge -> ge.addOperationsSimpleOperationExpression(OP_READ))
                                                    .addSocket(s -> s.addSocketType(WXCHAT.ChatSocket.asURI()))
                                                    .addOperationsSimpleOperationExpression(OP_AUTO_CONNECT))
                                    .build();
                    WonMessage createMessage = WonMessageBuilder.createAtom()
                                    .atom(atomUri1)
                                    .content().graph(RdfOutput.toGraph(atomContent))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(auth))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(getAnyoneMayConnectAuth()))
                                    .content()
                                    .aclGraph(won.auth.model.RdfOutput.toGraph(getConnectedMayCommunicateAuth()))
                                    .build();
                    createMessage = ctx.getWonMessageSender().prepareMessage(createMessage);
                    ctx.getBotContextWrapper().rememberAtomUri(atomUri1);
                    EventListener successCallback = event -> {
                        logger.debug("Connection autoconnecting atom created");
                        deactivate();
                    };
                    EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                    "Create autoconnecting atom");
                    EventBotActionUtils.makeAndSubscribeResponseListener(createMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(createMessage);
                }
            };
            // create match source
            final BotBehaviour bbCreateAtom2 = new BotBehaviour(ctx, "bbCreateAtom2") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    final String atomUriString = atomUri2.toString();
                    final AtomContent atomContent = AtomContent.builder(atomUriString)
                                    .addTitle("Connecting atom")
                                    .addSocket(Socket.builder(atomUriString + "#chatSocket")
                                                    .setSocketDefinition(WXCHAT.ChatSocket.asURI())
                                                    .build())
                                    .addType(URI.create(WON.Atom.getURI()))
                                    .build();
                    Authorization auth = Authorization.builder()
                                    .addGranteesAtomExpression(ae -> ae.addAtomsURI(atomUri1))
                                    .addGrant(ase -> ase
                                                    .addGraph(ge -> ge.addOperationsSimpleOperationExpression(OP_READ)))
                                    .build();
                    WonMessage createMessage = WonMessageBuilder.createAtom()
                                    .atom(atomUri2)
                                    .content().graph(RdfOutput.toGraph(atomContent))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(auth))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(getAnyoneMayConnectAuth()))
                                    .content()
                                    .aclGraph(won.auth.model.RdfOutput.toGraph(getConnectedMayCommunicateAuth()))
                                    .build();
                    createMessage = ctx.getWonMessageSender().prepareMessage(createMessage);
                    ctx.getBotContextWrapper().rememberAtomUri(atomUri2);
                    EventListener successCallback = event -> {
                        logger.debug("Connection initiating atom created");
                        deactivate();
                    };
                    EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                    "Create connection initiating  atom");
                    EventBotActionUtils.makeAndSubscribeResponseListener(createMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(createMessage);
                }
            };
            BotBehaviour bbSendConnect = new BotBehaviour(ctx, "bbSendConnect") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    WonMessage connectMessage = WonMessageBuilder.connect()
                                    .sockets()
                                    .sender(URI.create(atomUri2.toString() + "#chatSocket"))
                                    .recipient(URI.create(atomUri1.toString() + "#chatSocket"))
                                    .direction().fromOwner()
                                    .content().text("Hello there!")
                                    .build();
                    connectMessage = ctx.getWonMessageSender().prepareMessage(connectMessage);
                    EventListener successCallback = event -> {
                        logger.debug("Connection requested");
                        deactivate();
                    };
                    EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                    "Requesting connection");
                    EventBotActionUtils.makeAndSubscribeResponseListener(connectMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(connectMessage);
                }
            };
            BotBehaviour bbWaitForConnectFromAtom1 = new BotBehaviour(ctx, "bbWaitForConnectFromAtom1") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    bus.subscribe(ConnectFromOtherAtomEvent.class,
                                    new EventFilter() {
                                        @Override
                                        public boolean accept(Event event) {
                                            return event instanceof ConnectFromOtherAtomEvent
                                                            && ((ConnectFromOtherAtomEvent) event)
                                                                            .getSenderSocket()
                                                                            .equals(URI.create(atomUri1.toString()
                                                                                            + "#chatSocket"));
                                        }
                                    },
                                    new BaseEventBotAction(ctx) {
                                        @Override
                                        protected void doRun(Event event, EventListener executingListener)
                                                        throws Exception {
                                            passTest(bus);
                                        }
                                    });
                }
            };
            BehaviourBarrier barrier = new BehaviourBarrier(ctx);
            barrier.waitFor(bbCreateAtom1, bbCreateAtom2);
            barrier.thenStart(bbSendConnect);
            barrier.activate();
            bbCreateAtom1.activate();
            bbCreateAtom2.activate();
            bbWaitForConnectFromAtom1.activate();
        });
    }

    @Test(timeout = 60 * 1000)
    public void testSocketCapacity_nonConcurrentAccepts() throws Exception {
        runTest(ctx -> {
            EventBus bus = ctx.getEventBus();
            final URI wonNodeUri = ctx.getNodeURISource().getNodeURI();
            final URI atomUri1 = ctx.getWonNodeInformationService().generateAtomURI(wonNodeUri);
            final URI atomUri2 = ctx.getWonNodeInformationService().generateAtomURI(wonNodeUri);
            final URI atomUri3 = ctx.getWonNodeInformationService().generateAtomURI(wonNodeUri);
            final BotBehaviour bbCreateAtom1 = new BotBehaviour(ctx, "bbCreateAtom1") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    final String atomUriString = atomUri1.toString();
                    AtomContent atomContent = AtomContent.builder(atomUri1)
                                    .addTitle("Holdable atom (1/3)")
                                    .addSocket(Socket.builder(atomUriString + "#holdableSocket")
                                                    .setSocketDefinition(WXHOLD.HoldableSocket.asURI())
                                                    .build())
                                    .addType(URI.create(WON.Atom.getURI()))
                                    .build();
                    WonMessage createMessage = WonMessageBuilder.createAtom()
                                    .atom(atomUri1)
                                    .content().graph(RdfOutput.toGraph(atomContent))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(getAnyoneMayConnectAuth()))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(getAnyoneMayReadAnything()))
                                    .build();
                    createMessage = ctx.getWonMessageSender().prepareMessage(createMessage);
                    ctx.getBotContextWrapper().rememberAtomUri(atomUri1);
                    EventListener successCallback = event -> {
                        logger.debug("holdable atom created");
                        deactivate();
                    };
                    EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                    "Create holdable atom");
                    EventBotActionUtils.makeAndSubscribeResponseListener(createMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(createMessage);
                }
            };
            final BotBehaviour bbCreateAtom2 = new BotBehaviour(ctx, "bbCreateAtom2") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    final String atomUriString = atomUri2.toString();
                    AtomContent atomContent = AtomContent.builder(atomUri2)
                                    .addTitle("holder atom A (2/3)")
                                    .addSocket(Socket.builder(atomUriString + "#holderSocket")
                                                    .setSocketDefinition(WXHOLD.HolderSocket.asURI())
                                                    .build())
                                    .addType(URI.create(WON.Atom.getURI()))
                                    .build();
                    WonMessage createMessage = WonMessageBuilder.createAtom()
                                    .atom(atomUri2)
                                    .content().graph(RdfOutput.toGraph(atomContent))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(getAnyoneMayConnectAuth()))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(getAnyoneMayReadAnything()))
                                    .build();
                    createMessage = ctx.getWonMessageSender().prepareMessage(createMessage);
                    ctx.getBotContextWrapper().rememberAtomUri(atomUri2);
                    EventListener successCallback = event -> {
                        logger.debug("holder atom A created");
                        deactivate();
                    };
                    EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                    "Create holder atom A");
                    EventBotActionUtils.makeAndSubscribeResponseListener(createMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(createMessage);
                }
            };
            final BotBehaviour bbCreateAtom3 = new BotBehaviour(ctx, "bbCreateAtom3") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    final String atomUriString = atomUri3.toString();
                    AtomContent atomContent = AtomContent.builder(atomUri3)
                                    .addTitle("holder atom B (3/3)")
                                    .addSocket(Socket.builder(atomUriString + "#holderSocket")
                                                    .setSocketDefinition(WXHOLD.HolderSocket.asURI())
                                                    .build())
                                    .addType(URI.create(WON.Atom.getURI()))
                                    .build();
                    WonMessage createMessage = WonMessageBuilder.createAtom()
                                    .atom(atomUri3)
                                    .content().graph(RdfOutput.toGraph(atomContent))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(getAnyoneMayConnectAuth()))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(getAnyoneMayReadAnything()))
                                    .build();
                    createMessage = ctx.getWonMessageSender().prepareMessage(createMessage);
                    ctx.getBotContextWrapper().rememberAtomUri(atomUri3);
                    EventListener successCallback = event -> {
                        logger.debug("holder atom B created");
                        deactivate();
                    };
                    EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                    "Creating holder atom B");
                    EventBotActionUtils.makeAndSubscribeResponseListener(createMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(createMessage);
                }
            };
            BotBehaviour bbSendConnect21 = new BotBehaviour(ctx, "bbSendConnect21") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    WonMessage connectMessage = WonMessageBuilder.connect()
                                    .sockets()
                                    .sender(URI.create(atomUri2.toString() + "#holderSocket"))
                                    .recipient(URI.create(atomUri1.toString() + "#holdableSocket"))
                                    .direction().fromOwner()
                                    .build();
                    connectMessage = ctx.getWonMessageSender().prepareMessage(connectMessage);
                    EventListener successCallback = event -> {
                        logger.debug("holder A -> holdable connection requested");
                        deactivate();
                    };
                    EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                    "Requesting connection holder A -> holdable");
                    EventBotActionUtils.makeAndSubscribeResponseListener(connectMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(connectMessage);
                }
            };
            BotBehaviour bbAcceptConnect12 = new BotBehaviour(ctx, "bbAcceptConnect12") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    WonMessage connectMessage = WonMessageBuilder.connect()
                                    .sockets()
                                    .sender(URI.create(atomUri1.toString() + "#holdableSocket"))
                                    .recipient(URI.create(atomUri2.toString() + "#holderSocket"))
                                    .direction().fromOwner()
                                    .build();
                    connectMessage = ctx.getWonMessageSender().prepareMessage(connectMessage);
                    EventListener successCallback = event -> {
                        logger.debug("Holder A <-> holdable connection accepted");
                        deactivate();
                    };
                    EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                    "Accepting connection holder A <-> holdable");
                    EventBotActionUtils.makeAndSubscribeResponseListener(connectMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(connectMessage);
                }
            };
            BotBehaviour bbSendConnect31 = new BotBehaviour(ctx, "bbSendConnect31") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    WonMessage connectMessage = WonMessageBuilder.connect()
                                    .sockets()
                                    .sender(URI.create(atomUri3.toString() + "#holderSocket"))
                                    .recipient(URI.create(atomUri1.toString() + "#holdableSocket"))
                                    .direction().fromOwner()
                                    .build();
                    connectMessage = ctx.getWonMessageSender().prepareMessage(connectMessage);
                    EventListener successCallback = event -> {
                        logger.debug("holder B -> holdable connection requested");
                        deactivate();
                    };
                    EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                    "Requesting connection holder B -> holdable");
                    EventBotActionUtils.makeAndSubscribeResponseListener(connectMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(connectMessage);
                }
            };
            BotBehaviour bbAcceptConnect13 = new BotBehaviour(ctx, "bbAcceptConnect") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    WonMessage connectMessage = WonMessageBuilder.connect()
                                    .sockets()
                                    .sender(URI.create(atomUri1.toString() + "#holdableSocket"))
                                    .recipient(URI.create(atomUri3.toString() + "#holderSocket"))
                                    .direction().fromOwner()
                                    .build();
                    connectMessage = ctx.getWonMessageSender().prepareMessage(connectMessage);
                    EventListener successCallback = event -> {
                        logger.debug("Unexpected: Holder B <-> holdable connection accepted");
                        failTest(bus, String.format("Unexpectedly, holdable %s accepted connection to another holder",
                                        atomUri1.toString()));
                    };
                    EventListener failureCallback = event -> {
                        logger.debug("As expected, accepting second holder conneciton failed");
                        passTest(bus);
                    };
                    EventBotActionUtils.makeAndSubscribeResponseListener(connectMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(connectMessage);
                }
            };
            BotBehaviour bbPassTest = new BotBehaviour(ctx) {
                @Override
                protected void onActivate(Optional<Object> message) {
                    passTest(bus);
                }
            };
            BehaviourBarrier barrier = new BehaviourBarrier(ctx);
            barrier.waitFor(bbCreateAtom1, bbCreateAtom2, bbCreateAtom3);
            barrier.thenStart(bbSendConnect21);
            barrier.activate();
            bbSendConnect21.onDeactivateActivate(bbAcceptConnect12);
            bbAcceptConnect12.onDeactivateActivate(bbSendConnect31);
            bbSendConnect31.onDeactivateActivate(bbAcceptConnect13);
            bbCreateAtom1.activate();
            bbCreateAtom2.activate();
            bbCreateAtom3.activate();
        });
    }

    @Test(timeout = 60 * 1000)
    public void testSocketCapacity_concurrentAccepts() throws Exception {
        runTest(ctx -> {
            EventBus bus = ctx.getEventBus();
            final URI wonNodeUri = ctx.getNodeURISource().getNodeURI();
            final URI atomUri1 = ctx.getWonNodeInformationService().generateAtomURI(wonNodeUri);
            final URI atomUri2 = ctx.getWonNodeInformationService().generateAtomURI(wonNodeUri);
            final URI atomUri3 = ctx.getWonNodeInformationService().generateAtomURI(wonNodeUri);
            final BotBehaviour bbCreateAtom1 = new BotBehaviour(ctx, "bbCreateAtom1") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    final String atomUriString = atomUri1.toString();
                    AtomContent atomContent = AtomContent.builder(atomUri1)
                                    .addTitle("Holdable atom (1/3)")
                                    .addSocket(Socket.builder(atomUriString + "#holdableSocket")
                                                    .setSocketDefinition(WXHOLD.HoldableSocket.asURI())
                                                    .build())
                                    .addType(URI.create(WON.Atom.getURI()))
                                    .build();
                    WonMessage createMessage = WonMessageBuilder.createAtom()
                                    .atom(atomUri1)
                                    .content().graph(RdfOutput.toGraph(atomContent))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(getAnyoneMayConnectAuth()))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(getAnyoneMayReadAnything()))
                                    .build();
                    createMessage = ctx.getWonMessageSender().prepareMessage(createMessage);
                    ctx.getBotContextWrapper().rememberAtomUri(atomUri1);
                    EventListener successCallback = event -> {
                        logger.debug("holdable atom created");
                        deactivate();
                    };
                    EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                    "Create holdable atom");
                    EventBotActionUtils.makeAndSubscribeResponseListener(createMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(createMessage);
                }
            };
            final BotBehaviour bbCreateAtom2 = new BotBehaviour(ctx, "bbCreateAtom2") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    final String atomUriString = atomUri2.toString();
                    AtomContent atomContent = AtomContent.builder(atomUri2)
                                    .addTitle("holder atom A (2/3)")
                                    .addSocket(Socket.builder(atomUriString + "#holderSocket")
                                                    .setSocketDefinition(WXHOLD.HolderSocket.asURI())
                                                    .build())
                                    .addType(URI.create(WON.Atom.getURI()))
                                    .build();
                    WonMessage createMessage = WonMessageBuilder.createAtom()
                                    .atom(atomUri2)
                                    .content().graph(RdfOutput.toGraph(atomContent))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(getAnyoneMayConnectAuth()))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(getAnyoneMayReadAnything()))
                                    .build();
                    createMessage = ctx.getWonMessageSender().prepareMessage(createMessage);
                    ctx.getBotContextWrapper().rememberAtomUri(atomUri2);
                    EventListener successCallback = event -> {
                        logger.debug("holder atom A created");
                        deactivate();
                    };
                    EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                    "Create holder atom A");
                    EventBotActionUtils.makeAndSubscribeResponseListener(createMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(createMessage);
                }
            };
            final BotBehaviour bbCreateAtom3 = new BotBehaviour(ctx, "bbCreateAtom3") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    final String atomUriString = atomUri3.toString();
                    AtomContent atomContent = AtomContent.builder(atomUri3)
                                    .addTitle("holder atom B (3/3)")
                                    .addSocket(Socket.builder(atomUriString + "#holderSocket")
                                                    .setSocketDefinition(WXHOLD.HolderSocket.asURI())
                                                    .build())
                                    .addType(URI.create(WON.Atom.getURI()))
                                    .build();
                    WonMessage createMessage = WonMessageBuilder.createAtom()
                                    .atom(atomUri3)
                                    .content().graph(RdfOutput.toGraph(atomContent))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(getAnyoneMayConnectAuth()))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(getAnyoneMayReadAnything()))
                                    .build();
                    createMessage = ctx.getWonMessageSender().prepareMessage(createMessage);
                    ctx.getBotContextWrapper().rememberAtomUri(atomUri3);
                    EventListener successCallback = event -> {
                        logger.debug("holder atom B created");
                        deactivate();
                    };
                    EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                    "Creating holder atom B");
                    EventBotActionUtils.makeAndSubscribeResponseListener(createMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(createMessage);
                }
            };
            BotBehaviour bbSendConnect21 = new BotBehaviour(ctx, "bbSendConnect21") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    WonMessage connectMessage = WonMessageBuilder.connect()
                                    .sockets()
                                    .sender(URI.create(atomUri2.toString() + "#holderSocket"))
                                    .recipient(URI.create(atomUri1.toString() + "#holdableSocket"))
                                    .direction().fromOwner()
                                    .build();
                    connectMessage = ctx.getWonMessageSender().prepareMessage(connectMessage);
                    EventListener successCallback = event -> {
                        logger.debug("holder A -> holdable connection requested");
                        deactivate();
                    };
                    EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                    "Requesting connection holder A -> holdable");
                    EventBotActionUtils.makeAndSubscribeResponseListener(connectMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(connectMessage);
                }
            };
            BotBehaviour bbAcceptConnect12 = new BotBehaviour(ctx, "bbAcceptConnect12") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    WonMessage connectMessage = WonMessageBuilder.connect()
                                    .sockets()
                                    .sender(URI.create(atomUri1.toString() + "#holdableSocket"))
                                    .recipient(URI.create(atomUri2.toString() + "#holderSocket"))
                                    .direction().fromOwner()
                                    .build();
                    connectMessage = ctx.getWonMessageSender().prepareMessage(connectMessage);
                    EventListener successCallback = event -> {
                        logger.debug("Holder A <-> holdable connection accepted");
                        deactivate();
                    };
                    EventListener failureCallback = event -> {
                        logger.debug("As expected, accepting one of the holder connections failed");
                        passTest(bus);
                    };
                    EventBotActionUtils.makeAndSubscribeResponseListener(connectMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(connectMessage);
                }
            };
            BotBehaviour bbSendConnect31 = new BotBehaviour(ctx, "bbSendConnect31") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    WonMessage connectMessage = WonMessageBuilder.connect()
                                    .sockets()
                                    .sender(URI.create(atomUri3.toString() + "#holderSocket"))
                                    .recipient(URI.create(atomUri1.toString() + "#holdableSocket"))
                                    .direction().fromOwner()
                                    .build();
                    connectMessage = ctx.getWonMessageSender().prepareMessage(connectMessage);
                    EventListener successCallback = event -> {
                        logger.debug("holder B -> holdable connection requested");
                        deactivate();
                    };
                    EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                    "Requesting connection holder B -> holdable");
                    EventBotActionUtils.makeAndSubscribeResponseListener(connectMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(connectMessage);
                }
            };
            BotBehaviour bbAcceptConnect13 = new BotBehaviour(ctx, "bbAcceptConnect") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    WonMessage connectMessage = WonMessageBuilder.connect()
                                    .sockets()
                                    .sender(URI.create(atomUri1.toString() + "#holdableSocket"))
                                    .recipient(URI.create(atomUri3.toString() + "#holderSocket"))
                                    .direction().fromOwner()
                                    .build();
                    connectMessage = ctx.getWonMessageSender().prepareMessage(connectMessage);
                    EventListener successCallback = event -> {
                        logger.debug("Holder B <-> holdable connection accepted");
                        deactivate();
                    };
                    EventListener failureCallback = event -> {
                        logger.debug("As expected, accepting one of the holder connections failed");
                        passTest(bus);
                    };
                    EventBotActionUtils.makeAndSubscribeResponseListener(connectMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(connectMessage);
                }
            };
            BotBehaviour bbFailTest = new BotBehaviour(ctx) {
                @Override
                protected void onActivate(Optional<Object> message) {
                    failTest(bus, String.format("Unexpectedly, holdable %s accepted two holders.", atomUri1));
                }
            };
            BehaviourBarrier barrier = new BehaviourBarrier(ctx);
            barrier.waitFor(bbCreateAtom1, bbCreateAtom2, bbCreateAtom3);
            barrier.thenStart(bbSendConnect21, bbSendConnect31);
            barrier.activate();
            BehaviourBarrier barrier2 = new BehaviourBarrier(ctx);
            barrier2.waitFor(bbSendConnect21, bbSendConnect31);
            barrier2.thenStart(bbAcceptConnect12, bbAcceptConnect13);
            barrier2.activate();
            BehaviourBarrier barrier3 = new BehaviourBarrier(ctx);
            barrier3.waitFor(bbAcceptConnect12, bbAcceptConnect13);
            barrier3.thenStart(bbFailTest);
            barrier3.activate();
            bbCreateAtom1.activate();
            bbCreateAtom2.activate();
            bbCreateAtom3.activate();
        });
    }

    @Test(timeout = 60 * 1000)
    public void testGroupChat() throws Exception {
        runTest(ctx -> {
            EventBus bus = ctx.getEventBus();
            final URI wonNodeUri = ctx.getNodeURISource().getNodeURI();
            final URI atomUri1 = ctx.getWonNodeInformationService().generateAtomURI(wonNodeUri);
            final URI atomUri2 = ctx.getWonNodeInformationService().generateAtomURI(wonNodeUri);
            final URI atomUri3 = ctx.getWonNodeInformationService().generateAtomURI(wonNodeUri);
            final URI atomUri4 = ctx.getWonNodeInformationService().generateAtomURI(wonNodeUri);
            final URI atom1GroupSocket = URI.create(atomUri1.toString() + "#groupSocket");
            final BotBehaviour bbCreateAtom1 = new BotBehaviour(ctx, "bbCreateAtom1") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    final String atomUriString = atomUri1.toString();
                    AtomContent atomContent = AtomContent.builder(atomUri1)
                                    .addTitle("Group Chat Atom (1/4)")
                                    .addSocket(Socket.builder(atom1GroupSocket)
                                                    .setSocketDefinition(WXGROUP.GroupSocket.asURI())
                                                    .build())
                                    .addType(URI.create(WON.Atom.getURI()))
                                    .build();
                    Authorization autoConnectGroupSocket = Authorization.builder()
                                    .setGranteeGranteeWildcard(GranteeWildcard.ANYONE)
                                    .addGrant(b -> b.addSocket(
                                                    s -> s.addSocketType(WXGROUP.GroupSocket.asURI())
                                                                    .addOperationsSimpleOperationExpression(
                                                                                    OP_AUTO_CONNECT)
                                    ))
                                    .build();
                    WonMessage createMessage = WonMessageBuilder.createAtom()
                                    .atom(atomUri1)
                                    .content().graph(RdfOutput.toGraph(atomContent))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(getAnyoneMayConnectAuth()))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(getAnyoneMayReadAnything()))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(autoConnectGroupSocket))
                                    .build();
                    createMessage = ctx.getWonMessageSender().prepareMessage(createMessage);
                    ctx.getBotContextWrapper().rememberAtomUri(atomUri1);
                    EventListener successCallback = event -> {
                        logger.debug("group chat atom created");
                        deactivate();
                    };
                    EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                    "Create group chat atom");
                    EventBotActionUtils.makeAndSubscribeResponseListener(createMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(createMessage);
                }
            };
            final BotBehaviour bbCreateAtom2 = new BotBehaviour(ctx, "bbCreateAtom2") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    final String atomUriString = atomUri2.toString();
                    AtomContent atomContent = AtomContent.builder(atomUri2)
                                    .addTitle("member atom A (2/4)")
                                    .addSocket(Socket.builder(atomUriString + "#chatSocket")
                                                    .setSocketDefinition(WXCHAT.ChatSocket.asURI())
                                                    .build())
                                    .addType(URI.create(WON.Atom.getURI()))
                                    .build();
                    WonMessage createMessage = WonMessageBuilder.createAtom()
                                    .atom(atomUri2)
                                    .content().graph(RdfOutput.toGraph(atomContent))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(getAnyoneMayConnectAuth()))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(getAnyoneMayReadAnything()))
                                    .build();
                    createMessage = ctx.getWonMessageSender().prepareMessage(createMessage);
                    ctx.getBotContextWrapper().rememberAtomUri(atomUri2);
                    EventListener successCallback = event -> {
                        logger.debug("member atom A created");
                        deactivate();
                    };
                    EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                    "Create member atom A");
                    EventBotActionUtils.makeAndSubscribeResponseListener(createMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(createMessage);
                }
            };
            final BotBehaviour bbCreateAtom3 = new BotBehaviour(ctx, "bbCreateAtom3") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    final String atomUriString = atomUri3.toString();
                    AtomContent atomContent = AtomContent.builder(atomUri3)
                                    .addTitle("member atom B (3/4)")
                                    .addSocket(Socket.builder(atomUriString + "#chatSocket")
                                                    .setSocketDefinition(WXCHAT.ChatSocket.asURI())
                                                    .build())
                                    .addType(URI.create(WON.Atom.getURI()))
                                    .build();
                    WonMessage createMessage = WonMessageBuilder.createAtom()
                                    .atom(atomUri3)
                                    .content().graph(RdfOutput.toGraph(atomContent))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(getAnyoneMayConnectAuth()))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(getAnyoneMayReadAnything()))
                                    .build();
                    createMessage = ctx.getWonMessageSender().prepareMessage(createMessage);
                    ctx.getBotContextWrapper().rememberAtomUri(atomUri3);
                    EventListener successCallback = event -> {
                        logger.debug("member atom B created");
                        deactivate();
                    };
                    EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                    "Creating member atom B");
                    EventBotActionUtils.makeAndSubscribeResponseListener(createMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(createMessage);
                }
            };
            final BotBehaviour bbCreateAtom4 = new BotBehaviour(ctx, "bbCreateAtom4") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    final String atomUriString = atomUri4.toString();
                    AtomContent atomContent = AtomContent.builder(atomUri4)
                                    .addTitle("member atom C (4/4)")
                                    .addSocket(Socket.builder(atomUriString + "#chatSocket")
                                                    .setSocketDefinition(WXCHAT.ChatSocket.asURI())
                                                    .build())
                                    .addType(URI.create(WON.Atom.getURI()))
                                    .build();
                    WonMessage createMessage = WonMessageBuilder.createAtom()
                                    .atom(atomUri4)
                                    .content().graph(RdfOutput.toGraph(atomContent))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(getAnyoneMayConnectAuth()))
                                    .content().aclGraph(won.auth.model.RdfOutput.toGraph(getAnyoneMayReadAnything()))
                                    .build();
                    createMessage = ctx.getWonMessageSender().prepareMessage(createMessage);
                    ctx.getBotContextWrapper().rememberAtomUri(atomUri4);
                    EventListener successCallback = event -> {
                        logger.debug("member atom C created");
                        deactivate();
                    };
                    EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                    "Creating member atom C");
                    EventBotActionUtils.makeAndSubscribeResponseListener(createMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(createMessage);
                }
            };
            BotBehaviour bbSendConnect21 = new BotBehaviour(ctx, "bbSendConnect21") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    WonMessage connectMessage = WonMessageBuilder.connect()
                                    .sockets()
                                    .sender(URI.create(atomUri2.toString() + "#chatSocket"))
                                    .recipient(atom1GroupSocket)
                                    .direction().fromOwner()
                                    .build();
                    connectMessage = ctx.getWonMessageSender().prepareMessage(connectMessage);
                    EventListener successCallback = event -> {
                        logger.debug("member A -> group connection requested");
                        deactivate();
                    };
                    EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                    "Requesting connection member A -> group");
                    EventBotActionUtils.makeAndSubscribeResponseListener(connectMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(connectMessage);
                }
            };
            BotBehaviour bbSendConnect31 = new BotBehaviour(ctx, "bbSendConnect31") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    WonMessage connectMessage = WonMessageBuilder.connect()
                                    .sockets()
                                    .sender(URI.create(atomUri3.toString() + "#chatSocket"))
                                    .recipient(atom1GroupSocket)
                                    .direction().fromOwner()
                                    .build();
                    connectMessage = ctx.getWonMessageSender().prepareMessage(connectMessage);
                    EventListener successCallback = event -> {
                        logger.debug("member B -> group connection requested");
                        deactivate();
                    };
                    EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                    "Requesting connection member B -> group");
                    EventBotActionUtils.makeAndSubscribeResponseListener(connectMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(connectMessage);
                }
            };
            BotBehaviour bbSendConnect41 = new BotBehaviour(ctx, "bbSendConnect41") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    WonMessage connectMessage = WonMessageBuilder.connect()
                                    .sockets()
                                    .sender(URI.create(atomUri4.toString() + "#chatSocket"))
                                    .recipient(atom1GroupSocket)
                                    .direction().fromOwner()
                                    .build();
                    connectMessage = ctx.getWonMessageSender().prepareMessage(connectMessage);
                    EventListener successCallback = event -> {
                        logger.debug("member C -> group connection requested");
                        deactivate();
                    };
                    EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                    "Requesting connection member C -> group");
                    EventBotActionUtils.makeAndSubscribeResponseListener(connectMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(connectMessage);
                }
            };
            BotBehaviour bbWaitForAutoConnect = new BotBehaviour(ctx) {
                @Override protected void onActivate(Optional<Object> message) {
                    bus.subscribe(ConnectFromOtherAtomEvent.class,
                                    new ActionOnceAfterNEventsListener(ctx, new EventFilter() {
                                        @Override public boolean accept(Event event) {
                                            boolean waitingForThis = ((ConnectFromOtherAtomEvent) event)
                                                            .getSenderSocket()
                                                            .equals(atom1GroupSocket);
                                            return waitingForThis;
                                        }
                                    }, 3, new EventBotAction() {
                                        @Override public Runnable getActionTask(Event event,
                                                        EventListener eventListener) {
                                            return () -> deactivate();
                                        }
                                    }));
                }
            };
            BotBehaviour bbSendMsg41 = new BotBehaviour(ctx, "bbSendMsg41") {
                @Override
                protected void onActivate(Optional<Object> message) {
                    WonMessage connectMessage = WonMessageBuilder.connectionMessage()
                                    .sockets()
                                    .sender(URI.create(atomUri4.toString() + "#chatSocket"))
                                    .recipient(URI.create(atomUri1.toString() + "#groupSocket"))
                                    .direction().fromOwner()
                                    .content()
                                    .text("Hello, world!")
                                    .build();
                    connectMessage = ctx.getWonMessageSender().prepareMessage(connectMessage);
                    EventListener successCallback = event -> {
                        logger.debug("member C -> send message to group");
                        deactivate();
                    };
                    EventListener failureCallback = makeFailureCallbackToFailTest(bot, ctx, bus,
                                    "sending message member C -> group");
                    EventBotActionUtils.makeAndSubscribeResponseListener(connectMessage, successCallback,
                                    failureCallback, ctx);
                    ctx.getWonMessageSender().sendMessage(connectMessage);
                }
            };
            BotBehaviour bbWaitForGroupMessage = new BotBehaviour(ctx) {
                @Override protected void onActivate(Optional<Object> message) {
                    bus.subscribe(MessageFromOtherAtomEvent.class,
                                    new ActionOnceAfterNEventsListener(ctx, new EventFilter() {
                                        @Override public boolean accept(Event event) {
                                            boolean interestedInThis = ((MessageFromOtherAtomEvent) event)
                                                            .getWonMessage().getSenderSocketURIRequired()
                                                            .equals(atom1GroupSocket);
                                            return interestedInThis;
                                        }
                                    }, 2, new EventBotAction() {
                                        @Override public Runnable getActionTask(Event event,
                                                        EventListener eventListener) {
                                            return () -> passTest(bus);
                                        }
                                    }));
                }
            };
            BehaviourBarrier barrier = new BehaviourBarrier(ctx);
            barrier.waitFor(bbCreateAtom1, bbCreateAtom2, bbCreateAtom3, bbCreateAtom4);
            barrier.thenStart(bbSendConnect21, bbSendConnect31, bbSendConnect41);
            barrier.activate();
            bbWaitForAutoConnect.onDeactivateActivate(bbSendMsg41);
            bbWaitForGroupMessage.activate();
            bbWaitForAutoConnect.activate();
            bbCreateAtom1.activate();
            bbCreateAtom2.activate();
            bbCreateAtom3.activate();
            bbCreateAtom4.activate();
        });
    }

    private Authorization getAnyoneMayReadAnything() {
        return Authorization.builder()
                        .setGranteeGranteeWildcard(GranteeWildcard.ANYONE)
                        .addGrant(AseRoot.builder().addOperationsSimpleOperationExpression(OP_READ).build())
                        .build();
    }

    private Authorization getAnyoneMayConnectAuth() {
        return Authorization.builder()
                        .addGranteesAtomExpression(AtomExpression.builder()
                                        .addAtomsRelativeAtomExpression(RelativeAtomExpression.ANY_ATOM)
                                        .build())
                        .addGrant(AseRoot.builder().addOperationsMessageOperationExpression(OP_CONNECT_CLOSE).build())
                        .build();
    }

    private Authorization getConnectedMayCommunicateAuth() {
        return Authorization.builder()
                        .addGranteesAseRoot(ase -> ase
                                        .addSocket(se -> se
                                                        .addOperationsMessageOperationExpression(OP_COMMUNICATE)
                                                        .addConnection(
                                                                        ce -> ce
                                                                                        .setTargetAtom(new TargetAtomExpression())
                                                                                        .addConnectionState(
                                                                                                        ConnectionState.CONNECTED))))
                        .addGrant(AseRoot.builder().addOperationsMessageOperationExpression(OP_CONNECT_CLOSE).build())
                        .build();
    }

    private Authorization getBuddiesReceiveBuddyTokenAuth() {
        // buddies and buddies of buddies can see my content
        Authorization auth = Authorization.builder()
                        .addGranteesAseRoot(ase -> ase
                                        .addSocket(se -> se
                                                        .addSocketType(WXBUDDY.BuddySocket.asURI())
                                                        .addConnection(ce -> ce
                                                                        .addConnectionState(
                                                                                        ConnectionState.CONNECTED)
                                                                        .setTargetAtom(new TargetAtomExpression()))))
                        .addGrant(ase -> ase
                                        .addOperationsTokenOperationExpression(top -> top
                                                        .setRequestToken(rt -> rt
                                                                        .setTokenScopeURI(
                                                                                        WXBUDDY.BuddySocket.asURI()))))
                        .build();
        return auth;
    }

    private Authorization getBuddiesAndBOfBuddiesReadAllGraphsAuth() {
        // buddies and buddies of buddies can see my content
        Authorization auth = Authorization.builder()
                        .addGranteesAseRoot(ase -> ase
                                        .addSocket(se -> se
                                                        .addSocketType(WXBUDDY.BuddySocket.asURI())
                                                        .addConnection(ce -> ce
                                                                        .addConnectionState(ConnectionState.CONNECTED)
                                                                        .setTargetAtom(new TargetAtomExpression()))))
                        .addBearer(ts -> ts
                                        .setNodeSigned(true)
                                        .addTokenScopesURI(WXBUDDY.BuddySocket.asURI())
                                        .addIssuersAseRoot(i -> i
                                                        .addSocket(se -> se
                                                                        .addSocketType(WXBUDDY.BuddySocket.asURI())
                                                                        .addConnection(ce -> ce
                                                                                        .addConnectionState(
                                                                                                        ConnectionState.CONNECTED)
                                                                                        .setTargetAtom(new TargetAtomExpression())))))
                        .addBearer(ts -> ts
                                        .setNodeSigned(true)
                                        .addTokenScopesURI(WXBUDDY.BuddySocket.asURI())
                                        .addIssuersAtomExpression(i -> i.addAtomsRelativeAtomExpression(
                                                        RelativeAtomExpression.SELF)))
                        .addGrant(ase -> ase
                                        .addGraph(ge -> ge.addOperationsSimpleOperationExpression(OP_READ)))
                        .build();
        return auth;
    }

    private Authorization getAnyoneGetsAuthInfo() {
        return Authorization.builder()
                        .setGranteeGranteeWildcard(GranteeWildcard.ANYONE)
                        .setProvideAuthInfo(ase -> ase.addOperationsSimpleOperationExpression(ANY_OPERATION))
                        .build();
    }

    private Authorization getNobodyMayDoNothing() {
        return Authorization.builder()
                        .addGranteesAtomExpression(
                                        ae -> ae.addAtomsURI(URI.create("http://example.com/nobody")))
                        .addGrant(ase -> ase.addOperationsSimpleOperationExpression(OP_NOP))
                        .build();
    }
}
