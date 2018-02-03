/**
 * Project: STMv2
 * Package: stm
 * File: Transaction.java
 * 
 * @author sidmishraw
 *         Last modified: Dec 17, 2017 2:59:37 PM
 */
package stm;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rits.cloning.Cloner;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import monad.STMAction;

/**
 * @author sidmishraw
 *
 *         Qualified Name: stm.Transaction
 *
 */
public final class Transaction implements Runnable {
    
    // # For logging
    private static final Logger logger = LoggerFactory.getLogger(Transaction.class);
    // # For logging
    
    /**
     * The latch used for holding the invoking thread (main-thread) till this transaction is done computing.
     */
    private CountDownLatch latch;
    
    // ==================================================================================================================================
    // # Metadata
    
    /**
     * Denotes the version number of the transaction. It is the number of times the transaction has completed execution
     * successfully. It is updated after every successful run.
     */
    @Getter
    private int version;
    
    /**
     * A boolean flag that indicates if the transaction has completed execution successfully.
     */
    @Getter
    private boolean isComplete;
    
    // ================================================================================================================
    // # thread local quarantines =====================================================================================
    
    /**
     * The {@link Transaction}{@link #quarantine} indicates the thread local storage. This map holds the values of the
     * memory cells before the transaction commits. If the transaction commits successfully, the quarantined values
     * become visible to all other transactions/threads at once. This is important for maintaining ISOLATION and
     * ATOMICITY.
     * 
     * <p>
     * In earlier versions, I used to call this map as `oldValues`. But, the name doesn't suit its purpose.
     */
    
    /**
     * The set of memory cells that the transaction intends to read from. The transaction will only read from the memory
     * cell once. Then, it will add the contents of the memory cell into the readQuarantine. All subsequent reads will
     * take place from the readQuarantine.
     */
    private Map<MemoryCell<Object>, Object> readQuarantine;
    
    /**
     * The set of memory cells that the transaction intends to write to. The transaction writes the new data into the
     * write quarantine. The memory cell is updated only after all the actions of the transaction have been thoroughly
     * validated.
     */
    private Map<MemoryCell<Object>, Object> writeQuarantine;
    
    // # thread local quarantines =====================================================================================
    // ================================================================================================================
    
    // # Metadata
    // ==================================================================================================================================
    
    /**
     * The reference to the STM object the transaction operates upon.
     */
    private STM stm;
    
    // ==================================================================================================================================
    
    /**
     * The STM action that the transaction performs.
     */
    @Setter
    private Function<Transaction, STMAction<Boolean>> action;
    
    // ==================================================================================================================================
    
    /**
     * Creates a new transaction.
     * 
     * @param stm
     *            The STM that the transaction intends to modify.
     * @param action
     *            The STM action that needs to be performed by the transaction.
     */
    @Builder
    private Transaction(STM stm, Function<Transaction, STMAction<Boolean>> action) {
        //
        //
        //
        this.stm = stm;
        //
        //
        this.action = action;
        //
        //
        //
        this.writeQuarantine = new HashMap<>();
        this.readQuarantine = new HashMap<>();
        //
        //
        this.isComplete = false;
        this.version = 0;
    }
    
    // ==================================================================================================================================
    
    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        logger.debug("Transaction: " + Thread.currentThread().getName() + " has started execution.");
        //
        //
        this.isComplete = false; // the transaction has begun execution
        //
        //
        while (true) {
            //
            // 1. execute actions
            //
            if ((Objects.isNull(this.action)) || (!Objects.isNull(this.action) && !this.action.apply(this).unwrap())) {
                // execution of actions failed, the transaction needs to rollback and start from the beginning
                logger.info(Thread.currentThread().getName() + " failed to execute, hence rolling back");
                this.rollback();
                continue;
            }
            //
            // 2. validate quarantined values and commit
            //
            if (!this.commit()) {
                // if commit failed, rollback and begin execution from the beginning
                logger.info(Thread.currentThread().getName() + " failed to commit, hence rolling back");
                this.rollback();
                continue;
            }
            //
            //
            //
            break;
        }
        // # debugging -- checking state of memory cells
        // this.stm.printState();
        // # debugging -- checking state of memory cells
        logger.debug("Transaction: " + Thread.currentThread().getName() + " has finished execution.");
        //
        //
        //
        this.version = this.version + 1; // increment the version
        this.isComplete = true; // the transaction is done executing
        this.latch.countDown(); // signal the calling thread that this is done
    }
    
    /**
     * Runs transaction with a latch so that the parent thread will wait for the transaction to end execution.
     * 
     * @param latch
     *            the latch on which the parent thread will await
     */
    public void go(CountDownLatch latch) {
        this.latch = latch;
        Thread currentThread = new Thread(this);
        currentThread.start();
    }
    
    // ==================================================================================================================
    // # Transactional Operation related
    
    //
    // READ
    //
    
    /**
     * Reads the contents of the transactional variable or memory cell. It returns a deep clone or copy of the
     * original contents so that there is no accidental modification by the consumer before the commit phase.
     * 
     * <p>
     * <blockquote> <strong> Note: The clone or copy is a deep copy of the contents. </strong> </blockquote>
     * 
     * @param tVar
     *            the transactional variable or memory cell to read contents from
     * 
     * @return A STM action that when performed returns the contents of the transactional variable or memory cell
     */
    @SuppressWarnings("unchecked")
    public <T> STMAction<T> read(TVar<T> tVar) {
        return new STMAction<>(() -> {
            try {
                //
                //////////////////////////////
                // tVar null check ///////////
                //////////////////////////////
                if (Objects.isNull(tVar)) {
                    return null;
                }
                /////////////////////////////
                //
                //
                T data = null;
                //
                //
                // Inspired by S.P Jones' log based approach to the STM's actions,
                // the read action will read the value from the memory cell for the first time
                // and then it will `quarantine` that value -- store it in the quarantine map --
                // and subsequent reads for the transaction will all come from the quarantined
                // memory cell.
                //
                //
                if (Objects.isNull(this.readQuarantine.get((MemoryCell<T>) tVar))) {
                    //
                    // read directly from the memory cell and store in the read-quarantine
                    //
                    data = ((MemoryCell<T>) tVar).read();
                    this.readQuarantine.put((MemoryCell<Object>) tVar, data);
                } else {
                    //
                    // read from the read-quarantine
                    //
                    data = (T) this.readQuarantine.get((MemoryCell<T>) tVar);
                }
                //
                //
                Cloner clone = new Cloner(); // Java deep cloner
                clone.registerImmutable(TVar.class, MemoryCell.class); // register instances TVars
                                                                       // as immutable when cloning
                data = clone.deepClone(data); // data is a deep-copy/clone of the contents of the tVar
                //
                //
                //
                return data;
            } catch (Exception e) {
                //
                //
                logger.error(e.getMessage(), e);
                //
                return null;
            }
        });
    }
    
    //
    // WRITE
    //
    
    /**
     * Writes the data to the memory cell.
     * 
     * @param tVar
     *            the transactional variable or memory cell to write into.
     * @param newData
     *            the new data to be written.
     * 
     * @return A STM action which when performed will return the status of the write operation, true means success,
     *         false means failure.
     */
    @SuppressWarnings("unchecked")
    public <T> STMAction<Boolean> write(TVar<T> tVar, T newData) {
        return new STMAction<>(() -> {
            try {
                //
                //
                // Inspired by S.P. Jones' log based approach, the newData is written to the quarantine.
                // The final quarantined value is written into the tVar during the commit phase of the
                // transaction. The transaction writes to the memory cell only after a thorough validation.
                //
                //
                this.writeQuarantine.put((MemoryCell<Object>) tVar, newData);
                //
                //
                return true;
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                //
                //
                //
                return false;
            }
        });
    }
    
    // # Transactional Operation related
    // ==================================================================================================================
    
    /**
     * Executes all the actions of the transaction in-order. Returns true if all actions executed successfully
     * otherwise, returns false.
     * 
     * @return the result of the execution.
     */
    
    /**
     * Re-initializes the readQuarantine and the writeQuarantine so that the transaction can retry from the beginning.
     */
    private void rollback() {
        this.readQuarantine = new HashMap<>();
        this.writeQuarantine = new HashMap<>();
    }
    
    /**
     * The commit phase of the transaction. In this phase, the transaction validates its quarantined values and posts
     * the values of its write set members. The values of the read set members are validated since they might have been
     * changed by other transactions.
     * 
     * @return status of the commit, false signals a failed commit, true is a successful commit.
     */
    private boolean commit() {
        try {
            logger.info(Thread.currentThread().getName() + " begins its commit phase");
            //
            // Setup step: Acquire the commit lock on the STM. This lock helps to ensure ATOMICITY and ISOLATION.
            //
            this.stm.getCommitLock().lock();
            //
            // logger.error("HARMLESS: Lock acquired!");
            //
            // -- First, the transaction needs to validate its read quarantined values.
            //
            logger.info(Thread.currentThread().getName()
                    + " begins validating its read quarantined values in the commit phase");
            //
            //
            //
            if (this.readQuarantine.entrySet().stream().map(e -> {
                MemoryCell<Object> memCell = e.getKey();
                Object quarantinedVal = e.getValue();
                Object currentVal = memCell.read(); // current actual contents
                if (!currentVal.equals(quarantinedVal)) {
                    // memCell's value has been changed by some other transaction
                    return false;
                }
                return true;
            }).filter(s -> !s).count() > 0) {
                //
                //
                // Atleast one read set member has been modifed by another transaction, the data read is stale.
                // Need to release all the ownerships and fail the commit phase so that the transaction can
                // restart from the beginning.
                logger.info("FAILED:: " + Thread.currentThread().getName() + " read inconsistent");
                return false; // failed commit
            }
            //
            //
            // -- Second, after the read set members have been validated, the transaction is ready to flush
            // its write-quarantined values. Then, it must release ownerships of all the write set members.
            this.writeQuarantine.entrySet().forEach(e -> {
                MemoryCell<Object> memCell = e.getKey();
                Object val = e.getValue();
                //
                //
                // logger.info("WRITING -- " + new Gson().toJson(val)); // debugging value being written
                //
                //
                memCell.write(val);
            });
            //
            //
            logger.info(Thread.currentThread().getName() + " ends its commit phase");
            return true; // commit was successful
        } finally {
            //
            //
            // Release the commit lock on the STM
            this.stm.getCommitLock().unlock();
        }
    }
}
