package io.live.ingameime;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
        modid = "ingameime",
        version = "1.0.1",
        name = "IngameIME-PVP",
        acceptedMinecraftVersions = "[1.8.9]",
        acceptableRemoteVersions = "*",
        dependencies = "required-after:mixinbooter"
)
public class IngameIME_Forge {
    public static final Logger LOG = LogManager.getLogger("IngameIME-PVP");
    @SidedProxy(clientSide = "io.live.ingameime.ClientProxy", serverSide = "io.live.ingameime.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }
}
