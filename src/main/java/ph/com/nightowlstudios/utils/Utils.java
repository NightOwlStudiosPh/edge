package ph.com.nightowlstudios.utils;

import ph.com.nightowlstudios.entity.Entity;
import io.vavr.control.Try;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * @author <a href="mailto:josephharveyangeles@gmail.com">Joseph Harvey Angeles - <i>@yev</i></a>
 * @since 9/12/20
 */
public final class Utils {
    private Utils() {}

    public static <T> Optional<T> getFirstElement(List<T> list) {
        return list.isEmpty()
                ? Optional.empty()
                : Optional.ofNullable(list.get(0));
    }

    /**
     * Merges the field values of <code>to</code> onto <code>from</code>, replacing <code>from</code>'s value
     * with <code>to</code>'s <code>non-null</code> value. If <code>to</code>'s value for a certain field
     * is <code>null</code>, resulting return object will take <code>from</code>'s value for that field.
     * <br/><br/>
     * This does not mutate the original objects.
     *
     * @param from object to replace values with
     * @param to object whose values will be used to replace <code>from</code>'s
     * @param <T> <code>Entity.class</code> object should conform to <code>Table.class</code> and
     *           <code>Column.class</code>
     * @return the resulting <code>Entity</code> object.
     * @see ph.com.nightowlstudios.entity.Entity
     * @see ph.com.nightowlstudios.entity.Table
     * @see ph.com.nightowlstudios.entity.Column
     */
    public static <T extends Entity> T merge(T from, T to) {
        T result = Json.decodeValue(new JsonObject().encode(), (Class<T>) from.getClass());
        for (Field field: result.getClass().getDeclaredFields()) {
            final String prefix = field.getType().equals(boolean.class) ? "is" : "get";
            Supplier<?> toValue = () -> Try.of(() -> to.getClass()
                    .getDeclaredMethod(prefix + StringUtils.capitalize(field.getName()))
                    .invoke(to))
                    .getOrNull();

            Supplier<?> fromValue = () -> Try.of(() -> from.getClass()
                    .getDeclaredMethod(prefix + StringUtils.capitalize(field.getName()))
                    .invoke(from))
                    .getOrNull();

            if (toValue.get() == null) {
                Try.of(() -> result.getClass()
                        .getDeclaredMethod("set" + StringUtils.capitalize(field.getName()), field.getType())
                        .invoke(result, fromValue.get()));
            } else {
                Try.of(() -> result.getClass()
                        .getDeclaredMethod("set" + StringUtils.capitalize(field.getName()), field.getType())
                        .invoke(result, toValue.get()));
            }
        }
        return result;
    }
}


