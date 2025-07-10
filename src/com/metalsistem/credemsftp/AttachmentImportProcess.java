package com.metalsistem.credemsftp;

import java.util.List;

import org.compiere.model.MAttachmentEntry;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;

import com.metalsistem.credemsftp.model.M_PendingInvoices;
import com.metalsistem.credemsftp.utils.InvoiceParser;
import com.metalsistem.credemsftp.utils.InvoiceReceived;
import com.metalsistem.credemsftp.utils.InvoiceService;

public class AttachmentImportProcess extends SvrProcess {

	Integer recordId;

	private final InvoiceParser invoiceParser = new InvoiceParser();
	private final InvoiceService invoiceService = new InvoiceService();

	@Override
	protected void prepare() {
		ProcessInfoParameter[] params = getParameter();

		for (ProcessInfoParameter param : params) {
			String name = param.getParameterName();
			if ("RecordID".equals(name)) {
				recordId = param.getParameterAsInt();
			}
		}

	}

	@Override
	protected String doIt() throws Exception {
		M_PendingInvoices pinv = new M_PendingInvoices(getCtx(), recordId, null);
		List<MAttachmentEntry> entries = List.of(pinv.getAttachment().getEntries());
		if (entries.isEmpty())
			return "No Attachments";
		byte[] data = entries.get(0).getData();
		byte[] parsedData = invoiceParser.getXml(data);
		InvoiceReceived inv = invoiceParser.getInvoiceFromXml(parsedData);
		if (inv.getErrorMsg().isBlank()) {
			inv = invoiceService.saveInvoice(inv);
			invoiceService.archiveEInvoice(parsedData, inv);
		} else {
			return inv.getErrorMsg();
		}
		pinv.delete(false);
		return "Processo completato" ;

	}

}
