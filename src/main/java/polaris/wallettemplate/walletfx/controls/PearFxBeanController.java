/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package polaris.wallettemplate.walletfx.controls;

/**
 *
 * @author michael
 */
public class PearFxBeanController {
    
    
    private PeerFXModel model;
    
    
    private PeerFXModelView view;
    
    
    
    
    public PearFxBeanController(PeerFXModel model,
            PeerFXModelView view){
        this.model = model;
        this.view = view;
    }
    
    
    
    public void setIPproperty(String reverseDns){
        model.setIPAddress(reverseDns);
    }
    
}
