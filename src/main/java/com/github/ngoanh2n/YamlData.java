package com.github.ngoanh2n;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.*;

/**
 * Yaml file to Map, Model, List of Models.<br>
 * Class of Model should be `public` and has `setter` methods
 *
 * @author Ho Huu Ngoan (ngoanh2n@gmail.com)
 * @version 1.0.0
 * @since 2021-01-16
 */
@SuppressWarnings("unchecked")
public abstract class YamlData<Model> {

    private String resourceName;
    private Class<Model> modelClazz;

    public YamlData() {
        modelClazz = getModelClazz();
    }

    /**
     * Read Yaml file as {@linkplain Map}
     *
     * @param fromResource is the name of resource <br>
     *                     e.g. com/foo/File.yml
     * @return {@linkplain Map} if the file exists; <br>
     * {@linkplain ResourceNotFound } otherwise; <br>
     * Throws {@linkplain RuntimeError} when trying to read multiple object as single object
     */
    public static Map<String, Object> toMap(String fromResource) {
        Object result = toObject(fromResource);
        if (result instanceof LinkedHashMap) {
            return (Map<String, Object>) result;
        } else {
            throw new RuntimeError("It's list, use toMaps() instead");
        }
    }

    /**
     * Read Yaml file as {@linkplain List} of {@linkplain Map}
     *
     * @param fromResource is the name of resource <br>
     *                     e.g. com/foo/File.yml
     * @return {@linkplain List} of {@linkplain Map} if the file exists;<br>
     * {@linkplain ResourceNotFound } otherwise; <br>
     * {@linkplain List} of {@linkplain Map} with one element when trying to single object as multiple object
     */
    public static List<Map<String, Object>> toMaps(String fromResource) {
        Object result = toObject(fromResource);
        if (result instanceof ArrayList) {
            return (List<Map<String, Object>>) result;
        } else {
            return Collections.singletonList((Map<String, Object>) result);
        }
    }

    private static Object toObject(String fromResource) {
        InputStream is = Resource.getInputStream(fromResource);
        Iterator<Object> iterator = new Yaml().loadAll(is).iterator();
        if (iterator.hasNext()) return iterator.next();
        throw new RuntimeError("Yaml content is empty");
    }

    /**
     * Set resource name to get the Yaml file
     *
     * @param resourceName is the name of resource <br>
     *                     e.g. com/foo/File.yml
     * @return the current instance of {@linkplain Model}
     */
    public Model fromResource(String resourceName) {
        this.resourceName = resourceName;
        return (Model) this;
    }

    /**
     * Read Yaml file as a {@linkplain Model}
     *
     * @return the current instance of {@linkplain Model}
     */
    public Model toModel() {
        InputStream is = getResource();
        return new Yaml().loadAs(is, modelClazz);
    }

    /**
     * Read Yaml file as a list of {@linkplain Model}s
     *
     * @return list of instances for {@linkplain Model}
     */
    public List<Model> toModels() {
        InputStream is = getResource();
        ObjectMapper om = new ObjectMapper();
        List<Model> models = new ArrayList<>();
        Iterable<Object> objects = new Yaml().loadAs(is, List.class);
        objects.forEach(object -> models.add(om.convertValue(object, modelClazz)));
        return models;
    }

    private InputStream getResource() {
        if (resourceName == null) {
            FromResource resourceAnn = getClass().getAnnotation(FromResource.class);
            resourceName = resourceAnn == null ? null : resourceAnn.value();
        }
        if (resourceName != null) {
            return Resource.getInputStream(resourceName);
        } else {
            throw new ResourceNotFound("Use @FromResource or call fromResource()");
        }
    }

    private Class<Model> getModelClazz() {
        if (modelClazz == null) {
            if (getClass().getName().equals(YamlData.class.getName())) {
                throw new RuntimeError("Could not read YamlData without model");
            } else {
                // Current model is generic
                Type superType = getClass().getGenericSuperclass();
                Type paramType = ((ParameterizedType) superType).getActualTypeArguments()[0];

                try {
                    if (paramType.toString().split(" ").length > 1) {
                        // Specific model
                        String modelName = paramType.toString().split(" ")[1];
                        modelClazz = (Class<Model>) Class.forName(modelName);
                    } else {
                        // Generic model
                        TypeVariable<?> varType = ((TypeVariable<?>) paramType);
                        String modelName = varType.getGenericDeclaration().toString().split(" ")[1];
                        modelClazz = (Class<Model>) Class.forName(modelName);
                    }
                } catch (ClassNotFoundException e) {
                    throw new RuntimeError("Could not find model class", e);
                }
            }
        }
        return modelClazz;
    }
}
