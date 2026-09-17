package io.github.kizio806.spectraevents.core.event.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventInstanceIdTest {
  @Test
  void hasValueSemanticsAndAStableStringRepresentation() {
    UUID value = UUID.fromString("c56a4180-65aa-42ec-a945-5fd21dec0538");

    EventInstanceId first = new EventInstanceId(value);
    EventInstanceId second = new EventInstanceId(value);

    assertEquals(first, second);
    assertEquals(value.toString(), first.toString());
  }

  @Test
  void generatesDistinctIdentifiers() {
    assertNotEquals(EventInstanceId.generate(), EventInstanceId.generate());
  }

  @Test
  void rejectsANullValue() {
    assertThrows(NullPointerException.class, () -> new EventInstanceId(null));
  }
}
