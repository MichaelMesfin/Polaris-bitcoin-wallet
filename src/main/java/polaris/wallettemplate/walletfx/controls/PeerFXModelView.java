/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package polaris.wallettemplate.walletfx.controls;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/**
 *
 * @author michael
 */
public class PeerFXModelView extends TableView<PeerFXModel>{
    
    
    private PeerFXModel model;
    
    
    private TableView<PeerFXModel> tableView =  new TableView<>();
    
    
    public PeerFXModelView(PeerFXModel model){
        this.model = model;
        
                hookUpChangeListners();

    }
    

    private void hookUpChangeListners() {
        model.ipAddressProperty().addListener(new ChangeListener<String>() {

            @Override
            public void changed(ObservableValue<? extends String> ov, String oldValue, String newValue) {
                System.out.println("Property i changed: old value = " + oldValue + ", new value = " + newValue);
            }
        });
    }
}
