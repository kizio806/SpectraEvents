package io.github.kizio806.spectraevents.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.kizio806.spectraevents.application.port.LifecycleReporter;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SpectraEventsApplicationTest {
  @Test
  void reportsLifecycleInOrder() {
    RecordingLifecycleReporter reporter = new RecordingLifecycleReporter();
    SpectraEventsApplication application = new SpectraEventsApplication(reporter);

    application.start();
    application.stop();

    assertEquals(List.of("started", "stopped"), reporter.events);
  }

  private static final class RecordingLifecycleReporter implements LifecycleReporter {
    private final List<String> events = new ArrayList<>();

    @Override
    public void started() {
      events.add("started");
    }

    @Override
    public void stopped() {
      events.add("stopped");
    }
  }
}
