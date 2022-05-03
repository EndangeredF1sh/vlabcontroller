package hk.edu.polyu.comp.vlabcontroller.converter;

import io.fabric8.kubernetes.api.model.Quantity;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
@ConfigurationPropertiesBinding
public class QuantityConverter implements Converter<String, Quantity> {

    @Override
    public Quantity convert(String from) {
        return new Quantity(from);
    }
}