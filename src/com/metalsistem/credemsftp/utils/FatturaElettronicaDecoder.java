package com.metalsistem.credemsftp.utils;

import java.io.StringReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import it.cnet.idempiere.LIT_E_Invoice.modelXML2.FatturaElettronicaType;

public class FatturaElettronicaDecoder {

	public FatturaElettronicaType decode(String xmlContent) throws JAXBException {

		FatturaElettronicaType feType = null;

		JAXBContext context = null;
		Unmarshaller unMarshaller = null;
		JAXBElement<FatturaElettronicaType> element = null;
		ClassLoader clsInit = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(FatturaElettronicaType.class.getClassLoader());
			context = JAXBContext.newInstance("it.cnet.idempiere.LIT_E_Invoice.modelXML2",
					FatturaElettronicaType.class.getClassLoader());
			unMarshaller = context.createUnmarshaller();
			element = (JAXBElement<FatturaElettronicaType>) unMarshaller
					.unmarshal(new StreamSource(new StringReader(xmlContent)), FatturaElettronicaType.class);
			feType = element.getValue();
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			Thread.currentThread().setContextClassLoader(clsInit);
		}

		return feType;
	}
}