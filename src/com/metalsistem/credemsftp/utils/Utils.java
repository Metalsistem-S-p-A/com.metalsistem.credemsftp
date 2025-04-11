package com.metalsistem.credemsftp.utils;

import java.text.MessageFormat;

import org.compiere.util.Env;
import org.compiere.util.Msg;

public class Utils {
	public static String getMessage(String errorName, Object... args) {
		String err_message = Msg.translate(Env.getAD_Language(Env.getCtx()), errorName);
		return MessageFormat.format(err_message, args);
	}
}
