package cn.ksmcbrigade.pr;

import cn.ksmcbrigade.pr.arguments.PacketArgument;
import cn.ksmcbrigade.pr.utils.PackageUtils;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(PacketResender.MOD_ID)
public class PacketResender {

    public static final String MOD_ID = "pr";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final Config config;

    public static PacketArgument.StringType TYPE;

    static {
        try {
            config = new Config(new File("config/pr-config.json"));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public PacketResender() throws IOException, NoSuchMethodException {
        MinecraftForge.EVENT_BUS.register(this);
        ArrayList<String> packets = new ArrayList<>();
        Set<Class<?>> classes = PackageUtils.getClasses(ServerboundChatPacket.class.getPackageName());
        /*System.out.println(classes.size());
        System.out.println(Arrays.toString(classes.toArray(new Class<?>[0])));*/
        for (Class<?> aClass : classes) {
            if(aClass.getSimpleName().contains("Serverbound")){
                packets.add(aClass.getSimpleName());
            }
        }
        TYPE = new PacketArgument.StringType(packets.toArray(new String[0]));
        LOGGER.info("PR mod loaded.");
    }

    @SubscribeEvent
    public void commands(RegisterClientCommandsEvent event){
        event.getDispatcher().register(Commands.literal("pr-reload").executes(context -> {
            try {
                config.load();
                context.getSource().sendSystemMessage(CommonComponents.GUI_DONE);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return 0;
        }));

        event.getDispatcher().register(Commands.literal("pr-view-config").executes(context -> {
            String json = new GsonBuilder().setPrettyPrinting().create().toJson(config.get());
            for (String s : json.split("\n")) {
                context.getSource().sendSystemMessage(Component.literal(s));
            }
            return 0;
        }));

        event.getDispatcher().register(Commands.literal("pr-save").executes(context -> {
            try {
                config.save(true);
                context.getSource().sendSystemMessage(CommonComponents.GUI_DONE);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return 0;
        }));

        event.getDispatcher().register(Commands.literal("pr-add").then(Commands.argument("packet", new PacketArgument(TYPE)).then(Commands.argument("times", IntegerArgumentType.integer(1)).then(Commands.argument("internal", LongArgumentType.longArg()).then(Commands.argument("block", BoolArgumentType.bool()).executes(context -> {
            try {
                config.add(new Config.Info(PacketArgument.getString(context,"packet"),IntegerArgumentType.getInteger(context,"times"),LongArgumentType.getLong(context,"internal"),BoolArgumentType.getBool(context,"block")));
                context.getSource().sendSystemMessage(CommonComponents.GUI_DONE);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return 0;
        }))))));

        event.getDispatcher().register(Commands.literal("pr-edit").then(Commands.argument("packet", PacketArgument.pr_remove()).then(Commands.argument("times", IntegerArgumentType.integer(1)).then(Commands.argument("internal", LongArgumentType.longArg()).then(Commands.argument("block", BoolArgumentType.bool()).executes(context -> {
            try {
                String packet = PacketArgument.getString(context, "packet");
                if (config.contains(packet)){
                    config.remove(packet);
                    config.add(new Config.Info(packet, IntegerArgumentType.getInteger(context, "times"), LongArgumentType.getLong(context, "internal"), BoolArgumentType.getBool(context, "block")));
                }
                context.getSource().sendSystemMessage(CommonComponents.GUI_DONE);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return 0;
        }))))));

        event.getDispatcher().register(Commands.literal("pr-remove").then(Commands.argument("packet",PacketArgument.pr_remove()).executes(context -> {
            try {
                config.remove(PacketArgument.getString(context,"packet"));
                context.getSource().sendSystemMessage(CommonComponents.GUI_DONE);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return 0;
        })));
    }
}
