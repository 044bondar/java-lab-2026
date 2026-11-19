package com.vagcv.garage.annotation;

import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class CurrentOperatorArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentOperator.class)
                && String.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {

        CurrentOperator annotation = parameter.getParameterAnnotation(CurrentOperator.class);
        String fallback = (annotation != null) ? annotation.fallback() : "api_user";

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            String name = auth.getName();
            if (name != null && !name.isBlank()) {
                return name;
            }
        }

        String headerOperator = webRequest.getHeader("X-Operator");
        if (headerOperator != null && !headerOperator.isBlank()) {
            return headerOperator.trim();
        }

        return fallback;
    }
}