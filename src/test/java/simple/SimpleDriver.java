/**
 * Project: STMv2 Package: simple File: SimpleDriver.java
 * 
 * @author sidmishraw Last modified: Dec 18, 2017 11:15:49 PM
 */
package simple;

import java.util.Arrays;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.rits.cloning.Cloner;
import stm.STM;
import stm.TVar;
import stm.Transaction;

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
  private static final STM stm = new STM();

  /**
   * Makes a transaction that adds 1001 to the 3rd element of the array stored in a memory cell.
   * 
   * @param tVar the memory cell containing the array
   * @return the transaction
   */
  private static Transaction makeT1(TVar<Integer[]> tVar) {

    Transaction transaction = Transaction.builder().stm(stm).action(t -> {

      Integer[] arr = t.read(tVar); // read the contents of the tVar
      arr[2] += 1001; // update the value

      return t.write(tVar, arr); // write the contents to the tVar
    }).action(t -> {
      logger.info("Logging from the then clause!");

      return true;
    }).build();

    return transaction;
  }

  /**
   * Makes a transaction that deducts 1000 from the 3rd element of the array stored in a memory
   * cell.
   * 
   * @param tVar the memory cell containing the array
   * @return the transaction
   */
  private static Transaction makeT2(TVar<Integer[]> tVar) {

    Transaction transaction = Transaction.builder().stm(stm).action(t -> {

      Integer[] arr = t.read(tVar); // read the contents of the tVar
      arr[2] -= 1000; // modify the 3rd element

      return t.write(tVar, arr); // write the contents to the tVar
    }).build();

    return transaction;
  }

  /**
   * @param args
   */
  public static void main(String[] args) {

    // let my STM store an array of 5 ints [1,2,3,4,5] in one of its memory cells
    TVar<Integer[]> tVar = stm.newTVar(new Integer[] {1, 2, 3, 4, 5});

    // run the transactions
    // should add 1001 and deduct 3000
    // effectively, it must be 3 + 1001 - 3000 = -1996
    // Note: make sure not to run the same transaction run multiple times. That might cause shared
    // state corruption.
    // Use a transaction maker - builder function to construct transactions with logic like
    // makeT1(tVar) and
    // makeT2(tVar). Also, since transactions are threads, it makes sense to execute them again
    // after they are
    // done executing.
    stm.exec(makeT2(tVar), makeT1(tVar), makeT2(tVar), makeT2(tVar));

    // A logging transaction to check the contents of the tVar. It is a read-only transaction.
    stm.exec(Transaction.builder().stm(stm).action(t -> {

      Integer[] arr = t.read(tVar);
      Arrays.asList(arr).forEach(e -> logger.info("member = " + e));

      return true;
    }).build());
  }

  /**
   * Testing out the Java-Deep cloning library. The library uses reflection for deep cloning.
   */
  @Test
  public void testCloner() {
    Integer[] arr = new Integer[] {1, 2, 3, 4, 5};
    Cloner cloner = new Cloner();
    Integer[] newArr = cloner.deepClone(arr);
    System.out.println("arr = " + arr);
    System.out.println("newArr = " + newArr);
    System.out.println("arr == newArr = " + newArr.equals(arr));
  }
}
