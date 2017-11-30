package won.bot.framework.eventbot.event.impl.analyzation;

import won.bot.framework.eventbot.event.BaseNeedAndConnectionSpecificEvent;
import won.protocol.model.Connection;

/**
 * Created by fsuda on 27.11.2017.
 */
public abstract class ProposalEvent extends BaseNeedAndConnectionSpecificEvent {
    private final Object payload;

    public ProposalEvent(Connection con, Object payload) {
        super(con);
        this.payload = payload;
    }

    public Object getPayload() {
        return payload;
    }
}
