package com.metalsistem.credemsftp.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.compiere.model.MAttachmentEntry;
import org.compiere.model.MInvoice;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.kernel.pdf.PdfAConformanceLevel;
import com.itextpdf.kernel.pdf.PdfOutputIntent;
import com.itextpdf.kernel.pdf.PdfString;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.pdfa.PdfADocument;

public class PdfUtils {
    public byte[] create(byte[] xml, MInvoice inv, boolean addFooter, MAttachmentEntry pdfStyle)
            throws Exception {

        Source xsl = null;
        if (pdfStyle == null)
            xsl = new StreamSource(PdfUtils.class.getResourceAsStream("stilefattura.xsl"));
        else
            xsl = new StreamSource(pdfStyle.getInputStream());

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

        PdfADocument pdf = new PdfADocument(pdf_writer, PdfAConformanceLevel.PDF_A_3A,
                new PdfOutputIntent("Custom", "", "http://www.color.org", "sRGB IEC61966-2.1",
                        getClass().getResourceAsStream("sRGB_CS_profile.icm")));

        pdf.getCatalog().setLang(new PdfString("it"));
        pdf.setTagged();

        HtmlConverter.convertToPdf(new ByteArrayInputStream(html), pdf);
        byte[] pdfBytes = baos.toByteArray();
        baos.close();

        return pdfBytes;

    }
}
