-- Le cycle de vie horaire cherche désormais les abonnements réglés en espèces dont la période
-- est échue (TenantAccessLifecycleJob.expireCashSubscriptions). Sans index, cette recherche
-- balaie toute la table à chaque exécution — supportable à huit établissements, plus du tout
-- quand la plateforme en comptera quelques milliers.
--
-- Même forme que les deux index posés par V5 pour les deux autres étapes du job.
CREATE INDEX idx_subscriptions_status_current_period_end
    ON subscriptions (status, current_period_end);
