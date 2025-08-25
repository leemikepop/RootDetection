import express from 'express';
import cors from 'cors';
import { nanoid } from 'nanoid';
import { randomBytes } from 'crypto';
import { z } from 'zod';
import { decodeIntegrityReal } from './playIntegrityReal.js';
import fs from 'fs';
import path from 'path';

// 自動載入本地 service account key (僅限開發)；正式環境應由部署平台安全注入環境變數
(() => {
  if (!process.env.GOOGLE_APPLICATION_CREDENTIALS) {
    const candidate = path.resolve(process.cwd(), 'play-integrity-sa-key.json');
    if (fs.existsSync(candidate)) {
      process.env.GOOGLE_APPLICATION_CREDENTIALS = candidate;
      console.log('[init] GOOGLE_APPLICATION_CREDENTIALS set to', candidate);
    }
  }
})();

const app = express();
app.use(cors());
app.use(express.json());

// In-memory stores (開發測試用，重啟即清除)
const nonces: Record<string, { value: string; expiresAt: number; used: boolean }> = {};
// 僅保留 nonce 功能，不再儲存驗證結果

// 生成符合 Play Integrity 規範的 base64 web-safe (no wrap / no padding) nonce
// 規範：Base64 (URL safe) 後的字串；解碼後位元組長度需 16~500 bytes
function generateNonce(byteLength = 32): string {
  const buf = randomBytes(byteLength); // 32 bytes => 足夠 entropy
  return buf.toString('base64')
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/g, ''); // 移除 padding
}

app.get('/nonce', (req, res) => {
  const id = nanoid(12);
  const value = generateNonce();
  nonces[id] = { value, expiresAt: Date.now() + 2 * 60 * 1000, used: false };
  res.json({ nonceId: id, nonce: value, byteLen: 32 });
});

// 呼叫 Google Play Integrity decode (需要啟用 API + 服務帳戶權限)
// POST /integrity/decode { packageName, token, serviceAccountFile? }
app.post('/integrity/decode', async (req, res) => {
  try {
    const schema = z.object({
      packageName: z.string().min(1),
      token: z.string().min(10),
      serviceAccountFile: z.string().optional(),
    });
    const parsed = schema.safeParse(req.body);
    if (!parsed.success) return res.status(400).json({error: 'invalid_body', detail: parsed.error.flatten()});
    const {packageName, token, serviceAccountFile} = parsed.data;

    let keyJson: any | undefined;
    if (serviceAccountFile) {
      const p = path.resolve(serviceAccountFile);
      if (!fs.existsSync(p)) return res.status(400).json({error: 'service_account_file_not_found'});
      keyJson = JSON.parse(fs.readFileSync(p, 'utf-8'));
    }

    const cleanPackageName = packageName.trim();
    if (cleanPackageName !== packageName) {
      console.warn('[decode] packageName had leading/trailing spaces, trimmed:', JSON.stringify(packageName));
    }
    console.log('[decode] incoming packageName=', cleanPackageName, 'token.len=', token.length);
    const decoded = await decodeIntegrityReal({packageName: cleanPackageName, integrityToken: token, serviceAccountKeyJson: keyJson});
    const pkgInToken = decoded.tokenPayloadExternal?.requestDetails?.requestPackageName;
    if (pkgInToken && pkgInToken !== packageName) {
      console.warn('[decode] packageName mismatch: request=', packageName, ' payload=', pkgInToken);
    }
    console.log('[decode] tokenPayloadExternal:', JSON.stringify(decoded.tokenPayloadExternal, null, 2));
    res.json(decoded);
  } catch (e: any) {
    res.status(500).json({error: e.message});
  }
});

const PORT = process.env.PORT || 5179;
app.listen(PORT, () => {
  console.log('[local-server] listening on port', PORT);
});
