import React from 'react';
import { Composition } from 'remotion';
import { Presentation, totalDuration } from './Presentation';
import { FPS } from './theme';

export const RemotionRoot: React.FC = () => (
  <>
    <Composition
      id="Presentation"
      component={Presentation}
      durationInFrames={totalDuration(FPS)}
      fps={FPS}
      width={1920}
      height={1080}
    />
    {/* 9:16 pour le statut WhatsApp : mêmes scènes, mêmes textes, mêmes durées — seule la
        mise en page change (voir src/format.ts). */}
    <Composition
      id="PresentationVerticale"
      component={Presentation}
      durationInFrames={totalDuration(FPS)}
      fps={FPS}
      width={1080}
      height={1920}
    />
  </>
);
