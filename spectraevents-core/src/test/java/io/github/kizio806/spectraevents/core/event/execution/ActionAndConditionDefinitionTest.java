package io.github.kizio806.spectraevents.core.event.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kizio806.spectraevents.core.event.execution.action.CoreActions;
import io.github.kizio806.spectraevents.core.event.execution.action.IntegrationActions;
import io.github.kizio806.spectraevents.core.event.execution.action.PlatformActions;
import io.github.kizio806.spectraevents.core.event.execution.condition.CoreConditions;
import io.github.kizio806.spectraevents.core.event.execution.condition.IntegrationConditions;
import io.github.kizio806.spectraevents.core.event.execution.trigger.TriggerDefinition;
import io.github.kizio806.spectraevents.core.event.phase.PhaseDefinition;
import io.github.kizio806.spectraevents.core.event.phase.PhaseId;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ActionAndConditionDefinitionTest {

  @Test
  void testCoreActions() {
    CoreActions.InitializeHealthAction initHealth = new CoreActions.InitializeHealthAction(100);
    assertEquals("initialize_health", initHealth.type());
    assertEquals(100, initHealth.max());

    CoreActions.InitializeHitCounterAction initHits =
        new CoreActions.InitializeHitCounterAction(50);
    assertEquals("initialize_hit_counter", initHits.type());
    assertEquals(50, initHits.max());

    CoreActions.SetLockedAction setLocked = new CoreActions.SetLockedAction(Duration.ofSeconds(5));
    assertEquals("set_locked", setLocked.type());
    assertEquals(Duration.ofSeconds(5), setLocked.duration());

    CoreActions.TryClaimAction tryClaim = new CoreActions.TryClaimAction();
    assertEquals("try_claim", tryClaim.type());

    CoreActions.LootStack stack = new CoreActions.LootStack("minecraft:diamond", 5);
    assertEquals("minecraft:diamond", stack.material());
    assertEquals(5, stack.amount());

    assertThrows(IllegalArgumentException.class, () -> new CoreActions.LootStack(null, 5));
    assertThrows(IllegalArgumentException.class, () -> new CoreActions.LootStack("", 5));
    assertThrows(IllegalArgumentException.class, () -> new CoreActions.LootStack("diamond", 0));

    CoreActions.InitializeSharedLootAction sharedLoot =
        new CoreActions.InitializeSharedLootAction(List.of(stack));
    assertEquals("initialize_shared_loot", sharedLoot.type());
    assertEquals(1, sharedLoot.items().size());

    CoreActions.GroundLootEntry entry = new CoreActions.GroundLootEntry("minecraft:emerald", 2, 80);
    assertEquals("minecraft:emerald", entry.material());
    assertEquals(2, entry.amount());
    assertEquals(80, entry.chance());

    assertThrows(
        IllegalArgumentException.class, () -> new CoreActions.GroundLootEntry(null, 1, 50));
    assertThrows(
        IllegalArgumentException.class, () -> new CoreActions.GroundLootEntry("emerald", 0, 50));
    assertThrows(
        IllegalArgumentException.class, () -> new CoreActions.GroundLootEntry("emerald", 1, -1));
    assertThrows(
        IllegalArgumentException.class, () -> new CoreActions.GroundLootEntry("emerald", 1, 101));

    CoreActions.InitializeGroundLootAction initGround =
        new CoreActions.InitializeGroundLootAction(List.of(entry));
    assertEquals("initialize_ground_loot", initGround.type());

    CoreActions.CreateParticipantRewardClaimsAction participantClaims =
        new CoreActions.CreateParticipantRewardClaimsAction(List.of(entry));
    assertEquals("create_participant_reward_claims", participantClaims.type());
    assertThrows(
        IllegalArgumentException.class,
        () -> new CoreActions.CreateParticipantRewardClaimsAction(List.of()));

    CoreActions.ApplyDamageAction applyDmg = new CoreActions.ApplyDamageAction(20);
    assertEquals("apply_damage", applyDmg.type());
    assertEquals(20, applyDmg.amount());

    CoreActions.ApplyCombatDamageAction combatDmg =
        new CoreActions.ApplyCombatDamageAction(50, 10, Duration.ofMillis(500));
    assertEquals("apply_combat_damage", combatDmg.type());
    assertEquals(50, combatDmg.maximumDamage());
    assertEquals(10, combatDmg.healthFloorPercent());
    assertEquals(Duration.ofMillis(500), combatDmg.cooldown());

    assertThrows(
        IllegalArgumentException.class,
        () -> new CoreActions.ApplyCombatDamageAction(0, 10, Duration.ZERO));
    assertThrows(
        IllegalArgumentException.class,
        () -> new CoreActions.ApplyCombatDamageAction(10, -1, Duration.ZERO));
    assertThrows(
        IllegalArgumentException.class,
        () -> new CoreActions.ApplyCombatDamageAction(10, 101, Duration.ZERO));
    assertThrows(
        IllegalArgumentException.class,
        () -> new CoreActions.ApplyCombatDamageAction(10, 50, Duration.ofSeconds(-1)));

    assertEquals("increment_hits", new CoreActions.IncrementHitsAction(1).type());
    assertEquals("complete_event", new CoreActions.CompleteEventAction().type());
    assertEquals("cancel_event", new CoreActions.CancelEventAction().type());
    assertEquals("fail_event", new CoreActions.FailEventAction().type());
  }

  @Test
  void testPlatformActions() {
    assertEquals("spawn_model", new PlatformActions.SpawnModelAction("model", 2).type());
    assertEquals("move_model", new PlatformActions.MoveModelAction().type());
    assertEquals("remove_model", new PlatformActions.RemoveModelAction().type());
    assertEquals("play_animation", new PlatformActions.PlayAnimationAction("anim").type());
    assertEquals("play_sound", new PlatformActions.PlaySoundAction("sound", 1.0f, 1.0f).type());
    assertEquals("spawn_particles", new PlatformActions.SpawnParticlesAction("flame", 10).type());
    assertEquals("give_item", new PlatformActions.GiveItemAction("stone", 1).type());
    assertEquals("open_shared_loot", new PlatformActions.OpenSharedLootAction("Loot").type());
    assertEquals(
        "drop_loot",
        new PlatformActions.DropLootAction(
                3.0, List.of(new PlatformActions.LootItem("diamond", 1, 100)))
            .type());
    assertEquals("release_ground_loot", new PlatformActions.ReleaseGroundLootAction(2.0).type());
    PlatformActions.PodiumPool pool =
        new PlatformActions.PodiumPool(List.of(new PlatformActions.LootItem("diamond", 1, 100)));
    assertEquals(
        "award_podium", new PlatformActions.AwardPodiumAction(List.of(pool, pool, pool)).type());
    assertEquals("send_message", new PlatformActions.SendMessageAction("hello").type());
    assertEquals("broadcast_message", new PlatformActions.BroadcastMessageAction("msg").type());
    assertEquals(
        "show_title", new PlatformActions.ShowTitleAction("title", "sub", 10, 20, 10).type());
    assertEquals(
        "spawn_boss", new PlatformActions.SpawnBossAction(1, 2, 3, "zombie", "Boss").type());
    assertEquals(
        "spawn_mobs",
        new PlatformActions.SpawnMobsAction(
                List.of(new PlatformActions.MobSpawn("zombie", "Minion", 3, 2.0f)))
            .type());
    assertEquals(
        "show_bossbar",
        new PlatformActions.ShowBossbarAction("RED", "SOLID", "Boss", "health").type());
    assertEquals(
        "update_bossbar", new PlatformActions.UpdateBossbarAction("Boss", "health").type());
    assertEquals("remove_bossbar", new PlatformActions.RemoveBossbarAction().type());
    assertEquals(
        "show_scoreboard",
        new PlatformActions.ShowScoreboardAction("Score", List.of("line1", "line2")).type());
    assertEquals(
        "update_scoreboard",
        new PlatformActions.UpdateScoreboardAction(List.of("line1", "line2")).type());
    assertEquals("remove_scoreboard", new PlatformActions.RemoveScoreboardAction().type());
  }

  @Test
  void testIntegrationActionsAndConditions() {
    IntegrationActions.GiveMoneyAction giveMoney = new IntegrationActions.GiveMoneyAction(100.5);
    assertEquals("give_money", giveMoney.type());
    assertEquals(100.5, giveMoney.amount());

    IntegrationActions.TakeMoneyAction takeMoney = new IntegrationActions.TakeMoneyAction(50.0);
    assertEquals("take_money", takeMoney.type());
    assertEquals(50.0, takeMoney.amount());

    assertEquals("not_locked", new CoreConditions.NotLockedCondition().type());
    assertEquals("is_locked", new CoreConditions.IsLockedCondition().type());

    IntegrationConditions.HasPermissionCondition hasPerm =
        new IntegrationConditions.HasPermissionCondition("admin.perm");
    assertEquals("has_permission", hasPerm.type());
    assertEquals("admin.perm", hasPerm.permission());

    IntegrationConditions.HasGroupCondition hasGroup =
        new IntegrationConditions.HasGroupCondition("vip");
    assertEquals("has_group", hasGroup.type());
    assertEquals("vip", hasGroup.group());

    IntegrationConditions.InRegionCondition inRegion =
        new IntegrationConditions.InRegionCondition("boss_arena");
    assertEquals("in_region", inRegion.type());
    assertEquals("boss_arena", inRegion.region());
  }

  @Test
  void testTransitionRuleAndPhaseDefinition() {
    TriggerDefinition trigger = () -> "dummy_trigger";
    TransitionRule rule =
        new TransitionRule(
            trigger,
            List.of(new CoreConditions.NotLockedCondition()),
            Optional.of(new PhaseId("phase_two")),
            List.of(new CoreActions.IncrementHitsAction(1)));

    assertNotNull(rule.trigger());
    assertEquals(1, rule.conditions().size());
    assertTrue(rule.targetPhase().isPresent());
    assertEquals("phase_two", rule.targetPhase().get().value());
    assertEquals(1, rule.actions().size());

    assertThrows(
        NullPointerException.class,
        () -> new TransitionRule(null, List.of(), Optional.empty(), List.of()));
    assertThrows(
        NullPointerException.class,
        () -> new TransitionRule(trigger, null, Optional.empty(), List.of()));
    assertThrows(
        NullPointerException.class, () -> new TransitionRule(trigger, List.of(), null, List.of()));
    assertThrows(
        NullPointerException.class,
        () -> new TransitionRule(trigger, List.of(), Optional.empty(), null));

    PhaseId p1 = new PhaseId("p1");
    PhaseId p2 = new PhaseId("p2");
    PhaseId p3 = new PhaseId("p3");

    PhaseDefinition phaseDef =
        new PhaseDefinition(
            p1, Set.of(p2), List.of(rule), List.of(new CoreActions.InitializeHealthAction(100)));

    assertEquals(p1, phaseDef.id());
    assertTrue(phaseDef.allowsTransitionTo(p2));
    assertFalse(phaseDef.allowsTransitionTo(p3));
    assertEquals(1, phaseDef.rules().size());
    assertEquals(1, phaseDef.onEnterActions().size());

    PhaseDefinition simpleDef = new PhaseDefinition(p1, Set.of(p2));
    assertEquals(0, simpleDef.rules().size());
    assertEquals(0, simpleDef.onEnterActions().size());

    assertThrows(NullPointerException.class, () -> new PhaseDefinition(null, Set.of()));
    assertThrows(NullPointerException.class, () -> new PhaseDefinition(p1, null));
  }
}
