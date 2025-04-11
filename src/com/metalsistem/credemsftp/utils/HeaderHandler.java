package com.metalsistem.credemsftp.utils;



import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.font.PdfFontFactory.EmbeddingStrategy;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.element.Table;

public class HeaderHandler implements IEventHandler {
    public String numero, data, codice, ragione;
    private Table table;
    
    public HeaderHandler(String numero, String data, String codice, String ragione, Table table) {
        this.numero = numero;
        this.data = data;
        this.codice = codice;
        this.ragione = ragione;
        this.table = table;
    }
    
    @Override
    public void handleEvent(Event event) {
        PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
        PdfPage page = docEvent.getPage();
        int pageNum = docEvent.getDocument().getPageNumber(page);
        
        try
        {
            PdfFont font = PdfFontFactory.createRegisteredFont("helvetica", PdfEncodings.IDENTITY_H, EmbeddingStrategy.FORCE_EMBEDDED);
            
            PdfCanvas canvas = new PdfCanvas(page);
            canvas.beginText();
            canvas.setFontAndSize(font, 10);
            canvas.beginMarkedContent(PdfName.Artifact);
            
            
            double y = page.getPageSize().getHeight() - 20;
            
            canvas.moveText(20, y);
            canvas.showText(String.format("Pagina %d - Documento %s  del %s - %s [%s]", pageNum, numero, data, ragione, codice));
            
            canvas.endText();
            canvas.stroke();
            
            if(pageNum == 1 && table != null)
            {
            	try(Canvas c = new Canvas(canvas, new Rectangle(36, 20, page.getPageSize().getWidth(), 60)))
            	{
            	    c.add(table);
            	}
            }
            
            canvas.endMarkedContent();
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
    }
}