package ph.com.nightowlstudios.utils;

import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:josephharveyangeles@gmail.com">Joseph Harvey Angeles - <i>@yev</i></a>
 * @since 9/12/20
 */
public final class Utils {

  private static final String UNDERSCORE = "_";

  private Utils() {
  }

  public static <T> Optional<T> getFirstElement(List<T> list) {
    return list.isEmpty()
      ? Optional.empty()
      : Optional.ofNullable(list.get(0));
  }

  public static JsonObject camelCaseKeys(JsonObject json) {
    json.stream()
      .parallel()
      .filter(e -> e.getKey().contains("_"))
      .forEach(e -> {
        json.put(Utils.toCamelCase(e.getKey()), e.getValue());
        json.remove(e.getKey());
      });
    return json;
  }

  public static String toCamelCase(String word) {
    return toCamelCase(word, UNDERSCORE);
  }

  public static String toCamelCase(String word, String separator) {
    if (StringUtils.isBlank(word)) {
      return StringUtils.EMPTY;
    }
    String[] tokens = word.split(separator);
    if (tokens.length == 1) {
      return tokens[0].trim().toLowerCase();
    }
    String head = tokens[0];
    String[] tail = Arrays.copyOfRange(tokens, 1, tokens.length);
    return head.toLowerCase() + Arrays
      .stream(tail)
      .sequential()
      .map(StringUtils::capitalize)
      .collect(Collectors.joining());
  }
}


