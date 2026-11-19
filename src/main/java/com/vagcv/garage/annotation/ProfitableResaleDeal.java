package com.vagcv.garage.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ProfitableResaleDealValidator.class)
public @interface ProfitableResaleDeal {

    String message() default "Невигідна або збиткова угода перепродажу авто";

    double minProfit() default 200.00;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}