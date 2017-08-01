package org.libertaria.world.blockchain.explorer;


import org.bitcoinj.core.Block;
import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.BloomFilter;
import org.bitcoinj.core.Context;
import org.bitcoinj.core.FilteredBlock;
import org.bitcoinj.core.GetDataMessage;
import org.bitcoinj.core.Message;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerAddress;
import org.bitcoinj.core.PeerFilterProvider;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.Utils;
import org.bitcoinj.core.listeners.PeerConnectedEventListener;
import org.bitcoinj.core.listeners.PeerDataEventListener;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.store.LevelDBBlockStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Created by mati on 17/12/16.
 * todo: ver que hace la clase "Blockchain" y saber si puedo sacarla y usar solo el blockstore.
 */

public class TransactionFinder implements PeerFilterProvider, PeerDataEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(TransactionFinder.class);

    private Context context;
    private PeerGroup peerGroup;
    private BlockStore blockStore;
    private BlockChain blockChain;

    private boolean reusePeers;

    private File blockStoreFile;

    private List<Sha256Hash> txHashes;
    private List<byte[]> outpoints;

    /** Clase para ir guardando las transacciones (sean tx,inputs,outputs) */
    private TransactionStorage transactionStorage;

    /** Listeners */
    private List<TransactionFinderListener> listeners;
    /** last best chain height requested, esto sirve para saber hasta donde tengo actualizada la lista con las propuestas */
    // todo: deberia guardar esto en algun lado..
    private String lastBestChainHash;

    public TransactionFinder(Context context,TransactionStorage finderStorage) {
        this.context = context;
        txHashes = new ArrayList<>();
        outpoints = new ArrayList<>();
        this.transactionStorage = finderStorage;
    }

    public TransactionFinder(Context context, PeerGroup peerGroup,TransactionStorage finderStorage) {
        this(context,finderStorage);
        this.peerGroup = peerGroup;
        reusePeers = true;
    }

    public void startDownload(){
        LOG.info("StartDownload");
        if (reusePeers){
            LOG.info("Reusing peers");
            startDownloadTxFromPeer();
        }else{
            startDownloadTx();

        }
    }

    private void startDownloadTxFromPeer() {
//        try {
//            blockStore = new LevelDBBlockStore(context, blockStoreFile);
            peerGroup.addPeerFilterProvider(this);
            peerGroup.addBlocksDownloadedEventListener(this);

//        } catch (BlockStoreException e) {
//            e.printStackTrace();
//        }
    }

    /**
     * todo: acordarse de que si tenemos un peergroup activo tenemos que usar ese y no armar uno nuevo con una conexión nueva.
     * todo: por lo cual seguramente deba modificar algo lo que hay hecho del peergroup o ver como integrarlo.
     */
    private void startDownloadTx(){
        try {
            File blockStoreFile = new File("blockstore_especial.dat");


            blockStore = new LevelDBBlockStore(context, blockStoreFile);
            blockChain = new BlockChain(context.getParams(), blockStore);
            peerGroup = new PeerGroup(context.getParams(), blockChain);

            //regtest
            peerGroup.addAddress(new PeerAddress(context.getParams(), new InetSocketAddress("192.168.0.111", 7685)));
            // testnet
//            peerGroup.addAddress(new PeerAddress(params,new InetSocketAddress("192.168.0.111",7475)));

            peerGroup.addConnectedEventListener(new PeerConnectedEventListener() {
                @Override
                public void onPeerConnected(Peer peer, int i) {
                    System.out.println("onPeerConnected: " + peer);
                }
            });

            peerGroup.addPeerFilterProvider(this);

            peerGroup.start();
            peerGroup.startBlockChainDownload(this);


        } catch (BlockStoreException e) {
            e.printStackTrace();
        }
    }

    public void addTx(Sha256Hash hash){
        txHashes.add(hash);
    }

    public void addTx(String hexHash){
        addTx(Sha256Hash.wrap(hexHash));
    }

    public void addWatchedOutpoint(Sha256Hash hash, int index,int dataLenght){
        outpoints.add(transactionStorage.saveWatchedOutput(hash,index,dataLenght));
    }

    public void setLastBestChainHash(String lastBestChainHash) {
        this.lastBestChainHash = lastBestChainHash;
        transactionStorage.saveLastBestChainHash(lastBestChainHash);
    }

    public void addWatchedOutpoint(String hash, int index, int dataLenght){
        addWatchedOutpoint(Sha256Hash.wrap(hash),index,dataLenght);
    }

    public void addTransactionFinderListener(TransactionFinderListener listener) {
        if (listeners==null)listeners = new ArrayList<>();
        listeners.add(listener);
    }

    public void removeTransactionFinderListener(TransactionFinderListener listener){
        if (listeners!=null){
            listeners.remove(listener);
        }
    }

    public boolean isWatched(Sha256Hash hash) {
        return txHashes.contains(hash);
    }

    public boolean isOutPointWatched(TransactionOutPoint outPoint) {
        byte[] data = TxUtils.serializeData(outPoint.getHash(), (int) outPoint.getIndex(),outPoint.getMessageSize());
        return outpoints.contains(data);
    }

    private void saveTx(Transaction tx) {
        transactionStorage.saveTx(tx);
    }


    private void watchedInputArrived(Transaction tx, int index, int lenght) {
        transactionStorage.markOutputSpend(tx,index,lenght);
    }

    public List<Transaction> getWatchedTransactions() {
        return transactionStorage.getTransactions();
    }

    public String getLastBestChainHash(){
        return transactionStorage.getLastBestChainHash();
    }

    // ********************************************** BLOOM FILTER REGION **********************************************

    /**
     *
     * @return
     */
    @Override
    public long getEarliestKeyCreationTime() {
        return Utils.currentTimeSeconds();
    }

    @Override
    public void beginBloomFilterCalculation() {

    }

    @Override
    public int getBloomFilterElementCount() {
        return txHashes.size();
    }

    @Override
    public BloomFilter getBloomFilter(int size, double falsePositiveRate, long nTweak) {
        BloomFilter bloomFilter = new BloomFilter(size,falsePositiveRate,nTweak, BloomFilter.BloomUpdate.UPDATE_ALL);
        for (Sha256Hash txHash : txHashes) {
            bloomFilter.insert(Utils.reverseBytes(txHash.getBytes()));
        }
        for (byte[] outpoint : outpoints) {
            bloomFilter.insert(outpoint);
        }
        return bloomFilter;
    }

    @Override
    public boolean isRequiringUpdateAllBloomFilter() {
        return true;
    }

    @Override
    public void endBloomFilterCalculation() {

    }


    // ********************************************** END BLOOM FILTER REGION **********************************************



    // ********************************************** BLOCK LISTENER REGION **********************************************

    @Override
    public void onBlocksDownloaded(Peer peer, Block block, FilteredBlock filteredBlock, int blocksLeft) {
        LOG.info("onBlocksDownloaded: " + blocksLeft);

        if (block.getHash().toString().equals(lastBestChainHash)){
            // acá deberia cambiar pedir las nuevas transacciones de propuesta, eliminar el ultimo bloque y cambiar el filtro para recibir las transacciones de ese bloque
            LOG.info("block == lastBestChainHash");
        }


        if (filteredBlock!=null){
//                        System.out.println("Filtered block: "+filteredBlock);
            for (Map.Entry<Sha256Hash, Transaction> hashTransactionEntry : filteredBlock.getAssociatedTransactions().entrySet()) {

                Sha256Hash hash = hashTransactionEntry.getKey();
                Transaction tx = hashTransactionEntry.getValue();

                if (reusePeers){
                    boolean isTxSaved = false;
                    // chequeo si el hash de la transacción si es un falso positivo, una transacción del peer o es mia
                    if (isWatched(hash)){
                        LOG.info("##### Tx watched by me!!!");
                        saveTx(tx);
                        isTxSaved = true;
                        // notify
                        for (TransactionFinderListener listener : listeners) {
                            listener.onReceiveTransaction(tx);
                        }
                    }
                    // chequeo si algun input es mio
                    for (TransactionInput transactionInput : tx.getInputs()) {
                        TransactionOutPoint outPoint = transactionInput.getOutpoint();
                        if (isOutPointWatched(outPoint)){
                            LOG.info("##### Outpoint spended and is watched by me!!!");
                            watchedInputArrived(tx,(int) outPoint.getIndex(),outPoint.getMessageSize());
                        }
                    }

                    // chequeo si algun output es mio --> todo: esto no creo que sea necesario si solamentese va a usar la wallet en la app y no se va a poder exportar por el momento..
//                    for (TransactionOutput transactionOutput : tx.getOutputs()) {
//                        TransactionOutPoint outPoint = transactionOutput.getOutPointFor();
//                        if (!isTxSaved && isOutPointWatched(outPoint)){
//                            saveTx(tx);
//                        }
//                    }

                }

            }

        }
    }


    @Override
    public void onChainDownloadStarted(Peer peer, int blocksLeft) {
        System.out.println("onChainDownloadStarted: number " + blocksLeft);
    }

    @Override
    public List<Message> getData(Peer peer, GetDataMessage m) {
        return null;
    }

    @Override
    public Message onPreMessageReceived(Peer peer, Message m) {
        return m;
    }



    // ********************************************** END BLOCK LISTENER REGION **********************************************


}
