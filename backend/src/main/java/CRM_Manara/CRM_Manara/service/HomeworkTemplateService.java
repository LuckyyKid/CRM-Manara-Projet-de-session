package CRM_Manara.CRM_Manara.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class HomeworkTemplateService {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("(?<![A-Za-z])(\\d+)");
    private static final Pattern EXPONENT_PATTERN = Pattern.compile("\\^(\\d+)|([²³])");
    private static final List<String> MATH_ACTIVITY_HINTS = List.of(
            "math", "mathem", "algeb", "geometr", "trigono", "calcul", "statist", "arithm", "fonction"
    );
    private static final List<String> MATH_AXIS_HINTS = List.of(
            "derive", "equation", "fraction", "factor", "develop", "signe", "croissance", "variation", "polyn", "racine"
    );

    public AnthropicHomeworkGenerationService.GeneratedHomework buildDraft(String activityName,
                                                                           List<HomeworkService.AxisNeed> axisNeeds,
                                                                           boolean reviewMode) {
        List<AnthropicHomeworkGenerationService.GeneratedExercise> exercises = new ArrayList<>();
        int position = 1;
        for (HomeworkService.AxisNeed axisNeed : axisNeeds) {
            TemplatePlan plan = selectPlan(activityName, axisNeed, reviewMode);
            for (TemplateVariant variant : plan.variants()) {
                exercises.add(renderExercise(activityName, axisNeed, variant, position++));
            }
        }

        String title = reviewMode ? "Devoir de revision ciblee" : "Devoir de remediation ciblee";
        String summary = reviewMode
                ? "Revision guidee sur les notions deja vues pour consolider la memorisation."
                : "Exercices structures sur les axes faibles detectes apres le quiz.";
        return new AnthropicHomeworkGenerationService.GeneratedHomework(title, summary, exercises);
    }

    private TemplatePlan selectPlan(String activityName, HomeworkService.AxisNeed axisNeed, boolean reviewMode) {
        if (!isMathContext(activityName, axisNeed.axisTitle())) {
            return genericDomainPlan(activityName, reviewMode, axisNeed.exerciseCount());
        }

        String normalizedAxis = normalize(axisNeed.axisTitle());
        if (normalizedAxis.contains("derive")) {
            return reviewMode ? derivativeReviewPlan() : derivativeRemediationPlan(axisNeed.exerciseCount());
        }
        if (normalizedAxis.contains("factor")) {
            return factorizationPlan(reviewMode, axisNeed.exerciseCount());
        }
        if (normalizedAxis.contains("fraction") || normalizedAxis.contains("denominat")) {
            return fractionPlan(reviewMode, axisNeed.exerciseCount());
        }
        if (normalizedAxis.contains("develop")) {
            return expansionPlan(reviewMode, axisNeed.exerciseCount());
        }
        if (normalizedAxis.contains("signe") || normalizedAxis.contains("croissance") || normalizedAxis.contains("variation")) {
            return variationPlan(reviewMode, axisNeed.exerciseCount());
        }
        if (normalizedAxis.contains("equation")) {
            return equationPlan(reviewMode, axisNeed.exerciseCount());
        }
        return genericPlan(reviewMode, axisNeed.exerciseCount());
    }

    private TemplatePlan genericDomainPlan(String activityName, boolean reviewMode, int requestedCount) {
        List<TemplateVariant> variants = new ArrayList<>();
        String normalizedActivity = normalize(activityName);

        if (containsAny(normalizedActivity, "francais", "grammaire", "orthographe", "redaction", "lecture")) {
            variants.add(new TemplateVariant("FACILE", "language-understand"));
            variants.add(new TemplateVariant("MOYEN", "language-correct"));
            if (!reviewMode && requestedCount >= 3) {
                variants.add(new TemplateVariant("DIFFICILE", "language-produce"));
            }
            return new TemplatePlan(variants);
        }
        if (containsAny(normalizedActivity, "arabe", "arab")) {
            variants.add(new TemplateVariant("FACILE", "language-understand"));
            variants.add(new TemplateVariant("MOYEN", "language-apply"));
            if (!reviewMode && requestedCount >= 3) {
                variants.add(new TemplateVariant("DIFFICILE", "language-produce"));
            }
            return new TemplatePlan(variants);
        }
        if (containsAny(normalizedActivity, "histoire", "politique", "geographie", "geo", "economie", "finance", "citoyen")) {
            variants.add(new TemplateVariant("FACILE", "humanities-recall"));
            variants.add(new TemplateVariant("MOYEN", "humanities-explain"));
            if (!reviewMode && requestedCount >= 3) {
                variants.add(new TemplateVariant("DIFFICILE", "humanities-compare"));
            }
            return new TemplatePlan(variants);
        }
        if (containsAny(normalizedActivity, "musique", "cinema", "art", "theatre")) {
            variants.add(new TemplateVariant("FACILE", "arts-identify"));
            variants.add(new TemplateVariant("MOYEN", "arts-analyze"));
            if (!reviewMode && requestedCount >= 3) {
                variants.add(new TemplateVariant("DIFFICILE", "arts-justify"));
            }
            return new TemplatePlan(variants);
        }
        return genericPlan(reviewMode, requestedCount);
    }

    private TemplatePlan derivativeRemediationPlan(int requestedCount) {
        List<TemplateVariant> variants = new ArrayList<>();
        variants.add(new TemplateVariant("FACILE", "direct"));
        variants.add(new TemplateVariant("MOYEN", "sign"));
        if (requestedCount >= 3) {
            variants.add(new TemplateVariant("DIFFICILE", "transfer"));
        }
        return new TemplatePlan(variants);
    }

    private TemplatePlan derivativeReviewPlan() {
        return new TemplatePlan(List.of(
                new TemplateVariant("FACILE", "direct"),
                new TemplateVariant("MOYEN", "compare")
        ));
    }

    private TemplatePlan variationPlan(boolean reviewMode, int requestedCount) {
        List<TemplateVariant> variants = new ArrayList<>();
        variants.add(new TemplateVariant("FACILE", "recognize"));
        variants.add(new TemplateVariant("MOYEN", reviewMode ? "justify" : "apply"));
        if (!reviewMode && requestedCount >= 3) {
            variants.add(new TemplateVariant("DIFFICILE", "transfer"));
        }
        return new TemplatePlan(variants);
    }

    private TemplatePlan equationPlan(boolean reviewMode, int requestedCount) {
        List<TemplateVariant> variants = new ArrayList<>();
        variants.add(new TemplateVariant("FACILE", "solve"));
        variants.add(new TemplateVariant("MOYEN", "trap"));
        if (!reviewMode && requestedCount >= 3) {
            variants.add(new TemplateVariant("DIFFICILE", "justify"));
        }
        return new TemplatePlan(variants);
    }

    private TemplatePlan factorizationPlan(boolean reviewMode, int requestedCount) {
        List<TemplateVariant> variants = new ArrayList<>();
        variants.add(new TemplateVariant("FACILE", "factor-common"));
        variants.add(new TemplateVariant("MOYEN", "factor-sign"));
        if (!reviewMode && requestedCount >= 3) {
            variants.add(new TemplateVariant("DIFFICILE", "factor-compare"));
        }
        return new TemplatePlan(variants);
    }

    private TemplatePlan fractionPlan(boolean reviewMode, int requestedCount) {
        List<TemplateVariant> variants = new ArrayList<>();
        variants.add(new TemplateVariant("FACILE", "fraction-simplify"));
        variants.add(new TemplateVariant("MOYEN", "fraction-add"));
        if (!reviewMode && requestedCount >= 3) {
            variants.add(new TemplateVariant("DIFFICILE", "fraction-trap"));
        }
        return new TemplatePlan(variants);
    }

    private TemplatePlan expansionPlan(boolean reviewMode, int requestedCount) {
        List<TemplateVariant> variants = new ArrayList<>();
        variants.add(new TemplateVariant("FACILE", "expand-simple"));
        variants.add(new TemplateVariant("MOYEN", "expand-sign"));
        if (!reviewMode && requestedCount >= 3) {
            variants.add(new TemplateVariant("DIFFICILE", "expand-compare"));
        }
        return new TemplatePlan(variants);
    }

    private TemplatePlan genericPlan(boolean reviewMode, int requestedCount) {
        List<TemplateVariant> variants = new ArrayList<>();
        variants.add(new TemplateVariant("FACILE", "recognize"));
        variants.add(new TemplateVariant("MOYEN", "apply"));
        if (!reviewMode && requestedCount >= 3) {
            variants.add(new TemplateVariant("DIFFICILE", "transfer"));
        }
        return new TemplatePlan(variants);
    }

    private AnthropicHomeworkGenerationService.GeneratedExercise renderExercise(String activityName,
                                                                                HomeworkService.AxisNeed axisNeed,
                                                                                TemplateVariant variant,
                                                                                int position) {
        Map<String, String> values = inferValues(axisNeed, position);
        String axisTitle = axisNeed.axisTitle();
        String targetMistake = axisNeed.mistakes().isEmpty() ? "" : axisNeed.mistakes().get(0);
        String questionType = resolveQuestionType(variant.kind());
        String questionText = switch (variant.kind()) {
            case "direct" -> "Calcule la derivee de f(x) = " + values.get("term") + ".";
            case "sign" -> "Calcule la derivee de g(x) = " + values.get("signedTerm")
                    + " puis explique comment verifier le signe du resultat.";
            case "transfer" -> "Calcule la derivee de h(x) = " + values.get("term")
                    + " + " + values.get("constant") + " et justifie la place de la constante.";
            case "compare" -> "Quelle proposition donne la bonne derivee de p(x) = " + values.get("term")
                    + " ? Explique en une phrase pourquoi.";
            case "factor-common" -> "Factorise l'expression " + values.get("factorExpression") + ".";
            case "factor-sign" -> "Factorise " + values.get("signedFactorExpression")
                    + " puis explique comment ne pas perdre le signe.";
            case "factor-compare" -> "Quelle ecriture est la bonne forme factorisee de "
                    + values.get("factorExpression") + " ? Justifie ton choix.";
            case "fraction-simplify" -> "Simplifie la fraction " + values.get("fraction") + ".";
            case "fraction-add" -> "Calcule " + values.get("fractionSum")
                    + " en mettant au meme denominateur.";
            case "fraction-trap" -> "Explique pourquoi on ne peut pas simplifier "
                    + values.get("fractionTrap") + " en supprimant directement les x.";
            case "expand-simple" -> "Developpe puis reduis " + values.get("expandExpression") + ".";
            case "expand-sign" -> "Developpe " + values.get("expandNegative")
                    + " en faisant attention au signe.";
            case "expand-compare" -> "Choisis la bonne forme developpee de " + values.get("expandExpression")
                    + " et justifie brievement.";
            case "language-understand" -> "Dans l'axe \"" + axisTitle + "\", repere la regle, l'idee ou l'indice principal a retenir.";
            case "language-correct" -> "Corrige ou reformule un exemple sur \"" + axisTitle + "\" puis explique brievement ton choix.";
            case "language-apply" -> "Applique la regle de \"" + axisTitle + "\" sur un exemple proche de celui du cours.";
            case "language-produce" -> "Redige une reponse courte sur \"" + axisTitle + "\" en respectant la regle attendue.";
            case "humanities-recall" -> "Donne l'information essentielle a retenir sur \"" + axisTitle + "\" dans l'activite \"" + blank(activityName, "cours") + "\".";
            case "humanities-explain" -> "Explique clairement \"" + axisTitle + "\" avec une cause, une consequence ou une idee centrale.";
            case "humanities-compare" -> "Compare deux situations, notions ou positions liees a \"" + axisTitle + "\" et justifie ton raisonnement.";
            case "arts-identify" -> "Identifie l'element principal a observer ou ecouter dans \"" + axisTitle + "\".";
            case "arts-analyze" -> "Analyse l'effet, l'intention ou le choix artistique lie a \"" + axisTitle + "\".";
            case "arts-justify" -> "Justifie ton interpretation de \"" + axisTitle + "\" avec un vocabulaire precis.";
            case "recognize" -> "Dans l'axe \"" + axisTitle + "\", identifie la regle ou l'indice principal a utiliser avant de calculer.";
            case "apply" -> "Applique la methode de \"" + axisTitle + "\" a un exemple proche de celui du quiz et explique la premiere etape.";
            case "justify" -> "Justifie clairement la methode correcte pour \"" + axisTitle + "\" en evitant l'erreur frequente observee.";
            case "solve" -> "Resous l'equation " + values.get("equation") + ".";
            case "trap" -> "Resous " + values.get("equation")
                    + " puis indique a quelle etape un eleve peut facilement changer le signe par erreur.";
            default -> "Resous un exercice sur \"" + axisTitle + "\" et explique brievement ta demarche.";
        };

        String expectedAnswer = switch (variant.kind()) {
            case "direct" -> "La derivee attendue est " + values.get("derivative") + ".";
            case "sign" -> "La derivee attendue est " + values.get("signedDerivative")
                    + ". Il faut verifier le signe du coefficient et diminuer l'exposant de 1.";
            case "transfer" -> "La derivee attendue est " + values.get("derivative")
                    + ", car la derivee d'une constante vaut 0.";
            case "compare" -> "La bonne derivee est " + values.get("derivative")
                    + " parce qu'on multiplie par l'exposant puis on diminue l'exposant de 1.";
            case "factor-common" -> "La forme attendue est " + values.get("factorizedExpression") + ".";
            case "factor-sign" -> "La forme attendue est " + values.get("signedFactorizedExpression")
                    + " et il faut verifier le signe de chaque terme.";
            case "factor-compare" -> "La bonne factorisation est " + values.get("factorizedExpression")
                    + " car le facteur commun doit retrouver chaque terme.";
            case "fraction-simplify" -> "La fraction simplifiee attendue est " + values.get("fractionSimplified") + ".";
            case "fraction-add" -> "Le resultat attendu est " + values.get("fractionSumResult") + ".";
            case "fraction-trap" -> "On ne peut pas simplifier a travers une addition. Il faut d'abord factoriser ou calculer numerateur et denominateur correctement.";
            case "expand-simple" -> "La forme developpee attendue est " + values.get("expandedExpression") + ".";
            case "expand-sign" -> "La forme developpee attendue est " + values.get("expandedNegative")
                    + " en distribuant aussi le signe negatif.";
            case "expand-compare" -> "La bonne forme developpee est " + values.get("expandedExpression")
                    + " apres distribution sur chaque terme.";
            case "language-understand" -> "La reponse doit identifier correctement la regle, l'idee ou l'indice principal de l'axe \"" + axisTitle + "\".";
            case "language-correct" -> "La reponse doit corriger ou reformuler correctement et expliquer le choix en une phrase.";
            case "language-apply" -> "La reponse doit appliquer correctement la regle de l'axe \"" + axisTitle + "\" sur un nouvel exemple.";
            case "language-produce" -> "La reponse doit produire un court texte correct et coherent sur l'axe \"" + axisTitle + "\".";
            case "humanities-recall" -> "La reponse doit citer l'information essentielle a retenir sur \"" + axisTitle + "\".";
            case "humanities-explain" -> "La reponse doit expliquer clairement le concept et son lien principal avec le cours.";
            case "humanities-compare" -> "La reponse doit comparer deux elements de facon justifiee et structuree.";
            case "arts-identify" -> "La reponse doit identifier l'element artistique principal avec le bon vocabulaire.";
            case "arts-analyze" -> "La reponse doit analyser l'effet ou l'intention artistique de facon claire.";
            case "arts-justify" -> "La reponse doit justifier une interpretation avec au moins un argument pertinent.";
            case "recognize" -> "La reponse doit nommer la notion cle de l'axe \"" + axisTitle + "\" et l'indice utile avant calcul.";
            case "apply" -> "La reponse doit appliquer correctement l'axe \"" + axisTitle + "\" a un exemple proche du quiz.";
            case "justify" -> "La reponse doit expliquer la methode correcte pour \"" + axisTitle
                    + "\" et mentionner comment eviter l'erreur frequente.";
            case "solve" -> "La solution attendue est x = " + values.get("equationSolution") + ".";
            case "trap" -> "La solution attendue est x = " + values.get("equationSolution")
                    + " et l'erreur frequente est de mal changer le signe lors du passage de terme.";
            default -> "La reponse doit traiter correctement l'axe \"" + axisTitle + "\" avec une justification breve.";
        };

        List<String> options = questionType.equals("CHOICE")
                ? buildChoiceOptions(variant.kind(), values)
                : List.of();

        return new AnthropicHomeworkGenerationService.GeneratedExercise(
                axisTitle,
                questionType,
                variant.difficulty(),
                questionText,
                expectedAnswer,
                targetMistake,
                options
        );
    }

    private Map<String, String> inferValues(HomeworkService.AxisNeed axisNeed, int seed) {
        Map<String, String> values = new LinkedHashMap<>();
        int coefficient = 2 + (seed % 5);
        int exponent = 2 + (seed % 3);
        int constant = 3 + seed;
        String signPrefix = seed % 2 == 0 ? "-" : "";
        String term = coefficient + "x^" + exponent;
        String signedTerm = signPrefix + coefficient + "x^" + exponent;
        values.put("term", term);
        values.put("signedTerm", signedTerm);
        values.put("derivative", (coefficient * exponent) + "x^" + (exponent - 1));
        values.put("signedDerivative", (signPrefix.isBlank() ? "" : "-") + (coefficient * exponent) + "x^" + (exponent - 1));
        values.put("constant", String.valueOf(constant));
        int left = coefficient + exponent;
        int right = constant;
        values.put("equation", left + "x - " + right + " = 0");
        values.put("equationSolution", simplifyFraction(right, left));
        values.put("factorExpression", coefficient + "x + " + (coefficient * 2));
        values.put("factorizedExpression", coefficient + "(x + 2)");
        values.put("signedFactorExpression", "-" + coefficient + "x + " + (coefficient * 3));
        values.put("signedFactorizedExpression", "-" + coefficient + "(x - 3)");
        values.put("fraction", (2 * coefficient) + "/" + (2 * exponent));
        values.put("fractionSimplified", simplifyFraction(2 * coefficient, 2 * exponent));
        values.put("fractionSum", "1/" + exponent + " + 1/" + coefficient);
        values.put("fractionSumResult", simplifyFraction(coefficient + exponent, coefficient * exponent));
        values.put("fractionTrap", "(x + " + coefficient + ")/x");
        values.put("expandExpression", coefficient + "(x + " + exponent + ")");
        values.put("expandedExpression", coefficient + "x + " + (coefficient * exponent));
        values.put("expandNegative", "-(" + coefficient + "x - " + exponent + ")");
        values.put("expandedNegative", "-" + coefficient + "x + " + exponent);

        String extracted = extractFromMistakes(axisNeed.mistakes());
        if (!extracted.isBlank()) {
            values.put("term", extracted);
            values.put("derivative", deriveMonomial(extracted));
        }
        return values;
    }

    private String extractFromMistakes(List<String> mistakes) {
        return mistakes.stream()
                .map(this::findFormula)
                .filter(value -> !value.isBlank())
                .sorted(Comparator.comparingInt(String::length).reversed())
                .findFirst()
                .orElse("");
    }

    private String findFormula(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        Matcher matcher = Pattern.compile("([+-]?\\d*)x(?:\\^(\\d+)|([²³]))").matcher(normalizeMath(text));
        return matcher.find() ? matcher.group(0) : "";
    }

    private String deriveMonomial(String term) {
        String cleaned = normalizeMath(term);
        Matcher coefficientMatcher = NUMBER_PATTERN.matcher(cleaned);
        int coefficient = coefficientMatcher.find() ? Integer.parseInt(coefficientMatcher.group(1)) : 1;
        if (cleaned.startsWith("-")) {
            coefficient = -coefficient;
        }
        Matcher exponentMatcher = EXPONENT_PATTERN.matcher(cleaned);
        int exponent = 1;
        if (exponentMatcher.find()) {
            if (exponentMatcher.group(1) != null) {
                exponent = Integer.parseInt(exponentMatcher.group(1));
            } else {
                exponent = "²".equals(exponentMatcher.group(2)) ? 2 : 3;
            }
        }
        int derivedCoefficient = coefficient * exponent;
        int derivedExponent = exponent - 1;
        if (derivedExponent <= 0) {
            return String.valueOf(derivedCoefficient);
        }
        if (derivedExponent == 1) {
            return derivedCoefficient + "x";
        }
        return derivedCoefficient + "x^" + derivedExponent;
    }

    private String simplifyFraction(int numerator, int denominator) {
        int gcd = gcd(Math.abs(numerator), Math.abs(denominator));
        int simpleNumerator = numerator / gcd;
        int simpleDenominator = denominator / gcd;
        if (simpleDenominator == 1) {
            return String.valueOf(simpleNumerator);
        }
        return simpleNumerator + "/" + simpleDenominator;
    }

    private int gcd(int a, int b) {
        while (b != 0) {
            int next = a % b;
            a = b;
            b = next;
        }
        return a == 0 ? 1 : a;
    }

    private String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private boolean isMathContext(String activityName, String axisTitle) {
        String normalizedActivity = normalize(activityName);
        String normalizedAxis = normalize(axisTitle);
        return containsAny(normalizedActivity, MATH_ACTIVITY_HINTS)
                || containsAny(normalizedAxis, MATH_AXIS_HINTS);
    }

    private boolean containsAny(String value, List<String> hints) {
        for (String hint : hints) {
            if (value.contains(hint)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsAny(String value, String... hints) {
        for (String hint : hints) {
            if (value.contains(hint)) {
                return true;
            }
        }
        return false;
    }

    private String blank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String normalizeMath(String value) {
        return value == null ? "" : value.replace(" ", "").trim();
    }

    private String resolveQuestionType(String kind) {
        return switch (kind) {
            case "compare", "factor-compare", "expand-compare" -> "CHOICE";
            default -> "OPEN";
        };
    }

    private List<String> buildChoiceOptions(String kind, Map<String, String> values) {
        return switch (kind) {
            case "compare" -> List.of(
                    values.get("derivative"),
                    values.get("term"),
                    values.get("signedDerivative"),
                    values.get("derivative") + " + 1"
            );
            case "factor-compare" -> List.of(
                    values.get("factorizedExpression"),
                    values.get("factorExpression"),
                    values.get("signedFactorizedExpression"),
                    values.get("factorizedExpression").replace("(x + 2)", "(x - 2)")
            );
            case "expand-compare" -> List.of(
                    values.get("expandedExpression"),
                    values.get("expandExpression"),
                    values.get("expandedExpression").replace(" + ", " - "),
                    values.get("expandedExpression") + " + x"
            );
            default -> List.of();
        };
    }

    private record TemplatePlan(List<TemplateVariant> variants) {
    }

    private record TemplateVariant(String difficulty, String kind) {
    }
}
