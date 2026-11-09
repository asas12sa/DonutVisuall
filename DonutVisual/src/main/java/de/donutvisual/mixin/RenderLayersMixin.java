package de.donutvisual.mixin;

import de.donutvisual.FakeBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.RenderLayers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderLayers.class)
public class RenderLayersMixin {
    @Inject(method = "getBlockLayer", at = @At("HEAD"), cancellable = true)
    private static void donutvisual$fakeLayer(BlockState state, CallbackInfoReturnable<BlockRenderLayer> cir) {
        BlockState t = FakeBlocks.targetState(state);
        if (t != null) cir.setReturnValue(RenderLayers.getBlockLayer(t));
    }
}
