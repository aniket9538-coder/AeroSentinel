import React, { Component, ErrorInfo, ReactNode } from 'react';
import { AlertTriangle, RefreshCw, RotateCcw } from 'lucide-react';
import { Button } from './Button';

interface ErrorBoundaryProps {
  children: ReactNode;
  fallbackTitle?: string;
  fallbackMessage?: string;
  onReset?: () => void;
}

interface ErrorBoundaryState {
  hasError: boolean;
  error: Error | null;
}

/**
 * Global React Error Boundary
 * Catches unexpected rendering/runtime errors in the component tree,
 * displays a resilient fallback UI, and provides recovery actions.
 */
export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = {
      hasError: false,
      error: null,
    };
  }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return {
      hasError: true,
      error,
    };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo): void {
    // Log unexpected runtime crashes for developer observability
    console.error('[AeroSentinel Global ErrorBoundary]: An unexpected runtime error was caught:', error, errorInfo);
  }

  handleReset = (): void => {
    this.setState({ hasError: false, error: null });
    if (this.props.onReset) {
      this.props.onReset();
    }
  };

  handleReload = (): void => {
    window.location.reload();
  };

  render(): ReactNode {
    if (this.state.hasError) {
      const title = this.props.fallbackTitle || 'Unexpected Application Error';
      const message =
        this.props.fallbackMessage ||
        'A critical rendering issue occurred in the interface. You can attempt to restore the session or reload the dashboard.';

      return (
        <div
          style={{
            minHeight: '400px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '2.5rem 1.5rem',
            background: 'var(--bg-app, #0a0f1d)',
            fontFamily: 'var(--font-body, system-ui, sans-serif)',
          }}
        >
          <div
            style={{
              maxWidth: '520px',
              width: '100%',
              padding: '2rem 1.75rem',
              borderRadius: '16px',
              background: 'var(--bg-card, #111d35)',
              border: '1px solid rgba(239, 68, 68, 0.3)',
              boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.5), 0 8px 10px -6px rgba(0, 0, 0, 0.5)',
              textAlign: 'center',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
            }}
          >
            <div
              style={{
                width: '52px',
                height: '52px',
                borderRadius: '14px',
                background: 'rgba(239, 68, 68, 0.12)',
                border: '1px solid rgba(239, 68, 68, 0.25)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: 'var(--accent-rose, #ef4444)',
                marginBottom: '1.25rem',
              }}
            >
              <AlertTriangle size={26} />
            </div>

            <h3
              style={{
                fontSize: '1.25rem',
                fontWeight: 700,
                color: 'var(--text-primary, #ffffff)',
                marginBottom: '0.6rem',
                letterSpacing: '-0.01em',
              }}
            >
              {title}
            </h3>

            <p
              style={{
                fontSize: '0.875rem',
                color: 'var(--text-secondary, #94a3b8)',
                lineHeight: 1.6,
                marginBottom: '1.75rem',
              }}
            >
              {message}
            </p>

            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '0.85rem',
                flexWrap: 'wrap',
                justifyContent: 'center',
              }}
            >
              <Button variant="primary" size="md" onClick={this.handleReset}>
                <RotateCcw size={15} style={{ marginRight: '0.4rem' }} /> Try Again
              </Button>
              <Button variant="secondary" size="md" onClick={this.handleReload}>
                <RefreshCw size={15} style={{ marginRight: '0.4rem' }} /> Reload Application
              </Button>
            </div>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
