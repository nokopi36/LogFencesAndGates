package com.nokopi.logfencesandgates.compat.diagonalfences;

import java.util.List;

import com.nokopi.logfencesandgates.LogWood;
import com.nokopi.logfencesandgates.ModBlocks;
import com.nokopi.logfencesandgates.ModItems;
import com.nokopi.logfencesandgates.block.FencePart;

import fuzs.diagonalblocks.common.api.v2.block.DiagonalBlock;
import fuzs.diagonalblocks.common.api.v2.block.type.DiagonalBlockTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

// Diagonal Blocks が読み込まれているときだけ ModGameTests から呼ぶ
public final class DiagonalFencesCompatTests {
    private DiagonalFencesCompatTests() {
    }

    // SPEC 3.8: 斜め版は剥ぎの状態を持ち、斜めの横木も剥げ、全部剥がれていれば剥いだ版を落とす
    public static void diagonalLogFences(GameTestHelper helper) {
        for (LogWood wood : LogWood.values()) {
            Block diagonal = DiagonalBlockTypes.FENCE.getBlockConversions().get(ModBlocks.fence(wood).get());
            helper.assertTrue(diagonal instanceof DiagonalLogFenceBlock, wood + " has no DiagonalLogFenceBlock but " + diagonal);
            // 斜め版も元のフェンスも、ブロック → アイテムの対応は普通の版（剥いだ版にならない）
            helper.assertTrue(diagonal.asItem() == ModItems.fence(wood).get(), wood + " diagonal fence asItem is " + diagonal.asItem());
            helper.assertTrue(ModBlocks.fence(wood).get().asItem() == ModItems.fence(wood).get(), wood + " fence asItem is " + ModBlocks.fence(wood).get().asItem());
        }

        Block diagonal = DiagonalBlockTypes.FENCE.getBlockConversions().get(ModBlocks.fence(LogWood.OAK).get());
        BlockPos pos = new BlockPos(1, 1, 1);
        // 斜め方向（北東）にだけ繋がっている状態
        helper.setBlock(pos, diagonal.defaultBlockState().setValue(DiagonalBlock.NORTH_EAST, true));

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
        BlockPos absolute = helper.absolutePos(pos);
        // 柱の外側・上の横木の高さ（斜めの横木の上）をクリック
        Vec3 hit = Vec3.atLowerCornerOf(absolute).add(0.85, 13.5 / 16.0, 0.15);
        helper.useBlock(pos, player, new BlockHitResult(hit, Direction.UP, absolute, false));
        BlockState state = helper.getBlockState(pos);
        helper.assertTrue(state.getValue(FencePart.UPPER_RAIL.strippedProperty()) && !state.getValue(FencePart.POST.strippedProperty()),
                "diagonal upper rail should be stripped but was " + state);

        BlockState allStripped = state.setValue(FencePart.POST.strippedProperty(), true)
                .setValue(FencePart.LOWER_RAIL.strippedProperty(), true);
        List<ItemStack> drops = Block.getDrops(allStripped, helper.getLevel(), absolute, null);
        helper.assertTrue(drops.size() == 1 && drops.get(0).is(ModItems.strippedFence(LogWood.OAK).get()),
                "fully stripped diagonal fence should drop the stripped item but dropped " + drops);
        helper.succeed();
    }
}
