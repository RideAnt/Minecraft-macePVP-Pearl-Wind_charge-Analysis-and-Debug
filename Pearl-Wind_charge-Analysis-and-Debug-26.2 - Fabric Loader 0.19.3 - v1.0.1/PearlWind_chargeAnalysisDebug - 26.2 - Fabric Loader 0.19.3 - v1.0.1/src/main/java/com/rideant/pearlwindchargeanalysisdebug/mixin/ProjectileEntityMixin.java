package com.rideant.pearlwindchargeanalysisdebug.mixin;

import com.rideant.pearlwindchargeanalysisdebug.PearlWindChargeAnalysisDebug;
import com.rideant.pearlwindchargeanalysisdebug.ProjectileTraceLogger;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Projectile.class)
public abstract class ProjectileEntityMixin {
    @Unique
    private Vec3 pearlwindDebug$velocityBeforeDeflection = Vec3.ZERO;

    @Unique
    private double pearlwindDebug$accelerationBeforeDeflection = Double.NaN;

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

    @Inject(
            method = "shootFromRotation",
            at = @At("TAIL")
    )
    private void pearlwindDebug$logProjectileLaunch(
            Entity source,
            float xRot,
            float yRot,
            float yOffset,
            float power,
            float uncertainty,
            CallbackInfo ci
    ) {
        ProjectileTraceLogger.onProjectileLaunched(
                (Projectile) (Object) this
        );
    }

    @Inject(
            method = "onHit",
            at = @At("HEAD")
    )
    private void pearlwindDebug$logHit(
            HitResult hitResult,
            CallbackInfo ci
    ) {
        ProjectileTraceLogger.onHit(
                (Projectile) (Object) this,
                hitResult
        );
    }

    @Inject(
            method = "deflect",
            at = @At("HEAD")
    )
    private void pearlwindDebug$captureBeforeDeflection(
            ProjectileDeflection deflection,
            Entity deflectingEntity,
            EntityReference<Entity> newOwner,
            boolean byAttack,
            CallbackInfoReturnable<Boolean> cir
    ) {
        Projectile projectile = (Projectile) (Object) this;
        pearlwindDebug$velocityBeforeDeflection = projectile.getDeltaMovement();
        pearlwindDebug$accelerationBeforeDeflection =
                ProjectileTraceLogger.accelerationPower(projectile);
    }

    @Inject(
            method = "deflect",
            at = @At("RETURN")
    )
    private void pearlwindDebug$logAfterDeflection(
            ProjectileDeflection deflection,
            Entity deflectingEntity,
            EntityReference<Entity> newOwner,
            boolean byAttack,
            CallbackInfoReturnable<Boolean> cir
    ) {
        ProjectileTraceLogger.onDeflection(
                (Projectile) (Object) this,
                deflection,
                deflectingEntity,
                byAttack,
                Boolean.TRUE.equals(cir.getReturnValue()),
                pearlwindDebug$velocityBeforeDeflection,
                pearlwindDebug$accelerationBeforeDeflection
        );
    }
}
