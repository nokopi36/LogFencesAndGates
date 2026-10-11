package com.nokopi.logfencesandgates.compat.diagonalfences;

import com.nokopi.logfencesandgates.LogFencesAndGates;
import com.nokopi.logfencesandgates.LogWood;
import com.nokopi.logfencesandgates.ModBlocks;
import com.nokopi.logfencesandgates.ModItems;
import com.nokopi.logfencesandgates.block.LogFenceBlock;

import fuzs.diagonalblocks.common.api.v2.block.type.DiagonalBlockTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

/**
 * Diagonal Fences（Diagonal Blocks）との連携。このクラスは Diagonal Blocks が読み込まれているときだけ呼ぶこと
 * （Diagonal Blocks のクラスを参照するため、無いときに読み込むとクラスが見つからずに落ちる）。
 */
public final class DiagonalFencesCompat {
    public static final String DIAGONAL_BLOCKS_MOD_ID = "diagonalblocks";

    private DiagonalFencesCompat() {
    }

    /**
     * 原木のフェンスの斜め版の作り方を登録する。ブロックの登録より前（MOD のコンストラクタ）で呼ぶ。
     * 状態の項目がバニラと違うため Diagonal Blocks は自動では対象にしないので、剥ぎの状態を持つ独自の斜め版を使わせる。
     */
    public static void registerDiagonalFences() {
        for (LogWood wood : LogWood.values()) {
            Identifier fence = Identifier.fromNamespaceAndPath(LogFencesAndGates.MODID, wood.fenceName());
            DiagonalBlockTypes.FENCE.registerBlockFactory(fence, (Block base) -> {
                LogFenceBlock logFence = (LogFenceBlock) base;
                return properties -> new DiagonalLogFenceBlock(properties, logFence.isFlammable(), logFence.strippedItem());
            });
        }

        // Diagonal Fences はタグの読み込み時（最優先）にすべての BlockItem を斜め版に向け直す。その副作用で、
        // 同じブロックを置く「剥いだ版」が最後に処理され、元のフェンスの asItem() が剥いだ版になってしまう。
        // その後（最後）に、元のフェンスの対応を普通の版に戻す
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, TagsUpdatedEvent.ServerDataLoad.class, event -> restoreFenceItems());
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, TagsUpdatedEvent.ClientPacketReceived.class, event -> restoreFenceItems());
    }

    // Block#item はアクセストランスフォーマー（META-INF/accesstransformer.cfg）で公開している
    private static void restoreFenceItems() {
        for (LogWood wood : LogWood.values()) {
            ModBlocks.fence(wood).get().item = ModItems.fence(wood).get();
        }
    }
}
