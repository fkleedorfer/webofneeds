package won.bot.framework.events.listener.baStateBots.baPCMessagingBots.atomicBots.coordinationMessageAsTextBot;

import won.bot.framework.events.listener.baStateBots.BATestBotScript;
import won.bot.framework.events.listener.baStateBots.BATestScriptAction;
import won.bot.framework.events.listener.baStateBots.WON_BA;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * User: Danijel
 * Date: 17.4.14.
 */
public class ActiveFailVoteSPBot extends BATestBotScript
{

  @Override
  protected List<BATestScriptAction> setupActions() {
    List<BATestScriptAction> actions = new ArrayList();

    actions.add(new BATestScriptAction(true, "MESSAGE_FAIL", URI.create(WON_BA.STATE_ACTIVE.getURI())));
    //no vote!
    actions.add(new BATestScriptAction(false, "MESSAGE_FAILED", URI.create(WON_BA.STATE_FAILING_ACTIVE_CANCELING.getURI())));

    return actions;
  }
}