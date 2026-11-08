package net.thecommandcraft.vanishpp.hooks;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Coverage for the PLAYER_INFO/PLAYER_INFO_UPDATE scrub added as a backstop for the nametag
 * leak reported via a third-party team plugin: Bukkit's hidePlayer() only sends a one-shot
 * PLAYER_INFO_REMOVE at the moment visibility changes, so this policy strips vanished-player
 * entries out of every later PLAYER_INFO packet too, regardless of what caused it to be sent.
 */
class VanishPlayerInfoPolicyTest {

    private static final UUID VANISHED = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID VISIBLE = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final Set<UUID> VANISHED_SET = Set.of(VANISHED);

    @Test
    void keepsEverythingWhenNothingIsVanished() {
        var result = VanishPlayerInfoPolicy.filter(
                List.of("a", "b"), name -> VISIBLE, VANISHED_SET::contains);

        assertFalse(result.changed());
        assertFalse(result.cancel());
        assertEquals(List.of("a", "b"), result.kept());
    }

    @Test
    void stripsOnlyTheVanishedEntry() {
        var result = VanishPlayerInfoPolicy.filter(
                List.of("vanished-entry", "visible-entry"),
                name -> name.equals("vanished-entry") ? VANISHED : VISIBLE,
                VANISHED_SET::contains);

        assertTrue(result.changed());
        assertFalse(result.cancel(), "at least one entry survives, so the packet should still be sent");
        assertEquals(List.of("visible-entry"), result.kept());
    }

    @Test
    void cancelsWhenEveryEntryIsVanished() {
        var result = VanishPlayerInfoPolicy.filter(
                List.of("only-vanished"), name -> VANISHED, VANISHED_SET::contains);

        assertTrue(result.changed());
        assertTrue(result.cancel(), "a packet with nothing left to send should be cancelled outright");
        assertTrue(result.kept().isEmpty());
    }

    @Test
    void entryWithNoIdentifiableProfileIsAlwaysKept() {
        // A null UUID (entry with no resolvable profile) must never be treated as vanished -
        // it's simply passed through untouched.
        var result = VanishPlayerInfoPolicy.filter(
                List.of("no-profile"), name -> null, VANISHED_SET::contains);

        assertFalse(result.changed());
        assertEquals(List.of("no-profile"), result.kept());
    }

    @Test
    void emptyInputStaysEmptyAndUnchanged() {
        var result = VanishPlayerInfoPolicy.filter(
                List.of(), name -> VANISHED, VANISHED_SET::contains);

        assertFalse(result.changed());
        assertFalse(result.cancel());
        assertTrue(result.kept().isEmpty());
    }

    @Test
    void mixedListStripsOnlyVanishedEntriesPreservingOrder() {
        var result = VanishPlayerInfoPolicy.filter(
                List.of("v1", "keep1", "v2", "keep2"),
                name -> name.startsWith("v") ? VANISHED : VISIBLE,
                VANISHED_SET::contains);

        assertTrue(result.changed());
        assertEquals(List.of("keep1", "keep2"), result.kept());
    }
}
