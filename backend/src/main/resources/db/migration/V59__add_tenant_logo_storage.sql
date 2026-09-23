-- Logo de l'établissement, téléversé et stocké côté serveur (cahier-des-charges.md §2.4).
--
-- `logo_url` reste : c'est une adresse que le navigateur va chercher lui-même pour afficher
-- l'en-tête de l'application. Elle ne convient pas aux PDF, qui sont fabriqués par le
-- serveur : aller chercher une URL saisie librement par un administrateur ferait du serveur
-- un relais vers n'importe quelle adresse interne, et le contenu récupéré repartirait dans
-- un PDF téléchargeable. Le logo des documents officiels est donc téléversé, stocké, et lu
-- localement.
ALTER TABLE tenants ADD COLUMN logo_storage_key VARCHAR(255);
