package com.thoughtsandopinions.thoughtsservice.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class ApplicationContextHolder implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    /**
     * Statically fetch any bean from the Spring context by its class type.
     */
    public static <T> T getBean(Class<T> beanClass) {
        if (context == null) {
            throw new IllegalStateException("ApplicationContext has not been initialized yet.");
        }
        return context.getBean(beanClass);
    }
}
