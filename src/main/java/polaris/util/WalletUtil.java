package polaris.util;

/*
     * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
     * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.protobuf.ByteString;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.channels.FileLock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.application.Platform;
import javax.annotation.Nullable;
import org.bitcoinj.base.Address;
import org.bitcoinj.base.Base58;
import org.bitcoinj.base.BitcoinNetwork;
import org.bitcoinj.base.Coin;
import static org.bitcoinj.base.Coin.COIN;
import static org.bitcoinj.base.Coin.ZERO;
import static org.bitcoinj.base.Coin.parseCoin;
import static org.bitcoinj.base.Coin.valueOf;
import org.bitcoinj.base.Network;
import org.bitcoinj.base.ScriptType;
import org.bitcoinj.base.exceptions.AddressFormatException;
import org.bitcoinj.base.internal.ByteUtils;
import org.bitcoinj.base.internal.TimeUtils;
import org.bitcoinj.core.AbstractBlockChain;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.CheckpointManager;
import org.bitcoinj.core.Context;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PeerAddress;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.StoredBlock;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionBroadcast;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.core.listeners.DownloadProgressTracker;
import org.bitcoinj.crypto.AesKey;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.DumpedPrivateKey;
import org.bitcoinj.crypto.ECKey;
import org.bitcoinj.crypto.HDPath;
import org.bitcoinj.crypto.KeyCrypterException;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.protobuf.wallet.Protos;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.store.MemoryFullPrunedBlockStore;
import org.bitcoinj.store.SPVBlockStore;
import org.bitcoinj.testing.FakeTxBuilder;
import static org.bitcoinj.testing.FakeTxBuilder.createFakeTxWithChangeAddress;
import static org.bitcoinj.testing.FakeTxBuilder.roundTripTransaction;
import org.bitcoinj.utils.BriefLogFormatter;
import org.bitcoinj.utils.Threading;
import static org.bitcoinj.utils.Threading.USER_THREAD;
import org.bitcoinj.wallet.CoinSelector;
import org.bitcoinj.wallet.DeterministicKeyChain;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.KeyChainGroupStructure;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.WalletProtobufSerializer;
import org.bitcoinj.wallet.WalletTransaction;
import org.bitcoinj.wallet.listeners.WalletCoinsReceivedEventListener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import static org.junit.Assert.assertTrue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author michael
 */
public class WalletUtil extends AbstractIdleService implements   Closeable {

    private static final Logger log = LoggerFactory.getLogger(WalletUtil.class);

    private static BlockStore store;
    private static AbstractBlockChain chain;
    private static PeerGroup peerGroup;
    private Wallet wallet;
    private Wallet fdw;
    private static org.bitcoinj.protobuf.payments.Protos.PaymentRequest paymentRequest;

    private final boolean force = false;
    private String seedStr;

    private String watchKeyStr;

    private final ScriptType outputScriptType = ScriptType.P2PKH;

    private String password;

    private final File chainFile;

    private File walletFile;

    private final BitcoinNetwork net = BitcoinNetwork.MAINNET;

    private final NetworkParameters params = NetworkParameters.of(net);

    private String peersStr;

    //    private final WalletTool.Filter filter = WalletTool.Filter.SERVER;
    private static final String wPrefix = "MainNetEc";

    private boolean dumpPrivKeys = true;

    private boolean dumpLookAhead = true;

    private Address toAddress;

    private Filter filter = Filter.SERVER;
    private String privKeyStr;
    private String pubKeyStr;

    private Long unixtime = null;
    private LocalDate date;
    private static File feederWallet;
    private MemoryFullPrunedBlockStore blockStore;

    @Override
    public void startUp() throws Exception {
       setup();
    }

    @Override
    protected void shutDown() throws Exception {
        
        
            // Runs in a separate thread.
        try {
            peerGroup.stop();
            wallet.saveToFile(walletFile);
            store.close();

            peerGroup = null;
            wallet = null;
            store = null;
            chain = null;
        } catch (BlockStoreException e) {
            throw new IOException(e);
        }
    }



    record Pair(String a, Predicate<Transaction> pr) {

    }

    record PairData(Object a, Address b) {

    }

    //    private final Runnable onChange;
    //
    public WalletUtil() throws URISyntaxException, UnreadableWalletException {

//        password = "so@Right626J";
////
//        walletFile = new File(getClass().getResource("/MainNetEcF.wallet").toURI());
//        chainFile = new File("/home/michael/NetBeansProjects/btcJFX/MainNetEcF.spvchain");
//        
//        
            walletFile = new File("/home/michael/NetBeansProjects/btcJFX/polaris.wallet");

            chainFile = new File("/home/michael/NetBeansProjects/btcJFX/polaris-s2.spvchain");
    }

    public enum WaitForEnum {
        EVER,
        WALLET_TX,
        BLOCK,
        BALANCE
    }

    public enum Filter {
        NONE,
        SERVER, // bloom filter
    }

    public static class Condition {

        public enum Type {
            // Less than, greater than, less than or equal, greater than or equal.
            EQUAL, LT, GT, LTE, GTE
        }
        Type type;
        String value;

        public Condition(String from) {
            if (from.length() < 2) {
                throw new RuntimeException("Condition string too short: " + from);
            }

            if (from.startsWith("<=")) {
                type = Type.LTE;
            } else if (from.startsWith(">=")) {
                type = Type.GTE;
            } else if (from.startsWith("<")) {
                type = Type.LT;
            } else if (from.startsWith("=")) {
                type = Type.EQUAL;
            } else if (from.startsWith(">")) {
                type = Type.GT;
            } else {
                throw new RuntimeException("Unknown operator in condition: " + from);
            }

            String s;
            switch (type) {
                case LT:
                case GT:
                case EQUAL:
                    s = from.substring(1);
                    break;
                case LTE:
                case GTE:
                    s = from.substring(2);
                    break;
                default:
                    throw new RuntimeException("Unreachable");
            }
            value = s;

            System.out.print("condition is -> " + s);

        }

        public boolean matchBitcoins(Coin comparison) {
            try {
                Coin units = parseCoin(value);

                switch (type) {
                    case LT:
                        return comparison.compareTo(units) < 0;
                    case GT:
                        return comparison.compareTo(units) > 0;
                    case EQUAL:
                        return comparison.compareTo(units) == 0;
                    case LTE:
                        return comparison.compareTo(units) <= 0;
                    case GTE:
                        return comparison.compareTo(units) >= 0;
                    default:
                        throw new RuntimeException("Unreachable");
                }

            } catch (NumberFormatException e) {
                System.err.println("Could not parse value from condition string: " + value);
                System.exit(1);
                return false;
            }

        }
    }

    private static Condition condition;

    private final WalletCoinsReceivedEventListener coinsReceivedListener = this::coinForwardingListener;

    public void main() throws FileNotFoundException, IOException, UnreadableWalletException, BlockStoreException {

        Context.propagate(new Context());
        wallet = Wallet.loadFromFile(walletFile);
        store = new SPVBlockStore(params, chainFile);
//        chain = new BlockChain(params, wallet, store);
        
       

//       //        
        BriefLogFormatter.initVerbose();
        log.info("Starting up ...");
//
 
            


       

//            syncChain();
        System.out.println(wallet.toString());

        System.exit(0);



        
        
//            Transaction tx = fdw.getPendingTransactions().stream().findFirst().get();
//            
//   
//        final LinkedList<Transaction> txns = new LinkedList<>();
//        
//            wallet.addCoinsSentEventListener((wallet1, t, prevBalance, newBalance) -> txns.add(t));
//        tx.getConfidence().markBroadcastBy(PeerAddress.simple(InetAddress.getByAddress(new byte[]{1, 2, 3, 4}), params.getPort()));
//        tx.getConfidence().markBroadcastBy(PeerAddress.simple(InetAddress.getByAddress(new byte[]{10, 2, 3, 4}), params.getPort()));
//        
//
//
//
//            wallet.receivePending(tx, null);
//            final TransactionOutput output = tx.getOutput(1);
//            
//            System.out.println(output.getValue().toFriendlyString());
//
//            Coin outAmount = Coin.parseCoin("0.0001");
////
//            final Coin inAmount = tx.getOutput(1).getValue();
////
//            final Address toAddr = new ECKey().toAddress(ScriptType.P2WPKH, net);
////
//            ECKey fromKey = new ECKey();
//            Address fromAddress = fromKey.toAddress(ScriptType.P2WPKH, net);
//            Transaction t = new Transaction();
////            TransactionOutPoint outPoint = new TransactionOutPoint(0, tx.getTxId());
//
//            t.addOutput(outAmount, toAddr);
//            t.addOutput(Coin.parseCoin("0.0004"), wallet.freshReceiveAddress());
//            
//            t.addInput(output);
//            
//            System.out.println(t.toString());
//            
//            SendRequest req = SendRequest.forTx(t);
//             wallet.signTransaction(SendRequest.forTx(t));
////            forTx.
//             
//               wallet.completeTx(req);
//             
//
//        // Commit t3, so the coins from the pending t2 are spent
//        wallet.commitTx(t);
//        
//        wallet.saveToFile(walletFile);
        
        
            
//                    TransactionInput input = t.addSignedInput(outPoint, ScriptBuilder.createOutputScript(fromAddress), inAmount, fromKey);
//
//
//                       // verify signature
//        input.getScriptSig().correctlySpends(tx, 0, input.getWitness(), input.getValue(),
//                ScriptBuilder.createOutputScript(fromAddress), null);
//
//        byte[] rawTx = tx.serialize();

//        assertNotNull(rawTx);


  


    }

    
    
    
    
    private void receiveATransactionAmount(Wallet wallet, Address toAddress, Coin amount) throws VerificationException, BlockStoreException {
        final CompletableFuture<Coin> availFuture = wallet.getBalanceFuture(amount, Wallet.BalanceType.AVAILABLE);
        final CompletableFuture<Coin> estimatedFuture = wallet.getBalanceFuture(amount, Wallet.BalanceType.ESTIMATED);
        assertFalse(availFuture.isDone());
        assertFalse(estimatedFuture.isDone());
        // Send some pending coins to the wallet.
        Transaction t1 = sendMoneyToWallet(wallet, null, amount, toAddress);
        CompletableFuture<String> f = CompletableFuture.supplyAsync(() -> t1.toString(), USER_THREAD);
        f.join();
        System.out.println("done");
        

        final CompletableFuture<TransactionConfidence> depthFuture = wallet.waitForConfirmations(t1, 1);
        assertFalse(depthFuture.isDone());
        assertEquals(Coin.parseCoin("0.011"), wallet.getBalance(Wallet.BalanceType.AVAILABLE));
        assertEquals(Coin.parseCoin("0.611"), wallet.getBalance(Wallet.BalanceType.ESTIMATED));
        assertFalse(availFuture.isDone());
        // Our estimated balance has reached the requested level.
        assertTrue(estimatedFuture.isDone());
        assertEquals(2, wallet.getPoolSize(WalletTransaction.Pool.PENDING));
        assertEquals(1, wallet.getPoolSize(WalletTransaction.Pool.UNSPENT));
//        // Confirm the coins.
                System.out.println("done2");
                
                
                 if (wallet.isPendingTransactionRelevant(t1)) {
                    wallet.receivePending(t1, null);
            try {
                wallet.saveToFile(walletFile);
            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(WalletUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
                }
//
//        sendMoneyToWallet(wallet, AbstractBlockChain.NewBlockType.BEST_CHAIN, t1);
//        assertEquals("Incorrect confirmed tx balance", amount, wallet.getBalance());
//        assertEquals("Incorrect confirmed tx PENDING pool size", 0, wallet.getPoolSize(WalletTransaction.Pool.PENDING));
//        assertEquals("Incorrect confirmed tx UNSPENT pool size", 1, wallet.getPoolSize(WalletTransaction.Pool.UNSPENT));
//        assertEquals("Incorrect confirmed tx ALL pool size", 1, wallet.getTransactions(true).size());
//
//
//        CompletableFuture<String> w = CompletableFuture.supplyAsync(() -> wallet.toString(), USER_THREAD);
//        
//        
//        System.out.println(w.join());
//        System.out.println("done2");
//
//        assertTrue(availFuture.isDone());
//        assertTrue(estimatedFuture.isDone());
//        assertTrue(depthFuture.isDone());
    }

    protected Transaction sendMoneyToWallet(Wallet wallet, AbstractBlockChain.NewBlockType type, Coin value, Address toAddress) throws VerificationException, BlockStoreException {
        return sendMoneyToWallet(wallet, type, createFakeTx(BitcoinNetwork.TESTNET, value, toAddress));
    }

    /**
     * Create a fake TX of sufficient realism to exercise the unit tests. Two
     * outputs, one to us, one to somewhere else to simulate change. There is
     * one random input.
     */
    public static Transaction createFakeTx(Network network, Coin value, Address to) {
//        FakeTxBuilder.roundTripTransaction(null);
        return createFakeTxWithChangeAddress(value, to, new ECKey().toAddress(ScriptType.P2PKH, network));
    }
    
    
        private static Transaction createFakeTxWithChangeAddress(Coin value, Address to, Address changeOutput) {
        Transaction t = new Transaction();
        TransactionOutput outputToMe = new TransactionOutput(t, value, to);
        t.addOutput(outputToMe);
        TransactionOutput change = new TransactionOutput(t, Coin.parseCoin("0.1"), changeOutput);
        t.addOutput(change);
        // Make a previous tx simply to send us sufficient coins. This prev tx is not really valid but it doesn't
        // matter for our purposes.
        Wallet fdW;
        try {
             fdW = Wallet.loadFromFile(feederWallet);
        } catch (UnreadableWalletException ex) {
            java.util.logging.Logger.getLogger(WalletUtil.class.getName()).log(Level.SEVERE, null, ex);

            throw new RuntimeException(ex);
        }
        Transaction prevTx = fdW.getPendingTransactions().stream().findFirst().get();
        TransactionOutput prevOut = new TransactionOutput(prevTx, value, to);
        prevTx.addOutput(prevOut);
        // Connect it.
        t.addInput(prevOut).withScriptSig(ScriptBuilder.createInputScript(TransactionSignature.dummy()));
        // Fake signature.
        // Serialize/deserialize to ensure internal state is stripped, as if it had been read from the wire.
        return roundTripTransaction(t);
    }

    @Nullable
    protected Transaction sendMoneyToWallet(Wallet wallet, AbstractBlockChain.NewBlockType type, Transaction... transactions)
            throws VerificationException, BlockStoreException {
        if (type == null) {
            // Pending transaction
            for (Transaction tx : transactions) {
                if (wallet.isPendingTransactionRelevant(tx)) {
                    wallet.receivePending(tx, null);
                }
            }
        } else {
            StoredBlock chainHead = store.getChainHead();
            FakeTxBuilder.BlockPair bp = createFakeBlock(store,
                    chainHead,
                    Block.BLOCK_VERSION_GENESIS,
                    TimeUtils.currentTime(),
                    chainHead.getHeight(), transactions);
            for (Transaction tx : transactions) {
                wallet.receiveFromBlock(tx, bp.storedBlock, type, 0);
            }
            if (type == AbstractBlockChain.NewBlockType.BEST_CHAIN) {
                wallet.notifyNewBestBlock(bp.storedBlock);
            }
        }
        if (transactions.length == 1) {
            return wallet.getTransaction(transactions[0].getTxId());  // Can be null if tx is a double spend that's otherwise irrelevant.
        } else {
            return null;
        }
    }

    public List<Address> getAddressToWatchFromTransaction() {

        return fdw.getPendingTransactions().stream()
                .flatMap(t -> t.getOutputs().stream())
                .map(to -> to.getScriptPubKey().getToAddress(net))
                .collect(Collectors.toList());
    }

    public static FakeTxBuilder.BlockPair createFakeBlock(BlockStore blockStore, StoredBlock previousStoredBlock, long version,
            Instant time, int height, Transaction... transactions) {
        try {
            Block previousBlock = previousStoredBlock.getHeader();
            Block b = previousBlock.createNextBlock(null, version, time, height);
            // Coinbase tx was already added.
            for (Transaction tx : transactions) {
                tx.getConfidence().maybeSetSourceToNetwork();
                b.addTransaction(tx);
            }
            b.solve();
            FakeTxBuilder.BlockPair pair = new FakeTxBuilder.BlockPair(b, previousStoredBlock.build(b));
            blockStore.put(pair.storedBlock);
            blockStore.setChainHead(pair.storedBlock);
            return pair;
        } catch (VerificationException | BlockStoreException e) {
            throw new RuntimeException(e);  // Cannot happen.
        }
    }

    private void spendUnConformed() throws UnreadableWalletException,
            IllegalStateException, UnknownHostException, InsufficientMoneyException {
        List<TransactionOutput> unspents = wallet.getUnspents();
//
//
        System.out.println(unspents.size());

        AesKey aesKey = wallet.getKeyCrypter().deriveKey(password);
//
//        TransactionOutput unspents1 = unspents.getFirst();
        Transaction t = wallet.getPendingTransactions().stream().findFirst().get();
        System.out.println(t.getValueSentToMe(wallet));

//        Stream.of(unspents1.getParentTransaction(), "\n \n ***", unspents1)
//                .forEach(System.out::println);
////
//        Transaction t = unspents1.getParentTransaction();
//                Transaction t = new Transaction();
//
//        final LinkedList<Transaction> txns = new LinkedList<>();
//
//        wallet.addCoinsSentEventListener((wallet1, tx, prevBalance, newBalance) -> txns.add(t));
//        t.getConfidence().markBroadcastBy(PeerAddress.simple(InetAddress.getByAddress(new byte[]{1, 2, 3, 4}), params.getPort()));
//        t.getConfidence().markBroadcastBy(PeerAddress.simple(InetAddress.getByAddress(new byte[]{10, 2, 3, 4}), params.getPort()));
//                wallet.commitTx(t);
//
//        assertEquals(0, wallet.getPoolSize(WalletTransaction.Pool.UNSPENT));
//        assertEquals(1, wallet.getPoolSize(WalletTransaction.Pool.PENDING));
//        assertEquals(2, wallet.getTransactions(true).size());
//
        System.out.println(t.getOutputs().get(0).isAvailableForSpending());
//        
//             if(wallet.getTransactionSigners().size() == 1){
//                 throw new IllegalArgumentException("transaction signers is 1");
//             }
//        String s = t.getOutput(0).getScriptPubKey().getScriptType().toString();
//
//      
//        ECKey signKey = wallet.getActiveKeyChain().getRootKey().maybeDecrypt(aesKey);
//        
//        System.out.println( s);
//         
////         
////    
//        ECKey k2 = wallet.freshReceiveKey();
//        Coin v2 = Coin.parseCoin("0.4");
//        Transaction t2 = new Transaction();
//        TransactionOutput o2 = new TransactionOutput(t2, v2, k2.toAddress(ScriptType.P2WPKH, net));
////        t2.addOutput(o2);
////        t2.addSignedInput(t.getOutput(0),signKey);
//        
//
////        
////
////////
//        SendRequest req = SendRequest.forTx(t2);
//        req.shuffleOutputs = false;
//        req.aesKey = aesKey;
////////
////////            System.out.println(t2.toString());
////////        /*other stuff from wallet util sned coins*/
//        wallet.completeTx(req);

//        
//////
//////        // Commit t2, so it is placed in the pending pool
//        wallet.commitTx(t2);
//        
//        System.out.println(wallet.toString());
//
////        assertEquals(0, wallet.getPoolSize(WalletTransaction.Pool.UNSPENT));
//        assertEquals(2, wallet.getPoolSize(WalletTransaction.Pool.PENDING));
////        assertEquals(3, wallet.getTransactions(true).size());
//
//        // Now the output of t2 must not be available for spending
//        assertFalse(t.getOutputs().getFirst().isAvailableForSpending());
//
//        assertTrue(wallet.isConsistent());
//
//        System.out.println(wallet.isConsistent());
//        
////        
//        System.out.println("\n \n ########################");
//        System.out.println(wallet.toString());
//        
//        System.out.println(t2.getConfidence().getDepthInBlocks());
//        System.out.println(wallet.toString());
//        List<TransactionOutput> unspents2 = wallet.getUnspents();
//
//        if (unspents1.equals(unspents2)) {
//
//            Stream.of(unspents1.toString(), unspents2.toString())
//                    .forEach(System.out::println);
//
//            throw new IllegalStateException("similar spents");
//        }
//
//        final List<TransactionSigner> transactionSigners = wallet.getTransactionSigners();
//
//        if (transactionSigners.size() == 1) // don't bother reconfiguring the p2sh wallet
//        {
//            wallet = roundTrip(wallet);
////                throw new IllegalArgumentException("round trip " + transactionSigners.size());
//        }
////
//        System.out.println(transactionSigners.size());
//
//        Coin v3 = Coin.parseCoin("0.006406");
//
//        if (wallet.getBalance().compareTo(v3) < 0) {
//
//            throw new IllegalStateException(
//                    String.format("""
//                                                  insufficient balance
//                                                   wallet balace = %s :
//                                                  expected %s :
//                                                  """, wallet.getBalance().toFriendlyString(), v3.toFriendlyString())
//            );
//        }
//
//        System.out.println(wallet.getBalance().toFriendlyString());
//
//        ECKey ecKey = new ECKey();
//
////            wallet.importKey(ecKey);
//        SendRequest req = SendRequest.to(ecKey.toAddress(outputScriptType, net), v3);
////        req.aesKey = aesKey;
//        req.shuffleOutputs = false;
//        wallet.completeTx(req);
//        Transaction t3 = req.tx;
//        System.out.println(t3.toString());
    }

    private void importPubKey() throws IOException {
        //
//                    boolean pubKeyHashMine = wallet.isPubKeyHashMine(t.getOutput(0).getScriptPubKey().getPubKeyHash(), ScriptType.P2PKH);
//                    System.out.println(pubKeyHashMine);
//                    ECKey dECKey = wallet.findKeyFromPubKeyHash(pubKeyHash, ScriptType.P2PKH);
//                    ECKey fromPublicOnly = ECKey.fromPublicOnly(dECKey.getPubKey());
//                    System.out.println(fromPublicOnly.toString());

        byte[] pub = parseAsHexOrBase58("0278de28307bdd5c64add0d18a61c88090e0e5aa227c03f4c833c038c51bfcf1ec");
        ECKey pubK = ECKey.fromPublicOnly(pub);

        ECKey prK = new ECKey();

        ECKey key = ECKey.fromPrivateAndPrecalculatedPublic(prK.getPrivKeyBytes(), pubK.getPubKey());

        Wallet fw = Wallet.createDeterministic(net, ScriptType.P2PKH);
//
//
        fw.importKey(key);

        fw.saveToFile(new File("fr.wallet"));

        System.out.println(fw.toString());

//                    Coin vstm = t.getValueSentToMe(fw);
//                    System.out.println(vstm.toFriendlyString());
//
//                    t.getOutputs().stream()
//                            .map(to -> to.toString(net))
//                            .limit(1)
//
//                            
//                            .forEach(System.out::println);
//                    
//                    System.out.println("\n");
//                    Script scriptPubKey = t.getOutput(0).getScriptPubKey();
//
//
//                    Address add =scriptPubKey.getToAddress(net); 
//                    System.out.println(add.toString());
//                    ECKey.fromPrivateAndPrecalculatedPublic(priv, pub);
//                    BigInteger bigInteger = Base58.decodeToBigInteger("76a914ab95c9c103c5cb35cfdc21d7a296f0cf625fd93588ac");
//                    boolean pkC = ECKey.isPubKeyCompressed(scriptPubKey.program());
//                     ECKey.fromPrivate(bigInteger);
//                                        System.out.println(a.toString());
//                syncChain();
//                System.out.println(wallet.toString());
    }

    private byte[] parseAsHexOrBase58(String data) {
        try {
            return ByteUtils.parseHex(data);
        } catch (Exception e) {
            // Didn't decode as hex, try base58.
            try {
                return Base58.decodeChecked(data);
            } catch (AddressFormatException e1) {
                return null;
            }
        }
    }

    private void addKey() {
        ECKey key;
        Optional<Instant> creationTime = getCreationTime();
        if (privKeyStr != null) {
            try {
                DumpedPrivateKey dpk = DumpedPrivateKey.fromBase58(net, privKeyStr); // WIF
                key = dpk.getKey();
            } catch (AddressFormatException e) {
                byte[] decode = parseAsHexOrBase58(privKeyStr);
                if (decode == null) {
                    System.err.println("Could not understand --privkey as either WIF, hex or base58: " + privKeyStr);
                    return;
                }
                key = ECKey.fromPrivate(ByteUtils.bytesToBigInteger(decode));
            }
            if (pubKeyStr != null) {
                // Give the user a hint.
                System.out.println("You don't have to specify --pubkey when a private key is supplied.");
            }
            creationTime.ifPresentOrElse(key::setCreationTime, key::clearCreationTime);
        } else if (pubKeyStr != null) {
            byte[] pubkey = parseAsHexOrBase58(pubKeyStr);
            key = ECKey.fromPublicOnly(pubkey);
            creationTime.ifPresentOrElse(key::setCreationTime, key::clearCreationTime);
        } else {
            System.err.println("Either --privkey or --pubkey must be specified.");
            return;
        }
        if (wallet.hasKey(key)) {
            System.err.println("That key already exists in this wallet.");
            return;
        }
        try {
            if (wallet.isEncrypted()) {
                AesKey aesKey = passwordToKey(true);
                if (aesKey == null) {
                    return;   // Error message already printed.
                }
                key = key.encrypt(Objects.requireNonNull(wallet.getKeyCrypter()), aesKey);
            }
        } catch (KeyCrypterException kce) {
            System.err.println("There was an encryption related error when adding the key. The error was '"
                    + kce.getMessage() + "'.");
            return;
        }
        if (!key.isCompressed()) {
            System.out.println("WARNING: Importing an uncompressed key");
        }
        wallet.importKey(key);
        System.out.print("Addresses: " + key.toAddress(ScriptType.P2PKH, net));
        if (key.isCompressed()) {
            System.out.print("," + key.toAddress(ScriptType.P2WPKH, net));
        }
        System.out.println();
    }

    public void createWallet(Network network, File walletFile) throws IOException {
        KeyChainGroupStructure keyChainGroupStructure = KeyChainGroupStructure.BIP32;

        if (walletFile.exists() && !force) {
            System.err.println("Wallet creation requested but " + walletFile + " already exists, use --force");
            return;
        }
        Instant creationTime = MnemonicCode.BIP39_STANDARDISATION_TIME;
        if (seedStr != null) {
            DeterministicSeed seed;
            // Parse as mnemonic code.
            final List<String> split = splitMnemonic(seedStr);
            String passphrase = ""; // TODO allow user to specify a passphrase
            seed = DeterministicSeed.ofMnemonic(split, passphrase, creationTime);
            try {
                seed.check();
            } catch (MnemonicException.MnemonicLengthException e) {
                System.err.println("The seed did not have 12 words in, perhaps you need quotes around it?");
                return;
            } catch (MnemonicException.MnemonicWordException e) {
                System.err.println("The seed contained an unrecognised word: " + e.badWord);
                return;
            } catch (MnemonicException.MnemonicChecksumException e) {
                System.err.println("The seed did not pass checksumming, perhaps one of the words is wrong?");
                return;
            } catch (MnemonicException e) {
                // not reached - all subclasses handled above
                throw new RuntimeException(e);
            }
            wallet = Wallet.fromSeed(network, seed, outputScriptType, keyChainGroupStructure);
        } else if (watchKeyStr != null) {
            wallet = Wallet.fromWatchingKeyB58(network, watchKeyStr, creationTime);
        } else {
            wallet = Wallet.createDeterministic(network, outputScriptType, keyChainGroupStructure);
        }
        if (password != null) {
            wallet.encrypt(password);
        }
        wallet.saveToFile(walletFile);
    }

    private Optional<Instant> getCreationTime() {
        if (unixtime != null) {
            return Optional.of(Instant.ofEpochSecond(unixtime));
        } else if (date != null) {
            return Optional.of(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
        } else {
            return Optional.empty();
        }
    }

    // Sets up all objects needed for network communication but does not bring up the peers.
    private void setup() throws BlockStoreException, UnreadableWalletException, IOException {
        
        Context.propagate(new Context());
        wallet = Wallet.loadFromFile(walletFile);


        
        if (store != null) {
            return;  // Already done.
        }            // Will create a fresh chain if one doesn't exist or there is an issue with this one.
        boolean reset = !chainFile.exists();
        if (reset) {
            // No chain, so reset the wallet as we will be downloading from scratch.
            System.out.println("Chain file is missing so resetting the wallet.");
            reset();
        }
        store = new SPVBlockStore(params, chainFile);
        
         blockStore = new MemoryFullPrunedBlockStore(params, 100);
        
        
//        wallet.setUTXOProvider(blockStore);
//
//        Block b1 = TransactionBuilder.loadBlockFromFile("/home/michael/NetBeansProjects/btcJFX/complete_tx_with_existing_inputs.dat");
//
//        StoredBlock stb = blockStore.getChainHead().build(b1);
//        blockStore.put(stb);
//        blockStore.setChainHead(stb);
        if (reset) {
            try {
                CheckpointManager.checkpoint(params, CheckpointManager.openStream(params), store,
                        wallet.earliestKeyCreationTime());
                StoredBlock head = store.getChainHead();
                System.out.println("Skipped to checkpoint " + head.getHeight() + " at "
                        + TimeUtils.dateTimeFormat(head.getHeader().time()));
            } catch (IOException x) {
                System.out.println("Could not load checkpoints: " + x.getMessage());
            }
        }
        chain = new BlockChain(net, wallet, store);
//         This will ensure the wallet is saved when it changes.
        wallet.autosaveToFile(walletFile, Duration.ofSeconds(5), null);
        if (peerGroup == null) {
            peerGroup = new PeerGroup(net, chain);
        }
        peerGroup.setUserAgent("WalletTool", "1.0");
        if (net == BitcoinNetwork.REGTEST) {
            peerGroup.addAddress(PeerAddress.localhost(params));
            peerGroup.setMinBroadcastConnections(1);
            peerGroup.setMaxConnections(1);
        }
        peerGroup.addWallet(wallet);
        peerGroup.setBloomFilteringEnabled(filter == Filter.SERVER);
        if (peersStr != null) {
            String[] peerAddrs = peersStr.split(",");
            for (String peer : peerAddrs) {
                try {
                    peerGroup.addAddress(PeerAddress.simple(InetAddress.getByName(peer), params.getPort()));
                } catch (UnknownHostException e) {
                    System.err.println("Could not understand peer domain name/IP address: " + peer + ": " + e.getMessage());
                    System.exit(1);
                }
            }
        } else {
            peerGroup.setRequiredServices(0);
        }
    }

    private void syncChain() throws IOException {
        try {
            setup();
            int startTransactions = wallet.getTransactions(true).size();
            DownloadProgressTracker listener = new DownloadProgressTracker();
            peerGroup.start();
            peerGroup.startBlockChainDownload(listener);
            try {
                listener.await();
            } catch (InterruptedException e) {
                System.err.println("Chain download interrupted, quitting ...");
                System.exit(1);
            }
            int endTransactions = wallet.getTransactions(true).size();
            if (endTransactions > startTransactions) {
                System.out.println("Synced " + (endTransactions - startTransactions) + " transactions.");
            }

            wallet.saveToFile(walletFile);
            System.out.println(wallet.toString());
        } catch (BlockStoreException e) {
            System.err.println("Error reading block chain file " + chainFile + ": " + e.getMessage());
        } catch (UnreadableWalletException ex) {
            java.util.logging.Logger.getLogger(WalletUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void waitFor() {

        condition = new Condition(">0.10");
        WaitForEnum waitFor = WaitForEnum.BALANCE;

        try {
            setup();
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(WalletUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
        CompletableFuture<String> futureMessage = wait(waitFor, condition);
        if (!peerGroup.isRunning()) {
            peerGroup.startAsync();
        }
        System.out.println(futureMessage.join());
        if (!wallet.isConsistent()) {
            System.err.println("************** WALLET IS INCONSISTENT *****************");
            return;
        }
        saveWallet(walletFile);
    }

    private CompletableFuture<String> wait(WaitForEnum waitFor, Condition condition) {
        CompletableFuture<String> future = new CompletableFuture<>();
        switch (waitFor) {
            case EVER:
                break;  // Future will never complete

            case WALLET_TX:
                // Future will complete with a transaction ID string
                Consumer<Transaction> txListener = tx -> future.complete(tx.getTxId().toString());
                // Both listeners run in a peer thread
                wallet.addCoinsReceivedEventListener((wallet, tx, prevBalance, newBalance) -> txListener.accept(tx));
                wallet.addCoinsSentEventListener((wallet, tx, prevBalance, newBalance) -> txListener.accept(tx));
                break;

            case BLOCK:
                // Future will complete with a Block hash string
                peerGroup.addBlocksDownloadedEventListener((peer, block, filteredBlock, blocksLeft)
                        -> future.complete(block.getHashAsString())
                );
                break;

            case BALANCE:
                // Future will complete with a balance amount string
                // Check if the balance already meets the given condition.
                Coin existingBalance = wallet.getBalance(Wallet.BalanceType.ESTIMATED);
                if (condition.matchBitcoins(existingBalance)) {
                    future.complete(existingBalance.toFriendlyString());
                } else {
                    Runnable onChange = () -> {
                        synchronized (this) {
                            saveWallet(walletFile);
                            Coin balance = wallet.getBalance(Wallet.BalanceType.ESTIMATED);
                            log.info(wallet.toString() + "\n");
                            if (condition.matchBitcoins(balance)) {
                                future.complete(balance.toFriendlyString());
                            }
                        }
                    };
                    wallet.addCoinsReceivedEventListener((w, t, p, n) -> {

                        log.info("coins Recevied BTCJ \n \n");
                        onChange.run();
                        this.coinForwardingListener(w, t, p, n);
                    });
                    wallet.addCoinsSentEventListener((w, t, p, n) -> onChange.run());
                    wallet.addChangeEventListener(w -> onChange.run());
                    wallet.addReorganizeEventListener(w -> onChange.run());
                    wallet.addTransactionConfidenceEventListener((w, tx) -> {

                        if (Stream.of(TransactionConfidence.ConfidenceType.BUILDING,
                                TransactionConfidence.ConfidenceType.PENDING)
                                .anyMatch(con -> {
                                    return tx.getConfidence().getConfidenceType().equals(con);
                                })) {

                            onChange.run();

                            log.info("-----> confidence changed: " + tx.getTxId());
                            TransactionConfidence confidence = tx.getConfidence();
                            log.info("new block depth: \n \n" + confidence.getDepthInBlocks());
                            //                            Coin value = tx.getValueSentToMe(w);
                            //                            log.info("Received tx for %s : %s\n \n", value.toFriendlyString(), tx);

                            forward(w, tx, toAddress);
                            log.info("forwarding coins now \n");

                            //
                        }
                    });

                    log.info("waiting for Bitcoins");
                }
                break;
        }
        return future;
    }

    /**
     * A listener to receive coins and forward them to the configured address.
     * Implements the {@link WalletCoinsReceivedEventListener} functional
     * interface.
     *
     * @param wallet The active wallet
     * @param incomingTx the received transaction
     * @param prevBalance wallet balance before this transaction (unused)
     * @param newBalance wallet balance after this transaction (unused)
     */
    private void coinForwardingListener(Wallet wallet, Transaction incomingTx, Coin prevBalance, Coin newBalance) {
        // Incoming transaction received, now "compose" (i.e. chain) a call to wait for required confirmations
        // The transaction "incomingTx" can either be pending, or included into a block (we didn't see the broadcast).
        Coin value = incomingTx.getValueSentToMe(wallet);
        System.out.printf("Received tx for %s : %s\n", value.toFriendlyString(), incomingTx);
        System.out.println("Transaction will be forwarded after it confirms.");
        System.out.println("Waiting for confirmation...");
        wallet.waitForConfirmations(incomingTx, 1)
                .thenCompose(confidence -> {
                    // Required confirmations received, now create and send forwarding transaction
                    System.out.printf("Incoming tx has received %d confirmations.\n", confidence.getDepthInBlocks());

                    return forward(wallet, incomingTx, this.toAddress);

                })
                .whenComplete((broadcast, throwable) -> {
                    if (broadcast != null) {
                        System.out.printf("Sent %s onwards and acknowledged by peers, via transaction %s\n",
                                broadcast.transaction().getOutputSum().toFriendlyString(),
                                broadcast.transaction().getTxId());
                    } else {
                        System.out.println(throwable.getCause() + "\n");

                        throwable.printStackTrace();
                    }

                });

    }

    /**
     * Forward an incoming transaction by creating a new transaction, signing,
     * and sending to the specified address. The inputs for the new transaction
     * should only come from the incoming transaction, so we use a custom
     * {@link CoinSelector} that only selects wallet UTXOs with the correct
     * parent transaction ID.
     *
     * @param wallet The active wallet
     * @param incomingTx the received transaction
     * @param forwardingAddress the address to send to
     * @return A future for a TransactionBroadcast object that completes when
     * relay is acknowledged by peers
     */
    private CompletableFuture<TransactionBroadcast> forward(Wallet wallet, Transaction incomingTx, Address forwardingAddress) {
        // Send coins received in incomingTx onwards by sending exactly the UTXOs we have just received.
        // We're not truly emptying the wallet because we're limiting the available outputs with a CoinSelector.
        SendRequest sendRequest = SendRequest.emptyWallet(forwardingAddress);
        // Use a CoinSelector that only returns wallet UTXOs from the incoming transaction.
        sendRequest.coinSelector = CoinSelector.fromPredicate(output -> Objects.equals(output.getParentTransactionHash(), incomingTx.getTxId()));
        System.out.printf("Creating outgoing transaction for %s...\n", forwardingAddress);
        return wallet.sendTransaction(sendRequest)
                .thenCompose(broadcast -> {
                    System.out.printf("Transaction %s is signed and is being delivered to %s...\n", broadcast.transaction().getTxId(), net);
                    return broadcast.awaitRelayed(); // Wait until peers report they have seen the transaction
                });
    }

    /**
     * Forward an incoming transaction by creating a new transaction, signing,
     * and sending to the specified address. The inputs for the new transaction
     * should only come from the incoming transaction, so we use a custom
     * {@link CoinSelector} that only selects wallet UTXOs with the correct
     * parent transaction ID.
     *
     * @param wallet The active wallet
     * @param incomingTx the received transaction
     * @param forwardingAddress the address to send to
     * @return A future for a TransactionBroadcast object that completes when
     * relay is acknowledged by peers
     */
    private void reset() {
        // Delete the transactions and save. In future, reset the chain head pointer.
        wallet.clearTransactions(0);
        saveWallet(walletFile);
    }

    private void dumpWallet() throws BlockStoreException, InterruptedException, ExecutionException, TimeoutException {
        // Setup to get the chain height so we can estimate lock times, but don't wipe the transactions if it's not
        // there just for the dump case.
        if (chainFile.exists()) {
            try {
                setup();
            } catch (UnreadableWalletException | IOException ex) {
                java.util.logging.Logger.getLogger(WalletUtil.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException(ex);
            }
        }

        if (dumpPrivKeys && wallet.isEncrypted()) {
            if (password != null) {
                final AesKey aesKey = passwordToKey(true);
                if (aesKey == null) {
                    return; // Error message already printed.
                }
                printWallet(aesKey);
            } else {
                System.err.println("Can't dump privkeys, wallet is encrypted.");
                return;
            }
        } else {
            printWallet(null);
        }
    }

    private void printWallet(@Nullable AesKey aesKey) {
        System.out.println(wallet.toString(dumpLookAhead, dumpPrivKeys, aesKey, true, true, chain));
    }

    @Nullable
    private AesKey passwordToKey(boolean printError) {
        if (password == null) {
            if (printError) {
                System.err.println("You must provide a password.");
            }
            return null;
        }
        if (!wallet.checkPassword(password)) {
            if (printError) {
                System.err.println("The password is incorrect.");
            }
            return null;
        }
        return Objects.requireNonNull(wallet.getKeyCrypter()).deriveKey(password);
    }

    private void saveWallet(File walletFile) {
        try {
            // This will save the new state of the wallet to a temp file then rename, in case anything goes wrong.
            wallet.saveToFile(walletFile);
        } catch (IOException e) {
            System.err.println("Failed to save wallet! Old wallet should be left untouched.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private List<String> splitMnemonic(String seedStr) {
        return Stream.of(seedStr.split("[ :;,]")) // anyOf(" :;,")
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableList());
    }

    public void rawDump() throws FileNotFoundException, IOException {
        try (FileInputStream stream = new FileInputStream(walletFile)) {
            Protos.Wallet proto = WalletProtobufSerializer.parseToProto(stream);
            proto = attemptHexConversion(proto);
            System.out.println(proto.toString());
            Platform.exit();
        }
    }

    private static Protos.Wallet attemptHexConversion(Protos.Wallet proto) {
        // Try to convert any raw hashes and such to textual equivalents for easier debugging. This makes it a bit
        // less "raw" but we will just abort on any errors.
        try {
            Protos.Wallet.Builder builder = proto.toBuilder();
            for (Protos.Transaction tx : builder.getTransactionList()) {
                Protos.Transaction.Builder txBuilder = tx.toBuilder();
                txBuilder.setHash(bytesToHex(txBuilder.getHash()));
                for (int i = 0; i < txBuilder.getBlockHashCount(); i++) {
                    txBuilder.setBlockHash(i, bytesToHex(txBuilder.getBlockHash(i)));
                }
                for (Protos.TransactionInput input : txBuilder.getTransactionInputList()) {
                    Protos.TransactionInput.Builder inputBuilder = input.toBuilder();
                    inputBuilder.setTransactionOutPointHash(bytesToHex(inputBuilder.getTransactionOutPointHash()));
                }
                for (Protos.TransactionOutput output : txBuilder.getTransactionOutputList()) {
                    Protos.TransactionOutput.Builder outputBuilder = output.toBuilder();
                    if (outputBuilder.hasSpentByTransactionHash()) {
                        outputBuilder.setSpentByTransactionHash(bytesToHex(outputBuilder.getSpentByTransactionHash()));
                    }
                }
                // TODO: keys, ip addresses etc.
            }
            return builder.build();
        } catch (Throwable throwable) {
            log.error("Failed to do hex conversion on wallet proto", throwable);
            return proto;
        }
    }

    private static ByteString bytesToHex(ByteString bytes) {
        return ByteString.copyFrom(ByteUtils.formatHex(bytes.toByteArray()).getBytes());
    }

    @Override
    public void close() throws IOException {
        if (peerGroup == null) {
            return;  // setup() never called so nothing to do.
        }
        if (peerGroup.isRunning()) {
            peerGroup.stop();
        }
        saveWallet(walletFile);
        try {
            store.close();
            wallet = null;
        } catch (BlockStoreException ex) {
            java.util.logging.Logger.getLogger(WalletUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    protected Transaction sendMoneyToWallet(Wallet wallet, Transaction... transactions)
            throws VerificationException {
        // Pending transaction
        for (Transaction tx : transactions) {
            if (wallet.isPendingTransactionRelevant(tx)) {
                wallet.receivePending(tx, null);
            }
        }

        if (transactions.length == 1) {
            return wallet.getTransaction(transactions[0].getTxId());  // Can be null if tx is a double spend that's otherwise irrelevant.
        } else {
            return null;
        }
    }

    private void basicSanityChecks(Wallet wallet, Transaction t, Address destination) throws VerificationException {

        Predicate<Transaction> checkInputs = tx -> tx.getInputs().size() == 1;

        Predicate<Transaction> checkOutputs = tx -> tx.getOutputs().size() == 1;

        Predicate<Transaction> checkDestination = tx -> tx.getOutput(0)
                .getScriptPubKey()
                .getToAddress(net)
                .equals(destination);

        Predicate<Transaction> checkChangeAddress = tx
                -> wallet.currentChangeAddress()
                        .equals(t.getOutput(1).getScriptPubKey().getToAddress(net));

        Stream.of(
                new Pair("Wrong number of tx inputs", checkInputs),
                new Pair("Wrong number of tx outputs", checkOutputs),
                new Pair("destination mistmatch", checkDestination),
                new Pair("change address mistmatch", checkChangeAddress)
        )
                .mapMulti((p, downStream) -> {
                    if (!p.pr.test(t)) {
                        downStream.accept(p.a);
                    }

                }).forEach(System.err::println);

//        assertEquals("Wrong number of tx inputs", 1, t.getInputs().size());
//        assertEquals("Wrong number of tx outputs",2, t.getOutputs().size());
//        assertEquals(destination, t.getOutput(0).getScriptPubKey().getToAddress(net));
//        assertEquals(wallet.currentChangeAddress(), t.getOutput(1).getScriptPubKey().getToAddress(net));
//        assertEquals(valueOf(0, 50), t.getOutput(1).getValue());
        // Check the script runs and signatures verify.
        t.getInput(0).verify();
    }
    
    
        public boolean isChainFileLocked() throws IOException {
        RandomAccessFile file2 = null;
        try {
           
            if (!chainFile.exists())
                return false;
            if (chainFile.isDirectory())
                return false;
            file2 = new RandomAccessFile(chainFile, "rw");
            FileLock lock = file2.getChannel().tryLock();
            if (lock == null)
                return true;
            lock.release();
            return false;
        } finally {
            if (file2 != null)
                file2.close();
        }
    }

}
