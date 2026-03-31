document.addEventListener("DOMContentLoaded", () => {
    function fillModal(trigger, prefix) {
        if (!trigger) {
            return;
        }

        const title = document.getElementById(`${prefix}Title`);
        const description = document.getElementById(`${prefix}Description`);
        const time = document.getElementById(`${prefix}Time`);
        const age = document.getElementById(`${prefix}Age`);
        const status = document.getElementById(`${prefix}Status`);
        const animateur = document.getElementById(`${prefix}Animateur`);
        const children = document.getElementById(`${prefix}Children`);

        if (!title || !description || !time || !age || !status || !animateur || !children) {
            return;
        }

        title.textContent = trigger.dataset.title || "Activité";
        description.textContent = trigger.dataset.description || "";

        const start = trigger.dataset.start || "";
        const end = trigger.dataset.end || "";
        time.textContent = start && end ? `${start} à ${end}` : (start || end);

        age.textContent = trigger.dataset.ageRange || "";
        status.textContent = trigger.dataset.status || "";
        animateur.textContent = trigger.dataset.animateur || "Animateur à confirmer";
        children.innerHTML = "";

        const rawChildren = trigger.dataset.children || "Aucun enfant inscrit pour le moment.";
        const entries = rawChildren.split("|").map((entry) => entry.trim()).filter(Boolean);
        const list = document.createElement("ul");
        list.className = "mb-0 ps-3";

        entries.forEach((entry) => {
            const item = document.createElement("li");
            item.textContent = entry;
            list.appendChild(item);
        });

        children.appendChild(list);
    }

    function bindModal(modalId, prefix) {
        const modal = document.querySelector(modalId);
        if (!modal) {
            return;
        }

        modal.addEventListener("show.bs.modal", (event) => {
            fillModal(event.relatedTarget, prefix);
        });
    }

    bindModal("#parentActivityDetailModal", "parentActivityDetail");
    bindModal("#planningActivityDetailModal", "planningActivityDetail");

    document.querySelectorAll(".js-activity-detail-trigger").forEach((trigger) => {
        trigger.addEventListener("keydown", (event) => {
            if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                trigger.click();
            }
        });
    });
});
