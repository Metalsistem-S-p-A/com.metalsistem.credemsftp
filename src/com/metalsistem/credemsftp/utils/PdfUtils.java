package com.metalsistem.credemsftp.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.compiere.model.MInvoice;

import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.io.font.FontProgramFactory;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.font.PdfFontFactory.EmbeddingStrategy;
import com.itextpdf.kernel.pdf.PdfAConformanceLevel;
import com.itextpdf.kernel.pdf.PdfOutputIntent;
import com.itextpdf.kernel.pdf.PdfString;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.pdfa.PdfADocument;

public class PdfUtils {
	public byte[] create(byte[] xml, MInvoice inv, boolean addFooter) throws Exception {

		Source xsl = new StreamSource(PdfUtils.class.getResourceAsStream("stilefattura.xsl"));

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Writer w = new OutputStreamWriter(baos);
		TransformerFactory factory = TransformerFactory.newInstance();
		Transformer transformer = factory.newTransformer(xsl);
		transformer.transform(new StreamSource(new ByteArrayInputStream(xml)), new StreamResult(w));
		w.flush();
		w.close();

		byte[] html = baos.toByteArray();
		baos.close();

		baos = new ByteArrayOutputStream();
		PdfWriter pdf_writer = new PdfWriter(baos);

		PdfADocument pdf = new PdfADocument(pdf_writer, PdfAConformanceLevel.PDF_A_3A, new PdfOutputIntent("Custom", "",
				"http://www.color.org", "sRGB IEC61966-2.1", getClass().getResourceAsStream("sRGB_CS_profile.icm")));

		pdf.getCatalog().setLang(new PdfString("it"));

		FontProgramFactory.registerFont(getClass().getResource("helvetica-regular.otf").getPath(), "helvetica");
		PdfFont font = PdfFontFactory.createRegisteredFont("helvetica", PdfEncodings.IDENTITY_H,
				EmbeddingStrategy.FORCE_EMBEDDED);

		Table table = null;
		if (addFooter) {
			table = new Table(new float[] { 40, 110, 40, 105, 105, 70, 55 });

			table.addCell(new Cell()
					.add(new Paragraph("FORN.").setFont(font).setFontSize(10).setTextAlignment(TextAlignment.CENTER)));
			table.addCell(new Cell()
					.add(new Paragraph("CONTO").setFont(font).setFontSize(10).setTextAlignment(TextAlignment.CENTER)));
			table.addCell(new Cell()
					.add(new Paragraph("PAG.").setFont(font).setFontSize(10).setTextAlignment(TextAlignment.CENTER)));
			table.addCell(new Cell().add(
					new Paragraph("V. CONTR.").setFont(font).setFontSize(10).setTextAlignment(TextAlignment.CENTER)));
			table.addCell(new Cell().add(
					new Paragraph("V. PAG.").setFont(font).setFontSize(10).setTextAlignment(TextAlignment.CENTER)));
			table.addCell(new Cell().add(
					new Paragraph("DATA REG.").setFont(font).setFontSize(10).setTextAlignment(TextAlignment.CENTER)));
			table.addCell(new Cell().add(
					new Paragraph("N. PROT.").setFont(font).setFontSize(10).setTextAlignment(TextAlignment.CENTER)));

			table.addCell(new Cell().setMinHeight(UnitValue.createPointValue(60))
					.add(new Paragraph(inv.getDocumentNo()).setFont(font)
							.setFontSize(10)
							.setTextAlignment(TextAlignment.CENTER)));
		}
		LocalDate dataFatt = LocalDate
				.from(DateTimeFormatter.ofPattern("yyyy-MM-dd").parse(inv.getDateInvoiced().toString().split(" ")[0]));
		HeaderHandler headerHandler = new HeaderHandler(inv.getDocumentNo(),
				dataFatt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")).toString(), inv.getC_BPartner().getValue(),
				inv.getC_BPartner().getName(), table);
		pdf.addEventHandler(PdfDocumentEvent.START_PAGE, headerHandler);
		pdf.setTagged();

		HtmlConverter.convertToPdf(new ByteArrayInputStream(html), pdf);
		byte[] pdfBytes = baos.toByteArray();
		baos.close();

		return pdfBytes;

	}
}
