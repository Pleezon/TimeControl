package de.techgamez.pleezon.mixin.mixins;

import de.techgamez.pleezon.TimeControl;
import net.minecraft.command.ICommandSender;
import net.minecraft.crash.CrashReport;
import net.minecraft.network.ServerStatusResponse;
import net.minecraft.profiler.IPlayerUsage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IThreadListener;
import net.minecraft.util.ReportedException;
import net.minecraft.world.WorldServer;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * description missing.
 */
@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer implements Runnable, ICommandSender, IThreadListener, IPlayerUsage
{
	public int tps = 20;

	@Shadow protected abstract boolean startServer() throws IOException;

	@Shadow private long currentTime;

	@Shadow
	public static long getCurrentTimeMillis()
	{
		return 0;
	}

	@Shadow @Final private ServerStatusResponse statusResponse;

	@Shadow protected abstract void addFaviconToStatusResponse(ServerStatusResponse response);

	@Shadow private String motd;

	@Shadow private boolean serverRunning;

	@Shadow @Final private static Logger logger;

	@Shadow private long timeOfLastWarning;

	@Shadow public WorldServer[] worldServers;


	@Shadow private boolean serverIsRunning;

	@Shadow protected abstract void finalTick(CrashReport report);

	@Shadow public abstract CrashReport addServerInfoToCrashReport(CrashReport report);

	@Shadow public abstract File getDataDirectory();

	@Shadow public abstract void stopServer();

	@Shadow private boolean serverStopped;

	@Shadow protected abstract void systemExitNow();

	@Shadow public abstract void tick();

	/**
	 * @author Pleezon
	 * @reason make gameloop run faster
	 */
	@Overwrite
	public void run()
	{
		try {
			if (this.startServer()) {
				net.minecraftforge.fml.common.FMLCommonHandler.instance().handleServerStarted();
				this.currentTime = getCurrentTimeMillis();
				long i = 0L;
				this.statusResponse.setServerDescription(new ChatComponentText(this.motd));
				this.statusResponse.setProtocolVersionInfo(new ServerStatusResponse.MinecraftProtocolVersionIdentifier("1.8.9", 47));
				this.addFaviconToStatusResponse(this.statusResponse);

				while (this.serverRunning) {
					double ticks = (1000 / (tps * TimeControl.getFac()));
					long k = getCurrentTimeMillis();
					long j = k - this.currentTime;

					if (j > 2000L && this.currentTime - this.timeOfLastWarning >= 15000L) {
						logger.warn("Can't keep up! Did the system time change, or is the server overloaded? Running {}ms behind, skipping {} tick(s)", j, j / ticks);
						j = 2000L;
						this.timeOfLastWarning = this.currentTime;
					}

					if (j < 0L) {
						logger.warn("Time ran backwards! Did the system time change?");
						j = 0L;
					}

					i += j;
					this.currentTime = k;

					if (this.worldServers[0].areAllPlayersAsleep()) {

						this.tick();
						i = 0L;
					} else {
						while (i > ticks) {
							i -= ticks;
							this.tick();
						}
					}

					Thread.sleep(Math.max(1L, (int) ticks - i));
					this.serverIsRunning = true;
				}
				net.minecraftforge.fml.common.FMLCommonHandler.instance().handleServerStopping();
				net.minecraftforge.fml.common.FMLCommonHandler.instance().expectServerStopped(); // has to come before finalTick to avoid race conditions
			} else {
				net.minecraftforge.fml.common.FMLCommonHandler.instance().expectServerStopped(); // has to come before finalTick to avoid race conditions
				this.finalTick((CrashReport) null);
			}
		} catch (net.minecraftforge.fml.common.StartupQuery.AbortedException e) {
			// ignore silently
			net.minecraftforge.fml.common.FMLCommonHandler.instance().expectServerStopped(); // has to come before finalTick to avoid race conditions
		} catch (Throwable throwable1) {
			logger.error("Encountered an unexpected exception", throwable1);
			CrashReport crashreport = null;

			if (throwable1 instanceof ReportedException) {
				crashreport = this.addServerInfoToCrashReport(((ReportedException) throwable1).getCrashReport());
			} else {
				crashreport = this.addServerInfoToCrashReport(new CrashReport("Exception in server tick loop", throwable1));
			}

			File file1 = new File(new File(this.getDataDirectory(), "crash-reports"), "crash-" + (new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")).format(new Date()) + "-server.txt");

			if (crashreport.saveToFile(file1)) {
				logger.error("This crash report has been saved to: " + file1.getAbsolutePath());
			} else {
				logger.error("We were unable to save this crash report to disk.");
			}

			net.minecraftforge.fml.common.FMLCommonHandler.instance().expectServerStopped(); // has to come before finalTick to avoid race conditions
			this.finalTick(crashreport);
		} finally {
			try {
				this.stopServer();
				this.serverStopped = true;
			} catch (Throwable throwable) {
				logger.error("Exception stopping the server", throwable);
			} finally {
				net.minecraftforge.fml.common.FMLCommonHandler.instance().handleServerStopped();
				this.serverStopped = true;
				this.systemExitNow();
			}
		}
	}
}
