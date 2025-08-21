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
import com.gluonhq.attach.storage.StorageService;
import com.gluonhq.attach.util.Services;
import java.io.File;
import java.net.URISyntaxException;
import java.util.Comparator;
import java.util.Objects;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.bitcoinj.base.BitcoinNetwork;
import org.bitcoinj.base.ScriptType;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.wallet.UnreadableWalletException;
import polaris.util.TransactionBuilder;
import static polaris.util.TransactionBuilder.loadBlockFromFile;
import polaris.util.WalletUtil;
import polaris.wallettemplate.WalletTemplate;

/**
 * Proxy JavaFX {@link Application} that delegates all functionality to
 * {@link WalletTemplate}
 */
public class Main extends Application {

    private static final BitcoinNetwork network = BitcoinNetwork.MAINNET;
    private static final ScriptType PREFERRED_OUTPUT_SCRIPT_TYPE = ScriptType.P2WPKH;
    private static final String APP_NAME = "WalletTemplate";

    public static final File ROOT_DIR;

//    
//    public static final double SCREEN_WIDTH;
//    
//        public static final double SCREEN_HEIGHT;
    static {
        ROOT_DIR = Services.get(StorageService.class)
                .flatMap(StorageService::getPrivateStorage)
                .orElseGet(() -> new File("."));

//        Services.get(DisplayService.class)
//                .ifPresentOrElse(ds -> {
//                    
//            Dimension2D screenResolution = ds.getScreenResolution();
//
//            
//         SCREEN_WIDTH =    screenResolution.getWidth();
//                        },
//                        
//                        () -> {
//                            
//                            SCREEN_WIDTH = 600;
//                        });
    }

    private final AppDelegate delegate;

    public static void main(String[] args) {
        launch(args);
    }

    public Main() throws URISyntaxException, UnreadableWalletException {
        delegate = new WalletTemplate(APP_NAME, network, PREFERRED_OUTPUT_SCRIPT_TYPE);
    }

    @Override
    public void init() throws Exception {
        delegate.init(this);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
//        delegate.start(primaryStage);

//        
        
        Block block = TransactionBuilder
                .loadBlockFromFile("/home/michael/NetBeansProjects/btcJFX/complete_tx_with_existing_inputs");

        TransactionBuilder txBuilder = new TransactionBuilder(
                new File("/home/michael/Documents/wallet/my-wallet/polaris.wallet"),
                new File("/home/michael/Documents/wallet/my-wallet/polaris_refresh.spvchain"),
                block
        );

        BlockStore str = txBuilder.getStore();
        
        
        System.out.println(txBuilder.getWallet().toString());

        Transaction tx = txBuilder.getWallet().getPendingTransactions()
                .stream().sorted(Comparator.comparing((Transaction t) -> {
                    return t.updateTime().get();
                }).reversed())
                .limit(1).findAny().get();
        
        
        Transaction feederParentTx = tx.getInput(0).getParentTransaction();
        Objects.requireNonNull(feederParentTx);
        
        
        
        txBuilder.confirmUnonfirmedChange(str.getChainHead(), tx, feederParentTx);
        
        System.out.println(txBuilder.getWallet().toString());
        
        
        txBuilder.saveWallet(new File("/home/michael/Documents/wallet/my-wallet/polaris_confirmed.wallet"));
//
//
//
//        
//
//
//        System.out.println(str.getChainHead().getHeader().toString());
//        txBuilder.spendUnconfirmedChange(pTX, str.getChainHead());
//      TransactionBuilder tb =   new TransactionBuilder();
//      
//      tb.completeTxWithExistingInputs();
    }

    @Override
    public void stop() throws Exception {
        delegate.stop();
    }
}
