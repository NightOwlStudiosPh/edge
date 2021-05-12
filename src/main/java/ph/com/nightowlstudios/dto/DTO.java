package ph.com.nightowlstudios.dto;

import io.vertx.core.json.JsonObject;

/**
 * @author <a href="mailto:josephharveyangeles@gmail.com">Joseph Harvey Angeles - <i>yev</i></a>
 * @since 4/17/21
 **/
public abstract class DTO {

  public JsonObject toJson() {
    return JsonObject.mapFrom(this);
  }
}
