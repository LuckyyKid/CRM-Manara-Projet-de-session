document.addEventListener("DOMContentLoaded", () => {
    const modalElement = document.getElementById("mmConfirmModal");
    const titleElement = document.getElementById("mmConfirmTitle");
    const bodyElement = document.getElementById("mmConfirmBody");
    const confirmButton = document.getElementById("mmConfirmSubmit");

    if (!modalElement || !titleElement || !bodyElement || !confirmButton) {
        return;
    }

    let resolver = null;
    let modalInstance = null;

    function ensureModal() {
        if (!window.bootstrap || !window.bootstrap.Modal) {
            return null;
        }
        if (!modalInstance) {
            modalInstance = new window.bootstrap.Modal(modalElement);
            modalElement.addEventListener("hidden.bs.modal", () => {
                if (resolver) {
                    resolver(false);
                    resolver = null;
                }
            });
        }
        return modalInstance;
    }

    window.ManaraConfirm = {
        open({
            title = "Confirmer l'action",
            message = "Voulez-vous continuer ?",
            confirmLabel = "Confirmer",
            confirmClass = "btn-danger"
        } = {}) {
            const modal = ensureModal();
            if (!modal) {
                return Promise.resolve(window.confirm(message));
            }

            titleElement.textContent = title;
            bodyElement.textContent = message;
            confirmButton.textContent = confirmLabel;
            confirmButton.className = `btn ${confirmClass}`;

            return new Promise((resolve) => {
                resolver = resolve;
                modal.show();
            });
        }
    };

    confirmButton.addEventListener("click", () => {
        if (resolver) {
            resolver(true);
            resolver = null;
        }
        modalInstance?.hide();
    });
});
