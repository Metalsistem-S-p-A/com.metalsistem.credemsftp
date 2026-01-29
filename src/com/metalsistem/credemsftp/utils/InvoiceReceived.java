package com.metalsistem.credemsftp.utils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.compiere.model.MAttachmentEntry;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MInvoicePaySchedule;
import org.globalqss.model.MLCOInvoiceWithholding;

public class InvoiceReceived extends MInvoice {

	private static final long serialVersionUID = 8725224945859420772L;

	private List<MInvoiceLine> invoiceLines = new ArrayList<MInvoiceLine>();
	private List<MAttachmentEntry> attachmentEntries = new ArrayList<MAttachmentEntry>();
	private List<MLCOInvoiceWithholding> withHoldings = new ArrayList<MLCOInvoiceWithholding>();
	private List<MInvoicePaySchedule> scheduledPayments = new ArrayList<MInvoicePaySchedule>();
	private String withHoldingsNote = "";
	private BigDecimal grandTotalXML = BigDecimal.ZERO;
	private String errorMsg = "";

	public InvoiceReceived(Properties properties, int i, String copy) {
		super(properties, i, copy);
	}

	public InvoiceReceived(MInvoice mInvoice) {
		// TODO Auto-generated constructor stub
		super(mInvoice);
	}

	public List<MAttachmentEntry> getAttachmentEntries() {
		return attachmentEntries;
	}

	public void setAttachmentEntries(List<MAttachmentEntry> attachments) {
		this.attachmentEntries = attachments;
	}

	public List<MInvoicePaySchedule> getScheduledPayments() {
		return scheduledPayments;
	}

	public void setScheduledPayments(List<MInvoicePaySchedule> scheduledPayments) {
		this.scheduledPayments = scheduledPayments;
	}

	public List<MInvoiceLine> getInvoiceLines() {
		return invoiceLines;
	}

	public void setInvoiceLines(List<MInvoiceLine> invoiceLines) {
		this.invoiceLines = invoiceLines;
	}

	public List<MLCOInvoiceWithholding> getWithHoldings() {
		return withHoldings;
	}

	public void setWithHoldings(List<MLCOInvoiceWithholding> withHoldings) {
		this.withHoldings = withHoldings;
	}

	public String getErrorMsg() {
		return errorMsg;
	}

	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}

	public BigDecimal getGrandTotalXML() {
		return grandTotalXML;
	}

	public void setGrandTotalXML(BigDecimal grandTotalXML) {
		this.grandTotalXML = grandTotalXML;
	}

	public String getWithHoldingsNote() {
		return withHoldingsNote;
	}

	public void setWithHoldingsNote(String withHoldingsNote) {
		this.withHoldingsNote = withHoldingsNote;
	}

}
