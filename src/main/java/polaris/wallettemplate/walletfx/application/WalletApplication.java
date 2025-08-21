package polaris.wallettemplate.walletfx.application;

/*
 * Copyright by the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import com.gluonhq.attach.display.DisplayService;
import com.gluonhq.attach.util.Services;
import com.google.common.util.concurrent.Service;
import jakarta.annotation.Nullable;
import javafx.application.Platform;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import org.bitcoinj.base.BitcoinNetwork;
import org.bitcoinj.base.ScriptType;
import org.bitcoinj.base.internal.PlatformUtils;
import org.bitcoinj.core.Context;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.utils.BriefLogFormatter;
import org.bitcoinj.utils.Threading;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.KeyChainGroupStructure;
import polaris.wallettemplate.walletfx.utils.GuiUtils;
import polaris.wallettemplate.WalletSetPasswordController;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javafx.geometry.Dimension2D;
import javafx.scene.DepthTest;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PeerAddress;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.wallet.UnreadableWalletException;

import static polaris.wallettemplate.walletfx.utils.GuiUtils.informationalAlert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import polaris.util.FXWallketKit;

import polaris.wallettemplate.IPController;
import polaris.wallettemplate.walletfx.controls.IP;

/**
 * Base class for JavaFX Wallet Applications
 */
public abstract class WalletApplication implements AppDelegate {
    



    private static final Logger log = LoggerFactory.getLogger(WalletApplication.class);

    private static WalletApplication instance;
    private WalletAppKit walletAppKit;
    private final String applicationName;
    private final BitcoinNetwork network;
    private final KeyChainGroupStructure keyChainGroupStructure;
    private final ScriptType preferredOutputScriptType;
    private final String walletFileName;
    private MainWindowController controller;

    private final NetworkParameters params;
    
    public final  Dimension2D dimension;

    private List<String> peerAddresses = new ArrayList<>();


    public WalletApplication(String applicationName,
            BitcoinNetwork network,
            ScriptType preferredOutputScriptType,
            KeyChainGroupStructure keyChainGroupStructure) throws URISyntaxException, UnreadableWalletException {
//        this.walletUtil = new WalletUtil();

        applicationName = "btcWallet";
        instance = this;
        this.applicationName = applicationName;
        this.walletFileName = applicationName.replaceAll("[^a-zA-Z0-9.-]", "_") + "-" + suffixFromNetwork(network);
        this.network = network;
        this.preferredOutputScriptType = preferredOutputScriptType;
        this.keyChainGroupStructure = keyChainGroupStructure;
        this.params = NetworkParameters.of(network);
        
        
        
                
         dimension = Services.get(DisplayService.class)
                .map(DisplayService::getDefaultDimensions)
                .orElseGet(() -> new Dimension2D(355, 600));
        
                            System.out.printf("\n \n \n Dimension => %s \n \n",  dimension.toString());
    }

    public WalletApplication(String applicationName, BitcoinNetwork network, ScriptType preferredOutputScriptType) throws URISyntaxException, UnreadableWalletException {
        this(applicationName, network, preferredOutputScriptType, KeyChainGroupStructure.BIP43);
    }

    public static WalletApplication instance() {
        return instance;
    }

    public WalletAppKit walletAppKit() {
        return walletAppKit;
    }

    public String applicationName() {
        return applicationName;
    }

    public BitcoinNetwork network() {
        return network;
    }

    public ScriptType preferredOutputScriptType() {
        return preferredOutputScriptType;
    }

    public MainWindowController mainWindowController() {
        return controller;
    }

    protected abstract MainWindowController loadController() throws IOException;

    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            startImpl(primaryStage);
        } catch (Throwable e) {
            GuiUtils.crashAlert(e);
            throw e;
        }
    }

    private void startImpl(Stage primaryStage) throws IOException, BlockStoreException {
        // Show the crash dialog for any exceptions that we don't handle and that hit the main loop.
        GuiUtils.handleCrashesOnThisThread();

//        // Make log output concise.
        BriefLogFormatter.init();

        if (PlatformUtils.isMac()) {
            // We could match the Mac Aqua style here, except that (a) Modena doesn't look that bad, and (b)
            // the date picker widget is kinda broken in AquaFx and I can't be bothered fixing it.
            // AquaFx.style();
        }
        controller = loadController();
        IPController ipController = IloadIPController();

        //get the done button here and tringger the normal flow
        //get the value of the ip address from the ipList Observable variable and set the PeerAddress value
        //Monitor Peer address using UI on the Main Content section
        ipController.finishBtn.setOnMouseClicked(eh -> {

            peerAddresses = ipController.ipList.stream().map(IP::getIp)
                   .toList();

            try {
                loadWallet(primaryStage);
            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(WalletApplication.class.getName()).log(Level.SEVERE, null, ex);
            } catch (BlockStoreException ex) {
                java.util.logging.Logger.getLogger(WalletApplication.class.getName()).log(Level.SEVERE, null, ex);
            }

        });

        
        


        

       

                Scene sc = new Scene(ipController.getIpPane(),this.dimension.getWidth(), this.dimension.getHeight());


        primaryStage.setScene(sc);

        primaryStage.show();

    }

    private void loadWallet(Stage primaryStage) throws IOException, BlockStoreException {

        controller = loadController();
        
        
        primaryStage.setScene(controller.scene);
        //get the ip addressess from the listView Binding later to add it to peerGroup during wallet setup.
        startWalletAppKit(primaryStage);
        controller.scene().getAccelerators().put(KeyCombination.valueOf("Shortcut+F"), () -> walletAppKit().peerGroup().getDownloadPeer().close());
    }

    protected void startWalletAppKit(Stage primaryStage) throws IOException, BlockStoreException {

        Context.propagate(new Context());

        BriefLogFormatter.init();
        log.info("Starting up ...");
        // Tell bitcoinj to run event handlers on the JavaFX UI thread. This keeps things simple and means
        // we cannot forget to switch threads when adding event handlers. Unfortunately, the DownloadListener
        // we give to the app kit is currently an exception and runs on a library thread. It'll get fixed in
        // a future version.
        Threading.USER_THREAD = Platform::runLater;
        // Create the app kit. It won't do any heavyweight initialization until after we start it.
        setupWalletKit(null);

        if (walletAppKit.isChainFileLocked()) {
            informationalAlert("Already running", "This application is already running and cannot be started twice.");
            Platform.exit();
            return;
        }

        primaryStage.show();
//        
        WalletSetPasswordController.initEstimatedKeyDerivationTime();

        walletAppKit.addListener(new Service.Listener() {
            @Override
            public void failed(Service.State from, Throwable failure) {
                GuiUtils.crashAlert(failure);
            }
        }, Platform::runLater);

        WalletSetPasswordController.initEstimatedKeyDerivationTime();
        
        
        //2a12:a800:2:1:45:138:16:231

        if (!peerAddresses.isEmpty()) {
            

            PeerAddress[] peerArray = peerAddresses.stream()
                   
                    .map(ipAdd ->  {
                        
                        try {
                            
                            
                            return  PeerAddress.simple(InetAddress.getByName(ipAdd),params.getPort());
                        } catch (UnknownHostException ex) {
                            java.util.logging.Logger.getLogger(WalletApplication.class.getName()).log(Level.SEVERE, null, ex);
                            throw new RuntimeException(ex);
                        }
                    }).toArray(PeerAddress[]::new);
                        
            walletAppKit.setPeerNodes(peerArray);
            
                   }

//        
        ////    
    
        walletAppKit.startAsync();
        walletAppKit.awaitRunning();
//
//        PeerMonitorController peerMonitorController = new PeerMonitorController(params, walletAppKit.peerGroup());
////        
//
//        peerMonitorController.registerListners();
    }

    public void setupWalletKit(@Nullable DeterministicSeed seed)  {
        // If seed is non-null it means we are restoring from backup.

        walletAppKit = new FXWallketKit(
                network,
                preferredOutputScriptType,
                keyChainGroupStructure,
                Main.ROOT_DIR,
                "fresh-dec-14"
        ) {
            @Override
            protected void onSetupCompleted() {
                Platform.runLater(controller::onBitcoinSetup);
            }
        };
        // Now configure and start the appkit. This will take a second or two - we could show a temporary splash screen
        // or progress widget to keep the user engaged whilst we initialise, but we don't.
        if (network == BitcoinNetwork.REGTEST) {
            walletAppKit.connectToLocalHost();   // You should run a regtest mode bitcoind locally.
        }
        walletAppKit.setDownloadListener(controller.progressBarUpdater())
                .setBlockingStartup(false)
                .setAutoSave(true)
                .setUserAgent(applicationName, "1.0");
        if (seed != null) {
            walletAppKit.restoreWalletFromSeed(seed);
        }
    }

    @Override
    public void stop() throws Exception {
        walletAppKit.stopAsync();
        walletAppKit.awaitTerminated();
        // Forcibly terminate the JVM because Orchid likes to spew non-daemon threads everywhere.
        Runtime.getRuntime().exit(0);
    }

    protected String suffixFromNetwork(BitcoinNetwork network) {
        return switch (network) {
            case MAINNET ->
                "main";
            case TESTNET ->
                "test";
            case SIGNET ->
                "signet";
            case REGTEST ->
                "regtest";
        };
    }

    private IPController IloadIPController() {

        IPController ipController = new IPController();
        return ipController;
    }
    
    
    
    
}
