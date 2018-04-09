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
 * TArrayDriver.java
 * 
 * @author Sidharth Mishra <sidmishraw@gmail.com>
 * @created Apr 7, 2018 12:22:56 PM
 */
package examples;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Qualified Name: examples.TArrayDriver
 */
public class TArrayDriver {
  
  /**
   * @param args
   */
  public static void main(String[] args) {
    
    TArray ta = new TArray(1, 2, 3, 4, 5);
    
    ExecutorService executor = Executors.newFixedThreadPool(4);
    
    IntStream.range(0, 10000).forEach(i -> executor.submit(ta::add1001));
    
    shutdown(executor);
    
    System.out.println(ta);
    
    // try (Scanner sc = new Scanner(System.in)) {
    // sc.nextLine();
    // }
    //
    // System.out.println(ta);
  }
  
  /**
   * Stops the executor service softly.
   * 
   * @param executor
   */
  static void shutdown(ExecutorService executor) {
    try {
      executor.shutdown();
      executor.awaitTermination(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      System.err.println("tasks interrupted");
    } finally {
      if (!executor.isTerminated()) {
        System.err.println("cancelled non-finished tasks");
      }
      executor.shutdownNow();
      System.out.println("Executor has been shutdown!");
    }
  }
  
}
