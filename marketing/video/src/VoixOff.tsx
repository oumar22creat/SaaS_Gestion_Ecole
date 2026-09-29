import React from 'react';
import { Audio, Sequence, staticFile } from 'remotion';
import { VOIX_OFF } from './voix-off';

/**
 * Pose chaque réplique sur le début de sa scène.
 *
 * <p>Les décalages viennent de `SCENES` et des transitions : une transition recouvre les deux
 * plans qu'elle relie, donc la scène suivante commence avant que la précédente ne finisse
 * (voir le calcul de `totalDuration` dans Presentation.tsx). Recalculer ces débuts ici plutôt
 * que les recopier évite qu'ils cessent d'être justes à la première scène rallongée.
 */
export const VoixOff: React.FC<{ debuts: number[] }> = ({ debuts }) => (
  <>
    {VOIX_OFF.map((piste) =>
      piste.fichier === null ? null : (
        <Sequence key={piste.scene} from={debuts[piste.scene] + piste.retard}>
          <Audio src={staticFile(`voix-off/${piste.fichier}`)} />
        </Sequence>
      ),
    )}
  </>
);
