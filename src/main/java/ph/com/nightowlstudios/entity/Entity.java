package ph.com.nightowlstudios.entity;

import io.vavr.control.Try;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import ph.com.nightowlstudios.utils.Utils;

import java.lang.reflect.Method;
import java.util.Arrays;

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

  static <T extends Entity> JsonObject toJson(T entity) {
    return Utils.camelCaseKeys(JsonObject.mapFrom(entity));
  }

  static <T extends Entity> T fromJson(JsonObject json, Class<T> entityClass) {
    return Utils.camelCaseKeys(json).mapTo(entityClass);
  }

}
