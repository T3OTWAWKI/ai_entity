
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.aientity.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.common.ForgeSpawnEggItem;

import net.minecraft.world.item.Item;

import net.mcreator.aientity.item.CorruptedEssenceItem;
import net.mcreator.aientity.AiEntityMod;

public class AiEntityModItems {
	public static final DeferredRegister<Item> REGISTRY = DeferredRegister.create(ForgeRegistries.ITEMS, AiEntityMod.MODID);
	public static final RegistryObject<Item> C_2_SPAWN_EGG = REGISTRY.register("c_2_spawn_egg", () -> new ForgeSpawnEggItem(AiEntityModEntities.C_2, -16777216, -13434880, new Item.Properties()));
	public static final RegistryObject<Item> CORRUPTED_ESSENCE = REGISTRY.register("corrupted_essence", () -> new CorruptedEssenceItem());
	// Start of user code block custom items
	// End of user code block custom items
}
