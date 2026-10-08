package io.github.kizio806.spectraevents.application.execution;

import io.github.kizio806.spectraevents.core.event.execution.action.CoreActions;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstanceId;
import io.github.kizio806.spectraevents.core.gameplay.contribution.DamageContribution;
import io.github.kizio806.spectraevents.core.gameplay.health.Health;
import io.github.kizio806.spectraevents.core.gameplay.hits.HitCounter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/** Strongly-typed runtime state held per event instance. */
public final class EventRuntimeState {
  private final EventInstanceId instanceId;
  private final AtomicReference<Health> health = new AtomicReference<>(null);
  private final AtomicReference<HitCounter> hitCounter = new AtomicReference<>(null);
  private final AtomicLong lockedUntilMillis = new AtomicLong(0L);
  private final AtomicLong timerDeadlineMillis = new AtomicLong(0L);
  private final AtomicLong encounterDeadlineMillis = new AtomicLong(0L);
  private final java.util.concurrent.atomic.AtomicInteger minimumContribution =
      new java.util.concurrent.atomic.AtomicInteger(0);
  private final AtomicReference<EventZone> eventZone = new AtomicReference<>(null);
  private final AtomicReference<Object> platformLocation = new AtomicReference<>(null);
  private final AtomicReference<String> claimant = new AtomicReference<>(null);
  private final AtomicReference<DamageContribution> contribution =
      new AtomicReference<>(DamageContribution.empty());
  private final AtomicReference<Object> bossEntityId = new AtomicReference<>(null);
  private final Map<UUID, Long> contributionThresholdMillis = new ConcurrentHashMap<>();
  private final Map<UUID, Long> lastCombatDamageMillis = new ConcurrentHashMap<>();
  private final java.util.Set<UUID> participants = ConcurrentHashMap.newKeySet();
  private static final int MAX_TRACKED_EVENT_MOBS = 256;
  private final Map<String, TrackedWave> trackedWaves = new ConcurrentHashMap<>();
  private final Map<Integer, CoreActions.LootStack> sharedLoot = new ConcurrentHashMap<>();
  private final java.util.concurrent.atomic.AtomicBoolean sharedLootInitialized =
      new java.util.concurrent.atomic.AtomicBoolean(false);

  public EventRuntimeState(EventInstanceId instanceId) {
    this.instanceId = Objects.requireNonNull(instanceId, "instanceId");
  }

  public EventInstanceId instanceId() {
    return instanceId;
  }

  public Optional<Health> health() {
    return Optional.ofNullable(health.get());
  }

  public int currentHealth() {
    Health h = health.get();
    return h != null ? h.current() : 0;
  }

  public int maxHealth() {
    Health h = health.get();
    return h != null ? h.max() : 0;
  }

  public void setHealth(Health newHealth) {
    this.health.set(newHealth);
  }

  public Health updateHealth(java.util.function.Function<Health, Health> updateFn) {
    return this.health.updateAndGet(current -> current != null ? updateFn.apply(current) : null);
  }

  public HitCounter hitCounter() {
    return hitCounter.get();
  }

  public void setHitCounter(HitCounter newHitCounter) {
    this.hitCounter.set(newHitCounter);
  }

  public HitCounter updateHitCounter(java.util.function.Function<HitCounter, HitCounter> updateFn) {
    return this.hitCounter.updateAndGet(
        current -> current != null ? updateFn.apply(current) : null);
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

  public long encounterDeadlineMillis() {
    return encounterDeadlineMillis.get();
  }

  public void setEncounterDeadlineMillis(long millis) {
    encounterDeadlineMillis.set(millis);
  }

  public int minimumContribution() {
    return minimumContribution.get();
  }

  public void setMinimumContribution(int minimum) {
    if (minimum < 0) {
      throw new IllegalArgumentException("minimum contribution must not be negative");
    }
    minimumContribution.set(minimum);
  }

  public Optional<EventZone> eventZone() {
    return Optional.ofNullable(eventZone.get());
  }

  public void setEventZone(EventZone zone) {
    eventZone.set(zone);
  }

  public Optional<Object> platformLocation() {
    return Optional.ofNullable(platformLocation.get());
  }

  public void setPlatformLocation(Object location) {
    this.platformLocation.set(location);
  }

  public boolean tryClaim(String claimantId) {
    String current = claimant.get();
    return Objects.equals(current, claimantId) || this.claimant.compareAndSet(null, claimantId);
  }

  public String claimant() {
    return claimant.get();
  }

  public boolean isClaimed() {
    return claimant.get() != null;
  }

  public DamageContribution contribution() {
    return contribution.get();
  }

  public void recordDamage(UUID playerUuid, int amount) {
    recordDamage(playerUuid, amount, minimumContribution());
  }

  /** Records Metin damage without performing any I/O. */
  public void recordDamage(UUID playerUuid, int amount, int threshold) {
    if (playerUuid != null && amount > 0) {
      participants.add(playerUuid);
      contribution.updateAndGet(
          current -> {
            DamageContribution updated = current.addDamage(playerUuid, amount);
            if (updated.contributions().getOrDefault(playerUuid, 0) >= threshold) {
              contributionThresholdMillis.putIfAbsent(playerUuid, System.currentTimeMillis());
            }
            return updated;
          });
    }
  }

  /** Records a player who made a qualifying event interaction, independent of damage ranking. */
  public void recordParticipant(UUID playerId) {
    if (playerId != null) {
      participants.add(playerId);
    }
  }

  public java.util.Set<UUID> participants() {
    return java.util.Set.copyOf(participants);
  }

  public void setParticipants(java.util.Set<UUID> restored) {
    participants.clear();
    participants.addAll(Objects.requireNonNull(restored, "restored"));
  }

  public void setContribution(DamageContribution restored) {
    contribution.set(Objects.requireNonNull(restored, "restored"));
  }

  public Map<UUID, Long> contributionThresholdMillis() {
    return Map.copyOf(contributionThresholdMillis);
  }

  public void setContributionThresholdMillis(Map<UUID, Long> restored) {
    contributionThresholdMillis.clear();
    contributionThresholdMillis.putAll(Objects.requireNonNull(restored, "restored"));
  }

  /** Per-instance anti-spam guard; it intentionally expires with the running encounter. */
  public synchronized boolean tryAcceptCombatDamage(UUID playerId, long cooldownMillis) {
    if (playerId == null || cooldownMillis <= 0L) {
      return true;
    }
    long now = System.currentTimeMillis();
    Long previous = lastCombatDamageMillis.get(playerId);
    if (previous != null && now - previous < cooldownMillis) {
      return false;
    }
    lastCombatDamageMillis.put(playerId, now);
    return true;
  }

  public void beginWave(String waveId) {
    if (waveId == null || waveId.isBlank()) {
      return;
    }
    if (trackedWaves.putIfAbsent(waveId, new TrackedWave(waveId)) != null) {
      throw new IllegalStateException("wave is already active: " + waveId);
    }
  }

  public void trackWaveEntity(String waveId, UUID entityId) {
    if (waveId == null || waveId.isBlank()) {
      return;
    }
    int total = trackedWaves.values().stream().mapToInt(TrackedWave::size).sum();
    if (total >= MAX_TRACKED_EVENT_MOBS) {
      throw new IllegalStateException(
          "event mob budget of " + MAX_TRACKED_EVENT_MOBS + " exceeded");
    }
    TrackedWave wave = trackedWaves.get(waveId);
    if (wave == null) {
      throw new IllegalStateException("unknown tracked wave: " + waveId);
    }
    wave.add(entityId);
  }

  public boolean recordWaveEntityDeath(String waveId, UUID entityId) {
    TrackedWave wave = trackedWaves.get(waveId);
    return wave != null && wave.remove(entityId) && wave.isCleared();
  }

  public int trackedWaveEntityCount() {
    return trackedWaves.values().stream().mapToInt(TrackedWave::size).sum();
  }

  public Optional<Object> bossEntityId() {
    return Optional.ofNullable(bossEntityId.get());
  }

  public void setBossEntityId(Object id) {
    this.bossEntityId.set(id);
  }

  /** Initializes the public loot snapshot only once, preserving it across phase re-entry. */
  public synchronized boolean initializeSharedLoot(List<CoreActions.LootStack> items) {
    if (!sharedLootInitialized.compareAndSet(false, true)) {
      return false;
    }
    for (int slot = 0; slot < items.size(); slot++) {
      sharedLoot.put(slot, items.get(slot));
    }
    return true;
  }

  /** Atomically removes a slot. A caller must durably persist the changed state before delivery. */
  public synchronized Optional<CoreActions.LootStack> takeSharedLootSlot(int slot) {
    return Optional.ofNullable(sharedLoot.remove(slot));
  }

  public Map<Integer, CoreActions.LootStack> sharedLootSnapshot() {
    return Map.copyOf(sharedLoot);
  }

  public boolean sharedLootInitialized() {
    return sharedLootInitialized.get();
  }

  public boolean sharedLootEmpty() {
    return sharedLootInitialized.get() && sharedLoot.isEmpty();
  }

  public synchronized void restoreSharedLoot(Map<Integer, CoreActions.LootStack> slots) {
    sharedLoot.clear();
    sharedLoot.putAll(slots);
    sharedLootInitialized.set(true);
  }
}
