package CRM_Manara.CRM_Manara.Model.Entity.Service;

import CRM_Manara.CRM_Manara.Model.Entity.Enum.PresenceStatus;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.animationStatus;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.status;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.statusInscription;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.typeActivity;

public final class UiLabelService {

    private UiLabelService() {
    }

    public static String inscriptionStatus(statusInscription value) {
        if (value == null) {
            return "";
        }
        return switch (value) {
            case EN_ATTENTE -> "En attente";
            case APPROUVEE -> "Approuvée";
            case REFUSEE -> "Refusée";
            case ACTIF -> "Active";
            case ANNULÉE -> "Annulée";
        };
    }

    public static String presenceStatus(PresenceStatus value) {
        if (value == null) {
            return "";
        }
        return switch (value) {
            case PRESENT -> "Présent";
            case ABSENT -> "Absent";
            case NON_SIGNEE -> "Non signée";
        };
    }

    public static String activityStatus(status value) {
        if (value == null) {
            return "";
        }
        return switch (value) {
            case OUVERTE -> "Ouverte";
            case COMPLÈTE -> "Complète";
            case ANNULÉE -> "Annulée";
        };
    }

    public static String animationStatus(animationStatus value) {
        if (value == null) {
            return "";
        }
        return switch (value) {
            case ACTIF -> "Active";
            case ANNULÉ -> "Annulée";
            case REMPLACÉ -> "Remplacée";
        };
    }

    public static String activityType(typeActivity value) {
        if (value == null) {
            return "";
        }
        return switch (value) {
            case ART -> "Art";
            case LECTURE -> "Lecture";
            case MUSIQUE -> "Musique";
            case SPORT -> "Sport";
            case TUTORAT -> "Tutorat";
        };
    }
}
