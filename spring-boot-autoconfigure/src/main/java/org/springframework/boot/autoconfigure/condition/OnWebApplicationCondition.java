/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.condition;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.StandardServletEnvironment;

/**
 * {@link Condition} that checks for the presence or absence of
 * {@link WebApplicationContext}.
 *
 * @author Dave Syer
 * @see ConditionalOnWebApplication
 * @see ConditionalOnNotWebApplication
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
class OnWebApplicationCondition extends SpringBootCondition {

	private static final String WEB_CONTEXT_CLASS = "org.springframework.web.context."
			+ "support.GenericWebApplicationContext";

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata) {
		boolean required = metadata
				.isAnnotated(ConditionalOnWebApplication.class.getName());
		ConditionOutcome outcome = isWebApplication(context, metadata, required);
		if (required && !outcome.isMatch()) { // 需要是web应用上下文，但是不匹配
			return ConditionOutcome.noMatch(outcome.getConditionMessage()); // 则返回不匹配
		}
		if (!required && outcome.isMatch()) { // 不需要是web应用上下文，但是匹配
			return ConditionOutcome.noMatch(outcome.getConditionMessage()); // 则返回不匹配
		}
		return ConditionOutcome.match(outcome.getConditionMessage()); // 否则返回匹配
	}

	private ConditionOutcome isWebApplication(ConditionContext context,
			AnnotatedTypeMetadata metadata, boolean required) {
		ConditionMessage.Builder message = ConditionMessage.forCondition(
				ConditionalOnWebApplication.class, required ? "(required)" : "");
		if (!ClassUtils.isPresent(WEB_CONTEXT_CLASS, context.getClassLoader())) {
			return ConditionOutcome // GenericWebApplicationContext不存在，条件不匹配
					.noMatch(message.didNotFind("web application classes").atAll());
		}
		if (context.getBeanFactory() != null) {
			String[] scopes = context.getBeanFactory().getRegisteredScopeNames();
			if (ObjectUtils.containsElement(scopes, "session")) { // 包含session scope，则匹配
				return ConditionOutcome.match(message.foundExactly("'session' scope"));
			}
		}
		if (context.getEnvironment() instanceof StandardServletEnvironment) { // 根据Environment类型判断
			return ConditionOutcome
					.match(message.foundExactly("StandardServletEnvironment"));
		}
		if (context.getResourceLoader() instanceof WebApplicationContext) {
			return ConditionOutcome.match(message.foundExactly("WebApplicationContext"));
		}
		return ConditionOutcome.noMatch(message.because("not a web application"));
	}

}
