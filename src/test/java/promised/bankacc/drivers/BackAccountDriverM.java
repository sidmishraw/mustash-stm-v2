/**
 * 
 * BSD 3-Clause License
 * 
 * Copyright (c) 2018, Sidharth Mishra
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * 
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * * Neither the name of the copyright holder nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS AS IS
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *
 * BackAccountDriverM.java
 * 
 * @author Sidharth Mishra <sidmishraw@gmail.com>
 * @created Apr 10, 2018 10:05:55 PM
 */
package promised.bankacc.drivers;

import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import promised.bankacc.Account;
import stm.STM;

/**
 * In this simulation, I create a thread pool having 4 threads. Then, I have these threads perform
 * the various operations: deposit, withdraw, and transfer on the 2 bank accounts.
 * 
 * We'll see how the STM pattern fares in this scenario.
 * 
 * Note: Please ignore the logger, and executor service or thread pool implementations.
 * These are provided by Java library or third parties, and are beyond explanation in this
 * simulation.
 * 
 * Qualified Name: isolate.drivers.BackAccountDriverM
 *
 */
public class BackAccountDriverM {
  
  /**
   * LOGGER -- language specific -- I'm using SLF4J logging interface coupled with Logback backend.
   * It has no other purpose than providing logging support.
   */
  private static Logger logger = LoggerFactory.getLogger(BackAccountDriverM.class);
  
  /**
   * @param args
   */
  public static void main(String[] args) {
    
    /**
     * Instantiating the STM for this application. Ideally, having 1 STM throughout the lifetime of an
     * application should be ideal.
     */
    STM stm = new STM();
    
    // Create two accounts `account1` and `account2` with initial balances
    // `100` and `200` respectively.
    //
    Account acc1 = Account.builder().accountName("Account1").initialBalance(100).stm(stm).build();
    Account acc2 = Account.builder().accountName("Account2").initialBalance(200).stm(stm).build();
    
    // print the JSON structure for acc1 and acc2, cheap debugging.
    //
    logger.info("Account 1:\n" + acc1.toString());
    logger.info("Account 2:\n" + acc2.toString());
    stm.printState();
    
    // Create a threadPool having 4 threads to simulate multiple threads
    // requesting changes on the accounts.
    //
    ExecutorService threadPool = Executors.newFixedThreadPool(4);
    
    // ----------------------------------------------------------------------------------
    // perform the actions on the accounts
    // deposit 50 into acc1: acc1 = 150
    // deposit 300 into acc2: acc2 = 500
    // transfer 35 from acc1 to acc2: acc1 = 150 - 35 = 115, acc2 = 500 + 35 = 535
    // transfer 50 from acc2 to acc1: acc1 = 115 + 50 = 165, acc2 = 535 - 50 = 485
    //
    // Finally, acc1 = 165, acc2 = 485 (Test for consistency of the operations)
    //
    // Note: all these actions happen to be running on separate threads under the hood.
    //
    threadPool.submit(() -> {
      Optional<Integer> newBal = acc1.deposit(50);
      logger.info("Acc#1 :: New balance after deposit = " + newBal.orElse(-1).toString());
    });
    
    threadPool.submit(() -> {
      Optional<Integer> newBal = acc2.deposit(300);
      logger.info("Acc#2 :: New balance after deposit = " + newBal.orElse(-1).toString());
    });
    
    threadPool.submit(() -> {
      Optional<Integer[]> bals = acc1.transfer(acc2, 35);
      logger.info("------------------------------------------------------------------");
      logger.info("Acc#1 :: After successful transfer = " + bals.get()[0].toString());
      logger.info("Acc#2 :: After successful transfer = " + bals.get()[1].toString());
      logger.info("------------------------------------------------------------------");
    });
    
    threadPool.submit(() -> {
      Optional<Integer[]> bals = acc2.transfer(acc1, 50);
      logger.info("------------------------------------------------------------------");
      logger.info("Acc#1 :: After successful transfer = " + bals.get()[0].toString());
      logger.info("Acc#2 :: After successful transfer = " + bals.get()[1].toString());
      logger.info("------------------------------------------------------------------");
    });
    //
    // ----------------------------------------------------------------------------------
    
    logger.info("Account 1 balance = " + acc1.getBalance().get().toString());
    logger.info("Account 1 balance = " + acc1.getBalance().get().toString());
    logger.info("Account 1 balance = " + acc1.getBalance().get().toString());
    logger.info("Account 1 balance = " + acc1.getBalance().get().toString());
    
    // shutdown the threadPool, its job is done
    //
    shutdown(threadPool);
    
    // Hold for input, an easy way to make the main thread wait.
    // Other implementations might include use of CountDownLatch or Thread#join().
    //
    try (Scanner sc = new Scanner(System.in)) {
      sc.nextLine();
    } catch (Exception e) {}
    
    logger.info("Account 1 balance = " + acc1.getBalance().get().toString());
    
    // Poor man's debugging -- printing the JSON structures to check the current state
    // of the accounts.
    //
    logger.info("Account 1:\n" + acc1.toString());
    logger.info("Account 2:\n" + acc2.toString());
    stm.printState();
    
    // I prefer the callback approach, which can be extended into including Futures and Promises.
    // The {@link Future} is going to provide a better API when programming.
    // Asynchronous style of programming using the QSTM with guaranteed atomicity, isolation, and
    // order-specification.
    
    // Shutdown the STM as the work is done.l also cause the STM to clear up the resources.
    //
    stm.done();
  }
  
  /**
   * Stops the executor service softly.
   * 
   * @param executor
   *          The threadpool/executor service to shutdown.
   */
  private static void shutdown(ExecutorService executor) {
    
    try {
      
      executor.shutdown();
      executor.awaitTermination(5, TimeUnit.SECONDS);
      
    } catch (InterruptedException e) {
      
      logger.error("Tasks interrupted.", e);
      
    } finally {
      
      if (!executor.isTerminated()) {
        logger.error("Cancelled non-finished tasks.");
      }
      
      executor.shutdownNow();
      
      logger.info("Executor has been shutdown!");
      
    }
  }
  
}
