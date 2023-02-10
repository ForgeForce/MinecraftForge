package net.minecraftforge.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin
{
    @Unique
    private MinecraftServer self()
    {
        //noinspection ConstantConditions
        return (MinecraftServer) (Object) this;
    }

    @Inject(method = "runServer()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;onServerCrash(Lnet/minecraft/CrashReport;)V"))
    public void forge$expectServerStopped(CallbackInfo ci)
    {
        ServerLifecycleHooks.expectServerStopped();
    }

    @Inject(method = "runServer()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;onServerExit()V"))
    public void forge$serverStoppedEvent(CallbackInfo ci)
    {
        ServerLifecycleHooks.handleServerStopped(self());
    }
}
