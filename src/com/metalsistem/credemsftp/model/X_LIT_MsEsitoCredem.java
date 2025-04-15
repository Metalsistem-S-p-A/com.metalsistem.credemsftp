/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2012 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
/** Generated Model - DO NOT CHANGE */
package com.metalsistem.credemsftp.model;

import java.sql.ResultSet;
import java.util.Properties;
import org.compiere.model.*;
import org.compiere.util.KeyNamePair;

/** Generated Model for LIT_MsEsitoCredem
 *  @author iDempiere (generated)
 *  @version Release 11 - $Id$ */
@org.adempiere.base.Model(table="LIT_MsEsitoCredem")
public class X_LIT_MsEsitoCredem extends PO implements I_LIT_MsEsitoCredem, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20250415L;

    /** Standard Constructor */
    public X_LIT_MsEsitoCredem (Properties ctx, int LIT_MsEsitoCredem_ID, String trxName)
    {
      super (ctx, LIT_MsEsitoCredem_ID, trxName);
      /** if (LIT_MsEsitoCredem_ID == 0)
        {
			setDocumentNo (null);
			setLIT_MsEsitoCredem_ID (0);
			setName (null);
        } */
    }

    /** Standard Constructor */
    public X_LIT_MsEsitoCredem (Properties ctx, int LIT_MsEsitoCredem_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, LIT_MsEsitoCredem_ID, trxName, virtualColumns);
      /** if (LIT_MsEsitoCredem_ID == 0)
        {
			setDocumentNo (null);
			setLIT_MsEsitoCredem_ID (0);
			setName (null);
        } */
    }

    /** Standard Constructor */
    public X_LIT_MsEsitoCredem (Properties ctx, String LIT_MsEsitoCredem_UU, String trxName)
    {
      super (ctx, LIT_MsEsitoCredem_UU, trxName);
      /** if (LIT_MsEsitoCredem_UU == null)
        {
			setDocumentNo (null);
			setLIT_MsEsitoCredem_ID (0);
			setName (null);
        } */
    }

    /** Standard Constructor */
    public X_LIT_MsEsitoCredem (Properties ctx, String LIT_MsEsitoCredem_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, LIT_MsEsitoCredem_UU, trxName, virtualColumns);
      /** if (LIT_MsEsitoCredem_UU == null)
        {
			setDocumentNo (null);
			setLIT_MsEsitoCredem_ID (0);
			setName (null);
        } */
    }

    /** Load Constructor */
    public X_LIT_MsEsitoCredem (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 3 - Client - Org
      */
    protected int get_AccessLevel()
    {
      return accessLevel.intValue();
    }

    /** Load Meta Data */
    protected POInfo initPO (Properties ctx)
    {
      POInfo poi = POInfo.getPOInfo (ctx, Table_ID, get_TrxName());
      return poi;
    }

    public String toString()
    {
      StringBuilder sb = new StringBuilder ("X_LIT_MsEsitoCredem[")
        .append(get_ID()).append(",Name=").append(getName()).append("]");
      return sb.toString();
    }

	/** Set Description.
		@param Description Optional short description of the record
	*/
	public void setDescription (String Description)
	{
		set_Value (COLUMNNAME_Description, Description);
	}

	/** Get Description.
		@return Optional short description of the record
	  */
	public String getDescription()
	{
		return (String)get_Value(COLUMNNAME_Description);
	}

	/** Set Document No.
		@param DocumentNo Document sequence number of the document
	*/
	public void setDocumentNo (String DocumentNo)
	{
		set_Value (COLUMNNAME_DocumentNo, DocumentNo);
	}

	/** Get Document No.
		@return Document sequence number of the document
	  */
	public String getDocumentNo()
	{
		return (String)get_Value(COLUMNNAME_DocumentNo);
	}

	/** Set Folder Einvoices.
		@param LIT_EInvoice_ID Folder Einvoices
	*/
	public void setLIT_EInvoice_ID (int LIT_EInvoice_ID)
	{
		if (LIT_EInvoice_ID < 1)
			set_ValueNoCheck (COLUMNNAME_LIT_EInvoice_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_LIT_EInvoice_ID, Integer.valueOf(LIT_EInvoice_ID));
	}

	/** Get Folder Einvoices.
		@return Folder Einvoices	  */
	public int getLIT_EInvoice_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_LIT_EInvoice_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set LIT_MsEsitoCredem.
		@param LIT_MsEsitoCredem_ID LIT_MsEsitoCredem
	*/
	public void setLIT_MsEsitoCredem_ID (int LIT_MsEsitoCredem_ID)
	{
		if (LIT_MsEsitoCredem_ID < 1)
			set_ValueNoCheck (COLUMNNAME_LIT_MsEsitoCredem_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_LIT_MsEsitoCredem_ID, Integer.valueOf(LIT_MsEsitoCredem_ID));
	}

	/** Get LIT_MsEsitoCredem.
		@return LIT_MsEsitoCredem	  */
	public int getLIT_MsEsitoCredem_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_LIT_MsEsitoCredem_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set LIT_MsEsitoCredem_UU.
		@param LIT_MsEsitoCredem_UU LIT_MsEsitoCredem_UU
	*/
	public void setLIT_MsEsitoCredem_UU (String LIT_MsEsitoCredem_UU)
	{
		set_Value (COLUMNNAME_LIT_MsEsitoCredem_UU, LIT_MsEsitoCredem_UU);
	}

	/** Get LIT_MsEsitoCredem_UU.
		@return LIT_MsEsitoCredem_UU	  */
	public String getLIT_MsEsitoCredem_UU()
	{
		return (String)get_Value(COLUMNNAME_LIT_MsEsitoCredem_UU);
	}

	/** Set LIT_MsTipoEsito.
		@param LIT_MsTipoEsito LIT_MsTipoEsito
	*/
	public void setLIT_MsTipoEsito (int LIT_MsTipoEsito)
	{
		set_Value (COLUMNNAME_LIT_MsTipoEsito, Integer.valueOf(LIT_MsTipoEsito));
	}

	/** Get LIT_MsTipoEsito.
		@return LIT_MsTipoEsito	  */
	public int getLIT_MsTipoEsito()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_LIT_MsTipoEsito);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set LIT_MsYearInvoiced.
		@param LIT_MsYearInvoiced LIT_MsYearInvoiced
	*/
	public void setLIT_MsYearInvoiced (int LIT_MsYearInvoiced)
	{
		set_Value (COLUMNNAME_LIT_MsYearInvoiced, Integer.valueOf(LIT_MsYearInvoiced));
	}

	/** Get LIT_MsYearInvoiced.
		@return LIT_MsYearInvoiced	  */
	public int getLIT_MsYearInvoiced()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_LIT_MsYearInvoiced);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Name.
		@param Name Alphanumeric identifier of the entity
	*/
	public void setName (String Name)
	{
		set_Value (COLUMNNAME_Name, Name);
	}

	/** Get Name.
		@return Alphanumeric identifier of the entity
	  */
	public String getName()
	{
		return (String)get_Value(COLUMNNAME_Name);
	}

    /** Get Record ID/ColumnName
        @return ID/ColumnName pair
      */
    public KeyNamePair getKeyNamePair()
    {
        return new KeyNamePair(get_ID(), getName());
    }
}