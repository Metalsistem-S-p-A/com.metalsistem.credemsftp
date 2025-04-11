<?xml version="1.0"?>
<xsl:stylesheet
    version="1.1"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:b="http://www.fatturapa.gov.it/sdi/fatturapa/v1.1"
    xmlns:c="http://www.fatturapa.gov.it/sdi/fatturapa/v1.0"
    xmlns:a="http://ivaservizi.agenziaentrate.gov.it/docs/xsd/fatture/v1.2"
    xmlns:d="http://ivaservizi.agenziaentrate.gov.it/docs/xsd/fatture/v1.0">
    <xsl:output method="html" />
    <xsl:decimal-format name="euro" decimal-separator="," grouping-separator="."/>
    <xsl:template name="FormatDateIta">
        <xsl:param name="DateTime" />
        <xsl:variable name="year" select="substring($DateTime,1,4)" />
        <xsl:variable name="month" select="substring($DateTime,6,2)" />
        <xsl:variable name="day" select="substring($DateTime,9,2)" />
        <xsl:value-of select="$day" />
        <xsl:value-of select="'-'" />
        <xsl:value-of select="$month" />
        <xsl:value-of select="'-'" />
        <xsl:value-of select="$year" />
    </xsl:template>
    <xsl:template name="FormatIVA">
        <xsl:param name="Natura" />
        <xsl:param name="IVA" />
        <xsl:choose>
            <xsl:when test="$Natura">
                <xsl:value-of select="$Natura" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:if test="$IVA">
                    <xsl:value-of select="format-number($IVA,  '###.###.##0,00', 'euro')" />
                </xsl:if>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template name="AltraDescrizioneLinea">
        <xsl:param name = "textDescrizione" />
        <!-- testo della descrizione -->
        <tr>
            <td ></td>
            <td >
                <div class="tx-xsmall">
                    <xsl:value-of select="$textDescrizione" />
                </div>
            </td>
            <td></td>
            <td></td>
            <td></td>
            <td></td>
            <td></td>
            <td></td>
        </tr>
    </xsl:template>
    
    <xsl:template match="/">
        <html>
            <head>
                <meta http-equiv="X-UA-Compatible" content="IE=edge" />
                <style type="text/css">
                    #fattura-elettronica
                    {
                        max-width: 2480px;
                        padding: 0;
                        margin: 0;
                    }

                    .tbHeader
                    {
                        width: 100%;
                        border: 2px solid black;
                        margin-bottom: 10px;
                        border-radius: 5px;
                    }


                    tr td
                    {
                        vertical-align: top;
                        padding-left: 2px;
                        padding-right: 2px;
                    }

                    .tdHead {
                        width: 50%;
                        border: 1px solid black;
                    }

                    .tableHead
                    {
                        font-size:smaller;
                        width: 100%;
                    }

                    .headerLabel
                    {
                        color:#282828;
                        font-weight:bold;
                    }

                    .headContent
                    {
                        margin-left:10px;
                        margin-bottom:0px
                    }

                    .mt5
                    {
                        margin-top:5px
                    }

                    tr.break {
                        page-break-after: always; 
                    }

                    .ulAllegati
                    {
                        margin-top:0px;
                    }

                    .separa
                    {
                        height:20px;
                    }

                    table.tbFoglio
                    {
                        width: 100%;
                        table-layout: fixed;
                        border-collapse: collapse;
                        word-wrap: break-word; 
                        overflow: hidden;
                        border-bottom: solid 1px #000000;
                    }

                    table.tbFoglio th {
                        padding-left: 5px;
                        padding-right: 5px;
                        border: solid 1px #000000;
                        background-color: LightGrey;
                        font-size:x-small;
                    }
                    
                    .riepiloghi {
                        background-color: LightCyan !important;
                    }

                    table.tbFoglio tbody
                    {
                        border: solid 1px #000000;
                    }

                    table.tbFoglio th .perc
                    {
                        width:  50px;
                    }

                    table.tbFoglio td
                    {
                        font-size:small;
                        border-right: solid 1px #000000;
                        border-left: solid 1px #000000;
                    }

                    table.tbFoglio tr {
                    }

                    .textRight
                    {
                        text-align:right;
                    }

                    .textCenter
                    {
                        text-align:center;
                    }

                    .textPerc
                    {
                        width:50px;
                    }

                    td.Ritenuta
                    {
                        width:50px;
                        text-align:center;
                    }

                    th.title, .title td
                    {
                        width:48%
                    }

                    th.perc {
                        width:50px;
                    }

                    th.perc2 {
                        width:50px;
                        overflow: hidden;
                    }

                    th.data {
                        width:100px;
                    }

                    th.import
                    {
                        width:100px;
                    }

                    td.import
                    {
                        text-align:right;
                    }

                    th.import2
                    {
                        width:60px;
                    }

                    td.import2
                    {
                        text-align:right;
                    }

                    th.ximport
                    {
                        width:70px;
                    }

                    td.ximport
                    {
                        text-align:center;
                    }

                    th.ximport2
                    {
                        width:80px;
                    }

                    td.ximport2
                    {
                        text-align:center;
                    }

                    td.data
                    {
                        text-align:center;
                    }

                    .tx-xsmall {
                        font-size:x-small;
                    }

                    .tx-small {
                        font-size:small;
                    }

                    .import
                    {
                        text-align:right;
                    }
                    
                    .tbPagamento {
                        border: solid 1px #000000;
                        text-align: left;
                    }
                    
                    .tbPagamento tr {
                        line-height: 14px;
                    }
                    
                    .tbPagamento td {
                        border: solid 1px #000000;
                        text-align: left;
                    }

                    .altriDati {
                        padding-bottom: 5px;
                    }

                    .rowDettaglio {
                        background-color: #EFEFEF;
                    }
                    
                    .linee {
                        page-break-before: always;
                    }
                </style>
            </head>
            <body style="font-family: FreeSans">
                <div id="fattura-container">
                    <!--Variabile che contiene il codice destinatario dall'HEADER perchè viene visualizzato nella sezione BODY -->
                    <!--<xsl:variable name="CodiceDestinatario" select="a:FatturaElettronica/FatturaElettronicaHeader/DatiTrasmissione/CodiceDestinatario"/>-->
                    <xsl:variable name="PecDestinatario" select="a:FatturaElettronica/FatturaElettronicaHeader/DatiTrasmissione/PECDestinatario"/>
                    <!--Variabile che contiene il codice destinatario dall'HEADER perchè viene visualizzato nella sezione BODY -->
                    <xsl:variable name="CodiceDestinatario" >
                        <xsl:choose>
                            <xsl:when test="a:FatturaElettronica/FatturaElettronicaHeader/DatiTrasmissione/CodiceDestinatario='0000000'">
                                <xsl:value-of select="a:FatturaElettronica/FatturaElettronicaHeader/DatiTrasmissione/PECDestinatario" />
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="a:FatturaElettronica/FatturaElettronicaHeader/DatiTrasmissione/CodiceDestinatario" />
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:variable>
                    <div id="fattura-elettronica" class="page">
                        <!-- FatturaElettronicaHeader -->
                        <xsl:if test="a:FatturaElettronica/FatturaElettronicaHeader">
                            <table id="tbHeader" class="tbHeader">
                                <tr >
                                    <td class="tdHead">
                                        <table class="tableHead">
                                            <tr>
                                                <td >
                                                    <!--INIZIO CEDENTE PRESTATORE-->
                                                    <div class="headBorder" >
                                                        <label class= "headerLabel">Cedente/prestatore (fornitore) </label>
                                                        <xsl:for-each select="a:FatturaElettronica/FatturaElettronicaHeader/CedentePrestatore">
                                                            <xsl:choose>
                                                                <xsl:when test="DatiAnagrafici">
                                                                    <!--DatiAnagrafici FPA\FPR-->
                                                                    <xsl:for-each select="DatiAnagrafici">
                                                                        <div class="headContent mt5">
                                                                            <xsl:if test="IdFiscaleIVA">
                                                                                Identificativo fiscale ai fini IVA:
                                                                                <span>
                                                                                    <xsl:value-of select="IdFiscaleIVA/IdPaese" />
                                                                                    <xsl:value-of select="IdFiscaleIVA/IdCodice" />
                                                                                </span>
                                                                            </xsl:if>
                                                                        </div>
                                                                        <div class="headContent" >
                                                                            <xsl:if test="CodiceFiscale">
                                                                                Codice fiscale:
                                                                                <span>
                                                                                    <xsl:value-of select="CodiceFiscale" />
                                                                                </span>
                                                                            </xsl:if>
                                                                        </div>
                                                                        <div class="headContent" >
                                                                            <xsl:if test="Anagrafica/Denominazione">
                                                                                Denominazione:
                                                                                <span>
                                                                                    <xsl:value-of select="Anagrafica/Denominazione" />
                                                                                </span>
                                                                            </xsl:if>
                                                                        </div>
                                                                        <div class="headContent" >
                                                                            <xsl:if test="Anagrafica/Nome | Anagrafica/Cognome">
                                                                                Cognome nome:
                                                                                <xsl:if test="Anagrafica/Cognome">
                                                                                    <span>
                                                                                        <xsl:value-of select="Anagrafica/Cognome" />
                                                                                        <xsl:text></xsl:text>
                                                                                    </span>
                                                                                </xsl:if>
                                                                                <xsl:if test="Anagrafica/Nome">
                                                                                    <span>
                                                                                        <xsl:value-of select="Anagrafica/Nome" />
                                                                                    </span>
                                                                                </xsl:if>
                                                                            </xsl:if>
                                                                        </div>
                                                                        <div class="headContent" >
                                                                            <xsl:if test="RegimeFiscale">
                                                                                Regime fiscale:
                                                                                <span>
                                                                                    <xsl:value-of select="RegimeFiscale" />
                                                                                </span>
                                                                                <xsl:variable name="RF">
                                                                                    <xsl:value-of select="RegimeFiscale" />
                                                                                </xsl:variable>
                                                                                <xsl:choose>
                                                                                    <xsl:when test="$RF='RF01'">
                                                                                        (ordinario)
                                                                                    </xsl:when>
                                                                                    <xsl:when test="$RF='RF02'">
                                                                                        (contribuenti minimi)
                                                                                    </xsl:when>
                                                                                    <xsl:when test="$RF='RF03'">
                                                                                        (nuove iniziative produttive)
                                                                                    </xsl:when>
                                                                                    <xsl:when test="$RF='RF04'">
                                                                                        (agricoltura e attività connesse e pesca)
                                                                                    </xsl:when>
                                                                                    <xsl:when test="$RF='RF05'">
                                                                                        (vendita sali e tabacchi)
                                                                                    </xsl:when>
                                                                                    <xsl:when test="$RF='RF06'">
                                                                                        (commercio fiammiferi)
                                                                                    </xsl:when>
                                                                                    <xsl:when test="$RF='RF07'">
                                                                                        (editoria)
                                                                                    </xsl:when>
                                                                                    <xsl:when test="$RF='RF08'">
                                                                                        (gestione servizi telefonia pubblica)
                                                                                    </xsl:when>
                                                                                    <xsl:when test="$RF='RF09'">
                                                                                        (rivendita documenti di trasporto pubblico e di sosta)
                                                                                    </xsl:when>
                                                                                    <xsl:when test="$RF='RF10'">
                                                                                        (intrattenimenti, giochi e altre attività di cui alla tariffa allegata al DPR 640/72)
                                                                                    </xsl:when>
                                                                                    <xsl:when test="$RF='RF11'">
                                                                                        (agenzie viaggi e turismo)
                                                                                    </xsl:when>
                                                                                    <xsl:when test="$RF='RF12'">
                                                                                        (agriturismo)
                                                                                    </xsl:when>
                                                                                    <xsl:when test="$RF='RF13'">
                                                                                        (vendite a domicilio)
                                                                                    </xsl:when>
                                                                                    <xsl:when test="$RF='RF14'">
                                                                                        (rivendita beni usati, oggetti d'arte, d'antiquariato o da collezione)
                                                                                    </xsl:when>
                                                                                    <xsl:when test="$RF='RF15'">
                                                                                        (agenzie di vendite all'asta di oggetti d'arte, antiquariato o da collezione)
                                                                                    </xsl:when>
                                                                                    <xsl:when test="$RF='RF16'">
                                                                                        (IVA per cassa P.A.)
                                                                                    </xsl:when>
                                                                                    <xsl:when test="$RF='RF17'">
                                                                                        (IVA per cassa - art. 32-bis, D.L. 83/2012)
                                                                                    </xsl:when>
                                                                                    <xsl:when test="$RF='RF19'">
                                                                                        (Regime forfettario)
                                                                                    </xsl:when>
                                                                                    <xsl:when test="$RF='RF18'">
                                                                                        (altro)
                                                                                    </xsl:when>
                                                                                    <xsl:when test="$RF=''"></xsl:when>
                                                                                    <xsl:otherwise>
                                                                                        <span>(!!! codice non previsto !!!)</span>
                                                                                    </xsl:otherwise>
                                                                                </xsl:choose>
                                                                            </xsl:if>
                                                                        </div>
                                                                    </xsl:for-each>
                                                                    <xsl:for-each select="Sede">
                                                                        <div class="headContent" >
                                                                            <xsl:if test="Indirizzo">
                                                                                Indirizzo:
                                                                                <span>
                                                                                    <xsl:value-of select="Indirizzo" />
                                                                                    <xsl:text></xsl:text>
                                                                                    <xsl:value-of select="NumeroCivico" />
                                                                                </span>
                                                                            </xsl:if>
                                                                        </div>
                                                                        <div class="headContent" >
                                                                            <span>
                                                                                <xsl:if test="Comune">
                                                                                    Comune:
                                                                                    <span>
                                                                                        <xsl:value-of select="Comune" />
                                                                                    </span>
                                                                                </xsl:if>
                                                                                <xsl:if test="Provincia">
                                                                                    Provincia:
                                                                                    <span>
                                                                                        <xsl:value-of select="Provincia" />
                                                                                    </span>
                                                                                </xsl:if>
                                                                            </span>
                                                                        </div>
                                                                        <div class="headContent" >
                                                                            <span>
                                                                                <xsl:if test="CAP">
                                                                                    Cap:
                                                                                    <span>
                                                                                        <xsl:value-of select="CAP" />
                                                                                    </span>
                                                                                </xsl:if>
                                                                                <xsl:if test="Nazione">
                                                                                    Nazione:
                                                                                    <span>
                                                                                        <xsl:value-of select="Nazione" />
                                                                                    </span>
                                                                                </xsl:if>
                                                                            </span>
                                                                        </div>
                                                                    </xsl:for-each>
                                                                    <div class="headContent" >
                                                                        <xsl:if test="Contatti/Telefono">
                                                                            Telefono:
                                                                            <span>
                                                                                <xsl:value-of select="Contatti/Telefono" />
                                                                            </span>
                                                                        </xsl:if>
                                                                    </div>
                                                                    <div class="headContent" >
                                                                        <xsl:if test="Contatti/Email">
                                                                            Email:
                                                                            <span>
                                                                                <xsl:value-of select="Contatti/Email" />
                                                                            </span>
                                                                        </xsl:if>
                                                                    </div>
                                                                    <div class="headContent" >
                                                                        <xsl:if test="RiferimentoAmministrazione">
                                                                            Riferimento Amministrazione:
                                                                            <span>
                                                                                <xsl:value-of select="RiferimentoAmministrazione" />
                                                                            </span>
                                                                        </xsl:if>
                                                                    </div>
                                                                </xsl:when>
                                                                <xsl:otherwise>
                                                                    <!--Anagrafica FPRS-->
                                                                    <div class="headContent mt5">
                                                                        <xsl:if test="IdFiscaleIVA">
                                                                            Identificativo fiscale ai fini IVA:
                                                                            <span>
                                                                                <xsl:value-of select="IdFiscaleIVA/IdPaese" />
                                                                                <xsl:value-of select="IdFiscaleIVA/IdCodice" />
                                                                            </span>
                                                                        </xsl:if>
                                                                    </div>
                                                                    <div class="headContent" >
                                                                        <xsl:if test="CodiceFiscale">
                                                                            Codice fiscale:
                                                                            <span>
                                                                                <xsl:value-of select="CodiceFiscale" />
                                                                            </span>
                                                                        </xsl:if>
                                                                    </div>
                                                                    <div class="headContent" >
                                                                        <xsl:if test="Nome | Cognome">
                                                                            Cognome nome:
                                                                            <xsl:if test="Cognome">
                                                                                <span>
                                                                                    <xsl:value-of select="Cognome" />
                                                                                    <xsl:text></xsl:text>
                                                                                </span>
                                                                            </xsl:if>
                                                                            <xsl:if test="Nome">
                                                                                <span>
                                                                                    <xsl:value-of select="Nome" />
                                                                                </span>
                                                                            </xsl:if>
                                                                        </xsl:if>
                                                                    </div>
                                                                    <div class="headContent" >
                                                                        <xsl:if test="RegimeFiscale">
                                                                            Regime fiscale:
                                                                            <span>
                                                                                <xsl:value-of select="RegimeFiscale" />
                                                                            </span>
                                                                            <xsl:variable name="RF">
                                                                                <xsl:value-of select="RegimeFiscale" />
                                                                            </xsl:variable>
                                                                            <xsl:choose>
                                                                                <xsl:when test="$RF='RF01'">
                                                                                    (ordinario)
                                                                                </xsl:when>
                                                                                <xsl:when test="$RF='RF02'">
                                                                                    (contribuenti minimi)
                                                                                </xsl:when>
                                                                                <xsl:when test="$RF='RF03'">
                                                                                    (nuove iniziative produttive)
                                                                                </xsl:when>
                                                                                <xsl:when test="$RF='RF04'">
                                                                                    (agricoltura e attività connesse e pesca)
                                                                                </xsl:when>
                                                                                <xsl:when test="$RF='RF05'">
                                                                                    (vendita sali e tabacchi)
                                                                                </xsl:when>
                                                                                <xsl:when test="$RF='RF06'">
                                                                                    (commercio fiammiferi)
                                                                                </xsl:when>
                                                                                <xsl:when test="$RF='RF07'">
                                                                                    (editoria)
                                                                                </xsl:when>
                                                                                <xsl:when test="$RF='RF08'">
                                                                                    (gestione servizi telefonia pubblica)
                                                                                </xsl:when>
                                                                                <xsl:when test="$RF='RF09'">
                                                                                    (rivendita documenti di trasporto pubblico e di sosta)
                                                                                </xsl:when>
                                                                                <xsl:when test="$RF='RF10'">
                                                                                    (intrattenimenti, giochi e altre attività di cui alla tariffa allegata al DPR 640/72)
                                                                                </xsl:when>
                                                                                <xsl:when test="$RF='RF11'">
                                                                                    (agenzie viaggi e turismo)
                                                                                </xsl:when>
                                                                                <xsl:when test="$RF='RF12'">
                                                                                    (agriturismo)
                                                                                </xsl:when>
                                                                                <xsl:when test="$RF='RF13'">
                                                                                    (vendite a domicilio)
                                                                                </xsl:when>
                                                                                <xsl:when test="$RF='RF14'">
                                                                                    (rivendita beni usati, oggetti d'arte, d'antiquariato o da collezione)
                                                                                </xsl:when>
                                                                                <xsl:when test="$RF='RF15'">
                                                                                    (agenzie di vendite all'asta di oggetti d'arte, antiquariato o da collezione)
                                                                                </xsl:when>
                                                                                <xsl:when test="$RF='RF16'">
                                                                                    (IVA per cassa P.A.)
                                                                                </xsl:when>
                                                                                <xsl:when test="$RF='RF17'">
                                                                                    (IVA per cassa - art. 32-bis, D.L. 83/2012)
                                                                                </xsl:when>
                                                                                <xsl:when test="$RF='RF19'">
                                                                                    (Regime forfettario)
                                                                                </xsl:when>
                                                                                <xsl:when test="$RF='RF18'">
                                                                                    (altro)
                                                                                </xsl:when>
                                                                                <xsl:when test="$RF=''"></xsl:when>
                                                                                <xsl:otherwise>
                                                                                    <span>(!!! codice non previsto !!!)</span>
                                                                                </xsl:otherwise>
                                                                            </xsl:choose>
                                                                        </xsl:if>
                                                                    </div>
                                                                    <xsl:for-each select="Sede">
                                                                        <div class="headContent" >
                                                                            <xsl:if test="Indirizzo">
                                                                                Indirizzo:
                                                                                <span>
                                                                                    <xsl:value-of select="Indirizzo" />
                                                                                    <xsl:text></xsl:text>
                                                                                    <xsl:value-of select="NumeroCivico" />
                                                                                </span>
                                                                            </xsl:if>
                                                                        </div>
                                                                        <div class="headContent" >
                                                                            <span>
                                                                                <xsl:if test="Comune">
                                                                                    Comune:
                                                                                    <span>
                                                                                        <xsl:value-of select="Comune" />
                                                                                    </span>
                                                                                </xsl:if>
                                                                                <xsl:if test="Provincia">
                                                                                    Provincia:
                                                                                    <span>
                                                                                        <xsl:value-of select="Provincia" />
                                                                                    </span>
                                                                                </xsl:if>
                                                                            </span>
                                                                        </div>
                                                                        <div class="headContent" >
                                                                            <span>
                                                                                <xsl:if test="CAP">
                                                                                    Cap:
                                                                                    <span>
                                                                                        <xsl:value-of select="CAP" />
                                                                                    </span>
                                                                                </xsl:if>
                                                                                <xsl:if test="Nazione">
                                                                                    Nazione:
                                                                                    <span>
                                                                                        <xsl:value-of select="Nazione" />
                                                                                    </span>
                                                                                </xsl:if>
                                                                            </span>
                                                                        </div>
                                                                    </xsl:for-each>
                                                                </xsl:otherwise>
                                                            </xsl:choose>
                                                        </xsl:for-each>
                                                    </div>
                                                    <!--FINE CEDENTE PRESTATORE-->
                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                    <td class="tdHead">
                                        <table class="tableHead">
                                            <tr>
                                                <td >
                                                    <!--INIZIO CESSIONARIO COMMITTENTE-->
                                                    <div class="headBorder" >
                                                        <label class= "headerLabel"  >Cessionario/committente (cliente) </label>
                                                        <xsl:for-each select="a:FatturaElettronica/FatturaElettronicaHeader/CessionarioCommittente">
                                                            <xsl:choose>
                                                                <xsl:when test="DatiAnagrafici">
                                                                    <!--DatiAnagrafici FPA\FPR-->
                                                                    <xsl:for-each select="DatiAnagrafici">
                                                                        <div class="headContent mt5" >
                                                                            <xsl:if test="IdFiscaleIVA">
                                                                                Identificativo fiscale ai fini IVA:
                                                                                <span>
                                                                                    <xsl:value-of select="IdFiscaleIVA/IdPaese" />
                                                                                    <xsl:value-of select="IdFiscaleIVA/IdCodice" />
                                                                                </span>
                                                                            </xsl:if>
                                                                        </div>
                                                                        <div class="headContent" >
                                                                            <xsl:if test="CodiceFiscale">
                                                                                Codice fiscale:
                                                                                <span>
                                                                                    <xsl:value-of select="CodiceFiscale" />
                                                                                </span>
                                                                            </xsl:if>
                                                                        </div>
                                                                        <div class="headContent" >
                                                                            <xsl:if test="Anagrafica/Denominazione">
                                                                                Denominazione:
                                                                                <span>
                                                                                    <xsl:value-of select="Anagrafica/Denominazione" />
                                                                                </span>
                                                                            </xsl:if>
                                                                        </div>
                                                                        <div class="headContent" >
                                                                            <xsl:if test="Anagrafica/Nome | Anagrafica/Cognome">
                                                                                Cognome nome:
                                                                                <xsl:if test="Anagrafica/Cognome">
                                                                                    <span>
                                                                                        <xsl:value-of select="Anagrafica/Cognome" />
                                                                                        <xsl:text></xsl:text>
                                                                                    </span>
                                                                                </xsl:if>
                                                                                <xsl:if test="Anagrafica/Nome">
                                                                                    <span>
                                                                                        <xsl:value-of select="Anagrafica/Nome" />
                                                                                    </span>
                                                                                </xsl:if>
                                                                            </xsl:if>
                                                                        </div>
                                                                    </xsl:for-each>
                                                                    <xsl:for-each select="Sede">
                                                                        <div class="headContent" >
                                                                            <xsl:if test="Indirizzo">
                                                                                Indirizzo:
                                                                                <span>
                                                                                    <xsl:value-of select="Indirizzo" />
                                                                                    <xsl:text></xsl:text>
                                                                                    <xsl:value-of select="NumeroCivico" />
                                                                                </span>
                                                                            </xsl:if>
                                                                        </div>
                                                                        <div class="headContent" >
                                                                            <span>
                                                                                <xsl:if test="Comune">
                                                                                    Comune:
                                                                                    <span>
                                                                                        <xsl:value-of select="Comune" />
                                                                                    </span>
                                                                                </xsl:if>
                                                                                <xsl:if test="Provincia">
                                                                                    Provincia:
                                                                                    <span>
                                                                                        <xsl:value-of select="Provincia" />
                                                                                    </span>
                                                                                </xsl:if>
                                                                            </span>
                                                                        </div>
                                                                        <div class="headContent" >
                                                                            <span>
                                                                                <xsl:if test="CAP">
                                                                                    Cap:
                                                                                    <span>
                                                                                        <xsl:value-of select="CAP" />
                                                                                    </span>
                                                                                </xsl:if>
                                                                                <xsl:if test="Nazione">
                                                                                    Nazione:
                                                                                    <span>
                                                                                        <xsl:value-of select="Nazione" />
                                                                                    </span>
                                                                                </xsl:if>
                                                                            </span>
                                                                        </div>
                                                                        <div class="headContent" >
                                                                            <xsl:if test="$PecDestinatario">
                                                                                Pec: 
                                                                                <span>
                                                                                    <xsl:value-of select="$PecDestinatario" />
                                                                                </span>
                                                                            </xsl:if>
                                                                        </div>
                                                                    </xsl:for-each>
                                                                </xsl:when>
                                                                <xsl:otherwise>
                                                                    <!--Anagrafica FPRS-->
                                                                    <xsl:for-each select="IdentificativiFiscali">
                                                                        <div class="headContent mt5" >
                                                                            <xsl:if test="IdFiscaleIVA">
                                                                                Identificativo fiscale ai fini IVA:
                                                                                <span>
                                                                                    <xsl:value-of select="IdFiscaleIVA/IdPaese" />
                                                                                    <xsl:value-of select="IdFiscaleIVA/IdCodice" />
                                                                                </span>
                                                                            </xsl:if>
                                                                        </div>
                                                                        <div class="headContent" >
                                                                            <xsl:if test="CodiceFiscale">
                                                                                Codice fiscale:
                                                                                <span>
                                                                                    <xsl:value-of select="CodiceFiscale" />
                                                                                </span>
                                                                            </xsl:if>
                                                                        </div>
                                                                    </xsl:for-each>
                                                                    <xsl:for-each select="AltriDatiIdentificativi">
                                                                        <div class="headContent" >
                                                                            <xsl:if test="Denominazione">
                                                                                Denominazione:
                                                                                <span>
                                                                                    <xsl:value-of select="Denominazione" />
                                                                                </span>
                                                                            </xsl:if>
                                                                        </div>
                                                                        <div class="headContent" >
                                                                            <xsl:if test="Nome | Cognome">
                                                                                Cognome nome:
                                                                                <xsl:if test="Cognome">
                                                                                    <span>
                                                                                        <xsl:value-of select="Cognome" />
                                                                                        <xsl:text></xsl:text>
                                                                                    </span>
                                                                                </xsl:if>
                                                                                <xsl:if test="Nome">
                                                                                    <span>
                                                                                        <xsl:value-of select="Nome" />
                                                                                    </span>
                                                                                </xsl:if>
                                                                            </xsl:if>
                                                                        </div>
                                                                        <xsl:for-each select="AltriDatiIdentificativi/Sede">
                                                                            <div class="headContent" >
                                                                                <xsl:if test="Indirizzo">
                                                                                Indirizzo:
                                                                                    <span>
                                                                                        <xsl:value-of select="Indirizzo" />
                                                                                        <xsl:text></xsl:text>
                                                                                        <xsl:value-of select="NumeroCivico" />
                                                                                    </span>
                                                                                </xsl:if>
                                                                            </div>
                                                                            <div class="headContent" >
                                                                                <span>
                                                                                    <xsl:if test="Comune">
                                                                                        Comune:
                                                                                        <span>
                                                                                            <xsl:value-of select="Comune" />
                                                                                        </span>
                                                                                    </xsl:if>
                                                                                    <xsl:if test="Provincia">
                                                                                        Provincia:
                                                                                        <span>
                                                                                            <xsl:value-of select="Provincia" />
                                                                                        </span>
                                                                                    </xsl:if>
                                                                                </span>
                                                                            </div>
                                                                            <div class="headContent" >
                                                                                <span>
                                                                                    <xsl:if test="CAP">
                                                                                        Cap:
                                                                                        <span>
                                                                                            <xsl:value-of select="CAP" />
                                                                                        </span>
                                                                                    </xsl:if>
                                                                                    <xsl:if test="Nazione">
                                                                                        Nazione:
                                                                                        <span>
                                                                                            <xsl:value-of select="Nazione" />
                                                                                        </span>
                                                                                    </xsl:if>
                                                                                </span>
                                                                            </div>
                                                                            <div class="headContent" >
                                                                                <xsl:if test="$PecDestinatario">
                                                                                    Pec: 
                                                                                    <span>
                                                                                        <xsl:value-of select="$PecDestinatario" />
                                                                                    </span>
                                                                                </xsl:if>
                                                                            </div>
                                                                        </xsl:for-each>
                                                                    </xsl:for-each>
                                                                </xsl:otherwise>
                                                            </xsl:choose>
                                                        </xsl:for-each>
                                                    </div>
                                                    <!--FINE CESSIONARIO COMMITTENTE-->
                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>
                            </table>
                        </xsl:if>
                        <!-- FINE FatturaElettronicaHeader -->
                        <!--INIZIO BODY-->
                        <xsl:for-each select="a:FatturaElettronica/FatturaElettronicaBody">
                            <table class="tbFoglio">
                                <!-- TIPOLOGIA DOCUMENTO TESTATA-->
                                <thead>
                                    <tr>
                                        <th scope="col">Tipologia documento</th>
                                        <th scope="col" class="perc">Art. 73</th>
                                        <th scope="col">Numero documento</th>
                                        <th scope="col" class="data">Data documento</th>
                                        <th scope="col">Codice destinatario</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr>
                                        <td>
                                            <xsl:if test="DatiGenerali/DatiGeneraliDocumento/TipoDocumento">
                                                <xsl:value-of select="DatiGenerali/DatiGeneraliDocumento/TipoDocumento" />
                                                <xsl:variable name="TD">
                                                    <xsl:value-of select="DatiGenerali/DatiGeneraliDocumento/TipoDocumento" />
                                                </xsl:variable>
                                                <xsl:choose>
                                                    <xsl:when test="$TD='TD01'">
                                                        (fattura)
                                                    </xsl:when>
                                                    <xsl:when test="$TD='TD02'">
                                                        (acconto/anticipo su fattura)
                                                    </xsl:when>
                                                    <xsl:when test="$TD='TD03'">
                                                        (acconto/anticipo su parcella)
                                                    </xsl:when>
                                                    <xsl:when test="$TD='TD04'">
                                                        (nota di credito)
                                                    </xsl:when>
                                                    <xsl:when test="$TD='TD05'">
                                                        (nota di debito)
                                                    </xsl:when>
                                                    <xsl:when test="$TD='TD06'">
                                                        (parcella)
                                                    </xsl:when>
                                                    <xsl:when test="$TD='TD16'">
                                                        (Integrazione fattura reverse charge interno)
                                                    </xsl:when>
                                                    <xsl:when test="$TD='TD17'">
                                                        (Integrazione/autofattura per acquisto servizi dall’estero)
                                                    </xsl:when>
                                                    <xsl:when test="$TD='TD18'">
                                                        (Integrazione per acquisto di beni intracomunitari)
                                                    </xsl:when>
                                                    <xsl:when test="$TD='TD19'">
                                                        (Integrazione/autofattura per acquisto di beni ex art.17 c.2 DPR n. 633/72)
                                                    </xsl:when>
                                                    <xsl:when test="$TD='TD20'">
                                                        (autofattura)
                                                    </xsl:when>
                                                    <xsl:when test="$TD='TD21'">
                                                        (Autofattura per splafonamento)
                                                    </xsl:when>
                                                    <xsl:when test="$TD='TD22'">
                                                        (Estrazione beni da Deposito IVA)
                                                    </xsl:when>
                                                    <xsl:when test="$TD='TD23'">
                                                        (Estrazione beni da Deposito IVA con versamento dell’IVA)
                                                    </xsl:when>
                                                    <xsl:when test="$TD='TD24'">
														(fattura differita)
													</xsl:when>
													<xsl:when test="$TD='TD25'">
														(fattura differita)
													</xsl:when>
                                                    <xsl:when test="$TD='TD26'">
                                                        (Cessione di beni ammortizzabili e per passaggi interni (ex art.36 DPR 633/72))
                                                    </xsl:when>
                                                    <xsl:when test="$TD='TD27'">
                                                        (Fattura per autoconsumo o per cessioni gratuite senza rivalsa)
                                                    </xsl:when>
                                                    <!--FPRS-->
                                                    <xsl:when test="$TD='TD07'">
                                                        (fattura semplificata)
                                                    </xsl:when>
                                                    <xsl:when test="$TD='TD08'">
                                                        (nota di credito semplificata)
                                                    </xsl:when>
                                                    <xsl:when test="$TD='TD09'">
                                                        (nota di debito semplificata)
                                                    </xsl:when>
                                                    <xsl:when test="$TD=''"></xsl:when>
                                                    <xsl:otherwise>
                                                        <span>(!!! codice non previsto !!!)</span>
                                                    </xsl:otherwise>
                                                </xsl:choose>
                                            </xsl:if>
                                        </td>
                                        <td class="ritenuta"  >
                                            <xsl:if test="DatiGenerali/DatiGeneraliDocumento/Art73">
                                                <xsl:value-of select="DatiGenerali/DatiGeneraliDocumento/Art73" />
                                            </xsl:if>
                                        </td>
                                        <td class="textCenter" >
                                            <xsl:if test="DatiGenerali/DatiGeneraliDocumento/Numero">
                                                <xsl:value-of select="translate(DatiGenerali/DatiGeneraliDocumento/Numero, ' ', '&#183;')" />
                                            </xsl:if>
                                        </td>
                                        <td class="data" >
                                            <xsl:if test="DatiGenerali/DatiGeneraliDocumento/Data">
                                                <xsl:call-template name="FormatDateIta">
                                                    <xsl:with-param name="DateTime" select="DatiGenerali/DatiGeneraliDocumento/Data" />
                                                </xsl:call-template>
                                            </xsl:if>
                                        </td>
                                        <td class="textCenter" >
                                            <xsl:choose>
                                                <xsl:when test="$PecDestinatario">
                                                    Indicata PEC
                                                </xsl:when>
                                                <xsl:otherwise>
                                                    <xsl:if test="$CodiceDestinatario">
                                                        <xsl:value-of select="$CodiceDestinatario" />
                                                    </xsl:if>
                                                </xsl:otherwise>
                                            </xsl:choose>
                                        </td>
                                    </tr>
                                    <!--FINE TIPOLOGIA Documento TESTATA-->
                                </tbody>
                            </table>
                            <xsl:if test="DatiGenerali/DatiGeneraliDocumento/Causale">
                                <div class="separa"></div>
                                <table class="tbFoglio">
                                    <!-- TIPOLOGIA DOCUMENTO TESTATA - parte causale-->
                                    <thead>
                                        <tr>
                                            <th scope="col">Causale</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <tr>
                                            <td >
                                                <xsl:if test="DatiGenerali/DatiGeneraliDocumento/Causale">
                                                    <xsl:for-each select="DatiGenerali/DatiGeneraliDocumento/Causale"  >
                                                        <xsl:value-of select="." />
                                                    </xsl:for-each>
                                                </xsl:if>
                                            </td>
                                        </tr>
                                        <!--FINE TIPOLOGIA Documento TESTATA - parte causale -->
                                    </tbody>
                                </table>
                            </xsl:if>
                            <div class="separa"></div>
                            <!-- Dati RIEPILOGO-->
                            <table class="tbFoglio">
                                <thead>
                                    <tr>
                                        <th scope="col" class="riepiloghi" colspan="10">RIEPILOGHI IVA E TOTALI</th>
                                    </tr>
                                    <tr >
                                        <th scope="col" colspan="3" >Esigibilità iva / riferimenti normativi</th>
                                        <th scope="col" class="perc">%IVA</th>
                                        <th scope="col">Spese accessorie</th>
                                        <th scope="col" colspan="3" >Totale imponibile</th>
                                        <th scope="col" colspan="2" >Totale imposta</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <xsl:for-each select="DatiBeniServizi/DatiRiepilogo" >
                                        <xsl:if test="number(ImponibileImporto)">
                                            <tr>
                                                <td colspan="3" >
                                                    <xsl:choose>
                                                        <xsl:when test="EsigibilitaIVA">
                                                            <span>
                                                                <xsl:value-of select="EsigibilitaIVA" />
                                                            </span>
                                                            <xsl:variable name="EI">
                                                                <xsl:value-of select="EsigibilitaIVA" />
                                                            </xsl:variable>
                                                            <xsl:choose>
                                                                <xsl:when test="$EI='I'">
                                                                    (esigibilità immediata)
                                                                </xsl:when>
                                                                <xsl:when test="$EI='D'">
                                                                    (esigibilità differita)
                                                                </xsl:when>
                                                                <xsl:when test="$EI='S'">
                                                                    (scissione dei pagamenti)
                                                                </xsl:when>
                                                                <xsl:otherwise>
                                                                    <span>(!!! codice non previsto !!!)</span>
                                                                </xsl:otherwise>
                                                            </xsl:choose>
                                                        </xsl:when>
                                                    </xsl:choose>
                                                    <xsl:choose>
                                                        <xsl:when test="Natura='N1'">
                                                            <div class="tx-xsmall">Escluse (es. ex artt. 2, 3, 5, 13,15, del DPR n. 633/72)</div>
                                                        </xsl:when>
                                                        <xsl:when test="Natura='N2'">
                                                            <div class="tx-xsmall">Non soggette (es. ex art.7-bis, 7-ter, 7-quater, 7- quinquies, ecc. del DPR n. 633/72)</div>
                                                        </xsl:when>
                                                        <xsl:when test="Natura='N3'">
                                                            <div class="tx-xsmall">Non imponibile (es. ex artt.8, 8-bis, 9, 71, 72, del DPR n. 633/72 e artt.41 e 58 del D.L. n. 331/793)</div>
                                                        </xsl:when>
                                                        <xsl:when test="Natura='N4'">
                                                            <div class="tx-xsmall">Esente (ex art.10 del DPR n.633/72)</div>
                                                        </xsl:when>
                                                        <xsl:when test="Natura='N5'">
                                                            <div class="tx-xsmall">Regime del margine per i beni usati /editoria/ agenzie di viaggio e turismo</div>
                                                        </xsl:when>
                                                        <xsl:when test="Natura='N6'">
                                                            <div class="tx-xsmall">Inversione contabile ("reverse charge"") (es. ex art.74 commi 7 e 8, art.17, commi 2 e 6 del DPR n. 633/72, artt.38 e 40 del D.L. n. 331/93)</div>
                                                        </xsl:when>
                                                        <xsl:when test="Natura='N7'">
                                                            <div class="tx-xsmall">IVA assolta in altro Stato UE (vendite a distanza sopra la soglia, commercio elettronico diretto verso privati)</div>
                                                        </xsl:when>
                                                    </xsl:choose>
                                                    <xsl:if test="RiferimentoNormativo">
                                                        <div class="tx-xsmall">
                                                            <xsl:value-of select="RiferimentoNormativo" />
                                                        </div>
                                                    </xsl:if>
                                                </td>
                                                <td class="import" >
                                                    <xsl:call-template name="FormatIVA">
                                                        <xsl:with-param name="Natura" select="Natura" />
                                                        <xsl:with-param name="IVA" select="AliquotaIVA" />
                                                    </xsl:call-template>
                                                </td>
                                                <td class="import">
                                                    <xsl:if test="SpeseAccessorie">
                                                        <xsl:value-of select="format-number(SpeseAccessorie,  '###.###.##0,00', 'euro')" />
                                                    </xsl:if>
                                                </td>
                                                <td  colspan="3" class="import" >
                                                    <xsl:if test="ImponibileImporto">
                                                        <xsl:value-of select="format-number(ImponibileImporto,  '###.###.##0,00', 'euro')" />
                                                    </xsl:if>
                                                </td>
                                                <td colspan="2"  class="import" >
                                                    <xsl:if test="Imposta">
                                                        <xsl:choose>
                                                            <xsl:when test="Imposta = 0">
                                                                <xsl:text>0</xsl:text>
                                                            </xsl:when>
                                                            <xsl:otherwise>
                                                                <xsl:value-of select="format-number(Imposta,  '###.###.##0,00', 'euro')" />
                                                            </xsl:otherwise>
                                                        </xsl:choose>
                                                    </xsl:if>
                                                </td>
                                            </tr>
                                        </xsl:if>
                                    </xsl:for-each>
                                    <!-- Importo Totale  -->
                                    <tr >
                                        <th scope="colgroup" colspan="2">
                                            Importo bollo
                                        </th>
                                        <th scope="colgroup" colspan="3">
                                            Sconto/Maggiorazione
                                        </th>
                                        <th scope="colgroup" colspan="2"  >
                                            Valuta
                                        </th>
                                        <th scope="colgroup" colspan="3" >
                                            Totale documento
                                        </th>
                                    </tr>
                                    <tr >
                                        <td colspan="2" class="import" >
                                            <xsl:if test="DatiGenerali/DatiGeneraliDocumento/DatiBollo/ImportoBollo">
                                                <xsl:value-of select="format-number(DatiGenerali/DatiGeneraliDocumento/DatiBollo/ImportoBollo,  '###.###.##0,00', 'euro')" />
                                            </xsl:if>
                                        </td>
                                        <td colspan="3" class="import">
                                            <xsl:for-each select="DatiGenerali/DatiGeneraliDocumento/ScontoMaggiorazione"  >
                                                <xsl:choose>
                                                    <xsl:when test="Tipo = 'SC' ">
                                                        <xsl:text>-</xsl:text>
                                                    </xsl:when>
                                                    <xsl:when test="Tipo = 'MG'">
                                                        <xsl:text>+</xsl:text>
                                                    </xsl:when>
                                                    <xsl:otherwise></xsl:otherwise>
                                                </xsl:choose>
                                                <xsl:choose>
                                                    <xsl:when test="Percentuale">
                                                        <xsl:value-of select="Percentuale" />
                                                        <xsl:text>%</xsl:text>
                                                    </xsl:when>
                                                    <xsl:otherwise>
                                                        <xsl:if test="Importo">
                                                            <xsl:value-of select="format-number(Importo,  '###.###.##0,00', 'euro')" />
                                                        </xsl:if>
                                                    </xsl:otherwise>
                                                </xsl:choose>
                                                <xsl:text></xsl:text>
                                            </xsl:for-each>
                                        </td>
                                        <td colspan="2" class="textCenter"  >
                                            <xsl:if test="DatiGenerali/DatiGeneraliDocumento/Divisa">
                                                <xsl:value-of select="DatiGenerali/DatiGeneraliDocumento/Divisa" />
                                            </xsl:if>
                                        </td>
                                        <td colspan="3" class="import">
                                            <xsl:if test="DatiGenerali/DatiGeneraliDocumento/ImportoTotaleDocumento">
                                                <xsl:value-of select="format-number(DatiGenerali/DatiGeneraliDocumento/ImportoTotaleDocumento,  '###.###.##0,00', 'euro')" />
                                            </xsl:if>
                                        </td>
                                    </tr>
                                    <!-- FINE Importo Totale  -->
                                </tbody>
                            </table>
                            <!--  FINE Dettaglio Linee   -->
                            <!--   Dati Ritenuta Acconto   -->
                            <xsl:if test="DatiGenerali/DatiGeneraliDocumento/DatiRitenuta">
                                <div class="separa"></div>
                                <table class="tbFoglio">
                                    <thead>
                                        <tr>
                                            <th scope="col" class="title"> Dati ritenuta d'acconto</th>
                                            <th scope="col" class="perc">Aliquota ritenuta</th>
                                            <th scope="col">Causale </th>
                                            <th scope="col" width="15%">Importo </th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <xsl:for-each select="DatiGenerali/DatiGeneraliDocumento/DatiRitenuta" >
                                        <tr>
                                            <td>
                                                <xsl:if test="TipoRitenuta">
                                                    <span>
                                                        <xsl:value-of select="TipoRitenuta" />
                                                    </span>
                                                    <xsl:variable name="TR">
                                                        <xsl:value-of select="TipoRitenuta" />
                                                    </xsl:variable>
                                                    <xsl:choose>
                                                        <xsl:when test="$TR='RT01'">
                                                            (ritenuta persone fisiche)
                                                        </xsl:when>
                                                        <xsl:when test="$TR='RT02'">
                                                            (ritenuta persone giuridiche)
                                                        </xsl:when>
                                                        <xsl:when test="$TR='RT04'">
                                                            (contributo ENASARCO)
                                                        </xsl:when>
                                                        <xsl:when test="$TR=''"></xsl:when>
                                                        <xsl:otherwise>
                                                            <span>(!!! codice non previsto !!!)</span>
                                                        </xsl:otherwise>
                                                    </xsl:choose>
                                                </xsl:if>
                                            </td>
                                            <td class="import" >
                                                <xsl:if test="AliquotaRitenuta">
                                                    <xsl:value-of select="format-number(AliquotaRitenuta,  '###.###.##0,00', 'euro')" />
                                                </xsl:if>
                                            </td>
                                            <td >
                                                <xsl:if test="CausalePagamento">
                                                    <span>
                                                        <xsl:value-of select="CausalePagamento" />
                                                    </span>
                                                    <xsl:variable name="CP">
                                                        <xsl:value-of select="CausalePagamento" />
                                                    </xsl:variable>
                                                    <xsl:if test="$CP!=''">
                                                        (decodifica come da modello 770S)
                                                    </xsl:if>
                                                </xsl:if>
                                            </td>
                                            <td class="import" >
                                                <xsl:if test="ImportoRitenuta">
                                                    <xsl:value-of select="format-number(ImportoRitenuta,  '###.###.##0,00', 'euro')" />
                                                </xsl:if>
                                            </td>
                                        </tr>
                                        </xsl:for-each>
                                    </tbody>
                                </table>
                            </xsl:if>
                            <!--  Fine Dati Ritenuta   -->
                            <div class="separa"></div>
                            <!--   Dati Pagamento   -->
                            <xsl:for-each select="DatiPagamento" >
                                <xsl:for-each select="DettaglioPagamento">
                                    <table class="tbFoglio tbPagamento" >
                                        <tbody>
                                            <tr>
                                                <th scope="row">Modalità pagamento</th>
                                                <td>
                                                    <xsl:if test="ModalitaPagamento">
                                                        <span>
                                                            <xsl:value-of select="ModalitaPagamento" />
                                                        </span>
                                                        <xsl:variable name="MP">
                                                            <xsl:value-of select="ModalitaPagamento" />
                                                        </xsl:variable>
                                                        <xsl:choose>
                                                            <xsl:when test="$MP='MP01'">
                                                                Contanti
                                                            </xsl:when>
                                                            <xsl:when test="$MP='MP02'">
                                                                Assegno
                                                            </xsl:when>
                                                            <xsl:when test="$MP='MP03'">
                                                                Assegno circolare
                                                            </xsl:when>
                                                            <xsl:when test="$MP='MP04'">
                                                                Contanti presso Tesoreria
                                                            </xsl:when>
                                                            <xsl:when test="$MP='MP05'">
                                                                Bonifico
                                                            </xsl:when>
                                                            <xsl:when test="$MP='MP06'">
                                                                Vaglia cambiario
                                                            </xsl:when>
                                                            <xsl:when test="$MP='MP07'">
                                                                Bollettino bancario
                                                            </xsl:when>
                                                            <xsl:when test="$MP='MP08'">
                                                                Carta di pagamento
                                                            </xsl:when>
                                                            <xsl:when test="$MP='MP09'">
                                                                RID
                                                            </xsl:when>
                                                            <xsl:when test="$MP='MP10'">
                                                                RID utenze
                                                            </xsl:when>
                                                            <xsl:when test="$MP='MP11'">
                                                                RID veloce
                                                            </xsl:when>
                                                            <xsl:when test="$MP='MP12'">
                                                                RIBA
                                                            </xsl:when>
                                                            <xsl:when test="$MP='MP13'">
                                                                MAV
                                                            </xsl:when>
                                                            <xsl:when test="$MP='MP14'">
                                                                Quietanza erario
                                                            </xsl:when>
                                                            <xsl:when test="$MP='MP15'">
                                                                Giroconto su conti di contabilità speciale
                                                            </xsl:when>
                                                            <xsl:when test="$MP='MP16'">
                                                                Domiciliazione bancaria
                                                            </xsl:when>
                                                            <xsl:when test="$MP='MP17'">
                                                                Domiciliazione postale
                                                            </xsl:when>
                                                            <xsl:when test="$MP='MP18'">
                                                                Bollettino di c/c postale
                                                            </xsl:when>
                                                            <xsl:when test="$MP='MP19'">
                                                                SEPA Direct Debit
                                                            </xsl:when>
                                                            <xsl:when test="$MP='MP20'">
                                                                SEPA Direct Debit CORE
                                                            </xsl:when>
                                                            <xsl:when test="$MP='MP21'">
                                                                SEPA Direct Debit B2B
                                                            </xsl:when>
                                                            <xsl:when test="$MP='MP22'">
                                                                Trattenuta su somme già riscosse
                                                            </xsl:when>
                                                            <xsl:when test="$MP=''"></xsl:when>
                                                            <xsl:otherwise>
                                                                <span></span>
                                                            </xsl:otherwise>
                                                        </xsl:choose>
                                                        <span>
                                                            <xsl:value-of select="OpzDescrizionePagamento" />
                                                        </span>
                                                    </xsl:if>
                                                </td>
                                            </tr>
                                            <xsl:if test="IBAN != ''">
                                                <tr>
                                                    <th scope="row">IBAN</th>
                                                    <td>
                                                        <xsl:value-of select="IBAN" />
                                                    </td>
                                                </tr>
                                            </xsl:if>
                                            <xsl:if test="IstitutoFinanziario != ''">
                                                <tr>
                                                    <th scope="row">Istituto</th>
                                                    <td>
                                                        <xsl:if test="IstitutoFinanziario">
                                                            <xsl:value-of select="IstitutoFinanziario" />
                                                        </xsl:if>
                                                    </td>
                                                </tr>
                                            </xsl:if>
                                            <xsl:if test="DataScadenzaPagamento != ''">
                                                <tr>
                                                    <th scope="row" class="data">Data scadenza</th>
                                                    <td class="data">
                                                        <xsl:if test="DataScadenzaPagamento">
                                                            <xsl:call-template name="FormatDateIta">
                                                                <xsl:with-param name="DateTime" select="DataScadenzaPagamento" />
                                                            </xsl:call-template>
                                                        </xsl:if>
                                                    </td>
                                                </tr>
                                            </xsl:if>
                                            <xsl:if test="ImportoPagamento != ''">
                                                <tr>
                                                    <th scope="row" class="ximport">Importo</th>
                                                    <td class="import">
                                                        <xsl:if test="ImportoPagamento">
                                                            <xsl:value-of select="format-number(ImportoPagamento,  '###.###.##0,00', 'euro')" />
                                                        </xsl:if>
                                                    </td>
                                                </tr>
                                            </xsl:if>
                                        </tbody>
                                    </table>
                                    <div class="separa"></div>
                                </xsl:for-each>
                            </xsl:for-each>
                            
                            <div class="separa"></div>
                            
                            <!--  Dettaglio Linee   -->
                            <table class="tbFoglio linee"  >
                                <thead>
                                    <tr>
                                        <th scope="col" width="100px">Cod. articolo</th>
                                        <th scope="col">Descrizione</th>
                                        <th scope="col" class="import2" >Quantità</th>
                                        <th scope="col" class="import2">Prezzo unitario</th>
                                        <th scope="col" class="perc2">UM</th>
                                        <th scope="col" class="perc">Sconto o magg.</th>
                                        <th scope="col" class="perc2">%IVA</th>
                                        <th scope="col" class="ximport">Prezzo totale</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <xsl:for-each select="DatiGenerali/DatiOrdineAcquisto">
                                        <xsl:if test="not(RiferimentoNumeroLinea)">
                                        <tr class="rowWhite">
                                            <td></td>
                                            <td style="font-size: 10px">
                                                Vs.Ord. <xsl:value-of select="IdDocumento" /> del 
                                                <xsl:call-template name="FormatDateIta">
                                                    <xsl:with-param name="DateTime" select="Data" />
                                                </xsl:call-template>
                                            </td>
                                            <td></td>
                                            <td></td>
                                            <td></td>
                                            <td></td>
                                            <td></td>
                                            <td></td>
                                        </tr>
                                        </xsl:if>
                                    </xsl:for-each>
                                    
                                    <xsl:for-each select="DatiGenerali/DatiDDT">
                                        <xsl:if test="not(RiferimentoNumeroLinea)">
                                        <tr class="rowWhite">
                                            <td></td>
                                            <td style="font-size: 10px">
                                                D.D.T. <xsl:value-of select="NumeroDDT" /> del 
                                                <xsl:call-template name="FormatDateIta">
                                                    <xsl:with-param name="DateTime" select="DataDDT" />
                                                </xsl:call-template>
                                            </td>
                                            <td></td>
                                            <td></td>
                                            <td></td>
                                            <td></td>
                                            <td></td>
                                            <td></td>
                                        </tr>
                                        </xsl:if>
                                    </xsl:for-each>
                                    
                                    <xsl:for-each select="DatiBeniServizi/DettaglioLinee" >
                                        <xsl:variable name="nl" select="NumeroLinea"/>
                                        <xsl:variable name="pos" select="position()"/>
                                        <!--Pre LINEA OpzPreLineaDatiDDT -->
                                        <xsl:for-each select="OpzPreLineaDatiDDT"  >
                                            <xsl:call-template name="AltraDescrizioneLinea">
                                                <xsl:with-param name="textDescrizione" select = "." />
                                            </xsl:call-template>
                                        </xsl:for-each>
                                        <!--Pre LINEA OpzPreLineaDatiOrdineAcquisto  -->
                                        <xsl:for-each select="OpzPreLineaDatiOrdineAcquisto"  >
                                            <xsl:call-template name="AltraDescrizioneLinea">
                                                <xsl:with-param name="textDescrizione" select = "." />
                                            </xsl:call-template>
                                        </xsl:for-each>
                                        <!--Pre LINEA OpzPreLineaDatiContratto  -->
                                        <xsl:for-each select="OpzPreLineaDatiContratto"  >
                                            <xsl:call-template name="AltraDescrizioneLinea">
                                                <xsl:with-param name="textDescrizione" select = "." />
                                            </xsl:call-template>
                                        </xsl:for-each>
                                        <!--Pre LINEA OpzPreLineaDatiFattureCollegate  -->
                                        <xsl:for-each select="OpzPreLineaDatiFattureCollegate"  >
                                            <xsl:call-template name="AltraDescrizioneLinea">
                                                <xsl:with-param name="textDescrizione" select = "." />
                                            </xsl:call-template>
                                        </xsl:for-each>
                                        <!--DETTAGLIO LINEE -->
                                        <xsl:for-each select="../../DatiGenerali/DatiDDT">
                                            <xsl:if test="RiferimentoNumeroLinea = $nl">
                                            <tr>
                                                <xsl:attribute name="class">
                                                    <xsl:choose>
                                                        <xsl:when test="$pos mod 2 = 0">
                                                            <xsl:text>rowDettaglio</xsl:text>
                                                        </xsl:when>
                                                        <xsl:otherwise>
                                                            <xsl:text>rowWhite</xsl:text>
                                                        </xsl:otherwise>
                                                    </xsl:choose>
                                                </xsl:attribute>
                                                <td></td>
                                                <td style="font-size: 10px">
                                                    D.D.T. <xsl:value-of select="NumeroDDT" /> del 
                                                    <xsl:call-template name="FormatDateIta">
                                                        <xsl:with-param name="DateTime" select="DataDDT" />
                                                    </xsl:call-template>
                                                </td>
                                                <td></td>
                                                <td></td>
                                                <td></td>
                                                <td></td>
                                                <td></td>
                                                <td></td>
                                            </tr>
                                            </xsl:if>
                                        </xsl:for-each>
                                        
                                        <xsl:for-each select="../../DatiGenerali/DatiOrdineAcquisto">
                                            <xsl:if test="RiferimentoNumeroLinea = $nl">
                                            <tr>
                                                <xsl:attribute name="class">
                                                    <xsl:choose>
                                                        <xsl:when test="$pos mod 2 = 0">
                                                            <xsl:text>rowDettaglio</xsl:text>
                                                        </xsl:when>
                                                        <xsl:otherwise>
                                                            <xsl:text>rowWhite</xsl:text>
                                                        </xsl:otherwise>
                                                    </xsl:choose>
                                                </xsl:attribute>
                                                <td></td>
                                                <td style="font-size: 10px">
                                                    Vs.Ord. <xsl:value-of select="IdDocumento" /> del 
                                                    <xsl:call-template name="FormatDateIta">
                                                        <xsl:with-param name="DateTime" select="Data" />
                                                    </xsl:call-template>
                                                </td>
                                                <td></td>
                                                <td></td>
                                                <td></td>
                                                <td></td>
                                                <td></td>
                                                <td></td>
                                            </tr>
                                            </xsl:if>
                                        </xsl:for-each>
                                        
                                        <tr>
                                            <xsl:attribute name="class">
                                                <xsl:choose>
                                                    <xsl:when test="$pos mod 2 = 0">
                                                        <xsl:text>rowDettaglio</xsl:text>
                                                    </xsl:when>
                                                    <xsl:otherwise>
                                                        <xsl:text>rowWhite</xsl:text>
                                                    </xsl:otherwise>
                                                </xsl:choose>
                                            </xsl:attribute>
                                            <td>
                                                <xsl:for-each select="CodiceArticolo"  >
                                                    <div class="tx-xsmall">
                                                        <xsl:if test="CodiceValore">
                                                            <xsl:text></xsl:text>
                                                            <xsl:value-of select="CodiceValore" />
                                                        </xsl:if>
                                                        <xsl:if test="CodiceTipo">
                                                            (<xsl:value-of select="CodiceTipo" />)
                                                        </xsl:if>
                                                    </div>
                                                </xsl:for-each>
                                            </td>
                                            <td class="altriDati">
                                                <xsl:if test="Descrizione">
                                                    <div><xsl:value-of select="Descrizione" /></div>
                                                </xsl:if>
                                                <xsl:if test="TipoCessionePrestazione">
                                                    <div>(<xsl:value-of select="TipoCessionePrestazione" />)</div>
                                                </xsl:if>
                                                <xsl:for-each select="AltriDatiGestionali"  >
                                                    <xsl:if test="translate(TipoDato, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz') != 'aswrelstd' 
                                                            and translate(TipoDato, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz') != 'aswswhouse'  
                                                            and translate(TipoDato, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz') != 'aswtriga'">
                                                        <div class="tx-xsmall">
                                                            <xsl:value-of select="TipoDato" />
                                                            <xsl:if test=" translate(TipoDato, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz') = 'aswlottsca' ">
                                                                <xsl:text> (dati relativi a lotti e scadenze) </xsl:text>
                                                            </xsl:if>: 
                                                            <xsl:if test="RiferimentoTesto">
                                                                <xsl:if test="translate(TipoDato, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz') = 'aswlottsca' ">
                                                                    <xsl:text>Lotto: </xsl:text>
                                                                </xsl:if>
                                                                <xsl:value-of select="RiferimentoTesto" />
                                                            </xsl:if>
                                                            <xsl:if test="RiferimentoData">
                                                                <xsl:call-template name="FormatDateIta">
                                                                    <xsl:with-param name="DateTime" select="RiferimentoData" />
                                                                </xsl:call-template>
                                                            </xsl:if>
                                                            <xsl:if test="RiferimentoNumero">
                                                                <xsl:if test=" translate( TipoDato, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz') = 'aswlottsca' ">
                                                                    <xsl:text>Quantità del suddetto lotto: </xsl:text>
                                                                </xsl:if>
                                                                <xsl:value-of select="format-number(RiferimentoNumero,  '###.###.##0,########', 'euro')" />
                                                            </xsl:if>
                                                        </div>
                                                    </xsl:if>
                                                </xsl:for-each>
                                                <xsl:if test="RiferimentoAmministrazione">
                                                    <div class="tx-xsmall">
                                                        RIF.AMM. 
                                                        <xsl:value-of select="RiferimentoAmministrazione" />
                                                    </div>
                                                </xsl:if>
                                            </td>
                                            <td class="import2" >
                                                <xsl:if test="Quantita">
                                                    <xsl:if test="number(Quantita)">
                                                        <xsl:value-of select="format-number(Quantita,  '###.###.##0,00######', 'euro')" />
                                                    </xsl:if>
                                                </xsl:if>
                                            </td>
                                            <td class="import" >
                                                <xsl:if test="PrezzoUnitario">
                                                    <xsl:if test="number(PrezzoTotale)">
                                                        <xsl:value-of select="format-number(PrezzoUnitario,  '###.###.##0,00######', 'euro')" />
                                                    </xsl:if>
                                                </xsl:if>
                                            </td>
                                            <td class="textCenter" >
                                                <xsl:if test="UnitaMisura">
                                                    <xsl:value-of select="substring(UnitaMisura, 1, 5)" />
                                                </xsl:if>
                                            </td>
                                            <td class="import" >
                                                <xsl:for-each select="ScontoMaggiorazione" >
                                                    <div>
                                                        <xsl:choose>
                                                            <xsl:when test="Tipo = 'SC' ">
                                                                <xsl:text>-</xsl:text>
                                                            </xsl:when>
                                                            <xsl:when test="Tipo = 'MG'">
                                                                <xsl:text>+</xsl:text>
                                                            </xsl:when>
                                                            <xsl:otherwise></xsl:otherwise>
                                                        </xsl:choose>
                                                        <xsl:choose>
                                                            <xsl:when test="Percentuale">
                                                                <xsl:value-of select="Percentuale" />
                                                                <xsl:text>%</xsl:text>
                                                            </xsl:when>
                                                            <xsl:otherwise>
                                                                <xsl:if test="Importo">
                                                                    <xsl:value-of select="format-number(Importo,  '###.###.##0,00', 'euro')" />
                                                                </xsl:if>
                                                            </xsl:otherwise>
                                                        </xsl:choose>
                                                    </div>
                                                </xsl:for-each>
                                            </td>
                                            <td class="import" >
                                                <xsl:if test="number(PrezzoTotale)">
                                                    <xsl:call-template name="FormatIVA">
                                                        <xsl:with-param name="Natura" select="Natura" />
                                                        <xsl:with-param name="IVA" select="AliquotaIVA" />
                                                    </xsl:call-template>
                                                </xsl:if>
                                            </td>
                                            <td>
                                                <xsl:if test="PrezzoTotale">
                                                    <xsl:if test="number(PrezzoTotale)">
                                                        <div class="import">
                                                            <xsl:value-of select="format-number(PrezzoTotale,  '###.###.##0,00######', 'euro')" />
                                                        </div>
                                                    </xsl:if>
                                                    <xsl:if test="OpzPrezzoTotale">
                                                        <div class="tx-xsmall">
                                                            <xsl:value-of select="OpzPrezzoTotale" />
                                                        </div>
                                                    </xsl:if>
                                                </xsl:if>
                                            </td>
                                        </tr>
                                        <!--POST LINEA -->
                                        <xsl:for-each select="OpzPostLinea"  >
                                            <xsl:call-template name="AltraDescrizioneLinea">
                                                <xsl:with-param name="textDescrizione" select = "." />
                                            </xsl:call-template>
                                        </xsl:for-each>
                                    </xsl:for-each>
                                </tbody>
                            </table>
                            <!--   Dati Cassa Prevvidenziale    -->
                            <xsl:if test="DatiGenerali/DatiGeneraliDocumento/DatiCassaPrevidenziale">
                                <div class="separa"></div>
                                <table class="tbFoglio">
                                    <thead>
                                        <tr>
                                            <th scope="col" class="title">Dati Cassa Previdenziale</th>
                                            <th scope="col">Imponibile</th>
                                            <th scope="col" class="perc">%Contr.</th>
                                            <th scope="col" class="perc">Ritenuta</th>
                                            <th scope="col" class="perc">%IVA</th>
                                            <th scope="col">Importo</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <xsl:for-each select="DatiGenerali/DatiGeneraliDocumento/DatiCassaPrevidenziale"  >
                                            <tr>
                                                <td>
                                                    <xsl:if test="TipoCassa">
                                                        <span>
                                                            <xsl:value-of select="TipoCassa" />
                                                        </span>
                                                        <xsl:variable name="TC">
                                                            <xsl:value-of select="TipoCassa" />
                                                        </xsl:variable>
                                                        <xsl:choose>
                                                            <xsl:when test="$TC='TC01'">
                                                                (Cassa Nazionale Previdenza e Assistenza Avvocati e Procuratori legali)
                                                            </xsl:when>
                                                            <xsl:when test="$TC='TC02'">
                                                                (Cassa Previdenza Dottori Commercialisti)
                                                            </xsl:when>
                                                            <xsl:when test="$TC='TC03'">
                                                                (Cassa Previdenza e Assistenza Geometri)
                                                            </xsl:when>
                                                            <xsl:when test="$TC='TC04'">
                                                                (Cassa Nazionale Previdenza e Assistenza Ingegneri e Architetti liberi profess.)
                                                            </xsl:when>
                                                            <xsl:when test="$TC='TC05'">
                                                                (Cassa Nazionale del Notariato)
                                                            </xsl:when>
                                                            <xsl:when test="$TC='TC06'">
                                                                (Cassa Nazionale Previdenza e Assistenza Ragionieri e Periti commerciali)
                                                            </xsl:when>
                                                            <xsl:when test="$TC='TC07'">
                                                                (Ente Nazionale Assistenza Agenti e Rappresentanti di Commercio-ENASARCO)
                                                            </xsl:when>
                                                            <xsl:when test="$TC='TC08'">
                                                                (Ente Nazionale Previdenza e Assistenza Consulenti del Lavoro-ENPACL)
                                                            </xsl:when>
                                                            <xsl:when test="$TC='TC09'">
                                                                (Ente Nazionale Previdenza e Assistenza Medici-ENPAM)
                                                            </xsl:when>
                                                            <xsl:when test="$TC='TC10'">
                                                                (Ente Nazionale Previdenza e Assistenza Farmacisti-ENPAF)
                                                            </xsl:when>
                                                            <xsl:when test="$TC='TC11'">
                                                                (Ente Nazionale Previdenza e Assistenza Veterinari-ENPAV)
                                                            </xsl:when>
                                                            <xsl:when test="$TC='TC12'">
                                                                (Ente Nazionale Previdenza e Assistenza Impiegati dell'Agricoltura-ENPAIA)
                                                            </xsl:when>
                                                            <xsl:when test="$TC='TC13'">
                                                                (Fondo Previdenza Impiegati Imprese di Spedizione e Agenzie Marittime)
                                                            </xsl:when>
                                                            <xsl:when test="$TC='TC14'">
                                                                (Istituto Nazionale Previdenza Giornalisti Italiani-INPGI)
                                                            </xsl:when>
                                                            <xsl:when test="$TC='TC15'">
                                                                (Opera Nazionale Assistenza Orfani Sanitari Italiani-ONAOSI)
                                                            </xsl:when>
                                                            <xsl:when test="$TC='TC16'">
                                                                (Cassa Autonoma Assistenza Integrativa Giornalisti Italiani-CASAGIT)
                                                            </xsl:when>
                                                            <xsl:when test="$TC='TC17'">
                                                                (Ente Previdenza Periti Industriali e Periti Industriali Laureati-EPPI)
                                                            </xsl:when>
                                                            <xsl:when test="$TC='TC18'">
                                                                (Ente Previdenza e Assistenza Pluricategoriale-EPAP)
                                                            </xsl:when>
                                                            <xsl:when test="$TC='TC19'">
                                                                (Ente Nazionale Previdenza e Assistenza Biologi-ENPAB)
                                                            </xsl:when>
                                                            <xsl:when test="$TC='TC20'">
                                                                (Ente Nazionale Previdenza e Assistenza Professione Infermieristica-ENPAPI)
                                                            </xsl:when>
                                                            <xsl:when test="$TC='TC21'">
                                                                (Ente Nazionale Previdenza e Assistenza Psicologi-ENPAP)
                                                            </xsl:when>
                                                            <xsl:when test="$TC='TC22'">
                                                                (INPS)
                                                            </xsl:when>
                                                            <xsl:when test="$TC=''"></xsl:when>
                                                            <xsl:otherwise>
                                                                <span>(!!! codice non previsto !!!)</span>
                                                            </xsl:otherwise>
                                                        </xsl:choose>
                                                    </xsl:if>
                                                </td>
                                                <td class="import">
                                                    <xsl:if test="ImponibileCassa">
                                                        <xsl:value-of select="format-number(ImponibileCassa,  '###.###.##0,00', 'euro')" />
                                                    </xsl:if>
                                                </td>
                                                <td class="import">
                                                    <xsl:if test="AlCassa">
                                                        <xsl:value-of select="format-number(AlCassa,  '###.###.##0,00', 'euro')" />
                                                    </xsl:if>
                                                </td>
                                                <td  class="Ritenuta" >
                                                    <xsl:if test="Ritenuta">
                                                        <xsl:value-of select="Ritenuta" />
                                                    </xsl:if>
                                                </td>
                                                <td class="import" >
                                                    <xsl:choose>
                                                        <xsl:when test="Natura">
                                                            <xsl:value-of select="Natura" />
                                                        </xsl:when>
                                                        <xsl:otherwise>
                                                            <xsl:if test="AliquotaIVA">
                                                                <xsl:value-of select="format-number(AliquotaIVA,  '###.###.##0,00', 'euro')" />
                                                            </xsl:if>
                                                        </xsl:otherwise>
                                                    </xsl:choose>
                                                </td>
                                                <td class="import">
                                                    <xsl:if test="ImportoContributoCassa">
                                                        <xsl:value-of select="format-number(ImportoContributoCassa,  '###.###.##0,00', 'euro')" />
                                                    </xsl:if>
                                                </td>
                                            </tr>
                                        </xsl:for-each>
                                    </tbody>
                                </table>
                            </xsl:if>
                            <!--  Fine Cassa Prevvidenziale    -->
                            <div class="separa" ></div>
                            <!-- Definizione degli allegati -->
                            <xsl:if test="Allegati">
                                <div class="tx-small" >Allegati:</div>
                                <ul class="ulAllegati">
                                    <xsl:for-each select="Allegati">
                                        <li>
                                            <div class="tx-small">
                                                <xsl:value-of select="NomeAttachment" />
                                                <xsl:text></xsl:text>
                                                <xsl:value-of select="DescrizioneAttachment" />
                                            </div>
                                        </li>
                                    </xsl:for-each>
                                </ul>
                            </xsl:if>
                        </xsl:for-each>
                        <!--FINE BODY-->
                    </div>
                </div>
            </body>
        </html>
    </xsl:template>
</xsl:stylesheet>