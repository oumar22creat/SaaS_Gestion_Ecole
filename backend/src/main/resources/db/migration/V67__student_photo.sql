-- Photo de l'élève, pour la carte d'identité scolaire.
--
-- La carte sert à l'entrée de l'établissement, aux examens et aux sorties scolaires : sans
-- portrait, elle ne prouve rien. C'est la seule raison d'être de ce champ, et elle justifie
-- de stocker une image d'enfant — la politique de confidentialité le mentionnera.
--
-- Même mécanique que le logo de l'établissement : seule la clé de stockage est en base, le
-- fichier vit dans le stockage objet. Une colonne BYTEA ferait grossir chaque sauvegarde de
-- la base de plusieurs dizaines de mégaoctets par école, pour des données qui ne sont
-- jamais interrogées en SQL.
ALTER TABLE students
    ADD COLUMN photo_storage_key VARCHAR(255);
