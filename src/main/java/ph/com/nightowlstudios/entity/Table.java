package ph.com.nightowlstudios.entity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Table name of an {@link ph.com.nightowlstudios.entity.Entity}. Used by {@code PersistenceClient}
 *
 * @see ph.com.nightowlstudios.entity.Entity
 * @see ph.com.nightowlstudios.persistence.PersistenceClient
 *
 * @author <a href="mailto:josephharveyangeles@gmail.com">Joseph Harvey Angeles - <i>@yev</i></a>
 * @since 7/3/20
 **/
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Table {
    String value();
}