package com.metalsistem.credemsftp.utils;

import org.compiere.model.MCountry;
import org.compiere.model.MCountryGroupCountry;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.Env;

//@Data
//@EqualsAndHashCode(callSuper = true)
public class RegistroIva {

	private static final Integer CEE_ID = 1000000;
	private static final Integer ITA_ID = 214;
	private int id;

	public RegistroIva() {
	}

	public RegistroIva(Boolean cliente, MCountry from, MCountry to) {
		if (from == null || to == null) {
			setId(cliente ? 1000005 : 1000004);
			return;
		}
		MCountryGroupCountry cgc = new Query(Env.getCtx(), MCountryGroupCountry.Table_Name, "C_Country_ID=?", null)
				.setClient_ID()
				.setParameters(to.get_ID())
				.first();
		String searchKey = "";
		// TODO: imposta ID mancanti
		if (cliente) {
			// Vendite
			searchKey = "V1"; // DEFAULT
			if (to.get_ID() != ITA_ID && cgc.get_ID() == CEE_ID) {
				searchKey = "UE_V";
			} else if (to.get_ID() == (ITA_ID)) {
				searchKey = "V1";
			} else {
				searchKey = "VA";
			}
		} else {
			// Acquisti
			searchKey = "A1"; // DEFAULT
			if (to.get_ID() != ITA_ID && cgc.get_ID() == CEE_ID) {
				searchKey = "UE_A";
			} else if (to.get_ID() == (ITA_ID)) {
				searchKey = "A1";
			} else {
				searchKey = "ExUE_A";
			}
		}
		int id = DB.getSQLValue(null, "select lit_vatjournal_id from lit_vatjournal where lit_vatjournal.value = ?", searchKey);
		setId(id);
		
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}
}
/**
 * 
 * 1000000 "Registro UE Acquisti" 1000001 "Registro UE Vendite" 1000002
 * "Registro ExtraUE Acquisti (No Fiscale)" 1000004 "Registro IVA Acquisti ITA"
 * 1000005 "Registro IVA Vendite ITA" 1000003 "Registro ExtraUE Vendite" 1000008
 * "Registro IVA Acq ITA R.C. 17" 1000006 "Registro IVA Vendite Corrispettivi
 * ITA"
 */
