document.addEventListener("DOMContentLoaded", () => {
    const form = document.querySelector(".js-parent-enfant-form");
    const feedback = document.getElementById("parentEnfantFormFeedback");

    if (!form || !feedback) {
        return;
    }

    const fieldNames = ["nom", "prenom", "dateNaissance"];

    function clearFieldErrors() {
        fieldNames.forEach((name) => {
            const field = form.querySelector(`[data-field-error="${name}"]`);
            if (field) {
                field.textContent = "";
            }
        });
    }

    function showFeedback(message, success) {
        feedback.textContent = message;
        feedback.classList.remove("d-none", "alert-success", "alert-danger");
        feedback.classList.add(success ? "alert-success" : "alert-danger");
    }

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        clearFieldErrors();

        const mode = form.dataset.mode;
        const enfantId = form.dataset.enfantId;
        const url = mode === "edit"
            ? `/parent/api/enfants/${enfantId}/edit`
            : "/parent/api/enfants";

        try {
            const response = await fetch(url, {
                method: "POST",
                body: new FormData(form)
            });

            if (!response.ok) {
                throw new Error("Erreur reseau");
            }

            const data = await response.json();
            if (!data.success) {
                showFeedback(data.message, false);
                const errors = data.errors || {};
                Object.entries(errors).forEach(([fieldName, message]) => {
                    const field = form.querySelector(`[data-field-error="${fieldName}"]`);
                    if (field) {
                        field.textContent = message;
                    }
                });
                return;
            }

            showFeedback(data.message, true);
            window.setTimeout(() => {
                window.location.href = "/parent/enfants";
            }, 350);
        } catch (error) {
            showFeedback("Impossible d'enregistrer cet enfant pour le moment.", false);
        }
    });
});
