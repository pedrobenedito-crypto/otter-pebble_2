package com.huuda.otterpebble.event;

import com.huuda.otterpebble.OtterPebbleMod;
import com.huuda.otterpebble.config.OtterConfig;
import com.huuda.otterpebble.entity.OtterEntity;
import com.huuda.otterpebble.registry.ModTags;
import com.huuda.otterpebble.rescue.RescueTracker;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Comparator;
import java.util.List;

@Mod.EventBusSubscriber(modid = OtterPebbleMod.MODID)
public final class GameEvents {

    /** Notice: reliable provenance for "recent owner drop" comes from the toss event itself. */
    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        Player player = event.getPlayer();
        if (player.level().isClientSide) {
            return;
        }
        ItemEntity item = event.getEntity();
        if (item.getItem().is(ModTags.RESCUABLE_TOOLS)) {
            RescueTracker.markOwnerDrop(item, player);
        }
    }

    /** Appendix D - fishing support (off by default): one extra ordinary fish, the original catch untouched. */
    @SubscribeEvent
    public static void onItemFished(ItemFishedEvent event) {
        if (!OtterConfig.FISHING_SUPPORT.get()) {
            return;
        }
        Player player = event.getEntity();
        Level level = player.level();
        if (level.isClientSide || event.getDrops().isEmpty()) {
            return;
        }
        double range = OtterConfig.FISHING_RANGE.get();
        List<OtterEntity> helpers = level.getEntitiesOfClass(OtterEntity.class, player.getBoundingBox().inflate(range),
                otter -> otter.isAlive() && otter.isOwnedBy(player) && otter.canHelpFishing());
        if (helpers.isEmpty()) {
            return;
        }
        // Anti-stacking: at most one otter rolls per catch.
        OtterEntity helper = helpers.stream().min(Comparator.comparingDouble(o -> o.distanceToSqr(player))).orElse(null);
        if (helper == null || level.getRandom().nextDouble() >= OtterConfig.FISHING_CHANCE.get()) {
            return;
        }
        FishingHook hook = event.getHookEntity();
        Vec3 from = hook != null ? hook.position() : helper.position();
        helper.helpWithFishing(player, from);
    }

    /** Appendix D - drowned escape splash (off by default): one brief pushback, no damage. */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!OtterConfig.DROWNED_SPLASH.get()) {
            return;
        }
        if (!(event.getEntity() instanceof Player owner) || owner.level().isClientSide) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Drowned drowned)) {
            return;
        }
        List<OtterEntity> helpers = owner.level().getEntitiesOfClass(OtterEntity.class, owner.getBoundingBox().inflate(8.0D),
                otter -> otter.isOwnedBy(owner) && otter.canSplash() && otter.distanceToSqr(drowned) < 6.0D * 6.0D);
        // Anti-stacking: one otter per hit.
        helpers.stream().min(Comparator.comparingDouble(o -> o.distanceToSqr(drowned)))
                .ifPresent(otter -> otter.splashAway(drowned, owner));
    }

    private GameEvents() {
    }
}
