package com.metalsistem.credemsftp.factories;

import org.adempiere.base.IProcessFactory;
import org.compiere.process.ProcessCall;
import org.osgi.service.component.annotations.Component;

import com.metalsistem.credemsftp.FromCredemProcess;

@Component(immediate = true, service = IProcessFactory.class, property = { "service.ranking:Integer=100" })
public class FromCredemProcessFactory implements IProcessFactory {

	@Override
	public ProcessCall newProcessInstance(String className) {
		// TODO Auto-generated method stub
		if (className.equals(FromCredemProcess.class.getName()))
			return new FromCredemProcess();
		
		return null;
	}
}
