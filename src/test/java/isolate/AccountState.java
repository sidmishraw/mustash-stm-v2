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
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * AccountState.java
 * 
 * @author Sidharth Mishra <sidmishraw@gmail.com>
 * @created Apr 10, 2018 1:38:10 PM
 */
package isolate;

import java.util.Objects;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.GsonBuilder;

import lombok.Getter;
import stm.Value;

/**
 * The mutable part of the bank account. It holds the contents that represent
 * the state of the account.
 * 
 * The state of an account changes throughtout its lifetime, and when shared
 * across multiple threads is a cause for
 * concern.
 * 
 * Qualified Name: isolate.AccountState
 */
public class AccountState extends State {
  
  /**
   * The balance of the account. For simplicity's sake, lets assume the balance is
   * an integer.
   */
  private @Getter int balance;
  
  /**
   * Creates a 0 balance account state.
   */
  public AccountState() {
    this.balance = 0;
  }
  
  /**
   * Creates the account's state.
   * 
   * @param balance
   *          The starting balance for the account.
   */
  public AccountState(int balance) {
    this.balance = balance;
  }
  
  /*
   * (non-Javadoc)
   * @see stm.Value#makeCopy()
   */
  @Override
  public Value makeCopy() {
    AccountState copy = new AccountState();
    copy.balance = this.balance;
    return copy;
  }
  
  /*
   * (non-Javadoc)
   * @see stm.Value#isEqual(stm.Value)
   */
  @Override
  public Boolean isEqual(Value v) {
    if (!(v instanceof AccountState)) return false;
    AccountState other = (AccountState) v;
    if (this.balance != other.balance) return false;
    return true;
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
   * @throws Exception
   */
  public void withdraw(Integer amount) throws Exception {
    if (amount > this.balance) throw new Exception("Balance too low to withdraw");
    this.deposit(-1 * amount);
  }
  
  /**
   * Note: Poor man's JSON creation, to be used for debugging.
   * Please ignore the implementation, we don't need to go deep into how this thing works.
   * 
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    
    return new GsonBuilder().setExclusionStrategies(new ExclusionStrategy() {
      
      @Override
      public boolean shouldSkipField(FieldAttributes f) {
        if (f.getName().equals("memCellLock")) return true;
        return false;
      }
      
      @Override
      public boolean shouldSkipClass(Class<?> clazz) {
        return false;
      }
    }).create().toJson(this);
    
  }
  
}
