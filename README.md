# STM v2.0

Author: Sidharth Mishra


## Descripton

This is a work in progress. More updates soon.


## Usage Examples

The sample usage is demonstrated in the [SimpleDriver.java](./src/test/java/SimpleDriver.java) class.

* Initializing the STM.

```java
/**
 * My STM
 */
private static final STM    stm    = new STM();
```

* Making transactions is simpler now.

```java
/**
 * Makes a transaction that adds 1001 to the 3rd element of the array stored in a memory cell.
 * 
 * @param memCell
 *            the memory cell containing the array
 * @return the transaction
 */
private static Transaction makeT1(MemoryCell<Integer[]> memCell) {
    return Transactions.newT(stm).begin((t) -> {
        Integer[] arr = t.read(memCell); // read the contents of the memCell
        arr[2] += 1001; // update the value
        return t.write(memCell, arr); // write the contents to the memCell
    }).then(t -> {
        logger.info("Logging from the then clause!");
        return true;
    }).end().done();
}
```

* Executing the transactions. Use `exec` to execute the transactions concurrently while making the parent thread wait for all the transactions to be done executing. Otherwise, use `forkAndExec` to fork and execute the transactions concurrently.

```java
stm.exec(makeT2(memCell), makeT1(memCell), makeT2(memCell), makeT2(memCell));
```


## Requirements

* Java 8+

* Gradle - wrapper included with the source

* Eclipse/IntelliJ Idea + Project Lombok - used for boilerplate reduction

* Java Deep Cloning Library - uses reflection to construct deep clones


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