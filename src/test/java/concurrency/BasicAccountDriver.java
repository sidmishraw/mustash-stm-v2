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
 * BasicAccountDriver.java
 * 
 * @author Sidharth Mishra <sidmishraw@gmail.com>
 * @created Apr 15, 2018 7:58:01 PM
 */
package concurrency;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import seq.Account;

/**
 * Basic account driver.
 *
 * Qualified Name: concurrency.BasicAccountDriver
 */
public class BasicAccountDriver {
  
  /**
   * LOGGER -- language specific -- I'm using SLF4J logging interface coupled with Logback backend.
   * It has no other purpose than providing logging support.
   */
  private static Logger logger = LoggerFactory.getLogger(BasicAccountDriver.class);
  
  /**
   * @param args
   */
  public static void main(String[] args) {
    
    // Create two accounts
    //
    Account acc1 = new Account("account1", 100);
    Account acc2 = new Account("account2", 200);
    
    // print the JSON structure for acc1 and acc2, cheap debugging.
    //
    logger.info("Account 1:\n" + acc1.toString());
    logger.info("Account 2:\n" + acc2.toString());
    
    // Create a threadPool having 4 threads to simulate multiple threads
    // requesting changes on the accounts.
    //
    ExecutorService threadPool = Executors.newFixedThreadPool(4);
    
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
    threadPool.submit(() -> acc1.deposit(50));
    threadPool.submit(() -> acc2.deposit(300));
    threadPool.submit(() -> acc1.transfer(acc2, 35));
    threadPool.submit(() -> acc2.transfer(acc1, 50));
    
    shutdown(threadPool);
    
    // print the JSON structure for acc1 and acc2, cheap debugging.
    //
    logger.info("Account 1:\n" + acc1.toString());
    logger.info("Account 2:\n" + acc2.toString());
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
