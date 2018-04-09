/**
 * BSD 3-Clause License
 * Copyright (c) 2018, Sidharth Mishra
 * All rights reserved.
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
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * TArraySTM.java
 * 
 * @author Sidharth Mishra <sidmishraw@gmail.com>
 * @created Apr 7, 2018 11:18:50 PM
 */
package examples;

import java.util.Scanner;
import java.util.function.Function;

import stm.STM;
import stm.TVar;
import stm.Transaction;

/**
 * Qualified Name: examples.TArraySTM
 */
public class TArraySTM {
  
  /**
   * The STM. It is going to handle the shared memory for us.
   */
  private static STM stm = new STM();
  
  /**
   * Creates a new transactional action that invokes the {@link TArray#add1001()} on the instance stored in the STM.
   * 
   * @param tvar
   *          The transactional variable that holds the TArray instance.
   * @return The transactional action that invokes the {@link TArray#add1001()} on the instance inside the transactional
   *         variable tvar.
   */
  private static Function<Transaction, Boolean> performAdd1001(TVar tvar) {
    
    return t -> {
      TArray ta = t.read(tvar, TArray.class);
      ta.add1001();
      return t.write(tvar, ta);
    };
    
  }
  
  @SuppressWarnings("unchecked")
  public static void main(String[] args) {
    
    // Create a transactional variable and store the TArray instance in it.
    TVar tarray = stm.newTVar(new TArray(1, 2, 3, 4, 5));
    
    // Perform the transactional actions.
    // All the transactional actions are performed on separate threads -- they run concurrently.
    stm.perform(performAdd1001(tarray));
    stm.perform(performAdd1001(tarray));
    stm.perform(performAdd1001(tarray));
    
    try (Scanner sc = new Scanner(System.in)) {
      sc.nextLine();  // hold till input
    }
    
    stm.printState();
  }
}
