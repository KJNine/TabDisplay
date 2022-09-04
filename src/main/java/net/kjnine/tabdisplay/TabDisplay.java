package net.kjnine.tabdisplay;

import com.google.common.base.Suppliers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public final class TabDisplay extends JavaPlugin {

    private NamespacedKey showTps;

    private static final TextColor GOOD = TextColor.color(0x64FF64);
    private static final TextColor EHH = TextColor.color(0xFFFF64);
    private static final TextColor NOTGOOD = TextColor.color(0xFFAA64);
    private static final TextColor BAD = TextColor.color(0xFF6464);
    private static final TextColor HORRID = TextColor.color(0xFF64FF);

    private static final TextColor GREY = TextColor.color(0xAAAAAA);
    private static final String DECF = "%.01f";
    private static final Component TPS_TEXT = Component.text(" TPS / MSPT: ").color(GREY),
            SUBSCRIBED = Component.text("Subscribed to TPS tab-display").color(GREY),
            UNSUBSCRIBED = Component.text("Unsubscribed from TPS tab-display").color(GREY);

    private BukkitTask tabThread;

    @Override
    public void onEnable() {
        showTps = new NamespacedKey(this, "subscribe_tps");

        tabThread = getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            // Lazy-init the component - all the component-making code in the supplier
            // is only ever called if there are players to send it to,
            // then cached and sent to all players.
            // (Slight performance increase)
            Supplier<Component> tpsSupplier = Suppliers.memoize(() -> {
                double tps = getServer().getTPS()[0];
                double mspt = getServer().getAverageTickTime();
                TextColor tickColor = mspt > 60 ? HORRID
                        : (mspt > 50 ? BAD
                        : (mspt > 40 ? NOTGOOD
                        : (mspt > 30 ? EHH
                        : GOOD))); // <30 good, 30-40 ehh, 40-50 notgood, 50-60 BAD, >60 horrid
                TextColor tpsColor = tps < 16 ? HORRID
                        : (tps < 18 ? BAD
                        : (tps < 19.99 ? NOTGOOD
                        : (mspt > 35 ? EHH
                        : GOOD))); // <16 Horrid, <18 Bad, <20 Notgood, 20TPS but >35mspt EHH, otherwise GOOD
                return Component.text(String.format(DECF, tps))
                        .color(tpsColor)
                        .append(TPS_TEXT)
                        .append(Component.text(String.format(DECF, mspt))
                                .color(tickColor));
            });
            getServer().getOnlinePlayers().stream()
                    .filter(op -> op.getPersistentDataContainer().has(showTps))
                    .forEach(op -> op.sendPlayerListFooter(tpsSupplier.get()));
        }, 20L, 20L);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(!(sender instanceof Player)) return true;
        if(command.getName().equals("tab")) {
            // Could also check for args and allow multiple tab subscriptions.

            Player p = (Player) sender;
            if(p.getPersistentDataContainer().has(showTps)) {
                p.getPersistentDataContainer().remove(showTps);
                p.sendMessage(UNSUBSCRIBED);
                p.sendPlayerListFooter(Component.empty());
            } else {
                p.getPersistentDataContainer().set(showTps, PersistentDataType.INTEGER, 1);
                p.sendMessage(SUBSCRIBED);
            }

        }

        return super.onCommand(sender, command, label, args);
    }

    @Override
    public void onDisable() {
        try {
            tabThread.cancel();
        } catch(NullPointerException ignored) {}
    }
}
