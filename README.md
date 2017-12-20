# STM v2.0

Author: Sidharth Mishra

This is a work in progress. More updates soon.

## Requirements

* Java 8+

* Gradle - wrapper included with the source

* Eclipse/IntelliJ Idea + Project Lombok - used for boilerplate reduction



## Changelog

* Added `commitLock` and `stmLock` locks in the STM.

* Transaction building API inspired by S.P. Jones' `Beautiful Concurrency` [3].

* Removed `scanning` phase. MemoryCells have locks built-in.

* MemoryCells are parametrized.

## References

[1] N. Shavit and D. Touitou, "Software transactional memory", Distrib. Comput., vol. 10, no. 2, pp. 99-116, Feb 1997 [Online]. Available: https://doi.org/10.1007/s004460050028

[2] M. Weimerskirch, “Software transactional memory,” [Online]. Available: https://michel.weimerskirch.net/wp-content/uploads/2008/02/software_transactional_memory.pdf

[3] S. P. Jones, “Beautiful concurrency,” [Online]. Available: https://www.schoolofhaskell.com/school/advanced-haskell/beautiful-concurrency