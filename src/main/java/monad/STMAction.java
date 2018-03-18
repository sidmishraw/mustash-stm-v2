/**
 * Project: STMv2 Package: monad File: STMAction.java
 * 
 * @author sidmishraw Last modified: Feb 2, 2018 8:06:46 PM
 */
package monad;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A STM action. To be used as the actions inside the Transactions.
 * 
 * @author sidmishraw
 *
 *         Qualified Name: monad.STMAction
 *
 */
public class STMAction<R> implements Monad<R> {

  /**
   * The action performed when the STMAction is executed.
   */
  private Supplier<R> action;

  /**
   * Creates a new STMAction with the specified action.
   * 
   * @param action the action to be performed when executing the STMAction.
   */
  public STMAction(Supplier<R> action) {
    this.action = action;
  }

  /**
   * First executes the current STMAction, the result is then shoved into the function
   * `fromAToMonadOfB` and the resulting Monad is returned.
   * 
   * @see monad.Monad#bind(java.util.function.Function)
   */
  @Override
  public <RR extends Monad<R1>, R1> RR bind(Function<R, RR> transformer) {
    R actionResult = this.action.get();
    return transformer.apply(actionResult);
  }

  /**
   * <p>
   * Performs the STMAction and returns any result of the action.
   * </p>
   * 
   * @see monad.Monad#unwrap()
   */
  @Override
  public R unwrap() {
    return this.action.get();
  }

}
