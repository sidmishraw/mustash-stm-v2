/**
 * Project: STMv2 Package: stm File: Transaction.java
 * 
 * @author sidmishraw Last modified: Dec 17, 2017 2:59:37 PM
 */
package stm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

/**
 * The transaction is the only way to modify the state being managed by the STM.
 * 
 * @author sidmishraw
 *         Qualified Name: stm.Transaction
 */
public final class Transaction implements Callable<Optional<Quarantine>> {
  
  private static final Logger logger = LoggerFactory.getLogger(Transaction.class);
  
  /**
   * Denotes the version number of the transaction. It is the number of times the
   * transaction has completed execution successfully. It is updated after every successful run.
   */
  private @Getter(value = AccessLevel.PACKAGE) int version;
  
  /**
   * A boolean flag that indicates if the transaction has completed execution
   * successfully.
   */
  private @Getter(value = AccessLevel.PACKAGE) boolean isComplete;
  
  /**
   * Flag that protects against accessing deleted memory cells. If this is true,
   * the entire transaction is invalidated.
   */
  private boolean shouldAbort;
  
  /**
   * The transactional quarantine indicates the thread local storage. It is a
   * map/table that holds the memory cell and its thread local value. This is needed for maintaing
   * ISOLATION.
   * 
   * If the transaction commits successfully, the write quarantined values become
   * visible to all other transactions/threads at once.
   * 
   * This is important for maintaining ISOLATION and ATOMICITY. In earlier
   * versions, I used to call this map as `oldValues`. But, the name doesn't suit its purpose.
   */
  
  /**
   * The set of memory cells that the transaction intends to read from. The
   * transaction will only read from the memory cell once. Then, it will add the contents of the
   * memory cell into the readQuarantine.
   * 
   * All subsequent reads will take place from the readQuarantine.
   */
  private Map<MemoryCell, Value> readQuarantine;
  
  /**
   * The set of memory cells that the transaction intends to write to. The
   * transaction writes the new data into the write quarantine. The actual memory cell is updated
   * only after all the actions of the transaction have been thoroughly validated during the commit
   * phase.
   */
  private Map<MemoryCell, Value> writeQuarantine;
  
  /**
   * The reference to the STM object the transaction operates upon.
   */
  private STM stm;
  
  /**
   * The list of all the actions to be performed by the transaction in sequence.
   */
  private List<Function<Transaction, Boolean>> actions;
  
  /**
   * Creates a new transaction for the given STM, list of transactional actions (disjoint), and a
   * callback function.
   * 
   * @param stm
   *          The STM object that the transaction operates upon.
   * @param actions
   *          The disjoint transactional actions.
   */
  @Builder
  Transaction(STM stm, @Singular List<Function<Transaction, Boolean>> actions) {
    this.version = 0;
    this.isComplete = false;
    this.readQuarantine = new HashMap<>();
    this.writeQuarantine = new HashMap<>();
    this.stm = stm;
    this.actions = actions;
    this.shouldAbort = false;
  }
  
  /*
   * (non-Javadoc)
   * @see java.util.concurrent.Callable#call()
   */
  @Override
  public Optional<Quarantine> call() throws Exception {
    
    logger.debug("Transaction: " + Thread.currentThread().getName() + " has started execution.");
    
    this.isComplete = false; // the transaction has begun execution
    
    while (!this.isComplete && !this.shouldAbort) {
      
      // 1. execute actions
      //
      if (!this.executeActions()) {
        // execution of actions failed, the transaction needs to rollback and start from
        // the beginning
        //
        logger.info(Thread.currentThread().getName() + " failed to execute, hence rolling back");
        this.rollback();
        continue;
      }
      
      // 2. validate quarantined values and commit
      //
      if (!this.commit()) {
        // if commit failed, rollback and begin execution from the beginning
        //
        logger.info(Thread.currentThread().getName() + " failed to commit, hence rolling back");
        this.rollback();
        continue;
      }
      
      this.isComplete = true; // the transaction is done executing
    }
    
    // this.stm.printState(); // for debugging
    
    if (this.shouldAbort) {
      logger.info(Thread.currentThread().getName() + " has been invalidated!");
      this.isComplete = true;
    } else {
      logger.debug("Transaction: " + Thread.currentThread().getName() + " has finished execution.");
      this.version = this.version + 1;
    }
    
    // if the Transaction has been invalidated, there is no valid quarantine
    //
    if (this.shouldAbort) return Optional.empty();
    
    return Optional.ofNullable(new Quarantine(this.readQuarantine, this.writeQuarantine));
  }
  
  /**
   * Submits this transaction to the STM for execution.
   * 
   * @return The Future (promise) for the pending task.
   */
  Future<Optional<Quarantine>> execute() {
    return this.stm.submit(this);
  }
  
  /**
   * Reads the contents of the transactional variable or memory cell. It returns a
   * deep clone or copy of the original contents so that there is no accidental modification by
   * the consumer before the commit phase.
   * 
   * Note: The clone or copy is a deep copy of the contents.
   * 
   * @param tVar
   *          The transactional variable or memory cell to read contents from.
   * @param classz
   *          The concrete type for the Value.
   * 
   * @return The concrete value.
   */
  public <T> T read(TVar tVar, Class<T> classz) {
    
    try {
      
      // The null check!
      //
      if (Objects.isNull(tVar)) return null;
      
      Value data = null;
      
      // Inspired by S.P Jones' log based approach to the STM's actions,
      // the read action will read the value from the memory cell for the first time
      // and then it will `quarantine` that value -- store it in the quarantine map --
      // and then, the subsequent reads for the transaction will all come from the
      // quarantined memory cell.
      //
      if (Objects.isNull(this.readQuarantine.get((MemoryCell) tVar))) {
        data = ((MemoryCell) tVar).read();
        this.readQuarantine.put((MemoryCell) tVar, data);
      } else {
        data = this.readQuarantine.get((MemoryCell) tVar);
      }
      
      return classz.cast(data.makeCopy());
      
    } catch (Exception e) {
      
      logger.error(e.getMessage(), e);
      
      return null;
      
    }
  }
  
  /**
   * Writes the data to the quarantined memory cell. Basically, the transaction
   * holds onto its copy of updated data in its write quarantine. During the commit phase, it will
   * validate its read quarantine members and upon successful validation, it will flush the contents
   * of its write quarantined members into the STM.
   * 
   * This makes the changes of this transaction visible to its peers at once -- atomic.
   * 
   * @param tVar
   *          The transactional variable or memory cell to write into.
   * @param newData
   *          The new data to be written
   * 
   * @return The status of the write operation, true means success, false means
   *         failure.
   */
  public boolean write(TVar tVar, Value newData) {
    
    try {
      
      // Inspired by S.P. Jones' log based approach, the newData is written to the
      // quarantine.
      //
      // The final quarantined value is written into the tVar during the commit phase
      // of the transaction. The transaction writes to the memory cell only after a thorough
      // validation.
      //
      this.writeQuarantine.put((MemoryCell) tVar, newData);
      
      return true;
      
    } catch (Exception e) {
      
      logger.error(e.getMessage(), e);
      
      return false;
      
    }
  }
  
  /**
   * Executes all the actions of the transaction in-order. Returns true if all
   * actions executed successfully otherwise, returns false.
   * 
   * @return The result of the execution.
   */
  private boolean executeActions() {
    
    // logger.info("Executing Actions");
    
    // generate the stats for all the actions of this transaction.
    //
    List<Boolean> stats = this.actions.stream().map(action -> action.apply(this)).collect(Collectors.toList());
    
    // filter out failed actions
    //
    List<Boolean> res = stats.stream().filter(stat -> !stat).collect(Collectors.toList());
    
    if (res.size() > 0) {
      return false; // atleast one operation has failed
    }
    
    return true;
  }
  
  /**
   * Re-initializes the read quarantine and the write quarantine so that the
   * transaction can retry from the beginning.
   */
  private void rollback() {
    this.readQuarantine = new HashMap<>();
    this.writeQuarantine = new HashMap<>();
    this.isComplete = false;
  }
  
  /**
   * The commit phase of the transaction. In this phase, the transaction validates
   * its quarantined values and posts the values of its write set members. The values of the read
   * set members are validated since they might have been changed by other transactions.
   * 
   * @return status of the commit, false signals a failed commit, true is a
   *         successful commit.
   */
  private boolean commit() {
    
    try {
      
      logger.info(Thread.currentThread().getName() + " begins its commit phase");
      
      this.stm.acquireCommitLock(); // for serializability
      
      logger.info(Thread.currentThread().getName()
          + " begins validating its read quarantined values in the commit phase");
      
      Boolean validationStatus = this.validateReadQuarantined();
      
      if (!validationStatus) return false;
      
      return this.flushWriteQuarantined();
    } finally {
      
      this.stm.releaseCommitLock();
      
      logger.info(Thread.currentThread().getName() + " ends its commit phase");
    }
  }
  
  /**
   * Validates the read quarantined members. The transaction fetches the current
   * values of the memory cells.
   * 
   * Then, it compares the current values with its quarantined ones. If they match,
   * the memory cell is validated.
   * 
   * Otherwise, the memory cell is not valid and the validation fails.
   * 
   * Incase the transaction doesn't find the memory cell, it will abort since the
   * logic of its actions are no longer valid.
   * 
   * @return true if validated, else false.
   */
  private Boolean validateReadQuarantined() {
    
    for (Map.Entry<MemoryCell, Value> member : this.readQuarantine.entrySet()) {
      
      if (!this.stm.exists(member.getKey())) {
        // for protection against deletion
        this.shouldAbort = true;
        return false;
      }
      
      Value currentValue = member.getKey().read();
      Value quarantinedValue = member.getValue();
      
      if (!quarantinedValue.isEqual(currentValue)) return false;
      
    }
    
    return true;
    
  }
  
  /**
   * Flushes the write quarantined values into the STM.
   * 
   * @return true if successfully flushed the values into the STM, else false.
   */
  private Boolean flushWriteQuarantined() {
    
    for (Map.Entry<MemoryCell, Value> member : this.writeQuarantine.entrySet()) {
      
      if (!this.stm.exists(member.getKey())) {
        // for protection against deletion
        this.shouldAbort = true;
        return false;
      }
      
      member.getKey().write(member.getValue());
      
    }
    
    return true;
  }
}
