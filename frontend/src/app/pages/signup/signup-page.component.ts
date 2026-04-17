import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { inject } from '@angular/core';

@Component({
  selector: 'app-signup-page',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './signup-page.component.html',
})
export class SignupPageComponent {
  private http = inject(HttpClient);

  nom = signal('');
  prenom = signal('');
  adresse = signal('');
  email = signal('');
  password = signal('');

  errors = signal<Record<string, string>>({});
  message = signal('');
  errorMsg = signal('');
  loading = signal(false);

  // Vérification de la disponibilité de l'email
  emailAvailableMsg = signal('');

  checkEmail() {
    const val = this.email();
    if (!val || !val.includes('@')) {
      this.emailAvailableMsg.set('');
      return;
    }
    this.http
      .get<{ available: boolean; message: string }>(`/api/signUp/email-availability?email=${val}`)
      .subscribe((res) => {
        this.emailAvailableMsg.set(res.message);
      });
  }

  validate(): boolean {
    const errs: Record<string, string> = {};
    if (!this.nom().trim()) errs['nom'] = 'Le nom est obligatoire.';
    if (!this.prenom().trim()) errs['prenom'] = 'Le prénom est obligatoire.';
    if (!this.adresse().trim()) errs['adresse'] = "L'adresse est obligatoire.";
    if (!this.email().trim()) {
      errs['email'] = "L'email est obligatoire.";
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.email())) {
      errs['email'] = 'Entrez une adresse email valide.';
    }
    if (!this.password().trim()) {
      errs['password'] = 'Le mot de passe est obligatoire.';
    } else if (this.password().length < 6) {
      errs['password'] = 'Le mot de passe doit contenir au moins 6 caractères.';
    }
    this.errors.set(errs);
    return Object.keys(errs).length === 0;
  }

  onSubmit() {
    if (!this.validate()) return;
    this.loading.set(true);
    this.errorMsg.set('');
    this.message.set('');

    // POST au backend Spring en form-urlencoded
    // (le backend /signUp exige CSRF via cookie ou session)
    // On redirige vers le formulaire Thymeleaf du backend pour que le CSRF fonctionne.
    // Quand un endpoint POST /api/signUp sera ajouté, on pourra faire l'appel ici.
    const form = document.createElement('form');
    form.method = 'POST';
    form.action = '/signUp';
    const fields: Record<string, string> = {
      nom: this.nom(),
      prenom: this.prenom(),
      adresse: this.adresse(),
      email: this.email(),
      password: this.password(),
    };
    for (const [key, val] of Object.entries(fields)) {
      const input = document.createElement('input');
      input.type = 'hidden';
      input.name = key;
      input.value = val;
      form.appendChild(input);
    }
    document.body.appendChild(form);
    form.submit();
  }
}
