package com.metalsistem.credemsftp.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.bouncycastle.cms.CMSProcessable;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.util.encoders.Hex;
import org.compiere.model.MAttachmentEntry;
import org.compiere.model.MBPBankAccount;
import org.compiere.model.MBPartner;
import org.compiere.model.MBPartnerLocation;
import org.compiere.model.MBank;
import org.compiere.model.MCountry;
import org.compiere.model.MCurrency;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MInvoicePaySchedule;
import org.compiere.model.MLocation;
import org.compiere.model.MPaymentTerm;
import org.compiere.model.MProduct;
import org.compiere.model.MRefList;
import org.compiere.model.MRegion;
import org.compiere.model.MTax;
import org.compiere.model.MUOM;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.globalqss.model.MLCOInvoiceWithholding;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.itextpdf.io.source.ByteArrayOutputStream;
import com.metalsistem.credemsftp.model.M_EsitoCredem;
import com.metalsistem.credemsftp.model.M_MsEinvProduct;

import it.cnet.idempiere.LIT_E_Invoice.modelXML2.AllegatiType;
import it.cnet.idempiere.LIT_E_Invoice.modelXML2.DatiAnagraficiCedenteType;
import it.cnet.idempiere.LIT_E_Invoice.modelXML2.DatiCassaPrevidenzialeType;
import it.cnet.idempiere.LIT_E_Invoice.modelXML2.DatiGeneraliDocumentoType;
import it.cnet.idempiere.LIT_E_Invoice.modelXML2.DatiPagamentoType;
import it.cnet.idempiere.LIT_E_Invoice.modelXML2.DatiRiepilogoType;
import it.cnet.idempiere.LIT_E_Invoice.modelXML2.DatiRitenutaType;
import it.cnet.idempiere.LIT_E_Invoice.modelXML2.DettaglioLineeType;
import it.cnet.idempiere.LIT_E_Invoice.modelXML2.DettaglioPagamentoType;
import it.cnet.idempiere.LIT_E_Invoice.modelXML2.FatturaElettronicaBodyType;
import it.cnet.idempiere.LIT_E_Invoice.modelXML2.FatturaElettronicaType;
import it.cnet.idempiere.LIT_E_Invoice.modelXML2.IndirizzoType;
import it.cnet.idempiere.LIT_E_Invoice.modelXML2.ModalitaPagamentoType;
import it.cnet.idempiere.LIT_E_Invoice.modelXML2.NaturaType;
import it.cnet.idempiere.LIT_E_Invoice.modelXML2.ScontoMaggiorazioneType;
import it.cnet.idempiere.LIT_E_Invoice.modelXML2.TipoDocumentoType;
import it.cnet.idempiere.LIT_E_Invoice.utilXML.ManageXML_new;
import it.cnet.idempiere.VATJournalModel.MLITVATDocTypeSequence;
import it.cnet.idempiere.lettIntent.model.MBPLetterIntent;
import net.schmizz.sshj.sftp.RemoteFile;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;

public class InvoiceParser {
	private MBPartner bp = null;

	private static boolean isNewBP = false;
	private static String WHERE_ORG;
	private static String WHERE_ORG_ALL;
	private static String WHERE_ALL;

	private final List<MTax> taxes;
	private final List<MUOM> uoms;
	private final Integer orgId;

	private final String TIPO_RIGA_DETTAGLIO_LINEE = "DettaglioLinee";
	private final String TIPO_RIGA_DATI_CASSA = "DatiCassaPrevidenziale";
	private final String TIPO_RIGA_ARROTONDAMENTO = "Arrotondamento";

	private static final String LIT_IS_DEFAULT_TAX = "LIT_IsDefaultTax";
	private static final String LIT_XML_PRODUCT = "LIT_M_Product_XML_ID";
	private static final String LIT_CHECK_PAYMENT_TERM = "LIT_isNoCheckPaymentTerm";
	private static final String LIT_XML_INVOICE_TAX_TYPE = "LIT_XMLInvoice_TaxType";

	private static final String IS_DISPLAYED = "IsDisplayed";
	private static final String DEFAULT_PAYMENT_RULE = "MP05";
	private static final String NATURA_LETTERA_INTENTO = "N3.5";

	private static final CLogger log = CLogger.getCLogger(InvoiceParser.class);
	private static final List<TipoDocumentoType> BANNED_DOCUMENT_TYPES = List.of(TipoDocumentoType.TD_04,
			TipoDocumentoType.TD_16, TipoDocumentoType.TD_17, TipoDocumentoType.TD_18);

	public InvoiceParser() {
		orgId = Env.getAD_Org_ID(Env.getCtx());
		WHERE_ORG = " AND AD_Org_ID = %d ".formatted(orgId);
		WHERE_ORG_ALL = " AND AD_Org_ID IN(0, " + orgId + ") ";
		WHERE_ALL = " AND AD_Org_ID = 0 ";
		taxes = loadApplicableTaxes();
		uoms = new Query(Env.getCtx(), MUOM.Table_Name, "IsActive = 'Y'", null).setClient_ID().list();
	}

	public byte[] parseByteXML(byte[] xml) {
		try {
			byte[] parsed;
			CMSSignedData signature = new CMSSignedData(xml);
			CMSProcessable sc = signature.getSignedContent();
			parsed = (byte[]) sc.getContent();
			return removeBOMIfPresent(parsed);
		} catch (Exception e) {
			log.warning("Fattura non p7m");
		}
		return removeBOMIfPresent(xml);
	}

	private InvoiceReceived getInvoice(FatturaElettronicaType fattura) throws Exception {
		// TODO: Gestire caso di molteplici body(?)
		InvoiceReceived invoice = new InvoiceReceived(new MInvoice(Env.getCtx(), 0, null));
		FatturaElettronicaBodyType body = fattura.getFatturaElettronicaBody().get(0);
		if (isBannedDocument(body)) {
			invoice.setErrorMsg("Fattura non importata: tipo documento non valido");
			return invoice;
		}

		DatiGeneraliDocumentoType datiGeneraliDocumento = body.getDatiGenerali().getDatiGeneraliDocumento();
		String codice = fattura.getFatturaElettronicaHeader().getCedentePrestatore().getDatiAnagrafici()
				.getIdFiscaleIVA().getIdCodice();

		MBPartner mbp = new Query(Env.getCtx(), MBPartner.Table_Name,
				"? in (LIT_taxID, LIT_NationalIDNumber_ID) " + WHERE_ORG_ALL, null).setClient_ID().setParameters(codice)
				.first();

		if (mbp != null) {
			MInvoice res = new Query(Env.getCtx(), MInvoice.Table_Name,
					"DocumentNo = ? and C_BPartner_ID = ? and DateInvoiced = ?" + WHERE_ORG, null).setClient_ID()
					.setParameters(datiGeneraliDocumento.getNumero(), mbp.get_ID(),
							toTimestamp(datiGeneraliDocumento.getData()))
					.first();
			if (res != null) {
				// Fattura già importata
				invoice.setErrorMsg("Fattura già presente nel sistema");
				return invoice;
			}
		} else {
			// BUSINESS PARTNER
			mbp = findOrCreateBPartner(fattura);
		}

		invoice.setIsSOTrx(false);
		invoice.setAD_Org_ID(orgId);
		invoice.setDocumentNo(datiGeneraliDocumento.getNumero());
		invoice.setDateInvoiced(toTimestamp(datiGeneraliDocumento.getData()));
		invoice.setGrandTotal(datiGeneraliDocumento.getImportoTotaleDocumento());
		invoice.setC_Currency_ID(MCurrency.get(datiGeneraliDocumento.getDivisa()).get_ID());

		// TIPO DOCUMENTO
		MDocType docType = findDocType(datiGeneraliDocumento);

		invoice.setC_DocType_ID(docType.get_ID());
		invoice.setC_DocTypeTarget_ID(docType.get_ID());
		invoice.set_ValueOfColumn("LIT_FEPA_DOCTYPE", datiGeneraliDocumento.getTipoDocumento().value());

		// TERMINI E MODALITA' PAGAMENTO TESTATA
		invoice.setC_PaymentTerm_ID(mbp.getPO_PaymentTerm_ID());
		invoice.setPaymentRule(mbp.getPaymentRulePO());
		invoice.setBPartner(mbp);
		invoice.setC_BPartner_ID(mbp.get_ID());

		// LOCATION
		MBPartnerLocation mbpLocation = mbp.getPrimaryC_BPartner_Location();
		invoice.setC_BPartner_Location_ID(mbp.getPrimaryC_BPartner_Location_ID());

		MCountry to = new MCountry(Env.getCtx(), 214, null);
		MCountry from = mbpLocation.getLocation(true).getCountry();

		// REGISTRO IVA
		final MLITVATDocTypeSequence registroIva;
		if (to != null && from != null) {
			registroIva = MLITVATDocTypeSequence.getVATDocTypeSequenceWithCountry(invoice, from.get_ID(), to.get_ID());
			// Registro Iva impostato automaticamente in fase di completamento
		} else {
			registroIva = null;
		}

		// LETTERA D'INTENTO
		if (body.getDatiBeniServizi().getDatiRiepilogo().get(0).getNatura() != null && NATURA_LETTERA_INTENTO
				.equals(body.getDatiBeniServizi().getDatiRiepilogo().get(0).getNatura().value())) {

			MBPLetterIntent letter = new Query(Env.getCtx(), MBPLetterIntent.Table_Name,
					" isSOTrx = 'N' " + "AND bp_letterintentdatevalidfrom < Current_date "
							+ "AND bp_letterintentdatevalidto > current_date " + "AND c_bpartner_id = ?" + WHERE_ORG,
					null).setClient_ID().setParameters(mbp.get_ID()).first();
			if (letter != null)
				invoice.set_ValueOfColumn("c_bp_partner_letterintent_id", letter.get_ID());
		}

		// LINEE
		List<MInvoiceLine> linee = new ArrayList<MInvoiceLine>();
		BigDecimal imponibile = BigDecimal.ZERO;
		for (DettaglioLineeType linea : body.getDatiBeniServizi().getDettaglioLinee()) {
			MInvoiceLine il = new MInvoiceLine(Env.getCtx(), 0, null);
			il.setAD_Org_ID(orgId);
			il.setInvoice(invoice);
			il.setName(linea.getDescrizione());
			il.setPriceList(linea.getPrezzoUnitario());
			BigDecimal price = linea.getPrezzoUnitario();
			for (ScontoMaggiorazioneType sconto : linea.getScontoMaggiorazione()) {
				BigDecimal importo = sconto.getImporto();
				BigDecimal percentuale = sconto.getPercentuale();
				switch (sconto.getTipo()) {
				case MG -> {
					if (percentuale != null)
						price = linea.getPrezzoUnitario().add(linea.getPrezzoUnitario().multiply(percentuale)
								.divide(BigDecimal.valueOf(100), RoundingMode.HALF_DOWN));
					if (importo != null)
						price = linea.getPrezzoUnitario().add(importo);
				}
				case SC -> {
					if (percentuale != null)
						price = linea.getPrezzoUnitario().subtract(linea.getPrezzoUnitario().multiply(percentuale)
								.divide(BigDecimal.valueOf(100), RoundingMode.HALF_DOWN));
					if (importo != null)
						price = linea.getPrezzoUnitario().subtract(importo);
				}
				}
			}
			il.setPrice(price);
			il.setLine(linea.getNumeroLinea() * 10); // iDempiere Standard
			il.setDescription(linea.getDescrizione());
			if (linea.getDescrizione().length() > 255)
				il.set_ValueOfColumn("Help", linea.getDescrizione());
			MTax invTax = getTax(registroIva, linea.getNatura(), linea.getAliquotaIVA(), linea.getPrezzoUnitario(),
					mbp);
			il.setC_Tax_ID(invTax.get_ID());

			if (invTax.getRate().compareTo(BigDecimal.ZERO) > 0) {
				imponibile = imponibile.add(price);
			}

			il.setProduct(getProductByTax(invTax, mbp, TIPO_RIGA_DETTAGLIO_LINEE));
//			if (mbp.get_ValueAsInt("LIT_M_Product_XML_ID") > 0) {
//				MProduct prod = new MProduct(Env.getCtx(), mbp.get_ValueAsInt("LIT_M_Product_XML_ID"), null);
//				il.setProduct(prod);
//			}
			if (linea.getPrezzoUnitario().compareTo(BigDecimal.ZERO) == 0) {
				il.setIsDescription(true);
				il.setProduct(null);
			}
			if (!linea.getCodiceArticolo().isEmpty()) {
				il.set_ValueOfColumn("VendorProductNo", linea.getCodiceArticolo().get(0).getCodiceValore());
			}
			if (linea.getQuantita() != null) {
				il.setQtyEntered(linea.getQuantita());
				il.setQtyInvoiced(linea.getQuantita());
			} else if (linea.getQuantita() == null && linea.getPrezzoTotale() != null
					&& linea.getPrezzoUnitario() != null) {
				il.setQtyEntered(BigDecimal.ONE);
				il.setQtyInvoiced(BigDecimal.ONE);
			}
			if (linea.getUnitaMisura() != null) {
				for (MUOM uom : uoms) {
					if (linea.getUnitaMisura().toLowerCase().equals(uom.getUOMSymbol().toLowerCase())
							|| linea.getUnitaMisura().toLowerCase()
									.equals(uom.get_Translation(MUOM.COLUMNNAME_Name, "it_IT").toLowerCase())
							|| linea.getUnitaMisura().toLowerCase()
									.equals(uom.get_Translation(MUOM.COLUMNNAME_UOMSymbol, "it_IT").toLowerCase())) {
						il.setC_UOM_ID(uom.get_ID());
						break;
					}
				}
			}
			linee.add(il);
		}

		// DATI RIEPILOGO
		linee.addAll(parseDatiRiepilogo(invoice, body, mbp, registroIva));

		// CASSA PREVIDENZIALE
		List<DatiCassaPrevidenzialeType> datiCassa = body.getDatiGenerali().getDatiGeneraliDocumento()
				.getDatiCassaPrevidenziale();
		linee.addAll(parseDatiCassaPrevidenziale(invoice, mbp, registroIva, datiCassa));
		invoice.setInvoiceLines(linee);

		// SCADENZE
		List<MBPBankAccount> mbpas = List.of(mbp.getBankAccounts(true));
		List<MInvoicePaySchedule> scadenze = parseDatiPagamento(invoice, body, mbp, mbpas);
		invoice.setScheduledPayments(scadenze);

		// RITENUTE
		List<DatiRitenutaType> datiRitenuta = body.getDatiGenerali().getDatiGeneraliDocumento().getDatiRitenuta();
		List<MLCOInvoiceWithholding> ritenute = parseDatiRitenuta(imponibile, datiRitenuta);
		invoice.setWithHoldings(ritenute);

		BigDecimal totaleDocumento = datiGeneraliDocumento.getImportoTotaleDocumento();
//		for (MLCOInvoiceWithholding rit : ritenute) {
//			totaleDocumento = totaleDocumento.subtract(rit.getTaxAmt());
//		}
		invoice.setGrandTotalXML(totaleDocumento);

		// ALLEGATI
		List<MAttachmentEntry> allegati = new ArrayList<MAttachmentEntry>();
		for (AllegatiType allegato : body.getAllegati()) {
			String nome = allegato.getNomeAttachment();
			byte[] content = allegato.getAttachment();
			allegati.add(new MAttachmentEntry(nome, content));
		}
		invoice.setAttachmentEntries(allegati);

		invoice.setSalesRep_ID(0);
		invoice.setAD_User_ID(0);

		return invoice;
	}

	/**
	 * Parses the "DatiRiepilogo" section of the e-invoice and returns a list of
	 * {@code MInvoiceLine} objects representing rounding adjustments. The tax is
	 * determined by applying business logic based on the provided VAT registry,
	 * nature, and rate.
	 *
	 * Only lines with rounding amounts are considered; all others are skipped.
	 *
	 * @param invoice     the target invoice to associate the parsed lines with
	 * @param body        the body of the electronic invoice containing summary data
	 * @param mbp         the business partner involved in the invoice
	 * @param registroIva the VAT registry used to determine tax rules
	 * @return a list of invoice lines related to rounding adjustments
	 */
	private List<MInvoiceLine> parseDatiRiepilogo(InvoiceReceived invoice, FatturaElettronicaBodyType body,
			MBPartner mbp, final MLITVATDocTypeSequence registroIva) {
		List<MInvoiceLine> linee = new ArrayList<MInvoiceLine>();
		for (DatiRiepilogoType riepilogo : body.getDatiBeniServizi().getDatiRiepilogo()) {
			if (riepilogo.getArrotondamento() == null) {
				continue;
			}
			MInvoiceLine il = new MInvoiceLine(Env.getCtx(), -1, null);
			il.setInvoice(invoice);
			il.setName("Arrotondamento");
			il.setPrice(riepilogo.getArrotondamento());
			il.setDescription("Arrotondamento");
			il.setQtyEntered(BigDecimal.ONE);
			il.setQtyInvoiced(BigDecimal.ONE);

			MTax invTax = getTax(registroIva, riepilogo.getNatura(), riepilogo.getAliquotaIVA(),
					riepilogo.getArrotondamento(), mbp);

			il.setProduct(getProductByTax(invTax, mbp, TIPO_RIGA_ARROTONDAMENTO));
			il.setC_Tax_ID(invTax.get_ID());
			linee.add(il);
		}
		return linee;
	}

	/**
	 * Parses the "DatiCassaPrevidenziale" section of the e-invoice and creates
	 * corresponding invoice lines. Each contribution entry is transformed into an
	 * {@code MInvoiceLine} with predefined quantity, price, and description. The
	 * method calculates the applicable tax using invoice-specific rules based on
	 * the contribution nature, VAT rate, and registry.
	 *
	 * @param invoice     the invoice to which the parsed contribution lines will be
	 *                    attached
	 * @param mbp         the business partner related to the invoice
	 * @param registroIva the VAT registry reference used for determining tax
	 *                    applicability
	 * @param datiCassa   the list of contribution entries from the electronic
	 *                    invoice
	 * @return a list of invoice lines representing social security contributions
	 */
	private List<MInvoiceLine> parseDatiCassaPrevidenziale(InvoiceReceived invoice, MBPartner mbp,
			final MLITVATDocTypeSequence registroIva, List<DatiCassaPrevidenzialeType> datiCassa) {
		List<MInvoiceLine> linee = new ArrayList<MInvoiceLine>();
		for (DatiCassaPrevidenzialeType dato : datiCassa) {
			MInvoiceLine il = new MInvoiceLine(Env.getCtx(), -1, null);
			il.setInvoice(invoice);
			il.setQtyEntered(BigDecimal.ONE);
			il.setQtyInvoiced(BigDecimal.ONE);
			il.setPrice(dato.getImportoContributoCassa());
			il.setName("Contributo previdenziale " + dato.getTipoCassa().value() + " " + dato.getAlCassa());
			il.setDescription("Contributo previdenziale " + dato.getTipoCassa().value() + " " + dato.getAlCassa());
			MTax invTax = getTax(registroIva, dato.getNatura(), dato.getAliquotaIVA(), dato.getImportoContributoCassa(),
					mbp);

			il.setProduct(getProductByTax(invTax, mbp, TIPO_RIGA_DATI_CASSA));

			il.setC_Tax_ID(invTax.get_ID());
			linee.add(il);
		}
		return linee;
	}

	/**
	 * Parses the payment details from an e-invoice body and generates a list of
	 * {@code MInvoicePaySchedule} objects, each representing a scheduled payment
	 * (scadenza).
	 *
	 * <p>
	 * This method:
	 * <ul>
	 * <li>Iterates over the payment sections in the invoice</li>
	 * <li>Creates or retrieves a {@code MBPBankAccount} for the payment</li>
	 * <li>Creates a new {@code MInvoicePaySchedule} for each payment detail</li>
	 * <li>Sets the due amount, discount, due date, and early payment discount
	 * date</li>
	 * <li>Links each schedule to the invoice and sets additional fields like
	 * payment rule</li>
	 * </ul>
	 * If any date is missing in the input, default values (like the current
	 * timestamp) are used.
	 * </p>
	 *
	 * @param invoice the invoice being processed
	 * @param body    the body of the electronic invoice containing payment details
	 * @param mbp     the business partner associated with the invoice
	 * @param mbpas   a list of existing bank accounts for the business partner
	 * @return a list of {@code MInvoicePaySchedule} records populated from the
	 *         invoice data
	 */
	private List<MInvoicePaySchedule> parseDatiPagamento(InvoiceReceived invoice, FatturaElettronicaBodyType body,
			MBPartner mbp, List<MBPBankAccount> mbpas) {
		List<MInvoicePaySchedule> scadenze = new ArrayList<>();
		for (DatiPagamentoType pagamento : body.getDatiPagamento()) {
			for (DettaglioPagamentoType dettaglio : pagamento.getDettaglioPagamento()) {
				MBPBankAccount mbpa = createBPBankAccount(mbp, dettaglio, mbpas);
				MInvoicePaySchedule ips = new MInvoicePaySchedule(Env.getCtx(), 0, null);
				ips.setC_Invoice_ID(invoice.get_ID());
				ips.setParent(invoice);
				ips.setDueAmt(dettaglio.getImportoPagamento());
				ips.setDiscountAmt(
						dettaglio.getScontoPagamentoAnticipato() != null ? dettaglio.getScontoPagamentoAnticipato()
								: BigDecimal.ZERO);

				Timestamp scadenza = dettaglio.getDataScadenzaPagamento() != null
						? new Timestamp(dettaglio.getDataScadenzaPagamento().toGregorianCalendar().getTimeInMillis())
						: Timestamp.valueOf(LocalDateTime.now());

				Timestamp scadenzaSconto = dettaglio.getDataLimitePagamentoAnticipato() != null
						? new Timestamp(
								dettaglio.getDataLimitePagamentoAnticipato().toGregorianCalendar().getTimeInMillis())
						: scadenza;
				ips.setDueDate(scadenza);
				ips.setDiscountDate(scadenzaSconto);
				// In caso non ci siano le date, queste vengono generate
				// in idempiere al momento del completamento della fattura
				// CondizioniPagamentoType cpt = pagamento.getCondizioniPagamento();
				// int pTerm = parsePaymentTermId(cpt.value());
				// ips.set_ValueOfColumn("LIT_PaymentTermType",
				// pTerm != -1 ? pTerm : mbp.getPO_PaymentTerm_ID());

				ModalitaPagamentoType dpt = dettaglio.getModalitaPagamento();
				ips.set_ValueOfColumn("PaymentRule", parsePaymentRule(dpt.value()));
				if (mbpa != null)
					ips.set_ValueOfColumn("C_BP_BankAccount_ID", mbpa.get_ID());
				scadenze.add(ips);

				if (body.getDatiPagamento().indexOf(pagamento) == 0
						&& pagamento.getDettaglioPagamento().indexOf(dettaglio) == 0 && isNewBP) {
					mbp.setPaymentRule(parsePaymentRule(dpt.value()));
					mbp.saveEx();
				}

			}
		}
		String checkPayment = scadenze.size() > 0 ? "Y" : "N";
		invoice.set_ValueOfColumn("LIT_isNoCheckPaymentTerm", checkPayment);
		if (scadenze.size() > 0) {
			invoice.set_ValueOfColumn(LIT_CHECK_PAYMENT_TERM, "Y");
		} else {
			invoice.set_ValueOfColumn(LIT_CHECK_PAYMENT_TERM, "N");
		}
		return scadenze;
	}

	/**
	 * Parses a list of e-invoice withholdings and converts them into a list of
	 * {@code MLCOInvoiceWithholding} records for further processing or saving.
	 *
	 * <p>
	 * For each withholding entry in the electronic invoice:
	 * <ul>
	 * <li>Determines the corresponding withholding type ID by querying the
	 * database</li>
	 * <li>Finds the matching tax record (of type "Ritenuta") by percentage</li>
	 * <li>Creates and configures a new {@code MLCOInvoiceWithholding} object</li>
	 * </ul>
	 * If no tax record is found, an exception may be thrown.
	 * </p>
	 *
	 * @param imponibile the taxable base amount for the withholdings
	 * @param ritenute   a list of {@code DatiRitenutaType} containing withholding
	 *                   information from the e-invoice
	 * @return a list of {@code MLCOInvoiceWithholding} objects built from the input
	 *         data
	 * @throws Exception if any required lookup (withholding type or tax) fails
	 */
	private List<MLCOInvoiceWithholding> parseDatiRitenuta(BigDecimal imponibile, List<DatiRitenutaType> ritenute)
			throws Exception {
		List<MLCOInvoiceWithholding> acconti = new ArrayList<>();
		for (DatiRitenutaType ritenuta : ritenute) {
			MLCOInvoiceWithholding acconto = new MLCOInvoiceWithholding(Env.getCtx(), 0, null);
			int typeId = DB.getSQLValue(null,
					"select lco_withholdingType_id from lco_withholdingType where LIT_WithHoldingTypeEInv  LIKE '%' || ? || '%'  and ad_client_id = ? "
							+ WHERE_ALL,
					ritenuta.getTipoRitenuta().value(), Env.getAD_Client_ID(Env.getCtx()));
			List<MTax> impostaRitenute = new Query(Env.getCtx(), MTax.Table_Name, "Name like 'Ritenuta%'", null)
					.setClient_ID().list();

			BigDecimal aliquota = ritenuta.getAliquotaRitenuta().setScale(2, RoundingMode.HALF_DOWN);

			String filtroRitenuta;
			if (aliquota.stripTrailingZeros().scale() == 0) {
				filtroRitenuta = aliquota.stripTrailingZeros().toPlainString();
			} else {
				filtroRitenuta = aliquota.setScale(2, RoundingMode.HALF_DOWN).toPlainString();
			}

			MTax imposta = impostaRitenute.stream().filter(tax -> {
				return tax.getName()
						.contains(filtroRitenuta+ "% A");
			}).findFirst().get();

			acconto.setLCO_WithholdingType_ID(typeId);
			acconto.setC_Tax_ID(imposta.get_ID());
			acconto.setTaxBaseAmt(imponibile);
			acconto.setTaxAmt(ritenuta.getImportoRitenuta());

			acconti.add(acconto);
		}
		return acconti;
	}

	/**
	 * Retrieves the product associated with a given tax, business partner, and
	 * e-invoice product type.
	 * <p>
	 * First attempts to find a matching entry in the {@code M_MsEinvProduct} table
	 * based on: tax ID, e-invoice product type, and business partner ID. If found,
	 * returns the corresponding {@code MProduct}. If no match is found, falls back
	 * to the business partner's LIT_M_Product_XML_ID field, if set.
	 * </p>
	 * 
	 * @param invTax the tax record to match
	 * @param mbp    the business partner
	 * @param type   the e-invoice product type (value of LIT_MsEinvProdType)
	 * @return the matched {@code MProduct}, or {@code null} if no match is found
	 */
	private MProduct getProductByTax(MTax invTax, MBPartner mbp, String type) {
		int einv_product_id = new Query(Env.getCtx(), M_MsEinvProduct.Table_Name,
				"IsActive = 'Y' AND C_Tax_ID = ? AND LIT_MsEinvProdType = ? AND C_BPartner_ID = ?" + WHERE_ORG, null)
				.setParameters(invTax.get_ID(), type, mbp.get_ID()).setClient_ID().firstId();

		if (einv_product_id > 0) {
			M_MsEinvProduct einv_product = new M_MsEinvProduct(Env.getCtx(), einv_product_id, null);
			return new MProduct(Env.getCtx(), einv_product.getM_Product_ID(), null);
		} else {
			if (mbp.get_ValueAsInt(LIT_XML_PRODUCT) > 0) {
				return new MProduct(Env.getCtx(), mbp.get_ValueAsInt(LIT_XML_PRODUCT), null);
			}
		}
		return null;
	}

	/**
	 * Retrieves and extracts XML content from a remote SFTP file. If the file is
	 * P7M file the signed content is extracted. Otherwise, the original input is
	 * returned after removing any BOM (Byte Order Mark) if present.
	 *
	 * @param entry the remote file metadata
	 * @param sftp  the SFTP client used to access the file
	 * @return a byte array containing the extracted XML content without BOM
	 * @throws Exception if an SFTP, I/O, or CMS parsing error occurs
	 */
	public byte[] getXml(RemoteResourceInfo entry, SFTPClient sftp) throws Exception {
		try (RemoteFile f = sftp.getSFTPEngine().open(entry.getPath());
				InputStream is = f.new RemoteFileInputStream(0);
				ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

			is.transferTo(baos);
			byte[] xml = baos.toByteArray();

			if (entry.getName().toLowerCase().endsWith(".p7m")) {
				CMSSignedData signature = new CMSSignedData(xml);
				CMSProcessable sc = signature.getSignedContent();
				xml = (byte[]) sc.getContent();
			}

			return removeBOMIfPresent(xml);
		}
	}

	/**
	 * Reads the content of the given file and extracts the XML, handling optional
	 * P7M signature. If the file is a P7M file, the signed content is extracted.
	 * Otherwise, the raw content is returned. In both cases, any Byte Order Mark
	 * (BOM) is removed.
	 *
	 * @param entry the input file, possibly a P7M-signed XML
	 * @return a byte array containing the extracted XML content without BOM
	 * @throws Exception if an I/O or CMS parsing error occurs
	 */
	public byte[] getXml(File entry) throws Exception {
		try (InputStream is = new FileInputStream(entry); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

			is.transferTo(baos);
			byte[] xml = baos.toByteArray();

			if (entry.getName().toLowerCase().endsWith(".p7m")) {
				CMSSignedData signature = new CMSSignedData(xml);
				CMSProcessable sc = signature.getSignedContent();
				xml = (byte[]) sc.getContent();
			}
			return removeBOMIfPresent(xml);
		}
	}

	/**
	 * Parses a potentially signed XML (P7M) byte array and extracts the raw XML
	 * content. If the input is a valid P7M file, the signed content is extracted.
	 * Otherwise, the raw content is returned. In both cases, any Byte Order Mark
	 * (BOM) is removed.
	 *
	 * @param xml the input byte array, possibly a CMS-signed (P7M) file
	 * @return the unsigned XML content with BOM removed if present
	 */
	public byte[] getXml(byte[] xml) {
		try {
			byte[] parsed;
			CMSSignedData signature = new CMSSignedData(xml);
			CMSProcessable sc = signature.getSignedContent();
			parsed = (byte[]) sc.getContent();
			return removeBOMIfPresent(parsed);
		} catch (Exception e) {
			log.warning("Fattura non p7m");
		}
		return removeBOMIfPresent(xml);
	}

	/**
	 * Removes the UTF-8 BOM from the beginning of the byte array, if present.
	 * Checks if the first three bytes correspond to the UTF-8 BOM sequence (0xEF,
	 * 0xBB, 0xBF). If so, returns a new array without the BOM; otherwise, returns
	 * the original array unchanged.
	 *
	 * @param xml the byte array to check for a BOM
	 * @return the byte array without BOM if it was present, or the original array
	 */
	private byte[] removeBOMIfPresent(byte[] xml) {
		ByteBuffer bb = ByteBuffer.wrap(xml);
		byte[] bom = new byte[3];
		bb.get(bom);
		String content = new String(Hex.encode(bom));
		if ("efbbbf".equalsIgnoreCase(content)) {
			xml = new byte[xml.length - 3];
			bb.get(xml);
		}
		return xml;
	}

	/**
	 * Parses the given XML byte array representing an e-invoice and returns an
	 * {@code InvoiceReceived} object.
	 *
	 * The method uses {@code ManageXML_new} to deserialize the XML into a
	 * {@code FatturaElettronicaType} object, then processes it to generate the
	 * corresponding invoice. If an error occurs during processing, and a new
	 * business partner was created as part of the operation, the business partner
	 * is deleted to maintain consistency.
	 *
	 * Any exception is logged, and a partially constructed invoice object is
	 * returned with an error message attached.
	 *
	 * @param xml the XML data representing the electronic invoice
	 * @return the parsed invoice, or an empty one with error information if parsing
	 *         fails
	 */
	public InvoiceReceived getInvoiceFromXml(byte[] xml) {
		ManageXML_new manageXml = new ManageXML_new();
		FatturaElettronicaType fattura = manageXml.importFatturaElettronica(new ByteArrayInputStream(xml));
		InvoiceReceived inv = new InvoiceReceived(Env.getCtx(), 0, null);
		try {
			inv = getInvoice(fattura);
		} catch (Exception e) {
			e.printStackTrace();
			if (isNewBP) {
				bp.delete(false);
				log.warning("Errore parsing fattura, nuovo BP eliminato");
				inv.setErrorMsg("Errore durante la lettura della fattura");
			}
		}
		return inv;
	}

	/**
	 * Selects an appropriate tax based on VAT document sequence, tax nature, rate,
	 * and price.
	 * <p>
	 * The method attempts to find a matching {@code MTax} from a cached list by:
	 * <ul>
	 * <li>Filtering by tax nature and display status, prioritizing those linked to
	 * "Reverse charge"</li>
	 * <li>Matching tax rate and country group when VAT sequence is provided</li>
	 * <li>Returning a default tax if the price is zero</li>
	 * <li>Fallback to tax matching only by rate</li>
	 * </ul>
	 * If no match is found, returns a new empty {@code MTax} instance.
	 * </p>
	 *
	 * @param registroIva the VAT document sequence containing country group
	 *                    information
	 * @param natura      the tax nature type (can be null)
	 * @param aliquota    the tax rate as BigDecimal
	 * @param prezzo      the price amount for which tax is applied
	 * @return a matching {@code MTax} object or a new empty tax if no match is
	 *         found
	 */
	private MTax getTax(final MLITVATDocTypeSequence registroIva, NaturaType natura, BigDecimal aliquota,
			BigDecimal prezzo, MBPartner bp) {
		Optional<MTax> res = null;
		// Filtra in base alla natura
		if (natura != null) {
			res = taxes.stream()
					.filter(tax -> tax.get_ValueAsString(LIT_XML_INVOICE_TAX_TYPE).startsWith(natura.value())
							&& tax.get_ValueAsBoolean(IS_DISPLAYED) && tax.get_ValueAsBoolean(LIT_IS_DEFAULT_TAX))
					.findFirst();
			if (res.isPresent())
				return res.get();

			res = taxes.stream()
					.filter(tax -> tax.get_ValueAsString(LIT_XML_INVOICE_TAX_TYPE).startsWith(natura.value())
							&& tax.get_ValueAsBoolean(IS_DISPLAYED) && tax.get_ValueAsBoolean(LIT_IS_DEFAULT_TAX)
							&& tax.getC_TaxCategory().getName().contains("Reverse charge"))
					.findFirst();
			if (res.isPresent())
				return res.get();

			res = taxes.stream()
					.filter(tax -> tax.get_ValueAsString(LIT_XML_INVOICE_TAX_TYPE).startsWith(natura.value())
							&& !tax.get_ValueAsBoolean(IS_DISPLAYED)
							&& tax.getC_TaxCategory().getName().contains("Reverse charge"))
					.findFirst();
			if (res.isPresent())
				if (res.get().getParent_Tax_ID() > 0 && !res.get().get_ValueAsBoolean(LIT_IS_DEFAULT_TAX))
					return new MTax(Env.getCtx(), res.get().getParent_Tax_ID(), null);
				else if (res.get().get_ValueAsBoolean(LIT_IS_DEFAULT_TAX))
					return new MTax(Env.getCtx(), res.get().get_ID(), null);

			res = taxes.stream()
					.filter(tax -> tax.get_ValueAsString(LIT_XML_INVOICE_TAX_TYPE).startsWith(natura.value())
							&& tax.get_ValueAsBoolean(LIT_IS_DEFAULT_TAX) && tax.get_ValueAsBoolean(IS_DISPLAYED))
					.findFirst();

			Optional<MTax> alt = taxes.stream()
					.filter(tax -> tax.get_ValueAsString(LIT_XML_INVOICE_TAX_TYPE).startsWith(natura.value())
							&& tax.get_ValueAsBoolean(IS_DISPLAYED))
					.findFirst();

			if (res.isPresent())
				return res.get();
			else if (alt.isPresent())
				return alt.get();
		}
		// Filtra in base ad Aliquota e Country from/to e Persona giuridica
		res = taxes.stream().filter(tax -> {
			if (tax.getC_CountryGroupFrom() != null && registroIva != null) {
				return tax.getRate().compareTo(aliquota) == 0 && tax.getSOPOType().equals("B")
						&& tax.getC_CountryGroupFrom_ID() == registroIva.getC_CountryGroupFrom_ID()
						&& (bp.get_ValueAsString("LIT_TaxTypeBPPartner_ID")
								.equals(tax.get_ValueAsString("LIT_TaxTypeBPPartner_ID"))
								|| tax.get_ValueAsString("LIT_TaxTypeBPPartner_ID").equals(""));
			}
			return false;
		}).findFirst();
		if (res.isPresent())
			return res.get();

		// Probabile filtro ridondante
		res = taxes.stream().filter(tax -> {
			if (tax.getC_CountryGroupFrom() != null && registroIva != null) {
				return tax.getRate().compareTo(aliquota) == 0 && tax.getSOPOType().equals("B")
						&& tax.getC_CountryGroupFrom_ID() == registroIva.getC_CountryGroupFrom_ID();
			}
			return false;
		}).findFirst();
		if (res.isPresent())
			return res.get();

		// Default per prezzo = 0
		res = taxes.stream().filter(
				tax -> prezzo.compareTo(BigDecimal.ZERO) == 0 && tax.get_ValueAsString("Value").startsWith("G.06"))
				.findFirst();
		if (res.isPresent())
			return res.get();

		res = taxes.stream().filter(tax -> tax.getRate().compareTo(aliquota) == 0 && tax.getSOPOType().equals("B"))
				.findFirst();
		if (res.isPresent())
			return res.get();

		return new MTax(Env.getCtx(), 0, null);
	}

	/**
	 * Creates and saves a new business partner based on the e-invoice partner data.
	 * <p>
	 * Sets tax ID and national ID, assigns name from the partner's business or
	 * personal details, marks the partner as a vendor (not customer), and applies
	 * default payment terms and payment rule. The new partner is then saved to the
	 * database.
	 * </p>
	 *
	 * @param fattura the electronic invoice containing supplier information
	 * @param piva    the VAT number (tax ID) of the business partner
	 * @return the newly created and saved {@code MBPartner}
	 */
	private MBPartner createAndSaveBusinessPartner(FatturaElettronicaType fattura, String piva) {
		MBPartner mbp = new MBPartner(Env.getCtx(), 0, null);
		DatiAnagraficiCedenteType anagrafica = fattura.getFatturaElettronicaHeader().getCedentePrestatore()
				.getDatiAnagrafici();
		mbp.set_ValueOfColumn("LIT_TaxId", piva);
		mbp.setTaxID(piva);
		mbp.set_ValueOfColumn("LIT_NationalIdNumber",
				!Utils.isBlank(anagrafica.getCodiceFiscale()) ? anagrafica.getCodiceFiscale() : null);
		mbp.set_ValueOfColumn("LIT_NationalIdNumber_ID",
				!Utils.isBlank(anagrafica.getCodiceFiscale()) ? anagrafica.getCodiceFiscale() : null);

		if (anagrafica.getAnagrafica().getDenominazione() != null)
			mbp.setName(anagrafica.getAnagrafica().getDenominazione());
		else {
			mbp.setName(anagrafica.getAnagrafica().getNome() + " " + anagrafica.getAnagrafica().getCognome());
		}

		mbp.setIsVendor(true);
		mbp.setIsCustomer(false);
		Integer paymentTermId = new Query(Env.getCtx(), MPaymentTerm.Table_Name,
				"isdefault = 'Y' and isactive='Y'" + WHERE_ALL, null).setClient_ID().firstId();
		mbp.setPO_PaymentTerm_ID(paymentTermId);
		mbp.setPaymentRulePO(parsePaymentRule(DEFAULT_PAYMENT_RULE));
//		if (mbp.getPO_PaymentTerm_ID() < 1) {
//			Integer paymentTermId = new Query(Env.getCtx(), MPaymentTerm.Table_Name, "isdefault = 'Y' and isactive='Y'",
//					null).setClient_ID().firstId();
//			mbp.setPO_PaymentTerm_ID(paymentTermId);
//			mbp.saveEx();
//		}
//		if (mbp.getPaymentRulePO() == null || mbp.getPaymentRulePO().isBlank()) {
//			mbp.setPaymentRulePO(parsePaymentRule("MP05"));
//			mbp.saveEx();
//		}

		mbp.saveEx();
		isNewBP = true;
		return mbp;
	}

	/**
	 * Retrieves or creates a business partner location based on the invoice header
	 * address.
	 * <p>
	 * Attempts to find an existing {@code MLocation} matching the address, postal
	 * code, country, and city from the electronic invoice. If no existing location
	 * is found, creates and saves a new {@code MLocation} with the provided
	 * details, including country and region lookup. Finally, creates an
	 * {@code MBPartnerLocation} referencing this location, named "Sede legale".
	 * </p>
	 *
	 * @param fattura the electronic invoice containing the supplier's address
	 *                information
	 * @return a new {@code MBPartnerLocation} linked to the found or created
	 *         location
	 */
	private MBPartnerLocation getBPLocationFromEinvoice(FatturaElettronicaType fattura, String name) {
		MBPartnerLocation mbpLocation = new MBPartnerLocation(Env.getCtx(), 0, null);
		IndirizzoType sede = fattura.getFatturaElettronicaHeader().getCedentePrestatore().getSede();
		MLocation location = new Query(Env.getCtx(), MLocation.Table_Name,
				"c_location.address1 ILIKE '%' || ? || '%' AND " + " c_location.postal = ? AND co.countrycode = ? AND "
						+ " c_location.city ILIKE '%' || ? || '%'",
				null).addJoinClause("JOIN c_country co on C_Location.c_country_id = co.c_country_id ")
				.addJoinClause("JOIN c_region reg on C_Location.c_region_id = reg.c_region_id ")
				.setParameters(sede.getIndirizzo(), sede.getCAP(), sede.getNazione(), sede.getComune()).setClient_ID()
				.first();
		if (location == null) {
			location = new MLocation(Env.getCtx(), 0, null);
			String indirizzo = sede.getIndirizzo();
			if (sede.getNumeroCivico() != null && !indirizzo.contains(sede.getNumeroCivico())) {
				indirizzo += ", " + sede.getNumeroCivico();
			}
			location.setAddress1(indirizzo);
			location.setPostal(sede.getCAP());
			location.setCity(sede.getComune());
			MCountry country = new Query(Env.getCtx(), MCountry.Table_Name, "countrycode = ?", null)
					.setParameters(sede.getNazione()).first();

			if (country != null) {
				location.setCountry(country);
				MRegion region = new Query(Env.getCtx(), MRegion.Table_Name, "name = ? AND c_country_id = ?", null)
						.setParameters(sede.getProvincia(), country.get_ID()).first();
				location.setRegion(region);
			}
			location.saveEx();
		}
		mbpLocation.setAD_Org_ID(orgId);
		mbpLocation.setName(name);
		mbpLocation.setC_Location_ID(location.get_ID());
		return mbpLocation;
	}

	/**
	 * Creates or retrieves a business partner bank account based on payment
	 * details.
	 * <p>
	 * If the IBAN is missing, returns {@code null}. Otherwise, checks if a bank
	 * account with the same IBAN already exists in the provided list and returns
	 * it. If not found, creates a new {@code MBPBankAccount}, associates it with
	 * the business partner, sets bank details (including creating a new
	 * {@code MBank} if necessary), and saves it.
	 * </p>
	 *
	 * @param mbp       the business partner to associate the bank account with
	 * @param dettaglio the payment detail containing bank account information
	 * @param mbpas     the existing list of business partner bank accounts to check
	 *                  against
	 * @return the existing or newly created {@code MBPBankAccount}, or {@code null}
	 *         if IBAN is missing or on error
	 */
	private MBPBankAccount createBPBankAccount(MBPartner mbp, DettaglioPagamentoType dettaglio,
			List<MBPBankAccount> mbpas) {
		try {
			// No IBAN = No BankAccount
			if (dettaglio.getIBAN() == null)
				return mbp.getBankAccounts(true)[0];

			String iban = dettaglio.getIBAN();
			MBPBankAccount mbpa = mbpas.stream().filter(bpa -> dettaglio.getIBAN().equals(bpa.getIBAN())).findFirst()
					.orElse(null);

			if (mbpa != null) {
				return mbpa;
			}

			mbpa = new MBPBankAccount(Env.getCtx(), 0, null);
			mbpa.setC_BPartner_ID(mbp.get_ID());
			mbpa.setIsActive(true);
			mbpa.setIsACH(true);
			mbpa.setIBAN(dettaglio.getIBAN());

			String abi = dettaglio.getABI();
			String cab = dettaglio.getCAB();
			String routingNo;

			if (abi != null && cab != null) {
				routingNo = abi + cab;
			} else {
				routingNo = iban.substring(5, 10) + iban.substring(10, 15);
			}

			String bankName = dettaglio.getIstitutoFinanziario();
			if (bankName != null)
				mbpa.set_ValueOfColumn("BankNameBP", dettaglio.getIstitutoFinanziario());
			else
				bankName = "Banca - " + routingNo;

			MBank bank = new Query(Env.getCtx(), MBank.Table_Name, "RoutingNo = ? AND isActive = 'Y'" + WHERE_ORG, null)
					.setClient_ID().setParameters(routingNo).first();

			if (bank == null) {
				bank = new MBank(Env.getCtx(), 0, null);
				bank.setName(bankName);
				bank.setRoutingNo(routingNo);
				if (dettaglio.getBIC() != null)
					bank.setSwiftCode(dettaglio.getBIC());
				bank.saveEx();
			}

			mbpa.setC_Bank_ID(bank.get_ID());
			mbpa.saveEx();
			return mbpa;

		} catch (Exception e) {
			log.warning("Impossibile creare Business Partner Bank Account");
			log.warning(e.getMessage());
		}
		return null;
	}

	/**
	 * Finds an existing business partner by VAT or national tax code, or creates a
	 * new one if not found.
	 * <p>
	 * Searches for a {@code MBPartner} where the given tax code matches either
	 * {@code TaxID} or {@code LIT_NationalIDNumber}. If no match is found, a new
	 * business partner and location are created based on the electronic invoice
	 * data. The new partner is saved, marked as a vendor, and stored in the
	 * {@code bp} field.
	 * </p>
	 *
	 * @param fattura the electronic invoice containing the supplier's information
	 * @return the existing or newly created {@code MBPartner}
	 */
	private MBPartner findOrCreateBPartner(FatturaElettronicaType fattura) {
		String codice = fattura.getFatturaElettronicaHeader().getCedentePrestatore().getDatiAnagrafici()
				.getIdFiscaleIVA().getIdCodice();

		MBPartner mbp = createAndSaveBusinessPartner(fattura, codice);
		mbp.setIsVendor(true);
		mbp.setAD_Org_ID(orgId);

		MBPartnerLocation mbpLocation = getBPLocationFromEinvoice(fattura, mbp.getName());
		mbpLocation.setC_BPartner_ID(mbp.get_ID());
		mbpLocation.saveEx();
		mbp.setPrimaryC_BPartner_Location_ID(mbpLocation.get_ID());

		mbp.saveEx();
		bp = mbp;
		return mbp;
	}

	/**
	 * Finds the corresponding document type ({@code MDocType}) based on the
	 * e-invoice document code.
	 * <p>
	 * Attempts to match the {@code lit_fepa_doctype} field with the value from
	 * {@code DatiGeneraliDocumentoType#getTipoDocumento()}. If no match is found,
	 * falls back to the default type "TD01" (typically representing a standard
	 * invoice).
	 * </p>
	 *
	 * @param dgd the e-invoice general document data
	 * @return the matching {@code MDocType}, or the default "TD01" type if no exact
	 *         match is found
	 */
	private MDocType findDocType(DatiGeneraliDocumentoType dgd) {
		String tipoDocumento = dgd.getTipoDocumento().value();
		MDocType docType = new Query(Env.getCtx(), MDocType.Table_Name,
				"lit_fepa_doctype = ? and issotrx='N' " + WHERE_ALL, null).setClient_ID().setParameters(tipoDocumento)
				.first();
		if (docType != null)
			return docType;
		return new Query(Env.getCtx(), MDocType.Table_Name, "lit_fepa_doctype = 'TD01' and issotrx='N' " + WHERE_ALL,
				null).setClient_ID().first();
	}

	/**
	 * Maps a payment method code (e.g., "MP01") to its corresponding internal
	 * payment rule value.
	 * <p>
	 * The method translates known payment method codes to their descriptions and
	 * then looks up the corresponding {@code AD_Ref_List} value where the reference
	 * name is "_Payment Rule". If the code is unrecognized, it defaults to "Direct
	 * Debit".
	 * </p>
	 *
	 * @param id the external payment method code (e.g., "MP05")
	 * @return the internal payment rule value from {@code AD_Ref_List}
	 */
	private String parsePaymentRule(String id) {
		String modalitàPagamento = "";
		switch (id) {
		case "MP01":
			modalitàPagamento = "Mixed POS Payment";
			break;
		case "MP02":
			modalitàPagamento = "Check";
			break;
		case DEFAULT_PAYMENT_RULE:
			modalitàPagamento = "Direct Deposit";
			break;
		case "MP08":
			modalitàPagamento = "Credit Card";
			break;
		case "MP09":
			modalitàPagamento = "RID";
			break;
		case "MP12":
			modalitàPagamento = "Direct Debit";
			break;
		default:
			modalitàPagamento = "Direct Debit";
			break;
		}
		MRefList ref = new Query(Env.getCtx(), MRefList.Table_Name,
				"AD_Ref_List.Name like ? AND re.name = '_Payment Rule' ", null)
				.addJoinClause("JOIN ad_reference re on re.ad_reference_id = AD_Ref_List.ad_reference_id ")
				.setParameters(modalitàPagamento).first();
		return ref.getValue();
	}

	public List<M_EsitoCredem> getDatiEsito(byte[] xml) throws Exception {
		ArrayList<M_EsitoCredem> res = new ArrayList<>();

		DocumentBuilderFactory xmlFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = xmlFactory.newDocumentBuilder();
		XPathFactory xPathfactory = XPathFactory.newInstance();
		XPath xpath = xPathfactory.newXPath();

		Document document = builder.parse(new InputSource(new ByteArrayInputStream(xml)));

		NodeList esiti = (NodeList) xpath.compile("//ESITO").evaluate(document, XPathConstants.NODESET);
		for (int i = 1; i <= esiti.getLength(); i++) {
			M_EsitoCredem de = new M_EsitoCredem(Env.getCtx(), 0, null);

			de.setDescription((String) xpath.compile("//ESITO[" + i + "]/Descrizione/text()").evaluate(document,
					XPathConstants.STRING));
			de.setDocumentNo((String) xpath.compile("//ESITO[" + i + "]/RiferimentoFattura/NumeroFattura/text()")
					.evaluate(document, XPathConstants.STRING));
			de.setLIT_MsTipoEsito(Integer.valueOf((String) xpath.compile("//ESITO[" + i + "]/TipoEsito/text()")
					.evaluate(document, XPathConstants.STRING)));
			de.setLIT_MsYearInvoiced(
					Integer.valueOf((String) xpath.compile("//ESITO[" + i + "]/RiferimentoFattura/AnnoFattura/text()")
							.evaluate(document, XPathConstants.STRING)));
			de.setName("Esito: " + de.getDocumentNo());
			res.add(de);
		}

		return res;
	}

	/**
	 * Loads all active tax records for the current client.
	 *
	 * @return a list of active {@code MTax} entries
	 */
	private List<MTax> loadApplicableTaxes() {
		return new Query(Env.getCtx(), MTax.Table_Name, "IsActive = 'Y'" + WHERE_ALL, null).setClient_ID().list();
	}

	private boolean isBannedDocument(FatturaElettronicaBodyType body) {
		return BANNED_DOCUMENT_TYPES.contains(body.getDatiGenerali().getDatiGeneraliDocumento().getTipoDocumento());
	}

	private Timestamp toTimestamp(XMLGregorianCalendar calendar) {
		return new Timestamp(calendar.toGregorianCalendar().getTimeInMillis());
	}

	public static boolean getIsNewBP() {
		return isNewBP;
	}

	public static void setIsNewBP(boolean value) {
		isNewBP = value;
	}
}
