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
 * TArray.java
 * 
 * @author Sidharth Mishra <sidmishraw@gmail.com>
 * @created Apr 7, 2018 12:17:08 PM
 */
package examples;

import com.google.gson.Gson;

import stm.Value;

/**
 * Qualified Name: examples.TArray
 */
public class TArray implements Value {
  
  /**
   * The items being contained by this container.
   */
  private Integer[] data;
  
  /**
   * @return the data
   */
  public Integer[] getData() {
    return this.data;
  }
  
  /**
   * Creates a TArray container with the items.
   * 
   * @param items
   *          The items to be contained by this TArray container.
   */
  public TArray(Integer... items) {
    this.data = items;
  }
  
  /**
   * Adds 1001 to the 3rd element of this container if it exists, else does nothing.
   */
  public synchronized void add1001() {
    if (data.length < 3) return;
    this.data[2] = this.data[2] + 1001;
  }
  
  /**
   * Subtracts 1000 from the 3rd element of this container if it exists, else does nothing.
   */
  public void sub1000() {
    if (data.length < 3) return;
    this.data[2] = this.data[2] - 1000;
  }
  
  /*
   * (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return new Gson().toJson(this.data);
  }
  
  @Override
  public Value clone() {
    TArray ta = new TArray();
    Integer[] arr = new Integer[this.data.length];
    for (int i = 0; i < this.data.length; i++) {
      arr[i] = this.data[i];
    }
    ta.data = arr;
    return ta;
  }
  
  @Override
  public Boolean equals(Value v) {
    if (!(v instanceof TArray)) return false;
    TArray peer = (TArray) v;
    if (peer.data.length != this.data.length) return false;
    for (int i = 0; i < this.data.length; i++) {
      if (this.data[i] != peer.data[i]) return false;
    }
    return true;
  }
  
}
