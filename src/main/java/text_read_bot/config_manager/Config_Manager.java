package text_read_bot.config_manager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Properties;


public class Config_Manager {

	private static Config_Manage bot_config_instance;
	public static Config_Manage get_Bot_Config_Manage() {
		return bot_config_instance;
	}

	public static enum Bot_Config_Keys{
		AUDIO_EXPORT_PATH,
		AUDIO_GEN_CMD,
		DICTIONARY_PATH,
		DISCORD_TOKEN,
		AUDIO_GEN_TIMEOUT,
		AUDIO_GEN_DELAY,
		GENERATE_LOG,
		COMMAND_PREFIX,
		URL_READ_MODE,
		NAME_READ_MODE,
		AUTO_JOIN,
		AUTO_LEAVE,
	}

	public static String get_Bot_Property(Bot_Config_Keys bck) {
		Properties pro = get_Bot_Config_Manage().get_Properties();
		switch(bck) {
		case AUDIO_EXPORT_PATH:
			return pro.getProperty("audio_export_path");
		case AUDIO_GEN_CMD:
			return pro.getProperty("audio_gen_command");
		case DICTIONARY_PATH:
			return pro.getProperty("dictionary_path");
		case DISCORD_TOKEN:
			return pro.getProperty("token");
		case AUDIO_GEN_TIMEOUT:
			return pro.getProperty("generate_sound_timeout");
		case AUDIO_GEN_DELAY:
			return pro.getProperty("file_export_delay");
		case GENERATE_LOG:
			return pro.getProperty("generate_log");
		case COMMAND_PREFIX:
			return pro.getProperty("bot_command_prefix");
		case URL_READ_MODE:
			return pro.getProperty("url_read_mode");
		case NAME_READ_MODE:
			return pro.getProperty("name_read_mode");
		case AUTO_JOIN:
			return pro.getProperty("auto_join");
		case AUTO_LEAVE:
			return pro.getProperty("auto_leave");
		default:
			return "";
		}
	}

	public static boolean load_Configuration() {
		String system_path = System.getProperty("user.dir");

		// BOT CONFIGURATION FILE
		String path = system_path + File.separator + "bot.config";
		Config_Manage load_bot_config = new Config_Manage(path, false);
		if(!load_bot_config.exists()) {
			try {
				Properties pro = load_bot_config.get_Properties();
				InputStream in = ClassLoader.getSystemResourceAsStream("default.config");
				pro.load(in);
				in.close();
				load_bot_config.save_Config("Configure your setting.");
				System.out.println("コンフィグファイルを生成しました！。設定してからもう一度起動してください。");
			} catch (IOException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
				System.out.println("[CONFIG-ERROR]error occurred while loading default config.");
			}

			return false;
		}
		load_bot_config.load_Config();
		bot_config_instance = load_bot_config;

		return true;
	}

	public static class Config_Manage{
		private Properties config = new Properties();
		private String path = "";

		public Config_Manage(String c_path, Boolean load) {
			path = c_path;
			if(load)
				load_Config();
		}

		public void set_Path(String p) {
			path = p;
		}

		public String get_Path() {
			return path;
		}

		public Properties get_Properties() {
			return config;
		}

		public void set_Properties(Properties property) {
			config = property;
		}

		public boolean exists() {
			return new File(path).exists();
		}

		public boolean load_Config() {

			if(path.length() == 0) return false;

			Properties settings = new Properties();
			try {
				InputStreamReader cf = new InputStreamReader( new FileInputStream(path), "UTF-8" ) ;
				settings.load(cf);
				cf.close();
				set_Properties(settings);
				return true;
			} catch (IOException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
				return false;
			}
		}

		public boolean save_Config(String comments) {
			if(path.length() == 0) return false;

			try {
				OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(path), "UTF-8");
			    BufferedWriter bw = new BufferedWriter(osw);
				config.store(bw, comments);
				return true;
			}catch(Exception e) {
				return false;
			}
		}
	}

}
