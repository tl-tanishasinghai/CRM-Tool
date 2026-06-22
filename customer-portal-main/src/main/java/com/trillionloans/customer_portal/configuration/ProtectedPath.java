package com.trillionloans.customer_portal.configuration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation used to mark controller classes as "protected".
 *
 * <p>Endpoints annotated with {@code @ProtectedPath} will be intercepted by security filters (e.g.,
 * {@link com.trillionloans.customer_portal.configuration.filter.DynamicPathVariableFilter}) to
 * enforce path-based authorization logic.
 *
 * <p>This enables fine-grained access control where dynamic path variables (like leadId,
 * loanAccountNumber, etc.) are validated against the authenticated user's identity.
 *
 * <p>This annotation is picked up during startup by {@link ProtectedPathRegistry} to dynamically
 * register protected route patterns.
 *
 * @see com.trillionloans.customer_portal.configuration.ProtectedPathRegistry
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ProtectedPath {}
