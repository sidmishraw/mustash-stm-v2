/**
 * Project: STMv2
 * Package: simple
 * File: SimpleDriver2.java
 * 
 * @author sidmishraw
 *         Last modified: Dec 30, 2017 6:18:02 PM
 */
package simple;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import stm.STM;
import stm.TVar;
import stm.Transaction;
import stm.utils.Transactions;

/**
 * @author sidmishraw
 *
 *         Qualified Name: simple.SimpleDriver2
 *
 */
public class SimpleDriver2 {
    
    private static final Logger logger = LoggerFactory.getLogger(SimpleDriver2.class);
    private static final STM    stm    = new STM();
    
    /**
     * Deposits the amount into the account.
     * 
     * @param t
     *            The transaction.
     * @param account
     *            The account to deposit into.
     * @param amount
     *            The amount to deposit.
     * @return the status of the deposit operation
     */
    private static final boolean deposit(Transaction t, TVar<Integer> account, int amount) {
        int bal = t.read(account);
        return t.write(account, bal + amount);
    }
    
    /**
     * Withdraws the amount from the account.
     * 
     * @param t
     *            The transaction.
     * @param account
     *            The account to withdraw from.
     * @param amount
     *            The amount to withdraw.
     * @return the status of the operation.
     */
    private static final boolean withdraw(Transaction t, TVar<Integer> account, int amount) {
        int bal = t.read(account);
        return t.write(account, bal - amount);
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        TVar<Integer> account1 = stm.newTVar(1000);
        TVar<Integer> account2 = stm.newTVar(500);
        TVar<Integer> account3 = stm.newTVar(2000);
        // operation
        stm.exec(Transactions.newT(stm).begin().orElse(t -> {
            int acc1Bal = t.read(account1);
            if (acc1Bal > 1000) {
                return withdraw(t, account1, 100) && deposit(t, account2, 100);
            }
            return false;
        }, t -> withdraw(t, account3, 500) && deposit(t, account2, 500)).end().done());
        // logging
        stm.exec(Transactions.newT(stm).begin(t -> {
            logger.info("Acc1 = " + t.read(account1));
            logger.info("Acc2 = " + t.read(account2));
            logger.info("Acc3 = " + t.read(account3));
            return true;
        }).end().done());
    }
    
}
