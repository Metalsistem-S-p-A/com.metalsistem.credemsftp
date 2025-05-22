package com.metalsistem.credemsftp;

import java.io.File;
import java.io.FileInputStream;

import org.adempiere.base.annotation.Process;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;

import com.metalsistem.credemsftp.utils.InvoiceParser;
import com.metalsistem.credemsftp.utils.InvoiceReceived;
import com.metalsistem.credemsftp.utils.InvoiceService;

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
		FileInputStream is = new FileInputStream(new File(xml));
		byte[] data = is.readAllBytes();
		is.close();

		byte[] parsedData = invoiceParser.parseByteXML(data);
		InvoiceReceived inv = invoiceParser.getInvoiceFromXml(parsedData);
		if (inv != null) {
			inv = invoiceService.saveInvoice(inv);
			invoiceService.archiveEInvoice(parsedData, inv);
		}
		return "Processo completato";
	}

}
