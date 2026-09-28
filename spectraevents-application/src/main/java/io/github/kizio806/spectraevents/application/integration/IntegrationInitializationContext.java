package io.github.kizio806.spectraevents.application.integration;

import io.github.kizio806.spectraevents.application.SpectraEventsApplication;
import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import java.util.logging.Logger;

public record IntegrationInitializationContext(
    SpectraEventsApplication application, EventInstanceRepository repository, Logger logger) {}
