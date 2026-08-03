package com.rideant.pearlwindchargeanalysisdebug.mixin;

import com.rideant.pearlwindchargeanalysisdebug.PearlWindChargeAnalysisDebug;
import net.minecraft.entity.projectile.ProjectileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ProjectileEntity.class)
public abstract class ProjectileEntityMixin {
    @ModifyVariable(
            method = "setVelocity(DDDFF)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 1
    )
    private float pearlwindDebug$removeUncertainty(float uncertainty) {
        ProjectileEntity projectile = (ProjectileEntity) (Object) this;
        if (!PearlWindChargeAnalysisDebug.shouldRemoveRandomOffset(projectile)) {
            return uncertainty;
        }

        PearlWindChargeAnalysisDebug.logApplied(projectile, uncertainty);
        return 0.0F;
    }
}
