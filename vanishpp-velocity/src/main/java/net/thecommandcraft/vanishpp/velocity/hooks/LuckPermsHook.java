package net.thecommandcraft.vanishpp.velocity.hooks;

import com.velocitypowered.api.proxy.Player;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import net.thecommandcraft.vanishpp.velocity.ProxyStateManager;
import net.thecommandcraft.vanishpp.velocity.VanishppVelocity;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Registers {@code vanishpp:vanished=true/false} as a LuckPerms context on the proxy side,
 * mirroring {@code vanishpp-paper}'s {@code LuckPermsHook} so a permission condition on this
 * context evaluates consistently whether it's checked at the proxy or a backend server.
 *
 * <p>Sourced from {@link ProxyStateManager#isVanished(UUID)} - the proxy's own cross-server
 * vanish state, kept in sync by the existing plugin-messaging protocol - rather than a Paper
 * {@code Vanishpp} instance, which doesn't exist on this side.
 *
 * <p>Requires LuckPerms on the proxy (optional dependency in velocity-plugin.json).
 */
public class LuckPermsHook {

    private static final String CONTEXT_KEY = "vanishpp:vanished";

    private final VanishppVelocity plugin;
    private final ProxyStateManager stateManager;
    private LuckPerms luckPerms;
    private ContextCalculator<Player> calculator;

    public LuckPermsHook(VanishppVelocity plugin, ProxyStateManager stateManager) {
        this.plugin = plugin;
        this.stateManager = stateManager;
    }

    public void load() {
        // LuckPermsProvider.get() is the platform-agnostic accessor (same on Bukkit, Velocity,
        // Bungee, Sponge, Fabric) - unlike Paper's LuckPermsHook, there's no Velocity
        // equivalent of Bukkit's ServicesManager to look this up through instead.
        this.luckPerms = LuckPermsProvider.get();

        this.calculator = new ContextCalculator<Player>() {
            @Override
            public void calculate(@NotNull Player player, @NotNull ContextConsumer consumer) {
                consumer.accept(CONTEXT_KEY, stateManager.isVanished(player.getUniqueId()) ? "true" : "false");
            }

            @Override
            public @NotNull ContextSet estimatePotentialContexts() {
                return ImmutableContextSet.builder()
                        .add(CONTEXT_KEY, "true")
                        .add(CONTEXT_KEY, "false")
                        .build();
            }
        };
        luckPerms.getContextManager().registerCalculator(this.calculator);
    }

    public void unload() {
        if (luckPerms != null && calculator != null) {
            try {
                luckPerms.getContextManager().unregisterCalculator(calculator);
            } catch (Throwable ignored) {
            }
            calculator = null;
        }
    }

    /**
     * Signal a vanish-state change to LuckPerms so it invalidates context caches for the
     * affected player immediately, rather than waiting for the next query. Called from
     * {@link ProxyStateManager#setVanished}. No-op if the player isn't connected to this
     * specific proxy instance (e.g. a state sync for a player on a different proxy in a
     * multi-proxy setup) - nothing to invalidate locally in that case.
     */
    public void setVanished(UUID uuid) {
        if (luckPerms == null) return;
        plugin.getProxy().getPlayer(uuid).ifPresent(player -> {
            try {
                luckPerms.getContextManager().signalContextUpdate(player);
            } catch (Throwable ignored) {
                // LP API version differences - best-effort
            }
        });
    }
}
