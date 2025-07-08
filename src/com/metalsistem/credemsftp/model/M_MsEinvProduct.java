package com.metalsistem.credemsftp.model;

import java.sql.ResultSet;
import java.util.Properties;

public class M_MsEinvProduct extends X_LIT_MsEinvProduct {

	public M_MsEinvProduct(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
		// TODO Auto-generated constructor stub
	}

	public M_MsEinvProduct(Properties ctx, int einv_product_id, String trxName) {
		super(ctx, einv_product_id, trxName);
		// TODO Auto-generated constructor stub
	}

	private static final long serialVersionUID = 125455284907665252L;

}
