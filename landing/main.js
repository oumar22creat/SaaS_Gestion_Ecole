// Révélation des sections à l'entrée dans le viewport.
// IntersectionObserver plutôt qu'un écouteur de scroll : pas de travail par frame.
(function () {
  "use strict";

  var targets = document.querySelectorAll(".reveal");
  if (!targets.length) return;

  var reduced = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

  if (reduced || !("IntersectionObserver" in window)) {
    targets.forEach(function (el) {
      el.classList.add("is-in");
    });
    return;
  }

  var observer = new IntersectionObserver(
    function (entries) {
      entries.forEach(function (entry) {
        if (!entry.isIntersecting) return;
        entry.target.classList.add("is-in");
        observer.unobserve(entry.target);
      });
    },
    { threshold: 0.18, rootMargin: "0px 0px -8% 0px" }
  );

  targets.forEach(function (el) {
    observer.observe(el);
  });

  // Filet de sécurité : si l'observateur ne se déclenche pas (onglet en arrière-plan,
  // pré-rendu, navigateur intégré à une application), le contenu doit rester visible.
  window.setTimeout(function () {
    targets.forEach(function (el) {
      el.classList.add("is-in");
    });
    observer.disconnect();
  }, 2500);
})();
