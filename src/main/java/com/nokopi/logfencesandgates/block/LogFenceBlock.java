package com.nokopi.logfencesandgates.block;

import java.util.function.Supplier;

import com.nokopi.logfencesandgates.item.StrippedLogFenceItem;

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
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class LogFenceBlock extends FenceBlock {
    private final boolean flammable;
    // 樹皮を剥いだ版のアイテム（ブロックより後に登録されるので Supplier で受け取る）
    private final Supplier<? extends Item> strippedItem;

    public LogFenceBlock(Properties properties, boolean flammable, Supplier<? extends Item> strippedItem) {
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
        InteractionResult result = stripWithAxe(stack, state, level, pos, player, hand, hitResult, hasRails(state));
        return result != null ? result : super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    /**
     * 斧でクリックした部位の樹皮を剥ぐ。斧でなければ null（呼び出し元で通常の処理に回す）。
     * Diagonal Fences 連携の斜め版フェンスからも使う。
     */
    public static @Nullable InteractionResult stripWithAxe(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hitResult, boolean hasRails) {
        if (!BarkStripping.isAxe(stack)) {
            return null;
        }
        Vec3 local = hitResult.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
        FencePart part = FencePart.fromHit(local.x, local.y, local.z, hasRails);
        if (BarkStripping.isStripped(state, part)) {
            // 剥がれ済みなら何もせず、通常の右クリック（リードを結ぶなど）に回す
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        BarkStripping.strip(state, part, level, pos, player, stack, hand);
        return InteractionResult.SUCCESS;
    }

    public boolean isFlammable() {
        return this.flammable;
    }

    public Supplier<? extends Item> strippedItem() {
        return this.strippedItem;
    }

    private static boolean hasRails(BlockState state) {
        return state.getValue(NORTH) || state.getValue(EAST) || state.getValue(SOUTH) || state.getValue(WEST);
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
