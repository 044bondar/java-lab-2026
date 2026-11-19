package com.vagcv.garage.dto;

import com.vagcv.garage.domain.VehicleStatus;
import jakarta.validation.constraints.*;
import lombok.*;
import com.vagcv.garage.annotation.ProfitableResaleDeal;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ProfitableResaleDeal(minProfit = 200.00)
public class VehicleRequestDto {

    @NotBlank(message = "VIN-код обов'язковий")
    @Size(min = 17, max = 17, message = "VIN-код має складатися рівно з 17 символів")
    private String vin;

    @NotBlank(message = "Марка автомобіля обов'язкова")
    private String brand;

    @NotBlank(message = "Модель автомобіля обов'язкова")
    private String model;

    @NotNull(message = "Рік випуску обов'язковий")
    @Min(value = 1970, message = "Рік випуску не може бути меншим за 1970")
    @Max(value = 2027, message = "Некоректний рік випуску")
    private Integer manufactureYear;

    @NotNull(message = "Пробіг обов'язковий")
    @Min(value = 0, message = "Пробіг не може бути від'ємним")
    private Integer mileageKm;

    @NotNull(message = "Ціна закупівлі обов'язкова")
    @DecimalMin(value = "100.00", message = "Мінімальна ціна закупівлі 100.00")
    private BigDecimal purchasePrice;

    @NotNull(message = "Цільова ціна продажу обов'язкова")
    @DecimalMin(value = "110.00", message = "Мінімальна ціна продажу 110.00")
    private BigDecimal targetResalePrice;

    @NotNull(message = "Статус авто обов'язковий")
    private VehicleStatus status;
}
