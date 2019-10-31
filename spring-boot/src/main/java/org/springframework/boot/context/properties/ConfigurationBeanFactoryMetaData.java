/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.properties;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodCallback;

/**
 * Utility class to memorize {@code @Bean} definition meta data during initialization of
 * the bean factory.
 *
 * @author Dave Syer
 * @since 1.1.0
 */
public class ConfigurationBeanFactoryMetaData implements BeanFactoryPostProcessor {

	private ConfigurableListableBeanFactory beanFactory;

	private Map<String, FactoryMetaData> beansFactoryMetadata = new HashMap<String, FactoryMetaData>();

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
			throws BeansException {
		this.beanFactory = beanFactory;
		for (String name : beanFactory.getBeanDefinitionNames()) {
			BeanDefinition definition = beanFactory.getBeanDefinition(name);
			String method = definition.getFactoryMethodName(); // 工厂方法，如@Bean注解方法名
			String bean = definition.getFactoryBeanName(); // 工厂bean，如@Configuration配置类
			if (method != null && bean != null) {
				this.beansFactoryMetadata.put(name, new FactoryMetaData(bean, method));
			}
		}
	}

	public <A extends Annotation> Map<String, Object> getBeansWithFactoryAnnotation(
			Class<A> type) {
		Map<String, Object> result = new HashMap<String, Object>();
		for (String name : this.beansFactoryMetadata.keySet()) {
			if (findFactoryAnnotation(name, type) != null) {
				result.put(name, this.beanFactory.getBean(name));
			}
		}
		return result;
	}

	public <A extends Annotation> A findFactoryAnnotation(String beanName, // beanName表示由工厂生成的bean name
			Class<A> type) {
		Method method = findFactoryMethod(beanName);
		return (method != null) ? AnnotationUtils.findAnnotation(method, type) : null; // 在该@Bean注解工厂方法上查找指定注解
	}

	private Method findFactoryMethod(String beanName) {
		if (!this.beansFactoryMetadata.containsKey(beanName)) {
			return null;
		}
		final AtomicReference<Method> found = new AtomicReference<Method>(null);
		FactoryMetaData factoryMetaData = this.beansFactoryMetadata.get(beanName);
		final String factory = factoryMetaData.getMethod();
		Class<?> type = this.beanFactory.getType(factoryMetaData.getBean()); // 工厂bean类型
		ReflectionUtils.doWithMethods(type, new MethodCallback() {
			@Override
			public void doWith(Method method)
					throws IllegalArgumentException, IllegalAccessException {
				if (method.getName().equals(factory)) { // 判断工厂方法名是否相等
					found.compareAndSet(null, method);
				}
			}
		});
		return found.get();
	}

	private static class FactoryMetaData {

		private String bean; // 工厂bean

		private String method; // 工厂方法

		FactoryMetaData(String bean, String method) {
			this.bean = bean;
			this.method = method;
		}

		public String getBean() {
			return this.bean;
		}

		public String getMethod() {
			return this.method;
		}

	}

}
