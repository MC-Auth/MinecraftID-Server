package org.inventivetalent.mcauth;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;
import org.inventivetalent.mcauth.data.Request;
import org.inventivetalent.mcauth.data.Status;
import org.jongo.Jongo;
import org.jongo.MongoCollection;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Random;

public class MCAuthServer extends Plugin implements Listener {

	Configuration  config;
	DatabaseClient databaseClient;

	Jongo           jongo;
	MongoCollection requestsCollection;
	MongoCollection accountsCollection;

	@Override
	public void onEnable() {
		ProxyServer.getInstance().getPluginManager().registerListener(this, this);

		if (!getDataFolder().exists()) { getDataFolder().mkdir(); }
		File file = new File(getDataFolder(), "config.yml");
		if (!file.exists()) {
			try (InputStream in = getResourceAsStream("config.yml")) {
				Files.copy(in, file.toPath());
				throw new RuntimeException("Default config saved! Please edit and restart.");
			} catch (IOException e) {
				throw new RuntimeException("Failed to save default config", e);
			}
		}

		try {
			config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "config.yml"));
		} catch (IOException e) {
			throw new RuntimeException("Failed to load config", e);
		}

		databaseClient = new DatabaseClient(config.getString("mongodb.database"), config.getString("mongodb.host"), config.getInt("mongodb.port"), config.getString("mongodb.login.user"), config.getString("mongodb.login.pass").toCharArray(), config.getString("mongodb.login.db"));
		try {
			databaseClient.connect(10000);
			databaseClient.collectionCount();
		} catch (IOException e) {
			throw new RuntimeException("Failed to connect to databse", e);
		}

		jongo = databaseClient.jongo();
		requestsCollection = jongo.getCollection("requests");
		accountsCollection = jongo.getCollection("accounts");
	}

	@EventHandler
	public void onLogin(LoginEvent event) {
		String ip = event.getConnection().getAddress().getAddress().getHostAddress();
		getLogger().info(event.getConnection().getName() + " connecting (" + ip + ")...");

		event.registerIntent(this);
		getProxy().getScheduler().runAsync(this, () -> {
			Request request = requestsCollection.findOne("{username: #, status: #}", event.getConnection().getName(), Status.REQUESTED).as(Request.class);

			if (request == null) {
				event.setCancelReason("§cThere is no authentication request for your username");
				getLogger().info("No request found");
				event.setCancelled(true);
				event.completeIntent(this);
				return;
			}
			getLogger().info("Handling request #" + request.get_id());

			if (System.currentTimeMillis() - (request.getCreated().getTime()) > 5 * 60 * 1000) {// 5 Minutes timeout
				event.setCancelReason("§cYour authentication request timed out");
				event.setCancelled(true);

				request.setStatus(Status.TIMEOUT_LOGIN);
				requestsCollection.update("{_id: #}", request.get_id()).upsert().with(request);

				event.completeIntent(this);
				return;
			}

			request.setToken(generateToken(6));
			request.setTokenTime(System.currentTimeMillis());
			request.setStatus(Status.TOKEN_GENERATED);
			request.setUuid(event.getConnection().getUniqueId().toString().replaceAll("-", ""));

			requestsCollection.update("{_id: #}", request.get_id()).upsert().with(request);

			// Kick player
			event.setCancelReason("§aYour account has been authenticated.\n"
					+ "§aPlease enter this code on the website: §b" + request.getToken());
			event.setCancelled(true);
			event.completeIntent(this);
		});
	}

	@EventHandler
	public void onPing(ProxyPingEvent event) {
		String ip = event.getConnection().getAddress().getAddress().getHostAddress();

		event.registerIntent(this);
		getProxy().getScheduler().runAsync(this, () -> {
			Request request = requestsCollection.findOne("{request_ip: #, status: #}", ip, Status.REQUESTED).as(Request.class);

			if (request != null) {
				if (System.currentTimeMillis() - (request.getCreated().getTime()) < 5 * 60 * 1000) {
					event.setResponse(new ServerPing(
							new ServerPing.Protocol("MinecraftID", event.getConnection().getVersion()),
							new ServerPing.Players(1, 0, new ServerPing.PlayerInfo[0]),
							new TextComponent("§8MinecraftID Server - §7minecraft.id\n"
									+ "§aJoin now to verify your account!"),
							getProxy().getConfig().getFaviconObject()
					));
					event.completeIntent(this);
					return;
				}
			}
			event.setResponse(new ServerPing(
					new ServerPing.Protocol("MinecraftID", event.getConnection().getVersion()),
					new ServerPing.Players(0, 0, new ServerPing.PlayerInfo[0]),
					new TextComponent("§8MinecraftID Server - §7minecraft.id\n"
							+ "§6There is no request to verify your account"),
					getProxy().getConfig().getFaviconObject()
			));
			event.completeIntent(this);
		});
	}

	String generateToken(int length) {
		String string = "";
		Random random = new Random();
		for (int i = 0; i < length; i++) {
			string += String.valueOf(random.nextInt(10));
		}
		return string;
	}

}
