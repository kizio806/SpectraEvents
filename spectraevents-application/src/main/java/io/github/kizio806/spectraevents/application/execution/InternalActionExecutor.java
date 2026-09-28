package io.github.kizio806.spectraevents.application.execution;

import io.github.kizio806.spectraevents.application.port.EventInstanceRepository;
import io.github.kizio806.spectraevents.application.port.RewardClaimRepository;
import io.github.kizio806.spectraevents.core.event.definition.EventDefinition;
import io.github.kizio806.spectraevents.core.event.execution.TransitionRule;
import io.github.kizio806.spectraevents.core.event.execution.action.ActionDefinition;
import io.github.kizio806.spectraevents.core.event.execution.action.CoreActions;
import io.github.kizio806.spectraevents.core.event.execution.trigger.CoreTriggers;
import io.github.kizio806.spectraevents.core.event.phase.PhaseDefinition;
import io.github.kizio806.spectraevents.core.event.runtime.EventInstance;
import io.github.kizio806.spectraevents.core.gameplay.health.Health;
import io.github.kizio806.spectraevents.core.gameplay.hits.HitCounter;
import io.github.kizio806.spectraevents.core.gameplay.reward.RewardClaim;
import io.github.kizio806.spectraevents.core.gameplay.reward.RewardItem;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/** Handles execution of built-in domain actions. */
class InternalActionExecutor {

  private final EventInstanceRepository repository;
  private final List<IntegrationActionResolver> actionResolvers;
  private final EventExecutionEngine engine;

  InternalActionExecutor(
      EventInstanceRepository repository,
      List<IntegrationActionResolver> actionResolvers,
      EventExecutionEngine engine) {
    this.repository = repository;
    this.actionResolvers = actionResolvers;
    this.engine = engine;
  }

  CompletableFuture<Boolean> executeInternalAction(
      EventInstance instance,
      EventRuntimeState state,
      ActionDefinition action,
      ExecutionContext context) {

    for (IntegrationActionResolver resolver : actionResolvers) {
      if (resolver.supports(action.type())) {
        return resolver.execute(action, instance, state, context);
      }
    }

    if (action instanceof CoreActions.InitializeHealthAction initHealth) {
      state.setHealth(new Health(initHealth.max(), initHealth.max()));
      repository.saveState(state);
      return CompletableFuture.completedFuture(true);
    }

    if (action instanceof CoreActions.InitializeHitCounterAction initHits) {
      state.setHitCounter(HitCounter.startingAt(initHits.max()));
      repository.saveState(state);
      return CompletableFuture.completedFuture(true);
    }

    if (action instanceof CoreActions.SetLockedAction setLocked) {
      long durationMs = setLocked.duration().toMillis();
      state.setLockedUntilMillis(System.currentTimeMillis() + durationMs);
      repository.saveState(state);
      return CompletableFuture.completedFuture(true);
    }

    if (action instanceof CoreActions.TryClaimAction) {
      String claimantId =
          context != null && context.actor() != null ? context.actor().toString() : "unknown";
      boolean claimed = state.tryClaim(claimantId);
      if (claimed) {
        repository.saveState(state);
      }
      return CompletableFuture.completedFuture(claimed);
    }

    if (action instanceof CoreActions.InitializeSharedLootAction initializeLoot) {
      state.initializeSharedLoot(initializeLoot.items());
      repository.saveStateDurably(state);
      return CompletableFuture.completedFuture(true);
    }

    if (action instanceof CoreActions.InitializeGroundLootAction initializeLoot) {
      List<CoreActions.LootStack> rolled =
          initializeLoot.entries().stream()
              .filter(entry -> ThreadLocalRandom.current().nextInt(100) < entry.chance())
              .map(entry -> new CoreActions.LootStack(entry.material(), entry.amount()))
              .toList();
      state.initializeSharedLoot(rolled);
      repository.saveStateDurably(state);
      return CompletableFuture.completedFuture(true);
    }

    if (action instanceof CoreActions.CreateParticipantRewardClaimsAction rewards) {
      if (!(repository instanceof RewardClaimRepository claimRepository)) {
        throw new IllegalStateException("Reward-claim storage is unavailable");
      }
      List<RewardItem> snapshot =
          rewards.entries().stream()
              .filter(entry -> ThreadLocalRandom.current().nextInt(100) < entry.chance())
              .map(entry -> new RewardItem(entry.material(), entry.amount()))
              .toList();
      if (snapshot.isEmpty()) {
        return CompletableFuture.completedFuture(true);
      }
      for (java.util.UUID participant : state.participants()) {
        java.util.UUID claimId =
            java.util.UUID.nameUUIDFromBytes(
                (instance.id() + ":participant-rewards:" + participant)
                    .getBytes(StandardCharsets.UTF_8));
        claimRepository.savePendingDurably(
            new RewardClaim(
                claimId,
                instance.id(),
                participant,
                snapshot,
                io.github.kizio806.spectraevents.core.gameplay.reward.RewardClaimStatus.PENDING,
                java.time.Instant.now(),
                null));
      }
      return CompletableFuture.completedFuture(true);
    }

    if (action instanceof CoreActions.ApplyDamageAction applyDamage) {
      applyDamage(instance, state, applyDamage.amount(), 0, context);
      return CompletableFuture.completedFuture(true);
    }

    if (action instanceof CoreActions.ApplyCombatDamageAction applyCombatDamage) {
      if (context == null || !context.hasCombatDamage()) {
        return CompletableFuture.completedFuture(false);
      }
      if (!state.tryAcceptCombatDamage(
          context.actorId(), applyCombatDamage.cooldown().toMillis())) {
        return CompletableFuture.completedFuture(true);
      }
      int requested =
          Math.max(
              1,
              Math.min(applyCombatDamage.maximumDamage(), (int) Math.ceil(context.combatDamage())));
      applyDamage(instance, state, requested, applyCombatDamage.healthFloorPercent(), context);
      return CompletableFuture.completedFuture(true);
    }

    if (action instanceof CoreActions.IncrementHitsAction incrementHits) {
      if (state.hitCounter() != null) {
        if (context != null) {
          state.recordParticipant(context.actorId());
        }
        HitCounter previous = state.hitCounter();
        HitCounter updated =
            state.updateHitCounter(counter -> counter.addHits(incrementHits.amount()));
        repository.saveState(state);
        emitHitPercentThresholds(instance, previous, updated, context);
        if (!previous.isReached() && updated.isReached()) {
          engine.evaluateTrigger(instance.id(), new CoreTriggers.HitsReachedTrigger(), context);
        }
      }
      return CompletableFuture.completedFuture(true);
    }

    if (action instanceof CoreActions.CompleteEventAction) {
      engine.completeEvent(instance.id());
      return CompletableFuture.completedFuture(true);
    }

    if (action instanceof CoreActions.CancelEventAction) {
      engine.cancelEvent(instance.id());
      return CompletableFuture.completedFuture(true);
    }

    if (action instanceof CoreActions.FailEventAction) {
      engine.failEvent(
          instance.id(), new IllegalStateException("Event failure requested by definition action"));
      return CompletableFuture.completedFuture(true);
    }

    return null;
  }

  private void emitHitPercentThresholds(
      EventInstance instance, HitCounter previous, HitCounter updated, ExecutionContext context) {
    EventDefinition definition = engine.definitionForInstance(instance);
    if (definition == null || instance.currentPhase().isEmpty()) {
      return;
    }
    PhaseDefinition phase = definition.phase(instance.currentPhase().orElseThrow()).orElse(null);
    if (phase == null) {
      return;
    }
    for (TransitionRule rule : phase.rules()) {
      if (!(rule.trigger() instanceof CoreTriggers.HitsPercentThresholdCrossedTrigger threshold)) {
        continue;
      }
      int hitThreshold = (int) Math.ceil(updated.maximum() * (threshold.percent() / 100.0d));
      if (previous.current() < hitThreshold && updated.current() >= hitThreshold) {
        engine.evaluateTrigger(
            instance.id(),
            new CoreTriggers.HitsPercentThresholdCrossedTrigger(threshold.percent()),
            context);
      }
    }
  }

  private void applyDamage(
      EventInstance instance,
      EventRuntimeState state,
      int requestedDamage,
      int healthFloorPercent,
      ExecutionContext context) {
    if (requestedDamage < 1 || state.health().isEmpty()) {
      return;
    }
    Health oldHealth = state.health().orElseThrow();
    int floor = (int) Math.ceil(oldHealth.max() * (healthFloorPercent / 100.0d));
    int appliedDamage = Math.min(requestedDamage, Math.max(0, oldHealth.current() - floor));
    if (appliedDamage < 1) {
      return;
    }
    Health updated = state.updateHealth(h -> h.damage(appliedDamage));
    if (context != null && context.actorId() != null) {
      state.recordDamage(context.actorId(), appliedDamage);
    }
    if (updated == null) {
      return;
    }

    EventDefinition definition = engine.definitionForInstance(instance);
    if (definition != null && instance.currentPhase().isPresent()) {
      PhaseDefinition phaseDef = definition.phase(instance.currentPhase().get()).orElse(null);
      if (phaseDef != null) {
        List<Integer> absoluteThresholds = new java.util.ArrayList<>();
        List<Integer> percentThresholds = new java.util.ArrayList<>();
        for (TransitionRule rule : phaseDef.rules()) {
          if (rule.trigger() instanceof CoreTriggers.HealthThresholdCrossedTrigger threshold
              && threshold.threshold() != -1) {
            absoluteThresholds.add(threshold.threshold());
          }
          if (rule.trigger()
              instanceof CoreTriggers.HealthPercentThresholdCrossedTrigger threshold) {
            percentThresholds.add(threshold.percent());
          }
        }
        if (!absoluteThresholds.isEmpty()) {
          var thresholds =
              new io.github.kizio806.spectraevents.core.gameplay.health.HealthThresholds(
                  absoluteThresholds);
          for (Integer threshold : thresholds.checkCrossed(oldHealth, updated)) {
            engine.evaluateTrigger(
                instance.id(), new CoreTriggers.HealthThresholdCrossedTrigger(threshold), context);
          }
        }
        for (Integer percent : percentThresholds) {
          int threshold = (int) Math.ceil(updated.max() * (percent / 100.0d));
          if (oldHealth.current() > threshold && updated.current() <= threshold) {
            engine.evaluateTrigger(
                instance.id(),
                new CoreTriggers.HealthPercentThresholdCrossedTrigger(percent),
                context);
          }
        }
      }
    }
    if (updated.isDepleted()) {
      engine.evaluateTrigger(instance.id(), new CoreTriggers.HealthDepletedTrigger(), context);
    }
  }
}
