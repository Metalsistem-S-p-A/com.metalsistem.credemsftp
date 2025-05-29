package com.metalsistem.credemsftp.utils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.adempiere.model.MBroadcastMessage;
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
import org.compiere.model.MRole;
import org.compiere.model.MTax;
import org.compiere.model.MUOM;
import org.compiere.model.MWindow;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.globalqss.model.MLCOInvoiceWithholding;
import org.idempiere.broadcast.BroadcastMsgUtil;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.itextpdf.io.source.ByteArrayOutputStream;
import com.metalsistem.credemsftp.model.M_EsitoCredem;

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
import it.cnet.idempiere.LIT_E_Invoice.modelXML2.TipoDocumentoType;
import it.cnet.idempiere.LIT_E_Invoice.utilXML.ManageXML_new;
import it.cnet.idempiere.VATJournalModel.MLITVATDocTypeSequence;
import it.cnet.idempiere.lettIntent.model.MBPLetterIntent;
import net.schmizz.sshj.sftp.RemoteFile;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;

public class InvoiceParser {
	private static final CLogger log = CLogger.getCLogger(InvoiceParser.class);
	private final List<MTax> taxes;
	private final List<MUOM> uoms;
	private static final List<TipoDocumentoType> BANNED_DOCUMENT_TYPES = List.of(TipoDocumentoType.TD_04,
			TipoDocumentoType.TD_16, TipoDocumentoType.TD_17, TipoDocumentoType.TD_18);

	public InvoiceParser() {
		taxes = loadApplicableTaxes();
		uoms = new Query(Env.getCtx(), MUOM.Table_Name, "", null).setClient_ID().list();
	}

	private List<MTax> loadApplicableTaxes() {
		return new Query(Env.getCtx(), MTax.Table_Name, "to_country_id = 214", null).setClient_ID().list();
	}

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

	public InvoiceReceived getInvoiceFromXml(byte[] xml) throws Exception {
		ManageXML_new manageXml = new ManageXML_new();
		FatturaElettronicaType fattura = manageXml.importFatturaElettronica(new ByteArrayInputStream(xml));
		return getInvoice(fattura);
	}

	private InvoiceReceived getInvoice(FatturaElettronicaType fattura) throws Exception {
		// TODO: Gestire caso di molteplici body(?)
		InvoiceReceived invoice = new InvoiceReceived(new MInvoice(Env.getCtx(), 0, null));
		FatturaElettronicaBodyType body = fattura.getFatturaElettronicaBody().get(0);
		if (isBannedDocument(body)) {
			return null;
		}

		DatiGeneraliDocumentoType datiGeneraliDocumento = body.getDatiGenerali().getDatiGeneraliDocumento();
		invoice.setIsSOTrx(false);
		invoice.setAD_Org_ID(Env.getAD_Org_ID(Env.getCtx()));
		invoice.setDocumentNo(datiGeneraliDocumento.getNumero());
		invoice.setDateInvoiced(toTimestamp(datiGeneraliDocumento.getData()));
		invoice.setGrandTotal(datiGeneraliDocumento.getImportoTotaleDocumento());
		invoice.setC_Currency_ID(MCurrency.get(datiGeneraliDocumento.getDivisa()).get_ID());

		// TIPO DOCUMENTO
		MDocType docType = findDocType(datiGeneraliDocumento);

		invoice.setC_DocType_ID(docType.get_ID());
		invoice.setC_DocTypeTarget_ID(docType.get_ID());
		invoice.set_ValueOfColumn("LIT_FEPA_DOCTYPE", datiGeneraliDocumento.getTipoDocumento().value());

		// BUSINESS PARTNER
		MBPartner mbp = findOrCreateBPartner(fattura);

		invoice.setC_PaymentTerm_ID(mbp.getPO_PaymentTerm_ID());
		invoice.setPaymentRule(mbp.getPaymentRulePO());
		invoice.setBPartner(mbp);

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
		if (body.getDatiBeniServizi().getDatiRiepilogo().get(0).getNatura() != null
				&& "N3.5".equals(body.getDatiBeniServizi().getDatiRiepilogo().get(0).getNatura().value())) {

			MBPLetterIntent letter = new Query(Env.getCtx(), MBPLetterIntent.Table_Name,
					" isSOTrx = 'N' " + "AND bp_letterintentdatevalidfrom < Current_date "
							+ "AND bp_letterintentdatevalidto > current_date " + "AND c_bpartner_id = ?",
					null).setClient_ID().setParameters(mbp.get_ID()).first();
			if (letter != null)
				invoice.set_ValueOfColumn("c_bp_partner_letterintent_id", letter.get_ID());
		}

		// LINEE
		List<MInvoiceLine> linee = new ArrayList<MInvoiceLine>();
		BigDecimal imponibile = BigDecimal.ZERO;
		for (DettaglioLineeType linea : body.getDatiBeniServizi().getDettaglioLinee()) {
			MInvoiceLine il = new MInvoiceLine(Env.getCtx(), -1, null);
			il.setInvoice(invoice);
			il.setName(linea.getDescrizione());
			il.setPrice(linea.getPrezzoUnitario());
			il.setLine(linea.getNumeroLinea() * 10); // iDempiere Standard
			il.setDescription(linea.getDescrizione());

			MTax invTax = getTax(registroIva, linea);
			il.setC_Tax_ID(invTax.get_ID());

			if (invTax.getRate().compareTo(BigDecimal.ZERO) > 0) {
				imponibile = imponibile.add(linea.getPrezzoUnitario());
			}
			if (mbp.get_ValueAsInt("LIT_M_Product_XML_ID") > 0) {
				MProduct prod = new MProduct(Env.getCtx(), mbp.get_ValueAsInt("LIT_M_Product_XML_ID"), null);
				il.setProduct(prod);
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
							|| linea.getUnitaMisura()
									.toLowerCase()
									.equals(uom.get_Translation("Name", "it_IT").toLowerCase())
							|| linea.getUnitaMisura()
									.toLowerCase()
									.equals(uom.get_Translation("UOMSymbol", "it_IT").toLowerCase())) {
						il.setC_UOM_ID(uom.get_ID());
						break;
					}
				}
			}
			linee.add(il);
		}

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
			if (mbp.get_ValueAsInt("LIT_M_Product_XML_ID") > 0) {
				MProduct prod = new MProduct(Env.getCtx(), mbp.get_ValueAsInt("LIT_M_Product_XML_ID"), null);
				il.setProduct(prod);
			}
			MTax invTax = taxes.stream().filter(tax -> {
				if (riepilogo.getNatura() != null
						&& tax.get_ValueAsString("LIT_XMLInvoice_TaxType").startsWith(riepilogo.getNatura().value())) {
					return true;
				}
				if (tax.getC_CountryGroupFrom() != null && registroIva != null) {
					return tax.getRate().compareTo(riepilogo.getAliquotaIVA()) == 0 && tax.getSOPOType().equals("B")
							&& tax.getC_CountryGroupFrom_ID() == registroIva.getC_CountryGroupFrom_ID();
				} else {
					return tax.getRate().compareTo(riepilogo.getAliquotaIVA()) == 0 && tax.getSOPOType().equals("B");
				}
			}).findFirst().get();

			il.setC_Tax_ID(invTax.get_ID());
			linee.add(il);
		}

		// CASSA PREVIDENZIALE
		List<DatiCassaPrevidenzialeType> datiCassa = body.getDatiGenerali()
				.getDatiGeneraliDocumento()
				.getDatiCassaPrevidenziale();

		for (DatiCassaPrevidenzialeType dato : datiCassa) {
			MInvoiceLine il = new MInvoiceLine(Env.getCtx(), -1, null);
			il.setInvoice(invoice);
			il.setQtyEntered(BigDecimal.ONE);
			il.setQtyInvoiced(BigDecimal.ONE);
			il.setPrice(dato.getImportoContributoCassa());
			il.setName("Contributo previdenziale " + dato.getTipoCassa().value() + " " + dato.getAlCassa());
			il.setDescription("Contributo previdenziale " + dato.getTipoCassa().value() + " " + dato.getAlCassa());

			if (mbp.get_ValueAsInt("LIT_M_Product_XML_ID") > 0) {
				MProduct prod = new MProduct(Env.getCtx(), mbp.get_ValueAsInt("LIT_M_Product_XML_ID"), null);
				il.setProduct(prod);
			}

			MTax invTax = taxes.stream().filter(tax -> {
				if (dato.getNatura() != null
						&& tax.get_ValueAsString("LIT_XMLInvoice_TaxType").startsWith(dato.getNatura().value())) {
					return true;
				}
				if (tax.getC_CountryGroupFrom() != null && registroIva != null) {
					return tax.getRate().compareTo(dato.getAliquotaIVA()) == 0 && tax.getSOPOType().equals("B")
							&& tax.getC_CountryGroupFrom_ID() == registroIva.getC_CountryGroupFrom_ID();
				} else {
					return tax.getRate().compareTo(dato.getAliquotaIVA()) == 0 && tax.getSOPOType().equals("B");
				}
			}).findFirst().get();

			il.setC_Tax_ID(invTax.get_ID());
			linee.add(il);
		}
		invoice.setInvoiceLines(linee);

		// SCADENZE
		List<MBPBankAccount> mbpas = List.of(mbp.getBankAccounts(true));
		List<MInvoicePaySchedule> scadenze = parseDatiPagamento(invoice, body, mbp, mbpas);
		invoice.setScheduledPayments(scadenze);

		// RITENUTE
		List<DatiRitenutaType> datiRitenuta = body.getDatiGenerali().getDatiGeneraliDocumento().getDatiRitenuta();
		List<MLCOInvoiceWithholding> riteunute = parseDatiRitenuta(imponibile, datiRitenuta);
		invoice.setWithHoldings(riteunute);

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

	private List<MInvoicePaySchedule> parseDatiPagamento(InvoiceReceived invoice, FatturaElettronicaBodyType body,
			MBPartner mbp, List<MBPBankAccount> mbpas) {
		List<MInvoicePaySchedule> scadenze = new ArrayList<>();
		for (DatiPagamentoType pagamento : body.getDatiPagamento()) {
			for (DettaglioPagamentoType dettaglio : pagamento.getDettaglioPagamento()) {
				createBPBankAccount(mbp, dettaglio, mbpas);
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
				scadenze.add(ips);
			}
		}
		if (scadenze.size() > 0) {
			invoice.set_ValueOfColumn("LIT_isNoCheckPaymentTerm", "Y");
		} else {
			invoice.set_ValueOfColumn("LIT_isNoCheckPaymentTerm", "N");
		}
		return scadenze;
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

			de.setDescription((String) xpath.compile("//ESITO[" + i + "]/Descrizione/text()")
					.evaluate(document, XPathConstants.STRING));
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

	private List<MLCOInvoiceWithholding> parseDatiRitenuta(BigDecimal imponibile, List<DatiRitenutaType> ritenute)
			throws Exception {
		List<MLCOInvoiceWithholding> acconti = new ArrayList<>();
		for (DatiRitenutaType ritenuta : ritenute) {
			MLCOInvoiceWithholding acconto = new MLCOInvoiceWithholding(Env.getCtx(), 0, null);
			int typeId = DB.getSQLValue(null,
					"select lco_withholdingType_id from lco_withholdingType where LIT_WithHoldingTypeEInv  LIKE '%' || ? || '%'  and ad_client_id = ?",
					ritenuta.getTipoRitenuta().value(), Env.getAD_Client_ID(Env.getCtx()));
			List<MTax> impostaRitenute = new Query(Env.getCtx(), MTax.Table_Name, "Name like 'Ritenuta%'", null)
					.setClient_ID()
					.list();

			MTax imposta = impostaRitenute.stream().filter(tax -> {
				return tax.getName().contains(ritenuta.getAliquotaRitenuta().intValue() + "% A");
			}).findFirst().get();

			acconto.setLCO_WithholdingType_ID(typeId);
			acconto.setC_Tax_ID(imposta.get_ID());
			acconto.setTaxBaseAmt(imponibile);
			acconto.setTaxAmt(ritenuta.getImportoRitenuta());
			acconti.add(acconto);
		}
		return acconti;
	}

	private MTax getTax(final MLITVATDocTypeSequence registroIva, DettaglioLineeType linea) {
		MTax invTax = null;
		// N3.5 = Lettera d'intento
		if (linea.getNatura() != null && "N3.5".equals(linea.getNatura().value())) {
			// Non imponibile Art 8 c.1
			invTax = new Query(Env.getCtx(), MTax.Table_Name, "value = 'F.02'", null).setClient_ID().first();
		} else {
			invTax = taxes.stream().filter(tax -> {
				if (tax.getC_CountryGroupFrom() != null && registroIva != null) {
					return tax.getRate().compareTo(linea.getAliquotaIVA()) == 0 && tax.getSOPOType().equals("B")
							&& tax.getC_CountryGroupFrom_ID() == registroIva.getC_CountryGroupFrom_ID();
				} else {
					return tax.getRate().compareTo(linea.getAliquotaIVA()) == 0 && tax.getSOPOType().equals("B");
				}
			}).findFirst().get();
		}
		return invTax;
	}

	private MBPartner createAndSaveBusinessPartner(FatturaElettronicaType fattura, String piva) {
		MBPartner mbp = new MBPartner(Env.getCtx(), 0, null);
		DatiAnagraficiCedenteType anagrafica = fattura.getFatturaElettronicaHeader()
				.getCedentePrestatore()
				.getDatiAnagrafici();
		// mbp.set_ValueOfColumn("LIT_TaxId", codice);
		mbp.setTaxID(piva);
		mbp.set_ValueOfColumn("LIT_NationalIdNumber",
				!Utils.isBlank(anagrafica.getCodiceFiscale()) ? anagrafica.getCodiceFiscale() : piva);

		if (anagrafica.getAnagrafica().getDenominazione() != null)
			mbp.setName(anagrafica.getAnagrafica().getDenominazione());
		else {
			mbp.setName(anagrafica.getAnagrafica().getNome() + " " + anagrafica.getAnagrafica().getCognome());
		}

		mbp.setIsVendor(true);
		mbp.setIsCustomer(false);
		Integer paymentTermId = new Query(Env.getCtx(), MPaymentTerm.Table_Name, "isdefault = 'Y' and isactive='Y'",
				null).setClient_ID().firstId();
		mbp.setPO_PaymentTerm_ID(paymentTermId);
		mbp.setPaymentRulePO(parsePaymentRule("MP05"));
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
		return mbp;
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

	private MBPartnerLocation getBPLocationFromEinvoice(FatturaElettronicaType fattura) {
		MBPartnerLocation mbpLocation = new MBPartnerLocation(Env.getCtx(), 0, null);
		IndirizzoType sede = fattura.getFatturaElettronicaHeader().getCedentePrestatore().getSede();
		MLocation location = new Query(Env.getCtx(), MLocation.Table_Name,
				"c_location.address1 ILIKE '%' || ? || '%' AND " + " c_location.postal = ? AND co.countrycode = ? AND "
						+ " c_location.city ILIKE '%' || ? || '%'",
				null).addJoinClause("JOIN c_country co on C_Location.c_country_id = co.c_country_id ")
				.addJoinClause("JOIN c_region reg on C_Location.c_region_id = reg.c_region_id ")
				.setParameters(sede.getIndirizzo(), sede.getCAP(), sede.getNazione(), sede.getComune())
				.setClient_ID()
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
					.setParameters(sede.getNazione())
					.first();

			if (country != null) {
				location.setCountry(country);
				MRegion region = new Query(Env.getCtx(), MRegion.Table_Name, "name = ? AND c_country_id = ?", null)
						.setParameters(sede.getProvincia(), country.get_ID())
						.first();
				location.setRegion(region);
			}
			location.saveEx();
		}
		mbpLocation.setName("Sede legale");
		mbpLocation.setC_Location_ID(location.get_ID());
		return mbpLocation;
	}

	private void createBPBankAccount(MBPartner mbp, DettaglioPagamentoType dettaglio, List<MBPBankAccount> mbpas) {
		try {
			if (dettaglio.getIBAN() != null) {
				MBPBankAccount mbpa = mbpas.stream()
						.filter(bpa -> dettaglio.getIBAN().equals(bpa.getIBAN()))
						.findFirst()
						.orElse(null);
				if (mbpa == null) {
					mbpa = new MBPBankAccount(Env.getCtx(), 0, null);
					mbpa.setC_BPartner_ID(mbp.get_ID());
					mbpa.setIsActive(true);
					mbpa.setIsACH(true);
					mbpa.setIBAN(dettaglio.getIBAN());
					if (dettaglio.getABI() != null && dettaglio.getCAB() != null) {
						MBank bank = new Query(Env.getCtx(), MBank.Table_Name, "RoutingNo = ? AND isActive = 'Y'", null)
								.setClient_ID()
								.setParameters(dettaglio.getABI() + dettaglio.getCAB())
								.first();
						if (bank != null)
							mbpa.setC_Bank_ID(bank.get_ID());
					}
					mbpa.saveEx();
				}
			}
		} catch (Exception e) {
			log.warning("Impossibile creare Business Partner Bank Account");
			log.warning(e.getMessage());
		}
	}

	private boolean isBannedDocument(FatturaElettronicaBodyType body) {
		return BANNED_DOCUMENT_TYPES.contains(body.getDatiGenerali().getDatiGeneraliDocumento().getTipoDocumento());
	}

	private Timestamp toTimestamp(XMLGregorianCalendar calendar) {
		return new Timestamp(calendar.toGregorianCalendar().getTimeInMillis());
	}

	private MBPartner findOrCreateBPartner(FatturaElettronicaType fattura) {
		String codice = fattura.getFatturaElettronicaHeader()
				.getCedentePrestatore()
				.getDatiAnagrafici()
				.getIdFiscaleIVA()
				.getIdCodice();

		MBPartner mbp = new Query(Env.getCtx(), MBPartner.Table_Name, "? in (taxID, LIT_NationalIDNumber)", null)
				.setClient_ID()
				.setParameters(codice)
				.first();

		if (mbp != null)
			return mbp;

		mbp = createAndSaveBusinessPartner(fattura, codice);
		publishNewBpMessage(mbp);
		MBPartnerLocation mbpLocation = getBPLocationFromEinvoice(fattura);
		mbpLocation.setC_BPartner_ID(mbp.get_ID());
		mbpLocation.saveEx();
		mbp.setPrimaryC_BPartner_Location_ID(mbpLocation.get_ID());
		mbp.setIsVendor(true);
		mbp.saveEx();

		return mbp;
	}

	private MDocType findDocType(DatiGeneraliDocumentoType dgd) {
		String tipoDocumento = dgd.getTipoDocumento().value();
		MDocType docType = new Query(Env.getCtx(), MDocType.Table_Name, "lit_fepa_doctype = ? and issotrx='N' ", null)
				.setClient_ID()
				.setParameters(tipoDocumento)
				.first();
		if (docType != null)
			return docType;
		return new Query(Env.getCtx(), MDocType.Table_Name, "lit_fepa_doctype = 'TD01' and issotrx='N' ", null)
				.setClient_ID()
				.first();
	}

	private String parsePaymentRule(String id) {
		String modalitàPagamento = "";
		switch (id) {
		case "MP01":
			modalitàPagamento = "Mixed POS Payment";
			break;
		case "MP02":
			modalitàPagamento = "Check";
			break;
		case "MP05":
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
				.setParameters(modalitàPagamento)
				.first();
		return ref.getValue();
	}
}
