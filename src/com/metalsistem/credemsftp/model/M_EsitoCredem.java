package com.metalsistem.credemsftp.model;

import java.sql.ResultSet;
import java.util.Properties;

public class M_EsitoCredem extends X_LIT_MsEsitoCredem {

    private static final long serialVersionUID = 5654475698840234312L;

    public M_EsitoCredem(Properties ctx, int record_ID, String trxName) {
        super(ctx, record_ID, trxName);
    }

    public M_EsitoCredem(Properties ctx, ResultSet rs, String trxName) {
        super(ctx, rs, trxName);
    }
}
