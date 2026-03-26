import React from "react";

interface GlassCardProps {
  title?: string;
  subtitle?: string;
  className?: string;
  right?: React.ReactNode;
  children: React.ReactNode;
}
export function GlassCard({ title, subtitle, className = "", right, children }: GlassCardProps) {
  return (
    <section className={`glass-card ${className}`}>
      {(title || subtitle || right) && (
        <header className="glass-card__header">
          <div>
            {title ? <h3 className="glass-card__title">{title}</h3> : null}
            {subtitle ? <p className="glass-card__subtitle">{subtitle}</p> : null}
          </div>
          {right}
        </header>
      )}
      <div>{children}</div>
    </section>
  );
}
