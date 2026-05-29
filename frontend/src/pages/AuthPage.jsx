import React from 'react';
import { ArrowLeft, Mail, UserPlus } from 'lucide-react';

export function AuthPage({ t, authErrorMessage, authPending, onAuthenticated }) {
  const [mode, setMode] = React.useState('start');
  const [message, setMessage] = React.useState('');

  React.useEffect(() => {
    setMessage(authErrorMessage ?? '');
  }, [authErrorMessage]);

  React.useEffect(() => {
    setMessage('');
  }, [mode]);

  const handleSubmit = async (event) => {
    event.preventDefault();

    const formData = new FormData(event.currentTarget);
    const payload = {
      name: String(formData.get('name') ?? '').trim(),
      email: String(formData.get('email') ?? '').trim(),
      password: String(formData.get('password') ?? ''),
    };

    await onAuthenticated(mode, payload);
  };


  if (mode === 'login' || mode === 'register') {
    const isRegister = mode === 'register';

    return (
      <>
        <header className="app-header auth-form-header">
          <button className="icon-button" type="button" aria-label="Back" onClick={() => setMode('start')}>
            <ArrowLeft size={21} aria-hidden="true" />
          </button>
          <div>
            <p className="app-label">{t.appLabel}</p>
            <h1>{isRegister ? 'Create account' : 'Email login'}</h1>
          </div>
        </header>

        <form className="auth-card auth-form" aria-label={isRegister ? 'Create account' : 'Email login'} onSubmit={handleSubmit}>
          <div className="auth-heading">
            <p className="eyebrow">{isRegister ? 'Register' : t.welcomeBack}</p>
            <h2>{isRegister ? 'Create your account.' : 'Sign in with email.'}</h2>
          </div>

          {isRegister && (
            <label className="auth-field">
              <span>Name</span>
              <input autoComplete="name" name="name" placeholder="Ada Lovelace" required type="text" />
            </label>
          )}

          <label className="auth-field">
            <span>Email</span>
            <input autoComplete="email" name="email" placeholder="you@example.com" required type="email" />
          </label>

          <label className="auth-field">
            <span>Password</span>
            <input
              autoComplete={isRegister ? 'new-password' : 'current-password'}
              name="password"
              placeholder="Password"
              required
              type="password"
            />
          </label>

          {message && <p className="auth-error" role="alert">{message}</p>}

          <button className="auth-button register-button" disabled={authPending} type="submit">
            {authPending ? (isRegister ? 'Creating account...' : 'Signing in...') : isRegister ? 'Create account' : 'Sign in'}
          </button>

          <button
            className="text-login"
            type="button"
            onClick={() => setMode(isRegister ? 'login' : 'register')}
          >
            {isRegister ? 'Already have an account? Sign in' : 'Need an account? Create one'}
          </button>
        </form>
      </>
    );
  }

  return (
    <>
      <header className="app-header">
        <div className="logo-placeholder" aria-hidden="true">TD</div>
        <div>
          <p className="app-label">{t.appLabel}</p>
          <h1>APP_NAME</h1>
        </div>
      </header>

      <section className="auth-card" aria-label="Sign in options">
        <div className="auth-heading">
          <p className="eyebrow">{t.welcomeBack}</p>
          <h2>Sign in to continue learning.</h2>
        </div>

        <div className="auth-actions">
          <button className="auth-button register-button" type="button" onClick={() => setMode('register')}>
            <UserPlus size={20} aria-hidden="true" />
            Register
          </button>
        </div>

        <div className="divider">
          <span>or</span>
        </div>

        <button className="text-login" type="button" onClick={() => setMode('login')}>
          <Mail size={18} aria-hidden="true" />
          Sign in with email
        </button>
      </section>
    </>
  );
}
