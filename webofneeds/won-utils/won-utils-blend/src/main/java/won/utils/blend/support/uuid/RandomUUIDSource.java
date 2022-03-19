package won.utils.blend.support.uuid;

import java.util.UUID;

public class RandomUUIDSource implements UUIDSource {
    @Override
    public UUID createUUID() {
        return UUID.randomUUID();
    }
}
