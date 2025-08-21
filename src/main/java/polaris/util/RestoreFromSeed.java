/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package polaris.util;

/**
 *
 * @author michael
 */


import org.bitcoinj.base.BitcoinNetwork;
import org.bitcoinj.base.Network;
import org.bitcoinj.base.ScriptType;
import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.listeners.DownloadProgressTracker;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.store.SPVBlockStore;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.Wallet;

import java.io.File;
import java.io.FileInputStream;
import java.time.Instant;

/**
 * The following example shows you how to restore a HD wallet from a previously generated deterministic seed.
 * In this example we manually setup the blockchain, peer group, etc. You can also use the WalletAppKit which provides a restoreWalletFromSeed function to load a wallet from a deterministic seed.
 */
public class RestoreFromSeed {
 public static String walletFile = "/home/michael/NetBeansProjects/btcJFX/polaris.wallet";
    public static void main(String[] args) throws Exception {
        Network network = BitcoinNetwork.MAINNET;
        NetworkParameters params = NetworkParameters.of(network);

        // Bitcoinj supports hierarchical deterministic wallets (or "HD Wallets"): https://github.com/bitcoin/bips/blob/master/bip-0032.mediawiki
        // HD wallets allow you to restore your wallet simply from a root seed. This seed can be represented using a short mnemonic sentence as described in BIP 39: https://github.com/bitcoin/bips/blob/master/bip-0039.mediawiki

        // Here we restore our wallet from a seed with no passphrase. Also have a look at the BackupToMnemonicSeed.java example that shows how to backup a wallet by creating a mnemonic sentence.
     
        // The wallet class provides a easy fromSeed() function that loads a new wallet from a given seed.
        Wallet wallet = Wallet.loadFromFileStream(new FileInputStream(walletFile));

        // Because we are importing an existing wallet which might already have transactions we must re-download the blockchain to make the wallet picks up these transactions
        // You can find some information about this in the guides: https://bitcoinj.github.io/working-with-the-wallet#setup
        // To do this we clear the transactions of the wallet and delete a possible existing blockchain file before we download the blockchain again further down.
        File chainFile = new File("/home/michael/NetBeansProjects/btcJFX/polaris.spvchain");
//        if (chainFile.exists()) {
//            chainFile.delete();
//        }

        // Setting up the BlochChain, the BlocksStore and connecting to the network.
        SPVBlockStore chainStore = new SPVBlockStore(params, chainFile);
        BlockChain chain = new BlockChain(network, chainStore);
        PeerGroup peerGroup = new PeerGroup(network, chain);
        peerGroup.addPeerDiscovery(new DnsDiscovery(network));

        // Now we need to hook the wallet up to the blockchain and the peers. This registers event listeners that notify our wallet about new transactions.
        chain.addWallet(wallet);
        peerGroup.addWallet(wallet);

        DownloadProgressTracker bListener = new DownloadProgressTracker() {
            @Override
            public void doneDownload() {
                System.out.println("blockchain downloaded");
            }
        };

        
        
        
        // Now we re-download the blockchain. This replays the chain into the wallet. Once this is completed our wallet should know of all its transactions and print the correct balance.
        peerGroup.start();
        peerGroup.startBlockChainDownload(bListener);

        bListener.await();

        // Print a debug message with the details about the wallet. The correct balance should now be displayed.
        System.out.println(wallet.toString());

        // shutting down again
        peerGroup.stop();
    }
}