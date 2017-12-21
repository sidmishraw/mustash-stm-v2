/**
 * Project: STMv2
 * Package: stm
 * File: Transaction.java
 * 
 * @author sidmishraw
 *         Last modified: Dec 17, 2017 2:59:37 PM
 */
package stm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rits.cloning.Cloner;

import lombok.Getter;
import lombok.Setter;

/**
 * @author sidmishraw
 *
 *         Qualified Name: stm.Transaction
 *
 */
public final class Transaction implements Runnable {
    
    // # For logging
    private static final Logger             logger = LoggerFactory.getLogger(Transaction.class);
    // # For logging
    
    /**
     * The latch used for holding the invoking thread (main-thread) till this transaction is done computing.
     */
    private CountDownLatch                  latch;
    
    // # Metadata
    
    /**
     * Denotes the version number of the transaction. It is the number of times the transaction has completed execution
     * successfully. It is updated after every successful run.
     */
    private @Getter int                     version;
    
    /**
     * A boolean flag that indicates if the transaction has completed execution successfully.
     */
    private @Getter boolean                 isComplete;
    
    // # thread local quarantines
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
    
    // # thread local quarantines
    
    // # Metadata
    
    /**
     * The reference to the STM object the transaction operates upon.
     */
    private STM                             stm;
    
    /**
     * The list of all the actions to be performed by the transaction in sequence
     */
    private @Setter List<Supplier<Boolean>> actions;
    
    /**
     * Creates a new transaction for the given STM.
     * 
     * @param stm
     *            the STM object that the transaction operates on.
     */
    public Transaction(STM stm) {
        this.version = 0;
        this.isComplete = false;
        this.readQuarantine = new HashMap<>();
        this.writeQuarantine = new HashMap<>();
        this.stm = stm;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        logger.debug("Transaction: " + Thread.currentThread().getName() + " has started execution.");
        this.isComplete = false; // the transaction has begun execution
        while (true) {
            // 1. execute actions
            if (!this.executeActions()) {
                // execution of actions failed, the transaction needs to rollback and start from the beginning
                logger.info(Thread.currentThread().getName() + " failed to execute, hence rolling back");
                this.rollback();
                continue;
            }
            // 2. validate quarantined values and commit
            if (!this.commit()) {
                // if commit failed, rollback and begin execution from the beginning
                logger.info(Thread.currentThread().getName() + " failed to commit, hence rolling back");
                this.rollback();
                continue;
            }
            break;
        }
        // # debugging -- checking state of memory cells
        // this.stm.printState();
        // # debugging -- checking state of memory cells
        logger.debug("Transaction: " + Thread.currentThread().getName() + " has finished execution.");
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
    
    // # Transactional Operation related
    
    /**
     * Reads the contents of the memory cell. It returns a deep clone or copy of the
     * original contents so that there is no accidental modification by the consumer before the commit phase.
     * 
     * <p>
     * <blockquote> <strong> Note: The clone or copy is a deep copy of the contents. </strong> </blockquote>
     * 
     * @param memCell
     *            the memory cell to read contents from
     * @return the contents of the memory cell
     */
    @SuppressWarnings("unchecked")
    public <T> T read(MemoryCell<T> memCell) {
        T data = null;
        // Inspired by S.P Jones' log based approach to the STM's actions,
        // the read action will read the value from the memory cell for the first time
        // and then it will `quarantine` that value -- store it in the quarantine map --
        // and subsequent reads for the transaction will all come from the quarantined
        // memory cell.
        if (Objects.isNull(this.readQuarantine.get(memCell))) {
            // read directly from the memory cell and store in the read-quarantine
            data = memCell.read();
            this.readQuarantine.put((MemoryCell<Object>) memCell, data);
        } else {
            // read from the read-quarantine
            data = (T) this.readQuarantine.get(memCell);
        }
        Cloner clone = new Cloner(); // Java deep cloner
        data = clone.deepClone(data); // data is a deep-copy/clone of the contents of the memCell
        return data;
    }
    
    /**
     * Writes the data to the memory cell.
     * 
     * @param memCell
     *            the memory cell to write into.
     * @param newData
     *            the new data to be written
     * @return the status of the write operation, true means success, false means failure.
     */
    @SuppressWarnings("unchecked")
    public <T> boolean write(MemoryCell<T> memCell, T newData) {
        // Inspired by S.P. Jones' log based approach, the newData is written to the quarantine.
        // The final quarantined value is written into the memCell during the commit phase of the
        // transaction. The transaction writes to the memory cell only after a thorough validation.
        this.writeQuarantine.put((MemoryCell<Object>) memCell, newData);
        return true;
    }
    // # Transactional Operation related
    
    /**
     * Executes all the actions of the transaction in-order. Returns true if all actions executed successfully
     * otherwise, returns false.
     * 
     * @return the result of the execution.
     */
    private boolean executeActions() {
        // logger.info("Executing Actions");
        List<Boolean> stats = new ArrayList<>();
        this.actions.forEach(act -> {
            stats.add(act.get());
        });
        List<Boolean> res = stats.stream().filter(stat -> !stat).collect(Collectors.toList()); // filter out failed
                                                                                               // actions
        if (res.size() > 0) return false; // atleast one operation has failed
        return true;
    }
    
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
            // Setup step: Acquire the commit lock on the STM. This lock helps to ensure ATOMICITY and ISOLATION.
            this.stm.getCommitLock().lock();
            // logger.error("HARMLESS: Lock acquired!");
            // -- First, the transaction will take ownership of all its write quarantined members.
            // This prevents other transactions from viewing an inconsistent state.
            if (this.writeQuarantine.entrySet().stream().map((e) -> {
                MemoryCell<Object> memCell = e.getKey();
                Optional<Transaction> currentOwner = this.stm.getOwner(memCell);
                if (currentOwner.isPresent() && !currentOwner.get().equals(this)) {
                    // cannot take ownership since the memory cell is already owned by some other transaction
                    return false;
                }
                // the memCell is ready to be owned
                this.stm.takeOwnership(memCell, this);
                return true;
            }).filter(s -> !s).count() > 0) {
                // failed to take ownership of atleast 1 write set member
                logger.info("FAILED:: " + Thread.currentThread().getName() + " couldn't take ownerships");
                this.releaseOwnerships();
                return false;
            }
            // # for debugging only
            // logger.debug("Ownerships = " + this.writeQuarantine.entrySet().stream().map(e -> {
            // logger.error("Current owner = " + this.stm.getOwner(e.getKey()));
            // return this.stm.isOwner(e.getKey(), this);
            // }).filter(s -> s).count());
            // # for debugging only
            // -- Second, that the transaction has taken ownership of its write quarantined members
            // it needs to validate its read quarantined values.
            logger.info(Thread.currentThread().getName()
                    + " begins validating its read quarantined values in the commit phase");
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
                // Atleast one read set member has been modifed by another transaction, the data read is stale.
                // Need to release all the ownerships and fail the commit phase so that the transaction can
                // restart from the beginning.
                logger.info("FAILED:: " + Thread.currentThread().getName() + " read inconsistent");
                this.releaseOwnerships();
                return false;
            }
            // -- Third, after the read set members have been validated, the transaction is ready to flush
            // its write-quarantined values. Then, it must release ownerships of all the write set members.
            this.writeQuarantine.entrySet().forEach(e -> {
                MemoryCell<Object> memCell = e.getKey();
                Object val = e.getValue();
                // logger.info("WRITING -- " + new Gson().toJson(val)); // debugging value being written
                memCell.write(val);
            });
            logger.info(Thread.currentThread().getName() + " ends its commit phase");
            this.releaseOwnerships();
            return true; // commit was successful
        } finally {
            // Release the commit lock on the STM
            this.stm.getCommitLock().unlock();
        }
    }
    
    /**
     * Releases the ownerships of all the write set members.
     */
    private void releaseOwnerships() {
        this.writeQuarantine.entrySet().forEach(e -> {
            if (this.stm.isOwner(e.getKey(), this)) {
                // release ownership iff the transaction is the owner
                this.stm.releaseOwnership(e.getKey());
            }
        });
    }
}
