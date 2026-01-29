package com.metalsistem.credemsftp;

import java.io.File;
import java.io.FileInputStream;

import org.adempiere.base.annotation.Process;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;

import com.metalsistem.credemsftp.utils.InvoiceParser;
import com.metalsistem.credemsftp.utils.InvoiceReceived;
import com.metalsistem.credemsftp.utils.InvoiceService;
import com.metalsistem.credemsftp.utils.Utils;

@Process
public class ManualImportProcess extends SvrProcess {

	private String xml;
	private final InvoiceParser invoiceParser = new InvoiceParser();
	private final InvoiceService invoiceService = new InvoiceService();

	@Override
	protected void prepare() {
		ProcessInfoParameter[] params = getParameter();

		for (ProcessInfoParameter param : params) {
			String name = param.getParameterName();
			if (param.getParameter() != null) {
				if ("File".equals(name)) {
					xml = param.getParameterAsString();
				}
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
		} else if (InvoiceParser.FATTURA_DUPLICATA.equals(inv.getErrorMsg())) {
			log.warning(InvoiceParser.FATTURA_DUPLICATA);
			addLog("Fattura " + inv.getDocumentNo() + " già presente nel sistema ");
			return InvoiceParser.FATTURA_DUPLICATA;
		} else {
			return inv.getErrorMsg();
		}

		String zoomLink = Utils.getUrlDirectZoom(inv);
		return "La fattura è stata importata: " + zoomLink;
	}

}
