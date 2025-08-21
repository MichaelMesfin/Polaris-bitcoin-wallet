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

import java.math.BigInteger;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import org.bitcoinj.base.Address;
import static org.bitcoinj.base.BitcoinNetwork.TESTNET;
import org.bitcoinj.base.Coin;
import static org.bitcoinj.base.Coin.CENT;
import static org.bitcoinj.base.Coin.COIN;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.crypto.AesKey;
import org.bitcoinj.crypto.ECKey;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.Wallet;
import polaris.wallettemplate.walletfx.utils.TextFieldValidator;
import polaris.wallettemplate.walletfx.utils.WTUtils;

import static org.bitcoinj.base.internal.Preconditions.checkState;
import org.bitcoinj.core.AbstractBlockChain;
import org.bitcoinj.core.StoredBlock;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.script.Script;
import static org.bitcoinj.testing.FakeTxBuilder.createFakeTx;
import static org.bitcoinj.testing.FakeTxBuilder.makeSolvedTestBlock;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static polaris.wallettemplate.walletfx.utils.GuiUtils.checkGuiThread;
import static polaris.wallettemplate.walletfx.utils.GuiUtils.crashAlert;
import static polaris.wallettemplate.walletfx.utils.GuiUtils.informationalAlert;
import polaris.wallettemplate.walletfx.application.WalletApplication;
import polaris.wallettemplate.walletfx.controls.BitcoinAddressValidator;
import polaris.wallettemplate.walletfx.overlay.OverlayController;
import polaris.wallettemplate.walletfx.overlay.OverlayableStackPaneController;

public class SendMoneyController implements OverlayController<SendMoneyController> {
    public Button sendBtn;
    public Button cancelBtn;
    public TextField address;
    public Label titleLabel;
    public TextField amountEdit;
    public Label btcLabel;

    private WalletApplication app;
    private OverlayableStackPaneController rootController;
    private OverlayableStackPaneController.OverlayUI<? extends OverlayController<SendMoneyController>> overlayUI;

    private Wallet.SendResult sendResult;
    private AesKey aesKey;

    @Override
    public void initOverlay(OverlayableStackPaneController overlayableStackPaneController, OverlayableStackPaneController.OverlayUI<? extends OverlayController<SendMoneyController>> ui) {
        rootController = overlayableStackPaneController;
        overlayUI = ui;
    }

    // Called by FXMLLoader
    public void initialize() {
        app = WalletApplication.instance();
        Coin balance = app.walletAppKit().wallet().getBalance();
        checkState(!balance.isZero());
        new BitcoinAddressValidator(app.walletAppKit().wallet(), address, sendBtn);
        new TextFieldValidator(amountEdit, text ->
                !WTUtils.didThrow(() -> checkState(Coin.parseCoin(text).compareTo(balance) <= 0)));
        amountEdit.setText(balance.toPlainString());
        address.setPromptText(new ECKey().toAddress(app.preferredOutputScriptType(), app.network()).toString());
    }

    public void cancel(ActionEvent event) {
        overlayUI.done();
    }

    public void send(ActionEvent event) {
        // Address exception cannot happen as we validated it beforehand.
        try {
            Coin amount = Coin.parseCoin(amountEdit.getText());
            Address destination = app.walletAppKit().wallet().parseAddress(address.getText());
            SendRequest req;
            
            
            
            
            
            if (amount.equals(app.walletAppKit().wallet().getBalance()))
                
                
                //fix the how the request is made;
                req = SendRequest.emptyWallet(destination);
            else
                req = SendRequest.to(destination, amount);
            req.aesKey = aesKey;
            // Don't make the user wait for confirmations for now, as the intention is they're sending it
            // their own money!
            req.allowUnconfirmed();
            
            /*
            
            The final pice.
            
            
            
            //-Dserver.port=
            
            
            
            
            */
            sendResult = app.walletAppKit().wallet().sendCoins(req);
            sendResult.awaitRelayed().whenComplete((result, throwable) -> {
                if (throwable == null) {
                    Platform.runLater(() -> overlayUI.done());
                } else {
                    // We died trying to empty the wallet.
                    crashAlert(throwable);
                }
            });
            sendResult.transaction().getConfidence().addEventListener((tx, reason) -> {
                if (reason == TransactionConfidence.Listener.ChangeReason.SEEN_PEERS)
                    updateTitleForBroadcast();
            });
            sendBtn.setDisable(true);
            address.setDisable(true);
            ((HBox)amountEdit.getParent()).getChildren().remove(amountEdit);
            ((HBox)btcLabel.getParent()).getChildren().remove(btcLabel);
            updateTitleForBroadcast();
        } catch (InsufficientMoneyException e) {
            informationalAlert("Could not empty the wallet",
                    "You may have too little money left in the wallet to make a transaction.");
            overlayUI.done();
        } catch (ECKey.KeyIsEncryptedException e) {
            askForPasswordAndRetry();
        }
    }

    private void askForPasswordAndRetry() {
        OverlayableStackPaneController.OverlayUI<WalletPasswordController> pwd = rootController.overlayUI("wallet_password.fxml");
        final String addressStr = address.getText();
        final String amountStr = amountEdit.getText();
        pwd.controller.aesKeyProperty().addListener((observable, old, cur) -> {
            // We only get here if the user found the right password. If they don't or they cancel, we end up back on
            // the main UI screen. By now the send money screen is history so we must recreate it.
            checkGuiThread();
            OverlayableStackPaneController.OverlayUI<SendMoneyController> screen = rootController.overlayUI("send_money.fxml");
            screen.controller.aesKey = cur;
            screen.controller.address.setText(addressStr);
            screen.controller.amountEdit.setText(amountStr);
            screen.controller.send(null);
        });
    }

    private void updateTitleForBroadcast() {
        final int peers = sendResult.transaction().getConfidence().numBroadcastPeers();
        titleLabel.setText(String.format("Broadcasting ... seen by %d peers", peers));
    }
    
    
    
//     public void testCompleteTxWithExistingInputs() throws Exception {
//        // Tests calling completeTx with a SendRequest that already has a few inputs in it
//
//        // Generate a few outputs to us
//        StoredBlock block = new StoredBlock(makeSolvedTestBlock(blockStore, OTHER_ADDRESS), BigInteger.ONE, 1);
//        Transaction tx1 = createFakeTx(TESTNET, COIN, myAddress);
//        wallet.receiveFromBlock(tx1, block, AbstractBlockChain.NewBlockType.BEST_CHAIN, 0);
//        Transaction tx2 = createFakeTx(TESTNET, COIN, myAddress);
//        assertNotEquals(tx1.getTxId(), tx2.getTxId());
//        wallet.receiveFromBlock(tx2, block, AbstractBlockChain.NewBlockType.BEST_CHAIN, 1);
//        Transaction tx3 = createFakeTx(TESTNET, CENT, myAddress);
//        wallet.receiveFromBlock(tx3, block, AbstractBlockChain.NewBlockType.BEST_CHAIN, 2);
//
//        SendRequest request1 = SendRequest.to(OTHER_ADDRESS, CENT);
//        // If we just complete as-is, we will use one of the COIN outputs to get higher priority,
//        // resulting in a change output
//        request1.shuffleOutputs = false;
//        wallet.completeTx(request1);
//        assertEquals(1, request1.tx.getInputs().size());
//        assertEquals(2, request1.tx.getOutputs().size());
//        assertEquals(CENT, request1.tx.getOutput(0).getValue());
//        assertEquals(COIN.subtract(CENT), request1.tx.getOutput(1).getValue());
//
//        // Now create an identical request2 and add an unsigned spend of the CENT output
//        SendRequest request2 = SendRequest.to(OTHER_ADDRESS, CENT);
//        request2.tx.addInput(tx3.getOutput(0));
//        // Now completeTx will result in one input, one output
//        wallet.completeTx(request2);
//        assertEquals(1, request2.tx.getInputs().size());
//        assertEquals(1, request2.tx.getOutputs().size());
//        assertEquals(CENT, request2.tx.getOutput(0).getValue());
//        // Make sure it was properly signed
//        request2.tx.getInput(0).getScriptSig().correctlySpends(
//                request2.tx, 0, null, null, tx3.getOutput(0).getScriptPubKey(), Script.ALL_VERIFY_FLAGS);
//
//        // However, if there is no connected output, we connect it
//        SendRequest request3 = SendRequest.to(OTHER_ADDRESS, CENT);
//        request3.tx.addInput(new TransactionInput(request3.tx, new byte[] {}, new TransactionOutPoint(0, tx3.getTxId())));
//        // Now completeTx will find the matching UTXO from the wallet and add its value to the unconnected input
//        request3.shuffleOutputs = false;
//        wallet.completeTx(request3);
//        assertEquals(1, request3.tx.getInputs().size());
//        assertEquals(1, request3.tx.getOutputs().size());
//        assertEquals(CENT, request3.tx.getOutput(0).getValue());
//
//        SendRequest request4 = SendRequest.to(OTHER_ADDRESS, CENT);
//        request4.tx.addInput(tx3.getOutput(0));
//        // Now if we manually sign it, completeTx will not replace our signature
//        wallet.signTransaction(request4);
//        byte[] scriptSig = request4.tx.getInput(0).getScriptBytes();
//        wallet.completeTx(request4);
//        assertEquals(1, request4.tx.getInputs().size());
//        assertEquals(1, request4.tx.getOutputs().size());
//        assertEquals(CENT, request4.tx.getOutput(0).getValue());
//        assertArrayEquals(scriptSig, request4.tx.getInput(0).getScriptBytes());
//    }
}
