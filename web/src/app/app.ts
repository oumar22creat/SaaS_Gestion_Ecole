import { Component, inject } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { TenantBrandingService } from './branding/tenant-branding.service';

@Component({
  imports: [RouterOutlet, RouterLink, MatToolbarModule, MatIconModule],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App {
  private readonly brandingService = inject(TenantBrandingService);
  protected readonly branding = this.brandingService.branding;
}
