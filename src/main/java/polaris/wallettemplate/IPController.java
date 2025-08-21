/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package polaris.wallettemplate;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Rotate;
import polaris.wallettemplate.walletfx.application.WalletApplication;
import polaris.wallettemplate.walletfx.controls.IP;
import polaris.wallettemplate.walletfx.overlay.OverlayController;
import polaris.wallettemplate.walletfx.overlay.OverlayableStackPaneController;
import static polaris.wallettemplate.walletfx.utils.GuiUtils.checkGuiThread;

/**
 *
 * @author michael
 */
public class IPController extends AnchorPane implements OverlayController<IPController> {

    public Button addBtn;

    public TextField ipField;

    private WalletApplication app;

    private OverlayableStackPaneController rootController;

    private OverlayableStackPaneController.OverlayUI<? extends OverlayController<IPController>> overlayUI;

    public final ObservableList<IP> ipList = FXCollections.observableArrayList(IP.extractor);

    private ListView<IP> listView;
    
    private final StackPane ipPane;
    
    
    
    public Button finishBtn;
    
    
    private HBox hBox;
    
    
     private VBox vBox;

    public IPController() {
        this.ipPane = new StackPane();
        
        this.vBox =  new VBox(30);
        
        
        this.vBox.setPadding(new Insets(30, 30, 30, 30));
        askForIP();
        
        
        
      
        ipPane.getChildren().add(vBox);
    }
    
    
    

    @Override
    public void initOverlay(OverlayableStackPaneController overlayableStackPaneController,
            OverlayableStackPaneController.OverlayUI<? extends OverlayController<IPController>> ui) {
        rootController = overlayableStackPaneController;

        overlayUI = ui;
        

    }

    public void initialize() {
        app = WalletApplication.instance();
//        
//        
//        
       rootController.overlayUI(getIpPane(),this);
        
        

    }

    
    private void createButtonAction() {
        System.out.println("Create");
        
        
        IP ip = new IP(ipField.getText());
        ipList.add(ip);
        // and select it
        listView.getSelectionModel().select(ip);
    }

    public void cancel(ActionEvent event) {
        overlayUI.done();
    }
    
    
    private void askForIP() {
        

        
        
        
//        ipList.add(new IP("127.1.1.1"));
        
        
        listView = new ListView(ipList);

        
//        listView.getSelectionModel().selectFirst();

        
        Label label = new Label("add Peer Ip Address Below");
        ipField = new TextField("10.1.10.1");
        
      

        addBtn = new Button("add");
        
        
        finishBtn = new Button("Done");
        
        
        finishBtn.disableProperty().bind(listView.getSelectionModel().selectedItemProperty().isNull());
        
       

        hBox = new HBox(5,ipField, addBtn,finishBtn);
        
        
        vBox.getChildren().addAll(label,hBox,listView);
        
        addBtn.setOnMouseClicked(eh -> {
        
        createButtonAction();
        
        });
      

    }

    
    
    public void clockWise(){
        
        
        Rotate counterClockWise = new Rotate(-90);
        
        
        
        ipPane.getTransforms().add(counterClockWise);
        
    }
    
    
    
    public void counterClockWise(){
     
          Rotate clockWise = new Rotate(90);
        ipPane.getTransforms().add(clockWise);
    }
    
    
    /**
     * @return the ipPane
     */
    public StackPane getIpPane() {
        return ipPane;
    }

}
