document.addEventListener("DOMContentLoaded", () => {
    const forms = document.querySelectorAll(".js-admin-delete-form");

    if (forms.length === 0) {
        return;
    }

    function showFeedback(targetId, message, success) {
        const feedback = document.getElementById(targetId);
        if (!feedback) {
            return;
        }

        feedback.textContent = message;
        feedback.classList.remove("d-none", "alert-success", "alert-danger");
        feedback.classList.add(success ? "alert-success" : "alert-danger");
    }

    forms.forEach((form) => {
        form.addEventListener("submit", async (event) => {
            event.preventDefault();

            const message = form.dataset.confirm || "Confirmer la suppression ?";
            if (!window.confirm(message)) {
                return;
            }

            try {
                const response = await fetch(form.dataset.deleteUrl, {
                    method: "POST",
                    body: new FormData(form)
                });

                if (!response.ok) {
                    throw new Error("Erreur reseau");
                }

                const data = await response.json();
                showFeedback(form.dataset.feedbackTarget, data.message, Boolean(data.success));

                if (data.success) {
                    const row = document.querySelector(`[data-admin-row="${form.dataset.rowId}"]`);
                    if (row) {
                        row.remove();
                    }
                }
            } catch (error) {
                showFeedback(form.dataset.feedbackTarget, "Impossible de supprimer cet élément pour le moment.", false);
            }
        });
    });
});
