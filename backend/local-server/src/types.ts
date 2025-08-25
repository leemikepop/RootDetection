export interface RootSubCheckPayload { key: string; description: string; detected: boolean; }
export interface RootCheckResultPayload { rooted: boolean; triggeredChecks: string[]; subChecks?: RootSubCheckPayload[]; }

export interface IntegrityMockPayload {
  appIntegrity: { appRecognitionVerdict: string };
  deviceIntegrity: { deviceRecognitionVerdict: string[] };
  accountDetails?: { appLicensingVerdict?: string };
  requestDetails: { nonce: string; timestampMillis: number };
}

export interface VerifyRequestBody {
  nonceId: string;
  nonceValue: string;
  token: string; // 模擬 encrypted token
  root?: RootCheckResultPayload;
}
