package text_read_bot.discord;

import java.awt.Color;
import java.io.File;
import java.net.http.WebSocket.Listener;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;
import text_read_bot.config_manager.Dictionary_Manage;
import text_read_bot.Main;
import text_read_bot.config_manager.Config_Manager;
import text_read_bot.config_manager.Config_Manager.Config_Manage;
import text_read_bot.config_manager.Config_Manager.Bot_Config_Keys;
import text_read_bot.config_manager.Name_Read_Mode_Manager;
import text_read_bot.config_manager.Name_Read_Mode_Manager.Name_Read_Mode;
import text_read_bot.discord.lavaplayer.GuildMusicManager;

public class Discord_Events extends ListenerAdapter implements Listener{

	HashMap<Long,TextChannel> narrate_textchannel = new HashMap<Long,TextChannel>();

	class Audio_File_Narrate extends Thread {
		public String path="";
		public String message= "";
		public TextChannel textchannel;
		public long start_time = Calendar.getInstance().getTimeInMillis();
		@Override
	    public void run() {
			start_time = Calendar.getInstance().getTimeInMillis();
			File file = new File(path);
			long timeout = 2000;
			try {
				timeout = Long.parseLong(Config_Manager.get_Bot_Property(Bot_Config_Keys.AUDIO_GEN_TIMEOUT));
				if(timeout == -1) timeout = Long.MAX_VALUE;
				else timeout *= 1000;
			}catch(Exception e) {
				e.printStackTrace();
				System.out.println("[WARN] : generate_sound_timeout is not set");
			}
			while(!file.exists()) {
				long current_time= Calendar.getInstance().getTimeInMillis();
				if(current_time-start_time > timeout) {
					textchannel.sendMessage("エラー：読み上げファイルの生成が遅すぎます。").complete();
					textchannel.sendMessage("読み上げられなかった内容：" + message).complete();
					return;
				}
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					// TODO 自動生成された catch ブロック
					e.printStackTrace();
				}
			}

			int delay = -1;
			try {
				delay = Integer.parseInt(Config_Manager.get_Bot_Property(Bot_Config_Keys.AUDIO_GEN_DELAY));
			}catch(Exception e) {
				e.printStackTrace();
				System.out.println("[WARN] : file_export_delay is not proper value.");
			}

			if(delay > 0) {
				try {
					Thread.sleep(delay);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

	    	System.out.println(message + " => " + path);
			Lavaplayer_Control.narrate_message(textchannel, path);
	    }
	}

	private HashMap<Long,Long> last_text_channel = new HashMap<Long,Long>();

	@Override
	public void onMessageReceived(MessageReceivedEvent e) {
		Guild guild = e.getGuild();
		Member member = e.getMember();
		TextChannel textchannel = e.getTextChannel();
		Message message = e.getMessage();
		String message_content = message.getContentDisplay();
		AudioManager audio_manager = guild.getAudioManager();

		if(member.getUser().isBot()) return;

		if(message_content.toLowerCase().startsWith(Config_Manager.get_Bot_Property(Bot_Config_Keys.COMMAND_PREFIX))) {
			solve_command(e);
			return;
		}

		last_text_channel.put(guild.getIdLong(), textchannel.getIdLong());

		if(narrate_textchannel.get(guild.getIdLong()) == null) return;
		if(textchannel.getIdLong() != narrate_textchannel.get(guild.getIdLong()).getIdLong()) return;
		if(!audio_manager.isConnected()) {
			textchannel.sendMessage("ボイスチャンネルに参加していないため。読み上げできません。読み上げを中止します。").complete();
			narrate_textchannel.remove(guild.getIdLong());
			return;
		}

		String message_to_read = message_content;
		if(message_to_read.toLowerCase().startsWith("http")) {
			String url_read_mode = Config_Manager.get_Bot_Property(Bot_Config_Keys.URL_READ_MODE);
			if(url_read_mode.equalsIgnoreCase("message")) {
				message_to_read = "URLが送信されました。";
			}else if(url_read_mode.equalsIgnoreCase("no")) {
				return;
			}

		}

		//名前の読み上げ
		List<Name_Read_Mode> nrms = Name_Read_Mode_Manager.parse_Name_Read_Mode(Config_Manager.get_Bot_Property(Bot_Config_Keys.NAME_READ_MODE));
		Collections.reverse(nrms);
		if(!nrms.contains(Name_Read_Mode.OFF)) {//もし、OFFが含まれていなかったら。
			for(Name_Read_Mode nrm : nrms) {
				message_to_read = Name_Read_Mode_Manager.get_Name_Read_Mode_Value(nrm, member) + " " + message_to_read;
			}
		}

		//辞書のデータに基づいて　変換する単語を交換
		Config_Manage dic = Dictionary_Manage.get_Dictionary(guild.getIdLong());
		for(Entry<Object, Object> s : dic.get_Properties().entrySet()) {
			String key = (String) s.getKey();
			String yomi = (String) s.getValue();
			message_to_read = message_to_read.replace(key, yomi);
		}

		//読み上げ音声を生成し、そのパスを取得。
		String path = Narrate_Manage.generate_narrate_sound(message_to_read, e.getMessageId());
		if(path == null) {
			textchannel.sendMessage("読み上げするためのスクリプトが設定されていません。").complete();
			return;
		}
		Audio_File_Narrate afn = new Audio_File_Narrate();
		afn.path = path;
		afn.message = message_to_read;
		afn.textchannel = textchannel;
		afn.start();
		return;


	}

	public MessageEmbed get_Command_List() {
		String bot_command_prefix = Config_Manager.get_Bot_Property(Bot_Config_Keys.COMMAND_PREFIX);
		EmbedBuilder eb = new EmbedBuilder();
		eb.setColor(Color.GREEN);
		eb.setTitle("コマンドの使い方");
		eb.addField("ボイスチャンネル参加&読み上げ開始",bot_command_prefix,false);
		eb.addField("コマンド一覧を表示(これ)",bot_command_prefix + " help",false);
		eb.addField("ボイスチャンネル参加",bot_command_prefix + " join",false);
		eb.addField("ボイスチャンネル離脱",bot_command_prefix + " leave",false);
		eb.addField("読み上げ開始",bot_command_prefix + " n start",false);
		eb.addField("読み上げ停止",bot_command_prefix + " n stop",false);
		eb.addField("辞書追加",bot_command_prefix + " dic add [WORD] [READ]",false);
		eb.addField("辞書削除",bot_command_prefix + " dic remove [WORD]",false);
		eb.addField("コンフィグリロード",bot_command_prefix + " reload",false);
		return eb.build();
	}

	public void solve_command(MessageReceivedEvent e) {
		Guild guild = e.getGuild();
		Member member = e.getMember();
		TextChannel textchannel = e.getTextChannel();
		Message message = e.getMessage();
		String message_content = message.getContentDisplay();
		AudioManager audio_manager = guild.getAudioManager();
		String[] command_args = message_content.toLowerCase().split(" ");
		String[] original_args = message_content.split(" ");

		if(command_args.length == 1) {
			GuildVoiceState gvs = member.getVoiceState();
			if(!gvs.inVoiceChannel()) {
				textchannel.sendMessage("おいおい、" + member.getEffectiveName() + "。ボイスチャンネルに参加しといてくれよな。").complete();
				return;
			}
			VoiceChannel vc = gvs.getChannel();
			audio_manager.openAudioConnection(vc);
			narrate_textchannel.put(e.getGuild().getIdLong(), e.getTextChannel());

			EmbedBuilder eb = new EmbedBuilder();
			eb.setColor(Color.CYAN);
			eb.setTitle("読み上げ開始");
			eb.addField("読み上げるチャンネル", textchannel.getName(), false);
			eb.addField("参加したVC", vc.getName(), false);
			eb.addField("コメント","読み上げを開始しました",false);
			textchannel.sendMessage(eb.build()).complete();
			return;
		}

		if(command_args[1].equalsIgnoreCase("help")) {
			textchannel.sendMessage(get_Command_List()).complete();
		}else if(command_args[1].equalsIgnoreCase("join")) {

			if(command_args.length == 3) {//!jn join "name"
				String user_id = original_args[2];
				Member designation_member = e.getGuild().getMemberById(user_id);
				if(designation_member == null) {
					textchannel.sendMessage(user_id+"がIDの人は見つかりませんでした。").complete();
					return;
				}
				GuildVoiceState gvs_designate = designation_member.getVoiceState();
				if(!gvs_designate.inVoiceChannel()) {
					textchannel.sendMessage(designation_member.getEffectiveName() + "さんは、ボイスチャンネルに参加していません。").complete();
					return;
				}
				VoiceChannel vc = gvs_designate.getChannel();
				audio_manager.openAudioConnection(vc);
				textchannel.sendMessage(designation_member.getEffectiveName() + "さんが参加しているVCに参加しました。").complete();
			}else {
				GuildVoiceState gvs = member.getVoiceState();
				if(!gvs.inVoiceChannel()) {
					textchannel.sendMessage("おいおい、" + member.getEffectiveName() + "。ボイスチャンネルに参加しといてくれよな。").complete();
					return;
				}
				VoiceChannel vc = gvs.getChannel();
				audio_manager.openAudioConnection(vc);
				textchannel.sendMessage("VCに参加しました。").complete();
			}

		}else if(command_args[1].equalsIgnoreCase("leave")) {
			if(audio_manager.isConnected()) {
				audio_manager.closeAudioConnection();
				textchannel.sendMessage("VCから抜けました。").complete();
			}else {
				textchannel.sendMessage("VCに参加していません。").complete();
			}
		}else if(command_args[1].equalsIgnoreCase("n")) {
			if(command_args.length < 3) {
				textchannel.sendMessage("コマンドが違います。").complete();
				textchannel.sendMessage(get_Command_List()).complete();
				return;
			}
			if(command_args[2].equalsIgnoreCase("start")) {
				long guild_id = e.getGuild().getIdLong();
				TextChannel register = e.getTextChannel();
				narrate_textchannel.put(guild_id, register);

				EmbedBuilder eb = new EmbedBuilder();
				eb.setColor(Color.CYAN);
				eb.setTitle("読み上げ開始");
				eb.addField("読み上げるチャンネル", textchannel.getName(), false);
				eb.addField("コメント","読み上げを開始しました",false);
				textchannel.sendMessage(eb.build()).complete();
			}else if(command_args[2].equalsIgnoreCase("stop")) {
				long guild_id = e.getGuild().getIdLong();
				narrate_textchannel.remove(guild_id);

				EmbedBuilder eb = new EmbedBuilder();
				eb.setColor(Color.ORANGE);
				eb.setTitle("読み上げ終了");
				eb.addField("コメント","読み上げを終了しました",false);
				textchannel.sendMessage(eb.build()).complete();

				GuildMusicManager musicManager = Lavaplayer_Control.getGuildAudioPlayer(e.getGuild());
				musicManager.player.stopTrack();
			}
		}else if(command_args[1].equalsIgnoreCase("dic")) {
			if(command_args.length < 3) {
				textchannel.sendMessage("コマンドが違います。").complete();
				textchannel.sendMessage(get_Command_List()).complete();
				return;
			}
			Config_Manage dic = Dictionary_Manage.get_Dictionary(guild.getIdLong());
			Properties pro = dic.get_Properties();
			if(command_args[2].equalsIgnoreCase("add")) {
				if(command_args.length < 5) {
					textchannel.sendMessage("コマンドが違います。").complete();
					textchannel.sendMessage(get_Command_List()).complete();
					return;
				}
				String word = original_args[3];
				String yomi = original_args[4];
				pro.setProperty(word, yomi);

				EmbedBuilder eb = new EmbedBuilder();
				eb.setColor(Color.GREEN);
				eb.setTitle("読みを登録しました");
				eb.addField("単語",word,true);
				eb.addField("読み",yomi,true);
				textchannel.sendMessage(eb.build()).complete();
				dic.save_Config("A dictionary for " + String.valueOf(guild.getIdLong()));
			}else if(command_args[2].equalsIgnoreCase("remove")) {
				if(command_args.length < 4) {
					textchannel.sendMessage("コマンドが違います。").complete();
					textchannel.sendMessage(get_Command_List()).complete();
					return;
				}
				String word = original_args[3];
				Object result = pro.remove(word);
				if(result == null) {
					textchannel.sendMessage(word + "で登録されている辞書はありません。").complete();
				}else {
					textchannel.sendMessage(word + "の辞書登録を削除しました。").complete();
					dic.save_Config("A dictionary for " + String.valueOf(guild.getIdLong()));
				}
			}
		}
		else if(command_args[1].equalsIgnoreCase("reload")) {
			Config_Manager.get_Bot_Config_Manage().load_Config();
			textchannel.sendMessage("リロードしました。").complete();
		}
		else if(command_args[1].equalsIgnoreCase("manage")) {
			if(command_args[2].equalsIgnoreCase("shutdown")) {
				textchannel.sendMessage("Good bye.").complete();
				Main.get_JDA().shutdownNow();
				System.out.println("SHUTING DOWN...");
				try {
					Thread.sleep(200);
				} catch (InterruptedException e1) {
					// TODO 自動生成された catch ブロック
					e1.printStackTrace();
				}
				System.exit(0);
			}
		}

	}

	@Override
	public void onGuildVoiceJoin(GuildVoiceJoinEvent e) {

		if(!Boolean.parseBoolean(Config_Manager.get_Bot_Property(Bot_Config_Keys.AUTO_JOIN)))
			return;

		Guild guild = e.getGuild();
		AudioManager audio_manager = guild.getAudioManager();
		VoiceChannel vc = e.getChannelJoined();

		if( narrate_textchannel.containsKey(guild.getIdLong()) && audio_manager.isConnected() )
			return;

		// is bot
		if( e.getMember().getUser().isBot() )
			return;

		if ( narrate_textchannel.containsKey(guild.getIdLong()) == false ) {
			TextChannel tc = null;
			if(last_text_channel.get(guild.getIdLong()) == null) {
				if(tc == null) tc = e.getMember().getDefaultChannel();
				if(tc == null) tc = guild.getTextChannels().get(0);
				if(tc == null) return;
			}else {
				tc = guild.getTextChannelById(last_text_channel.get(guild.getIdLong()));
			}


			narrate_textchannel.put(guild.getIdLong(), tc);
		}

		TextChannel channel_to_read = narrate_textchannel.get(guild.getIdLong());

		audio_manager.openAudioConnection(vc);

		EmbedBuilder eb = new EmbedBuilder();
		eb.setColor(Color.CYAN);
		eb.setTitle("読み上げ開始");
		eb.addField("読み上げるチャンネル", channel_to_read.getName(), false);
		eb.addField("参加したVC", vc.getName(), false);
		eb.addField("コメント","読み上げを開始しました",false);
		channel_to_read.sendMessage(eb.build()).complete();
		return;
	}

	@Override
	public void onGuildVoiceLeave(GuildVoiceLeaveEvent e) {

		if(!Boolean.parseBoolean(Config_Manager.get_Bot_Property(Bot_Config_Keys.AUTO_LEAVE)))
			return;

		Guild guild = e.getGuild();
		AudioManager audio_manager = guild.getAudioManager();
		VoiceChannel vc = e.getChannelLeft();

		if ( ! narrate_textchannel.containsKey(guild.getIdLong()) ) return;

		//when bot is not connected to any vc.
		if ( ! audio_manager.isConnected() )
			return;

		//when event vc and connected vc are different.
		if ( audio_manager.getConnectedChannel().getIdLong() != vc.getIdLong())
			return;

		// when everyone isn't got disconnected
		int human_count = 0;
		for(Member member : vc.getMembers()) {
			if(!member.getUser().isBot())
				human_count++;
		}
		if ( human_count > 0 )
			return;

		TextChannel channel_to_read = narrate_textchannel.get(guild.getIdLong());

		EmbedBuilder eb = new EmbedBuilder();
		eb.setColor(Color.ORANGE);
		eb.setTitle("読み上げ終了");
		eb.addField("コメント","読み上げを終了しました",false);
		eb.addField("抜けたVC",vc.getName(),false);
		channel_to_read.sendMessage(eb.build()).complete();

		narrate_textchannel.remove(guild.getIdLong());

		GuildMusicManager musicManager = Lavaplayer_Control.getGuildAudioPlayer(e.getGuild());
		musicManager.player.stopTrack();
		musicManager.player.destroy();

		audio_manager.closeAudioConnection();



	}


}
