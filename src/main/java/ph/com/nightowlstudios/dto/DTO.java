package ph.com.nightowlstudios.dto;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

/**
 * @author <a href="mailto:josephharveyangeles@gmail.com">Joseph Harvey Angeles - <i>yev</i></a>
 * @since 4/17/21
 **/
public abstract class DTO {

  public JsonObject toJson() {
    return JsonObject.mapFrom(this);
  }

  protected <T> JsonArray toJsonArray(List<T> objects) {
    JsonArray result = new JsonArray();
    objects.forEach(o -> result.add(JsonObject.mapFrom(o)));
    return result;
  }

}
