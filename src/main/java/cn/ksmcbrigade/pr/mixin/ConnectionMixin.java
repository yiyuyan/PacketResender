package cn.ksmcbrigade.pr.mixin;

import cn.ksmcbrigade.pr.Config;
import cn.ksmcbrigade.pr.PacketResender;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.*;

@Mixin(Connection.class)
public abstract class ConnectionMixin {

    @Shadow protected abstract void doSendPacket(Packet<?> p_243260_, @Nullable PacketSendListener p_243290_, ConnectionProtocol p_243203_, ConnectionProtocol p_243307_);

    @Unique
    private CopyOnWriteArrayList<Packet<?>> pr$resend = new CopyOnWriteArrayList<>();

    @Inject(method = "doSendPacket",at = @At("TAIL"))
    public void send(Packet<?> p_243260_, PacketSendListener p_243290_, ConnectionProtocol p_243203_, ConnectionProtocol p_243307_, CallbackInfo ci) throws InterruptedException {
        synchronized (this) {
            if(pr$resend.contains(p_243260_)) return;
            if(PacketResender.config.contains(p_243260_)){
                Config.Info info = PacketResender.config.get(p_243260_);
                int times = info.times()-1;
                pr$resend.add(p_243260_);

                System.out.println(times);
                for (int i = 0; i < times; i++) {
                    this.doSendPacket(p_243260_,p_243290_,p_243203_,p_243307_);
                    Thread.sleep(info.sendingInterval());
                }

                pr$resend.remove(p_243260_);
                PacketResender.LOGGER.info("Re sent {} named {} packet(s).", times, info.packet());
            }
        }
    }

    @Inject(method = "doSendPacket",at = @At("HEAD"),cancellable = true)
    public void blockSend(Packet<?> p_243260_, PacketSendListener p_243290_, ConnectionProtocol p_243203_, ConnectionProtocol p_243307_, CallbackInfo ci){
        synchronized (this) {
            if(PacketResender.config.contains(p_243260_) && PacketResender.config.get(p_243260_).block()){
                ci.cancel();
            }
        }
    }
}
