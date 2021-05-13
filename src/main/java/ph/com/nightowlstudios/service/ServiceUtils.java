package ph.com.nightowlstudios.service;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import ph.com.nightowlstudios.dto.DTO;
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
  public static final String NIL_TYPE = "nil";

  private ServiceUtils() {
  }

  static JsonObject buildRequestPayload(Object... payload) {
    JsonArray array = new JsonArray();
    JsonArray types = new JsonArray();
    Arrays
      .stream(payload)
      .forEachOrdered(obj -> {
        if (isEntity(obj.getClass()) || isDTO(obj.getClass())) {
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
    return !typeName.equalsIgnoreCase(NIL_TYPE)
      ? Optional.ofNullable((R) Class.forName(typeName).cast(fromReplyPayload(body)))
      : Optional.empty();
  }

  @SuppressWarnings("unchecked")
  private static Object fromReplyPayload(JsonObject body) throws Exception {
    String typeName = body.getString(TYPE);

    Class<?> theClass = Class.forName(typeName);
    if (isEntity(Class.forName(typeName)) || isDTO(Class.forName(typeName))) {
      JsonObject payload = body.getJsonObject(PAYLOAD);
      return payload.mapTo(theClass);
    } else if (UUID.class.getName().equals(typeName)) {
      return UUID.fromString(body.getString(PAYLOAD));
    } else if (String.class.getName().equals(typeName)) {
      return body.getString(PAYLOAD);
    } else if (Integer.class.getName().equals(typeName)) {
      return body.getInteger(PAYLOAD);
    } else if (Long.class.getName().equals(typeName)) {
      return body.getLong(PAYLOAD);
    } else if (Instant.class.getName().equals(typeName)) {
      return body.getInstant(PAYLOAD);
    } else if (Double.class.getName().equals(typeName)) {
      return body.getDouble(PAYLOAD);
    } else if (Float.class.getName().equals(typeName)) {
      return body.getFloat(PAYLOAD);
    } else if (Number.class.getName().equals(typeName)) {
      return body.getNumber(PAYLOAD);
    } else if (Buffer.class.getName().equals(typeName)) {
      return body.getBuffer(PAYLOAD);
    } else if (Byte.class.getName().equals(typeName)) {
      return body.getBinary(PAYLOAD);
    } else if (typeName.toLowerCase().endsWith("list")) {
      List list = new ArrayList<>();
      JsonArray array = body.getJsonArray(PAYLOAD);
      for (int i = 0; i < array.size(); i++) {
        list.add(fromReplyPayload(array.getJsonObject(i)));
      }
      return list;
    } else {
      return body.getValue(PAYLOAD);
    }
  }

  static Object[] extractRequestPayloadParameters(JsonObject body) throws ClassNotFoundException {
    JsonArray payload = body.getJsonArray(PAYLOAD);
    JsonArray types = body.getJsonArray(TYPE);

    List<Object> args = new ArrayList<>();
    for (int pos = 0; pos < payload.size(); pos++) {
      String className = types.getString(pos);
      Class<?> theClass = Class.forName(className);

      if (isEntity(theClass) || isDTO(theClass)) {
        args.add(payload.getJsonObject(pos).mapTo(theClass));
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

  static <T> boolean isDTO(Class<T> tClass) {
    return tClass.getSuperclass().getName().equals(DTO.class.getName());
  }

  static Class<?>[] extractRequestPayloadParameterTypes(JsonObject body) throws ClassNotFoundException {
    JsonArray types = body.getJsonArray(TYPE);
    List<Class<?>> classes = new ArrayList<>();
    for (int i = 0; i < types.size(); i++) {
      classes.add(autoboxExtractParameterType(types.getString(i)));
    }
    return classes.toArray(new Class[0]);
  }

  static Class<?> autoboxExtractParameterType(String typeName) throws ClassNotFoundException {
    if (typeName.equals(Integer.class.getName())) {
      return int.class;
    } else if (typeName.equals(Long.class.getName())) {
      return long.class;
    } else if (typeName.equals(Float.class.getName())) {
      return float.class;
    } else if (typeName.equals(Double.class.getName())) {
      return double.class;
    } else if (typeName.equals(Boolean.class.getName())) {
      return boolean.class;
    } else if (typeName.equals(Character.class.getName())) {
      return char.class;
    } else if (typeName.equals(Byte.class.getName())) {
      return byte.class;
    } else if (typeName.equals(Short.class.getName())) {
      return short.class;
    } else {
      return Class.forName(typeName);
    }
  }

  static <T> JsonObject buildReplyPayload(T message) {
    JsonObject payload = new JsonObject();
    payload.put(PAYLOAD, toReplyPayload(message));
    payload.put(TYPE, Optional
      .ofNullable(message)
      .map(m -> m.getClass().getName())
      .orElse(NIL_TYPE)
    );
    return payload;
  }

  private static Object toReplyPayload(Object object) {
    if (object == null) {
      return StringUtils.EMPTY;
    } else if (isEntity(object.getClass()) || isDTO(object.getClass())) {
      return JsonObject.mapFrom(object);
    } else if (object.getClass().getName().equals(UUID.class.getName())) {
      return ((UUID) object).toString();
    } else if (object.getClass().getName().toLowerCase().endsWith("list")) {
      List<?> list = (List<?>) object;
      JsonArray array = new JsonArray();
      list.forEach(item -> array.add(buildReplyPayload(item)));
      return array;
    } else {
      return object;
    }
  }
}
