package net.idothehax.firstpersonnom.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {

    @Shadow
    private MinecraftClient client;

    @Shadow private float prevEquipProgressMainHand;
    @Shadow private float equipProgressMainHand;
    @Shadow private float prevEquipProgressOffHand;
    @Shadow private float equipProgressOffHand;
    @Shadow private ItemStack mainHand;
    @Shadow private ItemStack offHand;
    @Shadow @Final private EntityRenderDispatcher entityRenderDispatcher;

    @Shadow protected abstract void renderMapInBothHands(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, float pitch, float equipProgress, float swingProgress);

    @Shadow protected abstract void renderMapInOneHand(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, float equipProgress, Arm arm, float swingProgress, ItemStack stack);

    @Shadow protected abstract void applySwingOffset(MatrixStack matrices, Arm arm, float swingProgress);

    @Shadow public abstract void renderItem(LivingEntity entity, ItemStack stack, ModelTransformationMode renderMode, boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light);

    @Shadow protected abstract void applyBrushTransformation(MatrixStack matrices, float tickDelta, Arm arm, ItemStack stack, float equipProgress);

    @Unique
    private static final float EAT_OR_DRINK_X_ANGLE_MULTIPLIER = 45.0F;
    @Unique
    private static final float EAT_OR_DRINK_Y_ANGLE_MULTIPLIER = 120.0F;
    @Unique
    private static final float EAT_OR_DRINK_Z_ANGLE_MULTIPLIER = 40.0F;

    @Inject(method = "applySwingOffset", at = @At("HEAD"), cancellable = true)
    private void applySwingOffset(MatrixStack matrices, Arm arm, float swingProgress, CallbackInfo ci) {
        int i = arm == Arm.RIGHT ? 1 : -1;
        float f = MathHelper.sin(0 * 0 * 3.1415927F);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)i * (45.0F + f * -20.0F)));
        float g = 0;
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)i * -45.0F));

        ci.cancel();
    }


    @Inject(method = "applyEatOrDrinkTransformation", at = @At("HEAD"), cancellable = true)
    private void applyEatOrDrinkTransformation(MatrixStack matrices, float tickDelta, Arm arm, ItemStack stack, CallbackInfo ci) {
        PlayerEntity player = client.player;
        if (player == null) {
            return;
        }

        float f = (float)player.getItemUseTimeLeft() - tickDelta + 1.0F;
        float g = f / (float)stack.getMaxUseTime();
        float h;
        if (g < 0.8F) {
            h = MathHelper.abs(MathHelper.cos(f / 4.0F * 3.1415927F) * 0.1F);
            matrices.translate(0.0F, h, 0.0F);
        }

        h = 1.0F - (float)Math.pow((double)g, 27.0);
        int i = arm == Arm.RIGHT ? 1 : -1;
        matrices.translate(h * 1.4F * (float)i, h * 0.35F, h * -0.7F); // Adjusted translations
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)i * h * EAT_OR_DRINK_Y_ANGLE_MULTIPLIER));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(h * EAT_OR_DRINK_X_ANGLE_MULTIPLIER));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)i * h * EAT_OR_DRINK_Z_ANGLE_MULTIPLIER));

        // Ensure the arm is rendered
        renderArmHoldingItem(matrices, client.getBufferBuilders().getEntityVertexConsumers(), client.getEntityRenderDispatcher().getLight(player, tickDelta), 0.0F, 0.0F, arm);

        // Adjust the item position relative to the hand
        matrices.translate((float)i * 0.3F, -0.45, 1.85F); // Move item into the hand and to the bottom center
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-40.0F));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(0.0F));

        // Cancel the original method to prevent it from running
        ci.cancel();
    }

    @Unique
    private void renderArmHoldingItem(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, float equipProgress, float swingProgress, Arm arm) {
        boolean isRightArm = arm == Arm.RIGHT;
        float f = isRightArm ? 1.0F : -1.0F;

        matrices.push();

        // Adjust the arm position
        matrices.translate(0.125F, -1.28F, 1F); // Move the arm forward and slightly downward
        matrices.scale(2f, 2f, 2f);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(0.0F));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(f * -71.0F));

        AbstractClientPlayerEntity playerEntity = this.client.player;
        if (playerEntity == null) {
            matrices.pop();
            return;
        }

        this.client.getTextureManager().bindTexture(playerEntity.getSkinTexture());

        PlayerEntityRenderer playerRenderer = (PlayerEntityRenderer) this.client.getEntityRenderDispatcher().getRenderer(playerEntity);
        if (playerRenderer == null) {
            matrices.pop();
            return;
        }

        // Render the appropriate arm
        if (isRightArm) {
            playerRenderer.renderRightArm(matrices, vertexConsumers, light, playerEntity);
        } else {
            playerRenderer.renderLeftArm(matrices, vertexConsumers, light, playerEntity);
        }

        // Debug: Render a small cube to represent the item
        /*matrices.push();
        matrices.translate(f * 0.05F, -0.3F, -0.15F);
        matrices.scale(1F, 1F, 1F);
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntitySolid(new Identifier("minecraft", "textures/block/stone.png")));
        MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(Blocks.STONE.getDefaultState(), matrices, vertexConsumers, light, OverlayTexture.DEFAULT_UV);
        matrices.pop();*/

        matrices.pop();
    }
}
