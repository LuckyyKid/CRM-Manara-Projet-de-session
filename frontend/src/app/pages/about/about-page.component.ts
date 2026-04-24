import { Component } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-about-page',
  imports: [TranslatePipe],
  templateUrl: './about-page.component.html',
})
export class AboutPageComponent {}
