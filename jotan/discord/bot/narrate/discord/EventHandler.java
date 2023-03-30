package jotan.discord.bot.narrate.discord;

import java.awt.Color;
import java.io.File;
import java.net.http.WebSocket.Listener;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import jotan.discord.bot.narrate.Main;
import jotan.discord.bot.narrate.discord.lavaplayer.GuildMusicManager;
import jotan.discord.bot.narrate.managerConfig.ManagerConfig;
import jotan.discord.bot.narrate.managerConfig.ManagerDictionary;
import jotan.discord.bot.narrate.managerConfig.ManagerNameReadMode;
import jotan.discord.bot.narrate.managerConfig.ManagerConfig.BotConfigKey;
import jotan.discord.bot.narrate.managerConfig.ManagerConfig.Config;
import jotan.discord.bot.narrate.managerConfig.ManagerNameReadMode.Name_Read_Mode;

import java.util.Properties;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;

public class EventHandler extends ListenerAdapter implements Listener {

	HashMap<Long, TextChannel> narrateTextChannel = new HashMap<Long, TextChannel>();
	private HashMap<Long, Long> lastUsedTextChannel = new HashMap<Long, Long>();

	class AudioFileNarrater extends Thread {
		public String path = "";
		public String message = "";
		public TextChannel textchannel;
		public long start_time = Calendar.getInstance().getTimeInMillis();

		@Override
		public void run() {
			start_time = Calendar.getInstance().getTimeInMillis();
			File file = new File(path);
			long timeout = 2000;
			try {
				timeout = Long.parseLong(ManagerConfig.getConfigValue(BotConfigKey.NARRATE_GENERATE_TIMEOUT));
				if (timeout == -1)
					timeout = Long.MAX_VALUE;
				else
					timeout *= 1000;
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("[WARN] : generate_sound_timeout is not set");
			}
			while (!file.exists()) {
				long current_time = Calendar.getInstance().getTimeInMillis();
				if (current_time - start_time > timeout) {
					textchannel.sendMessage("音声生成タイムアウト「" + message + "」").complete();
					return;
				}
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			int delay = -1;
			try {
				delay = Integer.parseInt(ManagerConfig.getConfigValue(BotConfigKey.NARRATE_GENERATE_DELAY));
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("[WARN] : file_export_delay is not proper value.");
			}

			if (delay > 0) {
				try {
					Thread.sleep(delay);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			System.out.println(message + " => " + path);
			ControlLavaplayer.playLocalFile(textchannel, path);
		}
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent e) {
		if (!e.getChannelType().equals(ChannelType.TEXT))
			return;

		Guild guild = e.getGuild();
		Member member = e.getMember();
		TextChannel textchannel = e.getGuildChannel().asTextChannel();
		Message message = e.getMessage();
		String message_content = message.getContentDisplay();
		AudioManager audio_manager = guild.getAudioManager();

		if (member.getUser().isBot())
			return;

		if (message_content.toLowerCase().split(" ")[0]
				.equalsIgnoreCase(ManagerConfig.getConfigValue(BotConfigKey.DISCORD_COMMAND_PREFIX))) {
			solveCommand(e);
			return;
		}

		lastUsedTextChannel.put(guild.getIdLong(), textchannel.getIdLong());

		if (narrateTextChannel.get(guild.getIdLong()) == null)
			return;
		if (textchannel.getIdLong() != narrateTextChannel.get(guild.getIdLong()).getIdLong())
			return;
		if (!audio_manager.isConnected()) {
			textchannel.sendMessage("VCに参加していないため、読み上げを停止します。").complete();
			narrateTextChannel.remove(guild.getIdLong());
			return;
		}

		String message_to_read = message_content;

		if (message_to_read.startsWith(ManagerConfig.getConfigValue(BotConfigKey.NARRATE_IGNORE_PREFIX))) {
			return;
		}

		// URL READ MODE
		if (message_to_read.toLowerCase().startsWith("http")) {
			String url_read_mode = ManagerConfig.getConfigValue(BotConfigKey.NARRATE_URL_MODE);
			if (url_read_mode.equalsIgnoreCase("message")) {
				message_to_read = "URLが送信されました";
			} else if (url_read_mode.equalsIgnoreCase("no")) {
				return;
			}
		}

		// NAME READ MODE
		List<Name_Read_Mode> nrms = ManagerNameReadMode
				.parse_Name_Read_Mode(ManagerConfig.getConfigValue(BotConfigKey.NARRATE_NAME_MODE));
		Collections.reverse(nrms);
		if (!nrms.contains(Name_Read_Mode.OFF)) {
			for (Name_Read_Mode nrm : nrms) {
				message_to_read = ManagerNameReadMode.get_Name_Read_Mode_Value(nrm, member) + " " + message_to_read;
			}
		}

		// REPLACE DICTIONARY WORDS
		Config dic = ManagerDictionary.get_Dictionary(guild.getIdLong());
		for (Entry<Object, Object> s : dic.getProperties().entrySet()) {
			String key = (String) s.getKey();
			String yomi = (String) s.getValue();
			message_to_read = message_to_read.replace(key, yomi);
		}

		// GENERATE NARRATE AUDIO
		String path = ManagerNarrate.generateNarrate(message_to_read, e.getMessageId());
		if (path == null) {
			textchannel.sendMessage("音声の生成に失敗しました。").complete();
			return;
		}
		AudioFileNarrater afn = new AudioFileNarrater();
		afn.path = path;
		afn.message = message_to_read;
		afn.textchannel = textchannel;
		afn.start();
		return;

	}

	public MessageEmbed getCommandList() {
		String bot_command_prefix = ManagerConfig.getConfigValue(BotConfigKey.DISCORD_COMMAND_PREFIX);
		EmbedBuilder eb = new EmbedBuilder();
		eb.setColor(Color.GREEN);
		eb.setTitle("コマンドの使い方");
		eb.addField("VC参加&読み上げ開始", bot_command_prefix, false);
		eb.addField("ヘルプを表示(これ)", bot_command_prefix + " help", false);
		eb.addField("VC-参加", bot_command_prefix + " join", false);
		eb.addField("VC-離脱", bot_command_prefix + " leave", false);
		eb.addField("読み上げ-開始", bot_command_prefix + " n start", false);
		eb.addField("読み上げ-停止", bot_command_prefix + " n stop", false);
		eb.addField("辞書-単語追加", bot_command_prefix + " dic add [WORD] [READ]", false);
		eb.addField("辞書-単語削除", bot_command_prefix + " dic remove [WORD]", false);
		eb.addField("コンフィグ-リロード", bot_command_prefix + " reload", false);
		return eb.build();
	}

	public void solveCommand(MessageReceivedEvent e) {
		if (!e.getChannelType().equals(ChannelType.TEXT))
			return;

		Guild guild = e.getGuild();
		Member member = e.getMember();
		Message message = e.getMessage();
		String message_content = message.getContentDisplay();
		TextChannel channel = e.getGuildChannel().asTextChannel();
		AudioManager audio_manager = guild.getAudioManager();
		String[] command_args = message_content.toLowerCase().split(" ");
		String[] original_args = message_content.split(" ");

		if (command_args.length == 1) {
			GuildVoiceState gvs = member.getVoiceState();
			if (!gvs.inAudioChannel()) {
				channel.sendMessage("おいおい、" + member.getEffectiveName() + "。VCに参加してからこのコマンドを実行してくれ。").complete();
				return;
			}
			AudioChannel vc = gvs.getChannel();
			audio_manager.openAudioConnection(vc);
			narrateTextChannel.put(e.getGuild().getIdLong(), channel);

			EmbedBuilder eb = new EmbedBuilder();
			eb.setColor(Color.CYAN);
			eb.setTitle("読み上げ開始");
			eb.addField("読み上げるチャンネル名", channel.getName(), false);
			eb.addField("VC", vc.getName(), false);
			channel.sendMessageEmbeds(eb.build()).complete();
			return;
		}

		if (command_args[1].equalsIgnoreCase("help")) {
			channel.sendMessageEmbeds(getCommandList()).complete();
		} else if (command_args[1].equalsIgnoreCase("join")) {

			if (command_args.length == 3) {// !jn join "name"
				String user_id = original_args[2];
				Member designation_member = e.getGuild().getMemberById(user_id);
				if (designation_member == null) {
					channel.sendMessage(user_id + "は有効なIDではありません。").complete();
					return;
				}
				GuildVoiceState gvs_designate = designation_member.getVoiceState();
				if (!gvs_designate.inAudioChannel()) {
					channel.sendMessage(designation_member.getEffectiveName() + "はVCに参加していません。").complete();
					return;
				}
				AudioChannel vc = gvs_designate.getChannel();
				audio_manager.openAudioConnection(vc);
				channel.sendMessage(designation_member.getEffectiveName() + "が参加しているVCに参加しました。").complete();
			} else {
				GuildVoiceState gvs = member.getVoiceState();
				if (!gvs.inAudioChannel()) {
					channel.sendMessage("おいおい、" + member.getEffectiveName() + "。VCに参加してからこのコマンドを実行してくれ。").complete();
					return;
				}
				AudioChannel vc = gvs.getChannel();
				audio_manager.openAudioConnection(vc);
			}

		} else if (command_args[1].equalsIgnoreCase("leave")) {
			if (audio_manager.isConnected()) {
				audio_manager.closeAudioConnection();
			} else {
				channel.sendMessage("VCに入っていません").complete();
			}
		} else if (command_args[1].equalsIgnoreCase("n")) {
			if (command_args.length < 3) {
				channel.sendMessageEmbeds(getCommandList()).complete();
				return;
			}
			if (command_args[2].equalsIgnoreCase("start")) {
				long guild_id = e.getGuild().getIdLong();
				narrateTextChannel.put(guild_id, channel);

				EmbedBuilder eb = new EmbedBuilder();
				eb.setColor(Color.CYAN);
				eb.setTitle("読み上げ開始");
				eb.addField("読み上げるチャンネル", channel.getName(), false);
				channel.sendMessageEmbeds(eb.build()).complete();
			} else if (command_args[2].equalsIgnoreCase("stop")) {
				long guild_id = e.getGuild().getIdLong();
				narrateTextChannel.remove(guild_id);

				EmbedBuilder eb = new EmbedBuilder();
				eb.setColor(Color.ORANGE);
				eb.setTitle("読み上げ停止");
				channel.sendMessageEmbeds(eb.build()).complete();

				GuildMusicManager musicManager = ControlLavaplayer.getGuildAudioPlayer(e.getGuild());
				musicManager.player.stopTrack();
			}
		} else if (command_args[1].equalsIgnoreCase("dic")) {
			if (command_args.length < 3) {
				channel.sendMessageEmbeds(getCommandList()).complete();
				return;
			}
			Config dic = ManagerDictionary.get_Dictionary(guild.getIdLong());
			Properties pro = dic.getProperties();
			if (command_args[2].equalsIgnoreCase("add")) {
				if (command_args.length < 5) {
					channel.sendMessageEmbeds(getCommandList()).complete();
					return;
				}
				String word = original_args[3];
				String yomi = original_args[4];
				pro.setProperty(word, yomi);

				EmbedBuilder eb = new EmbedBuilder();
				eb.setColor(Color.GREEN);
				eb.setTitle("辞書を追加");
				eb.addField("単語", word, true);
				eb.addField("読み", yomi, true);
				channel.sendMessageEmbeds(eb.build()).complete();
				dic.save("A dictionary for " + String.valueOf(guild.getIdLong()));
			} else if (command_args[2].equalsIgnoreCase("remove")) {
				if (command_args.length < 4) {
					channel.sendMessageEmbeds(getCommandList()).complete();
					return;
				}
				String word = original_args[3];
				Object result = pro.remove(word);
				if (result == null) {
					channel.sendMessage(word + "を辞書から除去できませんでした。").complete();
				} else {
					channel.sendMessage(word + "を辞書から除去しました。").complete();
					dic.save("A dictionary for " + String.valueOf(guild.getIdLong()));
				}
			}
		} else if (command_args[1].equalsIgnoreCase("reload")) {
			ManagerConfig.getConfigInstance().load();
			channel.sendMessage("Reload Complete!").complete();
		} else if (command_args[1].equalsIgnoreCase("manage")) {
			if (command_args[2].equalsIgnoreCase("shutdown")) {
				channel.sendMessage("Good bye.").complete();
				Main.get_JDA().shutdownNow();
				System.out.println("SHUTING DOWN...");
				try {
					Thread.sleep(200);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				System.exit(0);
			}
		}

	}

	@Override
	public void onGuildVoiceUpdate(GuildVoiceUpdateEvent e) {
		if (e.getChannelJoined() != null)
			onGuildVoiceJoin(e);
		else
			onGuildVoiceLeave(e);
	}

	private void onGuildVoiceJoin(GuildVoiceUpdateEvent e) {
		if (!Boolean.parseBoolean(ManagerConfig.getConfigValue(BotConfigKey.AUTO_JOIN)))
			return;

		Guild guild = e.getGuild();
		AudioManager audio_manager = guild.getAudioManager();
		AudioChannel vc = e.getChannelJoined();

		if (narrateTextChannel.containsKey(guild.getIdLong()) && audio_manager.isConnected())
			return;

		// is bot
		if (e.getMember().getUser().isBot())
			return;

		if (narrateTextChannel.containsKey(guild.getIdLong()) == false) {
			TextChannel tc = null;
			if (lastUsedTextChannel.get(guild.getIdLong()) == null) {
				if (tc == null)
					tc = guild.getTextChannels().get(0);
				if (tc == null)
					return;
			} else {
				tc = guild.getTextChannelById(lastUsedTextChannel.get(guild.getIdLong()));
			}

			narrateTextChannel.put(guild.getIdLong(), tc);
		}

		TextChannel channel_to_read = narrateTextChannel.get(guild.getIdLong());

		audio_manager.openAudioConnection(vc);

		EmbedBuilder eb = new EmbedBuilder();
		eb.setColor(Color.CYAN);
		eb.setTitle("読み上げ開始");
		eb.addField("読み上げるチャンネル名", channel_to_read.getName(), false);
		eb.addField("VC", vc.getName(), false);
		channel_to_read.sendMessageEmbeds(eb.build()).complete();
		return;
	}

	private void onGuildVoiceLeave(GuildVoiceUpdateEvent e) {
		if (!Boolean.parseBoolean(ManagerConfig.getConfigValue(BotConfigKey.AUTO_LEAVE)))
			return;
		Guild guild = e.getGuild();
		AudioManager audio_manager = guild.getAudioManager();
		AudioChannel vc = e.getChannelLeft();

		if (!narrateTextChannel.containsKey(guild.getIdLong()))
			return;

		// when bot is not connected to any vc.
		if (!audio_manager.isConnected())
			return;

		// when event vc and connected vc are different.
		if (audio_manager.getConnectedChannel().getIdLong() != vc.getIdLong())
			return;

		// when everyone isn't got disconnected
		int human_count = 0;
		for (Member member : vc.getMembers()) {
			if (!member.getUser().isBot())
				human_count++;
		}
		if (human_count > 0)
			return;

		TextChannel channel_to_read = narrateTextChannel.get(guild.getIdLong());

		EmbedBuilder eb = new EmbedBuilder();
		eb.setColor(Color.ORANGE);
		eb.setTitle("読み上げ終了");
		eb.addField("VC", vc.getName(), false);
		channel_to_read.sendMessageEmbeds(eb.build()).complete();

		narrateTextChannel.remove(guild.getIdLong());

		GuildMusicManager musicManager = ControlLavaplayer.getGuildAudioPlayer(e.getGuild());
		musicManager.player.stopTrack();
		musicManager.player.destroy();

		audio_manager.closeAudioConnection();

	}

}
