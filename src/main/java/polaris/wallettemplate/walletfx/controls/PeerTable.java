/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package polaris.wallettemplate.walletfx.controls;

import javafx.beans.Observable;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.util.Callback;
import org.bitcoinj.core.Peer;

/**
 *
 * @author michael
 */
public class PeerTable {

    private StringProperty ipAdress = new SimpleStringProperty(this, "ipAdress","");
    
    
   
    

//    private SimpleIntegerProperty protocolVersion  = new SimpleIntegerProperty(this, "protocolVersion", 0);

    public PeerTable(String ip
//            ,int protocol
    ) {
        
        
        this.ipAdress.set(ip);
        
//        this.protocolVersion.set(protocol);
        
        


    }
    
    
    public PeerTable(){
        
    }

    public static Callback<PeerTable, Observable[]> extractor = p -> new Observable[]{p.getIpAdress()
    };

    /**
     * @return the ipAdress
     */
    public StringProperty getIpAdress() {
        return ipAdress;
    }

//    /**
//     * @return the protocolVersion
//     */
//    public SimpleIntegerProperty getProtocolVersion() {
//        return protocolVersion;
//    }
//    
//    
//       /**
//     * @param protocolVersion the protocolVersion to set
//     */
//    public void setProtocolVersion(SimpleIntegerProperty protocolVersion) {
//        this.protocolVersion = protocolVersion;
//    }
//
//
//    /**
//     * @param ipAdress the ipAdress to set
//     */
//    public void setIpAdress(StringProperty ipAdress) {
//        this.ipAdress = ipAdress;
//    }

}
 
