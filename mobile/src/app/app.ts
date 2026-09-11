import { Component, inject } from '@angular/core';
import { IonApp, IonHeader, IonRouterOutlet, IonTitle, IonToolbar } from '@ionic/angular';
import { TenantBrandingService } from './branding/tenant-branding.service';

@Component({
  imports: [IonApp, IonHeader, IonToolbar, IonTitle, IonRouterOutlet],
  selector: 'app-root',
  templateUrl: './app.html',
})
export class App {
  private readonly brandingService = inject(TenantBrandingService);
  protected readonly branding = this.brandingService.branding;
}
