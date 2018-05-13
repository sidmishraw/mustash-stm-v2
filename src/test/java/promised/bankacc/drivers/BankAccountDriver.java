/**
 * BSD 3-Clause License
 * 
 * Copyright (c) 2018, Sidharth Mishra
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * * Neither the name of the copyright holder nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS AS IS
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * BankAccountDriver.java
 * 
 * @author Sidharth Mishra <sidmishraw@gmail.com>
 * @created Apr 10, 2018 3:44:04 PM
 */
package promised.bankacc.drivers;

import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import isolate.Account;
import stm.STM;

/**
 * Single main thread simulation. This simulates the scenario when all the actions are
 * being done from 1 thread.
 * 
 * Qualified Name: isolate.BankAccountDriver
 */
public class BankAccountDriver {
  
  /**
   * LOGGER -- language specific -- I'm using SLF4J logging interface coupled with Logback backend.
   * It has no other purpose than providing logging support.
   */
  private static Logger logger = LoggerFactory.getLogger(BankAccountDriver.class);
  
  /**
   * Instantiating the STM for this application. Ideally, having 1 STM throughout the lifetime of an
   * application should be ideal.
   */
  private static final STM stm = new STM();
  
  /**
   * @param args
   */
  public static void main(String[] args) {
    
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
    
    // perform the actions on the accounts
    // deposit 50 into acc1: acc1 = 150
    // deposit 300 into acc2: acc2 = 500
    // transfer 35 from acc1 to acc2: acc1 = 150 - 35 = 115, acc2 = 500 + 35 = 535
    // transfer 50 from acc2 to acc1: acc1 = 115 + 50 = 165, acc2 = 535 - 50 = 485
    //
    // Note: all these actions happen to be running on separate threads under the hood.
    //
    acc1.deposit(50);
    acc2.deposit(300);
    acc1.transfer(acc2, 35);
    acc2.transfer(acc1, 50);
    
    // Hold for input, an easy way to make the main thread wait.
    // Other implementations might include use of CountDownLatch or Thread#join().
    //
    // Poor man's CountdownLatch!
    //
    try (Scanner sc = new Scanner(System.in)) {
      sc.nextLine();
    } catch (Exception e) {}
    
    // Poor mans debugging -- printing the JSON structures to check the current state
    // of the accounts.
    //
    logger.info("Account 1:\n" + acc1.toString());
    logger.info("Account 2:\n" + acc2.toString());
    stm.printState();
    
  }
  
}
