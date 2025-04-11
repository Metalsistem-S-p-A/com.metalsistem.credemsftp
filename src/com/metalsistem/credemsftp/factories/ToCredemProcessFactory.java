package com.metalsistem.credemsftp.factories;

import org.adempiere.base.IProcessFactory;
import org.compiere.process.ProcessCall;
import org.osgi.service.component.annotations.Component;

import com.metalsistem.credemsftp.ToCredemProcess;

@Component(immediate = true, service = IProcessFactory.class, property = { "service.ranking:Integer=100" })
public class ToCredemProcessFactory implements IProcessFactory {

	@Override
	public ProcessCall newProcessInstance(String className) {
		// TODO Auto-generated method stub
		if (className.equals(ToCredemProcess.class.getName()))
			return new ToCredemProcess();
		return null;
	}

}
