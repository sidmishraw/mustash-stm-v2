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
 * STMAction.java
 * 
 * @author Sidharth Mishra <sidmishraw@gmail.com>
 * @created Apr 27, 2018 12:00:48 PM
 */
package stm;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The STMAction monad is used to represent the implicit STM specific actions.
 * These actions when performed will have some side-effect on the STM's state and
 * return a value/data.
 * 
 * Generally, this is used in the {@link STM#newTVar(Value)}, {@link STM#deleteTVar(TVar)},
 * {@link Transaction#read(TVar, Class)}, {@link Transaction#write(TVar, Value)}, and transactional
 * actions.
 * 
 * We follow the Monad pattern to build this STMAction monad.
 * 
 * Qualified Name: stm.STMAction
 *
 */
public class STMAction<T> {
  
  /**
   * This {@link Supplier} function is used as a wrapper computation to make
   * the side-effect explicit.
   */
  private Supplier<T> action;
  
  /**
   * The unit function for this monad, takes the value and gives the computation.
   * 
   * @param action
   *          The computation logic of this monad.
   */
  public STMAction(Supplier<T> action) {
    this.action = action;
  }
  
  /**
   * Performs the implicit side-effect and returns the explicit result.
   * 
   * @return The standalone value or explicit result after performing the side-effect.
   */
  public T unwrap() {
    return this.action.get();
  }
  
  /**
   * The bind operation is used for chaining together STMActions together. Following the monadic bind
   * patter.
   * 
   * @param transformer
   *          The transformer function transforms this monad's contents.
   * @return The new STMAction monad with modified contents.
   */
  public <S> STMAction<S> bind(Function<T, STMAction<S>> transformer) {
    return transformer.apply(this.action.get());
  }
}
