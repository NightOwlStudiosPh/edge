package ph.com.nightowlstudios.entity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines the column name of a certain {@link ph.com.nightowlstudios.entity.Table}
 *
 * @author <a href="mailto:josephharveyangeles@gmail.com">Joseph Harvey Angeles - <i>@yev</i></a>
 * @see ph.com.nightowlstudios.entity.Table
 * @since 7/3/20
 **/
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Column {
  String value();
}
