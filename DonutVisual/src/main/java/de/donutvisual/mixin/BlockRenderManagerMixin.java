package de.donutvisual.mixin;

import de.donutvisual.FakeBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.block.BlockModels;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.model.BlockStateModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockRenderManager.class)
public abstract class BlockRenderManagerMixin {
    @Shadow public abstract BlockModels getModels();

    @Inject(method = "getModel", at = @At("HEAD"), cancellable = true)
    private void donutvisual$fakeModel(BlockState state, CallbackInfoReturnable<BlockStateModel> cir) {
        BlockState t = FakeBlocks.targetState(state);
        if (t != null) cir.setReturnValue(getModels().getModel(t));
    }
}
