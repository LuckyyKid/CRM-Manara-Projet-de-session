document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("settingsForm");
    if (!form) {
        return;
    }

    const feedback = document.getElementById("settingsFeedback");
    const avatarInput = document.getElementById("settingsAvatarFile");
    const avatarPreview = document.getElementById("settingsAvatarPreview");
    const passwordInput = document.getElementById("settingsPassword");
    const passwordConfirmInput = document.getElementById("settingsPasswordConfirm");

    const validators = {
        prenom: (value) => value.trim().length >= 2 ? "" : "Le prénom doit contenir au moins 2 caractères.",
        nom: (value) => value.trim().length >= 2 ? "" : "Le nom doit contenir au moins 2 caractères.",
        email: (value) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value.trim()) ? "" : "Veuillez saisir un courriel valide.",
        adresse: (value) => value.length <= 255 ? "" : "L'adresse ne peut pas dépasser 255 caractères.",
        newPassword: (value) => !value || value.length >= 8 ? "" : "Le mot de passe doit contenir au moins 8 caractères.",
        confirmPassword: (value) => !passwordInput.value || value === passwordInput.value ? "" : "La confirmation du mot de passe ne correspond pas.",
        avatarFile: (_, input) => {
            const file = input.files[0];
            if (!file) {
                return "";
            }
            if (!file.type.startsWith("image/")) {
                return "Le fichier doit être une image.";
            }
            if (file.size > 2_000_000) {
                return "L'image ne doit pas dépasser 2 Mo.";
            }
            return "";
        }
    };

    function setFieldError(name, message) {
        const input = form.querySelector(`[name="${name}"]`);
        const node = form.querySelector(`[data-error-for="${name}"]`);
        if (!input || !node) {
            return;
        }
        input.classList.toggle("is-invalid", Boolean(message));
        node.textContent = message || "";
    }

    function validateField(name) {
        const input = form.querySelector(`[name="${name}"]`);
        if (!input || !validators[name]) {
            return true;
        }
        const message = validators[name](input.value, input);
        setFieldError(name, message);
        return !message;
    }

    function validateForm() {
        return ["prenom", "nom", "email", "adresse", "newPassword", "confirmPassword", "avatarFile"]
            .map(validateField)
            .every(Boolean);
    }

    function showFeedback(type, message) {
        feedback.className = `alert alert-${type}`;
        feedback.textContent = message;
        feedback.classList.remove("d-none");
    }

    form.querySelectorAll("input").forEach((input) => {
        input.addEventListener("input", () => validateField(input.name));
        input.addEventListener("change", () => validateField(input.name));
    });

    if (avatarInput && avatarPreview) {
        avatarInput.addEventListener("change", () => {
            validateField("avatarFile");
            const file = avatarInput.files[0];
            if (!file || !file.type.startsWith("image/")) {
                return;
            }
            const reader = new FileReader();
            reader.onload = () => {
                avatarPreview.src = reader.result;
            };
            reader.readAsDataURL(file);
        });
    }

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        feedback.classList.add("d-none");

        if (!validateForm()) {
            showFeedback("danger", "Veuillez corriger les champs invalides.");
            return;
        }

        const formData = new FormData(form);
        const csrfName = form.dataset.csrfName;
        const csrfToken = form.dataset.csrfToken;
        if (csrfName && csrfToken) {
            formData.append(csrfName, csrfToken);
        }

        const response = await fetch("/api/settings", {
            method: "POST",
            body: formData
        });

        const payload = await response.json();
        if (!payload.success) {
            Object.entries(payload.errors || {}).forEach(([name, message]) => setFieldError(name, message));
            showFeedback("danger", payload.message || "Impossible d'enregistrer les paramètres.");
            return;
        }

        ["prenom", "nom", "email", "adresse", "newPassword", "confirmPassword", "avatarFile"].forEach((name) => setFieldError(name, ""));
        passwordInput.value = "";
        passwordConfirmInput.value = "";
        showFeedback("success", payload.message || "Paramètres enregistrés.");
        window.setTimeout(() => {
            window.location.href = payload.redirectUrl || "/settings";
        }, 900);
    });
});
