package com.nokopi.logfencesandgates.gametest;

import java.util.List;
import java.util.function.Consumer;

import com.nokopi.logfencesandgates.LogFencesAndGates;
import com.nokopi.logfencesandgates.LogWood;
import com.nokopi.logfencesandgates.ModBlocks;
import com.nokopi.logfencesandgates.ModItems;
import com.nokopi.logfencesandgates.datagen.ModRecipeProvider;
import com.nokopi.logfencesandgates.block.FencePart;
import com.nokopi.logfencesandgates.compat.diagonalfences.DiagonalFencesCompatTests;
import com.nokopi.logfencesandgates.block.GatePart;
import com.nokopi.logfencesandgates.block.StrippablePart;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestEnvironments;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, LogFencesAndGates.MODID);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FENCE_PART_DETECTION =
            TEST_FUNCTIONS.register("fence_part_detection", () -> ModGameTests::fencePartDetection);
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> STRIP_EACH_PART =
            TEST_FUNCTIONS.register("strip_each_part", () -> ModGameTests::stripEachPart);
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> GATE_PART_DETECTION =
            TEST_FUNCTIONS.register("gate_part_detection", () -> ModGameTests::gatePartDetection);
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> GATE_STRIP_AND_TOGGLE =
            TEST_FUNCTIONS.register("gate_strip_and_toggle", () -> ModGameTests::gateStripAndToggle);
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ALL_WOODS_REGISTERED =
            TEST_FUNCTIONS.register("all_woods_registered", () -> ModGameTests::allWoodsRegistered);
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> STRIPPED_PLACE_AND_DROP =
            TEST_FUNCTIONS.register("stripped_place_and_drop", () -> ModGameTests::strippedPlaceAndDrop);
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RECIPE_COUNTS =
            TEST_FUNCTIONS.register("recipe_counts", () -> ModGameTests::recipeCounts);
    // Diagonal Fences が無いときはすぐ成功にする（テストの定義は jar に含まれるので、関数は常に登録しておく）
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DIAGONAL_FENCES_COMPAT =
            TEST_FUNCTIONS.register("diagonal_fences_compat", () -> ModGameTests::diagonalFencesCompat);

    // data/log_fences_and_gates/structure/test_area.nbt（空の 3x3x3）
    private static final Identifier TEST_AREA = Identifier.fromNamespaceAndPath(LogFencesAndGates.MODID, "test_area");
    private static final int MAX_TICKS = 20;

    private ModGameTests() {
    }

    // datagen から呼ぶ: 登録済みのテスト関数ごとにテストインスタンスを作る
    public static void bootstrapInstances(BootstrapContext<GameTestInstance> context) {
        HolderGetter<TestEnvironmentDefinition<?>> environments = context.lookup(Registries.TEST_ENVIRONMENT);
        for (DeferredHolder<Consumer<GameTestHelper>, ? extends Consumer<GameTestHelper>> function : TEST_FUNCTIONS.getEntries()) {
            ResourceKey<GameTestInstance> key = ResourceKey.create(Registries.TEST_INSTANCE, function.getId());
            context.register(key, new FunctionGameTestInstance(function.getKey(),
                    new TestData<>(environments.getOrThrow(GameTestEnvironments.DEFAULT_KEY), TEST_AREA, MAX_TICKS, 0, true)));
        }
    }

    // SPEC 3.2: クリック位置から部位を判定する
    private static void fencePartDetection(GameTestHelper helper) {
        double postFace = 6.0 / 16.0;
        // 柱の表面（北面）・柱の上面
        assertPart(helper, FencePart.POST, postFace, 0.5, 0.5, true);
        assertPart(helper, FencePart.POST, 0.5, 1.0, 0.5, true);
        // 柱の範囲外: 上の横木の高さ・下の横木の高さ
        assertPart(helper, FencePart.UPPER_RAIL, 0.5, 13.5 / 16.0, 0.1, true);
        assertPart(helper, FencePart.LOWER_RAIL, 0.9, 7.5 / 16.0, 0.5, true);
        // 横木の隙間は近いほうの横木
        assertPart(helper, FencePart.UPPER_RAIL, 0.5, 11.0 / 16.0, 0.1, true);
        assertPart(helper, FencePart.LOWER_RAIL, 0.5, 10.0 / 16.0, 0.1, true);
        // どこにも繋がっていなければ柱
        assertPart(helper, FencePart.POST, 0.5, 13.5 / 16.0, 0.1, false);
        helper.succeed();
    }

    private static void assertPart(GameTestHelper helper, FencePart expected, double x, double y, double z, boolean hasRails) {
        FencePart actual = FencePart.fromHit(x, y, z, hasRails);
        helper.assertTrue(actual == expected,
                "(" + x + ", " + y + ", " + z + ", rails=" + hasRails + ") expected " + expected + " but was " + actual);
    }

    // SPEC 3.2: 斧でクリックした部位だけ剥がれ、剥がれ済みの部位では斧を消費しない
    private static void stripEachPart(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.fence(LogWood.OAK).get().defaultBlockState().setValue(FenceBlock.EAST, true));

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack axe = new ItemStack(Items.IRON_AXE);
        player.setItemInHand(InteractionHand.MAIN_HAND, axe);

        click(helper, player, pos, new Vec3(0.9, 13.5 / 16.0, 0.5), Direction.UP);
        assertStripped(helper, pos, false, true, false);

        click(helper, player, pos, new Vec3(0.9, 7.5 / 16.0, 0.5), Direction.SOUTH);
        assertStripped(helper, pos, false, true, true);

        click(helper, player, pos, new Vec3(0.5, 0.5, 6.0 / 16.0), Direction.NORTH);
        assertStripped(helper, pos, true, true, true);
        helper.assertTrue(axe.getDamageValue() == 3, "axe damage should be 3 but was " + axe.getDamageValue());

        // 剥がれ済みの柱をもう一度クリックしても変化なし・斧も消費しない
        click(helper, player, pos, new Vec3(0.5, 0.5, 6.0 / 16.0), Direction.NORTH);
        helper.assertTrue(axe.getDamageValue() == 3, "axe should not be damaged on a stripped part but was " + axe.getDamageValue());

        // 斧以外では剥がれない
        BlockPos other = new BlockPos(1, 1, 0);
        helper.setBlock(other, ModBlocks.fence(LogWood.OAK).get().defaultBlockState());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        click(helper, player, other, new Vec3(0.5, 0.5, 6.0 / 16.0), Direction.NORTH);
        assertStripped(helper, other, false, false, false);
        helper.succeed();
    }

    private static void click(GameTestHelper helper, Player player, BlockPos relativePos, Vec3 localHit, Direction face) {
        BlockPos absolutePos = helper.absolutePos(relativePos);
        Vec3 location = Vec3.atLowerCornerOf(absolutePos).add(localHit);
        helper.useBlock(relativePos, player, new BlockHitResult(location, face, absolutePos, false));
    }

    private static void assertStripped(GameTestHelper helper, BlockPos pos, boolean post, boolean upper, boolean lower) {
        BlockState state = helper.getBlockState(pos);
        helper.assertTrue(state.getValue(FencePart.POST.strippedProperty()) == post
                        && state.getValue(FencePart.UPPER_RAIL.strippedProperty()) == upper
                        && state.getValue(FencePart.LOWER_RAIL.strippedProperty()) == lower,
                "expected post=" + post + " upper=" + upper + " lower=" + lower + " but was " + state);
    }

    // SPEC 3.3: ゲートの部位判定（along はゲートの長さ方向）
    private static void gatePartDetection(GameTestHelper helper) {
        // 両端の柱
        assertGatePart(helper, GatePart.POST, 1.0 / 16.0, 13.5 / 16.0, false);
        assertGatePart(helper, GatePart.POST, 15.0 / 16.0, 7.5 / 16.0, false);
        // 扉の内側の縦木（左右どちらも）
        assertGatePart(helper, GatePart.INNER_POST, 7.0 / 16.0, 13.5 / 16.0, false);
        assertGatePart(helper, GatePart.INNER_POST, 9.0 / 16.0, 7.5 / 16.0, false);
        // 扉の横木
        assertGatePart(helper, GatePart.UPPER_RAIL, 4.0 / 16.0, 13.5 / 16.0, false);
        assertGatePart(helper, GatePart.LOWER_RAIL, 12.0 / 16.0, 7.5 / 16.0, false);
        // 塀付き（3/16 下がる）: 上の横木 y 9〜12/16、下の横木 y 3〜6/16
        assertGatePart(helper, GatePart.UPPER_RAIL, 4.0 / 16.0, 10.5 / 16.0, true);
        assertGatePart(helper, GatePart.LOWER_RAIL, 4.0 / 16.0, 4.5 / 16.0, true);
        helper.succeed();
    }

    private static void assertGatePart(GameTestHelper helper, GatePart expected, double along, double y, boolean inWall) {
        GatePart actual = GatePart.fromHit(along, y, inWall);
        helper.assertTrue(actual == expected,
                "gate(" + along + ", " + y + ", wall=" + inWall + ") expected " + expected + " but was " + actual);
    }

    // SPEC 3.3: 閉じたゲートは斧で部位を剥ぎ、剥がれ済みの部位や開いたゲートでは開閉する
    private static void gateStripAndToggle(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        // 東向き: ゲートは z 方向に伸びる
        helper.setBlock(pos, ModBlocks.fenceGate(LogWood.OAK).get().defaultBlockState().setValue(FenceGateBlock.FACING, Direction.EAST));

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack axe = new ItemStack(Items.IRON_AXE);
        player.setItemInHand(InteractionHand.MAIN_HAND, axe);

        // z = 4/16 の上の横木
        click(helper, player, pos, new Vec3(0.5, 13.5 / 16.0, 4.0 / 16.0), Direction.EAST);
        assertGateStripped(helper, pos, false, false, true, false);
        helper.assertBlockProperty(pos, FenceGateBlock.OPEN, false);

        // 剥がれ済みの上の横木をもう一度 → 開く（斧は減らない）
        click(helper, player, pos, new Vec3(0.5, 13.5 / 16.0, 4.0 / 16.0), Direction.EAST);
        helper.assertBlockProperty(pos, FenceGateBlock.OPEN, true);
        helper.assertTrue(axe.getDamageValue() == 1, "axe damage should be 1 but was " + axe.getDamageValue());

        // 開いているときは柱を狙っても剥がれず、閉じる
        click(helper, player, pos, new Vec3(0.5, 0.5, 1.0 / 16.0), Direction.EAST);
        helper.assertBlockProperty(pos, FenceGateBlock.OPEN, false);
        assertGateStripped(helper, pos, false, false, true, false);

        // 閉じた状態で内側の縦木 → 内側の縦木だけ剥がれ、両端の柱は残る
        click(helper, player, pos, new Vec3(0.5, 0.5, 8.0 / 16.0), Direction.EAST);
        assertGateStripped(helper, pos, false, true, true, false);

        // 両端の柱
        click(helper, player, pos, new Vec3(0.5, 0.5, 15.0 / 16.0), Direction.EAST);
        assertGateStripped(helper, pos, true, true, true, false);
        helper.assertTrue(axe.getDamageValue() == 3, "axe damage should be 3 but was " + axe.getDamageValue());
        helper.succeed();
    }

    private static void assertGateStripped(GameTestHelper helper, BlockPos pos, boolean post, boolean inner, boolean upper, boolean lower) {
        BlockState state = helper.getBlockState(pos);
        helper.assertTrue(state.getValue(GatePart.POST.strippedProperty()) == post
                        && state.getValue(GatePart.INNER_POST.strippedProperty()) == inner
                        && state.getValue(GatePart.UPPER_RAIL.strippedProperty()) == upper
                        && state.getValue(GatePart.LOWER_RAIL.strippedProperty()) == lower,
                "expected post=" + post + " inner=" + inner + " upper=" + upper + " lower=" + lower + " but was " + state);
    }

    // SPEC 3.4: 全種類のフェンス・ゲートに剥ぎ用の状態・ドロップ・レシピがある
    private static void allWoodsRegistered(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        for (LogWood wood : LogWood.values()) {
            for (Block block : List.of(ModBlocks.fence(wood).get(), ModBlocks.fenceGate(wood).get())) {
                for (StrippablePart part : block instanceof FenceGateBlock ? GatePart.values() : FencePart.values()) {
                    helper.assertTrue(block.defaultBlockState().hasProperty(part.strippedProperty()),
                            block + " has no " + part.strippedProperty().getName());
                }
                ResourceKey<LootTable> lootTable = block.getLootTable().orElseThrow();
                helper.assertTrue(server.reloadableRegistries().getLootTable(lootTable) != LootTable.EMPTY, block + " has no loot table");
                Identifier id = BuiltInRegistries.BLOCK.getKey(block);
                helper.assertTrue(server.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, id)).isPresent(), id + " has no recipe");
            }
            for (Item stripped : List.of(ModItems.strippedFence(wood).get(), ModItems.strippedFenceGate(wood).get())) {
                Identifier id = BuiltInRegistries.ITEM.getKey(stripped);
                helper.assertTrue(server.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, id)).isPresent(), id + " has no recipe");
            }
            // 剥いだ版を登録しても、ブロック → アイテムの対応は普通の版のまま
            helper.assertTrue(ModBlocks.fence(wood).get().asItem() == ModItems.fence(wood).get(), wood + " fence asItem is not the normal item");
            helper.assertTrue(ModBlocks.fenceGate(wood).get().asItem() == ModItems.fenceGate(wood).get(), wood + " gate asItem is not the normal item");
        }
        helper.succeed();
    }

    // SPEC 3.5: 剥いだ版は全部位が剥がれた状態で置かれ、全部剥がれていれば剥いだ版をドロップする
    private static void strippedPlaceAndDrop(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        BlockPos fencePos = placeOnFloor(helper, player, new BlockPos(0, 0, 1), new ItemStack(ModItems.strippedFence(LogWood.OAK).get()));
        assertStripped(helper, fencePos, true, true, true);
        BlockPos gatePos = placeOnFloor(helper, player, new BlockPos(2, 0, 1), new ItemStack(ModItems.strippedFenceGate(LogWood.OAK).get()));
        assertGateStripped(helper, gatePos, true, true, true, true);

        assertDrop(helper, helper.getBlockState(fencePos), ModItems.strippedFence(LogWood.OAK).get());
        assertDrop(helper, helper.getBlockState(gatePos), ModItems.strippedFenceGate(LogWood.OAK).get());
        // 一部だけ剥がれたものは普通の版
        assertDrop(helper, helper.getBlockState(fencePos).setValue(FencePart.LOWER_RAIL.strippedProperty(), false), ModItems.fence(LogWood.OAK).get());
        assertDrop(helper, helper.getBlockState(gatePos).setValue(GatePart.INNER_POST.strippedProperty(), false), ModItems.fenceGate(LogWood.OAK).get());
        helper.succeed();
    }

    private static BlockPos placeOnFloor(GameTestHelper helper, Player player, BlockPos floor, ItemStack stack) {
        helper.setBlock(floor, Blocks.STONE);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        click(helper, player, floor, new Vec3(0.5, 1.0, 0.5), Direction.UP);
        return floor.above();
    }

    private static void assertDrop(GameTestHelper helper, BlockState state, Item expected) {
        ServerLevel level = helper.getLevel();
        List<ItemStack> drops = Block.getDrops(state, level, helper.absolutePos(BlockPos.ZERO), null);
        helper.assertTrue(drops.size() == 1 && drops.get(0).is(expected), state + " should drop " + expected + " but dropped " + drops);
    }

    // SPEC 4 章・3.7: レシピの個数（原木 12・4、竹ブロック 6・2）
    private static void recipeCounts(GameTestHelper helper) {
        assertRecipeCount(helper, ModItems.fence(LogWood.OAK).get(), 12);
        assertRecipeCount(helper, ModItems.fenceGate(LogWood.OAK).get(), 4);
        assertRecipeCount(helper, ModItems.fence(LogWood.BAMBOO).get(), 6);
        assertRecipeCount(helper, ModItems.fenceGate(LogWood.BAMBOO).get(), 2);
        assertRecipeCount(helper, ModItems.strippedFence(LogWood.BAMBOO).get(), 6);
        assertRecipeCount(helper, ModItems.strippedFenceGate(LogWood.BAMBOO).get(), 2);
        for (LogWood wood : LogWood.values()) {
            assertRecipeCount(helper, ModItems.fence(wood).get(), ModRecipeProvider.fenceCount(wood));
            assertRecipeCount(helper, ModItems.fenceGate(wood).get(), ModRecipeProvider.fenceGateCount(wood));
        }
        helper.succeed();
    }

    @SuppressWarnings("unchecked")
    private static void assertRecipeCount(GameTestHelper helper, Item item, int expected) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        Recipe<?> recipe = helper.getLevel().getServer().getRecipeManager()
                .byKey(ResourceKey.create(Registries.RECIPE, id)).orElseThrow().value();
        ItemStack result = ((Recipe<CraftingInput>) recipe).assemble(CraftingInput.EMPTY);
        helper.assertTrue(result.is(item) && result.getCount() == expected,
                id + " recipe should make " + expected + " but makes " + result);
    }

    // SPEC 3.8: Diagonal Fences 連携
    private static void diagonalFencesCompat(GameTestHelper helper) {
        if (!LogFencesAndGates.isDiagonalBlocksLoaded()) {
            helper.succeed();
            return;
        }
        DiagonalFencesCompatTests.diagonalLogFences(helper);
    }
}
