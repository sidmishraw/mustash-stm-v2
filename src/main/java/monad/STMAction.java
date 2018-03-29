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
public class STMAction<R> {

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
   * First executes the current STMAction, the result is then shoved into the transformer function
   * and the resulting Monad is returned.
   */
  public <S> STMAction<S> bind(Function<R, STMAction<S>> transformer) {
    R actionResult = this.action.get();
    return transformer.apply(actionResult);
  }

  /**
   * Performs the STMAction and returns any result of the action.
   */
  public R unwrap() {
    return this.action.get();
  }

}
