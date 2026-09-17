package dev.spectraevents.application.execution;

import dev.spectraevents.core.event.runtime.EventInstanceId;
import dev.spectraevents.core.gameplay.contribution.DamageContribution;
import dev.spectraevents.core.gameplay.health.Health;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/** Strongly-typed runtime state held per event instance. */
public final class EventRuntimeState {
  private final EventInstanceId instanceId;
  private final AtomicReference<Health> health = new AtomicReference<>(null);
  private final AtomicLong lockedUntilMillis = new AtomicLong(0L);
  private final AtomicLong timerDeadlineMillis = new AtomicLong(0L);
  private final AtomicReference<Object> platformLocation = new AtomicReference<>(null);
  private final AtomicReference<String> claimant = new AtomicReference<>(null);
  private final AtomicReference<DamageContribution> contribution =
      new AtomicReference<>(DamageContribution.empty());
  private final AtomicReference<Object> bossEntityId = new AtomicReference<>(null);

  public EventRuntimeState(EventInstanceId instanceId) {
    this.instanceId = Objects.requireNonNull(instanceId, "instanceId");
  }

  public EventInstanceId instanceId() {
    return instanceId;
  }

  public Optional<Health> health() {
    return Optional.ofNullable(health.get());
  }

  public void setHealth(Health newHealth) {
    this.health.set(newHealth);
  }

  public Health updateHealth(java.util.function.Function<Health, Health> updateFn) {
    return this.health.updateAndGet(current -> current != null ? updateFn.apply(current) : null);
  }

  public long lockedUntilMillis() {
    return lockedUntilMillis.get();
  }

  public void setLockedUntilMillis(long millis) {
    this.lockedUntilMillis.set(millis);
  }

  public boolean isLocked() {
    return System.currentTimeMillis() < lockedUntilMillis.get();
  }

  public long timerDeadlineMillis() {
    return timerDeadlineMillis.get();
  }

  public void setTimerDeadlineMillis(long millis) {
    this.timerDeadlineMillis.set(millis);
  }

  public Optional<Object> platformLocation() {
    return Optional.ofNullable(platformLocation.get());
  }

  public void setPlatformLocation(Object location) {
    this.platformLocation.set(location);
  }

  public boolean tryClaim(String claimantId) {
    return this.claimant.compareAndSet(null, claimantId);
  }

  public Optional<String> claimant() {
    return Optional.ofNullable(claimant.get());
  }

  public boolean isClaimed() {
    return claimant.get() != null;
  }

  public DamageContribution contribution() {
    return contribution.get();
  }

  public void recordDamage(UUID playerUuid, int amount) {
    if (playerUuid != null && amount > 0) {
      contribution.updateAndGet(current -> current.addDamage(playerUuid, amount));
    }
  }

  public Optional<Object> bossEntityId() {
    return Optional.ofNullable(bossEntityId.get());
  }

  public void setBossEntityId(Object id) {
    this.bossEntityId.set(id);
  }
}
