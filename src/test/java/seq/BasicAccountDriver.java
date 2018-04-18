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
 * @created Apr 15, 2018 7:16:21 PM
 */
package seq;

/**
 *
 * Qualified Name: seq.BasicAccountDriver
 *
 */
public class BasicAccountDriver {
  
  /**
   * @param args
   */
  public static void main(String[] args) {
    
    // Create two accounts
    //
    Account acc1 = new Account("account1", 100);
    Account acc2 = new Account("account2", 200);
    
    System.out.println("Account 1 = " + acc1.toString());
    System.out.println("Account 2 = " + acc2.toString());
    
    acc1.deposit(50);
    acc2.deposit(200);
    
    acc1.transfer(acc2, 40);
    acc2.transfer(acc1, 50);
    
    System.out.println("Account 1 = " + acc1.toString());
    System.out.println("Account 2 = " + acc2.toString());
    
  }
  
}
