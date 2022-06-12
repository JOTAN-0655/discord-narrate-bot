package narrate_bot.config_manager;

import java.io.File;
import java.util.HashMap;

import narrate_bot.config_manager.Config_Manager.Bot_Config_Keys;
import narrate_bot.config_manager.Config_Manager.Config_Manage;

public class Dictionary_Manage {

	private static HashMap<Long,Config_Manage> dictionary = new HashMap<Long,Config_Manage>();

	public static Config_Manage get_Dictionary(Long guild_id) {
		String dic_path = Config_Manager.get_Bot_Property(Bot_Config_Keys.DICTIONARY_PATH);
		if(dictionary.get(guild_id) == null) {
			String path = dic_path + File.separator + String.valueOf(guild_id) + ".dictionary";
			Config_Manage dic = new Config_Manage(path , false);
			if(dic.exists()) {
				dic.load_Config();
			}else {
				dic.save_Config("A dictionary for " + String.valueOf(guild_id));
			}
			dictionary.put(guild_id, dic);
			return dic;
		}else {
			return dictionary.get(guild_id);
		}
	}

}
