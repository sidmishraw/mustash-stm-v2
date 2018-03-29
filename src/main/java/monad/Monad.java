/**
 * Project: STMv2 Package: monad File: Monad.java
 * 
 * @author sidmishraw Last modified: Feb 2, 2018 8:04:50 PM
 */
package monad;

import java.util.function.Function;

/**
 * A Monad of type T.
 * 
 * Unlike in Haskell, I think there is no need for a `return` function in the Monad typeclass. Only
 * {@link Monad#bind(Function)} is enough.
 * 
 * @author sidmishraw
 *
 *         Qualified Name: monad.Monad
 *
 */
@Deprecated
public interface Monad<A> {

  /**
   * Binds or shoves a Monad {@code M a} into the function {@code (f: a -> M b)} to produce a new
   * Monad {@code (M b)}.
   * 
   * The bind method of a monad allows for chaining or composition.
   * 
   * Similar to Haskell's {@code (>>=) :: M a -> (a -> M b) -> M b}
   * 
   * @param fromAToMonadOfB A function that takes something of type A and returns a Monad of type B,
   *        {@code f :: a -> M b}
   * @return The result, Monad of type B, {@code Monad<B>}
   */
  public <C extends Monad<B>, B> C bind(Function<A, C> fromAToMonadOfB);

  /**
   * Unwraps the Monad and returns the contents of the Monad.
   * 
   * @return The contents of the Monad.
   */
  public A unwrap();
}
