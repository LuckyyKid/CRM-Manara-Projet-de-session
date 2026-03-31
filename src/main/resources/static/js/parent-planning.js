document.addEventListener("DOMContentLoaded", () => {
    const triggers = document.querySelectorAll(".js-activity-detail-trigger");

    if (triggers.length === 0) {
        return;
    }

    function fillModal(trigger, prefix) {
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
        time.textContent = `${trigger.dataset.start || ""} à ${trigger.dataset.end || ""}`.trim();
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

    triggers.forEach((trigger) => {
        trigger.addEventListener("click", () => {
            const modalTarget = trigger.getAttribute("data-bs-target");
            if (modalTarget === "#parentActivityDetailModal") {
                fillModal(trigger, "parentActivityDetail");
            }
            if (modalTarget === "#planningActivityDetailModal") {
                fillModal(trigger, "planningActivityDetail");
            }
        });
    });
});
