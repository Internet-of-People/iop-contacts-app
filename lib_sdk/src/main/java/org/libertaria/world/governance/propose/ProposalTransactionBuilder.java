package org.libertaria.world.governance.propose;

import org.apache.commons.codec.DecoderException;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.libertaria.world.blockchain.OpReturnOutputTransaction;
import org.libertaria.world.crypto.CryptoBytes;
import org.libertaria.world.utils.ArraysUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.bitcoinj.core.Utils.HEX;
import static org.libertaria.world.global.utils.Preconditions.compareLessThan;
import static org.libertaria.world.utils.ArraysUtils.numericTypeToByteArray;

//import iop.org.iop_contributors_app.core.iop_sdk.blockchain.OpReturnOutputTransaction;
//import iop.org.iop_contributors_app.core.iop_sdk.crypto.CryptoBytes;
//
//import static iop.org.iop_contributors_app.core.iop_sdk.utils.ArraysUtils.numericTypeToByteArray;
//import static iop.org.iop_contributors_app.utils.Preconditions.compareLessThan;

/**
 * Created by mati on 11/11/16.
 */
public  class ProposalTransactionBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(ProposalTransactionBuilder.class);

    public static final Coin FREEZE_VALUE = Coin.valueOf(1000,0);

    // position
    private static final int CONTRACT_TAG_POSITION = 0;
    private static final int CONTRACT_VERSION_POSITION = 2;
    private static final int CONTRACT_START_HEIGHT_POSITION = 4;
    private static final int CONTRACT_END_HEIGHT_POSITION = 7;
    private static final int CONTRACT_REWARD_POSITION = 9;
    private static final int CONTRACT_HASH_POSITION = 12;
    private static final int CONTRACT_FORUM_ID_POSITION = 44;
    // size
    private static final int CONTRACT_SIZE = 46;
    private static final int CONTRACT_TAG_SIZE = 2;
    private static final int CONTRACT_VERSION_SIZE = 2;
    private static final int CONTRACT_START_HEIGHT_SIZE = 3;
    private static final int CONTRACT_END_HEIGHT_SIZE = 2;
    private static final int CONTRACT_REWARD_SIZE = 3;
    private static final int CONTRACT_HASH_SIZE = 32;
    private static final int CONTRACT_FORUM_ID_SIZE = 2;

    /** tag */
    private static final short tag = 0x4343;

    private NetworkParameters networkParameters;

    private Transaction proposalTransaction = null;

    private int blockStartHeight;

    private int endHeight;
    /** in IoPtoshis.. */
    private long blockReward;

    private byte[] proposalHash;
    /** Forum identifier */
    private int forumId;

    private int bestChainHeight;

    private List<TransactionOutput> inputs;

    private List<TransactionOutput> prevOpOutputs;

    private List<TransactionOutput> postOpOutputs;

    private OpReturnOutputTransaction contractTransaction;

    /** height+1000+startBlock */
    private int blockchainStartBlock;
    /** blockchainStartBlock+endHeight  */
    private int blockchainEndBlock;
    /** blockchainEndBlock-blockchainStartBlock */
    private int totalBlocks;
    /**    */
    private long totalReward;

    // inputs total amount
    private Coin totalCoins;


    public ProposalTransactionBuilder(NetworkParameters networkParameters, int bestChainHeight) {

        this.networkParameters = networkParameters;
        this.bestChainHeight = bestChainHeight;

        this.proposalTransaction = new Transaction(networkParameters);

        this.totalCoins = Coin.ZERO;

        inputs = new ArrayList<>();
        prevOpOutputs = new ArrayList<>();
        postOpOutputs = new ArrayList<>();
    }

    private void checkValid() {
        if (bestChainHeight>(blockStartHeight+1000)){
            throw new IllegalArgumentException("blockStartHeight must be 1000 blocks away from the bestChainHeight");
        }
    }

    /**
     * Add inputs
     *
     * @param unspentTransactions
     * @return
     */
    public ProposalTransactionBuilder addInputs(List<TransactionOutput> unspentTransactions) {
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
    public ProposalTransactionBuilder addLockedAddressOutput(Address address, byte[] lockedOutputHash){
        totalCoins=totalCoins.minus(FREEZE_VALUE);
        TransactionOutput transactionOutput = new TransactionOutput(networkParameters,proposalTransaction, FREEZE_VALUE,address);
        prevOpOutputs.add(transactionOutput);
        if (lockedOutputHash!=null)System.arraycopy(transactionOutput.getHash().getBytes(),0,lockedOutputHash,0,lockedOutputHash.length);
        return this;
    }

    public TransactionOutput addLockedAddressOutput(Address address){
        totalCoins=totalCoins.minus(FREEZE_VALUE);
        TransactionOutput transactionOutput = new TransactionOutput(networkParameters,proposalTransaction, FREEZE_VALUE,address);
        prevOpOutputs.add(transactionOutput);
        return transactionOutput;
    }

    /**
     *
     *
     * @param refundCoins
     * @param address
     */
    public ProposalTransactionBuilder addRefundOutput(Coin refundCoins, Address address){
        totalCoins=totalCoins.minus(refundCoins);
        prevOpOutputs.add(new TransactionOutput(networkParameters,proposalTransaction,refundCoins,address));
        return this;
    }

    public ProposalTransactionBuilder addContract(short version,int blockStartHeight, int endHeight, long blockReward, byte[] proposalHash,int forumId){

        // validate parameters
        if (proposalHash.length!=32) throw new IllegalArgumentException("hash is not from SHA256");
        compareLessThan(forumId,1,"invalid forum id");
        compareLessThan(endHeight,0,"invalid endHeight");
        compareLessThan(blockStartHeight,0,"invalid startHeight");
        compareLessThan(blockStartHeight,0,"invalid blockReward");

        this.blockStartHeight = blockStartHeight;
        this.endHeight = endHeight;
        this.blockReward = blockReward;
        this.proposalHash = proposalHash;
        this.forumId = forumId;


        // data
        blockchainStartBlock = bestChainHeight+1000+blockStartHeight;
        blockchainEndBlock = blockchainStartBlock+endHeight;
        totalBlocks = blockchainEndBlock-blockchainStartBlock;
        totalReward = blockReward*totalBlocks;


        try {

            // data
            byte[] prevData = new byte[CONTRACT_SIZE];
            ArraysUtils.numericTypeToByteArray(prevData,tag,CONTRACT_TAG_POSITION,CONTRACT_TAG_SIZE);
            ArraysUtils.numericTypeToByteArray(prevData,version,CONTRACT_VERSION_POSITION,CONTRACT_VERSION_SIZE);
            ArraysUtils.numericTypeToByteArray(prevData,blockStartHeight,CONTRACT_START_HEIGHT_POSITION,CONTRACT_START_HEIGHT_SIZE);
            ArraysUtils.numericTypeToByteArray(prevData,endHeight,CONTRACT_END_HEIGHT_POSITION,CONTRACT_END_HEIGHT_SIZE);
            ArraysUtils.numericTypeToByteArray(prevData,blockReward,CONTRACT_REWARD_POSITION,CONTRACT_REWARD_SIZE);
            System.arraycopy(proposalHash,0,prevData,CONTRACT_HASH_POSITION,CONTRACT_HASH_SIZE);
            ArraysUtils.numericTypeToByteArray(prevData,forumId,CONTRACT_FORUM_ID_POSITION,CONTRACT_FORUM_ID_SIZE);

            OpReturnOutputTransaction opReturnOutputTransaction = new OpReturnOutputTransaction.Builder(networkParameters)
                    .setParentTransaction(proposalTransaction)
                    .addData(prevData)
                    .build2();

            LOG.info("OP_RETURN TRANSACTION created, data: "+opReturnOutputTransaction.toString());
            LOG.info("OP_RETURN HEX: "+ CryptoBytes.toHexString(opReturnOutputTransaction.getData()));
            contractTransaction = opReturnOutputTransaction;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return this;
    }

    /**
     * Add a beneficiary
     *
     * @param address
     * @param coinPerBlock
     */
    public ProposalTransactionBuilder addBeneficiary(Address address, Coin coinPerBlock){
        totalCoins= totalCoins.minus(coinPerBlock);
        postOpOutputs.add(new TransactionOutput(networkParameters,proposalTransaction,coinPerBlock,address));
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
        proposalTransaction.addOutput(contractTransaction);


        // beneficiaries outputs
        long totalOutputReward = 0;
        for (TransactionOutput postOpOutput : postOpOutputs) {
            proposalTransaction.addOutput(postOpOutput);
            totalOutputReward+=postOpOutput.getValue().getValue();
        }

        if (blockReward<totalOutputReward){
            throw new IllegalArgumentException("total reward in the beneficiaries outputs is different that the total in the contract.");
        }

        return proposalTransaction;
    }


    /**
     * Metodo para decodificar el valor del op_retun
     *
     * @param  data -> OP_RETUTN data
     * @return
     */
    public static Proposal decodeContract(byte[] data) throws DecoderException, UnsupportedEncodingException {

        if (data.length!=CONTRACT_SIZE) throw new IllegalArgumentException("data has not the right size: "+data.length);

        LOG.debug("Data to decode: "+CryptoBytes.toHexString(data));

        Proposal proposal = new Proposal();

        short newTag = getShortData(data,CONTRACT_TAG_POSITION,CONTRACT_TAG_SIZE);
        if (tag != newTag ) throw new IllegalArgumentException("data tag is not the right one, tag: "+newTag) ;

        proposal.setVersion(getShortData(data,CONTRACT_VERSION_POSITION,CONTRACT_VERSION_SIZE));
        proposal.setVotingPeriodWithoutValidation(getIntData(data,CONTRACT_START_HEIGHT_POSITION,CONTRACT_START_HEIGHT_SIZE));
        proposal.setEndBlock(getIntData(data,CONTRACT_END_HEIGHT_POSITION,CONTRACT_END_HEIGHT_SIZE));
        proposal.setBlockReward(getLongData(data,CONTRACT_REWARD_POSITION,CONTRACT_REWARD_SIZE));
// 4343||0001||000014||003c||f5e100||0080a9f7727726783617077919407ceec77865f5ae67d908b87ab0b42ef55fc9||007f
        //todo: el hash deberia matchear despues cuando traigo la propuesta del foro y chequeo que sea igual
//        if (proposal.checkHash(getByteArray(data,CONTRACT_HASH_POSITION,CONTRACT_HASH_SIZE))) throw new IllegalArgumentException("Hash don't match");
        proposal.setGenesisTxHash(CryptoBytes.toHexString(getByteArray(data,CONTRACT_HASH_POSITION,CONTRACT_HASH_SIZE)));
        proposal.setForumId(getIntData(data,CONTRACT_FORUM_ID_POSITION,CONTRACT_FORUM_ID_SIZE));

        return proposal;
    }

    public static Proposal decodeContract(TransactionOutput transactionOutput) throws DecoderException, UnsupportedEncodingException {
        byte[] contract = new byte[CONTRACT_SIZE];
        System.arraycopy(transactionOutput.getScriptBytes(),2,contract,0,CONTRACT_SIZE);
        return decodeContract(contract);
    }

    public static Proposal decodeContract(String opReturn) throws DecoderException, UnsupportedEncodingException {
        byte[] opBytes = CryptoBytes.fromHexToBytes(opReturn);
        return decodeContract(opBytes);
    }

    private static byte[] getByteArray(byte[] data, int init, int lenght) {
        byte[] retDat = new byte[lenght];
        System.arraycopy(data,init,retDat,0,lenght);
        return retDat;
    }


    private static int getIntData(byte[] data, int init, int lenght){
        byte[] retDat = new byte[lenght];
        System.arraycopy(data,init,retDat,0,lenght);
        String versionStr = HEX.encode(retDat);
        return new BigInteger(versionStr,16).intValue();
    }

    private static short getShortData(byte[] data, int init, int lenght){
        byte[] retDat = new byte[lenght];
        System.arraycopy(data,init,retDat,0,lenght);
        String versionStr = HEX.encode(retDat);
        return new BigInteger(versionStr,16).shortValue();
    }

    private static long getLongData(byte[] data, int init, int lenght){
        byte[] retDat = new byte[lenght];
        System.arraycopy(data,init,retDat,0,lenght);
        String versionStr = HEX.encode(retDat);
        return new BigInteger(versionStr,16).longValue();
    }

    public static boolean isProposal(Transaction transaction) {
        List<TransactionOutput> list = transaction.getOutputs();
        if (list.size()>2) {
            for (int i = 2; i < list.size(); i++) {
                TransactionOutput transactionOutput = list.get(i);
                try {
                    if (ProposalTransactionBuilder.decodeContract(transactionOutput) != null) {
                        return true;
                    }
                } catch (DecoderException e) {
                    // nothing
                } catch (UnsupportedEncodingException e) {
                    // nothing
                } catch (Exception e) {
                    // nothing
                }
            }
        }
        return false;
    }

    public static Proposal getProposal(Transaction transaction) {
        Proposal proposal = null;
        List<TransactionOutput> list = transaction.getOutputs();
        if (list.size()>2) {
            for (int i = 2; i < list.size(); i++) {
                TransactionOutput transactionOutput = list.get(i);
                try {
                    if ((proposal = ProposalTransactionBuilder.decodeContract(transactionOutput)) != null) {
                        return proposal;
                    }
                } catch (DecoderException e) {
                    // nothing
                } catch (UnsupportedEncodingException e) {
                    // nothing
                } catch (Exception e) {
                    // nothing
                }
            }
        }
        return proposal;
    }


}

