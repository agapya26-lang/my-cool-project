package net.lanternlight.infuse;

import net.lanternlight.infuse.InfuseType.Slot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

public final class Listeners implements Listener {
    private final LanternInfuse plugin;
    private final Random rng = new Random();

    public Listeners(LanternInfuse plugin) {
        this.plugin = plugin;
    }

    private Profiles profiles() {
        return plugin.profiles();
    }

    // ---- equip by right-clicking an effect item ----

    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack inHand = e.getItem();
        ItemFactory.Held held = plugin.items().read(inHand);
        if (held == null) return;

        e.setCancelled(true);
        Player p = e.getPlayer();
        Slot slot = held.type().slot;
        EffectSettings s = plugin.settings(held.type());

        if (profiles().get(p, slot) != null) {
            plugin.msg(p, "&cYour " + (slot == Slot.PRIMARY ? "primary" : "support")
                    + " slot is full. Use &f/" + (slot == Slot.PRIMARY ? "ldrain" : "rdrain") + " &cfirst.");
            return;
        }

        profiles().set(p, slot, held.type(), held.aug());
        if (inHand.getAmount() <= 1) {
            p.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        } else {
            ItemStack rest = inHand.clone();
            rest.setAmount(inHand.getAmount() - 1);
            p.getInventory().setItemInMainHand(rest);
        }

        Location l = p.getLocation().add(0, 1, 0);
        p.getWorld().playSound(l, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.2f);
        p.getWorld().spawnParticle(Particle.END_ROD, l, 30, 0.5, 0.8, 0.5, 0.05);
        plugin.msg(p, "You equipped " + (held.aug() ? "&6✦ Augmented " : "") + s.name() + " Effect&7!");
    }

    // ---- spark via offhand key ----

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent e) {
        Player p = e.getPlayer();
        boolean def = plugin.getConfig().getString("settings.default-controls", "offhand").equalsIgnoreCase("offhand");
        if (!profiles().offhandControls(p, def)) return;
        if (profiles().get(p, Slot.PRIMARY) == null && profiles().get(p, Slot.SUPPORT) == null) return;
        e.setCancelled(true);
        plugin.abilities().trySpark(p, p.isSneaking() ? Slot.SUPPORT : Slot.PRIMARY);
    }

    // ---- damage hooks ----

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (plugin.suppress()) return;
        Player attacker = shooterOf(e.getDamager());
        if (attacker == null) return;
        if (!(e.getEntity() instanceof LivingEntity victim)) return;
        if (victim.equals(attacker)) return;
        plugin.abilities().onHit(attacker, victim, e, e.getDamager() instanceof Player);
    }

    private Player shooterOf(Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile pr && pr.getShooter() instanceof Player shooter) return shooter;
        return null;
    }

    @EventHandler(ignoreCancelled = true)
    public void onFall(EntityDamageEvent e) {
        if (e.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!(e.getEntity() instanceof Player p)) return;
        if (profiles().get(p, Slot.PRIMARY) == InfuseType.FEATHER) e.setCancelled(true);
    }

    // ---- Haste: lucky ore drops ----

    @EventHandler(ignoreCancelled = true)
    public void onDrops(BlockDropItemEvent e) {
        Player p = e.getPlayer();
        if (profiles().get(p, Slot.PRIMARY) != InfuseType.HASTE) return;
        Material m = e.getBlockState().getType();
        if (!(m.name().endsWith("_ORE") || m == Material.ANCIENT_DEBRIS)) return;

        boolean aug = profiles().isAug(p, Slot.PRIMARY);
        boolean on = profiles().isActive(p, Slot.PRIMARY);
        double chance = on ? (aug ? 0.60 : 0.50) : (aug ? 0.35 : 0.25);
        if (rng.nextDouble() >= chance) return;

        World w = e.getBlock().getWorld();
        Location at = e.getBlock().getLocation().add(0.5, 0.5, 0.5);
        boolean any = false;
        for (Item item : new ArrayList<>(e.getItems())) {
            ItemStack stack = item.getItemStack();
            if (stack.getType().isBlock()) continue; // never duplicate silk-touched ore blocks
            w.dropItemNaturally(at, stack.clone());
            any = true;
        }
        if (any) w.spawnParticle(Particle.HAPPY_VILLAGER, at, 12, 0.4, 0.4, 0.4, 0);
    }

    // ---- Emerald: bonus XP ----

    @EventHandler
    public void onExp(PlayerExpChangeEvent e) {
        Player p = e.getPlayer();
        if (profiles().get(p, Slot.SUPPORT) != InfuseType.EMERALD) return;
        boolean aug = profiles().isAug(p, Slot.SUPPORT);
        boolean on = profiles().isActive(p, Slot.SUPPORT);
        double mult = on ? (aug ? 4.0 : 3.0) : (aug ? 2.0 : 1.5);
        e.setAmount((int) Math.ceil(e.getAmount() * mult));
    }

    // ---- Invis: no pearls near an active spark ----

    @EventHandler(ignoreCancelled = true)
    public void onLaunch(ProjectileLaunchEvent e) {
        if (!(e.getEntity() instanceof EnderPearl pearl)) return;
        if (!(pearl.getShooter() instanceof Player thrower)) return;
        for (Player p : thrower.getWorld().getPlayers()) {
            if (p.equals(thrower)) continue;
            if (profiles().get(p, Slot.PRIMARY) != InfuseType.INVIS) continue;
            if (!profiles().isActive(p, Slot.PRIMARY)) continue;
            if (profiles().trusts(p, thrower)) continue;
            double r = plugin.getConfig().getDouble("settings.spark-radius", 16.0);
            if (p.getLocation().distanceSquared(thrower.getLocation()) > r * r) continue;
            e.setCancelled(true);
            plugin.msg(thrower, "&5An Invis field is blocking your pearl!");
            return;
        }
    }

    // ---- death / quit ----

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        plugin.abilities().removeHeart(p);
        if (plugin.getConfig().getBoolean("settings.death.reset-cooldowns", true)) {
            profiles().resetCooldowns(p);
        }
        profiles().forget(p);

        String mode = plugin.getConfig().getString("settings.death.mode", "DROP").toUpperCase(Locale.ROOT);
        if (mode.equals("KEEP")) return;

        for (Slot slot : Slot.values()) {
            InfuseType t = profiles().get(p, slot);
            if (t == null) continue;
            boolean aug = profiles().isAug(p, slot);
            profiles().clear(p, slot);
            if (mode.equals("DROP")) {
                Item drop = p.getWorld().dropItemNaturally(p.getLocation(), plugin.items().create(t, aug));
                drop.setInvulnerable(true);
                drop.setUnlimitedLifetime(true);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.abilities().removeHeart(e.getPlayer());
        profiles().forget(e.getPlayer());
    }
}
