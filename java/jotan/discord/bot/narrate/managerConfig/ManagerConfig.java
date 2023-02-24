package jotan.discord.bot.narrate.managerConfig;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Properties;

public class ManagerConfig {

	private static Config configInstance;

	public static Config getConfigInstance() {
		return configInstance;
	}

	public static enum BotConfigKey {
		NARRATE_EXPORT_PATH("NarrateExportPath", "./audio"),
		NARRATE_GENERATE_COMMAND("NarrateGenerateCommand", "sh ./generateNarrate.sh"),
		NARRATE_GENERATE_TIMEOUT("NarrateGenerateTimeout", "2"),
		NARRATE_GENERATE_DELAY("NarrateGenerateDelay", "-1"),
		NARRATE_GENERATE_LOG("NarrateGenerateLog", "true"),
		NARRATE_URL_MODE("NarrateUrlMode", "message"),
		NARRATE_NAME_MODE("NarrateNameMode", "nickname"),
		NARRATE_IGNORE_PREFIX("NarrateIgnorePrefix","!"),

		AUTO_JOIN("AutoJoin", "false"),
		AUTO_LEAVE("AutoLeave", "false"),

		DICTIONARY_SAVE_PATH("DictionarySavePath", "./dictionary"),

		DISCORD_COMMAND_PREFIX("DiscordCommand", "!narrate"),
		DISCORD_TOKEN("DiscordToken", "your token here"),
		;

		private final String key, defaultValue;

		private BotConfigKey(String key, String defaultValue) {
			this.key = key;
			this.defaultValue = defaultValue;
		}

		public String getKey() {
			return key;
		}

		public String getDefaultValue() {
			return this.defaultValue;
		}

	}

	public static String getConfigValue(BotConfigKey bck) {
		Properties pro = getConfigInstance().getProperties();
		return pro.getProperty(bck.getKey());
	}

	public static boolean loadConfiguration() {
		String path = System.getProperty("user.dir") + File.separator + "bot.config";
		Config load_bot_config = new Config(path, false);
		if (!load_bot_config.exists()) {
			try {
				setDefaultConfig();
				System.out.println("[CONFIG-INFO]Make your configuration");
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("[CONFIG-ERROR]error occurred while loading default config.");
			}

			return false;
		}
		load_bot_config.load();
		configInstance = load_bot_config;

		return true;
	}

	public static void setDefaultConfig() {
		String path = System.getProperty("user.dir") + File.separator + "bot.config";
		Config config = new Config(path, false);
		Properties properties = config.getProperties();

		for(BotConfigKey key : BotConfigKey.values()) {
			properties.put(key.getKey(), key.getDefaultValue());
		}

		config.save("Configure Settings");
	}

	public static class Config {
		private Properties config = new Properties();
		private String path = "";

		public Config(String c_path, Boolean load) {
			path = c_path;
			if (load)
				load();
		}

		public void setPath(String p) {
			path = p;
		}

		public String getPath() {
			return path;
		}

		public Properties getProperties() {
			return config;
		}

		public void setProperties(Properties property) {
			config = property;
		}

		public boolean exists() {
			return new File(path).exists();
		}

		public boolean load() {

			if (path.length() == 0)
				return false;

			Properties settings = new Properties();
			try {
				InputStreamReader cf = new InputStreamReader(new FileInputStream(path), "UTF-8");
				settings.load(cf);
				cf.close();
				setProperties(settings);
				return true;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}

		public boolean save(String comments) {
			if (path.length() == 0)
				return false;

			try {
				OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(path), "UTF-8");
				BufferedWriter bw = new BufferedWriter(osw);
				config.store(bw, comments);
				return true;
			} catch (Exception e) {
				return false;
			}
		}
	}

}
