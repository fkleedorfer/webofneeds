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

package won.bot.integrationtest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import won.bot.framework.events.event.WorkDoneEvent;
import won.bot.framework.events.listener.BaseEventListener;
import won.bot.framework.events.listener.CountingListener;
import won.bot.framework.events.listener.ExecuteOnEventListener;
import won.bot.framework.manager.impl.SpringAwareBotManagerImpl;
import won.bot.impl.Create2NeedsGroupingBot;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

/**
 * Integration test.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/spring/app/botRunner.xml"})
public class Create2NeedsGroupingBotTest
{
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private static final int RUN_ONCE = 1;
  private static final long ACT_LOOP_TIMEOUT_MILLIS = 100;
  private static final long ACT_LOOP_INITIAL_DELAY_MILLIS = 100;

  MyBot bot;

  @Autowired
  ApplicationContext applicationContext;

  @Autowired
  SpringAwareBotManagerImpl botManager;

  /**
   * This is run before each @Test method.
   */
  @Before
  public void before(){
    //create a bot instance and auto-wire it
    AutowireCapableBeanFactory beanFactory = applicationContext.getAutowireCapableBeanFactory();
    this.bot = (MyBot) beanFactory.autowire(MyBot.class, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
    Object botBean = beanFactory.initializeBean(this.bot, "mybot");
    this.bot = (MyBot) botBean;
        //the bot also needs a trigger so its act() method is called regularly.
    // (there is no trigger bean in the context)
    PeriodicTrigger trigger = new PeriodicTrigger(ACT_LOOP_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    trigger.setInitialDelay(ACT_LOOP_INITIAL_DELAY_MILLIS);
    this.bot.setTrigger(trigger);
  }

  /**
   * The main test method.
   * @throws Exception
   */
  @Test
  public void testCreate2NeedsGroupingBot() throws Exception
  {
    logger.info("starting test case testCreate2NeedsGroupingBot");
    //adding the bot to the bot manager will cause it to be initialized.
    //at that point, the trigger starts.
    botManager.addBot(this.bot);
    //the bot should now be running. We have to wait for it to finish before we
    //can check the results:
    //Together with the barrier.await() in the bot's listener, this trips the barrier
    //and both threads continue.
    this.bot.getBarrier().await();
    //now check the results!
    this.bot.executeAsserts();
    logger.info("finishing test case testCreate2NeedsGroupingBot");
  }


  /**
   * We create a subclass of the bot we want to test here so that we can
   * add a listener to its internal event bus and to access its listeners, which
   * record information during the run that we later check with asserts.
   */
  public static class MyBot extends Create2NeedsGroupingBot
  {
    /**
     * Used for synchronization with the @Test method: it should wait at the
     * barrier until our bot is done, then execute the asserts.
     */
    CyclicBarrier barrier = new CyclicBarrier(2);

    /**
     * Default constructor is required for instantiation through Spring.
     */
    public MyBot(){
    }

    @Override
    protected void initializeEventListeners()
    {
      //of course, let the real bot implementation initialize itself
      super.initializeEventListeners();
      //now, add a listener to the WorkDoneEvent.
      //its only purpose is to trip the CyclicBarrier instance that
      // the test method is waiting on
      getEventBus().subscribe(WorkDoneEvent.class, new ExecuteOnEventListener(getEventListenerContext(), new Runnable(){
        @Override
        public void run()
        {
          try {
            //together with the barrier.await() in the @Test method, this trips the barrier
            //and both threads continue.
            barrier.await();
          } catch (Exception e) {
            logger.warn("caught exception while waiting on barrier", e);
          }
        }
      }, RUN_ONCE));
    }

    public CyclicBarrier getBarrier()
    {
      return barrier;
    }

    /**
     * Here we check the results of the bot's execution.
     */
    public void executeAsserts()
    {
      //2 act events
      Assert.assertEquals(NO_OF_GROUPMEMBERS, this.groupMemberCreator.getEventCount());
      Assert.assertEquals(0, this.groupMemberCreator.getExceptionCount());
      //2 create need events
      Assert.assertEquals(NO_OF_GROUPMEMBERS, this.groupCreator.getEventCount());
      Assert.assertEquals(0, this.groupCreator.getExceptionCount());
      //1 create group events
      Assert.assertEquals(NO_OF_GROUPMEMBERS+1, this.needConnector.getEventCount());
      Assert.assertEquals(0, this.needConnector.getExceptionCount());
      //2 connect, 2 open
      Assert.assertEquals(NO_OF_GROUPMEMBERS*2, this.autoOpener.getEventCount());
      Assert.assertEquals(0, this.autoOpener.getExceptionCount());
      //41 messages
      Assert.assertEquals(NO_OF_GROUPMEMBERS, this.deactivator.getEventCount());
      Assert.assertEquals(0, this.deactivator.getExceptionCount());
      //check that the autoresponder creator was called
      Assert.assertEquals(NO_OF_GROUPMEMBERS, this.autoResponderCreator.getEventCount());
      Assert.assertEquals(0, this.autoResponderCreator.getExceptionCount());
      //check that the autoresponders were created
      Assert.assertEquals(NO_OF_GROUPMEMBERS, this.autoResponders.size());
      //check that the autoresponders responded as they should
      for (BaseEventListener autoResponder: this.autoResponders){
        Assert.assertEquals(NO_OF_MESSAGES, ((CountingListener) autoResponder).getCount());
        Assert.assertEquals(0, autoResponder.getExceptionCount());
      }
      //4 NeedDeactivated events
      Assert.assertEquals(NO_OF_GROUPMEMBERS, this.workDoneSignaller.getEventCount());
      Assert.assertEquals(0, this.workDoneSignaller.getExceptionCount());

      //TODO: there is more to check:
      //* what does the RDF look like?
      // --> pull it from the needURI/ConnectionURI and check contents
      //* what does the database look like?      */
    }

  }
}
