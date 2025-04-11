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
package com.metalsistem.credemsftp.factories;

import org.adempiere.base.IProcessFactory;
import org.compiere.process.ProcessCall;
import org.osgi.service.component.annotations.Component;

import com.metalsistem.credemsftp.ToCredemProcess;

@Component(immediate = true, service = IProcessFactory.class,
        property = {"service.ranking:Integer=100"})
public class ToCredemProcessFactory implements IProcessFactory {

    @Override
    public ProcessCall newProcessInstance(String className) {
        // TODO Auto-generated method stub
        if (className.equals(ToCredemProcess.class.getName()))
            return new ToCredemProcess();
        return null;
    }

}
