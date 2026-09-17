package io.github.kizio806.spectraevents.platform.paper.render;

import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import java.util.Map;

/** Platform handle wrapping spawned Paper display and interaction entity UUIDs for an instance. */
public record PaperModelInstanceHandle(
    EventInstanceId instanceId,
    Map<String, java.util.UUID> partEntities,
    java.util.UUID interactionEntityId) {}
