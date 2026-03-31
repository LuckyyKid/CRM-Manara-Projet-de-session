document.addEventListener("DOMContentLoaded", () => {
    const feedback = document.getElementById("animateurInscriptionsFeedback");
    const forms = document.querySelectorAll(".js-animateur-presence-form");
    const rows = document.querySelectorAll(".js-animateur-presence-row");
    const saveAllButton = document.querySelector(".js-animateur-save-all");

    if (!feedback || (forms.length === 0 && rows.length === 0)) {
        return;
    }

    function showFeedback(message, success) {
        feedback.textContent = message;
        feedback.classList.remove("d-none", "alert-success", "alert-danger");
        feedback.classList.add(success ? "alert-success" : "alert-danger");
    }

    async function submitPresence(inscriptionId, formData) {
        const response = await fetch(`/animateur/api/inscriptions/${inscriptionId}/presence`, {
            method: "POST",
            body: formData,
            headers: {
                "X-Requested-With": "XMLHttpRequest"
            }
        });

        if (!response.ok) {
            throw new Error("Erreur reseau");
        }

        return response.json();
    }

    forms.forEach((form) => {
        form.addEventListener("submit", async (event) => {
            event.preventDefault();

            const inscriptionId = form.dataset.inscriptionId;
            const submitButton = form.querySelector(".js-animateur-presence-submit");
            const formData = new FormData(form);

            submitButton.disabled = true;
            submitButton.textContent = "En cours...";

            try {
                const data = await submitPresence(inscriptionId, formData);
                showFeedback(data.message, Boolean(data.success));

                submitButton.disabled = false;
                submitButton.textContent = "Enregistrer";
            } catch (error) {
                showFeedback("Impossible de mettre a jour la presence pour le moment.", false);
                submitButton.disabled = false;
                submitButton.textContent = "Enregistrer";
            }
        });
    });

    if (saveAllButton && rows.length > 0) {
        saveAllButton.addEventListener("click", async () => {
            saveAllButton.disabled = true;
            saveAllButton.textContent = "En cours...";

            let successCount = 0;

            try {
                for (const row of rows) {
                    const inscriptionId = row.dataset.inscriptionId;
                    const formData = new FormData();

                    formData.append(row.dataset.csrfName, row.dataset.csrfToken);
                    formData.append("redirectTo", row.dataset.redirectTo);
                    formData.append(
                        "presenceStatus",
                        row.querySelector(".js-animateur-presence-status")?.value || ""
                    );
                    formData.append(
                        "incidentNote",
                        row.closest("tr")?.querySelector(".js-animateur-incident-note")?.value || ""
                    );

                    const data = await submitPresence(inscriptionId, formData);

                    if (!data.success) {
                        throw new Error(data.message || "Erreur lors de l'enregistrement.");
                    }

                    successCount += 1;
                }

                showFeedback(`Presences enregistrees pour ${successCount} enfant(s).`, true);
            } catch (error) {
                showFeedback("Impossible d'enregistrer toutes les presences pour le moment.", false);
            } finally {
                saveAllButton.disabled = false;
                saveAllButton.textContent = "Enregistrer";
            }
        });
    }
});
