package ru.sbtqa.tag.api;

import cucumber.api.DataTable;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.sbtqa.tag.api.annotation.Body;
import ru.sbtqa.tag.api.annotation.Endpoint;
import ru.sbtqa.tag.api.annotation.FromResponse;
import ru.sbtqa.tag.api.annotation.Header;
import ru.sbtqa.tag.api.annotation.ParameterType;
import ru.sbtqa.tag.api.annotation.Query;
import ru.sbtqa.tag.api.annotation.Stashed;
import ru.sbtqa.tag.api.annotation.Validation;
import ru.sbtqa.tag.api.annotation.applicators.Applicator;
import ru.sbtqa.tag.api.annotation.applicators.ApplicatorHandler;
import ru.sbtqa.tag.api.annotation.applicators.FromResponseApplicator;
import ru.sbtqa.tag.api.annotation.applicators.StashedApplicator;
import ru.sbtqa.tag.api.exception.ApiEntryInitializationException;
import ru.sbtqa.tag.api.exception.ApiException;
import static ru.sbtqa.tag.api.utils.ReflectionUtils.get;
import static ru.sbtqa.tag.api.utils.ReflectionUtils.set;
import ru.sbtqa.tag.qautils.properties.Props;
import ru.sbtqa.tag.qautils.reflect.FieldUtilsExt;

public class ReflectionHelper {

    private static final Logger LOG = LoggerFactory.getLogger(ReflectionHelper.class);

    private ApiEntry apiEntry;
    private List<Field> fields;

    public ReflectionHelper(ApiEntry apiEntry) {
        this.apiEntry = apiEntry;
        this.fields = FieldUtilsExt.getDeclaredFieldsWithInheritance(apiEntry.getClass());
    }

    public void applyAnnotations() {
        for (Field field : fields) {
            field.setAccessible(true);

            ApplicatorHandler<Applicator> applicators = new ApplicatorHandler<>();
            for (Annotation annotation : field.getAnnotations()) {
                if (annotation instanceof FromResponse) {
                    applicators.add(new FromResponseApplicator(apiEntry, field));
                } else if (annotation instanceof Stashed) {
                    applicators.add(new StashedApplicator(apiEntry, field));
                } else {
                    continue;
                }
            }

            applicators.apply();
        }
    }

    public void setParameterValueByTitle(String name, String value) {
        for (Field field : fields) {
            for (Annotation annotation : field.getAnnotations()) {
                if (annotation instanceof Body && ((Body) annotation).name().equals(name)
                        || annotation instanceof Query && ((Query) annotation).name().equals(name)
                        || annotation instanceof Header && ((Header) annotation).name().equals(name)
                        && value != null && !value.isEmpty()) {
                    set(apiEntry, field, value);
                    return;
                }
            }
        }

        throw new ApiEntryInitializationException("There is no '" + name + "' parameter in '" + apiEntry.getClass().getAnnotation(Endpoint.class).title() + "' api entry.");
    }

    /**
     * Get request body. Get request body template from resources
     *
     * @throws ApiException if template file
     * doesn't exist or not available
     */
    public String getBody(String template) {
        String body;

        //get body template from resources

        // TODO читать по имени класса
        String templatePath = Props.get("api.template.path", "");
        String encoding = Props.get("api.encoding");
        String templateFullPath = templatePath + template;
        try {
            body = IOUtils.toString(getClass().getClassLoader().getResourceAsStream(templateFullPath), encoding).replace("\uFEFF", "");
        } catch (NullPointerException ex) {
            throw new ApiEntryInitializationException("Can't find template file by path " + templateFullPath, ex);
        } catch (IOException ex) {
            throw new ApiEntryInitializationException("Template file '" + templateFullPath + "' is not available", ex);
        }

        //replace %parameter on parameter value
        for (Map.Entry<String, Object> parameter : getParameters(ParameterType.BODY).entrySet()) {
            if (parameter.getValue() instanceof String) {
                String value = (null != parameter.getValue()) ? (String) parameter.getValue() : "";
                body = body.replaceAll("%" + parameter.getKey(), value);
            } else {
                LOG.debug("Failed to substitute not String field to body template");
            }
        }

        return body;
    }

    /**
     * Perform action validation rule
     *
     * @param title a {@link java.lang.String} object.
     * @param params a {@link java.lang.Object} object.
     * @throws ApiException if can't invoke
     * method
     */
    public void validate(String title, Object... params) {
        Method[] methods = apiEntry.getClass().getMethods();
        for (Method method : methods) {
            if (null != method.getAnnotation(Validation.class)
                    && method.getAnnotation(Validation.class).title().equals(title)) {
                try {
                    method.invoke(apiEntry, params);
                } catch (InvocationTargetException | IllegalAccessException | IllegalArgumentException e) {
                    throw new ApiException("Failed to invoke method", e);
                }
                return;
            }
        }
        throw new ApiEntryInitializationException("There is no '" + title + "' validation rule in '" + apiEntry.getClass().getAnnotation(Endpoint.class).title() + "' api entry.");
    }

    public Map<String, Object> getParameters(ParameterType type) {
        Map<String, Object> parameters = new HashMap<>();

        for (Field field : fields) {
            for (Annotation annotation : field.getAnnotations()) {

                switch (type) {
                    case QUERY:
                        if (annotation instanceof Query) {
                            parameters.put(((Query) annotation).name(), get(apiEntry, field));
                        }
                        break;
                    case HEADER:
                        if (annotation instanceof Header) {
                            parameters.put(((Header) annotation).name(), get(apiEntry, field));
                        }
                        break;
                    case BODY:
                        if (annotation instanceof Body) {
                            parameters.put(((Body) annotation).name(), get(apiEntry, field));
                        }
                        break;
                    default:
                        throw new ApiException("TODO");
                }
            }
        }

        return parameters;
    }

    public void applyDatatable(DataTable dataTable) {
        for (Map.Entry<String, String> dataTableRow : dataTable.asMap(String.class, String.class).entrySet()) {
            setParameterValueByTitle(dataTableRow.getKey(), dataTableRow.getValue());
        }
    }
}
