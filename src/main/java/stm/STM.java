/**
 * Project: STMv2 Package: stm File: STM.java
 * 
 * @author sidmishraw Last modified: Dec 17, 2017 2:56:31 PM
 */
package stm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.AccessLevel;
import lombok.Getter;

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
  private static final Logger logger = LoggerFactory.getLogger(STM.class);
  // region [For logging]

  /**
   * Lock that is used for synchronizing commit phases of transactions. This lock ensures ISOLATION
   * and ATOMICITY are maintained. This lock must be taken before the ownership acquiring phase of
   * the commit phase. The lock is released after commit phase.
   * 
   * @InternalUsage
   */
  private @Getter(AccessLevel.PACKAGE) ReentrantLock commitLock;

  /**
   * The memory cells or the Memory vector
   */
  private List<MemoryCell<?>> memory;

  /**
   * Makes a new STM
   */
  public STM() {
    this.memory = new ArrayList<>();
    this.commitLock = new ReentrantLock();
  }

  // # Memcell related

  /**
   * Makes a new transactional variable holding the provided data. Internally it is a memory cell
   * containing the data.
   * 
   * @param data the data to be put into the transactional variable or memory cell
   * @return the transactional variable or memory cell holding the data
   */
  public <T> TVar<T> newTVar(T data) {
    MemoryCell<T> memCell = new MemoryCell<>(data);
    this.memory.add(memCell);
    return memCell;
  }

  // # Memcell related

  // # transaction execution strategies

  /**
   * Executes all the transactions concurrently and makes <strong>the main-thread or parent thread
   * wait till all of the transactions are done executing.</strong>
   * 
   * <p>
   * 
   * <blockquote> Note: Having this as a member of the STM object reduces the risk for executing
   * transactions in some other STM object's context. </blockquote>
   * 
   * @param ts the transactions to execute concurrently
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
   * Executes the transactions concurrently <strong> without making their parent (main-thread) wait
   * till all of them are done executing.</strong>
   * 
   * <p>
   * See also {@linkplain STM#exec(Transaction...)}
   * 
   * @param ts The transactions to execute concurrently
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
   * Prints the state of all the memory cells of the STM. To be used for debugging only.
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
