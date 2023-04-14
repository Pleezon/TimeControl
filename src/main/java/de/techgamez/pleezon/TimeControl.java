package de.techgamez.pleezon;

import de.techgamez.pleezon.mixin.mixins.MixinMinecraftServer;
import net.labymod.api.LabyModAddon;
import net.labymod.api.events.MessageSendEvent;
import net.labymod.main.LabyMod;
import net.labymod.settings.elements.SettingsElement;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;

import java.util.List;

/**
 * description missing.
 */
public class TimeControl extends LabyModAddon
{
	public static double fac = 1;

	@Override
	public void onEnable()
	{
		this.getApi().getEventManager().register((MessageSendEvent) s -> {
			if (s.startsWith(".fac")) {
				s = s.replaceFirst(".fac", "");
				try{
					fac = Double.parseDouble(s.trim());
					LabyMod.getInstance().displayMessageInChat("set time factor: " + fac);
				}catch (Exception e){
					LabyMod.getInstance().displayMessageInChat("not a valid factor: " + s);
				}

				return true;
			}
			return false;
		});
	}

	@Override
	public void loadConfig()
	{

	}

	@Override
	protected void fillSettings(List<SettingsElement> list)
	{

	}

	public static double getFac()
	{
		return fac;
	}
}
