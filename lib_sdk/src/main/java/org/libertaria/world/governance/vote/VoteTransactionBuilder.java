package org.libertaria.world.governance.vote;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.libertaria.world.utils.ArraysUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.libertaria.world.utils.ArraysUtils.numericTypeToByteArray;


/**
 * Created by mati on 19/11/16.
 *
 *
 *
 Voting tag
 A tag indicating that this transaction is a IoP Voting transaction. It is always 0x564f54.
 3 bytes
 VotePower
 The voter decision. 1 = Yes, 0 = NO
 1 byte
 Genesis Transaction
 Sha256 hash of the transaction that originated the contract
 32 bytes

 */

public class VoteTransactionBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(VoteTransactionBuilder.class);

    // position
    private static final int VOTE_TAG_POSITION = 0;
    private static final int VOTE_POWER_POSITION = 3;
    private static final int VOTE_GENESIS_TRANSACTION_POSITION = 4;

    // size
    private static final int VOTE_SIZE = 36;
    private static final int VOTE_TAG_SIZE = 3;
    private static final int VOTE_POWER_SIZE = 1;
    private static final int VOTE_GENESIS_TRANSACTION_SIZE = 32;


    /** tag */
    private static final int tag = 0x564f54;
    /**  */
    private static short version = 1;

    private NetworkParameters networkParameters;

    private Transaction proposalTransaction = null;

    private List<TransactionOutput> inputs;

    private List<TransactionOutput> prevOpOutputs;

    private List<TransactionOutput> postOpOutputs;

    private org.libertaria.world.blockchain.OpReturnOutputTransaction voteOutput;

    // outputs total amount
    private Coin totalCoins;

    public VoteTransactionBuilder(NetworkParameters networkParameters) {

        this.networkParameters = networkParameters;

        this.proposalTransaction = new Transaction(networkParameters);

        inputs = new ArrayList<>();
        prevOpOutputs = new ArrayList<>();
        postOpOutputs = new ArrayList<>();

        totalCoins = Coin.ZERO;
    }


    /**
     * Add inputs
     *
     * @param unspentTransactions
     * @return
     */
    public VoteTransactionBuilder addInputs(List<TransactionOutput> unspentTransactions) {
        // ahora que tengo los inputs los agrego
        for (TransactionOutput unspentTransaction : unspentTransactions) {
            inputs.add(unspentTransaction);
        }
        return this;
    }


    /* Coins to lock, voting power
    *
    * @param address
    * @return transaction hash
    */
    public VoteTransactionBuilder addLockedAddressOutput(Address address, byte[] lockedOutputHash,long freezeValue){
        Coin freeze = Coin.valueOf(freezeValue);
        totalCoins=totalCoins.minus(freeze);
        TransactionOutput transactionOutput = new TransactionOutput(networkParameters,proposalTransaction, freeze,address);
        prevOpOutputs.add(transactionOutput);
        if (lockedOutputHash!=null)System.arraycopy(transactionOutput.getHash().getBytes(),0,lockedOutputHash,0,lockedOutputHash.length);
        return this;
    }

    public TransactionOutput addLockedAddressOutput(Address address,long freezeValue){
        Coin freeze = Coin.valueOf(freezeValue);
        totalCoins=totalCoins.minus(freeze);
        TransactionOutput transactionOutput = new TransactionOutput(networkParameters,proposalTransaction, freeze,address);
        prevOpOutputs.add(transactionOutput);
        return transactionOutput;
    }

    /**
     *
     *
     * @param refundCoins
     * @param address
     */
    public VoteTransactionBuilder addRefundOutput(Coin refundCoins, Address address){
        totalCoins=totalCoins.minus(refundCoins);
        postOpOutputs.add(new TransactionOutput(networkParameters,proposalTransaction,refundCoins,address));
        return this;
    }


    public VoteTransactionBuilder addContract(boolean vote,byte[] contractHash){

        // validate parameters
        if (contractHash.length!=32) throw new IllegalArgumentException("hash is not from SHA256");

        try {

            // data
            byte[] prevData = new byte[VOTE_SIZE];
            ArraysUtils.numericTypeToByteArray(prevData,tag,VOTE_TAG_POSITION,VOTE_TAG_SIZE);
            prevData[VOTE_POWER_POSITION] = (byte) (vote ? 1:0);
            System.arraycopy(contractHash,0,prevData,VOTE_GENESIS_TRANSACTION_POSITION,VOTE_GENESIS_TRANSACTION_SIZE);

            org.libertaria.world.blockchain.OpReturnOutputTransaction opReturnOutputTransaction = new org.libertaria.world.blockchain.OpReturnOutputTransaction.Builder(networkParameters)
                    .setParentTransaction(proposalTransaction)
                    .addData(prevData)
                    .build2();

            LOG.info("OP_RETURN TRANSACTION created, data: "+opReturnOutputTransaction.toString());
            LOG.debug("OP_RETURN HEX: "+ org.libertaria.world.crypto.CryptoBytes.toHexString(opReturnOutputTransaction.getData()));

            this.voteOutput = opReturnOutputTransaction;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return this;
    }

    public Transaction build(){

        // put inputs
        for (TransactionOutput input : inputs) {
            proposalTransaction.addInput(input);
        }

        // first put the prev contract outputs (like lock coins, refund)
        for (TransactionOutput prevOpOutput : prevOpOutputs) {
            proposalTransaction.addOutput(prevOpOutput);
        }

        // contract output
        proposalTransaction.addOutput(voteOutput);


        for (TransactionOutput postOpOutput : postOpOutputs) {
            proposalTransaction.addOutput(postOpOutput);
        }


        return proposalTransaction;
    }



}
