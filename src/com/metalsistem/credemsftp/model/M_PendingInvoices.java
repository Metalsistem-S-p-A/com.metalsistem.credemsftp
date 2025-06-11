package com.metalsistem.credemsftp.model;

import java.sql.ResultSet;
import java.util.Properties;


public class M_PendingInvoices extends X_LIT_MsPendingInvoices {

	private static final long serialVersionUID = 6276636574778189009L;

	public M_PendingInvoices(Properties ctx, int LIT_MsPendingInvoices_ID, String trxName) {
		super(ctx, LIT_MsPendingInvoices_ID, trxName);
		// TODO Auto-generated constructor stub
	}

	public M_PendingInvoices(Properties ctx, ResultSet rs, String trxName) {
		// TODO Auto-generated constructor stub
		super(ctx, rs, trxName);
	}
	
}
