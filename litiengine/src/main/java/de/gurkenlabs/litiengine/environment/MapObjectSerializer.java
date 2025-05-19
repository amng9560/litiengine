package de.gurkenlabs.litiengine.environment;

import de.gurkenlabs.litiengine.entities.IEntity;
import de.gurkenlabs.litiengine.environment.tilemap.IMapObject;
import de.gurkenlabs.litiengine.environment.tilemap.MapObjectProperty;
import de.gurkenlabs.litiengine.environment.tilemap.TmxProperty;
import de.gurkenlabs.litiengine.environment.tilemap.TmxType;
import de.gurkenlabs.litiengine.environment.tilemap.xml.DecimalFloatAdapter;
import de.gurkenlabs.litiengine.environment.tilemap.xml.MapObject;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class MapObjectSerializer {
  private static final Logger log = Logger.getLogger(MapObjectSerializer.class.getName());

  private MapObjectSerializer() {}

  public static MapObject serialize(IEntity entity) {
    MapObject obj = new MapObject();
    obj.setId(entity.getMapId());
    obj.setX((float) entity.getX());
    obj.setY((float) entity.getY());
    obj.setWidth((float) entity.getWidth());
    obj.setHeight((float) entity.getHeight());
    obj.setName(entity.getName());

    TmxType type = entity.getClass().getAnnotation(TmxType.class);
    if (type != null) {
      obj.setType(type.value().toString());
    }

    serialize(entity.getClass(), entity, obj);

    if (!entity.getTags().isEmpty()) {
      obj.setValue(MapObjectProperty.TAGS, String.join(",", entity.getTags()));
    }

    return obj;
  }

  private static <T extends IEntity> void serialize(Class<?> clz, T entity, MapObject mapObject) {

    for (final Field field : clz.getDeclaredFields()) {
      serialize(field, entity, mapObject);
    }

    // recursively call all parent classes and serialize annotated fields
    Class<?> parentClass = clz.getSuperclass();
    if (parentClass != null) {
      serialize(parentClass, entity, mapObject);
    }
  }

  private static void serialize(Field field, Object entity, IMapObject mapObject) {
    TmxProperty property = field.getAnnotation(TmxProperty.class);
    if (property == null) {
      return;
    }

    Object value;
    try {
      if (!field.canAccess(entity)) {
        field.setAccessible(true);
      }

      value = field.get(entity);
      if (value == null) {
        return;
      }

      mapObject.setValue(property.name(), getPropertyValue(field, value));
    } catch (IllegalAccessException e) {
      log.log(Level.SEVERE, e.getMessage(), e);
    }
  }

  private static String getPropertyValue(Field field, Object value) {
    if (field.getType().equals(Float.class) || field.getType().equals(Double.class)) {
      try {
        return new DecimalFloatAdapter().marshal((Float) value);
      } catch (Exception e) {
        log.log(Level.SEVERE, e.getMessage(), e);
      }
    } else if (field.getType().equals(Integer.class)) {
      return Integer.toString((int) value);
    } else if (field.getType().equals(short.class)) {
      return Short.toString((short) value);
    } else if (field.getType().equals(byte.class)) {
      return Byte.toString((byte) value);
    } else if (field.getType().equals(long.class)) {
      return Long.toString((long) value);
    }

    if (value instanceof List<?>) {
      return Arrays.stream((Object[]) value)
        .map(Object::toString)
        .collect(Collectors.joining(","));
      // special handling
    }

    if (value.getClass().isArray()) {
      if (field.getType().getComponentType() == int.class) {
        return Arrays.stream((int[]) value)
          .mapToObj(String::valueOf)
          .reduce((a, b) -> a + "," + b)
          .orElse("");
      } else if (field.getType().getComponentType() == double.class) {
        return Arrays.stream((double[]) value)
          .mapToObj(String::valueOf)
          .reduce((a, b) -> a + "," + b)
          .orElse("");
      } else if (field.getType().getComponentType() == float.class) {
        return Arrays.stream((float[]) value)
          .mapToObj(String::valueOf)
          .collect(Collectors.joining(",", "[", "]"));
      } else if (field.getType().getComponentType() == short.class) {
        return Arrays.stream((short[]) value)
          .mapToObj(String::valueOf)
          .collect(Collectors.joining(","));
      } else if (field.getType().getComponentType() == byte.class) {
        return new String((byte[]) value, StandardCharsets.UTF_8);
      } else if (field.getType().getComponentType() == long.class) {
        return Arrays.stream((long[]) value)
          .mapToObj(String::valueOf)
          .reduce((a, b) -> a + "," + b)
          .orElse("");
      } else if (field.getType().getComponentType() == String.class) {
        return Arrays.stream((String[]) value)
          .collect(Collectors.joining(","));
      } else if (field.getType().getComponentType() == boolean.class) {
        return Arrays.stream((boolean[]) value)
          .mapToObj(String::valueOf)
          .collect(Collectors.joining());
      } else {
        return Arrays.stream((Object[]) value)
          .collect(Collectors.joining(","));
      }
    }

    return value.toString();
  }
}
