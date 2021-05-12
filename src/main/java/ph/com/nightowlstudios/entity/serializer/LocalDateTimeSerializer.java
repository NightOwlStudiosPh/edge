package ph.com.nightowlstudios.entity.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * @author <a href="mailto:josephharveyangeles@gmail.com">Joseph Harvey Angeles - <i>yev</i></a>
 * @since 4/10/21
 **/
public class LocalDateTimeSerializer extends StdSerializer<LocalDateTime> {

  protected LocalDateTimeSerializer() {
    this(LocalDateTime.class);
  }

  protected LocalDateTimeSerializer(Class<LocalDateTime> t) {
    super(t);
  }

  @Override
  public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider provider) throws IOException {
    Long epoch = value.atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();
    gen.writeString(epoch.toString());
  }
}
