package CRM_Manara.CRM_Manara.service;

import CRM_Manara.CRM_Manara.Model.Entity.Animateur;
import CRM_Manara.CRM_Manara.Model.Entity.Animation;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.statusInscription;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.typeActivity;
import CRM_Manara.CRM_Manara.Model.Entity.Parent;
import CRM_Manara.CRM_Manara.Model.Entity.SportPracticePlan;
import CRM_Manara.CRM_Manara.Model.Entity.SportPracticePlanItem;
import CRM_Manara.CRM_Manara.Repository.SportPracticePlanRepo;
import CRM_Manara.CRM_Manara.dto.SportPracticePlanCreateRequestDto;
import CRM_Manara.CRM_Manara.dto.SportPracticePlanDto;
import CRM_Manara.CRM_Manara.dto.SportPracticePlanItemDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SportPracticePlanService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SportPracticePlanService.class);

    private static final int MIN_NOTES_LENGTH = 20;
    private static final int MAX_ITEMS = 5;
    private static final Pattern WORD_PATTERN = Pattern.compile("\\p{L}[\\p{L}'-]{2,}");
    private static final Set<String> STOP_WORDS = Set.of(
            "avec", "dans", "pour", "pendant", "nous", "vous", "leur", "leurs", "cette",
            "seance", "sport", "enfant", "enfants", "activite", "travail", "faire", "fait",
            "plus", "moins", "comme", "sans", "etait", "etre", "avoir", "sont"
    );

    private final SportPracticePlanRepo sportPracticePlanRepo;
    private final AnimateurService animateurService;
    private final parentService parentService;
    private final AnthropicSportPracticePlanGenerationService anthropicSportPracticePlanGenerationService;

    public SportPracticePlanService(SportPracticePlanRepo sportPracticePlanRepo,
                                    AnimateurService animateurService,
                                    parentService parentService,
                                    AnthropicSportPracticePlanGenerationService anthropicSportPracticePlanGenerationService) {
        this.sportPracticePlanRepo = sportPracticePlanRepo;
        this.animateurService = animateurService;
        this.parentService = parentService;
        this.anthropicSportPracticePlanGenerationService = anthropicSportPracticePlanGenerationService;
    }

    @Transactional
    public SportPracticePlanDto createForAnimateur(String animateurEmail, SportPracticePlanCreateRequestDto request) {
        if (request == null || request.animationId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Une animation sportive est requise.");
        }
        if (request.sourceNotes() == null || request.sourceNotes().trim().length() < MIN_NOTES_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Les notes de seance doivent contenir au moins " + MIN_NOTES_LENGTH + " caracteres.");
        }

        Animateur animateur = animateurService.getAnimateurByEmail(animateurEmail);
        Animation animation = animateurService.getAnimationForAnimateur(request.animationId(), animateurEmail);
        if (!isSportAnimation(animation)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La pratique maison est reservee aux animations sportives.");
        }

        String notes = request.sourceNotes().trim();
        String title = cleanTitle(request.title(), animation);
        String activityName = animation.getActivity() == null ? null : animation.getActivity().getActivyName();

        AnthropicSportPracticePlanGenerationService.GeneratedPracticePlan generated =
                anthropicSportPracticePlanGenerationService.generatePlan(title, notes, activityName)
                        .orElseGet(() -> buildLocalPlan(title, notes, activityName));

        SportPracticePlan plan = new SportPracticePlan(
                animateur,
                animation,
                sanitizeText(generated.title(), 140),
                sanitizeText(generated.summary(), 600),
                notes
        );

        List<AnthropicSportPracticePlanGenerationService.GeneratedPracticePlanItem> items = generated.items().stream()
                .limit(MAX_ITEMS)
                .toList();
        for (int i = 0; i < items.size(); i++) {
            var item = items.get(i);
            plan.addItem(new SportPracticePlanItem(
                    sanitizeText(item.title(), 140),
                    sanitizeText(item.instructions(), 700),
                    sanitizeText(item.purpose(), 320),
                    sanitizeText(item.durationLabel(), 80),
                    sanitizeText(item.safetyTip(), 240),
                    i + 1
            ));
        }

        SportPracticePlan saved = sportPracticePlanRepo.save(plan);
        LOGGER.info("Pratique maison sportive creee. planId={}, animationId={}, animateurId={}, items={}",
                saved.getId(), animation.getId(), animateur.getId(), saved.getItems().size());
        return toDto(saved, true);
    }

    @Transactional(readOnly = true)
    public List<SportPracticePlanDto> listForAnimateur(String animateurEmail) {
        Long animateurId = animateurService.getAnimateurByEmail(animateurEmail).getId();
        return sportPracticePlanRepo.findByAnimateurIdOrderByCreatedAtDesc(animateurId).stream()
                .filter(plan -> isSportAnimation(plan.getAnimation()))
                .map(plan -> toDto(plan, true))
                .toList();
    }

    @Transactional(readOnly = true)
    public SportPracticePlanDto getForAnimateur(Long planId, String animateurEmail) {
        Long animateurId = animateurService.getAnimateurByEmail(animateurEmail).getId();
        SportPracticePlan plan = sportPracticePlanRepo.findByIdAndAnimateurId(planId, animateurId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pratique maison introuvable."));
        if (!isSportAnimation(plan.getAnimation())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pratique maison introuvable.");
        }
        return toDto(plan, true);
    }

    @Transactional(readOnly = true)
    public List<SportPracticePlanDto> listForParent(String parentEmail) {
        Parent parent = parentService.getParentByEmail(parentEmail);
        return sportPracticePlanRepo.findVisibleForParent(parent.getId(), visibleStatuses()).stream()
                .filter(plan -> isSportAnimation(plan.getAnimation()))
                .map(plan -> toDto(plan, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public SportPracticePlanDto getForParent(Long planId, String parentEmail) {
        Parent parent = parentService.getParentByEmail(parentEmail);
        SportPracticePlan plan = sportPracticePlanRepo.findVisibleDetailForParent(planId, parent.getId(), visibleStatuses())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pratique maison introuvable."));
        if (!isSportAnimation(plan.getAnimation())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pratique maison introuvable.");
        }
        return toDto(plan, false);
    }

    private List<statusInscription> visibleStatuses() {
        return List.of(statusInscription.EN_ATTENTE, statusInscription.APPROUVEE, statusInscription.ACTIF);
    }

    private boolean isSportAnimation(Animation animation) {
        return animation != null
                && animation.getActivity() != null
                && animation.getActivity().getType() == typeActivity.SPORT;
    }

    private String cleanTitle(String rawTitle, Animation animation) {
        if (rawTitle != null && !rawTitle.trim().isBlank()) {
            return rawTitle.trim();
        }
        String activityName = animation != null && animation.getActivity() != null
                ? animation.getActivity().getActivyName()
                : "Sport";
        return "Pratique maison - " + activityName;
    }

    private AnthropicSportPracticePlanGenerationService.GeneratedPracticePlan buildLocalPlan(String title,
                                                                                              String notes,
                                                                                              String activityName) {
        List<String> focuses = extractFocuses(notes);
        List<AnthropicSportPracticePlanGenerationService.GeneratedPracticePlanItem> items = new ArrayList<>();
        for (int i = 0; i < focuses.size() && items.size() < MAX_ITEMS; i++) {
            String focus = focuses.get(i);
            items.add(new AnthropicSportPracticePlanGenerationService.GeneratedPracticePlanItem(
                    "Atelier " + (i + 1) + " - " + focus,
                    "Refaites un petit bloc centre sur " + focus + ". Faites une demonstration lente, puis 6 a 10 repetitions calmes en alternant encouragement et correction simple.",
                    "Aider l'enfant a revoir " + focus + " sans pression et avec un repere clair.",
                    i % 2 == 0 ? "5 a 8 min" : "8 a 12 min",
                    "Arretez si le mouvement devient trop rapide ou mal controle."
            ));
        }
        if (items.isEmpty()) {
            items = List.of(
                    new AnthropicSportPracticePlanGenerationService.GeneratedPracticePlanItem(
                            "Echauffement ludique",
                            "Faites 2 a 3 minutes de deplacements faciles, petits sauts ou conduite de balle tres lente selon la seance.",
                            "Remettre le corps en action avant les gestes de la seance.",
                            "5 min",
                            "Verifier l'espace autour de l'enfant."
                    ),
                    new AnthropicSportPracticePlanGenerationService.GeneratedPracticePlanItem(
                            "Geste technique revise",
                            "Reprenez le geste vu en seance avec une consigne a la fois, puis faites une courte serie de repetitions.",
                            "Renforcer la memoire du geste et la qualite d'execution.",
                            "8 min",
                            "Prioriser la qualite plutot que la vitesse."
                    ),
                    new AnthropicSportPracticePlanGenerationService.GeneratedPracticePlanItem(
                            "Mini-defi parent-enfant",
                            "Transformez le travail en petit defi simple: nombre de passes, cible touchee, ou parcours sans erreur.",
                            "Garder la motivation et consolider l'autonomie.",
                            "5 a 10 min",
                            ""
                    )
            );
        }

        String summary = "Cette fiche reprend les points vus en seance pour une pratique simple a la maison ou au parc, avec consignes courtes et realistes pour le parent.";
        return new AnthropicSportPracticePlanGenerationService.GeneratedPracticePlan(
                title,
                summary,
                items
        );
    }

    private List<String> extractFocuses(String notes) {
        LinkedHashSet<String> focuses = new LinkedHashSet<>();
        for (String sentence : notes.split("(?<=[.!?])\\s+|\\R+")) {
            String trimmed = sentence.trim();
            if (trimmed.length() < 12) {
                continue;
            }
            String focus = titleFromSentence(trimmed);
            if (!focus.isBlank()) {
                focuses.add(focus);
            }
            if (focuses.size() >= MAX_ITEMS) {
                break;
            }
        }
        return new ArrayList<>(focuses);
    }

    private String titleFromSentence(String sentence) {
        List<String> words = new ArrayList<>();
        Matcher matcher = WORD_PATTERN.matcher(sentence.toLowerCase(Locale.ROOT));
        while (matcher.find() && words.size() < 4) {
            String word = stripAccents(matcher.group()).replace("'", "").replace("-", "");
            if (word.length() >= 4 && !STOP_WORDS.contains(word)) {
                words.add(capitalize(word));
            }
        }
        return words.isEmpty() ? "" : String.join(" ", words);
    }

    private SportPracticePlanDto toDto(SportPracticePlan plan, boolean includeSourceNotes) {
        return new SportPracticePlanDto(
                plan.getId(),
                plan.getAnimation() == null ? null : plan.getAnimation().getId(),
                plan.getAnimation() != null && plan.getAnimation().getActivity() != null
                        ? plan.getAnimation().getActivity().getActivyName()
                        : null,
                plan.getTitle(),
                plan.getSummary(),
                includeSourceNotes ? plan.getSourceNotes() : null,
                plan.getCreatedAt(),
                plan.getItems().stream()
                        .sorted(Comparator.comparingInt(SportPracticePlanItem::getPosition))
                        .map(item -> new SportPracticePlanItemDto(
                                item.getId(),
                                item.getTitle(),
                                item.getInstructions(),
                                item.getPurpose(),
                                item.getDurationLabel(),
                                item.getSafetyTip(),
                                item.getPosition()
                        ))
                        .toList()
        );
    }

    private String sanitizeText(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String sanitized = value
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace('\t', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        if (sanitized.length() > maxLength) {
            sanitized = sanitized.substring(0, maxLength).trim();
        }
        return sanitized;
    }

    private String stripAccents(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }

    private String capitalize(String value) {
        String cleaned = value == null ? "" : value.trim();
        if (cleaned.isEmpty()) {
            return cleaned;
        }
        return cleaned.substring(0, 1).toUpperCase(Locale.ROOT) + cleaned.substring(1);
    }
}
