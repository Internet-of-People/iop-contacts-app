package org.fermat.redtooth.governance.vote;

import com.google.common.util.concurrent.ListenableFuture;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.wallet.CoinSelection;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.WalletTransaction;
import org.fermat.redtooth.blockchain.NotConnectedPeersException;
import org.fermat.redtooth.utils.StringUtils;
import org.fermat.redtooth.wallet.BlockchainManager;
import org.fermat.redtooth.wallet.CantSendVoteException;
import org.fermat.redtooth.wallet.WalletManager;
import org.fermat.redtooth.wallet.WalletPreferenceConfigurations;
import org.fermat.redtooth.wallet.exceptions.InsuficientBalanceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.bitcoinj.core.Transaction.MIN_NONDUST_OUTPUT;
import static org.fermat.redtooth.wallet.utils.WalletUtils.sumValue;

/**
 * Created by mati on 21/12/16.
 * //todo: el fee tiene que ser el 1% de la cantidad de votos. -> 0.5 votes -> 0.005 fee
 */

public class VoteProposalRequest {

    private static final Logger LOG = LoggerFactory.getLogger(VoteProposalRequest.class);


    private BlockchainManager blockchainManager;
    private WalletManager walletManager;
    private VotesDao votesDaoImp;
    private WalletPreferenceConfigurations conf;

    private SendRequest sendRequest;

    private Vote vote;
    private long lockedBalance;


    public VoteProposalRequest(BlockchainManager blockchainManager, WalletManager walletManager, VotesDao votesDaoImp) {
        this.blockchainManager = blockchainManager;
        this.walletManager = walletManager;
        this.votesDaoImp = votesDaoImp;
        this.conf = walletManager.getConfigurations();
    }

    public void forVote(Vote vote) throws InsuficientBalanceException,CantSendVoteException {

        validateVote(vote);

        this.vote = vote;
        VoteTransactionBuilder voteTransactionBuilder = new VoteTransactionBuilder(conf.getNetworkParams());
        Wallet wallet = walletManager.getWallet();

        // output value to freeze
        Coin freezeOutputVotingPowerValue = Coin.valueOf(vote.getVotingPower());
        // vote transaction fee == voting power amount / 100 -> 10% of voting power
        Coin feeForVoting = Coin.valueOf(vote.getVotingPower()/100);
        // transaction fee
        Coin transactionFee = Transaction.REFERENCE_DEFAULT_MIN_TX_FEE;
        // total value of the vote transaction
        Coin totalOuputsValue = feeForVoting.plus(freezeOutputVotingPowerValue).plus(transactionFee);

        List<TransactionOutput> unspentTransactions = new ArrayList<>();
        Coin totalInputsValue = Coin.ZERO;
        // check if the vote is already used, if the wallet have the genesisTx of the vote we reuse the freeze output as input of the new vote.
        if (vote.getLockedOutputHex()!=null){
            LOG.info("Reusing the previous vote tx, adding the frozen output as input of the tx");

            //check if the transaction is confirmed
            Map<Sha256Hash, Transaction> pool = wallet.getTransactionPool(WalletTransaction.Pool.UNSPENT);
            Sha256Hash sha256Hash = Sha256Hash.wrap(vote.getLockedOutputHex());
            Transaction tx = pool.get(sha256Hash);
            if (tx!=null) {
                TransactionOutput prevFrozenOutput = tx.getOutput(0);
                unspentTransactions.add(prevFrozenOutput);
            }else {
                Transaction txTmp = wallet.getTransaction(sha256Hash);
                if (txTmp!=null){
                    if (!txTmp.isMature()){
                        throw new CantSendVoteException("Vote transaction is not mature in blockchain\nPlease wait until it is accepted");
                    }else
                        throw new CantSendVoteException("Locked output hash transaction exist but is already spent, tx: "+txTmp.toString());
                }else {
                    throw new CantSendVoteException("Locked output hash transaction don't exist in this wallet, tx: "+txTmp.toString());
                }
            }
        }
        // fill the tx with valid inputs
        if (!sumValue(unspentTransactions).isGreaterThan(totalOuputsValue) && !totalOuputsValue.isNegative() && totalOuputsValue.getValue()!=0) {
            // the second value is the already used outputs, todo: deberia restar el amount del input usado previamente en el totalOutputValue..
            unspentTransactions.addAll(walletManager.getInputsForAmount(totalOuputsValue,unspentTransactions));
        }
        // inputs value
        totalInputsValue = sumValue(unspentTransactions);
        // put inputs..
        voteTransactionBuilder.addInputs(unspentTransactions);
        // first check if the value is non dust
        if (MIN_NONDUST_OUTPUT.isGreaterThan(Coin.valueOf(vote.getVotingPower()))){
            throw new CantSendVoteException("Vote value is to small to be included, min value: "+ MIN_NONDUST_OUTPUT.toFriendlyString());
        }
        // freeze address -> voting power
        Address lockAddress = wallet.freshReceiveAddress();
        TransactionOutput transactionOutputToLock = voteTransactionBuilder.addLockedAddressOutput(lockAddress,vote.getVotingPower());
        //update locked balance
        lockedBalance+=Coin.valueOf(vote.getVotingPower()).getValue();
        // op return output
        voteTransactionBuilder.addContract(vote.isYesVote(),vote.getGenesisHash());
        // refunds output
        //
        Coin flyingCoins = totalInputsValue.minus(totalOuputsValue);
        // check si el refund output es dust o no
        if (flyingCoins.isLessThan(MIN_NONDUST_OUTPUT)){
            // Si es un dust output tengo que agregar nuevos inputs para que lo supere.
            List<TransactionOutput> outputs = walletManager.getInputsForAmount(MIN_NONDUST_OUTPUT,unspentTransactions);
            voteTransactionBuilder.addInputs(outputs);
            // agrego los inputs al flyingCoins
            flyingCoins = flyingCoins.add(sumValue(outputs));
        }
        voteTransactionBuilder.addRefundOutput(flyingCoins, wallet.freshReceiveAddress());

        // build the transaction..
        Transaction tran = voteTransactionBuilder.build();

        LOG.info("Transaction fee: " + tran.getFee());

        sendRequest = SendRequest.forTx(tran);

        sendRequest.signInputs = true;
        sendRequest.shuffleOutputs = false;
        sendRequest.coinSelector = new MyCoinSelector();

        StringBuilder outputsValue = new StringBuilder();
        int i=0;
        for (TransactionOutput transactionOutput : sendRequest.tx.getOutputs()) {
            outputsValue.append("Output "+i+" value "+transactionOutput.getValue().getValue());
            outputsValue.append("\n");
            i++;
        }

        LOG.info(" *Outpus value:\n "+outputsValue.toString());
        LOG.info("Total outputs sum: "+sendRequest.tx.getOutputSum());
        LOG.info("Total inputs sum sum: "+sendRequest.tx.getInputSum());

        // complete transaction
        try {
            wallet.completeTx(sendRequest);
        } catch (InsufficientMoneyException e) {
            LOG.error("Insuficient money exception",e);
            throw new InsuficientBalanceException("Insuficient money exception",e);
        }

        LOG.info("inputs value: " + tran.getInputSum().toFriendlyString() + ", outputs value: " + tran.getOutputSum().toFriendlyString() + ", fee: " + tran.getFee().toFriendlyString());
        LOG.info("total en el aire: " + tran.getInputSum().minus(tran.getOutputSum().minus(tran.getFee())).toFriendlyString());

        // now that the transaction is complete lock the output
        // lock address
        String parentTransactionHashHex = sendRequest.tx.getHash().toString();
        LOG.info("Locking transaction with 1000 IoPs: position: "+0+", parent hash: "+parentTransactionHashHex);
        vote.setLockedOutputHashHex(parentTransactionHashHex);
        vote.setLockedOutputIndex(0);

    }

    private void validateVote(Vote vote) {
        if (vote.getVote() == Vote.VoteType.NEUTRAL) throw new IllegalArgumentException("Invalid vote type, neutral vote is not a vote.");
        if (vote.getVote()== Vote.VoteType.YES) {
            // vote has to be greater than 10,000,000,000 toshis -> 100 IoPs
            if (vote.getVotingPower() < Vote.MIN_POSITIVE_VOTE_VALUE)
                throw new IllegalArgumentException("Vote value is lower than the min positive value accepted, min value: " + StringUtils.formatNumberToString(Vote.MIN_POSITIVE_VOTE_VALUE));
        }else {
            // vote has to be greater than 2,000,000,000 toshis -> 20 IoPs
            if (vote.getVotingPower() < Vote.MIN_NEGATIVE_VOTE_VALUE)
                throw new IllegalArgumentException("Vote value is lower than the min negative value accepted, min value: " + StringUtils.formatNumberToString(Vote.MIN_NEGATIVE_VOTE_VALUE));
        }
    }


    public void broadcast() throws NotConnectedPeersException {
        Wallet wallet = walletManager.getWallet();
        try {

            // check if we have at least one peer connected
            if(blockchainManager.getConnectedPeers().isEmpty()) throw new NotConnectedPeersException();

            wallet.commitTx(sendRequest.tx);

            ListenableFuture<Transaction> future = blockchainManager.broadcastTransaction(sendRequest.tx.getHash().getBytes());
            future.get(1, TimeUnit.MINUTES);

            LOG.info("TRANSACCION BROADCASTEADA EXITOSAMENTE, hash: "+sendRequest.tx.getHash().toString());

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
            throw new NotConnectedPeersException(e);
        }
    }

    public long getLockedBalance() {
        return lockedBalance;
    }

    public Vote getUpdatedVote() {
        return vote;
    }

    private class MyCoinSelector implements org.bitcoinj.wallet.CoinSelector {
        @Override
        public CoinSelection select(Coin coin, List<TransactionOutput> list) {
            return new CoinSelection(coin,new ArrayList<TransactionOutput>());
        }
    }

}
