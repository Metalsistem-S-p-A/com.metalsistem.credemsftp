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

import java.util.List;

import org.adempiere.base.annotation.Process;

import org.compiere.model.Query;
import org.compiere.process.SvrProcess;
import org.compiere.process.ProcessInfoParameter;

import com.metalsistem.credemsftp.utils.Utils;
import com.metalsistem.credemsftp.model.M_EsitoCredem;
import com.metalsistem.credemsftp.utils.InvoiceParser;
import com.metalsistem.credemsftp.utils.InvoiceService;
import com.metalsistem.credemsftp.utils.InvoiceReceived;

import it.cnet.idempiere.LIT_E_Invoice.model.ME_Invoice;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.RemoteResourceFilter;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

/**
 * @author jtomelleri This class handles the process of importing invoices from
 *         Credemtel via SFTP. It retrieves XML or P7M files from a remote SFTP
 *         server, processes the invoice data, and saves it into iDempiere's
 *         database.
 *
 *         <p>
 *         Key functionalities of this class include: - Connecting to an SFTP
 *         server to download invoice files. - Decoding and parsing XML
 *         invoices. - Creating or updating business partners and invoices in
 *         the system. - Storing attachments and handling payment schedules.
 *
 *         <p>
 *         Parameters that can be configured for this process: - SftpAddress:
 *         Address of the SFTP server. - CertificateFingerprint: Fingerprint of
 *         the certificate used for SSH authentication. - Username: Username for
 *         the SFTP server. - Password: Password for the SFTP server. -
 *         SftpPort: Port number for the SFTP server. - SiaCode: Identifier used
 *         for validating file names. - Path: Path on the SFTP server where
 *         files are located.
 *
 *         <p>
 *         Note: The process ensures data consistency by avoiding duplicate
 *         entries and notifying relevant users when new business partners are
 *         created.
 */
@Process
public class FromCredemProcess extends SvrProcess {
	private final InvoiceParser invoiceParser = new InvoiceParser();
	private final InvoiceService invoiceService = new InvoiceService();
	private String sftpAddress;
	private String certificateFingerprint;
	private String userName, password;
	private String credemId;
	private String path;

	private Integer importedInvoices = 0;
	private Integer existingInvoices = 0;
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
			List<RemoteResourceInfo> filelist = sftp.ls(path, new RemoteResourceFilter() {
				@Override
				public boolean accept(RemoteResourceInfo resource) {
					final String filename = resource.getName();
					if (filename.toLowerCase().endsWith(".xml") || filename.toLowerCase().endsWith(".p7m"))
						return true;
					return false;
				}
			});
			for (RemoteResourceInfo entry : filelist) {
				String[] parts = entry.getName().split("\\.");
				if (credemId.equals(parts[1])) {
					existingInvoices++;
					InvoiceReceived inv = null;
					byte[] xml = invoiceParser.getXml(entry, sftp);
					try {
						inv = invoiceParser.getInvoiceFromXml(xml);
					} catch (Exception e) {
						inv = null;
						log.warning("Impossibile leggere i dati dalla fattura, fattura non importata");
						e.printStackTrace();
					}
					if (inv != null) {
						try {
							inv = invoiceService.saveInvoice(inv);
							if (inv.get_ID() > 0)
								importedInvoices++;
							invoiceService.archiveEInvoice(xml, inv);
							sftp.rm(entry.getPath());
						} catch (Exception e) {
							log.warning("Fattura non importata, errore durante il salvataggio");
							e.printStackTrace();
						}
					}
				} else if (parts[0].length() >= 16 && credemId.contains(parts[0].substring(10, 15))) {
					// ELABORO ESITO
					byte[] xml = invoiceParser.getXml(entry, sftp);
					List<M_EsitoCredem> esiti = invoiceParser.getDatiEsito(xml);
					for (M_EsitoCredem esito : esiti) {
						ME_Invoice einv = new Query(getCtx(), ME_Invoice.Table_Name,
								"LIT_MsSyncCredem='Y' AND inv.VATDocumentNo = ?  AND inv.isSOTrx='Y' ", null)
								.setParameters(esito.getDocumentNo())
								.addJoinClause("join c_invoice inv on inv.c_invoice_id = lit_einvoice.c_invoice_id")
								.setClient_ID()
								.first();
						if (einv != null) {
							esito.setLIT_EInvoice_ID(einv.get_ID());
							esito.saveEx();
						}
					}
					sftp.rm(entry.getPath());
				}
			}
			sftp.close();
			ssh.close();
			return Utils.getMessage("LIT_MsInfoImportInvResult", importedInvoices,
					(existingInvoices - importedInvoices));
		}
	}
}
