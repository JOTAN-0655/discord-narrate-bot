package text_read_bot.discord;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import text_read_bot.config_manager.Config_Manager;
import text_read_bot.config_manager.Config_Manager.Bot_Config_Keys;

public class Narrate_Manage {

	public static String generate_narrate_sound(String message,String name) {
		String audio_gen_command = Config_Manager.get_Bot_Property(Bot_Config_Keys.AUDIO_GEN_CMD);

		List<String> commands = new ArrayList<String>();
		commands.addAll(Arrays.asList(audio_gen_command.split(" ")));
		commands.add("\""+message+"\"");
		commands.add(name);


		ProcessBuilder generate_sound_process_builder = new ProcessBuilder(commands);
		try {
			Process generate_sound_process = generate_sound_process_builder.start();

			try {
				if(Boolean.parseBoolean(Config_Manager.get_Bot_Property(Bot_Config_Keys.GENERATE_LOG))) {
			        new StreamThread(generate_sound_process.getInputStream(), "OUTPUT").start();
			        new StreamThread(generate_sound_process.getErrorStream(), "ERROR").start();
				}
			}catch(Exception e) {

			}


			int r = generate_sound_process.waitFor();


			if(r==0) return Config_Manager.get_Bot_Property(Bot_Config_Keys.AUDIO_EXPORT_PATH) + "/" + name + ".wav";
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	//code borrowed from
	//https://qiita.com/mfqmagic/items/5469fd4057144b76ad87
	public static class StreamThread extends Thread {
	    private InputStream in;
	    private String type;

	    public StreamThread(InputStream in, String type) {
	        this.in = in;
	        this.type = type;
	    }

	    @Override
	    public void run() {
	        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, "MS932"))) {
	            String line = null;
	            while ((line = br.readLine()) != null) {
	                System.out.println(type + ">" + line);
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }
	}

}
