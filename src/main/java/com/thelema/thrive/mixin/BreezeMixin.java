package com.thelema.thrive.mixin;

import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Breeze.class)
public class BreezeMixin {

    @Inject(method = "deflection", at = @At("HEAD"), cancellable = true)
    private void thrive$deflection(Projectile projectile, CallbackInfoReturnable<ProjectileDeflection> cir) {
        if (projectile instanceof AbstractArrow arrow && arrow.getPierceLevel() > 0) {
            cir.setReturnValue(ProjectileDeflection.NONE);
        }
    }
}