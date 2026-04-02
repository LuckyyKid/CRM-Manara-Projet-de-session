document.addEventListener("DOMContentLoaded", () => {
    const feedback = document.getElementById("adminInscriptionsFeedback");
    const forms = document.querySelectorAll(".js-admin-inscription-action-form");

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

            const confirmed = window.ManaraConfirm?.open
                ? await window.ManaraConfirm.open({
                    title: form.dataset.confirmTitle || "Confirmer la demande",
                    message: form.dataset.confirm || "Voulez-vous traiter cette demande ?",
                    confirmLabel: form.dataset.confirmLabel || "Confirmer",
                    confirmClass: form.dataset.confirmClass || "btn-primary"
                })
                : window.confirm(form.dataset.confirm || "Voulez-vous traiter cette demande ?");

            if (!confirmed) {
                return;
            }

            const submitButton = form.querySelector("button[type='submit']");
            const formData = new FormData(form);

            submitButton.disabled = true;

            try {
                const response = await fetch(form.dataset.actionUrl, {
                    method: "POST",
                    body: formData,
                    headers: {
                        "X-Requested-With": "XMLHttpRequest"
                    }
                });

                if (!response.ok) {
                    throw new Error("Erreur reseau");
                }

                const data = await response.json();
                showFeedback(data.message, Boolean(data.success));

                if (data.success) {
                    window.location.reload();
                }
            } catch (error) {
                showFeedback("Impossible d'appliquer cette action pour le moment.", false);
            } finally {
                submitButton.disabled = false;
            }
        });
    });
});
