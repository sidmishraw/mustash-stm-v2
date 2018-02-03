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

import monad.STMAction;
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
    
    //
    // STM
    //
    private static final STM stm = new STM();
    
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
    private static final STMAction<Boolean> deposit(Transaction t, TVar<Integer> account, int amount) {
        return t.read(account).bind(bal -> t.write(account, bal + amount));
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
    private static final STMAction<Boolean> withdraw(Transaction t, TVar<Integer> account, int amount) {
        return t.read(account).bind(bal -> t.write(account, bal - amount));
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        //
        //
        //
        stm.newTVar(1000).bind(account1 ->
        //
        //
        stm.newTVar(500).bind(account2 ->
        //
        //
        stm.newTVar(2000).bind(account3 ->
        
        {
            //
            // operation
            //
            stm.exec(Transaction.builder().stm(stm).action(t -> t.read(account1).bind(acc1Bal -> {
                if (acc1Bal > 1000) {
                    return withdraw(t, account1, 100).bind(b1 -> deposit(t, account2, 100).bind(b2 -> new STMAction<
                            Boolean>(() -> b1.booleanValue() && b2.booleanValue())));
                } else {
                    return withdraw(t, account1, 500).bind(b1 -> deposit(t, account2, 500).bind(b2 -> new STMAction<
                            Boolean>(() -> b1.booleanValue() && b2.booleanValue())));
                }
            })).build());
            
            //
            // logging
            //
            stm.exec(Transaction.builder().stm(stm).action(t -> t.read(account1).bind(acc1 -> {
                logger.info("Acc1 = " + acc1);
                return t.read(account2);
            }).bind(acc2 -> {
                logger.info("Acc2 = " + acc2);
                return t.read(account3);
            }).bind(acc3 -> {
                logger.info("Acc3 = " + acc3);
                return new STMAction<>(() -> true);
            })).build());
            
            //
            //
            //
            return new STMAction<Void>(() -> null);
        })));
        
    }
    
}
