package com.nokopi.logfencesandgates;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.nokopi.logfencesandgates.compat.diagonalfences.DiagonalFencesCompat;
import com.nokopi.logfencesandgates.gametest.ModGameTests;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;

@Mod(LogFencesAndGates.MODID)
public class LogFencesAndGates {
    public static final String MODID = "log_fences_and_gates";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LogFencesAndGates(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModGameTests.TEST_FUNCTIONS.register(modEventBus);
        modEventBus.addListener(ModCreativeTabs::addToVanillaTabs);

        // Diagonal Fences 連携（任意）。Diagonal Blocks が無いときは連携クラスを読み込まない
        if (isDiagonalBlocksLoaded()) {
            DiagonalFencesCompat.registerDiagonalFences();
        }
    }

    public static boolean isDiagonalBlocksLoaded() {
        return ModList.get().isLoaded(DiagonalFencesCompat.DIAGONAL_BLOCKS_MOD_ID);
    }
}
