
package net.mcreator.aientity.client.renderer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.HumanoidModel;

import net.mcreator.aientity.entity.C2Entity;

public class C2Renderer extends HumanoidMobRenderer<C2Entity, HumanoidModel<C2Entity>> {
	public C2Renderer(EntityRendererProvider.Context context) {
		super(context, new HumanoidModel<C2Entity>(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
		this.addLayer(new HumanoidArmorLayer(this, new HumanoidModel(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)), new HumanoidModel(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager()));
	}

	@Override
	public ResourceLocation getTextureLocation(C2Entity entity) {
		return new ResourceLocation("ai_entity:textures/entities/my_entity.png");
	}
}
