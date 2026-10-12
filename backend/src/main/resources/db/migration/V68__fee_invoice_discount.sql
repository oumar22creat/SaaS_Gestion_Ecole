-- Remise individuelle sur une facture de scolarité : bourse, tarif fratrie, exonération.
--
-- Rien ne permettait d'ajuster ce qu'un élève doit. Or dans une école privée d'Afrique de
-- l'Ouest, la réduction pour le deuxième enfant est la règle, pas l'exception. Le
-- secrétariat n'avait qu'un contournement : encaisser le montant réduit et laisser la
-- facture éternellement « partiellement payée ». Cela polluait la liste des impayés, faussait
-- le taux de recouvrement, et faisait relancer par SMS des familles parfaitement en règle.
--
-- Le choix de modélisation : `amount_due_cents` porte désormais le montant NET réellement dû,
-- remise déduite. Tout le calcul existant — reste à payer, impayés, recouvrement, reçus — le
-- lit déjà et continue de fonctionner sans retouche. Faire l'inverse (garder le brut et
-- soustraire partout) aurait supposé retrouver et corriger chaque calcul, dont certains dans
-- le portail famille et les PDF : la première ligne oubliée aurait affiché une dette qui
-- n'existe pas.
--
-- La remise est donc redondante avec l'écart au montant de l'échéancier, et c'est voulu :
-- une remise doit rester lisible après coup, même si l'échéancier de la classe change entre
-- temps. « Combien avons-nous accordé cette année » est une question que pose tout conseil
-- d'administration.
ALTER TABLE student_fee_invoices
    ADD COLUMN discount_cents BIGINT NOT NULL DEFAULT 0,
    -- Obligatoire dès qu'une remise est accordée, vérifié côté applicatif : une réduction
    -- sans motif est un trou dans la comptabilité, pas une faveur.
    ADD COLUMN discount_reason VARCHAR(255);

ALTER TABLE student_fee_invoices
    ADD CONSTRAINT student_fee_invoices_discount_non_negatif CHECK (discount_cents >= 0);
