package com.byt3.mixins;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.gameplay.GameplayConfig;
import com.hypixel.hytale.server.core.asset.type.gameplay.WorldConfig;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemTool;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.BlockHarvestUtils;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.BreakBlockInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;


@Mixin(BreakBlockInteraction.class)
public abstract class BreakBlockInteractionMixin extends SimpleBlockInteraction {

    @Shadow
    protected boolean harvest;
    @Shadow @Nullable
    protected String toolId;
    @Shadow protected boolean matchTool;

    @Inject(method = "interactWithBlock", at = @At("HEAD"), cancellable = true)
    protected void interactWithBlock(World world, CommandBuffer<EntityStore> commandBuffer, InteractionType type, InteractionContext context, ItemStack heldItemStack, Vector3i targetBlock, CooldownHandler cooldownHandler, CallbackInfo ci) {
        Ref<EntityStore> ref = context.getEntity();
        // Check if the entity is an NPC (The logic we want to add)
        LivingEntity npc = (LivingEntity) commandBuffer.getComponent(ref, NPCEntity.getComponentType());

        // If it is an NPC, we perform the logic here and CANCEL the original method.
        // This prevents the original code (which casts to Player) from running.
        if (npc != null) {
            ChunkStore chunkStore = world.getChunkStore();
            Store<ChunkStore> chunkStoreStore = chunkStore.getStore();
            long chunkIndex = ChunkUtil.indexChunkFromBlock(targetBlock.x, targetBlock.z);
            Ref<ChunkStore> chunkReference = chunkStore.getChunkReference(chunkIndex);

            if (chunkReference != null && chunkReference.isValid()) {
                WorldChunk worldChunkComponent = (WorldChunk) chunkStoreStore.getComponent(chunkReference, WorldChunk.getComponentType());

                // We can use simple null checks instead of assertions in mixins to be safe
                if (worldChunkComponent == null) return;

                BlockChunk blockChunkComponent = (BlockChunk) chunkStoreStore.getComponent(chunkReference, BlockChunk.getComponentType());
                if (blockChunkComponent == null) return;

                BlockSection blockSection = blockChunkComponent.getSectionAtBlockY(targetBlock.getY());
                GameplayConfig gameplayConfig = world.getGameplayConfig();
                WorldConfig worldConfig = gameplayConfig.getWorldConfig();

                if (this.harvest) {
                    // Harvest Logic (Identical to original, but using our variables)
                    int x = targetBlock.getX();
                    int y = targetBlock.getY();
                    int z = targetBlock.getZ();
                    BlockType blockType = worldChunkComponent.getBlockType(x, y, z);
                    if (blockType == null) {
                        context.getState().state = InteractionState.Failed;
                        ci.cancel(); return;
                    }

                    if (!worldConfig.isBlockGatheringAllowed()) {
                        context.getState().state = InteractionState.Failed;
                        ci.cancel(); return;
                    }

                    if (!BlockHarvestUtils.shouldPickupByInteraction(blockType)) {
                        context.getState().state = InteractionState.Failed;
                        ci.cancel(); return;
                    }

                    int filler = blockSection.getFiller(x, y, z);
                    BlockHarvestUtils.performPickupByInteraction(ref, targetBlock, blockType, filler, chunkReference, commandBuffer, chunkStoreStore);
                } else {
                    // Block Breaking Logic
                    boolean blockBreakingAllowed = worldConfig.isBlockBreakingAllowed();
                    if (!blockBreakingAllowed) {
                        context.getState().state = InteractionState.Failed;
                        ci.cancel(); return;
                    }

                    // NPC Logic: We skip the GameMode switch because NPCs don't have GameModes.
                    // We default to the standard "Survival" behavior (performBlockDamage).
                    BlockHarvestUtils.performBlockDamage(npc, ref, targetBlock, heldItemStack, (ItemTool) null, this.toolId, this.matchTool, 1.0F, 0, chunkReference, commandBuffer, chunkStoreStore);
                }
            }

            // Short Circuit: Cancel the original method so it doesn't crash trying to cast NPC to Player.
            ci.cancel();
        }

        // If npc == null, we do NOT cancel.
        // The original method runs, finds the Player component (or handles null), and executes as normal.
    }
}