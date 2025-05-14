package com.metalsistem.credemsftp.utils;

import java.util.ArrayList;
import java.util.List;

import org.compiere.model.MAttachmentEntry;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MInvoicePaySchedule;
import org.globalqss.model.MLCOInvoiceWithholding;

public class InvoiceReceived extends MInvoice {

	private static final long serialVersionUID = 8725224945859420772L;

	private List<MAttachmentEntry> attachmentEntries = new ArrayList<MAttachmentEntry>();
	private List<MInvoicePaySchedule> scheduledPayments = new ArrayList<MInvoicePaySchedule>();
	private List<MInvoiceLine> invoiceLines = new ArrayList<MInvoiceLine>();
	private List<MLCOInvoiceWithholding> withHoldings= new ArrayList<MLCOInvoiceWithholding>();

	public InvoiceReceived(MInvoice copy) {
		super(copy);
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

}
