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
 * Quarantine.java
 * 
 * @author Sidharth Mishra <sidmishraw@gmail.com>
 * @created May 13, 2018 12:04:58 PM
 */
package stm;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A transaction quarantine. Used to isolate data when reading it from the STM.
 * 
 * Qualified Name: stm.Quarantine
 *
 */
public class Quarantine {
  
  /**
   * The hash-table used for holding the mapping between a memory cell and its value.
   * This is the internal implementation of the quarantine.
   */
  private Map<MemoryCell, Value> table;
  
  /**
   * Create a new quarantine.
   */
  Quarantine() {
    this.table = new HashMap<>();
  }
  
  /**
   * Creates a new quarantine from the given table. The entries of the quanrantine are copied over.
   * The {@link Value#makeCopy()} is used for creating the copies.
   * 
   * @param m
   *          The mapping to use for constructing the new quarantine.
   * 
   * @param n
   *          The mapping to use for constructing the new quarantine.
   */
  Quarantine(Map<MemoryCell, Value> rq, Map<MemoryCell, Value> wq) {
    this.table = new HashMap<>();
    rq.forEach((memCell, val) -> {
      Value v = val.makeCopy();
      this.table.put(memCell, v);
    });
    wq.forEach((memCell, val) -> {
      Value v = val.makeCopy();
      this.table.put(memCell, v);
    });
  }
  
  /**
   * Gets the value from the quarantine for the provided transactional variable reference.
   * 
   * @param ref
   *          The TVar (transactional variable) reference whose value is needed.
   * @param clasz
   *          The concrete type of the transactional variable's value.
   * @return The optional value of the transactional variable casted to the concrete type.
   */
  public <V> Optional<V> get(TVar ref, Class<V> clasz) {
    if (Objects.isNull(ref)) return Optional.empty();
    V val = clasz.cast(this.table.get((MemoryCell) ref));
    return Optional.ofNullable(val);
  }
  
  /**
   * Stores the value into the quarantine mapping it against the transactional variable reference.
   * 
   * @param ref
   *          The transactional variable reference.
   * @param value
   *          The value to be stored in the quarantine.
   */
  public void put(TVar ref, Optional<Value> value) {
    if (Objects.isNull(ref)) return;
    if (value.isPresent()) {
      this.table.put((MemoryCell) ref, value.get());
    }
  }
  
  /**
   * Resets the quarantine, deleting all the mappings it previously held.
   */
  public void reset() {
    this.table.clear();
  }
}
