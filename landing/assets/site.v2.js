/* School Manager - site vitrine (v2). Amélioration du menu mobile uniquement (SPEC §6.1) :
   sans ce script, le menu <details> fonctionne déjà ; il ajoute la fermeture par Échap
   (le focus revient sur le bouton), par clic hors du panneau, au clic sur un lien et
   quand le focus clavier sort du menu (Tab), pour que le panneau ne recouvre pas
   l'élément focalisé. */
(function () {
  "use strict";
  var menu = document.querySelector(".nav-mobile");
  if (!menu) return;
  var summary = menu.querySelector("summary");

  function close(returnFocus) {
    if (!menu.open) return;
    menu.open = false;
    if (returnFocus && summary) summary.focus();
  }

  document.addEventListener("keydown", function (e) {
    if (e.key === "Escape" || e.key === "Esc") close(menu.contains(document.activeElement));
  });

  document.addEventListener("click", function (e) {
    if (!menu.contains(e.target)) close(false);
  });

  menu.addEventListener("click", function (e) {
    if (e.target.closest && e.target.closest("nav a")) close(false);
  });

  // Fermeture quand le focus quitte le menu (Tab / Maj+Tab hors du panneau).
  // relatedTarget nul (clic dans une zone non focalisable, changement de fenêtre) :
  // on laisse le clic document gérer la fermeture.
  menu.addEventListener("focusout", function (e) {
    var next = e.relatedTarget;
    if (next && !menu.contains(next)) close(false);
  });
})();
