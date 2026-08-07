package com.rideant.pearlwindchargeanalysisdebug.mixin;

import com.rideant.pearlwindchargeanalysisdebug.PearlWindChargeAnalysisDebug;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Projectile.class)
public abstract class ProjectileEntityMixin {
    @ModifyVariable(
            method = "shoot(DDDFF)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 1
    )
    private float pearlwindDebug$removeUncertainty(float uncertainty) {
        Projectile projectile = (Projectile) (Object) this;
        if (!PearlWindChargeAnalysisDebug.shouldRemoveRandomOffset(projectile)) {
            return uncertainty;
        }

        PearlWindChargeAnalysisDebug.logApplied(projectile, uncertainty);
        return 0.0F;
    }

    @Inject(
            method = "shoot(DDDFF)V",
            at = @At("HEAD")
    )
    private void pearlwindDebug$logShootDirection(
            double x,
            double y,
            double z,
            float divergence,
            float uncertainty,
            CallbackInfo ci
    ) {
        if (!PearlWindChargeAnalysisDebug.shouldLogShootDirection()) {
            return;
        }
        Projectile projectile = (Projectile) (Object) this;
        PearlWindChargeAnalysisDebug.logShootDirection(projectile, x, y, z);
    }
}
