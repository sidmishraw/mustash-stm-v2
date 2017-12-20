/**
 * Project: STMv2
 * Package: stm.annotations
 * File: WriteLocking.java
 * 
 * @author sidmishraw
 *         Last modified: Dec 18, 2017 6:25:20 PM
 */
package stm.annotations;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Retention(SOURCE)
@Target(METHOD)
/**
 * A marker annotation used to mark if a method will take write lock.
 * 
 * @author sidmishraw
 *
 *         Qualified Name: stm.annotations.WriteLocking
 *
 */
public @interface WriteLocking {}
