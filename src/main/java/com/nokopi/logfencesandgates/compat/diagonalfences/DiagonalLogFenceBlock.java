package com.nokopi.logfencesandgates.compat.diagonalfences;

import java.util.function.Supplier;

import com.nokopi.logfencesandgates.block.BarkStripping;
import com.nokopi.logfencesandgates.block.FencePart;
import com.nokopi.logfencesandgates.block.LogFenceBlock;
import com.nokopi.logfencesandgates.block.LogFenceProperties;
import com.nokopi.logfencesandgates.item.StrippedLogFenceItem;

import fuzs.diagonalblocks.common.api.v2.block.DiagonalBlock;
import fuzs.diagonalblocks.common.api.v2.block.DiagonalFenceBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Diagonal Fences が作る斜め版の原木のフェンス。剥ぎの状態・斧の処理・燃えやすさ・ピックブロックは {@link LogFenceBlock} と同じ。
 * Diagonal Blocks が読み込まれているときだけ使う（{@link DiagonalFencesCompat}）。
 */
public class DiagonalLogFenceBlock extends DiagonalFenceBlock {
    private final boolean flammable;
    private final Supplier<? extends Item> strippedItem;

    public DiagonalLogFenceBlock(Properties properties, boolean flammable, Supplier<? extends Item> strippedItem) {
        super(properties);
        this.flammable = flammable;
        this.strippedItem = strippedItem;
        this.registerDefaultState(BarkStripping.withAllBark(this.defaultBlockState(), FencePart.values()));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        for (FencePart part : FencePart.values()) {
            builder.add(part.strippedProperty());
        }
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hitResult) {
        InteractionResult result = LogFenceBlock.stripWithAxe(stack, state, level, pos, player, hand, hitResult, hasRails(state));
        return result != null ? result : super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    // 斜め方向にだけ繋がっている場合も横木あり
    private static boolean hasRails(BlockState state) {
        return state.getValue(DiagonalBlock.NORTH) || state.getValue(DiagonalBlock.EAST)
                || state.getValue(DiagonalBlock.SOUTH) || state.getValue(DiagonalBlock.WEST)
                || state.getValue(DiagonalBlock.NORTH_EAST) || state.getValue(DiagonalBlock.SOUTH_EAST)
                || state.getValue(DiagonalBlock.SOUTH_WEST) || state.getValue(DiagonalBlock.NORTH_WEST);
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return this.flammable ? LogFenceProperties.FLAMMABILITY : 0;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return this.flammable ? LogFenceProperties.FIRE_SPREAD_SPEED : 0;
    }

    // 全部位が剥がれていれば、ピックブロックで剥いだ版を取る
    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData, Player player) {
        if (StrippedLogFenceItem.isAllStripped(state, FencePart.values())) {
            return new ItemStack(this.strippedItem.get());
        }
        return super.getCloneItemStack(level, pos, state, includeData, player);
    }
}
