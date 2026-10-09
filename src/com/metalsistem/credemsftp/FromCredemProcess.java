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

import java.io.FileOutputStream;
import java.sql.Savepoint;
import java.util.List;
import java.util.Objects;

import org.adempiere.base.annotation.Process;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MAttachment;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;
import org.compiere.util.Trx;

import com.metalsistem.credemsftp.model.M_EsitoCredem;
import com.metalsistem.credemsftp.model.M_PendingInvoices;
import com.metalsistem.credemsftp.utils.InvoiceParser;
import com.metalsistem.credemsftp.utils.InvoiceReceived;
import com.metalsistem.credemsftp.utils.InvoiceService;
import com.metalsistem.credemsftp.utils.Utils;

import it.cnet.idempiere.LIT_E_Invoice.model.ME_Invoice;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

/**
 * @author jtomelleri This class handles the process of importing invoices from
 *         Credemtel via SFTP. It retrieves XML or P7M files from a remote SFTP
 *         server, processes the invoice data, and saves it into iDempiere's
 *         database.
 *
 *         <p>
 *         Key functionalities of this process include: - Connecting to an SFTP
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

	private static final String UNKNOWN_PARSE_ERROR = "Errore sconosciuto durante la lettura della fattura";

	private final String backupPath = "/mnt/idempierefs/MsBackupCredem/";

	private String sftpAddress;
	private String certificateFingerprint;
	private String userName, password;
	private String credemId;
	private String path;

	private Integer importedInvoices = 0;
	private Integer importedPendingInvoices = 0;
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
		String trxName = get_TrxName();
		Trx trx = Trx.get(trxName, false);
		if (Env.getAD_Org_ID(getCtx()) <= 0)
			return Utils.getMessage("LIT_MsErrorOrgNotSelected");

		retryPendingInvoices();

		try (final SSHClient ssh = new SSHClient()) {
			if ("test".equals(certificateFingerprint)) {
				ssh.addHostKeyVerifier(new PromiscuousVerifier());
			} else {
				ssh.addHostKeyVerifier(certificateFingerprint);
			}
			ssh.connect(sftpAddress, port);
			ssh.authPassword(userName, password.toCharArray());
			try (final SFTPClient sftp = ssh.newSFTPClient()) {
				List<RemoteResourceInfo> filelist = sftp.ls(path, (RemoteResourceInfo resource) -> {
					final String filename = resource.getName();
					return filename.toLowerCase().endsWith(".xml") || filename.toLowerCase().endsWith(".p7m");
				});

				for (RemoteResourceInfo entry : filelist) {
					String[] parts = entry.getName().split("\\.");
					if (credemId.equals(parts[1])) {
						existingInvoices++;
						InvoiceReceived inv = null;
						byte[] xml = null;
						try {
							xml = invoiceParser.getXml(entry, sftp);
						} catch (Exception e) {
							log.warning("Error parsing XML: " + e.getMessage());
							log.warning("Skipping invoice... ");
							addLog("Error parsing XML: " + e.getMessage());
							continue;
						}
						inv = invoiceParser.getInvoiceFromXml(xml);
						String errorMsg = Objects.requireNonNullElse(inv.getErrorMsg(), UNKNOWN_PARSE_ERROR);
						switch (errorMsg) {
						case "" -> {
							Savepoint savepoint = trx.setSavepoint(null);
							try {
								inv = invoiceService.saveInvoice(inv, trxName);
								if (inv.get_ID() > 0) {
									importedInvoices++;
									invoiceService.archiveEInvoice(xml, inv, trxName);
									sftp.rm(entry.getPath());
								}
								trx.releaseSavepoint(savepoint);
							} catch (Exception e) {
								log.warning("Fattura non importata, errore durante il salvataggio");
								e.printStackTrace();
								trx.rollback(savepoint);
								invoiceService.backupXml(entry, inv, xml, e, trxName);
								sftp.rm(entry.getPath());
							}
						}
						case InvoiceParser.FATTURA_DUPLICATA -> {
							log.warning(InvoiceParser.FATTURA_DUPLICATA);
							addLog("Fattura " + entry.getName() + " gia' presente nel sistema ");
							sftp.rm(entry.getPath());
						}
						case InvoiceParser.FATTURA_SCARTATA -> {
							log.warning(InvoiceParser.FATTURA_SCARTATA);
							addLog("Fattura " + entry.getName() + " e' un'autofattura con tipo documento "
									+ inv.getTipoDocumento());
							sftp.rm(entry.getPath());
						}
						default -> {
							invoiceService.backupXml(entry, inv, xml, errorMsg, trxName);
							sftp.rm(entry.getPath());
						}
						}
					} else if (parts[0].length() >= 16 && credemId.contains(parts[0].substring(10, 15))) {
						log.warning("Elaboro esito");
						byte[] xml = invoiceParser.getXml(entry, sftp);
						try {
							List<M_EsitoCredem> esiti = invoiceParser.getDatiEsito(xml);
							for (M_EsitoCredem esito : esiti) {
								ME_Invoice einv = new Query(getCtx(), ME_Invoice.Table_Name,
										"LIT_MsSyncCredem='Y' AND inv.VATDocumentNo = ?  AND inv.isSOTrx='Y' ", trxName)
										.setParameters(esito.getDocumentNo())
										.addJoinClause(
												"join c_invoice inv on inv.c_invoice_id = lit_einvoice.c_invoice_id")
										.setClient_ID().first();
								if (einv != null) {
									esito.setLIT_EInvoice_ID(einv.get_ID());
									esito.saveEx(trxName);
								}
							}
						} catch (Exception e) {
							String name = entry.getName();
							FileOutputStream output = new FileOutputStream(backupPath.concat(name));
							output.write(xml);
							output.close();
						}
						sftp.rm(entry.getPath());
					}
				}
				sftp.close();
				ssh.close();
			} catch (Exception e) {
				InvoiceService
						.sendEmailToAdmin(
								"Org: " + Env.getAD_Org_ID(getCtx()) + " Client: " + Env.getAD_Client_ID(getCtx())
										+ "\n\n" + e.getMessage(),
								"CREDEMSFTP: Errore non gestito in fase di ricezione");
				log.warning("ERRORE NON GESTITO Ricezione: ");
				e.printStackTrace();
				throw new AdempiereException(e);
			}
		} catch (Exception e) {
			log.warning("Errore connessione ssh: " + e.getLocalizedMessage());
			e.printStackTrace();
			throw new AdempiereException(e);
		}
		return Utils.getMessage("LIT_MsInfoImportInvResult", importedInvoices, (existingInvoices - importedInvoices));
	}

	private void retryPendingInvoices() {
		List<M_PendingInvoices> pendingInvoices = new Query(getCtx(), M_PendingInvoices.Table_Name,
				"IsActive = 'Y' AND AD_Org_ID = ?", get_TrxName()).setParameters(Env.getAD_Org_ID(getCtx()))
				.setClient_ID().setOrderBy(M_PendingInvoices.COLUMNNAME_Created).list();

		for (M_PendingInvoices pending : pendingInvoices) {
			MAttachment attachment = pending.getAttachment();
			if (attachment == null || attachment.getEntryCount() == 0) {
				log.warning("Fattura sospesa senza allegato: " + pending.getName());
				continue;
			}

			String trxName = Trx.createTrxName("CredemPending");
			Trx trx = Trx.get(trxName, true);
			try {
				byte[] xml = invoiceParser.getXml(attachment.getEntry(0).getData());
				InvoiceReceived inv = invoiceParser.getInvoiceFromXml(xml);
				String errorMsg = Objects.requireNonNullElse(inv.getErrorMsg(), UNKNOWN_PARSE_ERROR);
				M_PendingInvoices pendingTrx = new M_PendingInvoices(getCtx(), pending.get_ID(), trxName);
				switch (errorMsg) {
				case "" -> {
					inv = invoiceService.saveInvoice(inv, trxName);
					if (inv.get_ID() > 0) {
						invoiceService.archiveEInvoice(xml, inv, trxName);
						pendingTrx.deleteEx(true, trxName);
						trx.commit(true);
						importedPendingInvoices++;
						addLog("Fattura sospesa " + pending.getName() + " importata");
					} else {
						trx.rollback();
					}
				}
				case InvoiceParser.FATTURA_DUPLICATA, InvoiceParser.FATTURA_SCARTATA -> {
					pendingTrx.deleteEx(true, trxName);
					trx.commit(true);
					addLog("Fattura sospesa " + pending.getName() + " rimossa: " + errorMsg);
				}
				default -> {
					trx.rollback();
					updatePendingDescription(pending, errorMsg);
				}
				}
			} catch (Exception e) {
				trx.rollback();
				log.warning("Fattura sospesa " + pending.getName() + " non importata: " + e.getMessage());
				e.printStackTrace();
				updatePendingDescription(pending, e.getMessage());
			} finally {
				InvoiceParser.setIsNewBP(false);
				trx.close();
			}
		}
		if (!pendingInvoices.isEmpty())
			addLog("Fatture sospese importate: " + importedPendingInvoices + "/" + pendingInvoices.size());
	}

	private void updatePendingDescription(M_PendingInvoices pending, String err) {
		if (err == null || err.equals(pending.getDescription()))
			return;
		pending.setDescription(err);
		pending.saveEx(get_TrxName());
	}

}
