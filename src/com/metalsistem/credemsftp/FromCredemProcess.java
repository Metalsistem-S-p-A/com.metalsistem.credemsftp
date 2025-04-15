/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2012 Trek Global                							  *
 * Copyright (C) 2012 Carlos Ruiz                							  *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/
package com.metalsistem.credemsftp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
import org.adempiere.base.annotation.Process;
import org.adempiere.model.MBroadcastMessage;
import org.bouncycastle.cms.CMSProcessable;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.util.encoders.Hex;
import org.compiere.model.MAttachment;
import org.compiere.model.MAttachmentEntry;
import org.compiere.model.MBPartner;
import org.compiere.model.MBPartnerLocation;
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
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.idempiere.broadcast.BroadcastMsgUtil;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import com.metalsistem.credemsftp.model.M_EsitoCredem;
import com.metalsistem.credemsftp.utils.InvoiceReceived;
import com.metalsistem.credemsftp.utils.PdfUtils;
import com.metalsistem.credemsftp.utils.Utils;
import it.cnet.idempiere.LIT_E_Invoice.model.ME_Invoice;
import it.cnet.idempiere.LIT_E_Invoice.modelXML2.AllegatiType;
import it.cnet.idempiere.LIT_E_Invoice.modelXML2.CondizioniPagamentoType;
import it.cnet.idempiere.LIT_E_Invoice.modelXML2.DatiGeneraliDocumentoType;
import it.cnet.idempiere.LIT_E_Invoice.modelXML2.DatiPagamentoType;
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
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.RemoteFile;
import net.schmizz.sshj.sftp.RemoteResourceFilter;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

/**
 * @author jtomelleri
 * This class handles the process of importing invoices from Credemtel via SFTP.
 * It retrieves XML or P7M files from a remote SFTP server, processes the invoice data,
 * and saves it into iDempiere's database.
 *
 * <p>Key functionalities of this class include:
 * - Connecting to an SFTP server to download invoice files.
 * - Decoding and parsing XML invoices.
 * - Creating or updating business partners and invoices in the system.
 * - Storing attachments and handling payment schedules.
 *
 * <p>Parameters that can be configured for this process:
 * - SftpAddress: Address of the SFTP server.
 * - CertificateFingerprint: Fingerprint of the certificate used for SSH authentication.
 * - Username: Username for the SFTP server.
 * - Password: Password for the SFTP server.
 * - SftpPort: Port number for the SFTP server.
 * - SiaCode: Identifier used for validating file names.
 * - Path: Path on the SFTP server where files are located.
 *
 * <p>Note: The process ensures data consistency by avoiding duplicate entries
 * and notifying relevant users when new business partners are created.
 */
@Process
public class FromCredemProcess extends SvrProcess {
    private String sftpAddress;
    private String certificateFingerprint;
    private String userName, password;
    private String credemId;
    private String path;

    private Integer importedInvoices = 0;
    private Integer existingInvoices = 0;
    private Integer port;

    private static final CLogger log = CLogger.getCLogger(FromCredemProcess.class);

    @Override
    protected void prepare() {
        ProcessInfoParameter[] params = getParameter();
        for (ProcessInfoParameter param : params) {
            String name = param.getParameterName();
            if ("SftpAddress".equals(name)) {
                sftpAddress = param.getParameterAsString();
            } else if ("CertificateFingerprint".equals(name)) {
                certificateFingerprint = param.getParameterAsString();
            } else if ("Username".equals(name)) {
                userName = param.getParameterAsString();
            } else if ("Password".equals(name)) {
                password = param.getParameterAsString();
            } else if ("SftpPort".equals(name)) {
                port = param.getParameterAsInt();
            } else if ("SiaCode".equals(name)) {
                credemId = param.getParameterAsString();
            } else if ("Path".equals(name)) {
                path = param.getParameterAsString();
            }
        }
    }

    @Override
    protected String doIt() throws Exception {
        try (final SSHClient ssh = new SSHClient()) {
            if (certificateFingerprint.equals("test")) {
                ssh.addHostKeyVerifier(new PromiscuousVerifier());
            } else {
                ssh.addHostKeyVerifier(certificateFingerprint);
            }
            ssh.connect(sftpAddress, port);
            ssh.authPassword(userName, password.toCharArray());
            final SFTPClient sftp = ssh.newSFTPClient();
            List<RemoteResourceInfo> filelist = sftp.ls(path, new RemoteResourceFilter() {
                @Override
                public boolean accept(RemoteResourceInfo resource) {
                    final String filename = resource.getName();
                    if (filename.toLowerCase().endsWith(".xml")
                            || filename.toLowerCase().endsWith(".p7m"))
                        return true;
                    return false;
                }
            });
            for (RemoteResourceInfo entry : filelist) {
                String[] parts = entry.getName().split("\\.");
                if (credemId.equals(parts[1])) {
                    existingInvoices++;
                    log.warning("Elaboro " + entry.getName());
                    InvoiceReceived inv = null;
                    byte[] xml = getXml(entry, sftp);
                    inv = getInvoiceFromXml(xml, false); // fornitore

                    if (inv != null) {
                        saveInvoice(inv);
                        archiveEInvoice(xml, inv);
                        sftp.rm(entry.getPath());
                    }
                } else if (parts[0].length() >= 16
                        && credemId.contains(parts[0].substring(10, 15))) {
                    // ELABORO ESITO
                    log.info("Trovato esito " + entry.getName());
                    byte[] xml = getXml(entry, sftp);
                    List<M_EsitoCredem> esiti = getDatiEsito(xml);
                    for (M_EsitoCredem esito : esiti) {
                        ME_Invoice einv = new Query(getCtx(), ME_Invoice.Table_Name,
                                "LIT_MsSyncCredem='Y' AND inv.VATDocumentNo = ?  AND inv.isSOTrx='Y' ",
                                null)
                                .setParameters(esito.getDocumentNo())
                                .addJoinClause(
                                        "join c_invoice inv on inv.c_invoice_id = lit_einvoice.c_invoice_id")
                                .setClient_ID()
                                .first();
                        if (einv != null)
                            esito.setLIT_EInvoice_ID(einv.get_ID());
                        esito.saveEx();
                    }
                    // sftp.rm(entry.getPath());
                }
            }
            sftp.close();
            ssh.close();
            return Utils.getMessage("LIT_MsInfoImportInvResult", importedInvoices,
                    (existingInvoices - importedInvoices));
        }
    }



    private List<M_EsitoCredem> getDatiEsito(byte[] xml) throws Exception {
        ArrayList<M_EsitoCredem> res = new ArrayList<>();

        DocumentBuilderFactory xmlFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = xmlFactory.newDocumentBuilder();
        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();

        Document document = builder.parse(new InputSource(new ByteArrayInputStream(xml)));

        NodeList esiti =
                (NodeList) xpath.compile("//ESITO").evaluate(document, XPathConstants.NODESET);
        for (int i = 1; i <= esiti.getLength(); i++) {
            M_EsitoCredem de = new M_EsitoCredem(getCtx(), 0, null);

            de.setDescription((String) xpath.compile("//ESITO[" + i + "]/Descrizione/text()")
                    .evaluate(document, XPathConstants.STRING));
            de.setDocumentNo((String) xpath
                    .compile("//ESITO[" + i + "]/RiferimentoFattura/NumeroFattura/text()")
                    .evaluate(document, XPathConstants.STRING));
            de.setLIT_MsTipoEsito(
                    Integer.valueOf((String) xpath.compile("//ESITO[" + i + "]/TipoEsito/text()")
                            .evaluate(document, XPathConstants.STRING)));
            de.setLIT_MsYearInvoiced(Integer.valueOf((String) xpath
                    .compile("//ESITO[" + i + "]/RiferimentoFattura/AnnoFattura/text()")
                    .evaluate(document, XPathConstants.STRING)));
            de.setName("Esito: " + de.getDocumentNo());
            res.add(de);
        }

        return res;
    }

    private void archiveEInvoice(byte[] xml, MInvoice inv) {
        ME_Invoice einv =
                new Query(getCtx(), ME_Invoice.Table_Name, "Name = ?", null).setClient_ID()
                        .setParameters("FE: " + inv.getDocumentNo())
                        .first();
        if (einv == null) {
            einv = new ME_Invoice(getCtx(), 0, null);
            String noDocFile = inv.getDocumentNo().replaceAll("/", "-");
            einv.setBinaryData(xml);
            einv.setName("FE: " + inv.getDocumentNo());
            einv.setC_DocType_ID(inv.getDocTypeID());
            einv.setC_Invoice_ID(inv.get_ID());
            einv.setFileName("xml-" + noDocFile + ".xml");
            einv.setDateInvoiced(inv.getDateInvoiced());
            einv.set_ValueOfColumn("LIT_MsSyncCredem", false);
            einv.saveEx();

            MAttachment attachment = new MAttachment(getCtx(), 0, null);
            attachment.setRecord_ID(einv.get_ID());
            attachment.setAD_Table_ID(ME_Invoice.Table_ID);
            byte[] pdfBytes = {};
            try {
                PdfUtils utils = new PdfUtils();
                pdfBytes = utils.create(xml, inv, true);
                if (pdfBytes != null) {
                    MAttachmentEntry entry = new MAttachmentEntry("xml-" + noDocFile + ".xml", xml);
                    entry.setName("Fattura-" + noDocFile + ".pdf");
                    entry.setData(pdfBytes);

                    attachment.addEntry(entry);
                    attachment.save(null);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void saveInvoice(InvoiceReceived inv) throws Exception {
        // TODO: Controlla colonne db
        MInvoice res = new Query(getCtx(), MInvoice.Table_Name,
                "DocumentNo = ? and C_BPartner_ID = ? and DateInvoiced = ?", null).setClient_ID()
                .setParameters(inv.getDocumentNo(), inv.getC_BPartner_ID(), inv.getDateInvoiced())
                .first();
        if (res == null) {
            inv.saveEx();
            importedInvoices++;
            log.info("Fattura importata");

            for (MInvoiceLine l : inv.getInvoiceLines()) {
                l.setC_Invoice_ID(inv.get_ID());
                l.saveEx();
            }
            log.info("Linee Fattura importate");

            MAttachment attachment = new MAttachment(getCtx(), 0, null);
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
            log.warning("Fattura già importata");
        }
        return;
    }

    private InvoiceReceived getInvoice(FatturaElettronicaType fattura, boolean fornitore)
            throws Exception {
        InvoiceReceived invoice = new InvoiceReceived(new MInvoice(getCtx(), 0, null));
        invoice.setAD_Org_ID(Env.getAD_Org_ID(getCtx()));

        FatturaElettronicaBodyType firstBody = fattura.getFatturaElettronicaBody().get(0);
        DatiGeneraliDocumentoType datiGeneraliDocumento =
                firstBody.getDatiGenerali().getDatiGeneraliDocumento();

        List<MAttachmentEntry> allegati = new ArrayList<MAttachmentEntry>();
        for (AllegatiType allegato : firstBody.getAllegati()) {
            String nome = allegato.getNomeAttachment();
            byte[] content = allegato.getAttachment();
            allegati.add(new MAttachmentEntry(nome, content));
        }

        invoice.setAttachmentEntries(allegati);

        FatturaElettronicaBodyType body = firstBody;
        XMLGregorianCalendar gregorianDate = datiGeneraliDocumento.getData();

        invoice.setDateInvoiced(
                new Timestamp(gregorianDate.toGregorianCalendar().getTimeInMillis()));
        invoice.setDocumentNo(datiGeneraliDocumento.getNumero());
        invoice.setC_Currency_ID(MCurrency.get(datiGeneraliDocumento.getDivisa()).get_ID());
        // TIPO DOCUMENTO
        MDocType docType = new MDocType(getCtx(), 0, null);
        if (!List
                .of(TipoDocumentoType.TD_04, TipoDocumentoType.TD_16, TipoDocumentoType.TD_17,
                        TipoDocumentoType.TD_18)
                .contains(datiGeneraliDocumento.getTipoDocumento())) {
            invoice.set_ValueOfColumn("LIT_FEPA_DOCTYPE",
                    datiGeneraliDocumento.getTipoDocumento().value());

            docType = new Query(getCtx(), MDocType.Table_Name,
                    "lit_fepa_doctype = ? and issotrx='N' ", null).setClient_ID()
                    .setParameters(datiGeneraliDocumento.getTipoDocumento().value())
                    .first();
            if (docType == null) {
                docType = new Query(getCtx(), MDocType.Table_Name,
                        "lit_fepa_doctype = 'TD01' and issotrx='N' ", null).setClient_ID().first();
            }
            invoice.setC_DocType_ID(docType.get_ID());
            invoice.setC_DocTypeTarget_ID(docType.get_ID());

        } else {
            return null;
        }

        // BUSINESS PARTNER
        String codice = fattura.getFatturaElettronicaHeader()
                .getCedentePrestatore()
                .getDatiAnagrafici()
                .getIdFiscaleIVA()
                .getIdCodice();
        MBPartner mbp =
                new Query(getCtx(), MBPartner.Table_Name, "LIT_TaxId=?", null).setClient_ID()
                        .setParameters(codice)
                        .first();
        invoice.setIsSOTrx(fornitore);
        if (mbp == null || mbp.get_ID() <= 0) {
            mbp = createAndSaveBusinessPartner(fattura, codice);
            publishNewBpMessage(mbp);
            log.warning("BusinessPartner creato");
            MBPartnerLocation mbpLocation = getBPLocationFromEinvoice(fattura);
            mbpLocation.setC_BPartner_ID(mbp.get_ID());
            mbpLocation.saveEx();
            mbp.setPrimaryC_BPartner_Location_ID(mbpLocation.get_ID());
            mbp.saveEx();
        }

        invoice.setBPartner(mbp);
        // LOCATION
        // String invoiceLocation = fattura.getFatturaElettronicaHeader().getCedentePrestatore().getSede().getIndirizzo();
        // MLocation bpLocation = MLocation.getBPLocation(getCtx(), mbp.getPrimaryC_BPartner_Location_ID(), null);
        MBPartnerLocation mbpLocation = mbp.getPrimaryC_BPartner_Location();
        MCountry to = new MCountry(getCtx(), 214, null);
        if (mbpLocation.get_ID() <= 0) {
            mbpLocation = getBPLocationFromEinvoice(fattura);
            mbpLocation.setC_BPartner_ID(mbp.get_ID());
            mbpLocation.saveEx();
            mbp.setPrimaryC_BPartner_Location_ID(mbpLocation.get_ID());
            mbp.saveEx();
        }
        invoice.setC_BPartner_Location_ID(mbp.getPrimaryC_BPartner_Location_ID());
        MCountry from = mbpLocation.getLocation(true).getCountry();

        // REGISTRO IVA
        final MLITVATDocTypeSequence registroIva;
        if (to != null && from != null) {
            registroIva = MLITVATDocTypeSequence.getVATDocTypeSequenceWithCountry(invoice,
                    from.get_ID(), to.get_ID());
            invoice.set_ValueOfColumn("LIT_VATJournal_ID", registroIva.getLIT_VATJournal_ID());
        } else {
            registroIva = null;
        }

        invoice.setGrandTotal(datiGeneraliDocumento.getImportoTotaleDocumento());
        List<MInvoiceLine> linee = new ArrayList<MInvoiceLine>();
        List<MUOM> uoms = new Query(getCtx(), MUOM.Table_Name, "", null).setClient_ID().list();

        // LETTERA D'INTENTO
        MBPLetterIntent letter = new Query(getCtx(), MBPLetterIntent.Table_Name,
                " bp_letterintentdatevalidfrom < Current_date AND bp_letterintentdatevalidto > current_date and c_bpartner_id = ?",
                null).setClient_ID().setParameters(mbp.get_ID()).first();

        if (letter != null
                && body.getDatiBeniServizi().getDatiRiepilogo().get(0).getNatura() != null
                && "N3.5".equals(
                        body.getDatiBeniServizi().getDatiRiepilogo().get(0).getNatura().value())) {
            invoice.set_ValueOfColumn("c_bp_partner_letterintent_id", letter.get_ID());
        }

        // LINEE
        for (DettaglioLineeType linea : body.getDatiBeniServizi().getDettaglioLinee()) {
            MInvoiceLine il = new MInvoiceLine(getCtx(), -1, null);
            il.setInvoice(invoice);
            il.setLine(linea.getNumeroLinea() * 10); // iDempiere Standard
            il.setDescription(linea.getDescrizione());
            il.setName(linea.getDescrizione());
            il.setPrice(linea.getPrezzoUnitario());

            if (mbp.get_ValueAsInt("LIT_M_Product_XML_ID") > 0) {
                MProduct prod =
                        new MProduct(getCtx(), mbp.get_ValueAsInt("LIT_M_Product_XML_ID"), null);
                il.setProduct(prod);
            }
            if (!linea.getCodiceArticolo().isEmpty()) {
                il.set_ValueOfColumn("VendorProductNo",
                        linea.getCodiceArticolo().get(0).getCodiceValore());
            }
            if (linea.getQuantita() != null) {
                il.setQtyEntered(linea.getQuantita());
            } else if (linea.getQuantita() == null && linea.getPrezzoTotale() != null
                    && linea.getPrezzoUnitario() != null) { // Esempio: Extra costi finitura
                il.setQtyEntered(BigDecimal.ONE);
            }
            if (linea.getUnitaMisura() != null) {
                for (MUOM uom : uoms) {
                    if (linea.getUnitaMisura()
                            .toLowerCase()
                            .equals(uom.getUOMSymbol().toLowerCase())
                            || linea.getUnitaMisura()
                                    .toLowerCase()
                                    .equals(uom.get_Translation("Name", "it_IT").toLowerCase())
                            || linea.getUnitaMisura()
                                    .toLowerCase()
                                    .equals(uom.get_Translation("UOMSymbol", "it_IT")
                                            .toLowerCase())) {
                        il.setC_UOM_ID(uom.get_ID());
                        break;
                    }
                }
            }
            MTax invTax = getTax(registroIva, linea);
            il.setC_Tax_ID(invTax.get_ID());
            linee.add(il);
        }
        invoice.setInvoiceLines(linee);

        // TERMINI E MODALITA' PAGAMENTO
        List<DatiPagamentoType> datiPagamento = body.getDatiPagamento();
        if (datiPagamento.size() > 0) {
            DatiPagamentoType datiPagamentoType = datiPagamento.get(0);
            CondizioniPagamentoType cpt = datiPagamentoType.getCondizioniPagamento();
            ModalitaPagamentoType dpt =
                    datiPagamentoType.getDettaglioPagamento().get(0).getModalitaPagamento();

            int pTerm = parsePaymentTerm(cpt.value());
            invoice.setC_PaymentTerm_ID(pTerm != -1 ? pTerm : mbp.getPO_PaymentTerm_ID());
            invoice.setPaymentRule(parsePaymentRule(dpt.value()));
        } else {
            if (mbp.getPO_PaymentTerm() != null) {
                invoice.setC_PaymentTerm_ID(mbp.getPO_PaymentTerm_ID());
            }
            if (mbp.getPaymentRule() != null) {
                invoice.setPaymentRule(mbp.getPaymentRule());
            }
        }

        // SCADENZE
        List<MInvoicePaySchedule> scadenze = new ArrayList<MInvoicePaySchedule>();
        for (DatiPagamentoType pagamento : body.getDatiPagamento()) {
            for (DettaglioPagamentoType dettaglio : pagamento.getDettaglioPagamento()) {
                MInvoicePaySchedule ips = new MInvoicePaySchedule(getCtx(), 0, null);
                ips.setC_Invoice_ID(invoice.get_ID());
                ips.setParent(invoice);

                ips.setParent(invoice);
                ips.setDueAmt(dettaglio.getImportoPagamento());
                ips.setDiscountAmt(dettaglio.getImportoPagamento());

                XMLGregorianCalendar dataScadenza = dettaglio.getDataScadenzaPagamento();
                if (dataScadenza != null) {
                    ips.setDueDate(
                            new Timestamp(dataScadenza.toGregorianCalendar().getTimeInMillis()));
                    ips.setDiscountDate(
                            new Timestamp(dataScadenza.toGregorianCalendar().getTimeInMillis()));
                    scadenze.add(ips);
                }
                // In caso non ci siano le date, queste vengono generate
                // in idempiere al momento del completamento della fattura
            }
        }

        invoice.setScheduledPayments(scadenze);
        invoice.setAD_User_ID(-1);

        return invoice;
    }

    private MTax getTax(final MLITVATDocTypeSequence registroIva, DettaglioLineeType linea) {
        List<MTax> taxes =
                new Query(getCtx(), MTax.Table_Name, "to_country_id = 214 ", null).setClient_ID()
                        .list();
        MTax invTax = null;
        // N3.5 = Lettera d'intento
        if (linea.getNatura() != null && "N3.5".equals(linea.getNatura().value())) {
            // Non imponibile Art 8 c.1
            invTax = new Query(getCtx(), MTax.Table_Name, "value = 'F.02'", null).setClient_ID()
                    .first();
        } else {
            invTax = taxes.stream().filter(tax -> {
                if (tax.getC_CountryGroupFrom() != null && registroIva != null) {
                    return tax.getRate().compareTo(linea.getAliquotaIVA()) == 0
                            && tax.getSOPOType().equals("B")
                            && tax.getC_CountryGroupFrom_ID() == registroIva
                                    .getC_CountryGroupFrom_ID();
                } else {
                    return tax.getRate().compareTo(linea.getAliquotaIVA()) == 0
                            && tax.getSOPOType().equals("B");
                }
            }).findFirst().get();
        }
        return invTax;
    }

    private MBPartnerLocation getBPLocationFromEinvoice(FatturaElettronicaType fattura) {
        MBPartnerLocation mbpLocation = new MBPartnerLocation(getCtx(), 0, null);
        IndirizzoType sede = fattura.getFatturaElettronicaHeader().getCedentePrestatore().getSede();
        MLocation location = new Query(getCtx(), MLocation.Table_Name,
                "c_location.address1 like ? AND c_location.postal = ? AND"
                        + " c_location.city = ? and co.countrycode = ? and reg.name = ?",
                null)
                .addJoinClause("JOIN c_country co on C_Location.c_country_id = co.c_country_id ")
                .addJoinClause("JOIN c_region reg on C_Location.c_region_id = reg.c_region_id ")
                .setParameters(sede.getIndirizzo(), sede.getCAP(), sede.getComune(),
                        sede.getNazione(), sede.getProvincia())
                .first();
        if (location == null) {
            location = new MLocation(getCtx(), 0, null);
            String indirizzo = sede.getIndirizzo();
            if (sede.getNumeroCivico() != null) {
                indirizzo += ", " + sede.getNumeroCivico();
            }
            location.setAddress1(indirizzo);
            location.setPostal(sede.getCAP());
            location.setCity(sede.getComune());
            MCountry country = new Query(getCtx(), MCountry.Table_Name, "countrycode = ?", null)
                    .setParameters(sede.getNazione())
                    .first();

            if (country != null) {
                location.setCountry(country);
                MRegion region =
                        new Query(getCtx(), MRegion.Table_Name, "name = ? AND c_country_id = ?",
                                null).setParameters(sede.getProvincia(), country.get_ID()).first();
                location.setRegion(region);
            }
            location.saveEx();
        }

        mbpLocation.setC_Location_ID(location.get_ID());
        return mbpLocation;
    }

    private MBPartner createAndSaveBusinessPartner(FatturaElettronicaType fattura, String codice) {
        MBPartner mbp;
        mbp = new MBPartner(getCtx(), 0, null);
        mbp.set_ValueOfColumn("LIT_TaxId", codice);
        mbp.setTaxID(codice);
        mbp.set_ValueOfColumn("LIT_NationalIDNumber", codice);
        mbp.setName(fattura.getFatturaElettronicaHeader()
                .getCedentePrestatore()
                .getDatiAnagrafici()
                .getAnagrafica()
                .getDenominazione());
        mbp.setIsVendor(true);
        mbp.setIsCustomer(false);
        mbp.saveEx();
        return mbp;
    }

    private void publishNewBpMessage(MBPartner mbp) {
        MBroadcastMessage msg = new MBroadcastMessage(getCtx(), 0, null);
        MRole role = new Query(getCtx(), MRole.Table_Name,
                "name = 'Amministrazione (responsabili)'", null).setClient_ID().first();
        if (role == null) {
            role = new Query(getCtx(), MRole.Table_Name, "name like 'Amministratore%'", null)
                    .setClient_ID()
                    .first();
        }
        msg.setBroadcastMessage(Utils.getMessage("LIT_MsInfoBPCreated", mbp.getName()));
        msg.setBroadcastType(MBroadcastMessage.BROADCASTTYPE_ImmediatePlusLogin);
        msg.setBroadcastFrequency(
                MBroadcastMessage.BROADCASTFREQUENCY_UntilExpirationOrAcknowledge);
        msg.setTarget(MBroadcastMessage.TARGET_Role);
        msg.setAD_Role_ID(role.get_ID());
        msg.setPublish("Y");
        msg.setExpiration(Timestamp.valueOf(LocalDateTime.now().plusMonths(1)));
        msg.saveEx();
        BroadcastMsgUtil.publishBroadcastMessage(msg.get_ID(), null);
    }

    public InvoiceReceived getInvoiceFromXml(byte[] xml, boolean fornitore) throws Exception {
        // FatturaElettronicaDecoder decoder = new FatturaElettronicaDecoder();
        // String fatturaXML = new String(xml);
        ManageXML_new manageXml = new ManageXML_new();
        FatturaElettronicaType fattura =
                manageXml.importFatturaElettronica(new ByteArrayInputStream(xml));
        return getInvoice(fattura, fornitore);
    }

    public byte[] getXml(RemoteResourceInfo entry, SFTPClient sftp) throws Exception {
        RemoteFile f = sftp.getSFTPEngine().open(entry.getPath());
        InputStream is = f.new RemoteFileInputStream(0);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        is.transferTo(baos);

        byte[] xml = baos.toByteArray();
        baos.close();
        is.close();

        if (entry.getName().toLowerCase().endsWith(".p7m")) {
            CMSSignedData signature = new CMSSignedData(xml);
            CMSProcessable sc = signature.getSignedContent();
            xml = (byte[]) sc.getContent();
        }

        ByteBuffer bb = ByteBuffer.wrap(xml);
        byte[] bom = new byte[3];
        bb.get(bom, 0, bom.length);

        log.info("Recupero informazioni fattura");
        String content = new String(Hex.encode(bom));
        if ("efbbbf".equalsIgnoreCase(content)) {
            xml = new byte[xml.length - 3];
            bb.get(xml, 0, xml.length);
        }
        f.close();
        return xml;
    }

    public String parsePaymentRule(String id) {
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
        MRefList ref = new Query(getCtx(), MRefList.Table_Name,
                "AD_Ref_List.Name like ? AND re.name = '_Payment Rule' ", null)
                .addJoinClause(
                        "JOIN ad_reference re on re.ad_reference_id = AD_Ref_List.ad_reference_id ")
                .setParameters(modalitàPagamento)
                .first();
        return ref.getValue();
    }

    public int parsePaymentTerm(String id) {
        String terminePagamento = "";
        switch (id) {
            case "TP01": // Pagamento a Rate
                terminePagamento = "Vedi Scadenza";
                break;
            case "TP02": // Pagamento completo
                terminePagamento = "30gg d.f.";
                break;
            case "TP03": // Pagamento Anticipato
                terminePagamento = "Anticipato";
                break;
            default:
                terminePagamento = null;
                break;
        }
        MPaymentTerm term = new Query(getCtx(), MPaymentTerm.Table_Name, "Name = ?", null)
                .setParameters(terminePagamento)
                .setClient_ID()
                .first();
        if (term == null || terminePagamento == null)
            return -1;
        return term.get_ID();
    }
}
