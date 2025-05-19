package com.metalsistem.credemsftp.utils;

import java.util.List;

import org.compiere.model.Query;
import org.compiere.model.MProcess;
import org.compiere.model.MInvoice;
import org.compiere.model.MAttachment;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MAttachmentEntry;
import org.compiere.model.MInvoicePaySchedule;

import org.compiere.util.Env;
import org.compiere.util.CLogger;

import org.globalqss.model.MLCOInvoiceWithholding;

import it.cnet.idempiere.LIT_E_Invoice.model.ME_Invoice;

public class InvoiceService {
	private static final CLogger log = CLogger.getCLogger(InvoiceService.class);

	public void archiveEInvoice(byte[] xml, MInvoice inv) {
		ME_Invoice einv = new Query(Env.getCtx(), ME_Invoice.Table_Name, "Name = ?", null).setClient_ID()
				.setParameters("FE: " + inv.getDocumentNo())
				.first();
		if (einv == null) {
			einv = new ME_Invoice(Env.getCtx(), 0, null);
			String noDocFile = inv.getDocumentNo().replaceAll("/", "-");
			einv.setBinaryData(xml);
			einv.setName("FE: " + inv.getDocumentNo());
			einv.setC_DocType_ID(inv.getDocTypeID());
			einv.setC_Invoice_ID(inv.getC_Invoice_ID());
			einv.setDocumentNo(inv.getDocumentNo());
			einv.setFileName("xml-" + noDocFile + ".xml");
			einv.setDateInvoiced(inv.getDateInvoiced());
			einv.set_ValueOfColumn("LIT_MsSyncCredem", false);

			einv.saveEx();

			MAttachment attachment = new MAttachment(Env.getCtx(), 0, null);
			attachment.setRecord_ID(einv.get_ID());
			attachment.setAD_Table_ID(ME_Invoice.Table_ID);
			byte[] pdfBytes = {};
			try {
				PdfUtils utils = new PdfUtils();
				MAttachment procAttachments = new MAttachment(Env.getCtx(), MProcess.Table_ID, einv.get_ID(),
						einv.get_UUID(), null);
				List<MAttachmentEntry> pdfAttachment = List.of(procAttachments.getEntries());
				MAttachmentEntry pdfStyle = pdfAttachment.stream()
						.filter(att -> att.getFile().getName().endsWith(".xsl"))
						.findFirst()
						.orElse(null);

				pdfBytes = utils.create(xml, inv, true, pdfStyle);
				if (pdfBytes != null) {
					MAttachmentEntry entry = new MAttachmentEntry("xml-" + noDocFile + ".xml", xml);
					entry.setName("Fattura-" + noDocFile + ".pdf");
					entry.setData(pdfBytes);

					attachment.addEntry(entry);
					attachment.save(null);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public InvoiceReceived saveInvoice(InvoiceReceived inv) throws Exception {
		MInvoice res = new Query(Env.getCtx(), MInvoice.Table_Name,
				"DocumentNo = ? and C_BPartner_ID = ? and DateInvoiced = ?", null).setClient_ID()
				.setParameters(inv.getDocumentNo(), inv.getC_BPartner_ID(), inv.getDateInvoiced())
				.first();
		if (res == null) {
			inv.saveEx();
			log.info("Fattura importata");

			for (MInvoiceLine l : inv.getInvoiceLines()) {
				l.setC_Invoice_ID(inv.get_ID());
				l.saveEx();
			}
			log.info("Linee Fattura importate");

			MAttachment attachment = new MAttachment(Env.getCtx(), 0, null);
			attachment.setRecord_ID(inv.get_ID());
			attachment.setAD_Table_ID(MInvoice.Table_ID);
			for (MAttachmentEntry a : inv.getAttachmentEntries()) {
				attachment.addEntry(a);
				attachment.saveEx(null);
			}
			log.info("Allegati Fattura importati");

			for (MInvoicePaySchedule s : inv.getScheduledPayments()) {
				s.setC_Invoice_ID(inv.get_ID());
				s.saveEx();
			}
			log.info("Scadenze Fattura importate");

			for (MLCOInvoiceWithholding wh : inv.getWithHoldings()) {
				wh.setC_Invoice_ID(inv.get_ID());
				wh.saveEx();
			}
		} else {
			inv = new InvoiceReceived(res);
			log.warning("Fattura %s già importata".formatted(res.getDocumentNo()));
		}
		return inv;
	}
}
