package persistence;

import entity.Column;
import entity.Entity;
import io.vavr.control.Try;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collector;

/**
 * @author <a href="mailto:josephharveyangeles@gmail.com">Joseph Harvey Angeles - <i>@{yev}</i></a>
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

    public static Collector<Row, ?, JsonArray> ofJsonObjects(String ... columnNames) {
        return Collector.of(
                JsonArray::new,
                (array, row) -> array.add(fromRow(row, columnNames)),
                (first, second) -> {
                    first.addAll(second);
                    return first;
                }
        );
    }

    private static <T extends Entity> T fromRow(Row row, Class<T> clasz)  {
        T attempt = null;
        try {
            attempt = (T) clasz.newInstance();
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
                        Try.of(() -> method.get().invoke(result, row.get(field.getType(), pos)));
                    }
                });
        return result;
    }

    private static JsonObject fromRow(Row row, String ... columnNames) {
        JsonObject json = new JsonObject();
        for (String column: columnNames) {
            json.put(column, row.getValue(column));
        }
        return json;
    }
}

