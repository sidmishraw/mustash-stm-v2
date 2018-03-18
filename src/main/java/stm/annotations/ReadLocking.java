/**
 * Project: STMv2 Package: stm.annotations File: ReadLocking.java
 * 
 * @author sidmishraw Last modified: Dec 18, 2017 6:24:31 PM
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
 * A marker annotation used to mark if a method will take read lock.
 * 
 * @author sidmishraw
 *
 *         Qualified Name: stm.annotations.ReadLocking
 *
 */
public @interface ReadLocking {
}
