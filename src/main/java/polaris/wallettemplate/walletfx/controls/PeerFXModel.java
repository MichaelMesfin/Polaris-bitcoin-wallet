/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package polaris.wallettemplate.walletfx.controls;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.bitcoinj.core.AddressMessage;
import org.bitcoinj.core.Peer;

/**
 *
 * @author michael
 */
public class PeerFXModel {

    private final IntegerProperty protocolVersion = new SimpleIntegerProperty(this, "protocol version", 0);

    private final StringProperty ipAddress = new SimpleStringProperty(this, "ipAdress", "");

    private final StringProperty addressMessage = new SimpleStringProperty(this, "address Message", "");

    private final SimpleLongProperty chainHeight = new SimpleLongProperty(this, "chain Height", 0);

    private final SimpleStringProperty feeFeeliter = new SimpleStringProperty(this, "fee filter", "");

    private final LongProperty pingTime = new SimpleLongProperty(this, "ping time", 0);

    private final LongProperty lastPingTime = new SimpleLongProperty(this, " last ping time", 0);
    
    
    private final Peer peer;

    public PeerFXModel(Peer peer) {
        
        this.peer = peer;
   
    }
    
    
    
    

    public final String getIPAddress() {
        return this.ipAddress.get();
    }

    public final void setIPAddress(String reverseDns) {

        this.ipAddress.setValue(reverseDns);

    }

    public StringProperty ipAddressProperty() {

        return ipAddress;

    }

    public final String getAddressMessage() {
        return this.addressMessage.get();
    }

    public final void setAddresssMessage(String message) {
        this.addressMessage.setValue(message);
    }

    public StringProperty messageProperty() {
        return this.addressMessage;
    }


    
    public final int getProtocolVersion(){
        
        return this.protocolVersion.get();
    }
    
    
    public final void setProtocolVersion(int version){
        this.protocolVersion.setValue(version);
    }

    
    public final IntegerProperty prtocolVersionProperty(){
        return this.protocolVersion;
    }
    
    
    
    
    public final void setChainHeight(long height){
        this.chainHeight.setValue(height);
    }

    /**
     * @return the chainHeight
     */
    public SimpleLongProperty chainHeightProperty() {
        return chainHeight;
    }

    /**
     * @param value
     */
    
    public final void setFeeFilter(String value){
        this.feeFeeliter.setValue(value);
    }

    /**
     *
     * @return
     */
    public final SimpleStringProperty feeFilterProperty() {
        return this.feeFeeliter;
    }
    
    
        public final void setPingTimeProperty(long pingTime){
        this.pingTime.setValue(pingTime);
    }


    /**
     * @return the pingTime
     */
    public LongProperty pingTimeProperty() {
        return pingTime;
    }
    
    /**
     *
     * @param lastPingTime
     */
    public final void setLastPingTime(long lastPingTime){
        this.lastPingTime.setValue(lastPingTime);
    }

    /**
     * @return the lastPingTime
     */
    public LongProperty lastPingTimeProperty() {
        return lastPingTime;
    }
    
    /**
     *
     * @return peer associated with the PeerFX
     */
    public Peer getPear(){
        return this.peer;
    }
    
}
