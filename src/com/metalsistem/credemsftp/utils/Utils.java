package com.metalsistem.credemsftp.utils;

import java.text.MessageFormat;

import org.compiere.model.PO;
import org.compiere.util.Env;
import org.compiere.util.Msg;

public class Utils {
	public static String getMessage(String errorName, Object... args) {
		String err_message = Msg.translate(Env.getAD_Language(Env.getCtx()), errorName);
		return MessageFormat.format(err_message, args);
	}

	public static boolean isBlank(String string) {
		return string == null || string.isBlank();
	}

	public static String getUrlDirectZoom(PO inv) {
		return String.format("<a href=\"javascript:void(0)\" onClick=\"window.idempiere.directZoom('%s',%d);\">%s</a>",
				inv.get_KeyColumns()[0], inv.get_ID(), inv.get_ID());
	}
}
