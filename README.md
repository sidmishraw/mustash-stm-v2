# STM v2.2

Author: Sidharth Mishra

---- Documentation needs to rewritten -------

## Descripton

A monadic quarantined STM (m-QSTM) is different from the regular STM in the aspect that it uses two maps/tables for thread local quarantines and the STM specific actions are explicit actions following the monad pattern.

When the transaction needs to read the data from a memory cell, for the first read, the transaction reads the data directly from the memory cell.
Then, it stores this data into its read quarantined map. All the subsequent reads happen from this read quarantine.
Similarly, the when the transaction needs to write a value into a memory cell, it writes to its write quarantine. The actual data of the memory cell is updated during the commit phase.

When the transaction enters its commit phase, it first acquires the commit lock on the STM. After acquiring the commit lock, it validates its read quarantine members by equating their thread local values with the actual values. Upon successful validation, it flushes its write set members into the STM.
Finally, it releases the commit lock on the STM.

All the threads run in complete isolation and their changes become visible to their peers at once when they successfully commit.


## Usage Examples

The sample usage is demonstrated in the [SimpleDriver.java](./src/test/java/simple/SimpleDriver.java) class.

* Initializing the STM.

```java
/**
 * My STM
 */
private static final STM    stm    = new STM();
```

* The values stored in a memory cell need to realize the `stm.Value` interface.
For example, lets assume that we have a class called `TArray` which is a simulation for transactional integer array.

```java

/**
 * A sample value that can be stored in the STM.
 * Qualified Name: simple.TArray
 */
class TArray implements Value {

  Integer[] data;
  
  /**
   * Creates a new TArray instance.
   * 
   * @param integers
   *          The elements in the data.
   */
  public TArray(Integer... integers) {
    this.data = integers;
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

```
The `clone()` and `equals()` methods need to be implemented by the data that is intended to be stored in the memory cell.
The `clone()` method ensures that there is no accidental modification of the contents of the memory cell during the transaction's execution.
The `equal()` method is necessary to check the equality of the contents of the memory cell during the commit phase of the transaction.


* Creating transactional variables (internally represented as memory cells):

```java
// let my STM store an array of 5 ints [1,2,3,4,5] in one of its memory cells
TVar tvar = stm.newTVar(new TArray(1, 2, 3, 4, 5));
```

* The STM accepts `transactional actions` and processes them for us. The transactional actions are of the form `f :: Transaction -> Boolean` or
`Function<Transaction, Boolean>`.

```java

  /**
   * Creates a transactional action that adds 1001 to the 3rd element of the TArray stored in a memory cell.
   * 
   * @param tvar
   *          The transactional variable holding the TArray.
   * @return The transactional action.
   */
  private static Function<Transaction, Boolean> add1001(TVar tvar) {
    return (t) -> {
      TArray tArr = t.read(tvar, TArray.class);
      tArr.data[2] += 1001;
      return t.write(tvar, tArr);
    };
  }
  
  /**
   * Creates a transactional action that deducts 1000 from the 3rd element of the array stored in a memory
   * cell.
   * 
   * @param tvar
   *          The memory cell containing the array.
   * @return The transactional action.
   */
  private static Function<Transaction, Boolean> subtract1000(TVar tvar) {
    return (t) -> {
      TArray tarr = t.read(tvar, TArray.class);
      tarr.data[2] -= 1000;
      return t.write(tvar, tarr);
    };
  }
```
The STM internally generates a transaction to execute the transactional action.

* Using the `perform()` method of the STM, the transactional actions can be submitted to the STM. 

```java
stm.perform(add1001(tvar));
```


## Requirements

* Java 8+

* Gradle - wrapper included with the source

* Eclipse/IntelliJ Idea + Project Lombok - used for boilerplate reduction

* Java Deep Cloning Library - uses reflection to construct deep clones



## Changelog v2.2 - only on branch - quarantined-and-fattened

* Moved from a generics `MemoryCell<T>` into `Value`. This relieves the use of `deep cloner` library.

* The `STM` class has been fattened with more responsiblity, the user no longer has the ability to create transactions.
They only create transactional actions which are submitted to the `STM` to perform. 


## Changelog v2.1 - only on branch - without-ownerships

* Made `MemoryCell` package scoped, renamed the `newMemCell` method to `newTVar`. Added `TVar` empty interface to make sure that the transactional variables or memory cells are only operated internally -- within the `stm` package.

* Transactional `read` and `write` methods now take `TVar`s as arguments instead of `MemoryCell`s. 


## Changelog v2.0 - only on branch - without-ownerships

* Removed `ownerships` map from STM. Now, the STM is only an object with list of memory cells.

* Removed `stmLock`, the STM only needs the `commitLock`. The `commitLock` is used for making sure that the changes of a transaction are visible to all other transactions all at once.
  This ensures *ATOMICITY* and *ISOLATION* are enforced.
  
* Using a lock internally makes the code less denser and helps prune away uncertainity. The transactions are now dynamic in the sense that the consumer doesn't need to specify their read-set and write-set members explicitly.
  The API manages this internally.
  
* Added Java Deep Cloning Library to make deep clones of data read from the memory cells inside transactions. This prevents the consumer from accidentally updating the contents of the memory cells before the transaction's commit phase.
  Also, this is a step in the direction to achieve immutability of data.


## Changelog v1.0
 
* Added `commitLock` and `stmLock` locks in the STM.

* Transaction building API inspired by S.P. Jones' `Beautiful Concurrency` [3].

* Removed `scanning` phase. MemoryCells have locks built-in.

* MemoryCells are parametrized.


## References

[1] N. Shavit and D. Touitou, "Software transactional memory", Distrib. Comput., vol. 10, no. 2, pp. 99-116, Feb 1997 [Online]. Available: https://doi.org/10.1007/s004460050028

[2] M. Weimerskirch, “Software transactional memory,” [Online]. Available: https://michel.weimerskirch.net/wp-content/uploads/2008/02/software_transactional_memory.pdf

[3] S. P. Jones, “Beautiful concurrency,” [Online]. Available: https://www.schoolofhaskell.com/school/advanced-haskell/beautiful-concurrency