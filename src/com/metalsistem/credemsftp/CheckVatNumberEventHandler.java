package com.metalsistem.credemsftp;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.adempiere.base.event.AbstractEventHandler;
import org.adempiere.base.event.IEventManager;
import org.adempiere.base.event.IEventTopics;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MInvoice;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MSequence;
import org.compiere.model.MSysConfig;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.event.Event;

import com.metalsistem.credemsftp.utils.Utils;

import it.cnet.idempiere.VATJournalModel.I_C_Invoice_lit;
import it.cnet.idempiere.VATJournalModel.MLITVATDocTypeSequence;

@Component(property = {
		"service.ranking:Integer=100" }, reference = @Reference(name = "IEventManager", bind = "bindEventManager", unbind = "unbindEventManager", policy = ReferencePolicy.STATIC, cardinality = ReferenceCardinality.MANDATORY, service = IEventManager.class))
public class CheckVatNumberEventHandler extends AbstractEventHandler {

	@Override
	protected void doHandleEvent(Event event) throws AdempiereException {
		if (!MSysConfig.getBooleanValue("LIT_MsCheckVatNumber", true, Env.getAD_Client_ID(Env.getCtx()),
				Env.getAD_Org_ID(Env.getCtx()))) {
			return;
		}
		if (IEventTopics.DOC_AFTER_COMPLETE.equals(event.getTopic())) {
			MInvoice inv = (MInvoice) getPO(event);
			int fromId = 0;
			int toId = 0;
			MOrgInfo oi = MOrgInfo.get(inv.getCtx(), inv.getAD_Org_ID(), inv.get_TrxName());
			if (inv.isSOTrx()) {
				fromId = oi.getC_Location_ID();
				toId = inv.getC_BPartner_Location().getC_Location_ID();
			} else {
				fromId = inv.getC_BPartner_Location().getC_Location_ID();
				toId = oi.getC_Location_ID();
			}

			MLITVATDocTypeSequence litVAT = null;
			if (inv.get_ValueAsInt(I_C_Invoice_lit.COLUMNNAME_LIT_VATJournal_ID) > 0) {
				litVAT = new Query(Env.getCtx(), MLITVATDocTypeSequence.Table_Name,
						I_C_Invoice_lit.COLUMNNAME_LIT_VATJournal_ID + "=?", null).setClient_ID()
						.setParameters(inv.get_ValueAsInt(I_C_Invoice_lit.COLUMNNAME_LIT_VATJournal_ID))
						.setOnlyActiveRecords(true)
						.first();
			} else
				litVAT = MLITVATDocTypeSequence.getVATDocTypeSequence(inv, fromId, toId);

			MSequence seq = null;

			if (litVAT != null)
				seq = (MSequence) litVAT.getAD_Sequence();
			if (seq != null) {
				Timestamp dataPo = inv.getDateAcct();
				String numeroPo = inv.get_ValueAsString("vatdocumentno");
				if (numeroPo.isBlank())
					numeroPo = MSequence.getDocumentNoFromSeq(seq, inv.get_TrxName(), inv);// MSequence__patch:

				String sql = "select VATDocumentNo, DateAcct from c_invoice "
						+ "where lit_vatjournal_id = ? and ad_org_id = ? and vatdocumentno is not null order by DateAcct desc, vatdocumentno desc  "
						+ "fetch first row only ";
				try (PreparedStatement stmt = DB.prepareStatement(sql, inv.get_TrxName())) {
					stmt.setInt(1, litVAT.getLIT_VATJournal_ID());
					stmt.setInt(2, inv.getAD_Org_ID());
					ResultSet rs = stmt.executeQuery();

					while (rs.next()) {
						String numero = rs.getString("VATDocumentNo");
						Timestamp date = rs.getTimestamp(2);
						String msg = Utils.getMessage("LIT_MsCheckVatNumber", date, numeroPo);
						if (!dataPo.after(date) && numeroPo.compareTo(numero) > 0) {
							rs.close();
							stmt.close();
							throw new AdempiereException(msg);
						}
					}
				} catch (SQLException e) {
					throw new AdempiereException(e.getMessage());
				}
			}
		}
	}

	@Override
	protected void initialize() {
		// TODO Auto-generated method stub
		registerTableEvent(IEventTopics.DOC_AFTER_COMPLETE, MInvoice.Table_Name);
	}

}
