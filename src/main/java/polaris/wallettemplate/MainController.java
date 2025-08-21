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

package polaris.wallettemplate;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.util.Duration;
import org.bitcoinj.base.Coin;
import org.bitcoinj.base.utils.MonetaryFormat;
import org.bitcoinj.core.listeners.DownloadProgressTracker;
import polaris.wallettemplate.walletfx.MainUIOverride;

import polaris.wallettemplate.walletfx.controls.ClickableBitcoinAddress;
import polaris.wallettemplate.walletfx.utils.BitcoinUIModel;
import polaris.wallettemplate.walletfx.utils.GuiUtils;
import polaris.wallettemplate.walletfx.utils.TextFieldValidator;
import polaris.wallettemplate.walletfx.application.MainWindowController;
import polaris.wallettemplate.walletfx.application.WalletApplication;
import polaris.wallettemplate.walletfx.controls.NotificationBarPane;
import polaris.wallettemplate.walletfx.utils.easing.EasingMode;
import polaris.wallettemplate.walletfx.utils.easing.ElasticInterpolator;

/**
 * Gets created auto-magically by FXMLLoader via reflection. The widget fields are set to the GUI controls they're named
 * after. This class handles all the updates and event handling for the main UI.
 */
public class MainController extends MainWindowController {
    public HBox controlsBox;
    public Label balance;
    public Button sendMoneyOutBtn;
    public ClickableBitcoinAddress addressControl;

    
     
        public PeerMonitorController peerController;
               
    private final BitcoinUIModel model = new BitcoinUIModel();
    private NotificationBarPane.Item syncItem;
    private static final MonetaryFormat MONETARY_FORMAT = MonetaryFormat.BTC.noCode();
    
    

    private WalletApplication app;
    private NotificationBarPane notificationBar;

    // Called by FXMLLoader.
    public void initialize() {
        app = WalletApplication.instance();

        scene = new Scene(uiStack,app.dimension.getWidth(),
               app.dimension.getHeight()
        );
      
        TextFieldValidator.configureScene(scene);
        // Special case of initOverlay that passes null as the 2nd parameter because ClickableBitcoinAddress is loaded by FXML
        // TODO: Extract QRCode Pane to separate reusable class that is a more standard OverlayController instance
  
    }

    @Override
    public void controllerStart(Pane mainUI, String cssResourceName) {
        
        
        
        
        
        
        
        
        MainUIOverride mainUIOverride = new MainUIOverride();
        
        this.mainUI = mainUIOverride.getPane();

        
//        this.mainUI.setScaleX(0.9);
//        this.mainUI.setScaleY(0.9);
        this.balance = mainUIOverride.getBalance();
        
        this.sendMoneyOutBtn = mainUIOverride.getSendMoneyButton();
        
        mainUIOverride.getSettingButton().setOnAction(this::settingsClicked);
        
        mainUIOverride.getSendMoneyButton().setOnAction(this::sendMoneyOut);
        
        mainUIOverride.getPeersButton().setOnAction(this::peersClicked);
        
        
       this.addressControl = mainUIOverride.getAddressControl();
                
                
        addressControl.initOverlay(this, null);

        addressControl.setAppName(app.applicationName());
//        addressControl.setOpacity(0.0);
        

        // Configure the window with a StackPane so we can overlay things on top of the main UI, and a
        // NotificationBarPane so we can slide messages and progress bars in from the bottom. Note that
        // ordering of the construction and connection matters here, otherwise we get (harmless) CSS error
        // spew to the logs.
        notificationBar = new NotificationBarPane(mainUIOverride.getPane());
        
        
        // Add CSS that we need. cssResourceName will be loaded from the same package as this class.
        scene.getStylesheets().add(getClass().getResource(cssResourceName).toString());
        uiStack.getChildren().add(notificationBar);
        scene.getAccelerators().put(KeyCombination.valueOf("Shortcut+F"), () -> app.walletAppKit().peerGroup().getDownloadPeer().close());
    }

    @Override
    public void onBitcoinSetup() {
        model.setWallet(app.walletAppKit().wallet());
        addressControl.addressProperty().bind(model.addressProperty());
        balance.textProperty().bind(createBalanceStringBinding(model.balanceProperty()));
            
        balance.setFont(Font.font(24));
        
        // Don't let the user click send money when the wallet is empty.
        sendMoneyOutBtn.disableProperty().bind(model.balanceProperty().isEqualTo(Coin.ZERO));

        showBitcoinSyncMessage();
        model.syncProgressProperty().addListener(x -> {
            if (model.syncProgressProperty().get() >= 1.0) {
                readyToGoAnimation();
                if (syncItem != null) {
                    syncItem.cancel();
                    syncItem = null;
                }
            } else if (syncItem == null) {
                showBitcoinSyncMessage();
            }
        });
    }

    private static String formatCoin(Coin coin) {
        return MONETARY_FORMAT.format(coin).toString();
    }

    private static Binding<String> createBalanceStringBinding(ObservableValue<Coin> coinProperty) {
        return Bindings.createStringBinding(() -> formatCoin(coinProperty.getValue()), coinProperty);
    }

    private void showBitcoinSyncMessage() {
        syncItem = notificationBar.pushItem("Synchronising with the Bitcoin network", model.syncProgressProperty());
    }

    public void sendMoneyOut(ActionEvent event) {
        // Hide this UI and show the send money UI. This UI won't be clickable until the user dismisses send_money.
        overlayUI("/wallettemplate/send_money.fxml");
    }

    public void settingsClicked(ActionEvent event) {
        OverlayUI<WalletSettingsController> screen = overlayUI("/wallettemplate/wallet_settings.fxml");
        screen.controller.initialize(null);
    }
    
    
    public void peersClicked(ActionEvent event) {
        
        
       // intialize in the constructor
        if (peerController == null) {
            peerController = new PeerMonitorController(app.walletAppKit().params(), app.walletAppKit().peerGroup());

        }

        

        OverlayUI<PeerMonitorController> overlayUI = overlayUI(peerController.getPane(), peerController);

       
       
        
        }
    
    
    
    

    public void primaryClicked(ActionEvent event) {
        GuiUtils.informationalAlert("Unused button #1", "You can hook this up in your app");
    }

    public void secondaryClicked(ActionEvent event) {
        GuiUtils.informationalAlert("Unused button #2", "You can hook this up in your app");
    }

    @Override
    public void restoreFromSeedAnimation() {
        // Buttons slide out ...
        TranslateTransition leave = new TranslateTransition(Duration.millis(1200), controlsBox);
        leave.setByY(80.0);
        leave.play();
    }

    public void readyToGoAnimation() {
        // Buttons slide in and clickable address appears simultaneously.
        TranslateTransition arrive = new TranslateTransition(Duration.millis(1200), controlsBox);
        arrive.setInterpolator(new ElasticInterpolator(EasingMode.EASE_OUT, 1, 2));
        arrive.setToY(0.0);
        FadeTransition reveal = new FadeTransition(Duration.millis(1200), addressControl);
        reveal.setToValue(1.0);
        ParallelTransition group = new ParallelTransition(arrive, reveal);
        group.setDelay(NotificationBarPane.ANIM_OUT_DURATION);
        group.setCycleCount(1);
        group.play();
    }

    @Override
    public DownloadProgressTracker progressBarUpdater() {
        return model.getDownloadProgressTracker();
    }

}
