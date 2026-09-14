package com.schoolsaas.canteen.dto;

import com.schoolsaas.canteen.Menu;
import java.time.LocalDate;

public record MenuResponse(Long id, LocalDate date, String mainDescription, String specialDietDescription) {

    public static MenuResponse from(Menu menu) {
        return new MenuResponse(menu.getId(), menu.getDate(), menu.getMainDescription(), menu.getSpecialDietDescription());
    }
}
