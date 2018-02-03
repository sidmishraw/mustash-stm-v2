# STM v2.0

Author: Sidharth Mishra


## Descripton

Moved over to STMAction and Monads.

This is a work in progress. More updates soon.


## Usage Examples

The sample usage is demonstrated in the [SimpleDriver.java](./src/test/java/simple/SimpleDriver.java) class.

* Initializing the STM.

```java
/**
 * My STM
 */
private static final STM    stm    = new STM();
```

* Creating transactional variables (internally represented as memory cells):

```java
//
// STM#newTVar() now returns a STMAction which when performed creates a new memory cell in the STM.
// let my STM store an array of 5 ints [1,2,3,4,5] in one of its memory cells
//
STMAction<TVar<Integer[]>> creationAction = stm.newTVar(new Integer[] { 1, 2, 3, 4, 5 });

//
// To access the tVar, perform the creationAction (STNAction)
//
TVar<Integer[]> tVar = creationAction.unwrap();

//
// Or in order to chain other actions, one can use the Monad#bind()
//

//
//
//
stm.newTVar(1000).bind(account1 ->
//
//
stm.newTVar(500).bind(account2 ->
//
//
stm.newTVar(2000).bind(account3 ->

{
    //
    // operation
    //
    stm.exec(Transaction.builder().stm(stm).action(t -> t.read(account1).bind(acc1Bal -> {
        if (acc1Bal > 1000) {
            return withdraw(t, account1, 100).bind(b1 -> deposit(t, account2, 100).bind(b2 -> new STMAction<
                    Boolean>(() -> b1.booleanValue() && b2.booleanValue())));
        } else {
            return withdraw(t, account1, 500).bind(b1 -> deposit(t, account2, 500).bind(b2 -> new STMAction<
                    Boolean>(() -> b1.booleanValue() && b2.booleanValue())));
        }
    })).build());
    
    //
    // logging
    //
    stm.exec(Transaction.builder().stm(stm).action(t -> t.read(account1).bind(acc1 -> {
        logger.info("Acc1 = " + acc1);
        return t.read(account2);
    }).bind(acc2 -> {
        logger.info("Acc2 = " + acc2);
        return t.read(account3);
    }).bind(acc3 -> {
        logger.info("Acc3 = " + acc3);
        return new STMAction<>(() -> true);
    })).build());
    
    //
    //
    //
    return new STMAction<Void>(() -> null);
})));
       
    
```

* Making transactions is simpler now. I have included a TransactionBuilder (courtesy Lombok):

```java
/**
 * Makes a transaction that adds 1001 to the 3rd element of the array stored in a memory cell.
 * 
 * @param tVar
 *            the memory cell containing the array
 * @return the transaction
 */
private static Transaction makeT1(TVar<Integer[]> tVar) {
    return Transaction.builder()
    	.stm(stm)
    	.action(t -> t.read(tVar)
    		.bind(arr -> new STMAction<Integer[]>(() -> {
				arr[2] += 1001;
				return arr;
			}).bind(uarr -> t.write(tVar, uarr))))
	.build();
}
```

* Executing the transactions. Use `exec` to execute the transactions concurrently while making the parent thread wait for all the transactions to be done executing. Otherwise, use `forkAndExec` to fork and execute the transactions concurrently.

```java
stm.exec(makeT2(tVar), makeT1(tVar), makeT2(tVar), makeT2(tVar));
```


## Requirements

* Java 8+

* Gradle - wrapper included with the source

* Eclipse/IntelliJ Idea + Project Lombok - used for boilerplate reduction

* Java Deep Cloning Library - uses reflection to construct deep clones


## Changelog v3.0 - only on branch - with-stm-monad

* Introduced `Monad`.

* Introduced the `STMAction<R>` Monad.

* Updated the `Transaction` to include a `TransactionBuilder` to make the API more uniform.

* Haskell like `shove` or `bind` operations to utilize full power of Monads.


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