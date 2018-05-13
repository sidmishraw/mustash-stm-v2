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
 * Account.java
 * 
 * @author Sidharth Mishra <sidmishraw@gmail.com>
 * @created Apr 10, 2018 1:47:07 PM
 */
package promised.bankacc;

import java.util.Optional;
import java.util.concurrent.Future;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.GsonBuilder;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import stm.Quarantine;
import stm.STM;
import stm.TVar;
import stm.Transaction;

/**
 * The bank account is the domain object. We segregate its content into identity
 * and state.
 * 
 * We let the STM manage the state. This ensures safe concurrency.
 * 
 * Qualified Name: isolate.Account
 */
public class Account {
  /**
   * The identity part of the account.
   */
  private @Getter AccountDetails details;
  
  /**
   * The state part of the account. It is maintained inside the STM.
   * So, we get the transactional variable that refers to the memory cell
   * where the account state is actually stored.
   */
  private @Getter TVar accountState;
  
  /**
   * The reference to the STM that will be managing the state of this account.
   */
  private @NonNull STM stm;
  
  /**
   * @Utility
   *          This an utility element just to provide the view of the account.
   *          It is only used internally -- implementation specific -- may vary
   *          upon use-cases.
   */
  // private Integer currentBalance;
  
  /**
   * Creates a new bank account.
   * 
   * @param accountName
   *          The name of the account.
   * @param initialBalance
   *          The initial balance in the account.
   */
  @Builder
  public Account(String accountName, Integer initialBalance, STM stm) {
    
    AccountDetails details = new AccountDetails(accountName);
    this.details = details;
    
    this.stm = stm;
    
    this.accountState = this.stm.newTVar(new AccountState(initialBalance));
    
  }
  
  /**
   * Deposits the amount into this account.
   * 
   * @param amount
   *          The amount to be deposited into the account.
   * @return The new balance of this account.
   */
  @SuppressWarnings("unchecked")
  public Optional<Integer> deposit(Integer amount) {
    
    Optional<Integer> bal = Optional.empty();
    
    Future<Optional<Quarantine>> fq = this.stm.perform((Transaction t) -> {
      
      AccountState as = t.read(this.accountState, AccountState.class);
      
      as.deposit(amount);
      
      return t.write(this.accountState, as);
    });
    
    try {
      
      Optional<Quarantine> q = fq.get();
      
      if (q.isPresent()) {
        
        bal = Optional.ofNullable(q.get().get(this.accountState, AccountState.class).get().getBalance());
      }
    } catch (Exception e) {}
    
    return bal;
  }
  
  /**
   * Withdraws the specified amount from this account.
   * 
   * @param amount
   *          The amount to be withdrawn from the account.
   * @return The new balance of this account.
   */
  @SuppressWarnings("unchecked")
  public Optional<Integer> withdraw(Integer amount) {
    
    Optional<Integer> bal = Optional.empty();
    
    Future<Optional<Quarantine>> fq = this.stm.perform((Transaction t) -> {
      
      AccountState as = t.read(this.accountState, AccountState.class);
      
      try {
        
        as.withdraw(amount);
        
        return t.write(this.accountState, as);
        
      } catch (Exception e) {
        
        return false;
        
      }
    });
    
    try {
      
      Optional<Quarantine> q = fq.get();
      
      if (q.isPresent()) {
        
        bal = Optional.ofNullable(q.get().get(this.accountState, AccountState.class).get().getBalance());
      }
    } catch (Exception e) {}
    
    return bal;
  }
  
  /**
   * Transfers the desired amount from this account to the destination account.
   * 
   * @param destination
   *          The account to transfer to.
   * @param amt
   *          The amount to transfer.
   */
  @SuppressWarnings("unchecked")
  public Optional<Integer[]> transfer(Account destination, Integer amt) {
    
    Optional<Integer[]> bals = Optional.empty();
    
    Future<Optional<Quarantine>> fq = this.stm.perform((Transaction t) -> {
      
      AccountState srcState = t.read(this.accountState, AccountState.class);
      AccountState destState = t.read(destination.accountState, AccountState.class);
      
      try {
        
        srcState.withdraw(amt);
        destState.deposit(amt);
        
        Boolean status = t.write(this.accountState, srcState);
        status = status && t.write(destination.accountState, destState);
        
        return status;
        
      } catch (Exception e) {
        
        return false;
        
      }
      
    });
    
    try {
      
      Optional<Quarantine> q = fq.get();
      
      if (q.isPresent()) {
        bals = Optional.ofNullable(new Integer[2]);
        bals.get()[0] = q.get().get(this.accountState, AccountState.class).get().getBalance();
        bals.get()[1] = q.get().get(destination.accountState, AccountState.class).get().getBalance();
      }
    } catch (Exception e) {}
    
    return bals;
  }
  
  /**
   * Gets the balance of the account and executes the callback.
   * 
   * @param callback
   *          The callback to execute after getting the balance from the QSTM.
   */
  @SuppressWarnings("unchecked")
  public Optional<Integer> getBalance() {
    Optional<Integer> bal = Optional.empty();
    Future<Optional<Quarantine>> fq = this.stm.perform((Transaction t) -> {
      t.read(this.accountState, AccountState.class);
      return true;
    });
    try {
      Optional<Quarantine> q = fq.get();
      if (q.isPresent()) {
        bal = Optional.ofNullable(q.get().get(this.accountState, AccountState.class).get().getBalance());
      }
    } catch (Exception e) {}
    return bal;
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
        
        switch (f.getName()) {
          case "stm":
          case "memCellLock":
          case "commitLock":
            return true;
          default:
            return false;
        }
        
      }
      
      @Override
      public boolean shouldSkipClass(Class<?> clazz) {
        return false;
      }
    }).setPrettyPrinting().create().toJson(this);
    
  }
  
}
