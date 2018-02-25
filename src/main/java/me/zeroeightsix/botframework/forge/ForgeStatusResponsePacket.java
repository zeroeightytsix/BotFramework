package me.zeroeightsix.botframework.forge;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.auth.util.Base64;
import com.github.steveice10.mc.protocol.data.message.Message;
import com.github.steveice10.mc.protocol.data.status.PlayerInfo;
import com.github.steveice10.mc.protocol.data.status.ServerStatusInfo;
import com.github.steveice10.mc.protocol.data.status.VersionInfo;
import com.github.steveice10.mc.protocol.packet.status.server.StatusResponsePacket;
import com.github.steveice10.packetlib.io.NetInput;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;

public class ForgeStatusResponsePacket extends StatusResponsePacket {
    private ServerStatusInfo info;
    private ModdedServerInfo modInfo;

    @SuppressWarnings("unused")
    public ForgeStatusResponsePacket() {
        super(null);
    }

    public ForgeStatusResponsePacket(ServerStatusInfo info) {
        super(info);
    }

    public ServerStatusInfo getInfo() {
        return this.info;
    }

    @Override
    public void read(NetInput in) throws IOException {
        JsonObject obj = new Gson().fromJson(in.readString(), JsonObject.class);
        JsonObject ver = obj.get("version").getAsJsonObject();
        VersionInfo version = new VersionInfo(ver.get("name").getAsString(), ver.get("protocol").getAsInt());
        JsonObject plrs = obj.get("players").getAsJsonObject();
        GameProfile profiles[] = new GameProfile[0];
        if(plrs.has("sample")) {
            JsonArray prof = plrs.get("sample").getAsJsonArray();
            if(prof.size() > 0) {
                profiles = new GameProfile[prof.size()];
                for(int index = 0; index < prof.size(); index++) {
                    JsonObject o = prof.get(index).getAsJsonObject();
                    profiles[index] = new GameProfile(o.get("id").getAsString(), o.get("name").getAsString());
                }
            }
        }

        PlayerInfo players = new PlayerInfo(plrs.get("max").getAsInt(), plrs.get("online").getAsInt(), profiles);
        JsonElement desc = obj.get("description");
        Message description = Message.fromJson(desc);
        BufferedImage icon = null;
        if(obj.has("favicon")) {
            icon = this.stringToIcon(obj.get("favicon").getAsString());
        }

        if (obj.has("modinfo")) {
            JsonObject modinfo = obj.get("modinfo").getAsJsonObject();
            String type = modinfo.get("type").getAsString();
            JsonArray modList = modinfo.get("modList").getAsJsonArray();
            ArrayList<ForgeHandshakeHandler.FMLHandshakeMessage.ModList.ModContainer> containers = new ArrayList<>();
            for (int i = 0; i < modList.size(); i++) {
                JsonObject mod = modList.get(i).getAsJsonObject();
                String mod_version = mod.get("version").getAsString();
                String mod_id = mod.get("modid").getAsString();
                containers.add(new ForgeHandshakeHandler.FMLHandshakeMessage.ModList.ModContainer(mod_id, mod_version));
            }
            this.modInfo = new ModdedServerInfo(type, containers);
        }

        this.info = new ServerStatusInfo(version, players, description, icon);
    }

    private BufferedImage stringToIcon(String str) throws IOException {
        if(str.startsWith("data:image/png;base64,")) {
            str = str.substring("data:image/png;base64,".length());
        }

        byte bytes[] = Base64.decode(str.getBytes("UTF-8"));
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        BufferedImage icon = ImageIO.read(in);
        in.close();
        if(icon != null && (icon.getWidth() != 64 || icon.getHeight() != 64)) {
            throw new IOException("Icon must be 64x64.");
        }

        return icon;
    }

    public ModdedServerInfo getModInfo() {
        return modInfo;
    }
}
