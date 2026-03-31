document.addEventListener("DOMContentLoaded", () => {
    const feedback = document.getElementById("adminParentsFeedback");
    const forms = document.querySelectorAll(".js-admin-family-action-form");

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

                if (form.dataset.deleteCard === "true") {
                    document.querySelector(`[data-admin-parent-card='${form.dataset.parentId}']`)?.remove();
                }

                if (form.dataset.deleteRow === "true") {
                    document.querySelector(`[data-admin-child-row='${form.dataset.childId}']`)?.remove();
                } else {
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
