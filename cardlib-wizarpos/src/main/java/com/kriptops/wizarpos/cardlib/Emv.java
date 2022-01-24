package com.kriptops.wizarpos.cardlib;

import android.content.Context;
import android.util.Log;

import com.cloudpos.OperationListener;
import com.cloudpos.OperationResult;
import com.cloudpos.jniinterface.EMVJNIInterface;
import com.cloudpos.jniinterface.IFuntionListener;
import com.cloudpos.msr.MSROperationResult;
import com.cloudpos.msr.MSRTrackData;
import com.kriptops.wizarpos.cardlib.kernel.CardType;
import com.kriptops.wizarpos.cardlib.kernel.TLVMap;
import com.kriptops.wizarpos.cardlib.kernel.Tag;
import com.kriptops.wizarpos.cardlib.func.Consumer;
import com.kriptops.wizarpos.cardlib.kernel.TagName;
import com.kriptops.wizarpos.cardlib.tools.StringUtil;
import com.kriptops.wizarpos.cardlib.tools.Util;
import com.wizarpos.emvsample.constant.Constant;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.cloudpos.jniinterface.EMVJNIInterface.emv_get_candidate_list;
import static com.cloudpos.jniinterface.EMVJNIInterface.emv_get_tag_data;
import static com.cloudpos.jniinterface.EMVJNIInterface.emv_is_tag_present;
import static com.cloudpos.jniinterface.EMVJNIInterface.emv_set_candidate_list_result;
import static com.cloudpos.jniinterface.EMVJNIInterface.emv_terminal_param_set_tlv;

public class Emv {

    private static final int EMV_READER_ICC = 1;
    private static final int EMV_READER_NFC = 2;
    //1 true, 0 false
    private static final int EMV_READER_ONLINE_ONLY = 0;

    ExecutorService emvCallbacks = Executors.newFixedThreadPool(4);

    private boolean iccOpenned = false;
    private boolean nfcOpenned = false;
    private boolean msrOpenned = false;
    private boolean cardInserted = false;
    private int[] taglist;
    private Consumer<TransactionData> processOnline;
    private Pos pos;

    public int[] getTaglist() {
        return taglist;
    }

    public void setTaglist(int[] taglist) {
        this.taglist = taglist;
    }

    protected void setProcessOnline(Consumer<TransactionData> consumer) {
        this.processOnline = consumer;
    }


    //debe ser llamado unsa sola vez en la creacion del app
    public Emv(Pos pos, Context context) {
        this.pos = pos;
        //lib path
        String tmpEmvLibDir = getLibPath(context.getApplicationContext());
        byte[] data = tmpEmvLibDir.getBytes();
        EMVJNIInterface.registerFunctionListener(new EmvListener());
        pos.getMsr().setListener(new MsrListener());
        try {
            int res = EMVJNIInterface.loadEMVKernel(data, data.length);
            //on first init res is 0
            if (res == 0) {
                EMVJNIInterface.emv_kernel_initialize();
                // EMVJNIInterface.emv_set_kernel_attr(new byte[]{0x20}, 1);
                EMVJNIInterface.emv_set_kernel_attr(new byte[]{0x04,0x08}, 2);
                EMVJNIInterface.emv_terminal_param_set_drl(new byte[]{0x00}, 1);

                //TODO move to function
                EMVJNIInterface.emv_capkparam_clear();
                for (String capk: Defaults.CAPKS) {
                    // Log.d(Defaults.LOG_TAG, "CAPK: " + capk);
                    byte[] buffer = Util.toByteArray(capk);
                    EMVJNIInterface.emv_capkparam_add(buffer, buffer.length);
                }

                //TODO move to function
                EMVJNIInterface.emv_aidparam_clear();
                EMVJNIInterface.emv_contactless_aidparam_clear();
                for (String aid: Defaults.AIDS) {
                    // Log.d(Defaults.LOG_TAG, "AID: " + aid);
                    byte[] buffer = Util.toByteArray(aid);
                    EMVJNIInterface.emv_aidparam_add(buffer, buffer.length);
                    //EMVJNIInterface.emv_contactless_aidparam_add(buffer, buffer.length);
                }

                EMVJNIInterface.emv_set_force_online(EMV_READER_ONLINE_ONLY);
                Log.i(Defaults.LOG_TAG, "kernel id:" + EMVJNIInterface.emv_get_kernel_id());
                Log.i(Defaults.LOG_TAG, "process type:" + EMVJNIInterface.emv_get_process_type());
            }
        } catch (Exception ex) {
            // Log.d(Defaults.LOG_TAG, "no se puede inicializar el kernel en loademv kernel");
            throw new RuntimeException("No se puede inicializar el kernel EMV", ex);
        }
    }

    private void performAntishake() {
        // Log.d(Defaults.LOG_TAG, "PERFORM THE ANTISHAKE");
        emvCallbacks.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // Log.d(Defaults.LOG_TAG, "Esperando Otras tarjetas");
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (Emv.this.cardInserted) {
                    // Log.d(Defaults.LOG_TAG, "No procede NFC, otra interfaz tiene la tarjeta");
                    EMVJNIInterface.emv_anti_shake_finish(1);
                } else {
                    // Log.d(Defaults.LOG_TAG, "Va adelante NFC, no se ha sensado tarjeta en ICC o MSR");
                    Emv.this.pos.beep();
                    EMVJNIInterface.emv_anti_shake_finish(0);
                }
            }
        });
    }

    private void processCardInserted() {
        this.cardInserted = true;
        int emvKernelType = EMVJNIInterface.get_card_type();
        // Log.d(Defaults.LOG_TAG, "CARD INSERTED " + emvKernelType);
        CardType cardType = CardType.fromEmv(emvKernelType);
        if (cardType == null) {
            this.pos.raiseError("EMV", "invalid_card_type");
            return;
        }
        pos.getMsr().close();
        // Log.d(Defaults.LOG_TAG, "Inicializar EMV Kernel para " + cardType);
        pos.data.captureType = cardType.tag;
        /*
        switch (cardType) {
            case ICC:
                //this.setTag(0x9f07, "FFC0");
                // this.setTag(0x5f28, "");
                break;
            case NFC:
                this.setTag(0x9f07, "FFC0");
                this.setTag(0x5f28, "0604");
                break;
        }
        */
        EMVJNIInterface.close_reader(cardType.close);
        EMVJNIInterface.emv_set_kernel_type(cardType.set);
        this.next();
    }

    /**
     * @param merchantId         9F16 identificador del comercio maximo 15 caracteres completado con espacio al final
     * @param merchantName       9F4E nombre del comercio ancho variable en caracteres
     * @param terminalId         9F1C identificador del pos dentro del comercio maximo 15 caracteres completado con espacio al final
     * @param serialNumber
     * @param clFloorLimit
     * @param clTransactionLimit
     * @param cvmLimit
     */
    public void initParams(String merchantId, String merchantName, String terminalId, String serialNumber, String clFloorLimit, String clTransactionLimit, String cvmLimit) {
        merchantName = Util.nvl(merchantName, "COMERCIO");
        merchantId = Util.left(Util.nvl(merchantId, "COMERCIO123") + "               ", 15);
        terminalId = Util.right("00000000" + Util.nvl(terminalId, ""), 8);
        serialNumber = Util.right("00000000" + Util.nvl(serialNumber, ""), 8);
        Tag[] tags = new Tag[]{
                new Tag("9F16", Util.toHexString(merchantId.getBytes())),
                new Tag("9F1C", Util.toHexString(terminalId.getBytes())),
                new Tag("9F1E", Util.toHexString(serialNumber.getBytes())),
                new Tag("9F4E", Util.toHexString(merchantName.getBytes())),
                new Tag("5F2A", "0604"),
                new Tag("5F36", "02"),
                new Tag("9F1A", "0604"),
                new Tag("9F33", "E0F8C8"),
                // this is an online only terminal so terminal type is 21
                new Tag("9F35", "21"),
                //additional capabilities
                new Tag("9F40", "FF80F0A001"),
                new Tag("9F66", "36"),
                new Tag("DF19", clFloorLimit),
                new Tag("DF20", clTransactionLimit),
                new Tag("DF21", cvmLimit),
                //Privado
                new Tag("EF01", "00")
        };
        String tlvHex = Tag.compile(tags);
        // Log.d(Defaults.LOG_TAG, "cargando parametros " + tlvHex);
        byte[] data = Util.toByteArray(tlvHex);
        int response = EMVJNIInterface.emv_terminal_param_set_tlv(data, data.length);
        // Log.d(Defaults.LOG_TAG, "respuesta de carga de parametros " + response);
    }

    public void configTerminal(EMVConfig emvConfig) {
        List<Tag> tagList = new LinkedList<>();
        tagList.add(TagName.TRANSACTION_CURRENCY_CODE.tag(emvConfig.currencyCode));
        tagList.add(TagName.TRANSACTION_CURRENCY_EXPONENT.tag(emvConfig.currencyExponent));
        if (emvConfig.merchantIdentifier != null)
            tagList.add(TagName.MERCHANT_IDENTIFIER.tag(Util.toHexString(emvConfig.merchantIdentifier, 15, '0')));
        tagList.add(TagName.TERMINAL_COUNTRY_CODE.tag(emvConfig.terminalCountryCode));
        if (emvConfig.terminalIdentification != null)
            tagList.add(TagName.TERMINAL_IDENTIFICATION.tag(emvConfig.terminalIdentification));
        tagList.add(TagName.IFD_SERIAL_NUMBER.tag(Util.toHexString(android.os.Build.SERIAL, 8, '0')));
        tagList.add(TagName.TERMINAL_CAPABILITIES.tag(emvConfig.terminalCapabilities));
        tagList.add(TagName.TERMINAL_TYPE.tag(emvConfig.terminalType));
        tagList.add(TagName.ADDITIONAL_TERMINAL_CAPABILITIES.tag(emvConfig.additionalTerminalCapabilities));
        if (emvConfig.merchantNameAndLocation != null)
            tagList.add(TagName.MERCHANT_NAME_AND_LOCATION.tag(Util.toHexString(emvConfig.merchantNameAndLocation)));
        tagList.add(TagName.TTQ_1.tag(emvConfig.ttq1));
        if (emvConfig.contactlessFloorLimit != null)
            tagList.add(TagName.WP_CONTACTLESS_FLOOR_LIMIT.tag(emvConfig.contactlessFloorLimit));
        if (emvConfig.contactlessTransactionLimit != null)
            tagList.add(TagName.WP_CONTACTLESS_TRANSACTION_LIMIT.tag((emvConfig.contactlessTransactionLimit)));
        if (emvConfig.contactlessCvmLimit != null)
            tagList.add(TagName.WP_CONTACTLESS_CVM_LIMIT.tag(emvConfig.contactlessCvmLimit));
        tagList.add(TagName.WP_STATUS_CHECK_SUPPORT.tag(emvConfig.statusCheckSupport));

        String terminalSettings = Tag.compile(tagList.toArray(new Tag[tagList.size()]));
        // Log.d("KRIPTO", "++++++++++++++++++++ SETTING TERMINAL INFO " + terminalSettings);
        byte[] buffer = Util.toByteArray(terminalSettings);
        emv_terminal_param_set_tlv(buffer, buffer.length);
    }

    /**
     * @param date                       en texto en formato yyMMdd
     * @param time                       en texto en formato HHmmss
     * @param transactionSequenceCounter contador de transaccion en formato 00000000
     * @param amount                     monto en formato numerico sin decimales (los centavos son los dos ultimos digitos)
     * @param cashback                   indica si es una reversa
     */
    public void beginTransaction(
            String date,
            String time,
            String transactionSequenceCounter,
            String amount,
            boolean cashback) {
        PosOptions options = pos.getPosOptions();
        EMVJNIInterface.emv_trans_initialize();
        //BIENES Y SERVICIOS
        // this.setTag(0x9f07, "FFC0");
        // this.setTag(0x5f28, "0604");
        this.setTag(0x9a, date);
        this.setTag(0x9f21, time);
        this.setTag(0x9f41, transactionSequenceCounter);
        pos.data.type = cashback
                ? options.getReverseProcessingCode()
                : options.getAuthProcessingCode();
        EMVJNIInterface.emv_set_trans_type(pos.data.type);
        this.setAmount(amount);
        this.setAmountOther("000");

        EMVJNIInterface.emv_set_force_online(EMV_READER_ONLINE_ONLY);

        if (!iccOpenned) {
            iccOpenned = true;
            EMVJNIInterface.open_reader(EMV_READER_ICC);
            // Log.d(Defaults.LOG_TAG, "Reading Contact");
        }

        if (!nfcOpenned) {
            nfcOpenned = true;
            EMVJNIInterface.emv_set_anti_shake(1);
            EMVJNIInterface.open_reader(EMV_READER_NFC);
            // Log.d(Defaults.LOG_TAG, "Reading Contactless");
        }

        if (!msrOpenned) {
            msrOpenned = true;
            pos.getMsr().open();
            pos.getMsr().waitForTrack();
            // Log.d(Defaults.LOG_TAG, "Reading MSR");
        }
    }

    public int setAmount(String amount) {
        byte[] amountBuffer = amount.getBytes();
        byte[] buffer = new byte[amount.length() + 1];
        System.arraycopy(amountBuffer, 0, buffer, 0, amountBuffer.length);
        return EMVJNIInterface.emv_set_trans_amount(buffer);
    }

    public int setAmountOther(String amount) {
        byte[] amountBuffer = amount.getBytes();
        byte[] buffer = new byte[amount.length() + 1];
        System.arraycopy(amountBuffer, 0, buffer, 0, amountBuffer.length);
        return EMVJNIInterface.emv_set_other_amount(buffer);
    }

    public int setTag(int tag, String hexPayload) {
        byte[] buffer = Util.toByteArray(hexPayload);
        return EMVJNIInterface.emv_set_tag_data(tag, buffer, buffer.length);
    }

    public int next() {
        // Log.d(Defaults.LOG_TAG, "======> EMV NEXT ");
        return EMVJNIInterface.emv_process_next();
    }

    public int setCaptureType() {

        return -1;
    }

    public void reset() {
        EMVJNIInterface.close_reader(1);
        EMVJNIInterface.close_reader(2);
        pos.getMsr().close();
        cardInserted = false;
        iccOpenned = false;
        nfcOpenned = false;
    }

    private void readAppData() {
        TransactionData data = pos.data;
        data.maskedPan = Util.nvl(data.maskedPan, () -> this.readTag(0x5a));
        data.track2Clear = Util.nvl(data.track2Clear, () -> this.readTag(0x57));
        data.track2 = data.track2Clear;
        data.panSequenceNumber = Util.nvl(data.panSequenceNumber, () -> this.readTag(0x5f34));
        data.expiry = Util.nvl(data.expiry, () -> this.readTag(0x5f24));
        data.aid = Util.nvl(data.aid, () -> this.readTag(0x84));
        data.ecBalance = Util.nvl(data.ecBalance, () -> this.readTag(0x9f79));
        if (data.maskedPan == null) {
            data.maskedPan = data.track2.split("D")[0];
        }
    }

    private void requestPin() {
        if (pos.isPinpadCustomUI()) {
            pos.requestPinToUser();
        } else {
            pos.callPin();
        }
    }

    private void processOnline() {
        TransactionData data = pos.data;
        if ("nfc".equals(data.captureType) || "icc".equals(data.captureType)) {
            int[] tags = "nfc".equals(data.captureType)
                    ? pos.getPosOptions().getNfcTagList()
                    : pos.getPosOptions().getIccTaglist();
            data.emvData = readTags(tags);
            if (data.maskedPan == null && data.track2 != null) {
                //tratar de leer el masked pan desde el track 2
                data.maskedPan = data.track2.split("[D=]")[0];
            }
        }

        pos.data.tsiFinal = readTag(0x9b);
        pos.data.tvrFinal = readTag(0x95);
        pos.processOnline();
    }

    private String readTags(int[] tags) {
        byte[] buffer = new byte[10240];
        int readSize = EMVJNIInterface.emv_get_tag_list_data(
                tags,
                tags.length,
                buffer,
                buffer.length
        );
        byte[] tagsMaterial = new byte[readSize];
        System.arraycopy(buffer, 0, tagsMaterial, 0, readSize);
        buffer = null;
        return Util.toHexString(tagsMaterial);
    }

    protected String readTag(int tag) {
        if (isTagPresent(tag)) {
            byte[] buffer = new byte[1024];
            int read_length = emv_get_tag_data(tag, buffer, buffer.length);
            if (read_length < 0) return null;
            if (read_length == 0) return "";
            byte[] tagData = new byte[read_length];
            System.arraycopy(buffer, 0, tagData, 0, read_length);
            return Util.toHexString(tagData);
        }
        return null;
    }

    private boolean isTagPresent(int tag) {
        return emv_is_tag_present(tag) >= 0;
    }

    private class MsrListener implements OperationListener {

        @Override
        public void handleResult(OperationResult operationResult) {
            pos.getMsr().close();
            msrOpenned = false;
            int code = operationResult.getResultCode();
            switch (code) {
                case OperationResult.SUCCESS:
                    MSROperationResult result = (MSROperationResult) operationResult;
                    MSRTrackData data = result.getMSRTrackData();
                    if (data.getTrackError(1) == MSRTrackData.NO_ERROR) {
                        cardInserted = true;
                        pos.data.captureType = "msr";
                        EMVJNIInterface.close_reader(0);
                        EMVJNIInterface.close_reader(1);
                        String track2ascii = new String(data.getTrackData(1), StandardCharsets.ISO_8859_1);
                        String track2Hex = new String(track2ascii).trim().replace('=', 'D');
                        pos.data.track2 = track2Hex;
                        pos.data.maskedPan = track2Hex.split("D")[0];

                        //Strip cardnumber && strip countryCode
                        String serviceCode = track2ascii.substring(track2ascii.indexOf('='));
                        if (track2ascii.startsWith("59")) serviceCode = serviceCode.substring(3);
                        while (serviceCode.startsWith("="))
                            serviceCode = serviceCode.substring(1);//remueve la fecha si no esta presente
                        serviceCode = serviceCode.substring(0, 3);

                        // beings whitelisting of card bins for msr
                        boolean whitelisted = false;
                        for (String bin: Emv.this.pos.getPosOptions().getMsrBinWhitelistSupplier().get()) {
                            whitelisted = pos.data.track2.startsWith(bin);
                            if (whitelisted) break;
                        }
                        if (!whitelisted) {
                            char sc1 = serviceCode.charAt(0);
                            switch (sc1) {
                                case '2':
                                case '6':
                                    pos.raiseError("msr", "require_emv");
                                    return;
                                case '7':
                                    pos.raiseError("msr", "not_available");
                                    return;
                            }
                        }
                        // ends whitelisting of card bins for msr

                        // todo pin should be with bins or optional
                        // this is for pin request
                        char sc3 = serviceCode.charAt(2);
                        switch (sc3) {
                            case '3': //ATM
                            case '4': //CASH WITHDRAWL
                                pos.raiseError("msr", "not_available");
                                return;
                            case '0': //NR Pin Mandatory
                            case '5': //G&S Pin Mandatory
                            case '6': //NR Pin Feasible
                            case '7': //G&S Pin Feasible
                                //TODO RESTORE THE PIN
                                Emv.this.requestPin();
                                return;
                        }
                        // end pin request

                        processOnline();
                    }
                    break;
                case OperationResult.ERR_TIMEOUT:
                    pos.raiseError("msr", "timeout");
                    break;
                case OperationResult.CANCEL:
                    pos.raiseWarning("msr", "read_cancelled");
                    break;
                default:
                    pos.raiseError("msr", "" + code);
            }
        }
    }

    private class EmvListener implements IFuntionListener {
        @Override
        public void emvProcessCallback(byte[] bytes) {
            byte status = bytes[0];
            byte code = bytes[1];
            switch (status) {
                case 0x00:
                    switch (code) {
                        case 10:
                            pos.raiseError("emv", "nfc_icc_fallbackk");
                            break;
                        case 12:
                            pos.raiseError("emv", "retry");
                            break;
                        case 47:
                            pos.raiseError("emv", "terminal_not_initialized");
                            break;
                        default:
                            pos.raiseError("emv", "process " + code);
                            break;
                    }
                    break;
                case 0x01:
                    // aun hay mas pasos que hacer esperar y evaluar
                    switch (code) {
                        case Constant.EMV_CANDIDATE_LIST:
                            // Log.d(Defaults.LOG_TAG, "EMV_CANDIDATE_LIST " + code);
                            List<String> aidList = getAidList();
                            //TODO raise an event where we can choose the app and then continue
                            int defaultApp = 0;
                            pos.data.selectedApp = aidList.get(defaultApp);
                            emv_set_candidate_list_result(defaultApp);
                            Emv.this.emvCallbacks.execute(Emv.this::next);
                            break;
                        case Constant.EMV_APP_SELECTED:
                            // Log.d(Defaults.LOG_TAG, "EMV_APP_SELECTED " + code);
                            Emv.this.emvCallbacks.execute(Emv.this::next);
                            break;
                        case Constant.EMV_READ_APP_DATA:
                            // Log.d(Defaults.LOG_TAG, "EMV_READ_APP_DATA " + code);
                            Emv.this.readAppData();
                            Emv.this.emvCallbacks.execute(Emv.this::next);
                            break;
                        case Constant.EMV_DATA_AUTH:
                            // Log.d(Defaults.LOG_TAG, "EMV_DATA_AUTH " + code);
                            pos.data.tsi = readTag(0x9b);
                            pos.data.tvr = readTag(0x95);
                            Emv.this.emvCallbacks.execute(Emv.this::next);
                            break;
                        case Constant.EMV_OFFLINE_PIN:
                            // Log.d(Defaults.LOG_TAG, "EMV_OFFLINE_PIN " + code);
                            //TODO FIX OFFLINE PIN IN CASES OFFLINE IS NEEDED
                            Emv.this.emvCallbacks.execute(Emv.this::next);
                            break;
                        case Constant.EMV_ONLINE_ENC_PIN:
                            // Log.d(Defaults.LOG_TAG, "EMV_ONLINE_ENC_PIN " + code);
                            if (pos.data.maskedPan == null) {
                                readAppData();
                            }
                            Emv.this.requestPin();
                            break;
                        case Constant.EMV_PROCESS_ONLINE:
                            // Log.d(Defaults.LOG_TAG, "EMV_PROCESS_ONLINE " + code);
                            pos.data.online = true;
                            Emv.this.readAppData();
                            Emv.this.processOnline();
                            break;
                        default:
                            // Log.d(Defaults.LOG_TAG, "EMV STAGE " + code);
                            Emv.this.emvCallbacks.execute(Emv.this::next);
                            break;
                    }
                case 0x02:
                    //ha terminado la transaccion
                    // Log.d(Defaults.LOG_TAG, "TRANSACTION RESULT 2 " + code);
                    //some offline rejections need process online anyways
                    //TODO enviar al success o failed handler
                    break;
            }
        }

        @Override
        public void cardEventOccured(int i) {
            switch (i) {
                case Constant.SMART_CARD_EVENT_INSERT_CARD:
                    Emv.this.processCardInserted();
                    break;
                case Constant.SMART_CARD_EVENT_CONTALESS_ANTI_SHAKE:
                    Emv.this.performAntishake();
                    break;
                case Constant.SMART_CARD_EVENT_REMOVE_CARD:
                    Emv.this.pos.raiseWarning("emv", "icc_card_removed");
                    break;
                case Constant.SMART_CARD_EVENT_POWERON_ERROR:
                    Emv.this.pos.raiseError("emv", "cant_init_readers");
                case 12:
                    Emv.this.pos.raiseWarning("emv", "nfc_card_removed");
                    break;
                default:
                    Emv.this.pos.raiseError("emv", "card " + i);
            }
        }
    }

    private static List<String> getAidList() {
        byte[] buffer = new byte[2048];
        int read = emv_get_candidate_list(buffer, buffer.length);
        //aids is length (1 byte), value (length bytes)
        int offset = 0;
        List<String> list = new LinkedList<>();
        while (offset < read) {
            // lee la longitud
            int length = buffer[offset];
            // mueve el offset luego de la longitud
            offset++;
            // crea el espacio para el aid
            byte[] aid = new byte[length];
            // lee el aid
            System.arraycopy(buffer, offset, aid, 0, length);
            list.add(Util.toHexString(aid));
            // mueve el offset luego del aid
            offset += length;
        }
        return list;
    }

    private static String getLibPath(Context context) {
        //get app path
        String libPath = context.getDir("", 0).getAbsolutePath();
        //trim to base path
        int end = libPath.lastIndexOf('/');
        libPath = libPath.substring(0, end);
        //append library name
        libPath += "/lib/libEMVKernal.so";
        return libPath;
    }

}
