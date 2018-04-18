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
 * Account.java
 * 
 * @author Sidharth Mishra <sidmishraw@gmail.com>
 * @created Apr 15, 2018 7:09:28 PM
 */
package seq;

import java.util.Objects;
import java.util.UUID;

import lombok.Getter;

/**
 * The bank account is the domain object.
 * 
 * Qualified Name: seq.Account
 *
 */
public class Account {
  
  /**
   * The unique identifier of the account.
   * Generated from UUID 4.
   */
  private @Getter String ID;
  
  /**
   * The name of the account.
   */
  private @Getter String name;
  
  /**
   * The balance of the account. For simplicity's sake,
   * lets assume the balance is an integer.
   */
  private @Getter int balance;
  
  /**
   * Creates a new bank account.
   * 
   * @param name
   *          The name of the account.
   * @param balance
   *          The initial balance of the account.
   */
  public Account(String name, int balance) {
    this.ID = UUID.randomUUID().toString();
    this.name = name;
    this.balance = balance;
  }
  
  /**
   * Deposit adds the amount to the account's balance.
   * Note: This is a behavior that mutates the state of the account.
   * 
   * @param amount
   *          The amount to deposit.
   */
  public void deposit(Integer amount) {
    
    if (Objects.isNull(amount)) return;
    
    this.balance = this.balance + amount;
  }
  
  /**
   * Withdraw reduces the balance if it is enough, else does nothing.
   * Note: This is a behavior that mutates the state of the account.
   * 
   * @param amount
   *          The amount to withdraw from the account.
   * 
   * @throws Exception
   */
  public void withdraw(Integer amount) throws Exception {
    
    if (amount > this.balance || Objects.isNull(amount)) throw new Exception("Balance too low to withdraw");
    
    this.deposit(-1 * amount);
  }
  
  /**
   * Transfers the desired amount from this account to the destination account.
   * 
   * @param dest
   *          The destination account.
   * @param amount
   *          The desired amount.
   */
  public void transfer(Account dest, Integer amount) {
    
    try {
      
      this.withdraw(amount);
      dest.deposit(amount);
      
    } catch (Exception e) {
      
      System.err.println("Failed to transfer, insufficient funds");
    }
  }
  
  /*
   * (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "Account [ID=" + this.ID + ", name=" + this.name + ", balance=" + this.balance + "]";
  }
}
