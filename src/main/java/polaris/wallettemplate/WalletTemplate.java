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

import java.io.File;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Pane;
import org.bitcoinj.base.BitcoinNetwork;
import org.bitcoinj.base.ScriptType;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import org.bitcoinj.wallet.UnreadableWalletException;
import polaris.wallettemplate.walletfx.application.WalletApplication;

/**
 * Template implementation of WalletApplication
 */
public class WalletTemplate extends WalletApplication {

    public WalletTemplate(String applicationName, BitcoinNetwork network, ScriptType preferredOutputScriptType) throws URISyntaxException, UnreadableWalletException {
        super(applicationName, network, preferredOutputScriptType);
    }

    @Override
    protected MainController loadController() throws IOException {
        // Load the GUI. The MainController class will be automagically created and wired up.

        URL location = getClass().getResource("/wallettemplate/main.fxml");
        FXMLLoader loader = new FXMLLoader(location);

        Pane mainUI = loader.load();
        MainController controller = loader.getController();

//here add the main ui from the ipcontroller.
        controller.controllerStart(mainUI, "/wallettemplate/wallet.css");
        return controller;
    }
    
    
    
}
