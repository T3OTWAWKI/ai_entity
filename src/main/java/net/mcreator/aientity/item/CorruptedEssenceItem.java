
package net.mcreator.aientity.item;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;

public class CorruptedEssenceItem extends Item {
	public CorruptedEssenceItem() {
		super(new Item.Properties().stacksTo(64).rarity(Rarity.RARE));
	}
}
