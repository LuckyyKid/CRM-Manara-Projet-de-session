document.addEventListener("DOMContentLoaded", () => {
    const feedback = document.getElementById("parentEnfantsFeedback");
    const forms = document.querySelectorAll(".js-parent-enfant-delete-form");

    if (!feedback || forms.length === 0) {
        return;
    }

    function showFeedback(message, success) {
        feedback.textContent = message;
        feedback.classList.remove("d-none", "alert-success", "alert-danger");
        feedback.classList.add(success ? "alert-success" : "alert-danger");
    }

    forms.forEach((form) => {
        form.addEventListener("submit", async (event) => {
            event.preventDefault();

            const enfantId = form.dataset.enfantId;
            if (!window.confirm("Supprimer cet enfant ?")) {
                return;
            }

            try {
                const response = await fetch(`/parent/api/enfants/${enfantId}/delete`, {
                    method: "POST",
                    body: new FormData(form)
                });

                if (!response.ok) {
                    throw new Error("Erreur reseau");
                }

                const data = await response.json();
                showFeedback(data.message, Boolean(data.success));

                if (data.success) {
                    const row = document.querySelector(`[data-enfant-row="${data.id}"]`);
                    if (row) {
                        row.remove();
                    }
                }
            } catch (error) {
                showFeedback("Impossible de supprimer cet enfant pour le moment.", false);
            }
        });
    });
});
