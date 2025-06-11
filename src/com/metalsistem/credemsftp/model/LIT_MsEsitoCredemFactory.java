package com.metalsistem.credemsftp.model;

import java.sql.ResultSet;
import org.adempiere.base.AnnotationBasedModelFactory;
import org.adempiere.base.IModelFactory;
import org.compiere.model.PO;
import org.compiere.util.Env;
import org.osgi.service.component.annotations.Component;

@Component(immediate = true, service = IModelFactory.class, property = "service.ranking:Integer=100")
public class LIT_MsEsitoCredemFactory extends AnnotationBasedModelFactory {
	@Override
	protected String[] getPackages() {
		return new String[] { M_EsitoCredem.class.getPackageName(), M_PendingInvoices.class.getPackageName() };
	}

	@Override
	public Class<?> getClass(String tableName) {

		switch (tableName) {
		case M_EsitoCredem.Table_Name:
			return M_EsitoCredem.class;
		case M_PendingInvoices.Table_Name:
			return M_PendingInvoices.class;
		}
		return null;
	}

	@Override
	public PO getPO(String tableName, int Record_ID, String trxName) {

		switch (tableName) {
		case M_EsitoCredem.Table_Name:
			return new M_EsitoCredem(Env.getCtx(), Record_ID, trxName);
		case M_PendingInvoices.Table_Name:
			return new M_PendingInvoices(Env.getCtx(), Record_ID, trxName);
		}
		return null;
	}

	@Override
	public PO getPO(String tableName, ResultSet rs, String trxName) {

		switch (tableName) {
		case M_EsitoCredem.Table_Name:
			return new M_EsitoCredem(Env.getCtx(), rs, trxName);
		case M_PendingInvoices.Table_Name:
			return new M_PendingInvoices(Env.getCtx(), rs, trxName);
		}
		return null;
	}
}
