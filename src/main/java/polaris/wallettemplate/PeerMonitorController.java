/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package polaris.wallettemplate;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import org.bitcoinj.base.Coin;
import org.bitcoinj.core.AddressMessage;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import polaris.wallettemplate.walletfx.controls.PeerFXModel;
import polaris.wallettemplate.walletfx.overlay.OverlayController;
import polaris.wallettemplate.walletfx.overlay.OverlayableStackPaneController;

/**
 *
 * @author michael
 */
public final class PeerMonitorController extends AnchorPane implements OverlayController<PeerMonitorController> {

    private static final Logger log = LoggerFactory.getLogger(PeerMonitorController.class);

    private OverlayableStackPaneController rootController;

    private OverlayableStackPaneController.OverlayUI<? extends OverlayController<PeerMonitorController>> overlayUI;

    private Task<Void> peerFXTask;

    private Task<Void> refreshTask;

    private final ConcurrentLinkedQueue<Peer> peerQueue;

    private final ConcurrentLinkedQueue<PeerFXModel> peerFXQueue;

    private NetworkParameters params;
    private PeerGroup peerGroup;

    ObservableList<PeerFXModel> peerList = FXCollections.observableArrayList();

    Set<Peer> removeSet = new HashSet<>();

    private final Map<Peer, String> reverseDnsLookups = new ConcurrentHashMap<>();

    private final Map<Peer, AddressMessage> addressMessages = new ConcurrentHashMap<>();

    private TableView<PeerFXModel> tableView = new TableView<>();

//
    TableColumn<PeerFXModel, String> columnIP;

    TableColumn<PeerFXModel, Integer> columnProtocolVersion;

    TableColumn<PeerFXModel, String> columnAddressMessage;

    TableColumn<PeerFXModel, Long> columnChainHeight;

    TableColumn<PeerFXModel, String> columnFeeFeeliter;

    TableColumn<PeerFXModel, Long> columnPingTime;

    TableColumn<PeerFXModel, Long> columnLastPingTime;

//
    HBox box = new HBox(3);

    Group group = new Group(box);

    Button close = new Button("x");

    private ChangeListener<Number> ChangeListener;

    SimpleDoubleProperty transformProperty = new SimpleDoubleProperty(null, "", 0.0);

    private Button rotate = new Button();

    private SimpleStringProperty rotationTitleProperty = new SimpleStringProperty(null, "", "Counter ClockWise");

    GridPane gridPane;
    
    AnchorPane pane;

    public PeerMonitorController(
            NetworkParameters params,
            PeerGroup peerGroup
    ) {
        
        
      

        createPeerFXModel();
        
        
        styleButton(close);
        styleButton(rotate);

        close.setOnAction(eh -> closeClicked());
        
        
        

        rotate.textProperty().bind(rotationTitleProperty);

         gridPane = new GridPane();

        pane = new AnchorPane(gridPane);

//        AnchorPane.setLeftAnchor(gridPane, 2.0);
//        AnchorPane.setRightAnchor(gridPane, 2.0);




           
           
           
        gridPane.addColumn(0, close);

        gridPane.addColumn(1, rotate);

        gridPane.addRow(1, tableView);
//        group.getChildren().add(gridPane);
        
        
       

        gridPane.rotateProperty().bind(transformProperty);

        rotate.setOnAction(eh -> {

            if (transformProperty.getValue().equals(90.0)) {

              
                Platform.runLater(() -> {
                     transformProperty.set(0.0);


                rotationTitleProperty.setValue("<-");
                });
               

            } else {
                Platform.runLater(() -> {
                       transformProperty.set(90.0);
                       
                rotationTitleProperty.setValue("<-");
                });
             

            }
        });

        this.peerQueue = new ConcurrentLinkedQueue<>();

        this.peerFXQueue = new ConcurrentLinkedQueue<>();

        this.params = params;
        this.peerGroup = peerGroup;

        registerListners();

        startTasks();

    }

    private void createPeerFXModel() {
        this.tableView = new TableView<>(peerList);

        this.columnIP = new TableColumn<>("ip");
        this.columnIP.setResizable(true);

        this.columnProtocolVersion = new TableColumn<>("protocolVersion");

        this.columnAddressMessage = new TableColumn<>("adress Message");
        
        this.columnAddressMessage.setResizable(true);

        this.columnChainHeight = new TableColumn<>("chain height");

        this.columnFeeFeeliter = new TableColumn<>("Fee Filter");

        this.columnPingTime = new TableColumn<>("ping time");
        
        
        this.columnLastPingTime = new TableColumn<>("Last ping time");

        this.columnIP.setCellValueFactory((CellDataFeatures<PeerFXModel, String> p) -> p.getValue().ipAddressProperty());

        this.columnAddressMessage.setCellValueFactory((CellDataFeatures<PeerFXModel, String> p) -> p.getValue().messageProperty());

        this.columnProtocolVersion.setCellValueFactory((CellDataFeatures<PeerFXModel, Integer> p) -> {

            return p.getValue().prtocolVersionProperty().asObject();
        });

        this.columnChainHeight.setCellValueFactory(((CellDataFeatures<PeerFXModel, Long> p) -> p.getValue().chainHeightProperty().asObject()));

        this.columnFeeFeeliter.setCellValueFactory((CellDataFeatures<PeerFXModel, String> p) -> p.getValue().feeFilterProperty());

        this.columnPingTime.setCellValueFactory((CellDataFeatures<PeerFXModel, Long> p) -> p.getValue().pingTimeProperty().asObject());
        
        
        this.columnLastPingTime.setCellValueFactory((CellDataFeatures<PeerFXModel, Long> p) -> p.getValue().lastPingTimeProperty().asObject());

        tableView.getColumns().add(columnIP);


        tableView.getColumns().add(columnAddressMessage);

        tableView.getColumns().add(this.columnChainHeight);

        tableView.getColumns().add(this.columnFeeFeeliter);
        
        tableView.getColumns().add(columnProtocolVersion);


        tableView.getColumns().add(this.columnPingTime);
        
        tableView.getColumns().add(this.columnLastPingTime);
        
        
        tableView.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
    }


    @Override
    public void initOverlay(OverlayableStackPaneController overlayableStackPaneController,
            OverlayableStackPaneController.OverlayUI<? extends OverlayController<PeerMonitorController>> ui) {

        rootController = overlayableStackPaneController;

        overlayUI = ui;

    }

    public void registerListners() {

//        
//        peerList.addListener(new ListChangeListener<PeerFXModel>() {
//            @Override
//            public void onChanged(ListChangeListener.Change<? extends PeerFXModel> change) {
//                System.out.println("\tstrings = "
//                        + change.getList());
//            }
//        });
        peerGroup.addConnectedEventListener((peer, peerCount) -> {

            peerQueue.add(peer);

        });
        peerGroup.addDisconnectedEventListener((peer, peerCount) -> {
            /*remove peer ffrom the queue and peer table*/

            peerQueue.remove(peer);

            addressMessages.remove(peer);
            reverseDnsLookups.remove(peer);

            removeSet.add(peer);

        });

    }

    private void startTasks() {

        peerFXTask = new Task<Void>() {

            @Override
            protected Void call() throws Exception {
                while (true) {

                    while (!peerQueue.isEmpty()) {

                        Peer p = peerQueue.poll();

                        PeerFXModel peerFX = new PeerFXModel(p);

                        peerFXQueue.add(peerFX);

                        getReverseDNS(p);

                        getAddressMessage(p);

                    }

                }

            }
        };

        refreshTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {

                while (true) {
                    while (!peerFXQueue.isEmpty()) {

                        PeerFXModel peerFX = peerFXQueue.poll();

                        Peer p = peerFX.getPear();

                        Platform.runLater(() -> {

                            List<PeerFXModel> removedPeers = peerList.stream().filter(pfx -> removeSet.contains(pfx.getPear())).toList();

                            peerList.removeAll(removedPeers);

                            peerFX.setChainHeight(p.getBestHeight());

                            Coin feeFilter = p.getFeeFilter();
                            String fft = feeFilter != null ? feeFilter.toFriendlyString() : "";

                            peerFX.setFeeFilter(fft);

                            peerFX.setProtocolVersion(p.getVersionMessage().clientVersion);

                            p.pingInterval().map(name -> name.getSeconds()).ifPresentOrElse(sec
                                    -> peerFX.setPingTimeProperty(sec),
                                    () -> peerFX.setPingTimeProperty(0));

                            p.lastPingInterval().map(Duration::getSeconds)
                                    .ifPresentOrElse(
                                            dur -> peerFX.setPingTimeProperty(dur),
                                            () -> peerFX.lastPingTimeProperty().setValue(0)
                                    );

                            peerList.add(peerFX);

                            peerList.stream()
                                    .filter(pfxm -> reverseDnsLookups.containsKey(pfxm.getPear()))
                                    .forEach(pfx -> pfx.setIPAddress(reverseDnsLookups.get(pfx.getPear())));

                            peerList.stream()
                                    .filter(pfxm -> addressMessages.containsKey(pfxm.getPear()))
                                    .forEach(pfxm -> {

                                        AddressMessage addressMessage = addressMessages.get(pfxm.getPear());

                                        pfxm.setAddresssMessage(addressMessage != null ? addressMessage.toString() : "");
                                    });

                        });

                        Thread.sleep(1000);
                    }
                }

            }
        };

//
        Thread peerFXThread = new Thread(peerFXTask);
        peerFXThread.setDaemon(true);

        peerFXThread.start();

        Thread refreshThread = new Thread(refreshTask);

        refreshThread.setDaemon(true);

        refreshThread.start();

    }

    public void closeClicked() {
        overlayUI.done();

    }

    public void getReverseDNS(Peer peer) {

        Thread.ofVirtual().start(() -> {

            String reverseDns = peer.getAddress().getAddr().getCanonicalHostName();

            reverseDnsLookups.put(peer, reverseDns);
        });

    }

    public void getAddressMessage(Peer peer) {

        Thread.ofVirtual().start(() -> {

            AddressMessage addressMessage;
            try {
                addressMessage = peer.getAddr().get(15, TimeUnit.SECONDS);
                addressMessages.put(peer, addressMessage);

            } catch (InterruptedException | ExecutionException | TimeoutException ex) {
                java.util.logging.Logger.getLogger(PeerMonitorController.class.getName()).log(Level.SEVERE, null, ex);
            }

        });

    }

    public Node getPane() {
        return pane;
    }

    
    
    private void styleButton(Button button){
         button.setPadding(new Insets(5, 10, 5, 10));

        button.setMinWidth(50);
    }
}
