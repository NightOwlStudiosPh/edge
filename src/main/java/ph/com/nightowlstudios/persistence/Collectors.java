package ph.com.nightowlstudios.persistence;

import io.vavr.control.Try;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ph.com.nightowlstudios.entity.Column;
import ph.com.nightowlstudios.entity.Entity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collector;

/**
 * @author <a href="mailto:josephharveyangeles@gmail.com">Joseph Harvey Angeles - <i>@yev</i></a>
 * @since 9/12/20
 */
public class Collectors {

  private static final Logger log = LoggerFactory.getLogger(Collectors.class);

  public static <T extends Entity> Collector<Row, ?, List<T>> ofEntities(Class<T> clasz) {
    return Collector.of(
      ArrayList::new,
      (list, row) -> list.add(fromRow(row, clasz)),
      (first, second) -> {
        first.addAll(second);
        return first;
      }
    );
  }

  public static Collector<Row, ?, JsonArray> ofJsonObjects(String... columnNames) {
    return Collector.of(
      JsonArray::new,
      (array, row) -> array.add(fromRow(row, columnNames)),
      (first, second) -> {
        first.addAll(second);
        return first;
      }
    );
  }

  public static <T extends Entity> T fromRow(Row row, Class<T> clasz) {
    T attempt = null;
    try {
      attempt = clasz.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      log.error("ERROR mapping Row to Entity: {}", e.getMessage());
      throw new RuntimeException(e.getCause());
    }
    T result = attempt;
    Arrays.stream(clasz.getDeclaredFields())
      .parallel()
      .filter(field -> field.isAnnotationPresent(Column.class))
      .forEach(field -> {
        Optional<Method> method = Optional
          .ofNullable(Try
            .of(() -> clasz.getDeclaredMethod("set" + StringUtils.capitalize(field.getName()), field.getType()))
            .getOrNull());

        if (!method.isPresent()) {
          return;
        }
        int pos = row.getColumnIndex(field.getDeclaredAnnotation(Column.class).value());
        if (pos >= 0) {
          Try.of(() -> method
            .get()
            .invoke(
              result,
              row.get(autoboxFieldType(field), pos))
          );
        }
      });
    return result;
  }

  private static Class<?> autoboxFieldType(Field field) {
    if (!field.getType().isPrimitive()) {
      return field.getType();
    }
    String type = field.getType().getTypeName();
    if (type.equals(byte.class.getTypeName())) {
      return Byte.class;
    } else if (type.equals(short.class.getTypeName())) {
      return Short.class;
    } else if (type.equals(int.class.getTypeName())) {
      return Integer.class;
    } else if (type.equals(long.class.getTypeName())) {
      return Long.class;
    } else if (type.equals(float.class.getTypeName())) {
      return Float.class;
    } else if (type.equals(double.class.getTypeName())) {
      return Double.class;
    } else if (type.equals(boolean.class.getTypeName())) {
      return Boolean.class;
    } else {
      throw new UnsupportedOperationException();
    }
  }

  public static JsonObject fromRow(Row row, String... columnNames) {
    JsonObject json = new JsonObject();
    for (String column : columnNames) {
      json.put(column, row.getValue(column));
    }
    return json;
  }

  public static <T> T getRowValue(Row row, Class<T> type, String column, T def) {
    return Optional.ofNullable(row.get(type, column)).orElse(def);
  }

  public static <T> T getRowValue(Row row, Class<T> type, String column) {
    return row.get(type, column);
  }
}

