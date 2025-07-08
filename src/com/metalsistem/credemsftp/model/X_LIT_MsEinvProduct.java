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

/** Generated Model for LIT_MsEinvProduct
 *  @author iDempiere (generated)
 *  @version Release 11 - $Id$ */
@org.adempiere.base.Model(table="LIT_MsEinvProduct")
public class X_LIT_MsEinvProduct extends PO implements I_LIT_MsEinvProduct, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20250707L;

    /** Standard Constructor */
    public X_LIT_MsEinvProduct (Properties ctx, int LIT_MsEinvProduct_ID, String trxName)
    {
      super (ctx, LIT_MsEinvProduct_ID, trxName);
      /** if (LIT_MsEinvProduct_ID == 0)
        {
			setLIT_MsEinvProduct_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_LIT_MsEinvProduct (Properties ctx, int LIT_MsEinvProduct_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, LIT_MsEinvProduct_ID, trxName, virtualColumns);
      /** if (LIT_MsEinvProduct_ID == 0)
        {
			setLIT_MsEinvProduct_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_LIT_MsEinvProduct (Properties ctx, String LIT_MsEinvProduct_UU, String trxName)
    {
      super (ctx, LIT_MsEinvProduct_UU, trxName);
      /** if (LIT_MsEinvProduct_UU == null)
        {
			setLIT_MsEinvProduct_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_LIT_MsEinvProduct (Properties ctx, String LIT_MsEinvProduct_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, LIT_MsEinvProduct_UU, trxName, virtualColumns);
      /** if (LIT_MsEinvProduct_UU == null)
        {
			setLIT_MsEinvProduct_ID (0);
        } */
    }

    /** Load Constructor */
    public X_LIT_MsEinvProduct (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 7 - System - Client - Org
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
      StringBuilder sb = new StringBuilder ("X_LIT_MsEinvProduct[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public org.compiere.model.I_C_BPartner getC_BPartner() throws RuntimeException
	{
		return (org.compiere.model.I_C_BPartner)MTable.get(getCtx(), org.compiere.model.I_C_BPartner.Table_ID)
			.getPO(getC_BPartner_ID(), get_TrxName());
	}

	/** Set Business Partner.
		@param C_BPartner_ID Identifies a Business Partner
	*/
	public void setC_BPartner_ID (int C_BPartner_ID)
	{
		if (C_BPartner_ID < 1)
			set_ValueNoCheck (COLUMNNAME_C_BPartner_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_C_BPartner_ID, Integer.valueOf(C_BPartner_ID));
	}

	/** Get Business Partner.
		@return Identifies a Business Partner
	  */
	public int getC_BPartner_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_BPartner_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_C_Tax getC_Tax() throws RuntimeException
	{
		return (org.compiere.model.I_C_Tax)MTable.get(getCtx(), org.compiere.model.I_C_Tax.Table_ID)
			.getPO(getC_Tax_ID(), get_TrxName());
	}

	/** Set Tax.
		@param C_Tax_ID Tax identifier
	*/
	public void setC_Tax_ID (int C_Tax_ID)
	{
		if (C_Tax_ID < 1)
			set_ValueNoCheck (COLUMNNAME_C_Tax_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_C_Tax_ID, Integer.valueOf(C_Tax_ID));
	}

	/** Get Tax.
		@return Tax identifier
	  */
	public int getC_Tax_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Tax_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Cassa previdenziale = Cp */
	public static final String LIT_MSEINVPRODTYPE_CassaPrevidenziale = "Cp";
	/** Prodotto standard = Std */
	public static final String LIT_MSEINVPRODTYPE_ProdottoStandard = "Std";
	/** Set EInvoice product type .
		@param LIT_MsEinvProdType EInvoice product type 
	*/
	public void setLIT_MsEinvProdType (String LIT_MsEinvProdType)
	{

		set_Value (COLUMNNAME_LIT_MsEinvProdType, LIT_MsEinvProdType);
	}

	/** Get EInvoice product type .
		@return EInvoice product type 	  */
	public String getLIT_MsEinvProdType()
	{
		return (String)get_Value(COLUMNNAME_LIT_MsEinvProdType);
	}

	/** Set EInvoice Product.
		@param LIT_MsEinvProduct_ID EInvoice Product
	*/
	public void setLIT_MsEinvProduct_ID (int LIT_MsEinvProduct_ID)
	{
		if (LIT_MsEinvProduct_ID < 1)
			set_ValueNoCheck (COLUMNNAME_LIT_MsEinvProduct_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_LIT_MsEinvProduct_ID, Integer.valueOf(LIT_MsEinvProduct_ID));
	}

	/** Get EInvoice Product.
		@return EInvoice Product	  */
	public int getLIT_MsEinvProduct_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_LIT_MsEinvProduct_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set LIT_MsEinvProduct_UU.
		@param LIT_MsEinvProduct_UU LIT_MsEinvProduct_UU
	*/
	public void setLIT_MsEinvProduct_UU (String LIT_MsEinvProduct_UU)
	{
		set_Value (COLUMNNAME_LIT_MsEinvProduct_UU, LIT_MsEinvProduct_UU);
	}

	/** Get LIT_MsEinvProduct_UU.
		@return LIT_MsEinvProduct_UU	  */
	public String getLIT_MsEinvProduct_UU()
	{
		return (String)get_Value(COLUMNNAME_LIT_MsEinvProduct_UU);
	}

	public org.compiere.model.I_M_Product getM_Product() throws RuntimeException
	{
		return (org.compiere.model.I_M_Product)MTable.get(getCtx(), org.compiere.model.I_M_Product.Table_ID)
			.getPO(getM_Product_ID(), get_TrxName());
	}

	/** Set Product.
		@param M_Product_ID Product, Service, Item
	*/
	public void setM_Product_ID (int M_Product_ID)
	{
		if (M_Product_ID < 1)
			set_Value (COLUMNNAME_M_Product_ID, null);
		else
			set_Value (COLUMNNAME_M_Product_ID, Integer.valueOf(M_Product_ID));
	}

	/** Get Product.
		@return Product, Service, Item
	  */
	public int getM_Product_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_Product_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}
}