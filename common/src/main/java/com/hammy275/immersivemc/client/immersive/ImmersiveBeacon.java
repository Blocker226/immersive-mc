package com.hammy275.immersivemc.client.immersive;

import com.hammy275.immersivemc.client.config.ClientConstants;
import com.hammy275.immersivemc.client.immersive.info.AbstractImmersiveInfo;
import com.hammy275.immersivemc.client.immersive.info.AbstractWorldStorageInfo;
import com.hammy275.immersivemc.client.immersive.info.BeaconInfo;
import com.hammy275.immersivemc.client.immersive.info.InfoTriggerHitboxes;
import com.hammy275.immersivemc.common.immersive.ImmersiveCheckers;
import com.hammy275.immersivemc.common.network.Network;
import com.hammy275.immersivemc.common.network.packet.InteractPacket;
import com.hammy275.immersivemc.common.storage.ImmersiveStorage;
import com.hammy275.immersivemc.mixin.BeaconBlockEntityMixin;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ImmersiveBeacon extends AbstractWorldStorageImmersive<BeaconInfo> {

    private static final double effectHitboxSize = 0.2;
    private static final double displayHitboxSize = 0.2;
    private static final ResourceLocation[] effectLocations = new ResourceLocation[]{
            new ResourceLocation("textures/mob_effect/speed.png"),
            new ResourceLocation("textures/mob_effect/haste.png"),
            new ResourceLocation("textures/mob_effect/resistance.png"),
            new ResourceLocation("textures/mob_effect/jump_boost.png"),
            new ResourceLocation("textures/mob_effect/strength.png")
    };
    private static final ResourceLocation regenerationLocation = new ResourceLocation("textures/mob_effect/regeneration.png");

    public ImmersiveBeacon() {
        super(1);
    }

    @Override
    protected void render(BeaconInfo info, PoseStack stack, boolean isInVR) {
        float itemSize = ClientConstants.itemScaleSizeBeacon / info.getItemTransitionCountdown();

        renderItem(info.items[0], stack, info.getPosition(0), info.slotHovered == 0 ? itemSize * 1.25f : itemSize,
                info.lastPlayerDir.getOpposite(), null, info.getHitbox(0),
                false, -1);
        for (int i = 0; i < info.triggerBoxes.length; i++) {
            if (info.triggerBoxes[i] != null) {
                renderHitbox(stack, info.triggerBoxes[i], info.triggerBoxes[i].getCenter());
            }
        }

        for (int i = 0; i <= 4; i++) {
            if (info.triggerBoxes[i] != null) {
                renderImage(stack, effectLocations[i], info.triggerBoxes[i].getCenter().add(0, -0.05, 0), info.lastPlayerDir,
                        info.triggerHitboxSlotHovered == i ? 0.25f : 0.2f);
            }
        }

        if (info.effectSelected != -1) {
            renderImage(stack, effectLocations[info.effectSelected], info.effectSelectedDisplayPos.add(0, -0.05, 0),
                    info.lastPlayerDir, (float) displayHitboxSize);
        }

        if (info.triggerBoxes[6] != null) { // Regen and plus
            renderImage(stack, regenerationLocation, info.triggerBoxes[5].getCenter().add(0, -0.05, 0),
                    info.lastPlayerDir, (float) displayHitboxSize);
            // TODO: Render plus
        }
    }

    protected void setHitboxesAndPositions(BeaconInfo info) {
        if (Minecraft.getInstance().level.getBlockEntity(info.getBlockPosition()) instanceof BeaconBlockEntity beacon) {

            // NOTE: Unlike most other places in ImmersiveMC, left refers to left from the player's
            // perspective, not the block's!
            Vec3 center = Vec3.atCenterOf(info.getBlockPosition()).add(0, 1, 0);
            Direction forward = getForwardFromPlayer(Minecraft.getInstance().player);
            Vec3 forwardFromBlockVec = new Vec3(forward.getNormal().getX(), forward.getNormal().getY(),
                    forward.getNormal().getZ());
            Direction left = forward.getClockWise();

            Vec3 leftVec = new Vec3(left.getNormal().getX(), left.getNormal().getY(), left.getNormal().getZ());

            // For item input
            double itemHitboxSize = 0.42;
            info.setPosition(0, Vec3.atBottomCenterOf(info.getBlockPosition()).add(forwardFromBlockVec.scale(0.25)
                    .add(forwardFromBlockVec.scale(itemHitboxSize / 2d)).add(0, itemHitboxSize / 2d + 0.01, 0)));
            info.setHitbox(0, AABB.ofSize(info.getPosition(0),
                    itemHitboxSize, itemHitboxSize, itemHitboxSize));

            info.effectSelectedDisplayPos = center.add(0, 0.125, 0).add(leftVec.scale(-1d/3d));


            int beaconLevel = ((BeaconBlockEntityMixin) beacon).getLevels();
            if (beaconLevel > 0) {
                if (beaconLevel == 1) {
                    info.triggerBoxes[0] = AABB.ofSize(center.add(leftVec.scale(0.75)), effectHitboxSize, effectHitboxSize, effectHitboxSize);
                    info.triggerBoxes[1] = AABB.ofSize(center.add(leftVec.scale(0.5)), effectHitboxSize, effectHitboxSize, effectHitboxSize);
                    for (int i = 2; i < info.triggerBoxes.length; i++) {
                        info.triggerBoxes[i] = null;
                    }
                } else if (beaconLevel == 2) {
                    info.triggerBoxes[0] = AABB.ofSize(center.add(leftVec.scale(0.75).add(0, 0.125, 0)),
                            effectHitboxSize, effectHitboxSize, effectHitboxSize);
                    info.triggerBoxes[1] = AABB.ofSize(center.add(leftVec.scale(0.5).add(0, 0.125, 0)),
                            effectHitboxSize, effectHitboxSize, effectHitboxSize);
                    info.triggerBoxes[2] = AABB.ofSize(center.add(leftVec.scale(0.75).add(0, -0.125, 0)),
                            effectHitboxSize, effectHitboxSize, effectHitboxSize);
                    info.triggerBoxes[3] = AABB.ofSize(center.add(leftVec.scale(0.5).add(0, -0.125, 0)),
                            effectHitboxSize, effectHitboxSize, effectHitboxSize);
                    for (int i = 4; i < info.triggerBoxes.length; i++) {
                        info.triggerBoxes[i] = null;
                    }
                } else {
                    info.triggerBoxes[0] = AABB.ofSize(center.add(leftVec.scale(0.75).add(0, 0.25, 0)),
                            effectHitboxSize, effectHitboxSize, effectHitboxSize);
                    info.triggerBoxes[1] = AABB.ofSize(center.add(leftVec.scale(0.5).add(0, 0.25, 0)),
                            effectHitboxSize, effectHitboxSize, effectHitboxSize);
                    info.triggerBoxes[2] = AABB.ofSize(center.add(leftVec.scale(0.75)),
                            effectHitboxSize, effectHitboxSize, effectHitboxSize);
                    info.triggerBoxes[3] = AABB.ofSize(center.add(leftVec.scale(0.5)),
                            effectHitboxSize, effectHitboxSize, effectHitboxSize);
                    info.triggerBoxes[4] = AABB.ofSize(center.add(leftVec.scale(0.625).add(0, -0.25, 0)),
                            effectHitboxSize, effectHitboxSize, effectHitboxSize);
                    if (beaconLevel == 4) {
                        info.triggerBoxes[5] = AABB.ofSize(info.effectSelectedDisplayPos.add(leftVec.scale(-0.25)),
                                displayHitboxSize, displayHitboxSize, displayHitboxSize);
                        info.triggerBoxes[6] = AABB.ofSize(info.effectSelectedDisplayPos.add(0, -0.25, 0),
                                displayHitboxSize, displayHitboxSize, displayHitboxSize);
                    } else {
                        for (int i = 5; i < info.triggerBoxes.length; i++) {
                            info.triggerBoxes[i] = null;
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void doTick(BeaconInfo info, boolean isInVR) {
        super.doTick(info, isInVR);

        // Also refresh every second in case if beacon power changes
        if (info.lastPlayerDir != Minecraft.getInstance().player.getDirection()
            || Minecraft.getInstance().player.tickCount % 20 == 0) {
            setHitboxesAndPositions(info);
        }

        info.lastPlayerDir = Minecraft.getInstance().player.getDirection();
    }

    @Override
    protected boolean enabledInConfig() {
        return true; // TODO: Replace with config entry
    }

    @Override
    public void handleTriggerHitboxRightClick(InfoTriggerHitboxes info, Player player, int hitboxNum) {
        if (hitboxNum <= 4) {
            ((BeaconInfo) info).effectSelected = hitboxNum;
        } else {
            ((BeaconInfo) info).regenSelected = hitboxNum == 5;
        }
    }

    @Override
    public boolean shouldTrack(BlockPos pos, BlockState state, BlockEntity tileEntity, Level level) {
        return ImmersiveCheckers.isBeacon(pos, state, tileEntity, level);
    }

    @Override
    public AbstractImmersive<? extends AbstractImmersiveInfo> getSingleton() {
        return Immersives.immersiveBeacon;
    }

    @Override
    protected void initInfo(BeaconInfo info) {
        setHitboxesAndPositions(info);
    }

    @Override
    public void handleRightClick(AbstractImmersiveInfo info, Player player, int closest, InteractionHand hand) {
        Network.INSTANCE.sendToServer(new InteractPacket(info.getBlockPosition(), closest, hand));
    }

    @Override
    public void processStorageFromNetwork(AbstractWorldStorageInfo info, ImmersiveStorage storage) {
        info.items[0] = storage.items[0];
    }

    @Override
    public BeaconInfo getNewInfo(BlockPos pos) {
        return new BeaconInfo(pos);
    }

    @Override
    public int getTickTime() {
        return ClientConstants.ticksToRenderBeacon;
    }
}