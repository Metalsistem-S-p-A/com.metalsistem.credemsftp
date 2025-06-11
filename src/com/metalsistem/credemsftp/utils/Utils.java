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

	public static String getUrlZoom(PO po, String windowUUID, String text) {
		StringBuilder url = new StringBuilder("");
		url.append("<a href=\"javascript:void(0)\" class=\"rp-href\" onclick=\"window.idempiere.zoomWindow(@"
				+ "#clientInfo_BroadcastComponentId" + "@, '");
		url.append(po.get_KeyColumns()[0]);
		url.append("', '").append(po.get_ID()).append("','").append(windowUUID).append("')\">");
		url.append(text);
		url.append("</a>");

		return url.toString();
	}
}
