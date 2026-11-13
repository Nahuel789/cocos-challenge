package com.cocos.challenge.infrastructure.interfaces.rest.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PlaceOrderRequestValidator.class)
public @interface ValidPlaceOrder {

	String message() default "The order is invalid";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
