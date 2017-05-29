package org.fermat.redtooth.governance.propose;

import com.google.common.util.concurrent.ListenableFuture;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.wallet.CoinSelection;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.Wallet;
import org.fermat.redtooth.blockchain.NotConnectedPeersException;
import org.fermat.redtooth.exceptions.CantSendTransactionException;
import org.fermat.redtooth.wallet.BlockchainManager;
import org.fermat.redtooth.wallet.WalletManager;
import org.fermat.redtooth.wallet.WalletPreferenceConfigurations;
import org.fermat.redtooth.wallet.exceptions.InsuficientBalanceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.fermat.redtooth.wallet.utils.WalletUtils.sortOutputsHighToLowValue;
import static org.fermat.redtooth.wallet.utils.WalletUtils.sumValue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

;

/**
 * Created by mati on 05/12/16.
 */

public class ProposalTransactionRequest {

    private static final Logger LOG = LoggerFactory.getLogger(ProposalTransactionRequest.class.getName());

    public static final Coin PROPOSAL_CONTRACT_FEE = Coin.valueOf(1,0);

    private BlockchainManager blockchainManager;
    private WalletManager walletManager;
    private WalletPreferenceConfigurations conf;

    private Proposal proposal;
    private String lockedOutputHashHex;
    private int lockedOutputPosition = 0;
    private long lockedBalance;

    private SendRequest sendRequest;

    public ProposalTransactionRequest(BlockchainManager blockchainManager, WalletManager walletManager) {
        this.blockchainManager = blockchainManager;
        this.walletManager = walletManager;
        this.conf = walletManager.getConfigurations();
    }

    public void forProposal(Proposal proposal) throws InsuficientBalanceException, CantCompleteProposalException, CantCompleteProposalMaxTransactionExcededException {

        validateContract(proposal);

        this.proposal = proposal;

        org.bitcoinj.core.Context.propagate(conf.getWalletContext());

        ProposalTransactionBuilder proposalTransactionBuilder = new ProposalTransactionBuilder(
                conf.getNetworkParams(),
                blockchainManager.getChainHeadHeight()
        );

        Wallet wallet = walletManager.getWallet();

        Coin totalOuputsValue = Coin.ZERO;

        for (Beneficiary beneficiary : proposal.getBeneficiaries()) {
            totalOuputsValue = totalOuputsValue.add(Coin.valueOf(beneficiary.getAmount()));
        }

        // locked coins 1000 IoPs
        totalOuputsValue = totalOuputsValue.add(Coin.valueOf(1000, 0));
        // 1 IoP minimum contract fee
        Coin extraFee = Coin.valueOf(proposal.getExtraFeeValue());
        if (extraFee.isLessThan(Coin.COIN)) throw new CantCompleteProposalException("Proposal fee is lesser than 1 IoP");
        totalOuputsValue = totalOuputsValue.add(extraFee);

        // unspent inputs
        List<TransactionOutput> unspentTransactions = walletManager.getInputsForAmount(totalOuputsValue,sortOutputsHighToLowValue(wallet.getUnspents()),null);

        // inputs value
        Coin totalInputsValue = sumValue(unspentTransactions);
        // put inputs..
        proposalTransactionBuilder.addInputs(unspentTransactions);

        // lock address output
        Address lockAddress = wallet.freshReceiveAddress();
        TransactionOutput transactionOutputToLock = proposalTransactionBuilder.addLockedAddressOutput(lockAddress);

        // lock balance
        lockedBalance += Coin.valueOf(1000,0).getValue();

        // refund transaction, tengo el fee agregado al totalOutputsValue
        Coin flyingCoins = totalInputsValue.minus(totalOuputsValue);
        // le resto el fee
        //flyingCoins = flyingCoins.minus(Coin.valueOf(proposal.getExtraFeeValue()));//.minus(conf.getWalletContext().getFeePerKb());
        proposalTransactionBuilder.addRefundOutput(flyingCoins, wallet.freshReceiveAddress());


        // contract
        proposalTransactionBuilder.addContract(
                proposal.getVersion(),
                proposal.getVotingPeriod(),
                proposal.getEndBlock(),
                proposal.getBlockReward(),
                proposal.hash(),
                proposal.getForumId()
        );

        // beneficiaries outputs
        for (Beneficiary beneficiary : proposal.getBeneficiaries()) {
            LOG.info("beneficiary address: "+beneficiary.getAddress());
            proposalTransactionBuilder.addBeneficiary(
                    Address.fromBase58(conf.getNetworkParams(), beneficiary.getAddress()),
                    Coin.valueOf(beneficiary.getAmount())
            );
        }

        // build the transaction..
        Transaction tran = proposalTransactionBuilder.build();

        LOG.info("Transaction fee: " + tran.getFee());

        sendRequest = SendRequest.forTx(tran);

        sendRequest.signInputs = true;
        sendRequest.shuffleOutputs = false;
        sendRequest.coinSelector = new MyCoinSelector();


        // complete transaction
        try {
            wallet.completeTx(sendRequest);
        } catch (InsufficientMoneyException e) {
            LOG.error("Insuficient money exception",e);
            throw new InsuficientBalanceException("Insuficient money exception",e);
        } catch (Wallet.DustySendRequested e) {
            e.printStackTrace();
            LOG.error("DustySendRequest: "+sendRequest.tx+", wallet: "+wallet);
            throw new CantCompleteProposalException("DustySendRequestException");
        } catch (Wallet.ExceededMaxTransactionSize e){
            e.printStackTrace();
            LOG.error("ExceededMaxTransactionSize: "+sendRequest.tx+", wallet: "+wallet);
            throw new CantCompleteProposalMaxTransactionExcededException("ExceededMaxTransactionSize",e);
        }

        LOG.info("inputs value: " + tran.getInputSum().toFriendlyString() + ", outputs value: " + tran.getOutputSum().toFriendlyString() + ", fee: " + tran.getFee().toFriendlyString());
        LOG.info("total en el aire: " + tran.getInputSum().minus(tran.getOutputSum().minus(tran.getFee())).toFriendlyString());


        // now that the transaction is complete lock the output
        // lock address
        String parentTransactionHashHex = sendRequest.tx.getHash().toString();
        LOG.info("Locking transaction with 1000 IoPs: position: "+0+", parent hash: "+parentTransactionHashHex);
        proposal.setGenesisTxHash(parentTransactionHashHex);
        proposal.setLockedOutputIndex(lockedOutputPosition);
        lockedOutputHashHex = parentTransactionHashHex;
        lockedOutputPosition = 0;

    }


    public Transaction broadcast() throws NotConnectedPeersException,CantSendTransactionException {
        Wallet wallet = walletManager.getWallet();
        try {

            wallet.commitTx(sendRequest.tx);

            ListenableFuture<Transaction> future = blockchainManager.broadcastTransaction(sendRequest.tx.getHash().getBytes());

            Transaction tx = future.get(1,TimeUnit.MINUTES);

            if (sendRequest.tx.getHash().toString().equals("")) {
                LOG.error("something bad happen broadcast: "+tx.toString());
                new CantSendTransactionException("Tx hash is null, please send log");
            }

            LOG.info("TRANSACCION BROADCASTEADA EXITOSAMENTE, hash: "+sendRequest.tx.getHash().toString());

            return tx;

        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new CantSendTransactionException(e.getMessage());
        } catch (ExecutionException e) {
            e.printStackTrace();
            throw new CantSendTransactionException(e.getMessage());
        } catch (TimeoutException e) {
            e.printStackTrace();
            throw new NotConnectedPeersException(e);
        } catch (Exception e){
            e.printStackTrace();
            throw new CantSendTransactionException(e.getMessage());
        }
    }

    private void validateContract(Proposal contract) throws IllegalArgumentException {

        Proposal.validateVotingPeriod(contract.getVotingPeriod());
        Proposal.validateEndBlock(contract.getEndBlock());
        Proposal.validateBlockReward(contract.getBlockReward());
        validateBeneficiaries(contract);

    }

    private void validateBeneficiaries(Proposal contract){
        long beneficiariesAmount = 0;
        for (Beneficiary beneficiary : contract.getBeneficiaries()) {
            Proposal.validateBeneficiary(beneficiary);
            try{
                Address.fromBase58(conf.getNetworkParams(),beneficiary.getAddress());
            }catch (Exception e){
                throw new IllegalArgumentException("Address not valid: "+beneficiary.getAddress());
            }
            beneficiariesAmount+=beneficiary.getAmount();
        }
        if (beneficiariesAmount>contract.getBlockReward()){
            throw new IllegalArgumentException("Total amount of all beneficieries is higher than the block reward.\nPlease check the 'reward left' on the screen ");
        }else if (beneficiariesAmount<contract.getBlockReward()){
            throw new IllegalArgumentException("Total amount of all beneficieries is lower than the block reward.\n" + "Please check the 'reward left' on the screen ");
        }
    }


    public Proposal getUpdatedProposal() {
        return proposal;
    }

    public long getLockedBalance() {
        return lockedBalance;
    }

    public Transaction getTransaction() {
        return sendRequest.tx;
    }


    private class MyCoinSelector implements org.bitcoinj.wallet.CoinSelector {
        @Override
        public CoinSelection select(Coin coin, List<TransactionOutput> list) {
            return new CoinSelection(coin,new ArrayList<TransactionOutput>());
        }
    }

    public String getLockedOutputHashHex() {
        return lockedOutputHashHex;
    }

    public int getLockedOutputPosition() {
        return lockedOutputPosition;
    }
}
