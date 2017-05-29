package org.fermat.redtooth.blockchain;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.ProtocolException;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptOpCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * Created by Matias Furszyfer on 11/11/16.
 */

public class OpReturnOutputTransaction extends TransactionOutput {

    private static final Logger LOG = LoggerFactory.getLogger(OpReturnOutputTransaction.class);

    /** OP_RETURN limit size */
    public static final int FIELD_SIZE = 80;

    private byte[] data;

    public static OpReturnOutputTransaction buildFrom(TransactionOutput transactionOutput){
        return new OpReturnOutputTransaction(transactionOutput.getParams(),transactionOutput.getParentTransaction(),transactionOutput.getValue(),transactionOutput.getScriptBytes());
    }

    public OpReturnOutputTransaction(NetworkParameters params, Transaction parent, Coin value, byte[] scriptBytes) {
        super(params, parent, value, scriptBytes);
    }

    public OpReturnOutputTransaction(NetworkParameters params, Transaction parent, byte[] payload, int offset) throws ProtocolException {
        super(params, parent, payload, offset);
    }


    private void setData(byte[] data){
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }


    public static class Builder{

        protected NetworkParameters params;

        /** Script builder */
        private ScriptBuilder scriptBuilder;

        /** Parent transaction */
        private Transaction parent;

        /** Hex data */
        private StringBuilder hexData;

        /** Data buffer */
        private ByteBuffer buff;

        /** value (this will burn the tokens..) */
        private Coin value = Coin.ZERO;

        /** OP_RETURN limit size */
        private int limitSize = FIELD_SIZE;

        public Builder(NetworkParameters params) {
            this.scriptBuilder = new ScriptBuilder();
            this.params = params;
            this.buff = ByteBuffer.allocate(limitSize);
            this.hexData = new StringBuilder();
        }

        public Builder setParentTransaction(Transaction parent){
            this.parent = parent;
            return this;
        }

        public Builder addData(int hexInt){
            buff.putInt(hexInt);
            return this;
        }

        public Builder addData(short hexShort){
            buff.putShort(hexShort);
            return this;
        }

        public Builder addData(String hexData){
            this.hexData.append(hexData);
            return this;
        }

        public Builder addData(Long data){
            buff.putLong(data);
            return this;
        }

        public Builder addData(byte[] data){
            buff.put(data);
            return this;
        }

//
//        public OpReturnOutputTransaction build() throws Exception {
//            String hexData = this.hexData.toString();
//            byte[] data = Hex.HEX.decode(hexData);
//            if (data.length > FIELD_SIZE) throw new Exception("data size not supported");
//            scriptBuilder.op(ScriptOpCodes.OP_RETURN).data(data);
//            OpReturnOutputTransaction transaction = new OpReturnOutputTransaction(params, parent, value, scriptBuilder.build().getProgram());
//            transaction.setHexData(hexData);
//            transaction.setData(data);
//            LOG.info("OpReturnTransaction created!, data: "+hexData);
//            return transaction;
//        }

        public OpReturnOutputTransaction build2(){
            buff.flip();
            byte[] b = getByteArray(buff);
            buff.compact();
            scriptBuilder.op(ScriptOpCodes.OP_RETURN).data(b);
            OpReturnOutputTransaction transaction = new OpReturnOutputTransaction(params, parent, value, scriptBuilder.build().getProgram());
            transaction.setData(b);
            LOG.info("OpReturnTransaction created!, data: "+hexData);
            return transaction;
        }

        static byte[] getByteArray(ByteBuffer bb) {
            byte[] ba = new byte[bb.limit()];
            bb.get(ba,0,ba.length);
            return ba;
        }
        /**
         * Add data to the buffer
         *
//         * @param data
         * @return
         */
//        public Builder addData(byte[] data) throws OpReturnOverflowException {
//            try {
//                this.buff.put(data);
//            }catch (BufferOverflowException e){
//                e.printStackTrace();
//                throw new OpReturnOverflowException("overflow op return field, limit size: "+limitSize+", current size: "+buff.position()+", data exception size: "+data.length);
//            }
//            return this;
//        }
//
//        public Builder addData(int pos,byte[] data) throws OpReturnOverflowException {
//            this.buff.position(pos);
//            try {
//                this.buff.put(data);
//            }catch (BufferOverflowException e){
//                throw new OpReturnOverflowException("overflow op return field, limit size: "+limitSize+", current size: "+buff.position()+", data exception size: "+data.length);
//            }
//            return this;
//        }

//        public OpReturnOutputTransaction build() throws Exception {
//            if (parent==null) throw new Exception(" null parent transaction");
//            if (buff.position()==0) throw new Exception(" null data");
//            scriptBuilder.op(ScriptOpCodes.OP_RETURN).data(buff.array());
//            return new OpReturnOutputTransaction(params, parent, value, scriptBuilder.build().getProgram());
//        }

    }

}
