document.addEventListener("DOMContentLoaded", () => {
    const forms = document.querySelectorAll(".js-confirm-submit-form");

    if (forms.length === 0) {
        return;
    }

    async function askConfirmation(form) {
        const message = form.dataset.confirm || "Voulez-vous continuer ?";
        const title = form.dataset.confirmTitle || "Confirmer l'action";
        const confirmLabel = form.dataset.confirmLabel || "Confirmer";
        const confirmClass = form.dataset.confirmClass || "btn-danger";

        if (window.ManaraConfirm?.open) {
            return window.ManaraConfirm.open({ title, message, confirmLabel, confirmClass });
        }

        return window.confirm(message);
    }

    forms.forEach((form) => {
        form.addEventListener("submit", async (event) => {
            if (form.dataset.confirmed === "true") {
                form.dataset.confirmed = "false";
                return;
            }

            event.preventDefault();
            const confirmed = await askConfirmation(form);
            if (!confirmed) {
                return;
            }

            form.dataset.confirmed = "true";
            form.requestSubmit();
        });
    });
});
