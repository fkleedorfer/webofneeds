package won.utils.blend.support.uuid;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class SequentialUUIDSource implements UUIDSource {
    private AtomicLong count = new AtomicLong(0);

    @Override
    public UUID createUUID() {
        return UUID.nameUUIDFromBytes((count.getAndIncrement() + "").getBytes());
    }
}
