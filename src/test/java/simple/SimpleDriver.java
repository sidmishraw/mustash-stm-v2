/**
 * Project: STMv2
 * Package: simple
 * File: SimpleDriver.java
 * 
 * @author sidmishraw
 *         Last modified: Dec 18, 2017 11:15:49 PM
 */
package simple;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import stm.MemoryCell;
import stm.STM;
import stm.Transaction;
import stm.utils.Transactions;

/**
 * @author sidmishraw
 *
 *         Qualified Name: simple.SimpleDriver
 *
 */
public class SimpleDriver {
    
    /**
     * 
     */
    private static final Logger logger = LoggerFactory.getLogger(SimpleDriver.class);
    
    /**
     * My STM
     */
    private static final STM    stm    = new STM();
    
    /**
     * Makes a transaction that adds 1001 to the 3rd element of the array stored in a memory cell.
     * 
     * @param memCell
     *            the memory cell containing the array
     * @return the transaction
     */
    private static Transaction makeT1(MemoryCell<Integer[]> memCell) {
        return Transactions.newT(stm).begin((t) -> {
            Integer[] arr = t.read(memCell);
            Integer[] newArr = Arrays.copyOf(arr, 5); // get a new copy
            newArr[2] += 1001; // update the value
            return t.write(memCell, newArr);
        }).then(t -> {
            logger.info("Logging from the then clause!");
            return true;
        }).end().done();
    }
    
    /**
     * Makes a transaction that deducts 1000 from the 3rd element of the array stored in a memory cell.
     * 
     * @param memCell
     *            the memory cell containing the array
     * @return the transaction
     */
    private static Transaction makeT2(MemoryCell<Integer[]> memCell) {
        return Transactions.newT(stm).begin((t) -> {
            Integer[] arr = t.read(memCell);
            Integer[] newArr = Arrays.copyOf(arr, 5); // get a new copy
            newArr[2] -= 1000; // update the value
            return t.write(memCell, newArr);
        }).end().done();
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        
        // let my STM store an array of 5 ints [1,2,3,4,5] in one of its memory cells
        MemoryCell<Integer[]> memCell = stm.newMemCell(new Integer[] { 1, 2, 3, 4, 5 });
        
        // build a transaction
        Transaction t1 = makeT1(memCell);
        
        // build a transaction
        Transaction t2 = makeT2(memCell);
        
        // run the transactions
        // should add 1001 and deduct 3000
        // effectively, it must be 3 + 1001 - 3000 = -1996
        // stm.exec(t1, t2, t2, t2);
        stm.exec(t2, t1, t2, t2);
        // stm.exec(t2, t2, t1, t2);
        // stm.exec(t2, t2, t2, t1);
        
        stm.exec(Transactions.newT(stm).begin((t) -> {
            Integer[] arr = t.read(memCell);
            Arrays.asList(arr).forEach(e -> logger.info("member = " + e));
            return true;
        }).end().done());
    }
}
