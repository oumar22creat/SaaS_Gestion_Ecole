package com.schoolsaas.tenant.dto;

import com.schoolsaas.tenant.Tenant;

/** Doit rester compatible avec web/src/app/branding/tenant-branding.model.ts (TenantBranding). */
public record TenantBrandingResponse(String name, String logoUrl, String primaryColor, String secondaryColor) {

    /**
     * Marque à appliquer quand aucun établissement n'est résolu : page d'inscription, écran de
     * connexion sur le sous-domaine applicatif, développement local. Ce n'est pas une erreur
     * mais un état normal, et la question « quelle marque afficher ? » y a une réponse.
     *
     * <p>Mêmes couleurs que celles d'un établissement fraîchement inscrit (Tenant.java,
     * migration V53) et que les jetons de docs/DESIGN.md §2 : l'écran ne change pas d'aspect
     * entre avant et après la connexion.
     */
    public static TenantBrandingResponse neutre() {
        return new TenantBrandingResponse(null, null, "#0f5c4c", "#c9a227");
    }

    public static TenantBrandingResponse from(Tenant tenant) {
        return new TenantBrandingResponse(tenant.getName(), tenant.getLogoUrl(), tenant.getPrimaryColor(), tenant.getSecondaryColor());
    }
}
