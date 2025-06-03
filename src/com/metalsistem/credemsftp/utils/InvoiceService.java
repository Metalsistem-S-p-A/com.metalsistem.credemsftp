package com.metalsistem.credemsftp.utils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.adempiere.model.MBroadcastMessage;
import org.compiere.model.MAttachment;
import org.compiere.model.MAttachmentEntry;
import org.compiere.model.MBPartner;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MInvoicePaySchedule;
import org.compiere.model.MProcess;
import org.compiere.model.MRole;
import org.compiere.model.MWindow;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.globalqss.model.MLCOInvoiceWithholding;
import org.idempiere.broadcast.BroadcastMsgUtil;

import it.cnet.idempiere.LIT_E_Invoice.model.ME_Invoice;

public class InvoiceService {
	private static final CLogger log = CLogger.getCLogger(InvoiceService.class);

	public void archiveEInvoice(byte[] xml, MInvoice inv) {
		String noDocFile = inv.getDocumentNo().replaceAll("/", "-");
		String nomeEinv = noDocFile + "_" + inv.getC_BPartner_ID();
		String nomeRecord = noDocFile + "_" + inv.getC_BPartner().getName();
		ME_Invoice einv = new Query(Env.getCtx(), ME_Invoice.Table_Name, "Name = ?", null).setClient_ID()
				.setParameters("FE: " + nomeRecord)
				.first();
		if (einv == null) {
			einv = new ME_Invoice(Env.getCtx(), 0, null);
			einv.setBinaryData(xml);
			einv.setName("FE: " + nomeRecord);
			einv.setC_DocType_ID(inv.getDocTypeID());
			einv.setC_Invoice_ID(inv.getC_Invoice_ID());
			einv.setDocumentNo(inv.getDocumentNo());
			einv.setFileName("xml-" + nomeEinv + ".xml");
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
					entry.setName("Fattura-" + nomeEinv + ".pdf");
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
			StringBuilder nota = new StringBuilder();
			StringBuilder notaRitenute = new StringBuilder();
			for (MLCOInvoiceWithholding wh : inv.getWithHoldings()) {
				notaRitenute.append("- Ritenuta ")
						.append(wh.getC_Tax().getRate())
						.append("%: ")
						.append(wh.getTaxAmt())
						.append(" EUR \n");

			}
			nota.append(notaRitenute.toString());
			MBPartner bp = new MBPartner(Env.getCtx(), inv.getC_BPartner_ID(), null);
			try {
				inv.saveEx();
				publishNewBpMessage(bp);
			} catch (Exception e) {
				if (InvoiceParser.getIsNewBP()) {
					bp.deleteEx(false);
				}
			}
			log.info("Fattura importata");

//			StringBuilder notaDescrizione = new StringBuilder();
//			boolean newLine = true;
			for (MInvoiceLine l : inv.getInvoiceLines()) {
				l.setC_Invoice_ID(inv.get_ID());
				l.saveEx();
//				if (l.getPriceEntered().compareTo(BigDecimal.ZERO) == 0) {
//					notaDescrizione.append(l.getDescription()).append(" ");
//					newLine = true;
//				} else if (newLine) {
//					notaDescrizione.append("\n\n");
//					newLine = false;
//				}
			}
//			nota.append(notaDescrizione.toString());
			log.info("Linee Fattura importate");

			inv.set_ValueOfColumn("Note", nota.toString());
			inv.saveEx();
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

		} else {
			inv = new InvoiceReceived(res);
			log.warning("Fattura %s già importata".formatted(res.getDocumentNo()));
		}
		return inv;
	}

	private void publishNewBpMessage(MBPartner mbp) {
		MBroadcastMessage msg = new MBroadcastMessage(Env.getCtx(), 0, null);
		MRole role = new Query(Env.getCtx(), MRole.Table_Name, "name = 'Amministrazione'", null).setClient_ID().first();
		if (role == null) {
			role = new Query(Env.getCtx(), MRole.Table_Name, "name like 'Amministratore%'", null).setClient_ID()
					.first();
		}
		int winUUID = Env.getZoomWindowID(MBPartner.Table_ID, mbp.get_ID());
		MWindow bpWindow = MWindow.get(winUUID);
		msg.setBroadcastMessage(
				Utils.getMessage("LIT_MsInfoBPCreated", msg.getUrlZoom(mbp, bpWindow.get_UUID(), mbp.getName())));
		msg.setBroadcastType(MBroadcastMessage.BROADCASTTYPE_ImmediatePlusLogin);
		msg.setBroadcastFrequency(MBroadcastMessage.BROADCASTFREQUENCY_UntilExpirationOrAcknowledge);
		msg.setTarget(MBroadcastMessage.TARGET_Role);
		msg.setAD_Role_ID(role.get_ID());
		msg.setPublish("Y");
		msg.setExpiration(Timestamp.valueOf(LocalDateTime.now().plusMonths(1)));
		msg.saveEx();
		BroadcastMsgUtil.publishBroadcastMessage(msg.get_ID(), null);
	}
}
