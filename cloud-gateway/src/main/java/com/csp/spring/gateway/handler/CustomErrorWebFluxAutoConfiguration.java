package com.csp.spring.gateway.handler;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.view.ViewResolver;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Custom ErrorWebFluxAutoConfiguration
 * {@link org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfiguration}
 *
 * @author chensiping
 * @since 2022-09-17
 */
@Configuration
@ConditionalOnWebApplication(
        type = ConditionalOnWebApplication.Type.REACTIVE
)
@ConditionalOnClass(WebFluxConfigurer.class)
@AutoConfigureBefore(WebFluxAutoConfiguration.class)
@EnableConfigurationProperties({ServerProperties.class, WebProperties.class})
public class CustomErrorWebFluxAutoConfiguration {
    private final ServerProperties serverProperties;
    private final ApplicationContext applicationContext;
    private final WebProperties webProperties;
    private final List<ViewResolver> viewResolvers;
    private final ServerCodecConfigurer serverCodecConfigurer;

    public CustomErrorWebFluxAutoConfiguration(ServerProperties serverProperties,
                                               WebProperties webProperties,
                                               ObjectProvider<ViewResolver> viewResolversProvider,
                                               ServerCodecConfigurer serverCodecConfigurer,
                                               ApplicationContext applicationContext) {
        this.serverProperties = serverProperties;
        this.applicationContext = applicationContext;
        this.webProperties = webProperties;
        this.viewResolvers = viewResolversProvider.orderedStream().collect(Collectors.toList());
        this.serverCodecConfigurer = serverCodecConfigurer;
    }

    /**
     * Gateway Exception Universal Processor
     */
    @Bean
    @ConditionalOnMissingBean(
            value = ErrorWebExceptionHandler.class,
            search = SearchStrategy.CURRENT
    )
    @Order(-2) // 优先级必须高于ErrorWebFluxAutoConfiguration（-1）
    public ErrorWebExceptionHandler errorWebExceptionHandler(ErrorAttributes errorAttributes) {
        // use custom ErrorWebExceptionHandler
        CustomErrorWebExceptionHandler exceptionHandler = new CustomErrorWebExceptionHandler(
                errorAttributes,
                this.webProperties.getResources(),
                this.serverProperties.getError(),
                this.applicationContext);
        exceptionHandler.setViewResolvers(this.viewResolvers);
        exceptionHandler.setMessageWriters(this.serverCodecConfigurer.getWriters());
        exceptionHandler.setMessageReaders(this.serverCodecConfigurer.getReaders());
        return exceptionHandler;
    }

    @Bean
    @ConditionalOnMissingBean(
            value = ErrorAttributes.class,
            search = SearchStrategy.CURRENT
    )
    public DefaultErrorAttributes errorAttributes() {
        return new DefaultErrorAttributes();
    }
}
