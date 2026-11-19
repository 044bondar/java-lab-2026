package com.vagcv.garage.aop;

import java.lang.annotation.*;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TrackExecutionSla {

    String operationName() default "";

    long maxMillis() default 200L;
}