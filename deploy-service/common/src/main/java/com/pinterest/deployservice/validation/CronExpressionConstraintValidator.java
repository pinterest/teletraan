package com.pinterest.deployservice.validation;


import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.quartz.CronExpression;

public class CronExpressionConstraintValidator implements ConstraintValidator<CronExpressionConstraint, String>{

    @Override
    public void initialize(CronExpressionConstraint constraintAnnotation) {
    }

    @Override
    public boolean isValid(String schedule, ConstraintValidatorContext context) {
        if (schedule == null) {
            return true;
        }
        try {
            new CronExpression(schedule);
            return true;
        } catch (Exception e) {

            return false;
        }
    }

}
