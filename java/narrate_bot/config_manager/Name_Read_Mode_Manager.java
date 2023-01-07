package narrate_bot.config_manager;

import java.util.ArrayList;
import java.util.List;

import net.dv8tion.jda.api.entities.Member;

public class Name_Read_Mode_Manager {

	public static enum Name_Read_Mode{
		OFF,
		USER_ID,
		NICKNAME,
		NAME
	}

	public static List<Name_Read_Mode> parse_Name_Read_Mode(String s){
		if(s == null) return null;
		List<Name_Read_Mode> nrm_list = new ArrayList<Name_Read_Mode>();
		String[] modes = s.split(",");
		for(String mode : modes) {
			Name_Read_Mode nrm = Name_Read_Mode.valueOf(mode.toUpperCase());
			if(nrm == null) return null;
			nrm_list.add(nrm);
		}
		return nrm_list;
	}

	public static String get_Name_Read_Mode_Value(Name_Read_Mode nrm,Member mem) {
		switch(nrm) {
		case USER_ID:
			return mem.getId();
		case NICKNAME:
			String nick_name = mem.getNickname();
			if(nick_name == null) return mem.getEffectiveName();
			else return nick_name;
		case NAME:
			return mem.getEffectiveName();
		default:
			return "ERROR";
		}
	}



}
