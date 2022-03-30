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
					textchannel.sendMessage("�G���[�F�ǂݏグ�t�@�C���̐������x�����܂��B").complete();
					textchannel.sendMessage("�ǂݏグ���Ȃ��������e�F" + message).complete();
					return;
				}
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					// TODO �����������ꂽ catch �u���b�N
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
			textchannel.sendMessage("�{�C�X�`�����l���ɎQ�����Ă��Ȃ����߁B�ǂݏグ�ł��܂���B�ǂݏグ�𒆎~���܂��B").complete();
			narrate_textchannel.remove(guild.getIdLong());
			return;
		}

		String message_to_read = message_content;
		if(message_to_read.toLowerCase().startsWith("http")) {
			String url_read_mode = Config_Manager.get_Bot_Property(Bot_Config_Keys.URL_READ_MODE);
			if(url_read_mode.equalsIgnoreCase("message")) {
				message_to_read = "URL�����M����܂����B";
			}else if(url_read_mode.equalsIgnoreCase("no")) {
				return;
			}

		}

		//���O�̓ǂݏグ
		List<Name_Read_Mode> nrms = Name_Read_Mode_Manager.parse_Name_Read_Mode(Config_Manager.get_Bot_Property(Bot_Config_Keys.NAME_READ_MODE));
		Collections.reverse(nrms);
		if(!nrms.contains(Name_Read_Mode.OFF)) {//�����AOFF���܂܂�Ă��Ȃ�������B
			for(Name_Read_Mode nrm : nrms) {
				message_to_read = Name_Read_Mode_Manager.get_Name_Read_Mode_Value(nrm, member) + " " + message_to_read;
			}
		}

		//�����̃f�[�^�Ɋ�Â��ā@�ϊ�����P�������
		Config_Manage dic = Dictionary_Manage.get_Dictionary(guild.getIdLong());
		for(Entry<Object, Object> s : dic.get_Properties().entrySet()) {
			String key = (String) s.getKey();
			String yomi = (String) s.getValue();
			message_to_read = message_to_read.replace(key, yomi);
		}

		//�ǂݏグ�����𐶐����A���̃p�X���擾�B
		String path = Narrate_Manage.generate_narrate_sound(message_to_read, e.getMessageId());
		if(path == null) {
			textchannel.sendMessage("�ǂݏグ���邽�߂̃X�N���v�g���ݒ肳��Ă��܂���B").complete();
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
		eb.setTitle("�R�}���h�̎g����");
		eb.addField("�{�C�X�`�����l���Q��&�ǂݏグ�J�n",bot_command_prefix,false);
		eb.addField("�R�}���h�ꗗ��\��(����)",bot_command_prefix + " help",false);
		eb.addField("�{�C�X�`�����l���Q��",bot_command_prefix + " join",false);
		eb.addField("�{�C�X�`�����l�����E",bot_command_prefix + " leave",false);
		eb.addField("�ǂݏグ�J�n",bot_command_prefix + " n start",false);
		eb.addField("�ǂݏグ��~",bot_command_prefix + " n stop",false);
		eb.addField("�����ǉ�",bot_command_prefix + " dic add [WORD] [READ]",false);
		eb.addField("�����폜",bot_command_prefix + " dic remove [WORD]",false);
		eb.addField("�R���t�B�O�����[�h",bot_command_prefix + " reload",false);
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
				textchannel.sendMessage("���������A" + member.getEffectiveName() + "�B�{�C�X�`�����l���ɎQ�����Ƃ��Ă����ȁB").complete();
				return;
			}
			VoiceChannel vc = gvs.getChannel();
			audio_manager.openAudioConnection(vc);
			narrate_textchannel.put(e.getGuild().getIdLong(), e.getTextChannel());

			EmbedBuilder eb = new EmbedBuilder();
			eb.setColor(Color.CYAN);
			eb.setTitle("�ǂݏグ�J�n");
			eb.addField("�ǂݏグ��`�����l��", textchannel.getName(), false);
			eb.addField("�Q������VC", vc.getName(), false);
			eb.addField("�R�����g","�ǂݏグ���J�n���܂���",false);
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
					textchannel.sendMessage(user_id+"��ID�̐l�͌�����܂���ł����B").complete();
					return;
				}
				GuildVoiceState gvs_designate = designation_member.getVoiceState();
				if(!gvs_designate.inVoiceChannel()) {
					textchannel.sendMessage(designation_member.getEffectiveName() + "����́A�{�C�X�`�����l���ɎQ�����Ă��܂���B").complete();
					return;
				}
				VoiceChannel vc = gvs_designate.getChannel();
				audio_manager.openAudioConnection(vc);
				textchannel.sendMessage(designation_member.getEffectiveName() + "���񂪎Q�����Ă���VC�ɎQ�����܂����B").complete();
			}else {
				GuildVoiceState gvs = member.getVoiceState();
				if(!gvs.inVoiceChannel()) {
					textchannel.sendMessage("���������A" + member.getEffectiveName() + "�B�{�C�X�`�����l���ɎQ�����Ƃ��Ă����ȁB").complete();
					return;
				}
				VoiceChannel vc = gvs.getChannel();
				audio_manager.openAudioConnection(vc);
				textchannel.sendMessage("VC�ɎQ�����܂����B").complete();
			}

		}else if(command_args[1].equalsIgnoreCase("leave")) {
			if(audio_manager.isConnected()) {
				audio_manager.closeAudioConnection();
				textchannel.sendMessage("VC���甲���܂����B").complete();
			}else {
				textchannel.sendMessage("VC�ɎQ�����Ă��܂���B").complete();
			}
		}else if(command_args[1].equalsIgnoreCase("n")) {
			if(command_args.length < 3) {
				textchannel.sendMessage("�R�}���h���Ⴂ�܂��B").complete();
				textchannel.sendMessage(get_Command_List()).complete();
				return;
			}
			if(command_args[2].equalsIgnoreCase("start")) {
				long guild_id = e.getGuild().getIdLong();
				TextChannel register = e.getTextChannel();
				narrate_textchannel.put(guild_id, register);

				EmbedBuilder eb = new EmbedBuilder();
				eb.setColor(Color.CYAN);
				eb.setTitle("�ǂݏグ�J�n");
				eb.addField("�ǂݏグ��`�����l��", textchannel.getName(), false);
				eb.addField("�R�����g","�ǂݏグ���J�n���܂���",false);
				textchannel.sendMessage(eb.build()).complete();
			}else if(command_args[2].equalsIgnoreCase("stop")) {
				long guild_id = e.getGuild().getIdLong();
				narrate_textchannel.remove(guild_id);

				EmbedBuilder eb = new EmbedBuilder();
				eb.setColor(Color.ORANGE);
				eb.setTitle("�ǂݏグ�I��");
				eb.addField("�R�����g","�ǂݏグ���I�����܂���",false);
				textchannel.sendMessage(eb.build()).complete();

				GuildMusicManager musicManager = Lavaplayer_Control.getGuildAudioPlayer(e.getGuild());
				musicManager.player.stopTrack();
			}
		}else if(command_args[1].equalsIgnoreCase("dic")) {
			if(command_args.length < 3) {
				textchannel.sendMessage("�R�}���h���Ⴂ�܂��B").complete();
				textchannel.sendMessage(get_Command_List()).complete();
				return;
			}
			Config_Manage dic = Dictionary_Manage.get_Dictionary(guild.getIdLong());
			Properties pro = dic.get_Properties();
			if(command_args[2].equalsIgnoreCase("add")) {
				if(command_args.length < 5) {
					textchannel.sendMessage("�R�}���h���Ⴂ�܂��B").complete();
					textchannel.sendMessage(get_Command_List()).complete();
					return;
				}
				String word = original_args[3];
				String yomi = original_args[4];
				pro.setProperty(word, yomi);

				EmbedBuilder eb = new EmbedBuilder();
				eb.setColor(Color.GREEN);
				eb.setTitle("�ǂ݂�o�^���܂���");
				eb.addField("�P��",word,true);
				eb.addField("�ǂ�",yomi,true);
				textchannel.sendMessage(eb.build()).complete();
				dic.save_Config("A dictionary for " + String.valueOf(guild.getIdLong()));
			}else if(command_args[2].equalsIgnoreCase("remove")) {
				if(command_args.length < 4) {
					textchannel.sendMessage("�R�}���h���Ⴂ�܂��B").complete();
					textchannel.sendMessage(get_Command_List()).complete();
					return;
				}
				String word = original_args[3];
				Object result = pro.remove(word);
				if(result == null) {
					textchannel.sendMessage(word + "�œo�^����Ă��鎫���͂���܂���B").complete();
				}else {
					textchannel.sendMessage(word + "�̎����o�^���폜���܂����B").complete();
					dic.save_Config("A dictionary for " + String.valueOf(guild.getIdLong()));
				}
			}
		}
		else if(command_args[1].equalsIgnoreCase("reload")) {
			Config_Manager.get_Bot_Config_Manage().load_Config();
			textchannel.sendMessage("�����[�h���܂����B").complete();
		}
		else if(command_args[1].equalsIgnoreCase("manage")) {
			if(command_args[2].equalsIgnoreCase("shutdown")) {
				textchannel.sendMessage("Good bye.").complete();
				Main.get_JDA().shutdownNow();
				System.out.println("SHUTING DOWN...");
				try {
					Thread.sleep(200);
				} catch (InterruptedException e1) {
					// TODO �����������ꂽ catch �u���b�N
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
		eb.setTitle("�ǂݏグ�J�n");
		eb.addField("�ǂݏグ��`�����l��", channel_to_read.getName(), false);
		eb.addField("�Q������VC", vc.getName(), false);
		eb.addField("�R�����g","�ǂݏグ���J�n���܂���",false);
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
		eb.setTitle("�ǂݏグ�I��");
		eb.addField("�R�����g","�ǂݏグ���I�����܂���",false);
		eb.addField("������VC",vc.getName(),false);
		channel_to_read.sendMessage(eb.build()).complete();

		narrate_textchannel.remove(guild.getIdLong());

		GuildMusicManager musicManager = Lavaplayer_Control.getGuildAudioPlayer(e.getGuild());
		musicManager.player.stopTrack();
		musicManager.player.destroy();

		audio_manager.closeAudioConnection();



	}


}
