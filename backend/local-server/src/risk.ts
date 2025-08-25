import { IntegrityMockPayload, RootCheckResultPayload } from './types.js';

export function computeRisk(root?: RootCheckResultPayload, integrity?: IntegrityMockPayload) {
  let score = 0;
  if (root?.rooted) score += 40;
  const verdicts = integrity?.deviceIntegrity.deviceRecognitionVerdict || [];
  if (!verdicts.includes('MEETS_DEVICE_INTEGRITY')) score += 40;
  if (integrity && integrity.appIntegrity.appRecognitionVerdict !== 'PLAY_RECOGNIZED') score += 20;
  if (root?.triggeredChecks?.some(c => /magisk|su/i.test(c))) score += 10;
  return Math.min(score, 100);
}
