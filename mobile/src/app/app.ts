import { Component, inject } from '@angular/core';
import { IonApp, IonHeader, IonIcon, IonRouterOutlet, IonTitle, IonToolbar } from '@ionic/angular';
import { TenantBrandingService } from './branding/tenant-branding.service';

@Component({
  imports: [IonApp, IonHeader, IonToolbar, IonTitle, IonRouterOutlet, IonIcon],
  selector: 'app-root',
  templateUrl: './app.html',
  styles: `
    .brand-icon {
      display: grid;
      place-items: center;
      width: 30px;
      height: 30px;
      margin-inline: var(--space-2) var(--space-1);
      border-radius: var(--radius-sm);
      background: color-mix(in srgb, var(--tenant-primary) 55%, #ffffff 6%);
      font-size: 16px;
      line-height: 1;
    }
  `,
})
export class App {
  private readonly brandingService = inject(TenantBrandingService);
  protected readonly branding = this.brandingService.branding;
}
