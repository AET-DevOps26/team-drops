import React from 'react';

const steps = [
  {
    selector: '[data-intro-target="current-plan"]',
    title: 'Current plan',
    body: 'This card shows the learning plan you are working on now.',
  },
  {
    selector: '[data-intro-target="start-learning"]',
    title: 'Start learning',
    body: 'Use this tab to open your plans, lessons, and exercises.',
  },
  {
    selector: '[data-intro-target="overview"]',
    title: 'Overview',
    body: 'Come back here to check recent progress and finished lessons.',
  },
  {
    selector: '[data-intro-target="settings"]',
    title: 'Settings',
    body: 'Change your profile, app language, learning language, and theme here.',
  },
];

export function IntroOverlay({ onFinish }) {
  const [stepIndex, setStepIndex] = React.useState(0);
  const [targetRect, setTargetRect] = React.useState(null);
  const step = steps[stepIndex];

  React.useLayoutEffect(() => {
    if (!step) {
      return undefined;
    }

    let frameId = 0;

    const updateTargetRect = () => {
      const shell = document.querySelector('.app-shell');
      const target = document.querySelector(step.selector);

      if (!shell || !target) {
        setTargetRect(null);
        return;
      }

      const shellRect = shell.getBoundingClientRect();
      const rect = target.getBoundingClientRect();
      const padding = 6;

      setTargetRect({
        top: Math.max(0, rect.top - shellRect.top - padding),
        left: Math.max(0, rect.left - shellRect.left - padding),
        width: Math.min(shellRect.width, rect.width + padding * 2),
        height: Math.min(shellRect.height, rect.height + padding * 2),
        shellHeight: shellRect.height,
      });
    };

    const scheduleUpdate = () => {
      window.cancelAnimationFrame(frameId);
      frameId = window.requestAnimationFrame(updateTargetRect);
    };

    scheduleUpdate();
    window.addEventListener('resize', scheduleUpdate);
    document.querySelector('.dashboard-content')?.addEventListener('scroll', scheduleUpdate, { passive: true });

    return () => {
      window.cancelAnimationFrame(frameId);
      window.removeEventListener('resize', scheduleUpdate);
      document.querySelector('.dashboard-content')?.removeEventListener('scroll', scheduleUpdate);
    };
  }, [step]);

  if (!step) {
    return null;
  }

  const goNext = () => {
    if (stepIndex === steps.length - 1) {
      onFinish();
      return;
    }

    setStepIndex((index) => index + 1);
  };

  const tooltipBelow = !targetRect || targetRect.top + targetRect.height < targetRect.shellHeight - 150;
  const tooltipStyle = targetRect
    ? {
      left: 18,
      right: 18,
      top: tooltipBelow ? targetRect.top + targetRect.height + 14 : 'auto',
      bottom: tooltipBelow ? 'auto' : targetRect.shellHeight - targetRect.top + 14,
    }
    : undefined;

  return (
    <div className="intro-overlay" role="dialog" aria-modal="true" aria-label="App introduction">
      {targetRect && (
        <button
          className="intro-spotlight"
          style={{
            top: targetRect.top,
            left: targetRect.left,
            width: targetRect.width,
            height: targetRect.height,
          }}
          type="button"
          aria-label={`Continue from ${step.title}`}
          onClick={goNext}
        />
      )}

      <section className="intro-card" style={tooltipStyle}>
        <div>
          <small>{stepIndex + 1} / {steps.length}</small>
          <h3>{step.title}</h3>
          <p>{step.body}</p>
        </div>
        <button className="intro-skip" type="button" onClick={onFinish}>
          Skip
        </button>
      </section>
    </div>
  );
}
