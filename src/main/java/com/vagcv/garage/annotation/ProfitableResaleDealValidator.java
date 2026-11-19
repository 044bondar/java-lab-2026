package com.vagcv.garage.annotation;

import com.vagcv.garage.domain.VehicleStatus;
import com.vagcv.garage.dto.VehicleRequestDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ProfitableResaleDealValidator implements ConstraintValidator<ProfitableResaleDeal, VehicleRequestDto> {

    private BigDecimal minProfit;

    @Override
    public void initialize(ProfitableResaleDeal constraintAnnotation) {
        this.minProfit = BigDecimal.valueOf(constraintAnnotation.minProfit()).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public boolean isValid(VehicleRequestDto dto, ConstraintValidatorContext context) {
        if (dto == null || dto.getPurchasePrice() == null || dto.getTargetResalePrice() == null) {
            return true; // базові @NotNull спрацюють окремо
        }

        BigDecimal expectedProfit = dto.getTargetResalePrice().subtract(dto.getPurchasePrice());

        if (dto.getStatus() == VehicleStatus.SOLD && expectedProfit.compareTo(BigDecimal.ZERO) < 0) {
            buildCustomMessage(
                    context,
                    String.format(
                            "Продане авто (SOLD) не може бути збитковим: ціна продажу (%s) менша за ціну закупівлі (%s)",
                            dto.getTargetResalePrice(), dto.getPurchasePrice()
                    )
            );
            return false;
        }

        if (expectedProfit.compareTo(minProfit) < 0) {
            buildCustomMessage(
                    context,
                    String.format(
                            "Очікувана маржа перепродажу (%s) менша за мінімально допустимий поріг (%s). Збільште targetResalePrice мінімум до %s",
                            expectedProfit.setScale(2, RoundingMode.HALF_UP),
                            minProfit,
                            dto.getPurchasePrice().add(minProfit).setScale(2, RoundingMode.HALF_UP)
                    )
            );
            return false;
        }

        return true;
    }

    private void buildCustomMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode("targetResalePrice")
                .addConstraintViolation();
    }
}