/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package won.bot.impl;

import won.bot.framework.bot.base.EventBot;
import won.bot.framework.events.EventBus;
import won.bot.framework.events.event.*;
import won.bot.framework.events.listener.BaseEventListener;
import won.bot.framework.events.listener.EventListenerContext;
import won.bot.framework.events.listener.ExecuteOnEventListener;
import won.bot.framework.events.listener.ExecuteOnceAfterNEventsListener;
import won.bot.framework.events.listener.action.ConnectFromListToListAction;
import won.bot.framework.events.listener.action.CreateNeedWithFacetsAction;
import won.bot.framework.events.listener.action.DeactivateAllNeedsOfGroupAction;
import won.bot.framework.events.listener.action.SignalWorkDoneAction;
import won.bot.framework.events.listener.baStateBots.BATestBotScript;
import won.bot.framework.events.listener.baStateBots.BATestScriptListener;
import won.bot.framework.events.listener.filter.AcceptOnceFilter;
import won.bot.framework.events.listener.filter.FinishedEventFilter;
import won.bot.framework.events.listener.filter.OrFilter;
import won.protocol.model.FacetType;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Danijel
 * Date: 26.2.14.
 * Time: 15.15
 * To change this template use File | Settings | File Templates.
 */
public abstract class BABaseBot extends EventBot
{
  public static final String URI_LIST_NAME_PARTICIPANT = "participants";
  public static final String URI_LIST_NAME_COORDINATOR = "coordinator";
  protected final int noOfNeeds;
  protected final List<BATestBotScript> scripts;
  private static final long MILLIS_BETWEEN_MESSAGES = 10;
  //we use protected members so we can extend the class and
  //access the listeners for unit test assertions and stats
  //
  //we use BaseEventListener as their types so we can access the generic
  //functionality offered by that class
  protected BaseEventListener participantNeedCreator;
  protected BaseEventListener coordinatorNeedCreator;
  protected BaseEventListener needConnector;
  protected BaseEventListener scriptsDoneListener;
  protected BaseEventListener workDoneSignaller;
  protected final List<BATestScriptListener> testScriptListeners;

  protected BABaseBot() {
    this.scripts= getScripts();
    this.noOfNeeds = scripts.size();
    this.testScriptListeners = new ArrayList<BATestScriptListener>(noOfNeeds);
  }

  @Override
  protected void initializeEventListeners() {
    final EventListenerContext ctx = getEventListenerContext();
    final EventBus bus = getEventBus();

    //create needs every trigger execution until noOfNeeds are created
    this.participantNeedCreator = new ExecuteOnEventListener(
      ctx, "participantCreator",
      new CreateNeedWithFacetsAction(ctx, URI_LIST_NAME_PARTICIPANT, FacetType.BACCParticipantFacet.getURI()),
      noOfNeeds - 1
    );
    bus.subscribe(ActEvent.class, this.participantNeedCreator);

    //when done, create one coordinator need
    this.coordinatorNeedCreator = new ExecuteOnEventListener(
      ctx, "coordinatorCreator", new FinishedEventFilter(participantNeedCreator),
      new CreateNeedWithFacetsAction(ctx, URI_LIST_NAME_COORDINATOR, FacetType.BACCCoordinatorFacet.getURI()),
      1
    );
    bus.subscribe(FinishedEvent.class, this.coordinatorNeedCreator);


    final Iterator<BATestBotScript> scriptIterator = scripts.iterator();
    //make a composite filter, with one filter for each testScriptListener that wait
    // for the FinishedEvents the they emit. That filter will be used to shut
    // down all needs after all the scriptListeners have finished.
    final OrFilter mainScriptListenerFilter = new OrFilter();
    //create a callback that gets called immediately before the connection is established
    ConnectFromListToListAction.ConnectHook scriptConnectHook = new ConnectFromListToListAction.ConnectHook()
    {
      @Override
      public void onConnect(final URI fromNeedURI, final URI toNeedURI) {
        //create the listener that will execute the script actions
        BATestScriptListener testScriptListener = new BATestScriptListener(ctx, scriptIterator.next(), fromNeedURI,
          toNeedURI, MILLIS_BETWEEN_MESSAGES);
        //remember it so we can check its state later
        testScriptListeners.add(testScriptListener);
        //subscribe it to the relevant events.
        bus.subscribe(ConnectFromOtherNeedEvent.class, testScriptListener);
        bus.subscribe(OpenFromOtherNeedEvent.class, testScriptListener);
        bus.subscribe(MessageFromOtherNeedEvent.class, testScriptListener);
        //add a filter that will wait for the FinishedEvent emitted by that listener
        //wrap it in an acceptonce filter to make extra sure we count each listener only once.
        mainScriptListenerFilter.addFilter(
          new AcceptOnceFilter(
            new FinishedEventFilter(testScriptListener)));
      }
    };

    //when done, connect the participants to the coordinator
    this.needConnector = new ExecuteOnceAfterNEventsListener(
      ctx, "needConnector", noOfNeeds,
      new ConnectFromListToListAction(ctx, URI_LIST_NAME_COORDINATOR, URI_LIST_NAME_PARTICIPANT,
        FacetType.BACCCoordinatorFacet.getURI(), FacetType.BACCParticipantFacet.getURI(), MILLIS_BETWEEN_MESSAGES,
        scriptConnectHook));
    bus.subscribe(NeedCreatedEvent.class, this.needConnector);


    //for each group member, there are 2 listeners waiting for messages. when they are all finished, we're done.
    this.scriptsDoneListener = new ExecuteOnceAfterNEventsListener(
      ctx, "scriptsDoneListener", mainScriptListenerFilter,
      noOfNeeds - 1,
      new DeactivateAllNeedsOfGroupAction(ctx, URI_LIST_NAME_PARTICIPANT));
    bus.subscribe(FinishedEvent.class, this.scriptsDoneListener);

    //When the needs are deactivated, all connections are closed. wait for the close events and signal work done.
    this.workDoneSignaller = new ExecuteOnceAfterNEventsListener(
      ctx, "workDoneSignaller",
      noOfNeeds - 1, new SignalWorkDoneAction(ctx)
    );
    bus.subscribe(CloseFromOtherNeedEvent.class, this.workDoneSignaller);
  }

  protected abstract List<BATestBotScript> getScripts();
}

