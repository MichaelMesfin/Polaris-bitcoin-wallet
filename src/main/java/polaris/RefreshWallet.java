/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package polaris;

/**
 *
 * @author michael
 */
import org.bitcoinj.base.BitcoinNetwork;
import org.bitcoinj.base.Coin;
import org.bitcoinj.base.Network;
import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.MemoryBlockStore;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.listeners.WalletCoinsReceivedEventListener;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.time.Duration;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.StoredBlock;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.store.SPVBlockStore;
import static org.bitcoinj.testing.FakeTxBuilder.makeSolvedTestBlock;
import org.bitcoinj.wallet.WalletProtobufSerializer;
import polaris.util.TransactionBuilder;

/**
 * RefreshWallet loads a wallet, then processes the block chain to update the
 * transaction pools within it. To get a test wallet you can use wallet-tool
 * from the tools subproject.
 */
public class RefreshWallet {

    public static String walletPath = "/home/michael/NetBeansProjects/btcJFX/polaris.wallet";

    public static void main(String[] args) throws Exception {

      
                final Network network = BitcoinNetwork.MAINNET;

                final NetworkParameters params = NetworkParameters.of(network);
                
                
                  File walletFile = new File(walletPath);
        Wallet wallet = Wallet.loadFromFile(walletFile);
        

        SPVBlockStore store = new SPVBlockStore(params, new File("/home/michael/NetBeansProjects/btcJFX/polaris_refresh.spvchain") );


        // Set up the components and link them together.
//        BlockStore blockStore = new MemoryBlockStore(NetworkParameters.of(network).getGenesisBlock());

        Block b1 = TransactionBuilder.loadBlockFromFile("/home/michael/NetBeansProjects/btcJFX/complete_tx_with_existing_inputs.dat");

        
        StoredBlock sb = new StoredBlock(params.getGenesisBlock(), BigInteger.ONE, 1);
        StoredBlock sb1 = sb.build(b1);
        store.put(sb1);
        store.setChainHead(sb1);
        BlockChain chain = new BlockChain(network, wallet, store);

        final PeerGroup peerGroup = new PeerGroup(network, chain);
        
        peerGroup.addPeerDiscovery( new DnsDiscovery(network));

        wallet.autosaveToFile(walletFile, Duration.ofSeconds(30), null);

        peerGroup.startAsync();

        wallet.addCoinsReceivedEventListener((Wallet w, Transaction tx, Coin prevBalance, Coin newBalance) -> {
            System.out.println("\nReceived tx " + tx.getTxId());
            System.out.println(tx.toString());
                });

        // Now download and process the block chain.
        peerGroup.downloadBlockChain();
        peerGroup.stopAsync();
        wallet.saveToFile(walletFile);
        System.out.println("\nDone!\n");
        System.out.println(wallet.toString());
    }
}
