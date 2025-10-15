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

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

/**
 * @author jtomelleri
 * 
 *         This class handles the process of exporting invoices to Credemtel via
 *         SFTP. It generates CSV files for PO invoices and ZIP files containing
 *         XML for SO invoices. The files are then uploaded to a specified SFTP
 *         server.
 *
 *         <p>
 *         Key functionalities of this class include: - Reading invoice data
 *         from the database. - Generating CSV and ZIP files based on invoice
 *         data. - Uploading generated files to a remote SFTP server. - Marking
 *         invoices as synchronized once processed and uploaded.
 *
 *         <p>
 *         Parameters that can be configured for this process: - SftpAddress:
 *         Address of the SFTP server. - CertificateFingerprint: Fingerprint of
 *         the certificate used for SSH authentication. - Username: Username for
 *         the SFTP server. - Password: Password for the SFTP server. -
 *         SftpPort: Port number for the SFTP server. - SiaCode: Identifier used
 *         for naming the files. - CsvIdentifier: Identifier for CSV file
 *         naming. - ZipIdentifier: Identifier for ZIP file naming. - Path: Path
 *         on the SFTP server where files will be uploaded.
 *
 *         <p>
 *         Note: The process ensures data consistency by marking synchronized
 *         invoices in the database to avoid duplicate processing.
 */
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
			} else if ("CsvIdentifier".equals(name)) {
				csvIdentifier = param.getParameterAsString();
			} else if ("ZipIdentifier".equals(name)) {
				zipIdentifier = param.getParameterAsString();
			} else if ("Path".equals(name)) {
				path = param.getParameterAsString();
				if (!path.endsWith("/")) {
					path += "/";
				}
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
					"isSoTrx='N' AND DocStatus = 'CO' AND isActive='Y' AND DateAcct < CURRENT_DATE - 1"
					+ " and c_invoice_id in (select c_invoice_id from lit_einvoice le where le.lit_mssynccredem = 'N')", null)
					.setClient_ID()
					.list();

			String dataOra = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
			
			StringWriter sw = new StringWriter();
			CSVFormat csvFormat = CSVFormat.DEFAULT.builder().setDelimiter(';').build();
			CSVPrinter csvPrinter = new CSVPrinter(sw, csvFormat);
			int count = 0;

			List<MInvoice> einvs = new ArrayList<>();
			for (MInvoice inv : poInvoices) {
				MBPartner bp = new MBPartner(getCtx(), inv.getC_BPartner_ID(), null);
				MDocType doctype = new MDocType(getCtx(), inv.getDocTypeID(), null);
				ME_Invoice einv = new Query(getCtx(), ME_Invoice.Table_Name, "C_Invoice_ID=?", null).setClient_ID()
						.setParameters(inv.getC_Invoice_ID())
						.first();
				if (isValidRecord(inv, bp, doctype, einv)) {
					einvs.add(inv);
					einv.set_ValueOfColumn("LIT_MsSyncCredem", true);
					einv.setLIT_SendDate(Timestamp.valueOf(LocalDateTime.now()));
					einv.saveEx();
					count++;
				}

			}
			einvs.sort((me1, me2) -> {
				String protocollo1 = me1.get_ValueAsString("VATDocumentNo").split("/")[0];
				String protocollo2 = me2.get_ValueAsString("VATDocumentNo").split("/")[0];
				return protocollo1.compareTo(protocollo2);
			});

			for (MInvoice inv : einvs) {
				MBPartner bp = new MBPartner(getCtx(), inv.getC_BPartner_ID(), null);
				csvPrinter.printRecord("IT" + bp.getTaxID(),
						bp.get_Value("LIT_NationalIdNumber") != null ? bp.get_Value("LIT_NationalIdNumber") : "",
						inv.getDocumentNo(),
						inv.getDateInvoiced().toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
						inv.get_Value("VATDocumentNo"),
						inv.getDateAcct().toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
			}

			csvPrinter.close();
			String csvName = credemId + csvIdentifier + dataOra + ".csv";

			if (count > 0 && path != null) {
				sftp.put(new SftpFile(csvName, sw.toString().getBytes()), path + csvName);
			}

			// XML -> ZIP
			List<ME_Invoice> soEinvoices = new Query(getCtx(), ME_Invoice.Table_Name,
					"LIT_MsSyncCredem='N' AND inv.isActive='Y' AND inv.isSOTrx='Y'", null)
					.addJoinClause("join c_invoice inv on inv.c_invoice_id = lit_einvoice.c_invoice_id")
					.setClient_ID()
					.list();

			if (soEinvoices.size() > 0) {
				ByteArrayOutputStream zipOut = new ByteArrayOutputStream();
				ZipOutputStream zip = new ZipOutputStream(zipOut);
				for (ME_Invoice einv : soEinvoices) {
					byte[] xml = einv.getBinaryData();
					String name = einv.getDocumentNo().replaceAll("/", "-");
					ZipEntry e = new ZipEntry(name + ".xml");
					zip.putNextEntry(e);
					zip.write(xml);
					zip.closeEntry();
					einv.set_ValueOfColumn("LIT_MsSyncCredem", true);
					einv.setLIT_SendDate(Timestamp.valueOf(LocalDateTime.now()));
					einv.saveEx();
				}
				zip.close();
				String zipName = credemId + zipIdentifier + dataOra + ".zip";
				if (path != null)
					sftp.put(new SftpFile(zipName, zipOut.toByteArray()), path + zipName);
				zipOut.close();
			}

			sftp.close();
			ssh.close();
			return "OK";
		}

	}

	private boolean isValidRecord(MInvoice inv, MBPartner bp, MDocType doctype, ME_Invoice einv) {
		return einv != null && !einv.get_ValueAsBoolean("LIT_MsSyncCredem")
				&& doctype.get_Value("LIT_FEPA_DOCTYPE") != null && bp.getTaxID() != null && inv.getDocumentNo() != null
				&& inv.getDateInvoiced() != null && inv.get_Value("VATDocumentNo") != null && inv.getDateAcct() != null;
	}

}
