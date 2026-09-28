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
  </>
);
