package slimeknights.tconstruct.blocks;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ObjectHolder;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.registry.BlockItemRegistryAdapter;
import slimeknights.tconstruct.common.registry.BlockRegistryAdapter;
import slimeknights.tconstruct.library.TinkerRegistry;
import slimeknights.tconstruct.tables.block.chest.PartChestBlock;
import slimeknights.tconstruct.tables.block.chest.PatternChestBlock;

import static slimeknights.tconstruct.common.TinkerPulse.injected;

@SuppressWarnings("unused")
@ObjectHolder(TConstruct.modID)
@Mod.EventBusSubscriber(modid = TConstruct.modID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class TableBlocks {

  public static final Block pattern_chest = injected();
  public static final Block part_chest = injected();

  @SubscribeEvent
  static void registerBlocks(final RegistryEvent.Register<Block> event) {
    BlockRegistryAdapter registry = new BlockRegistryAdapter(event.getRegistry());

    registry.register(new PatternChestBlock(BlockProperties.TOOL_TABLE), "pattern_chest");
    registry.register(new PartChestBlock(BlockProperties.TOOL_TABLE), "part_chest");
  }

  @SubscribeEvent
  static void registerBlockItems(final RegistryEvent.Register<Item> event) {
    BlockItemRegistryAdapter registry = new BlockItemRegistryAdapter(event.getRegistry(), TinkerRegistry.tabGeneral);

    registry.registerBlockItem(pattern_chest);
    registry.registerBlockItem(part_chest);
  }

  private TableBlocks() {
  }
}
