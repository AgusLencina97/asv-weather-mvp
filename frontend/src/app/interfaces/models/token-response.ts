export interface TokenResponse {
  accessToken: string;
  tokenType: string;
  /** Segundos hasta que caduca el token */
  expiresIn: number;
}
