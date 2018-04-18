/**
 * Project: STMv2 Package: simple File: SimpleDriver.java
 * 
 * @author sidmishraw Last modified: Dec 18, 2017 11:15:49 PM
 */
package simple;

import java.util.Objects;
import java.util.Scanner;
import java.util.function.Function;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rits.cloning.Cloner;

import stm.STM;
import stm.TVar;
import stm.Transaction;
import stm.Value;

/**
 * @author sidmishraw
 *         Qualified Name: simple.SimpleDriver
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
   * Creates a transactional action that adds 1001 to the 3rd element of the TArray stored in a
   * memory cell.
   * 
   * @param tvar
   *          The transactional variable holding the TArray.
   * @return The transactional action.
   */
  private static Function<Transaction, Boolean> add1001(TVar tvar) {
    return (t) -> {
      TArray tArr = t.read(tvar, TArray.class);
      tArr.data[2] += 1001;
      return t.write(tvar, tArr);
    };
  }
  
  /**
   * Creates a transactional action that deducts 1000 from the 3rd element of the array stored in a
   * memory
   * cell.
   * 
   * @param tvar
   *          The memory cell containing the array.
   * @return The transactional action.
   */
  private static Function<Transaction, Boolean> subtract1000(TVar tvar) {
    return (t) -> {
      TArray tarr = t.read(tvar, TArray.class);
      tarr.data[2] -= 1000;
      return t.write(tvar, tarr);
    };
  }
  
  /**
   * @param args
   */
  @SuppressWarnings({ "unchecked" })
  public static void main(String[] args) {
    
    // let my STM store an array of 5 ints [1,2,3,4,5] in one of its memory cells
    //
    TVar tvar = stm.newTVar(new TArray(1, 2, 3, 4, 5));
    
    stm.printState();
    
    // Run the transactions
    //
    // should add 1001 and deduct 3000
    // effectively, it must be 3 + 1001 - 3000 = -1996
    // Note: make sure not to run the same transaction run multiple times. That might cause shared
    // state corruption.
    // Use a transaction maker - builder function to construct transactions with logic like
    // makeT1(tVar) and
    // makeT2(tVar). Also, since transactions are threads, it makes sense to execute them again
    // after they are
    // done executing.
    //
    stm.perform(add1001(tvar));
    stm.perform(subtract1000(tvar));
    
    // delete the tVar and invalidate the transactional actions
    //
    stm.deleteTVar(tvar);
    
    stm.perform(subtract1000(tvar));
    stm.perform(subtract1000(tvar));
    
    try (Scanner sc = new Scanner(System.in)) {
      sc.nextLine();  // hold till input
    }
    
    stm.printState();
    
    // The memory cell has been removed from the STM and cannot be used further.
    // However, the tvar still refers to it. So, as soon as tvar goes out of scope
    // the old memory cell can be reclaimed by the garbage collector.
    //
    logger.info("tvar = " + (Objects.isNull(tvar) ? "null" : tvar.toString()));
  }
  
  /**
   * Testing out the Java-Deep cloning library. The library uses reflection for deep cloning.
   */
  @Test
  public void testCloner() {
    Integer[] arr = new Integer[] { 1, 2, 3, 4, 5 };
    Cloner cloner = new Cloner();
    Integer[] newArr = cloner.deepClone(arr);
    System.out.println("arr = " + arr);
    System.out.println("newArr = " + newArr);
    System.out.println("arr == newArr = " + newArr.equals(arr));
  }
}

/**
 * A sample value that can be stored in the STM.
 * Qualified Name: simple.TArray
 */
class TArray implements Value {
  Integer[] data;
  
  /**
   * Creates a new TArray instance.
   * 
   * @param integers
   *          The elements in the data.
   */
  public TArray(Integer... integers) {
    this.data = integers;
  }
  
  @Override
  public Value makeCopy() {
    TArray ta = new TArray();
    Integer[] arr = new Integer[this.data.length];
    for (int i = 0; i < this.data.length; i++) {
      arr[i] = this.data[i];
    }
    ta.data = arr;
    return ta;
  }
  
  @Override
  public Boolean isEqual(Value v) {
    if (!(v instanceof TArray)) return false;
    TArray peer = (TArray) v;
    if (peer.data.length != this.data.length) return false;
    for (int i = 0; i < this.data.length; i++) {
      if (this.data[i] != peer.data[i]) return false;
    }
    return true;
  }
}