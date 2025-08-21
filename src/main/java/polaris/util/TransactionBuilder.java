/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package polaris.util;

import com.google.common.io.ByteStreams;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javax.annotation.Nullable;
import static junit.framework.Assert.assertNotSame;
import org.bitcoinj.base.Address;
import org.bitcoinj.base.BitcoinNetwork;
import static org.bitcoinj.base.Coin.FIFTY_COINS;
import org.bitcoinj.core.AbstractBlockChain;
import org.bitcoinj.core.Block;
import static org.bitcoinj.core.Block.BLOCK_HEIGHT_UNKNOWN;
import org.bitcoinj.core.BlockChain;
import org.bitcoinj.base.Coin;
import static org.bitcoinj.base.Coin.CENT;
import static org.bitcoinj.base.Coin.COIN;
import static org.bitcoinj.base.Coin.valueOf;
import org.bitcoinj.base.ScriptType;
import org.bitcoinj.base.internal.StreamUtils;
import org.bitcoinj.base.internal.TimeUtils;
import org.bitcoinj.core.Context;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PeerAddress;
import org.bitcoinj.core.ProtocolException;
import org.bitcoinj.core.StoredBlock;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicHierarchy;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.ECKey;
import org.bitcoinj.crypto.HDPath;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.protobuf.wallet.Protos;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.store.SPVBlockStore;
import org.bitcoinj.utils.Threading;
import org.bitcoinj.wallet.BasicKeyChain;
import org.bitcoinj.wallet.DeterministicKeyChain;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.WalletProtobufSerializer;
import org.bitcoinj.wallet.WalletTransaction;
import org.junit.Assert;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author michael
 */
public class TransactionBuilder {

    private static NetworkParameters params = MainNetParams.get();

    private static ECKey coinbaseOutKey = new ECKey();
    private static byte[] coinbaseOutKeyPubKey;

    private BlockStore blockStore;

    private Wallet wallet;
    private BlockChain blockChain;
    private Address coinbaseTo;

    private File walletFile;

    private File chainFile;

    private BitcoinNetwork net = BitcoinNetwork.MAINNET;

    public TransactionBuilder(File wFile, File sFile,
            Block b1 // receive Block
    ) throws UnreadableWalletException, BlockStoreException {

        Context.propagate(new Context(100, Coin.ZERO, false, false));

        this.walletFile = wFile;
        this.chainFile = sFile;

        this.wallet = Wallet.loadFromFile(walletFile);

        this.blockStore = new SPVBlockStore(params, chainFile);

        StoredBlock sb2 = this.blockStore.getChainHead().build(b1);

        this.blockStore.put(sb2);
        this.blockStore.setChainHead(sb2);
    }

    public Block makeSolvedFXBlock(Address coinsTo) throws BlockStoreException {

        Block b = blockStore.getChainHead().getHeader().createNextBlock(coinsTo);
        b.solve();
        return b;

    }

    public static Block makeSolvedBlockWithCoinBase(Address to,
            List<Transaction> transactions) {

        coinbaseOutKey = new ECKey();
//
        coinbaseOutKeyPubKey = coinbaseOutKey.getPubKey();

        //create b2 from prevBlock
        Block b2 = params.getGenesisBlock().createNextBlockWithCoinbase(Block.BLOCK_VERSION_GENESIS,
                coinbaseOutKeyPubKey, FIFTY_COINS, BLOCK_HEIGHT_UNKNOWN);

        for (Transaction tx : transactions) {
            b2.addTransaction(tx);
        }
        System.out.printf(" \n coins to block  \n %s", b2.toString());

//        StoredBlock build = prevBlock.build(b2);
        return b2;

    }

    public static Block makeSolvedBlockV2(
            Block prev,
            @Nullable Address to,
            String blockId,
            Transaction... transactions
    ) throws BlockStoreException {

        Block b = prev.createNextBlock(to);
        // Coinbase tx already exists.
        for (Transaction tx : transactions) {

            tx.getConfidence().setSource(TransactionConfidence.Source.NETWORK);
            b.addTransaction(tx);

        }

        System.out.printf(" \n **** \n done creationg  block %s", LocalDateTime.now().toString());

        try {
            serializeBlockAndSaveToFile(b, blockId);

            System.out.printf(" \n **** \n done saving block to File   %s", "binanceBlock");

            return b;

        } catch (IOException ex) {
            Logger.getLogger(TransactionBuilder.class.getName()).log(Level.SEVERE, null, ex);

            throw new RuntimeException(ex);
        }

    }

    public static void serializeBlockAndSaveToFile(Block block, String name) throws FileNotFoundException, IOException {
//        Block testBlock = params.getGenesisBlock();

        FileOutputStream outputStream = new FileOutputStream(new File(name + ".dat"));
        block.serialize();
    }

    public static Block loadBlockFromFile(String fileName) throws FileNotFoundException, IOException, ProtocolException {

        //"/home/michael/NetBeansProjects/btcJFX/genesis.dat"
        FileInputStream fileInputStream = new FileInputStream(fileName + ".dat");
        byte[] blockG = ByteStreams.toByteArray(fileInputStream);
        Block block = params.getDefaultSerializer().makeBlock(ByteBuffer.wrap(blockG));

        Assert.assertArrayEquals(blockG, block.serialize());

        return block;
    }

    public Block makeSolvedBlock(Block prev, String blockId, Transaction... transactions) throws BlockStoreException {
        return makeSolvedBlockV2(prev, null, blockId, transactions);
    }

    public Block makeSolvedBlockV3(BlockStore store, int height, String blockId, Transaction... transactions) throws BlockStoreException {

        Block prev = store.getChainHead().getHeader();

        Block b = prev.createNextBlock(null, Block.BLOCK_VERSION_GENESIS,
                TimeUtils.currentTime(),
                height);

        // Coinbase tx already exists.
        for (Transaction tx : transactions) {

            tx.getConfidence().setSource(TransactionConfidence.Source.NETWORK);
            b.addTransaction(tx);

        }

        StoredBlock sb = store.getChainHead().build(b);
        blockStore.put(sb);
        blockStore.setChainHead(sb);

        System.out.printf(" \n **** \n done creationg  block %s", LocalDateTime.now().toString());

        try {
            serializeBlockAndSaveToFile(b, blockId);

            System.out.printf(" \n **** \n done saving block to File   %s", "binanceBlock");

            return b;

        } catch (IOException ex) {
            Logger.getLogger(TransactionBuilder.class.getName()).log(Level.SEVERE, null, ex);

            throw new RuntimeException(ex);
        }

    }

    public static List<Block> loadSpendableBlock() {

        // load spendable  blocks  form database
        return null;

    }

    public void spendUnconfirmedChange(Transaction t2,
            
            // receive block
            StoredBlock sb
    
    ) throws Exception {
        if (wallet.getTransactionSigners().size() == 1) // don'prevTx bother reconfiguring the p2sh wallet
        {

//            throw new RuntimeException("tx signer is 1");
            wallet = roundTrip(wallet);
        }
        
        
//        //increase the fee
//
//        Coin v3 = t2.getOutput(0).getValue();
//
//        ECKey ecKey = new ECKey();
//
//        Address dest = ecKey.toAddress(ScriptType.P2PKH, net);
//        SendRequest req = SendRequest.to(dest, v3);
//        req.shuffleOutputs = false;
//        wallet.completeTx(req);
//        Transaction t3 = req.tx;
//        assertNotSame(t2.getOutput(0).getScriptPubKey().getToAddress(net),
//                t3.getOutput(0).getScriptPubKey().getToAddress(net));
//
//        assertNotNull(t3);
//        wallet.commitTx(t3);
//        assertTrue(wallet.isConsistent());
        // t2 and t3 gets confirmed in the same block.
//        sendMoneyToWallet(AbstractBlockChain.NewBlockType.BEST_CHAIN, t2, t3);
 
            }
    
    
    
    
    public void confirmUnonfirmedChange( StoredBlock sb,Transaction tx1,Transaction tx2) throws BlockStoreException{
               Block b2 = makeSolvedBlock(params.getGenesisBlock(), "confirm_tx_with_existing_inputs", tx1, tx2);

        StoredBlock sb2 = sb.build(b2);

        receiveFromBlock(sb2);

        assertTrue(wallet.isConsistent());
    }

    private void receiveFromBlock(StoredBlock sb) {
        sb.getHeader().getTransactions().forEach(t -> {

            System.out.println(t.toString());
            if (wallet.isTransactionRelevant(t)) {
                wallet.receiveFromBlock(t, sb, AbstractBlockChain.NewBlockType.BEST_CHAIN, sb.getHeight());

            }
        });

        wallet.notifyNewBestBlock(sb);
//

        saveWallet(walletFile);

//        System.out.printf("\n %s", wallet.toString());
//        blockChain.add(sb1.getHeader());
//        blockStore.put(sb2);
//        blockStore.put(sb3);
//        blockStore.setChainHead(sb3);
//        System.out.printf(" \n %s", wallet.toString());
    }

    public void testCompleteTxWithExistingInputs() throws Exception {
        final Context context = new Context(params);
        // Tests calling completeTx with a SendRequest that already has a few inputs in it
//        // Generate a few outputs to us
        final Address OTHER_ADDRESS = Address.fromKey(params, coinbaseOutKey, ScriptType.P2PKH);
//
//
//        Wallet wallet = Wallet.createDeterministic(context, ScriptType.P2PKH);
//
//        Address myAddress = wallet.currentReceiveAddress();
//
//        Block block = makeSolvedFXBlock(OTHER_ADDRESS);
//
//        StoredBlock sb = new StoredBlock(block, BigInteger.ONE, 1);
//
//        serializeBlockAndSaveToFile(block, "complete_tx_with_existing_inputs");

        Context.propagate(context);
        Block block = loadBlockFromFile("complete_tx_with_existing_inputs.dat");

        Block.verifyHeader(block);

        StoredBlock genesis = blockStore.getChainHead();

        StoredBlock sb = genesis.build(block);

        SPVBlockStore spvBlockStore = new SPVBlockStore(params, chainFile);

        spvBlockStore.put(genesis);
        spvBlockStore.setChainHead(genesis);

        spvBlockStore.put(sb);
        spvBlockStore.setChainHead(sb);

//     new StoredBlock(block, BigInteger.ONE, 1);
        Wallet wallet = Wallet.createDeterministic(params, ScriptType.P2PKH);

        Address myAddress = wallet.currentReceiveAddress();

        Transaction tx1 = createFakeTxWithChangeAddress(COIN, myAddress, randomAddress());
        wallet.receiveFromBlock(tx1, sb, AbstractBlockChain.NewBlockType.BEST_CHAIN, 0);

        System.out.printf("\n receive one %s \n", wallet.getBalance().toFriendlyString());

        Transaction tx2 = createFakeTxWithChangeAddress(COIN, myAddress, randomAddress());
        assertNotSame(tx1.getTxId(), tx2.getTxId());
        wallet.receiveFromBlock(tx2, sb, AbstractBlockChain.NewBlockType.BEST_CHAIN, 1);

        System.out.printf("\n receive two %s \n", wallet.getBalance().toFriendlyString());

        Transaction tx3 = createFakeTxWithChangeAddress(CENT, myAddress, randomAddress());
        wallet.receiveFromBlock(tx3, sb, AbstractBlockChain.NewBlockType.BEST_CHAIN, 2);

        System.out.printf("\n receive three %s \n", wallet.getBalance().toFriendlyString());

        SendRequest request1 = SendRequest.to(OTHER_ADDRESS, CENT);
        // If we just complete as-is, we will use one of the COIN outputs to get higher priority,
        // resulting in a change output
        request1.shuffleOutputs = false;
        wallet.completeTx(request1);

        final Coin txFee = request1.tx.getFee();

        assertEquals(1, request1.tx.getInputs().size());
        assertEquals(2, request1.tx.getOutputs().size());
        assertEquals(CENT, request1.tx.getOutput(0).getValue());
        assertEquals(COIN.subtract(CENT).subtract(txFee), request1.tx.getOutput(1).getValue());

        Coin coin2 = CENT.minus(Transaction.DEFAULT_TX_FEE).plus(Coin.parseCoin("0.00084"));

        // Now create an identical request2 and add an unsigned spend of the CENT output
        SendRequest request2 = SendRequest.to(OTHER_ADDRESS, coin2);
        request2.tx.addInput(tx3.getOutput(0));

        // Now completeTx will result in one input, one output
        wallet.completeTx(request2);

        assertEquals(1, request2.tx.getInputs().size());
        assertEquals(1, request2.tx.getOutputs().size());
        assertEquals(coin2, request2.tx.getOutput(0).getValue());
//         Make sure it was properly signed

        request2.tx.getInput(0).getScriptSig().correctlySpends(
                request2.tx, 0, null, null, tx3.getOutput(0).getScriptPubKey(), Script.ALL_VERIFY_FLAGS);

        final Coin coin3 = CENT.minus(Transaction.DEFAULT_TX_FEE).plus(Coin.parseCoin("0.00077")
                .plus(Coin.parseCoin("0.000006")));

        // However, if there is no connected output, we connect it
        SendRequest request3 = SendRequest.to(OTHER_ADDRESS, coin3);
//      
//        // Now completeTx will find the matching UTXO from the wallet and add its value to the unconnected input
//   
//        
//                request3.tx.addInput(new TransactionInput(params, request3.tx, new byte[]{},op ));

        request3.shuffleOutputs = false;

//        CoinSelector coinSelector = BtcPostgres.buildCoinSelector(tx3);
//               };
//        request3.coinSelector = coinSelector;
        wallet.completeTx(request3);

        //        request3.tx.addInput(new TransactionInput(params, request3.tx, new byte[]{}, new TransactionOutPoint(params, 0, tx3.getTxId())));
        assertEquals(1, request3.tx.getInputs().size());
        assertEquals(1, request3.tx.getOutputs().size());
//        assertEquals(coin3, request3.tx.getOutput(0).getValue());

        System.out.printf("\n \n  \n \n \n  \n \n *************** \n \n \n");

        SendRequest request4 = SendRequest.to(OTHER_ADDRESS, CENT.minus(Transaction.DEFAULT_TX_FEE).plus(Coin.parseCoin("0.000733")));
        request4.tx.addInput(tx3.getOutput(0));
        // Now if we manually sign it, completeTx will not replace our signature
        wallet.signTransaction(request4);
        byte[] scriptSig = request4.tx.getInput(0).getScriptBytes();
        wallet.completeTx(request4);
        assertEquals(1, request4.tx.getInputs().size());
        assertEquals(1, request4.tx.getOutputs().size());
//        assertEquals(CENT, request4.tx.getOutput(0).getValue());
        assertArrayEquals(scriptSig, request4.tx.getInput(0).getScriptBytes());

//        
        broadcastAndCommit(wallet, request4.tx);

        wallet.saveToFile(new File("polaris.wallet"));

        System.out.println(wallet.toString());
    }

    private static void broadcastAndCommit(Wallet wallet, Transaction t) throws Exception {
        final LinkedList<Transaction> txns = new LinkedList<>();
        wallet.addCoinsSentEventListener((wallet1, tx, prevBalance, newBalance) -> txns.add(tx));

        t.getConfidence().markBroadcastBy(PeerAddress.simple(InetAddress.getByAddress(new byte[]{1, 2, 3, 4}), params.getPort()));
        t.getConfidence().markBroadcastBy(PeerAddress.simple(InetAddress.getByAddress(new byte[]{10, 2, 3, 4}), params.getPort()));
        wallet.commitTx(t);
        Threading.waitForUserCode();
        assertEquals(1, wallet.getPoolSize(WalletTransaction.Pool.PENDING));
        assertEquals(1, wallet.getPoolSize(WalletTransaction.Pool.SPENT));
        assertEquals(2, wallet.getTransactions(true).size());
        assertEquals(t, txns.getFirst());
        assertEquals(1, txns.size());
    }

    /**
     * Create a fake TX of sufficient realism to exercise the unit tests. Two
     * outputs, one to us, one to somewhere else to simulate change. There is
     * one random input.
     */
    public static Transaction createFakeTxWithChangeAddress(Coin value, Address to, Address changeOutput) {
        Transaction t = new Transaction(params);
        TransactionOutput outputToMe = new TransactionOutput(t, value, to);
        t.addOutput(outputToMe);
        TransactionOutput change = new TransactionOutput(t, valueOf(1, 11), changeOutput);
        t.addOutput(change);
        // Make a previous tx simply to send us sufficient coins. This prev tx is not really valid but it doesn't
        // matter for our purposes.
        Transaction prevTx = new Transaction(params);
        TransactionOutput prevOut = new TransactionOutput(prevTx, value, to);
        prevTx.addOutput(prevOut);
        // Connect it.
        TransactionInput in = t.addInput(prevOut);

        in.withScriptSig(ScriptBuilder.createInputScript(TransactionSignature.dummy()));
        replaceInput(t.getInputs().size() - 1, in, in.getParentTransaction());
        // Fake signature.
        // Serialize/deserialize to ensure internal state is stripped, as if it had been read from the wire.
        return roundTripTransaction(t);
    }

    public static void replaceInput(int index, TransactionInput input, Transaction parent) {

        TransactionInput oldInput = parent.getInputs().get(index);

//        oldInput.setParent(null);
//        input.setParent(parent);
        parent.clearInputs();

        parent.addInput(input);
//        invalidateCachedTxIds();
    }

    public static Transaction roundTripTransaction(Transaction tx) {
        return Transaction.read(ByteBuffer.wrap(tx.serialize()));
    }

    public static Address randomAddress() {
        return Address.fromKey(params, randomKey(), ScriptType.P2PKH);
    }

    private static ECKey randomKey() {
        return new ECKey();
    }

    private void exitNow() {
        if (true) {
            System.exit(0);
        }
    }

    public void saveWallet(File walletFile) {
        try {
            // This will save the new state of the wallet to a temp file then rename, in case anything goes wrong.
            wallet.saveToFile(walletFile);
        } catch (IOException e) {
            System.err.println("Failed to save wallet! Old wallet should be left untouched.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public Wallet getWallet() {
        return this.wallet;
    }

    public BlockStore getStore() {
        return this.blockStore;
    }

    public void quit() {
        Platform.exit();
    }

    private Wallet roundTrip(Wallet wallet) throws UnreadableWalletException {

        int numActiveKeyChains = wallet.getActiveKeyChains().size();

        DeterministicKeyChain activeKeyChain = wallet.getActiveKeyChain();

        HDPath accountPath = activeKeyChain.getAccountPath();

        DeterministicKey rootKey = wallet.getActiveKeyChain().getRootKey();
        
        

        DeterministicHierarchy dh = new DeterministicHierarchy(rootKey);

        int numIssuedInternal = activeKeyChain.getIssuedInternalKeys();
        int numIssuedExternal = activeKeyChain.getIssuedExternalKeys();

        DeterministicKey internalParentKey = dh.deriveChild(accountPath, false, true, ChildNumber.ONE);

        DeterministicKey externalparentKey = dh.deriveChild(accountPath, false, true, ChildNumber.ZERO);

        int numKeys = getKeys(
                false,
                true,
                accountPath,
                numIssuedInternal,
                numIssuedExternal,
                internalParentKey,
                externalparentKey).size();

        DeterministicKey watchingKey = activeKeyChain.getWatchingKey();
        ScriptType outputScriptType = activeKeyChain.getOutputScriptType();

        Protos.Wallet protos = new WalletProtobufSerializer().walletToProto(wallet);
        Wallet roundTrippedWallet = new WalletProtobufSerializer().readWallet(net, null, protos);

        assertEquals(numActiveKeyChains, roundTrippedWallet.getActiveKeyChains().size());
        DeterministicKeyChain roundTrippedActiveKeyChain = roundTrippedWallet.getActiveKeyChain();
//        assertEquals(numKeys, getKeys(false, true).size(),);

        assertEquals(numKeys, getKeys(
                false,
                true,
                accountPath,
                numIssuedInternal,
                numIssuedExternal,
                internalParentKey,
                externalparentKey).size());

        assertEquals(numIssuedInternal, roundTrippedActiveKeyChain.getIssuedInternalKeys());
        assertEquals(numIssuedExternal, roundTrippedActiveKeyChain.getIssuedExternalKeys());
        assertEquals(rootKey, roundTrippedWallet.getActiveKeyChain().getRootKey());
        assertEquals(watchingKey, roundTrippedActiveKeyChain.getWatchingKey());
        assertEquals(accountPath, roundTrippedActiveKeyChain.getAccountPath());
        assertEquals(outputScriptType, roundTrippedActiveKeyChain.getOutputScriptType());
        return roundTrippedWallet;
    }

    /* package */
    List<DeterministicKey> getKeys(boolean includeLookahead, boolean includeParents,
            HDPath path,
            int issuedInternalKeys,
            int issuedExternalKeys,
            DeterministicKey internalParentKey,
            DeterministicKey externalParentKey
    ) {
        return getKeys(filterKeys(includeLookahead, includeParents,
                path,
                issuedInternalKeys,
                issuedExternalKeys,
                internalParentKey,
                externalParentKey
        ));
    }

    private Predicate<DeterministicKey> filterKeys(
            boolean includeLookahead,
            boolean includeParents,
            HDPath path,
            int issuedInternalKeys,
            int issuedExternalKeys,
            DeterministicKey internalParentKey,
            DeterministicKey externalParentKey
    ) {
        Predicate<DeterministicKey> keyFilter;
        if (!includeLookahead) {
            int treeSize = path.size();
            keyFilter = key -> {
                DeterministicKey parent = key.getParent();
                return !((!includeParents && parent == null)
                        || (!includeParents && key.getPath().size() <= treeSize)
                        || (internalParentKey.equals(parent) && key.getChildNumber().i() >= issuedInternalKeys)
                        || (externalParentKey.equals(parent) && key.getChildNumber().i() >= issuedExternalKeys));
            };
        } else {
            // TODO includeParents is ignored here
            keyFilter = key -> true;
        }
        return keyFilter;
    }

    private List<DeterministicKey> getKeys(Predicate<DeterministicKey> keyFilter) {

        return wallet.getActiveKeyChains().stream()
                .map(kc -> kc.getRootKey())
                .filter(keyFilter)
                .collect(StreamUtils.toUnmodifiableList());
    }

}
