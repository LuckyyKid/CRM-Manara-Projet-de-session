document.addEventListener("DOMContentLoaded", () => {
    const emailInput = document.getElementById("signUpEmail");
    const messageBox = document.getElementById("emailAvailabilityMessage");

    if (!emailInput || !messageBox) {
        return;
    }

    let pendingController = null;
    let debounceTimer = null;

    function setMessage(text, className) {
        messageBox.textContent = text;
        messageBox.className = `small mt-1 ${className}`;
    }

    function resetMessage() {
        setMessage("", "text-secondary");
    }

    function checkEmailAvailability(email) {
        if (pendingController) {
            pendingController.abort();
        }

        pendingController = new AbortController();
        setMessage("Verification en cours...", "text-secondary");

        fetch(`/api/signUp/email-availability?email=${encodeURIComponent(email)}`, {
            signal: pendingController.signal
        })
            .then((response) => {
                if (!response.ok) {
                    throw new Error("Erreur reseau");
                }
                return response.json();
            })
            .then((data) => {
                if (data.available) {
                    setMessage(data.message, "text-success");
                } else {
                    setMessage(data.message, "text-danger");
                }
            })
            .catch((error) => {
                if (error.name === "AbortError") {
                    return;
                }
                setMessage("Impossible de verifier l'email pour le moment.", "text-danger");
            });
    }

    emailInput.addEventListener("input", () => {
        const email = emailInput.value.trim();
        clearTimeout(debounceTimer);

        if (!email) {
            resetMessage();
            return;
        }

        if (!emailInput.validity.valid) {
            setMessage("Entrez une adresse email valide.", "text-warning");
            return;
        }

        debounceTimer = setTimeout(() => checkEmailAvailability(email), 300);
    });
});
