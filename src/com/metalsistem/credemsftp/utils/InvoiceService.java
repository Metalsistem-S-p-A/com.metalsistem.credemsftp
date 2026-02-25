package com.metalsistem.credemsftp.utils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.adempiere.model.MBroadcastMessage;
import org.compiere.model.MAttachment;
import org.compiere.model.MAttachmentEntry;
import org.compiere.model.MBPartner;
import org.compiere.model.MClient;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MInvoicePaySchedule;
import org.compiere.model.MProcess;
import org.compiere.model.MRole;
import org.compiere.model.MSysConfig;
import org.compiere.model.MWindow;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.EMail;
import org.compiere.util.Env;
import org.idempiere.broadcast.BroadcastMsgUtil;

import com.metalsistem.credemsftp.model.M_PendingInvoices;

import it.cnet.idempiere.LIT_E_Invoice.model.ME_Invoice;

public class InvoiceService {
	private static final String PENDING_INVOICE_MSG = "LIT_MsNewPendingInvoice";
	private static String WHERE_ORG;
//	private static String WHERE_ORG_ALL;
	private static final CLogger log = CLogger.getCLogger(InvoiceService.class);

	public InvoiceService() {
		int orgId = Env.getAD_Org_ID(Env.getCtx());
		WHERE_ORG = " AND AD_Org_ID = " + orgId + " ";
//		WHERE_ORG_ALL = " AND AD_Org_ID IN(0, " + orgId + ",) ";
	}

	public void archiveEInvoice(byte[] xml, InvoiceReceived inv) {
		String noDocFile = inv.getDocumentNo().replaceAll("/", "-");
		String nomeEinv = noDocFile + "_" + inv.getC_BPartner_ID();
		String nomeRecord = noDocFile + "_" + inv.getC_BPartner().getName();
		String nomeDoc = "FE: " + nomeRecord;
		ME_Invoice einv = new Query(Env.getCtx(), ME_Invoice.Table_Name, "Name = ?" + WHERE_ORG, null).setClient_ID()
				.setParameters(nomeDoc).first();
		if (einv == null) {
			einv = new ME_Invoice(Env.getCtx(), 0, null);
			einv.setBinaryData(xml);
			einv.setName(nomeDoc);
			einv.setC_DocType_ID(inv.getDocTypeID());
			einv.setC_Invoice_ID(inv.getC_Invoice_ID());
			einv.setDocumentNo(inv.getDocumentNo());
			einv.setFileName("xml-" + nomeEinv + ".xml");
			einv.setDateInvoiced(inv.getDateInvoiced());
			einv.set_ValueOfColumn("LIT_MsSyncCredem", false);
			einv.set_ValueOfColumn(InvoiceParser.TIPO_DOC_FEPA, inv.getTipoDocumento());
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
						.filter(att -> att.getFile().getName().endsWith(".xsl")).findFirst().orElse(null);

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
		/*
		 * NOTA: Controllo già eseguito in fase di parsing, a questo punto del codice la
		 * fattura dovrebbe essere sempre nuova. Ma non si sa mai...
		 */
		MInvoice res = new Query(Env.getCtx(), MInvoice.Table_Name,
				"DocumentNo = ? and C_BPartner_ID = ? and DateInvoiced = ?" + WHERE_ORG, null).setClient_ID()
				.setParameters(inv.getDocumentNo(), inv.getC_BPartner_ID(), inv.getDateInvoiced()).first();
		if (res == null) {
			StringBuilder nota = new StringBuilder();

			nota.append(inv.getWithHoldingsNote());
			MBPartner bp = new MBPartner(Env.getCtx(), inv.getC_BPartner_ID(), null);
			try {
				inv.saveEx();
				if (InvoiceParser.getIsNewBP()) {
					publishNewBpMessage(bp);
				}
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
			// checkTotal(inv);
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

		InvoiceParser.setIsNewBP(false);
		return inv;
	}

	public void publishNewPendingInvoiceMessage(M_PendingInvoices inv) {
		publishBroadcastMessage(inv, M_PendingInvoices.Table_ID, PENDING_INVOICE_MSG, inv.getName(),
				getAdminClientRole());
	}

	public void publishNewBpMessage(MBPartner mbp) {
		Boolean canSend = MSysConfig.getBooleanValue("LIT_MsCredSendNotification", false);
		if (canSend)
			publishBroadcastMessage(mbp, MBPartner.Table_ID, "LIT_MsInfoBPCreated", mbp.getName(), getAdminRole());
	}

	private void publishBroadcastMessage(PO model, int tableId, String messageKey, String recordName, MRole role) {
		MBroadcastMessage msg = new MBroadcastMessage(Env.getCtx(), 0, null);

		int winUUID = Env.getZoomWindowID(tableId, model.get_ID());
		MWindow window = MWindow.get(winUUID);

		String msgBody = Utils.getMessage(messageKey, msg.getUrlZoom(model, window.get_UUID(), recordName));
		msg.setBroadcastMessage(msgBody);
		msg.setBroadcastType(MBroadcastMessage.BROADCASTTYPE_ImmediatePlusLogin);
		msg.setBroadcastFrequency(MBroadcastMessage.BROADCASTFREQUENCY_UntilExpirationOrAcknowledge);
		msg.setTarget(MBroadcastMessage.TARGET_Role);
		msg.setAD_Role_ID(role.get_ID());
		msg.setPublish("Y");
		msg.setExpiration(Timestamp.valueOf(LocalDateTime.now().plusMonths(1)));
		msg.saveEx();

		if (PENDING_INVOICE_MSG.equals(messageKey)) {
			M_PendingInvoices inv = (M_PendingInvoices) model;
			String mailMsg = "Errore nella fase di import della ".concat(inv.getName()).concat("\n\n")
					.concat(inv.getDescription());
			new EMail(MClient.get(Env.getAD_Client_ID(Env.getCtx())), "credemsftp@metalsistem.com",
					"notificheidempiere@metalsistem.com", "Credemsftp fattura non importata", mailMsg).send();
		}

		BroadcastMsgUtil.publishBroadcastMessage(msg.get_ID(), null);
	}

	private MRole getAdminRole() {
		MRole role = new Query(Env.getCtx(), MRole.Table_Name, "name = 'Amministrazione'", null).setClient_ID().first();
		if (role == null) {
			role = new Query(Env.getCtx(), MRole.Table_Name, "name like 'Amministratore%'", null).setClient_ID()
					.first();
		}
		return role;
	}

	private MRole getAdminClientRole() {
		MRole role = new Query(Env.getCtx(), MRole.Table_Name, "name ilike 'Amministratore client'", null)
				.setClient_ID().first();
		return role;
	}

}
