package com.metalsistem.credemsftp;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.adempiere.base.annotation.Process;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.compiere.model.MBPartner;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;

import com.metalsistem.credemsftp.utils.SftpFile;

import it.cnet.idempiere.LIT_E_Invoice.model.ME_Invoice;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

@Process
public class ToCredemProcess extends SvrProcess {

	private String sftpAddress;
	private String certificateFingerprint;
	private String userName, password;
	private String credemId;
	private String csvIdentifier;
	private String zipIdentifier;
	private String path;

	private Integer port;

//	private final String[] HEADERS = { "partita_iva", "codice_fiscale", "numero_fattura", "data_fattura",
//			"protocollo/registro", "data_registrazione" };

	@Override
	protected void prepare() {
		// TODO Auto-generated method stub
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
			} else if ("CsvIdentifier".equals(name)) {
				csvIdentifier = param.getParameterAsString();
			} else if ("ZipIdentifier".equals(name)) {
				zipIdentifier = param.getParameterAsString();
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

			// CSV
			List<MInvoice> poInvoices = new Query(getCtx(), MInvoice.Table_Name,
					"isSoTrx='N' AND DocStatus = 'CO' AND isActive='Y'", null).setClient_ID().list();

			String dataOra = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));

			StringWriter sw = new StringWriter();
			CSVFormat csvFormat = CSVFormat.DEFAULT.builder().setDelimiter(';').build();
			CSVPrinter csvPrinter = new CSVPrinter(sw, csvFormat);
			int count = 0;

			for (MInvoice inv : poInvoices) {
				MBPartner bp = new MBPartner(getCtx(), inv.getC_BPartner_ID(), null);
				MDocType doctype = new MDocType(getCtx(), inv.getDocTypeID(), null);
				ME_Invoice einv = new Query(getCtx(), ME_Invoice.Table_Name, "C_Invoice_ID=?", null).setClient_ID()
						.setParameters(inv.getC_Invoice_ID()).first();
				if (einv != null && !einv.get_ValueAsBoolean("LIT_MsSyncCredem")
						&& doctype.get_Value("LIT_FEPA_DOCTYPE") != null && bp.getTaxID() != null
						&& bp.get_Value("LIT_NationalIdNumber") != null && inv.getDocumentNo() != null
						&& inv.getDateInvoiced() != null && inv.get_Value("VATDocumentNo") != null
						&& inv.getDateAcct() != null) {

					csvPrinter.printRecord("IT" + bp.getTaxID(), bp.get_Value("LIT_NationalIdNumber"),
							inv.getDocumentNo(),
							inv.getDateInvoiced().toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
							inv.get_Value("VATDocumentNo"),
							inv.getDateAcct().toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

					einv.set_ValueOfColumn("LIT_MsSyncCredem", true);
					einv.setLIT_SendDate(Timestamp.valueOf(LocalDateTime.now()));
					einv.saveEx();
					count++;
				}

			}
			csvPrinter.close();
			String csvName = credemId + csvIdentifier + dataOra + ".csv";

			if (count > 0) {
				sftp.put(new SftpFile(csvName, sw.toString().getBytes()), path + csvName);
			}

			// XML -> ZIP
			List<ME_Invoice> soEinvoices = new Query(getCtx(), ME_Invoice.Table_Name,
					"LIT_MsSyncCredem='N' AND inv.isActive='Y' AND inv.isSOTrx='Y'", null)
					.addJoinClause("join c_invoice inv on inv.c_invoice_id = lit_einvoice.c_invoice_id").setClient_ID()
					.list();

			if (soEinvoices.size() > 0) {
				ByteArrayOutputStream zipOut = new ByteArrayOutputStream();
				ZipOutputStream zip = new ZipOutputStream(zipOut);
				for (ME_Invoice einv : soEinvoices) {
					byte[] xml = einv.getBinaryData();
					ZipEntry e = new ZipEntry(einv.getName() + ".xml");
					zip.putNextEntry(e);
					zip.write(xml);
					zip.closeEntry();
					einv.set_ValueOfColumn("LIT_MsSyncCredem", true);
					einv.setLIT_SendDate(Timestamp.valueOf(LocalDateTime.now()));
					einv.saveEx();
				}
				zip.close();
				String zipName = credemId + zipIdentifier + dataOra + ".zip";
				sftp.put(new SftpFile(zipName, zipOut.toByteArray()), path + zipName);
				zipOut.close();
			}

			sftp.close();
			ssh.close();
			return "OK";
		}

	}

}
