/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package polaris.wallettemplate.walletfx;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import org.bitcoinj.base.Address;
import org.bitcoinj.base.ScriptType;
import org.bitcoinj.crypto.ECKey;
import polaris.wallettemplate.walletfx.application.WalletApplication;
import polaris.wallettemplate.walletfx.controls.ClickableBitcoinAddress;

/**
 *
 * @author michael
 */
public class MainUIOverride {

    private Label balance;

    private Label amount;

    private HBox balanceSection;

    private HBox controlBox;

    private ImageView imageView;

    private ClickableBitcoinAddress addressControl;

    private ButtonBar walletButtons;
    
    private  Button sendMoneyButton;
    
    private  Button peersButton;
    
    private Button settingButton;
//    
//    
//    private final double width = WalletApplication.instance().dimension.getWidth();
//    private final double height =   WalletApplication.instance().dimension.getHeight();
    
    private final BorderPane pane;
    
    
    
    public MainUIOverride(){
        pane = new BorderPane();
        
        pane.setPadding(new Insets(10,10,10,10));
        
        
        
        
                
                
        //test address
   

        topSection();
        
        
    }
    
    private void topSection(){
        
        try {
            addressControl = new ClickableBitcoinAddress();
            
            
            
           
        } catch (IOException ex) {
            
            Logger.getLogger(MainUIOverride.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }

           String family = Font.getDefault().getFamily();

        double size = Font.getDefault().getSize();
        
         balance = new Label("Balance");
         amount = new Label("BTC");
         
         getBalance().setFont(Font.font(family,FontWeight.BOLD,25));
         
         
         
         balanceSection = new HBox(5);
         
         
         getBalanceSection().setTranslateX(10);
         
         getBalanceSection().getChildren().addAll(getBalance(), getAmount());
         
         
         
         walletButtons = new ButtonBar();
         
         peersButton = new Button("Peers");
        
        
         settingButton = new Button("Settings");
        
        
        
        
        fatButton(getSettingButton());
        fatButton(getPeersButton());
        
        sendMoneyButton = new Button("Send Money");

        Background background = new Background(new BackgroundFill(Color.LIGHTGREEN, null, null));

        getSendMoneyButton().setBackground(background);

     

        getSendMoneyButton().setFont(Font.font(family, FontWeight.BOLD, size));
        
        
        fatButton(getSendMoneyButton());
        
        
        GridPane gridPane = new GridPane();
        
        gridPane.add(getSettingButton(), 0, 0);
        gridPane.add(getSendMoneyButton(), 1, 0);
        
        
        gridPane.add(getPeersButton(), 1, 1,2,1);
        
        gridPane.setVgap(10);
        gridPane.setHgap(20);

        getWalletButtons().getButtons().add(gridPane);

        
        
        GridPane topLeftPane = new GridPane();
        
//         getBalanceSection(),
//                addressControl,
        topLeftPane.add(getBalanceSection(), 0, 0);
        topLeftPane.add(getAddressControl(), 0, 1,2,1);
        topLeftPane.setVgap(10);
        
        
        
        
//
//        HBox topSection = new HBox(10,
//               topLeftPane,
//                 getWalletButtons());
        FlowPane topSection = new FlowPane(Orientation.HORIZONTAL,10,10,topLeftPane, walletButtons);
        
        
        
        
//        walletButtons.setTranslateX(-100);
       
        
        getPane().setTop(topSection);
        
        
        getPane().autosize();
        
        pane.setSnapToPixel(true);
        
        this.sendMoneyButton.setOnMouseClicked(eh -> {
            
            
            sendMoneyToWallet();
        });
        
        
    }

    /**
     * @return the balance
     */
    public Label getBalance() {
        return balance;
    }

    /**
     * @return the amount
     */
    public Label getAmount() {
        return amount;
    }

    /**
     * @return the balanceSection
     */
    public HBox getBalanceSection() {
        return balanceSection;
    }

    /**
     * @return the controlBox
     */
    public HBox getControlBox() {
        return controlBox;
    }

    /**
     * @return the imageView
     */
    public ImageView getImageView() {
        return imageView;
    }

    /**
     * @return the addressControl
     */
    public ClickableBitcoinAddress getAddressControl() {
        return addressControl;
    }

    /**
     * @return the walletButtons
     */
    public ButtonBar getWalletButtons() {
        return walletButtons;
    }

    /**
     * @return the pane
     */
    public BorderPane getPane() {
        return pane;
    }

    private void fatButton(Button button) {

        button.setPadding(new Insets(10, 15, 10, 15));

        button.setMinWidth(100);
//
//        Background background = new Background(new BackgroundFill(Color.ORANGE, null, null));
//
//        button.setBackground(background);
//        
//        
//.fat-button {
//    -fx-padding: 10 15 10 15;
//    -fx-min-width: 100;
//    -fx-base: whitesmoke;
//}

    }

    /**
     * @return the sendMoneyButton
     */
    public Button getSendMoneyButton() {
        return sendMoneyButton;
    }

    /**
     * @return the peersButton
     */
    public Button getPeersButton() {
        return peersButton;
    }

    /**
     * @return the settingButton
     */
    public Button getSettingButton() {
        return settingButton;
    }

    private void sendMoneyToWallet() {
        
        // all you are is doing sending money to a wallet.
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    
    
    
    
    
    
    
    
}
