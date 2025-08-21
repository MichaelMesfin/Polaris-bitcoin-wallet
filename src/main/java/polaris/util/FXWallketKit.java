/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package polaris.util;

import java.io.File;
import org.bitcoinj.base.BitcoinNetwork;
import org.bitcoinj.base.ScriptType;
import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.PeerAddress;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.store.SPVBlockStore;
import org.bitcoinj.wallet.KeyChainGroupStructure;
import org.bitcoinj.wallet.Wallet;
import polaris.wallettemplate.walletfx.application.Main;

/**
 *
 * @author michael
 */
public class FXWallketKit extends WalletAppKit {

    private final File chainFile;

    public FXWallketKit(BitcoinNetwork network,
            ScriptType preferredOutputScriptType,
            KeyChainGroupStructure structure,
            File directory,
            String filePrefix) {
        super(network, preferredOutputScriptType, structure, directory, filePrefix);

        this.vWalletFile = new File(Main.ROOT_DIR, "polaris.wallet");

//        getClass().getResource(Main.ROOT_DIR + "app-wallet.wallet");
        this.chainFile = new File(Main.ROOT_DIR, "polaris.spvchain");

    }

    @Override
    protected void startUp() throws Exception {

        log.info("Starting up with directory = {}", directory);

        if (!this.vWalletFile.exists()) {
            vWallet = createWallet();
            vWallet.freshReceiveKey();

            vWallet.saveToFile(this.vWalletFile);

            vWallet = Wallet.loadFromFile(this.vWalletFile);

        } else {
            vWallet = Wallet.loadFromFile(vWalletFile);
        }

        vStore = new SPVBlockStore(params, chainFile);

        vChain = new BlockChain(network, vStore);

        vPeerGroup = createPeerGroup();
        if (this.userAgent != null) {
            vPeerGroup.setUserAgent(userAgent, version);
        }

        // Set up peer addresses or discovery first, so if wallet extensions try to broadcast a transaction
        // before we're actually connected the broadcast waits for an appropriate number of connections.
        if (peerAddresses != null) {
            for (PeerAddress addr : peerAddresses) {
                vPeerGroup.addAddress(addr);
            }
            vPeerGroup.setMaxConnections(peerAddresses.length);
            peerAddresses = null;
        }
        
        vPeerGroup.addPeerDiscovery(discovery != null ? discovery : new DnsDiscovery(network));

        vPeerGroup.setMaxConnections(10);
//        
////        else
//            if (params.network() != BitcoinNetwork.REGTEST) {
//            vPeerGroup.addPeerDiscovery(discovery != null ? discovery : new DnsDiscovery(network));
//        }
            

            
            
        vChain.addWallet(vWallet);
        vPeerGroup.addWallet(vWallet);

        // vChain, vWallet, and vPeerGroup all initialized; allow subclass (if any) a chance to adjust configuration
        onSetupCompleted();

        // Start the PeerGroup (asynchronously) and start downloading the blockchain (asynchronously)
        vPeerGroup.startAsync().whenComplete((result, t) -> {
            if (t == null) {
                vPeerGroup.startBlockChainDownload(downloadListener);
            } else {
                throw new RuntimeException(t);
            }
        });

        // Make sure we shut down cleanly.
        installShutdownHook();

        if (blockingStartup) {
            downloadListener.await();   // Wait for the blockchain to download
        }

    }

    private void installShutdownHook() {
        if (autoStop) {
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownHook, "shutdownHook"));
        }
    }

    private void shutdownHook() {
        try {
            stopAsync();
            awaitTerminated();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //
}
