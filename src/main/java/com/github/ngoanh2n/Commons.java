package com.github.ngoanh2n;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.mozilla.universalchardet.UniversalDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Common helpers.
 *
 * @author Ho Huu Ngoan (ngoanh2n@gmail.com)
 */
@SuppressWarnings({"unchecked", "ResultOfMethodCallIgnored"})
@CanIgnoreReturnValue
public final class Commons {
    private static final Logger logger = LoggerFactory.getLogger(Commons.class);

    private Commons() { /* No implementation necessary */ }

    //-------------------------------------------------------------------------------//

    /**
     * Creates a timestamp.
     *
     * @return timestamp as string.
     */
    public static String timestamp() {
        Format dateFormat = new SimpleDateFormat("yyyyMMdd.HHmmss.SSS");
        return dateFormat.format(new Date());
    }

    /**
     * Creates recursively directory from {@linkplain File}.
     *
     * @param file is directory as File.
     * @return directory as a file.
     */
    public static File createDir(@Nonnull File file) {
        return createDir(file.toPath()).toFile();
    }

    /**
     * Creates recursively directory from {@linkplain Path}.
     *
     * @param path is directory as Path.
     * @return directory as a path.
     */
    public static Path createDir(@Nonnull Path path) {
        Iterator<Path> elements = path.iterator();
        Path parentElement = Paths.get("");

        while (elements.hasNext()) {
            parentElement = parentElement.resolve(elements.next());
            parentElement.toFile().mkdirs();
        }
        return path;
    }

    /**
     * Gets relative path of file against to current user directory.
     *
     * @param file to get relative path.
     * @return relative path.
     */
    public static File getRelative(@Nonnull File file) {
        return getRelative(file.toPath()).toFile();
    }

    /**
     * Gets relative path of path against to current user directory.
     *
     * @param path to get relative path.
     * @return relative path.
     */
    public static Path getRelative(@Nonnull Path path) {
        String userDir = System.getProperty("user.dir");
        Path userPath = Paths.get(userDir);
        try {
            return userPath.relativize(path);
        } catch (IllegalArgumentException ignored) {
            return path;
        }
    }

    /**
     * Writes {@linkplain Properties} to file.
     *
     * @param file  to be stored.
     * @param props to be written.
     * @return output file.
     */
    public static File writeProps(Properties props, File file) {
        createDir(file.toPath());
        String msg = String.format("Write Properties to %s", getRelative(file));

        try (OutputStream os = Files.newOutputStream(file.toPath())) {
            props.store(os, null);
        } catch (IOException e) {
            logger.error(msg);
            throw new RuntimeError(msg, e);
        }
        logger.debug(msg);
        return file;
    }

    /**
     * Reads {@linkplain Properties} from given Java resource name.
     *
     * @param resourceName Java resource name to read.
     * @return {@linkplain Properties} object.
     */
    public static Properties readProps(@Nonnull String resourceName) {
        File file = Resource.getFile(resourceName);
        return readProps(file, "UTF-8");
    }

    /**
     * Reads {@linkplain Properties} from given properties file.
     *
     * @param file    to read.
     * @param charset The name of a supported charset.
     * @return {@linkplain Properties} object.
     */
    public static Properties readProps(@Nonnull File file, String charset) {
        Properties props = new Properties();
        String msg = String.format("Read Properties from %s", getRelative(file));

        try (InputStream is = Files.newInputStream(file.toPath())) {
            InputStreamReader isr = new InputStreamReader(is, charset);
            props.load(isr);
        } catch (IOException e) {
            logger.error(msg);
            throw new RuntimeError(msg, e);
        }
        logger.debug(msg);
        return props;
    }

    /**
     * Gets the charset of a file. <br>
     * Method to mark {@linkplain UniversalDetector} for reusing.
     *
     * @param file The file to check charset for.
     * @return The charset of the file, null when could not be determined.
     * @throws IOException if some IO error occurs.
     */
    public static String detectCharset(File file) throws IOException {
        return UniversalDetector.detectCharset(file.toPath());
    }

    /**
     * Reads value of the {@link Field}. Its parents will be considered. <br>
     * <ul>
     *     <li>{@code private Type aField}
     *     <li>{@code private final Type aField}
     * </ul>
     *
     * @param <T>    Type of result will be returned.
     * @param target The object instance to reflect, must not be {@code null}.
     * @param name   The field name to obtain.
     * @return The field value.
     */
    public static <T> T readField(Object target, String name) {
        String msg = msgFieldAccess(target.getClass(), name, "Read");
        Field[] fields = FieldUtils.getAllFields(target.getClass());

        for (Field field : fields) {
            if (field.getName().equals(name)) {
                field.setAccessible(true);
                try {
                    return (T) field.get(target);
                } catch (IllegalAccessException e) {
                    logger.error(msg);
                    throw new RuntimeError(msg, e);
                }
            }
        }
        logger.error(msg);
        throw new RuntimeError(msg);
    }

    /**
     * Reads value of the {@link Field}. Its parents will be considered. <br>
     * <ul>
     *     <li>{@code private static final Type aField}
     * </ul>
     *
     * @param <T>    Type of result will be returned.
     * @param target The object class to reflect, must not be {@code null}.
     * @param name   The field name to obtain.
     * @return The field value.
     */
    public static <T> T readField(Class<?> target, String name) {
        String msg = msgFieldAccess(target, name, "Read");
        Field[] fields = FieldUtils.getAllFields(target);

        for (Field field : fields) {
            if (field.getName().equals(name)) {
                field.setAccessible(true);
                try {
                    return (T) field.get(target);
                } catch (IllegalAccessException e) {
                    logger.error(msg);
                    throw new RuntimeError(msg, e);
                }
            }
        }
        logger.error(msg);
        throw new RuntimeError(msg);
    }

    /**
     * Writes value to the field with modifiers:
     * <ul>
     *     <li>Target object has fields:<pre>
     *         {@code private Type aField}
     *         {@code private final Type aField}</pre>
     *     <li>Target object's parents have fields:<pre>
     *         {@code private Type aField}
     *         {@code private final Type aField}
     *         {@code private static Type aField}
     *         {@code private static final Type aField}</pre>
     * </ul>
     *
     * @param target The object instance to reflect, must not be {@code null}.
     * @param name   The field name to obtain.
     * @param value  The new value for the field of object being modified.
     */
    public static void writeField(Object target, String name, Object value) {
        String msg = msgFieldAccess(target.getClass(), name, "Write");
        List<Field> fieldList = Arrays.stream(FieldUtils.getAllFields(target.getClass()))
                .filter(field -> field.getName().equals(name))
                .collect(Collectors.toList());

        for (Field field : fieldList) {
            field.setAccessible(true);
            try {
                if (!Modifier.isStatic(field.getModifiers())) {
                    Object fValue = field.get(target);
                    if (fValue.hashCode() != value.hashCode()) {
                        field.set(target, value);
                    }
                } else {
                    Field modifiers = FieldUtils.getField(Field.class, "modifiers", true);
                    modifiers.setAccessible(true);
                    modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
                    field.set(null, value);
                }
            } catch (IllegalAccessException e) {
                logger.error(msg);
                throw new RuntimeError(msg, e);
            }
        }
    }

    /**
     * Writes value to the field with modifiers:
     * <ul>
     *     <li>Target object has fields:<pre>
     *         {@code private static Type aField}
     *         {@code private static final Type aField}</pre>
     *     <li>Target object's parents have fields:<pre>
     *         {@code private static Type aField}
     *         {@code private static final Type aField}</pre>
     * </ul>
     *
     * @param target The object class to reflect, must not be {@code null}.
     * @param name   The field name to obtain.
     * @param value  The new value for the field of object being modified.
     */
    public static void writeField(Class<?> target, String name, Object value) {
        String msg = msgFieldAccess(target, name, "Write");
        List<Field> fields = Arrays.stream(FieldUtils.getAllFields(target))
                .filter(field -> field.getName().equals(name))
                .collect(Collectors.toList());

        for (Field field : fields) {
            field.setAccessible(true);
            try {
                if (!Modifier.isStatic(field.getModifiers())) {
                    boolean override = readField(field, "override");
                    if (!override) {
                        Object fValue = field.get(target);
                        if (fValue.hashCode() != value.hashCode()) {
                            field.set(target, value);
                        }
                    }
                } else {
                    Field modifiers = FieldUtils.getField(Field.class, "modifiers", true);
                    modifiers.setAccessible(true);
                    modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
                    field.set(null, value);
                }
            } catch (IllegalAccessException e) {
                logger.error(msg);
                throw new RuntimeError(msg, e);
            }
        }
    }

    //-------------------------------------------------------------------------------//

    private static String msgFieldAccess(Class<?> target, String name, String action) {
        return String.format("%s field %s.%s", action, target.getName(), name);
    }
}
