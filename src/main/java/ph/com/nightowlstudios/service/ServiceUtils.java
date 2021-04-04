package ph.com.nightowlstudios.service;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import ph.com.nightowlstudios.entity.Table;

import java.time.Instant;
import java.util.*;

/**
 * @author <a href="mailto:josephharveyangeles@gmail.com">Joseph Harvey Angeles - <i>yev</i></a>
 * @since 4/4/21
 **/
public class ServiceUtils {

  public static final String PAYLOAD = "payload";
  public static final String TYPE = "type";

  private ServiceUtils() {}

  static JsonObject buildRequestPayload(Object ...payload) {
    JsonArray array = new JsonArray();
    JsonArray types = new JsonArray();
    Arrays
      .stream(payload)
      .forEachOrdered(obj -> {
        if (isEntity(obj.getClass())) {
          array.add(JsonObject.mapFrom(obj));
          types.add(obj.getClass().getName());
        } else if (obj.getClass().getName().equals(UUID.class.getName())) {
          array.add(((UUID) obj).toString()); // Add as string, receiver will just unwrap base on type.
          types.add(UUID.class.getName());    // Type as UUID to match exact method parameter of receiver.
        } else {
          array.add(obj);
          types.add(obj.getClass().getName());
        }
      });
    return new JsonObject()
      .put(PAYLOAD, array)
      .put(TYPE, types);
  }

  @SuppressWarnings("unchecked")
  static <R> Optional<R> unwrapRequestResponse(JsonObject body) throws Exception {
    String typeName = body.getString(TYPE);

    Class<?> theClass = Class.forName(typeName);
    final Object res;
    if (isEntity(theClass)) {
      JsonObject payload = body.getJsonObject(PAYLOAD);
      res = Json.decodeValue(payload.encode(), theClass);
    } else if (UUID.class.getName().equals(typeName)) {
      res = UUID.fromString(body.getString(PAYLOAD));
    } else if (String.class.getName().equals(typeName)) {
      res = body.getString(PAYLOAD);
    } else if (Integer.class.getName().equals(typeName)) {
      res = body.getInteger(PAYLOAD);
    } else if (Long.class.getName().equals(typeName)) {
      res = body.getLong(PAYLOAD);
    } else if (Instant.class.getName().equals(typeName)) {
      res = body.getInstant(PAYLOAD);
    } else if (Double.class.getName().equals(typeName)) {
      res = body.getDouble(PAYLOAD);
    } else if (Float.class.getName().equals(typeName)) {
      res = body.getFloat(PAYLOAD);
    } else if (Number.class.getName().equals(typeName)) {
      res = body.getNumber(PAYLOAD);
    } else if (Buffer.class.getName().equals(typeName)) {
      res = body.getBuffer(PAYLOAD);
    } else if (Byte.class.getName().equals(typeName)) {
      res = body.getBinary(PAYLOAD);
    } else {
      res = body.getValue(PAYLOAD);
    }
    return Optional.ofNullable((R) theClass.cast(res));
  }



  static Object[] extractRequestPayloadParameters(JsonObject body) throws ClassNotFoundException {
    JsonArray payload = body.getJsonArray(PAYLOAD);
    JsonArray types = body.getJsonArray(TYPE);

    List<Object> args = new ArrayList<>();
    for (int pos = 0; pos < payload.size(); pos++) {
      String className = types.getString(pos);
      Class<?> theClass = Class.forName(className);

      if (isEntity(theClass)) {
        args.add(Json.decodeValue(payload.getJsonObject(pos).encode(), theClass));
      } else if (UUID.class.getName().equals(className)) {
        args.add(UUID.fromString(payload.getString(pos)));
      } else if (String.class.getName().equals(className)) {
        args.add(payload.getString(pos));
      } else if (Integer.class.getName().equals(className)) {
        args.add(payload.getInteger(pos));
      } else if (Long.class.getName().equals(className)) {
        args.add(payload.getLong(pos));
      } else if (Instant.class.getName().equals(className)) {
        args.add(payload.getInstant(pos));
      } else if (Double.class.getName().equals(className)) {
        args.add(payload.getDouble(pos));
      } else if (Float.class.getName().equals(className)) {
        args.add(payload.getFloat(pos));
      } else if (Number.class.getName().equals(className)) {
        args.add(payload.getNumber(pos));
      } else if (Buffer.class.getName().equals(className)) {
        args.add(payload.getBuffer(pos));
      } else if (Byte.class.getName().equals(className)) {
        args.add(payload.getBinary(pos));
      } else {
        args.add(payload.getValue(pos));
      }
    }
    return args.toArray(new Object[0]);
  }

  static <T> boolean isEntity(Class<T> clasz) {
    return Arrays.stream(clasz.getDeclaredAnnotations())
      .anyMatch(a -> a.annotationType().getName().equals(Table.class.getName()));
  }

  static Class<?>[] extractRequestPayloadParameterTypes(JsonObject body) throws ClassNotFoundException {
    JsonArray types = body.getJsonArray(TYPE);
    List<Class<?>> classes = new ArrayList<>();
    for (int i = 0; i < types.size(); i++) {
      classes.add(Class.forName(types.getString(i)));
    }
    return classes.toArray(new Class[0]);
  }

  static <T> JsonObject buildReplyPayload(T message) {
    JsonObject payload = new JsonObject();
    if (isEntity(message.getClass())) {
      payload.put(PAYLOAD, JsonObject.mapFrom(message));
      payload.put(TYPE, message.getClass().getName());
    } else if (message.getClass().getName().equals(UUID.class.getName())) {
      payload.put(PAYLOAD, ((UUID) message).toString());
      payload.put(TYPE, UUID.class.getName());
    } else {
      payload.put(PAYLOAD, message);
      payload.put(TYPE, message.getClass().getName());
    }
    return payload;
  }
}
