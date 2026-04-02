package CRM_Manara.CRM_Manara.dto;

import java.time.LocalDate;

public record EnfantSummaryDto(
        Long id,
        String nom,
        String prenom,
        LocalDate dateNaissance,
        boolean active
) {
}
