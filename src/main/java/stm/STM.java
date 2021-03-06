/**
 * Project: STMv2 Package: stm File: STM.java
 * 
 * @author sidmishraw
 *         Last modified: Dec 17, 2017 2:56:31 PM
 */
package stm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.GsonBuilder;

/**
 * The shared memory object that behaves like a memory whose addresses/cells can
 * be operated by transactions.
 * 
 * @author sidmishraw
 *         Qualified Name: stm.STM
 */
public class STM {
  
  private static final Logger logger = LoggerFactory.getLogger(STM.class);
  
  /**
   * The memory cells or the Memory vector.
   */
  private List<MemoryCell> memory;
  
  /**
   * Lock that is used for synchronizing commit phases of transactions. This lock
   * ensures the transactions are SERIALIZED because the second requirement of the STM is
   * serializability.
   * 
   * This lock must be taken before the ownership acquiring phase of the commit
   * phase. The lock is released after commit phase.
   */
  private ReentrantLock commitLock;
  
  /**
   * Acquires the commit lock on the STM.
   */
  void acquireCommitLock() {
    this.commitLock.lock();
  }
  
  /**
   * Releases the commit lock on the STM.
   */
  void releaseCommitLock() {
    this.commitLock.unlock();
  }
  
  /**
   * Makes a new STM
   */
  public STM() {
    this.memory = new ArrayList<>();
    this.commitLock = new ReentrantLock();
  }
  
  /**
   * Makes a new transactional variable holding the provided data. Internally it
   * is a memory cell containing the data.
   * 
   * @param data
   *          The data to be put into the transactional variable or memory cell.
   * @return The transactional variable or memory cell holding the data
   */
  public TVar newTVar(Value data) {
    MemoryCell memCell = new MemoryCell(data);
    this.memory.add(memCell);
    return memCell;
  }
  
  /**
   * Removes the transactional variable from the memory. The transactions trying
   * to access this deleted transactional variable need to take special care. They should abort the
   * moment they encounter this variable.
   * 
   * Basically, these transactions are invalidated since they are trying to
   * operate on memory that doesn't exist anymore.
   * 
   * @param tVar
   *          The transactional variable to get rid off.
   * 
   * @return The status of the removal operation.
   */
  public Boolean deleteTVar(TVar tVar) {
    MemoryCell memCell = (MemoryCell) tVar; // get the concrete memory cell
    return this.memory.remove(memCell);
  }
  
  /**
   * Checks if the transactional variable is still valid -- it exists in the
   * memory and hasn't been deleted.
   * 
   * @param tVar
   *          The transactional variable in question.
   * 
   * @return true if it exists in the memory, else false.
   */
  Boolean exists(TVar tVar) {
    return this.memory.contains(tVar);
  }
  
  /**
   * The STM spins up a transaction to perform the actions.
   * 
   * @param actions
   *          The actions to perform transactionally.
   */
  @SuppressWarnings("unchecked")
  public void perform(Function<Transaction, Boolean>... actions) {
    List<Function<Transaction, Boolean>> transactionalActions = Arrays.asList(actions);
    Transaction t = Transaction.builder().stm(this).actions(transactionalActions).build();
    t.execute();
  }
  
  /**
   * Prints the state of all the memory cells of the STM. To be used for debugging
   * only.
   */
  public void printState() {
    
    String stateString = new GsonBuilder().setExclusionStrategies(new ExclusionStrategy() {
      
      @Override
      public boolean shouldSkipField(FieldAttributes f) {
        if (f.getName().equals("commitLock")) return true;
        if (f.getName().equals("memCellLock")) return true;
        return false;
      }
      
      @Override
      public boolean shouldSkipClass(Class<?> clazz) {
        return false;
      }
    }).setPrettyPrinting().create().toJson(this);
    
    logger.debug("HARMLESS :: DEBUGGING STATE :: " + stateString);
    
  }
}
