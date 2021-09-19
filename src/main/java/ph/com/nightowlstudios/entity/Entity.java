package ph.com.nightowlstudios.entity;

import io.vavr.control.Try;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import ph.com.nightowlstudios.utils.Utils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * This just serves as a tagging type for Persistence Entities.
 * Subclasses should implement {@link ph.com.nightowlstudios.entity.Table} and {@link ph.com.nightowlstudios.entity.Column} as
 * the {@code tableName} and {@code column} respectively.
 *
 * @author <a href="mailto:josephharveyangeles@gmail.com">Joseph Harvey Angeles - <i>@yev</i></a>
 * @see ph.com.nightowlstudios.entity.Table
 * @see ph.com.nightowlstudios.entity.Column
 * @since 7/3/20
 */
public interface Entity {

  static <T extends Entity> T setId(T entity, UUID id) {
    try {
      FieldUtils.writeField(entity, "id", id, true);
      return entity;
    } catch (IllegalAccessException | IllegalArgumentException e) {
      throw new IllegalStateException(String.format("Error setting id of %s entity", entity.getClass().getName()));
    }
  }

  static <T extends Entity> String getTableName(Class<T> entity) {
    return entity.getAnnotation(Table.class).value();
  }

  static <T extends Entity> String[] getColumns(Class<T> clasz) {
    return Arrays
            .stream(clasz.getDeclaredFields())
            .filter(field -> field.isAnnotationPresent(Column.class))
            .map(field -> field.getDeclaredAnnotation(Column.class).value())
            .toArray(String[]::new);
  }

  static <T extends Entity> String[] getColumnsWithoutId(Class<T> clasz) {
    return Arrays
            .stream(clasz.getDeclaredFields())
            .filter(field -> field.isAnnotationPresent(Column.class))
            .filter(field -> !StringUtils.equals(field.getName(), "id"))
            .map(field -> field.getDeclaredAnnotation(Column.class).value())
            .toArray(String[]::new);
  }

  static <T extends Entity> String column(Class<T> entityClass, String column) {
    return String.format("%s.%s", Entity.getTableName(entityClass), column);
  }

  static <T extends Entity> JsonObject toJson(T entity) {
    return Utils.camelCaseKeys(JsonObject.mapFrom(entity));
  }

  static <T extends Entity> T fromJson(JsonObject json, Class<T> entityClass) {
    return Utils.camelCaseKeys(json).mapTo(entityClass);
  }

  /**
   * Merges the field values of <code>to</code> onto <code>from</code>, replacing <code>from</code>'s value
   * with <code>to</code>'s <code>non-null</code> value. If <code>to</code>'s value for a certain field
   * is <code>null</code>, resulting return object will take <code>from</code>'s value for that field.
   * <br><br>
   * This does not mutate the original objects.
   *
   * @param from object to replace values with
   * @param to   object whose values will be used to replace <code>from</code>'s
   * @param <T>  <code>Entity.class</code> object should conform to <code>Table.class</code> and
   *             <code>Column.class</code>
   * @return the resulting <code>Entity</code> object.
   * @see ph.com.nightowlstudios.entity.Entity
   * @see ph.com.nightowlstudios.entity.Table
   * @see ph.com.nightowlstudios.entity.Column
   */
  @SuppressWarnings("unchecked")
  static <T extends Entity> T merge(T from, T to) {
    T result = Json.decodeValue(new JsonObject().encode(), (Class<T>) from.getClass());
    for (Field field : result.getClass().getDeclaredFields()) {
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
