package dev.spectraevents.application.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.spectraevents.core.event.definition.EventDefinition;
import dev.spectraevents.core.event.definition.EventDefinitionId;
import dev.spectraevents.core.event.phase.PhaseDefinition;
import dev.spectraevents.core.event.phase.PhaseId;
import dev.spectraevents.core.event.runtime.EventInstance;
import dev.spectraevents.core.event.runtime.EventInstanceId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InMemoryEventInstanceRepositoryTest {
  private static final EventInstanceId FIRST_ID =
      new EventInstanceId(UUID.fromString("c56a4180-65aa-42ec-a945-5fd21dec0538"));
  private static final EventInstanceId SECOND_ID =
      new EventInstanceId(UUID.fromString("9f1c2c26-68ce-4f6a-98d6-bf80a6ec7704"));
  private static final EventDefinitionId DEF_ID = new EventDefinitionId("meteor");

  private static final PhaseId INITIAL_PHASE = new PhaseId("initial");
  private static final EventDefinition DEFINITION =
      new EventDefinition(
          DEF_ID,
          INITIAL_PHASE,
          Map.of(INITIAL_PHASE, new PhaseDefinition(INITIAL_PHASE, Set.of())));

  private final InMemoryEventInstanceRepository repository = new InMemoryEventInstanceRepository();

  @Test
  void savesAndFindsAnInstanceByIdentity() {
    EventInstance eventInstance = EventInstance.create(FIRST_ID, DEF_ID);

    repository.save(eventInstance);

    assertSame(eventInstance, repository.findById(FIRST_ID).orElseThrow());
  }

  @Test
  void returnsEmptyWhenAnIdentityIsUnknown() {
    assertTrue(repository.findById(FIRST_ID).isEmpty());
  }

  @Test
  void replacesTheStoredStateForTheSameIdentity() {
    EventInstance created = EventInstance.create(FIRST_ID, DEF_ID);
    EventInstance running = created.start(DEFINITION).eventInstance();
    repository.save(created);

    repository.save(running);

    assertSame(running, repository.findById(FIRST_ID).orElseThrow());
    assertEquals(1, repository.findAll().size());
  }

  @Test
  void returnsAnImmutableSnapshotOfAllInstances() {
    EventInstance first = EventInstance.create(FIRST_ID, DEF_ID);
    EventInstance second = EventInstance.create(SECOND_ID, DEF_ID);
    repository.save(first);

    List<EventInstance> snapshot = repository.findAll();
    repository.save(second);

    assertEquals(List.of(first), snapshot);
    assertThrows(UnsupportedOperationException.class, () -> snapshot.add(second));
    assertEquals(2, repository.findAll().size());
    assertTrue(repository.findAll().containsAll(List.of(first, second)));
  }

  @Test
  void removesAnExistingInstanceAndReportsTheOutcome() {
    repository.save(EventInstance.create(FIRST_ID, DEF_ID));

    assertTrue(repository.remove(FIRST_ID));
    assertFalse(repository.remove(FIRST_ID));
    assertTrue(repository.findById(FIRST_ID).isEmpty());
  }

  @Test
  void rejectsNullArgumentsAtTheRepositoryBoundary() {
    assertThrows(NullPointerException.class, () -> repository.save(null));
    assertThrows(NullPointerException.class, () -> repository.findById(null));
    assertThrows(NullPointerException.class, () -> repository.remove(null));
  }
}
