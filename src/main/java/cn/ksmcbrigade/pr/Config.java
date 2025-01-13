package cn.ksmcbrigade.pr;

import com.google.gson.*;
import net.minecraft.network.protocol.Packet;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class Config {
    public final ArrayList<Info> packetInfos = new ArrayList<>();

    public final File config;

    public Config(File file) throws IOException {
        this.config = file;
        this.save(false);
        this.load();
    }

    public void save(boolean e) throws IOException {
        if(!config.exists() || e){
            FileUtils.writeStringToFile(this.config, new GsonBuilder().setPrettyPrinting().create().toJson(get()));
        }
    }

    public void load() throws IOException {
        this.packetInfos.clear();
        JsonArray array = JsonParser.parseString(FileUtils.readFileToString(this.config)).getAsJsonArray();
        for (JsonElement element : array) {
            String packet = null;
            int times = 1;
            long interval = 0;
            boolean block = false;
            if(element instanceof JsonObject object){
                if(!object.has("packet")) continue;
                packet = object.get("packet").getAsString();
                if(object.has("times")) times = object.get("times").getAsInt();
                if(object.has("interval")) interval = object.get("interval").getAsLong();
                if(object.has("block")) block = object.get("block").getAsBoolean();
            }
            this.packetInfos.add(new Info(packet,times,interval,block));
        }
    }

    public JsonArray get(){
        JsonArray array = new JsonArray();
        for (Info packetInfo : packetInfos) {
            array.add(packetInfo.get());
        }
        return array;
    }

    public boolean contains(String packet){
        for (Info packetInfo : packetInfos) {
            if(packetInfo.packet.equalsIgnoreCase(packet)) return true;
        }
        return false;
    }

    public boolean contains(Packet<?> packet){
        return contains(packet.getClass().getSimpleName());
    }

    public Info get(String packet){
        for (Info packetInfo : this.packetInfos) {
            if(packetInfo.packet.equalsIgnoreCase(packet)){
                return packetInfo;
            }
        }
        return null;
    }

    public void add(Info info) throws IOException {
        this.packetInfos.add(info);
        this.save(true);
    }

    public void remove(Info info) throws IOException{
        this.packetInfos.remove(info);
        this.save(true);
    }

    public void remove(String packet) throws IOException{
        Info info = null;
        for (Info packetInfo : this.packetInfos) {
            if(packetInfo.packet.equalsIgnoreCase(packet)){
                info = packetInfo;
                break;
            }
        }
        if(info!=null) this.remove(info);
    }

    public Info get(Packet<?> packet){
       return get(packet.getClass().getSimpleName());
    }

    public record Info(String packet, int times,long sendingInterval,boolean block){
        public JsonObject get(){
            JsonObject object = new JsonObject();
            object.addProperty("packet",packet);
            object.addProperty("times",times);
            object.addProperty("interval",sendingInterval);
            object.addProperty("block",block);
            return object;
        }
    }
}
