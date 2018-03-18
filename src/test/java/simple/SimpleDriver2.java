/**
 * Project: STMv2 Package: simple File: SimpleDriver2.java
 * 
 * @author sidmishraw Last modified: Dec 30, 2017 6:18:02 PM
 */
package simple;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stm.STM;
import stm.TVar;
import stm.Transaction;

/**
 * @author sidmishraw
 *
 *         Qualified Name: simple.SimpleDriver2
 *
 */
public class SimpleDriver2 {

  private static final Logger logger = LoggerFactory.getLogger(SimpleDriver2.class);

  /**
   * The STM
   */
  private static final STM stm = new STM();

  /**
   * Deposits the amount into the account.
   * 
   * @param t The transaction.
   * @param account The account to deposit into.
   * @param amount The amount to deposit.
   * @return the status of the deposit operation
   */
  private static final boolean deposit(Transaction t, TVar<Integer> account, int amount) {

    int bal = t.read(account);

    return t.write(account, bal + amount);
  }

  /**
   * Withdraws the amount from the account.
   * 
   * @param t The transaction.
   * @param account The account to withdraw from.
   * @param amount The amount to withdraw.
   * @return the status of the operation.
   */
  private static final boolean withdraw(Transaction t, TVar<Integer> account, int amount) {

    int bal = t.read(account);

    if (bal < amount) {

      // not enough funds to withdraw
      return false;
    }

    return t.write(account, bal - amount);
  }

  /**
   * @param args
   */
  public static void main(String[] args) {

    // storing account balances in the transactional variables
    // These are actually stored in the STM's memory cells.
    TVar<Integer> account1 = stm.newTVar(10);
    TVar<Integer> account2 = stm.newTVar(50);
    TVar<Integer> account3 = stm.newTVar(200);

    /**
     * The scenario demonstrated is as follows:
     * 
     * Transaction#1: In this transaction we check if `account1` has sufficient funds (> 100). If
     * so, we transfer 25 from `account1` to `account2`. Otherwise, we transfer 100 from `account3`
     * to `account1`.
     * 
     * Transaction#2: In this transaction we simply try to deposit 100 from `account1`.
     * 
     * Observation: We will get 2 states:
     * 
     * State 1: `account1` doesn't have sufficient funds and hence we transfer funds from
     * `account3`. For this scenario we have the following output:
     * 
     * {@code 
     * 
     * account1 = 210
     * 
     * account2 = 50
     * 
     * account3 = 100
     * 
     * }
     * 
     * State 2: The second transaction happens before the first transaction therefore, `account1`
     * has sufficient funds. As a result, 25 is transferred from `account1` to `account2`. For this
     * scenario we have the following output:
     * 
     * {@code 
     * 
     * account1 = 85 
     * 
     * account2 = 75 
     * 
     * account3 = 200 
     * 
     * }
     */
    stm.exec(Transaction.builder().stm(stm).action(t -> {

      boolean status = true;

      Integer acc1Balance = t.read(account1); // read the balance of account 1

      if (acc1Balance > 100) {

        // transfer 25 from account 1 to account 2
        status = status && withdraw(t, account1, 25);
        status = status && deposit(t, account2, 25);
      } else {

        // transfer 100 from account 3 to account 1
        status = status && withdraw(t, account3, 100);
        status = status && deposit(t, account1, 100);
      }

      return status;
    }).build(), Transaction.builder().stm(stm).action(t -> {
      return deposit(t, account1, 100);
    }).build());


    // logging
    stm.exec(Transaction.builder().stm(stm).action(t -> {

      logger.info("Acc1 = " + t.read(account1));
      logger.info("Acc2 = " + t.read(account2));
      logger.info("Acc3 = " + t.read(account3));

      return true;
    }).build());
  }

}
