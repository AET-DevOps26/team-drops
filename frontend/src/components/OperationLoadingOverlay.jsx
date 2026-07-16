import React from 'react';
import { BarChart3, LoaderCircle, Sparkles } from 'lucide-react';

export function OperationLoadingOverlay({ description, title, variant = 'progress' }) {
  const Icon = variant === 'rag' ? Sparkles : BarChart3;

  return (
    <div
      className="language-switch-overlay"
      role="dialog"
      aria-modal="true"
      aria-labelledby="operation-loading-title"
      aria-describedby="operation-loading-description"
    >
      <section className="language-switch-card" aria-live="polite" aria-busy="true">
        <span className="language-switch-icon" aria-hidden="true">
          <Icon size={28} />
        </span>
        <LoaderCircle className="language-switch-spinner" size={34} aria-hidden="true" />
        <div>
          <h2 id="operation-loading-title">{title}</h2>
          <p id="operation-loading-description">{description}</p>
        </div>
      </section>
    </div>
  );
}
