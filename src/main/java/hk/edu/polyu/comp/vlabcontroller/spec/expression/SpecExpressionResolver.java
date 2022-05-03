package hk.edu.polyu.comp.vlabcontroller.spec.expression;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.expression.*;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeConverter;
import org.springframework.expression.spel.support.StandardTypeLocator;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Note: inspired by org.springframework.context.expression.StandardBeanExpressionResolver
 */
@Component
@RequiredArgsConstructor
public class SpecExpressionResolver {

    private final Map<SpecExpressionContext, StandardEvaluationContext> evaluationCache = new ConcurrentHashMap<>(8);
    private final ParserContext beanExpressionParserContext = new ParserContext() {
        @Override
        public boolean isTemplate() {
            return true;
        }

        @Override
        public String getExpressionPrefix() {
            return StandardBeanExpressionResolver.DEFAULT_EXPRESSION_PREFIX;
        }

        @Override
        public String getExpressionSuffix() {
            return StandardBeanExpressionResolver.DEFAULT_EXPRESSION_SUFFIX;
        }
    };
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final ApplicationContext appContext;

    public Object evaluate(String expression, SpecExpressionContext context) {
        if (expression == null) return null;
        if (expression.isEmpty()) return "";

        var expr = this.expressionParser.parseExpression(expression, this.beanExpressionParserContext);

        ConfigurableBeanFactory beanFactory = ((ConfigurableApplicationContext) appContext).getBeanFactory();

        var sec = evaluationCache.get(context);
        if (sec == null) {
            sec = new StandardEvaluationContext() {{
                setRootObject(context);
                addPropertyAccessor(new BeanExpressionContextAccessor());
                addPropertyAccessor(new BeanFactoryAccessor());
                addPropertyAccessor(new MapAccessor());
                addPropertyAccessor(new EnvironmentAccessor());
                setBeanResolver(new BeanFactoryResolver(appContext));
                setTypeLocator(new StandardTypeLocator(beanFactory.getBeanClassLoader()));
            }};
            var conversionService = beanFactory.getConversionService();
            if (conversionService != null) sec.setTypeConverter(new StandardTypeConverter(conversionService));
            evaluationCache.put(context, sec);
        }

        return expr.getValue(sec);
    }

    public String evaluateToString(String expression, SpecExpressionContext context) {
        return String.valueOf(evaluate(expression, context));
    }
}
