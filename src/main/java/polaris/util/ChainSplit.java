
package polaris.util;

import java.nio.ByteBuffer;
import org.bitcoinj.base.Address;
import org.bitcoinj.base.BitcoinNetwork;
import static org.bitcoinj.base.Coin.FIFTY_COINS;
import static org.bitcoinj.base.Coin.ZERO;
import org.bitcoinj.base.ScriptType;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.ProtocolException;
import org.bitcoinj.core.PrunedException;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.crypto.ECKey;
import org.bitcoinj.wallet.Wallet;
import static org.junit.Assert.assertEquals;

/**
 *
 * @author michael
 */
public class ChainSplit {
    
private NetworkParameters params =  NetworkParameters.of(BitcoinNetwork.MAINNET);    


    private Wallet wallet;
    private BlockChain chain;
    private Address coinsTo;
    private Address coinsTo2;
    private Address someOtherGuy;

    public void confidenceBuilding() throws VerificationException, PrunedException, InsufficientMoneyException{
         Block b1 = params.getGenesisBlock().createNextBlock(coinsTo);
        chain.add(b1);
        assertEquals(FIFTY_COINS, wallet.getBalance());
        Address dest = new ECKey().toAddress(ScriptType.P2PKH, BitcoinNetwork.TESTNET);
        Transaction spend = wallet.createSend(dest, FIFTY_COINS);
        // We do NOT confirm the spend here. That means it's not considered to be pending because createSend is
        // stateless. For our purposes it is as if some other program with our keys created the tx.
        //
        // genesis -> b1 (receive 50) --> b2
        //                            \-> b3 (external spend) -> b4
        Block b2 = b1.createNextBlock(someOtherGuy);
        chain.add(b2);
        Block b3 = b1.createNextBlock(someOtherGuy);
        b3.addTransaction(spend);
        b3.solve();
        chain.add(roundtrip(b3));
        // The external spend is now pending.
        assertEquals(ZERO, wallet.getBalance());
        Transaction tx = wallet.getTransaction(spend.getTxId());
        assertEquals(TransactionConfidence.ConfidenceType.PENDING, tx.getConfidence().getConfidenceType());
        Block b4 = b3.createNextBlock(someOtherGuy);
        chain.add(b4);
        // The external spend is now active.
        assertEquals(ZERO, wallet.getBalance());
        assertEquals(TransactionConfidence.ConfidenceType.BUILDING, tx.getConfidence().getConfidenceType());
    }
    
    
    
    private Block roundtrip(Block b2) throws ProtocolException {
        return params.getDefaultSerializer().makeBlock(ByteBuffer.wrap(b2.serialize()));
    }

    
    
    
}
