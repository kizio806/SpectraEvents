package io.github.kizio806.spectraevents.platform.sponge.render;

import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import java.util.Map;
import java.util.UUID;

public record SpongeModelInstanceHandle(
    EventInstanceId instanceId, Map<String, UUID> partEntities, UUID interactionEntityId) {}
