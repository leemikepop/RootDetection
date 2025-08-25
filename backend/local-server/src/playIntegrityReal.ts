import {GoogleAuth} from 'google-auth-library';
import {google} from 'googleapis';
import type {JWTInput} from 'google-auth-library/build/src/auth/credentials';

const SCOPE = 'https://www.googleapis.com/auth/playintegrity';
const BASE = 'https://playintegrity.googleapis.com/v1';

export interface DecodeIntegrityResponse { tokenPayloadExternal?: any }

export interface RealDecodeParams {
  packageName: string;
  integrityToken: string;
  /** Optional service account key JSON loaded as object; if omitted, Application Default Credentials used */
  serviceAccountKeyJson?: JWTInput;
}

export async function decodeIntegrityReal(params: RealDecodeParams): Promise<DecodeIntegrityResponse> {
  const {packageName, integrityToken, serviceAccountKeyJson} = params;
  const auth = serviceAccountKeyJson
    ? new GoogleAuth({scopes: [SCOPE], credentials: serviceAccountKeyJson as any})
    : new GoogleAuth({scopes: [SCOPE]});
  const client = await auth.getClient();
  // 依官方文件固定格式 (不對等號左側進行 encode)；packageName 只 encode 內容部分
  const rawUrl = `${BASE}/packageName=${encodeURIComponent(packageName)}:decodeIntegrityToken`;
  const projectId = await (auth as any).getProjectId?.();
  const credentials: any = (client as any).email ? { email: (client as any).email } : {};
  console.log('[decodeIntegrityReal] using projectId=', projectId, 'serviceAccountEmail=', credentials.email, 'rawUrl=', rawUrl);

  // 先嘗試使用 googleapis 官方客戶端 (discovery)
  try {
    const playintegrity = google.playintegrity({version: 'v1', auth});
    // 官方 typings: playintegrity.v1.Playintegrity
    const res: any = await (playintegrity as any).v1.decodeIntegrityToken({
      packageName,
      requestBody: { integrityToken }
    });
    return res.data as DecodeIntegrityResponse;
  } catch (primaryErr: any) {
    console.warn('[decodeIntegrityReal] primary (googleapis) decode failed, fallback raw HTTP', primaryErr?.response?.data || primaryErr.message);
    // fallback：直接使用已授權 client 發送 raw 請求
    try {
      const res = await client.request<DecodeIntegrityResponse>({
        url: rawUrl,
        method: 'POST',
        data: { integrityToken },
      });
      return res.data;
    } catch (fallbackErr: any) {
      if (fallbackErr.response) {
        console.error('[decodeIntegrityReal] fallback url=', rawUrl, 'status=', fallbackErr.response.status, 'data=', fallbackErr.response.data);
      } else {
        console.error('[decodeIntegrityReal] fallback request error', fallbackErr.message);
      }
      throw fallbackErr;
    }
  }
}
