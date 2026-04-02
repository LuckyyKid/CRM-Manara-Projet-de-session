document.addEventListener("DOMContentLoaded", () => {
    const feedback = document.getElementById("parentActivitiesFeedback");
    const forms = document.querySelectorAll(".js-parent-inscription-form");

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

            const submitButton = form.querySelector(".js-parent-inscription-submit");
            const formData = new FormData(form);

            submitButton.disabled = true;
            submitButton.textContent = "En cours...";

            try {
                const response = await fetch("/parent/api/inscriptions", {
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
                    submitButton.textContent = "Demande envoyee";
                    submitButton.classList.remove("btn-primary");
                    submitButton.classList.add("btn-success");
                } else {
                    submitButton.disabled = false;
                    submitButton.textContent = "Envoyer la demande";
                }
            } catch (error) {
                showFeedback("Impossible de traiter la demande pour le moment.", false);
                submitButton.disabled = false;
                submitButton.textContent = "Envoyer la demande";
            }
        });
    });
});
