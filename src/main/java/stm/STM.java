/**
 * Project: STMv2
 * Package: stm
 * File: STM.java
 * 
 * @author sidmishraw
 *         Last modified: Dec 17, 2017 2:56:31 PM
 */
package stm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;
import stm.annotations.ReadLocking;
import stm.annotations.WriteLocking;

/**
 * The shared memory object that behaves like a memory whose addresses/cells can be operated by
 * transactions.
 * 
 * @author sidmishraw
 *
 *         Qualified Name: stm.STM
 *
 */
public class STM {
    
    // region [For logging]
    private static final Logger             logger = LoggerFactory.getLogger(STM.class);
    // region [For logging]
    
    /**
     * Lock used for synchronized reads and writes to the ownerships and memory objects
     */
    private ReentrantReadWriteLock          stmLock;
    
    /**
     * Lock that is used for synchronizing commit phases of transactions. This lock ensures ISOLATION and ATOMICITY are
     * maintained. This lock must be taken before the ownership acquiring phase of the commit phase. The lock is
     * released after commit phase.
     */
    private @Getter ReentrantLock           commitLock;
    
    /**
     * The memory cells or the Memory vector
     */
    private List<MemoryCell<?>>             memory;
    
    /**
     * The Ownerships vector, maps from the memory cell to the owning transaction.
     * 
     * Note: Do I even need it anymore? S.P. Jone's approach seems more natural.
     */
    private Map<MemoryCell<?>, Transaction> ownerships;
    
    /**
     * Makes a new STM
     */
    public STM() {
        this.memory = new ArrayList<>();
        this.ownerships = new HashMap<>();
        this.stmLock = new ReentrantReadWriteLock();
        this.commitLock = new ReentrantLock();
    }
    
    // # Memcell related
    
    /**
     * Makes a new memory cell containing the data
     * 
     * @param data
     *            the data to be put into the memory cell
     * @return the memory cell holding the data
     */
    public <T> MemoryCell<T> newMemCell(T data) {
        MemoryCell<T> memCell = new MemoryCell<>(data);
        this.memory.add(memCell);
        return memCell;
    }
    
    // # Memcell related
    
    // # Ownership related
    
    /**
     * 
     * Gets the owner of the memcell, could be null if no owner present
     * 
     * @param memCell
     *            the memory cell
     * @return the owner transaction, might be null if the memory cell is not owned by any transactions
     */
    @ReadLocking
    public Optional<Transaction> getOwner(MemoryCell<?> memCell) {
        Optional<Transaction> ot = Optional.ofNullable(null);
        if (Objects.isNull(memCell)) {
            return ot;
        }
        try {
            this.stmLock.readLock().lock();
            ot = Optional.ofNullable(this.ownerships.get(memCell));
            return ot;
        } finally {
            this.stmLock.readLock().unlock();
        }
    }
    
    /**
     * Transaction t takes the ownership of the memory cell.
     * 
     * @param memCell
     *            the memory cell to be owned
     * @param t
     *            the transaction to take ownership
     */
    @WriteLocking
    public void takeOwnership(MemoryCell<?> memCell, Transaction t) {
        if (Objects.isNull(memCell) || Objects.isNull(t)) {
            return;
        }
        logger.debug("Transaction: " + t.toString() + " is trying to take ownership of memcell: " + memCell.toString());
        try {
            this.stmLock.writeLock().lock();
            this.ownerships.put(memCell, t);
        } finally {
            this.stmLock.writeLock().unlock();
        }
        logger.debug("Transaction: " + t.toString() + " has taken ownership of memcell: " + memCell.toString());
    }
    
    /**
     * Checks if the transaction t is the owner of the memCell
     * 
     * @param memCell
     *            the memory cell
     * @param t
     *            the transaction t
     * @return the result of the test
     */
    @ReadLocking
    public boolean isOwner(MemoryCell<?> memCell, Transaction t) {
        try {
            this.stmLock.readLock().lock();
            Optional<Transaction> ot = Optional.ofNullable(this.ownerships.get(memCell));
            if (!ot.isPresent() || !ot.get().equals(t)) {
                // the memCell is not owned by any transaction or
                // it is owned by some other transaction
                return false;
            }
            // transaction is owned by the transaction
            return true;
        } finally {
            this.stmLock.readLock().unlock();
        }
    }
    
    /**
     * The transaction releases the ownership of the memory cell
     * 
     * @param memCell
     *            The concerned memory cell
     */
    @WriteLocking
    public void releaseOwnership(MemoryCell<?> memCell) {
        if (Objects.isNull(memCell)) {
            logger.debug("No memcell found!");
            return;
        }
        logger.debug("Freeing memcell : " + memCell.toString());
        try {
            this.stmLock.writeLock().lock();
            this.ownerships.remove(memCell);
        } finally {
            this.stmLock.writeLock().unlock();
        }
        logger.debug("Freed memcell : " + memCell.toString());
    }
    
    // # Ownership related
    
    // # transaction execution strategies
    
    /**
     * Executes all the transactions concurrently and makes <strong>the main-thread or parent thread wait till all of
     * the transactions are done executing.</strong>
     * 
     * <p>
     * 
     * <blockquote>
     * Note: Having this as a member of the STM object reduces the risk for executing transactions in some other STM
     * object's context.
     * </blockquote>
     * 
     * @param ts
     *            the transactions to execute concurrently
     */
    public void exec(Transaction... ts) {
        CountDownLatch latch = new CountDownLatch(ts.length);
        Arrays.asList(ts).forEach(t -> {
            t.go(latch); // execute the transaction with the latch
        });
        try {
            // wait till all the transactions are done executing
            latch.await();
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }
    
    /**
     * Executes the transactions concurrently <strong> without making their parent (main-thread) wait till all of them
     * are done executing.</strong>
     * 
     * <p>
     * See also {@linkplain STM#exec(Transaction...)}
     * 
     * @param ts
     *            The transactions to execute concurrently
     */
    public void forkAndExec(Transaction... ts) {
        new Thread(() -> {
            logger.debug("Forking and starting all the transactions...");
            CountDownLatch latch = new CountDownLatch(ts.length);
            Arrays.asList(ts).forEach(t -> {
                t.go(latch); // execute the transaction with the latch
            });
            try {
                // wait till all the transactions are done executing
                latch.await();
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }).start();
    }
    
    // # transaction execution strategies
    
    /**
     * Prints the state of all the memory cells of the STM.
     * To be used for debugging only.
     * 
     * @InternalUsage
     */
    void printState() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("HARMLESS :: LOGGING STATE :: ");
        this.memory.forEach(m -> {
            buffer.append(m.toString()).append("  ");
        });
        logger.debug(buffer.toString());
    }
}
