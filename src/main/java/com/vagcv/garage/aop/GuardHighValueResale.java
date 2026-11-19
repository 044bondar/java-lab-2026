package com.vagcv.garage.aop;

import java.lang.annotation.*;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GuardHighValueResale {

    double threshold() default 10000.00;
}