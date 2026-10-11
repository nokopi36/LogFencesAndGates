package com.nokopi.logfencesandgates.datagen;

import java.util.Set;

import com.nokopi.logfencesandgates.LogWood;
import com.nokopi.logfencesandgates.ModBlocks;
import com.nokopi.logfencesandgates.ModItems;
import com.nokopi.logfencesandgates.block.FencePart;
import com.nokopi.logfencesandgates.block.GatePart;
import com.nokopi.logfencesandgates.block.StrippablePart;

import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.advancements.predicates.BlockPredicate;
import net.minecraft.advancements.predicates.StatePropertiesPredicate;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.MatchBlock;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

public class ModBlockLootSubProvider extends BlockLootSubProvider {
    public ModBlockLootSubProvider(LootTableSubProvider.Context context) {
        super(Set.of(), FeatureFlags.DEFAULT_FLAGS, context);
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(e -> (Block) e.value()).toList();
    }

    @Override
    protected void generate() {
        for (LogWood wood : LogWood.values()) {
            this.add(ModBlocks.fence(wood).get(), this.createLogFenceTable(ModBlocks.fence(wood).get(),
                    ModItems.strippedFence(wood).get(), FencePart.values()));
            this.add(ModBlocks.fenceGate(wood).get(), this.createLogFenceTable(ModBlocks.fenceGate(wood).get(),
                    ModItems.strippedFenceGate(wood).get(), GatePart.values()));
        }
    }

    // 全部位が剥がれていれば剥いだ版、そうでなければ普通の版を落とす。
    // Diagonal Fences の斜め版ブロックもこのルートテーブルを使うので、ブロックの種類は条件に入れない
    private LootTable.Builder createLogFenceTable(Block block, Item strippedItem, StrippablePart[] parts) {
        StatePropertiesPredicate.Builder allStripped = StatePropertiesPredicate.Builder.properties();
        for (StrippablePart part : parts) {
            allStripped.hasProperty(part.strippedProperty(), true);
        }
        return LootTable.lootTable().withPool(this.applyExplosionCondition(block, LootPool.lootPool()
                .setRolls(ContextIntProviders.exactly(1))
                .add(LootItem.lootTableItem(strippedItem)
                        .when(MatchBlock.blockMatches(BlockPredicate.Builder.block().setProperties(allStripped)))
                        .otherwise(LootItem.lootTableItem(block)))));
    }
}
