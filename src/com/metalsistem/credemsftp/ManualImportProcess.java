package com.metalsistem.credemsftp;

import java.io.File;
import java.io.FileInputStream;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MInvoice;
import org.compiere.model.MWindow;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;

import com.metalsistem.credemsftp.utils.InvoiceParser;
import com.metalsistem.credemsftp.utils.InvoiceReceived;
import com.metalsistem.credemsftp.utils.InvoiceService;
import com.metalsistem.credemsftp.utils.Utils;

@Process
public class ManualImportProcess extends SvrProcess {

	String xml;
	private final InvoiceParser invoiceParser = new InvoiceParser();
	private final InvoiceService invoiceService = new InvoiceService();

	@Override
	protected void prepare() {
		ProcessInfoParameter[] params = getParameter();

		for (ProcessInfoParameter param : params) {
			String name = param.getParameterName();
			if ("File".equals(name)) {
				xml = (String) param.getParameter();
			}
		}
	}

	@Override
	protected String doIt() throws Exception {
		if (Env.getAD_Org_ID(getCtx()) <= 0)
			return Utils.getMessage("LIT_MsErrorOrgNotSelected");

		FileInputStream is = new FileInputStream(new File(xml));
		byte[] data = is.readAllBytes();
		is.close();

		byte[] parsedData = invoiceParser.getXml(data);
		InvoiceReceived inv = invoiceParser.getInvoiceFromXml(parsedData);
		if (inv.getErrorMsg().isBlank()) {
			inv = invoiceService.saveInvoice(inv);
			invoiceService.archiveEInvoice(parsedData, inv);
		} else {
			return inv.getErrorMsg();
		}

		int winUUID = Env.getZoomWindowID(MInvoice.Table_ID, inv.get_ID());
		MWindow bpWindow = MWindow.get(winUUID);
		return "Processo completato: " + Utils.getUrlZoom(inv, bpWindow.get_UUID(), inv.getDocumentNo());
	}

	
}
