import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';

const BASE_URL = `${environment.apiUrl}/paperwork`;

/**
 * Documents officiels remis au guichet : certificat de scolarité, reçu de règlement.
 *
 * <p>Le téléchargement passe par HttpClient et non par un simple lien : les documents sont
 * derrière l'authentification, et un `<a href>` partirait sans le jeton.
 */
@Injectable({ providedIn: 'root' })
export class PaperworkService {
  private readonly http = inject(HttpClient);

  async downloadEnrollmentCertificate(studentId: number, studentName: string): Promise<void> {
    await this.download(
      `${BASE_URL}/students/${studentId}/enrollment-certificate.pdf`,
      `certificat-scolarite-${slug(studentName)}.pdf`,
    );
  }

  async downloadPaymentReceipt(paymentId: number, studentName: string): Promise<void> {
    await this.download(
      `${BASE_URL}/payments/${paymentId}/receipt.pdf`,
      `recu-${slug(studentName)}-${paymentId}.pdf`,
    );
  }

  private async download(url: string, filename: string): Promise<void> {
    const blob = await firstValueFrom(this.http.get(url, { responseType: 'blob' }));
    const objectUrl = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = objectUrl;
    anchor.download = filename;
    anchor.click();
    URL.revokeObjectURL(objectUrl);
  }
}

/** Le nom de l'élève dans le nom du fichier : le secrétariat en classe des dizaines par jour. */
function slug(name: string): string {
  return name
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .replace(/[^a-zA-Z0-9]+/g, '-')
    .replace(/^-|-$/g, '')
    .toLowerCase();
}
