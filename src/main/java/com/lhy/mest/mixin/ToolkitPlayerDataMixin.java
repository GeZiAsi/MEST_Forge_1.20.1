package com.lhy.mest.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

import com.lhy.mest.terminal.ToolkitDataHolder;
import com.lhy.mest.terminal.ToolkitInternalInventory;

/** Stores the player's toolkit on the player entity and in its save data. */
@Mixin(Player.class)
public abstract class ToolkitPlayerDataMixin implements ToolkitDataHolder {
    @Unique
    private static final String MEST$TAG = "mesplicedterminal:toolkit";

    @Unique
    private ToolkitInternalInventory mest$toolkit;

    @Override
    public ToolkitInternalInventory mest$toolkit() {
        if (mest$toolkit == null) {
            mest$toolkit = new ToolkitInternalInventory((Player) (Object) this);
        }
        return mest$toolkit;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void mest$saveToolkit(CompoundTag tag, CallbackInfo ci) {
        if (mest$toolkit != null) {
            tag.put(MEST$TAG, mest$toolkit.save());
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void mest$loadToolkit(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains(MEST$TAG, Tag.TAG_COMPOUND)) {
            mest$toolkit().load(tag.getCompound(MEST$TAG));
        }
    }
}
