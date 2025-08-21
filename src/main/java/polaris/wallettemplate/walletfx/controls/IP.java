/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package polaris.wallettemplate.walletfx.controls;

import javafx.beans.Observable;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.util.Callback;

/**
 *
 * @author michael
 */
public class IP {

    private final StringProperty ip = new SimpleStringProperty(this, "ip", "");

    public IP() {
    }

    public IP(String value) {
        this.ip.set(value);
    }
    
    
    

    /**
     * @return the ip
     */
    public String getIp() {
        return ip.get();
    }

    /**
     * @param ip the ip to set
     */
    public void setIp(String ip) {
        this.ip.set(ip);
    }

    
      public static Callback<IP, Observable[]> extractor = i -> new Observable[]

      {i.ip};
      
      
      
      @Override
      public String toString(){
          return ip.get();
      }
}
