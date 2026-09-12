export interface TenantRegistrationRequest {
  schoolName: string;
  subdomain: string;
  adminEmail: string;
  adminPassword: string;
  adminFirstName: string;
  adminLastName: string;
}

export interface TenantRegistrationResponse {
  tenantId: number;
  subdomain: string;
  tokens: {
    accessToken: string;
    refreshToken: string;
    expiresIn: number;
  };
}
